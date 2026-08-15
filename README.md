# Wird

A *wird* (ورد) is the daily portion of Qur'an a person commits to reading.

Wird is an Android app that knows which page you're on, shows that page as it's actually
printed in the Madani mushaf, nudges you at a moment you're likely to be free, and treats
"done" as something you say out loud rather than something you tick.

It is not a Qur'an reader. It shows today's page and links out to Quran for Android and
Tarteel for everything else.

## Why

Reminders fix forgetting. Forgetting was the smaller half of the problem. The thing that
actually worked was having a teacher to recite to — someone who expected you, and in
front of whom you had to open your mouth. School ended that. Wird is a stand-in for it.

## Status — 5 of 21 tasks done

| | What works today |
|---|---|
| ✅ 1 | Project builds. `check` runs lint + tests. 3 tests pass. |
| ✅ 2 | App installs and opens on the Pixel 6 Pro. |
| ✅ 3 | A scheduled notification fires, and tapping it opens the page. |
| ✅ 4 | The real mushaf page renders, with today's portion lit and the rest dimmed. |
| ✅ 5 | It remembers where you are and how much you read a day. |
| ⬜ 6 | Marking a day done — recording yourself, or tapping. |

Everything after task 6 is in `PLAN.md`.

---

# How it works

Written for someone who owns this project but is still learning the vocabulary. Every
term is explained the first time it appears. Skim the bold sentences if you want the
short version.

## The shape of the whole thing

There is no server. Nothing is uploaded. The app talks to two places on the internet, and
only to *download*:

```
   quran.com API  ──►  which words are on page 453, and on which line
   qpc-fonts repo ──►  the font that draws those words
                            │
                            ▼
                   saved to the phone's own storage
                            │
                            ▼
                   drawn on screen, works offline forever after
```

That's it. Your reading history, your position, and later your recordings never leave the
phone. This isn't a privacy feature bolted on — it's Sacred Rule 1 in `PROFILE.md`, and
it's enforced in the build itself (see *Backups* below).

## 1. The build system

**Plain version:** Android apps aren't a file you double-click. You *build* them — a tool
gathers your code, checks it, and packs it into one installable file. We use Gradle, which
is the standard tool for that.

- **Gradle** — the build tool. `gradlew.bat` is a small script that downloads the right
  version of Gradle automatically, so the project builds the same way on any machine.
- **APK** — Android Package. The single file that gets installed on a phone. Ours is
  currently about 10.5 MB in *debug* form (debug builds are fat because nothing is
  stripped out; the real one will be much smaller).
- **Kotlin** — the language. **Jetpack Compose** — the way we describe screens: instead of
  designing a layout in a separate file, you write a function that returns what should be
  on screen, and Android redraws it when the data changes.

**The one command you need:**

```bash
./check.ps1
```

This runs **lint** (a tool that reads your code looking for known mistakes) and the
**unit tests** (small programs that check your logic gives the right answer). It prints
`check: PASS` or `check: FAIL`. Nothing gets committed on a FAIL.

Lint is set to `warningsAsErrors` — meaning even a mild complaint stops the build. Three
checks are switched off, each with a written reason in `app/build.gradle.kts`. If you ever
need to switch off a fourth, write down why; a silently growing list of ignored warnings
is how a check stops meaning anything.

## 2. The maths, and why it's tested

Two pieces of pure logic live in `app/src/main/java/com/mosman/wird/domain/`:

- **`Portion.kt`** — given "you're on page 453, you read one page a day", work out where
  today ends. Handles half-pages, and wraps from page 604 back to page 1.
- **`Progress.kt`** — the streak, the total days read, and how many days you *recited*
  versus merely *tapped*.

- **`Plan.kt`** — how much you read on a given weekday, and which lines of a page that
  works out to.
- **`SurahIndex.kt`** — where each of the 114 surahs sits in the mushaf.

**Why these have tests and the screens don't:** a wrong colour is visible. Wrong
arithmetic is not. If the streak silently miscounts, you'd never know — you'd just quietly
stop trusting the app. The six checks in `WirdQaTest.kt` cover the cases most likely to go
wrong:

1. Page maths across the wrap from 604 back to 1
2. A missed day breaks the streak, but total days read never goes down
3. Recited and tapped are counted separately, and a double-tap doesn't count twice
4. A weekday override changes only that day, and the same day always gives the same portion
5. A portion crossing into the next surah reports both, in the order you meet them
6. A half-page target lights half the lines, and the two halves tile the page exactly

Two of these have already earned their keep by catching real bugs.

## 3. The nudge

**Plain version:** the app asks Android to wake it up at a certain time. When that happens,
it posts a notification. Tapping the notification opens the app at today's page.

Three files in `nudge/`:

- **`Nudge.kt`** — asks Android to set the alarm, and remembers when it's for.
- **`NudgeReceiver.kt`** — runs when the alarm goes off, and posts the notification.
- **`BootReceiver.kt`** — runs after the phone restarts. **Alarms do not survive a
  reboot**, so without this, a phone that restarts overnight silently loses the nudge, and
  it looks exactly like the app being broken.

Terms:

- **AlarmManager** — Android's system for "wake me at this time", even if the app is
  closed.
- **BroadcastReceiver** — a piece of code that isn't a screen; it just runs when something
  happens. Ours is marked `exported="false"`, meaning no other app can trigger it. We
  tested that: even `adb` was refused.
- **Notification channel** — since Android 8, every notification belongs to a named
  category the user can mute individually. Ours is "Daily reminder".

### The exact-alarm problem — the important one

**From Android 14, an app is not allowed to set a precise alarm unless it's a clock or a
calendar app.** Wird is neither. Without permission, "remind me at 19:30" quietly becomes
"remind me sometime around then", and it drifts.

We measured this on your actual phone, on a clean install:

```
cmd appops get com.mosman.wird SCHEDULE_EXACT_ALARM  →  Default mode: default
canScheduleExactAlarms()                             →  false
```

So the app **checks, and tells you which one you got** — "Reminders arrive on time" or
"Reminders will drift". A reminder that arrives half an hour late without saying so is how
people decide an app is broken.

This is also why the home-screen widget is in the plan (task 10). A widget doesn't depend
on an alarm firing at all — it just sits there.

**Ghana-specific, and worse:** Tecno, Infinix and itel phones aggressively freeze
background apps to save battery. Your nudge can fire perfectly on a Pixel and never arrive
on a friend's Tecno. Task 15 handles that by detecting the manufacturer and walking the
user through their specific phone's settings.

## 4. The mushaf page — the hard part

This is the piece worth understanding properly, because it's unusual.

**Plain version:** the Qur'an page on your screen is not text in the normal sense. Each
printed page of the Madani mushaf has *its own font*, containing only the shapes needed
for that one page. Page 453 has a font. Page 454 has a different font.

**Why it's built that way:** a printed mushaf is typeset by hand so that every line ends
exactly at the margin and every page ends on the same ayah, worldwide. Ordinary Arabic
text can't reproduce that — the letters would join and break differently. So the King Fahd
Complex made each page into a set of pre-drawn shapes.

- **Glyph** — one drawn shape in a font.
- **Codepoint** — the number a computer uses for a character. `U+FB51` is a codepoint.

So the app does two downloads for a page:

1. **From quran.com:** a list of "codepoint U+FB51 goes on line 2, U+FB52 next to it…"
2. **From the font repo:** the font file where those codepoints have page 453's shapes.

Both get saved to the phone. **Opening a page you've read before costs no data at all.**

### The trap that could have shipped wrong Qur'an

Those codepoints (`U+FB51` and friends) are **not** private, made-up numbers. They sit in
a real Unicode block called Arabic Presentation Forms-A. That means **every font has an
opinion about them.**

The same codepoint draws different words in different fonts:

| Codepoint | In `QCF_P453.TTF` | In `QCF_BSML.TTF` |
|---|---|---|
| U+FB51 | ص | بِسْمِ |

So if the correct font fails to download and Android helpfully substitutes any other
Arabic font, the screen fills with **real, well-formed, completely wrong Qur'an**. Not
squares, not gibberish — wrong verses that look right.

That is why `MushafRepository.kt` **refuses to render without its own font** and says so
on screen instead. Showing nothing is far better than showing the wrong thing. This is
Sacred Rule 2, enforced in code rather than promised in a document.

We hit the same trap a second time building the bismillah header, which is three glyphs in
its own dedicated font — feeding it page one's codes drew the phrase one and two-thirds
times.

### Which font version, and what it costs you

Measured across all 604 pages:

| Version | Per page | One page a day | Loads on Android? |
|---|---|---|---|
| **v1 TTF ← we use this** | 154 KB | **~4.5 MB/month** | yes, directly |
| v2 TTF | 336 KB | ~9.9 MB/month | yes, directly |
| v1 WOFF2 | 78 KB | ~2.3 MB/month | no — needs a decoder |

There is no v2 WOFF2 build at all, so the small-and-native option only exists at v1. One
line — `Mushaf.FONT_VERSION` — switches both the font URL and which codes we ask the API
for.

### What costs data, and when

Three rules, in order:

1. **A page you have seen before costs nothing, ever.** Font and layout are both cached on
   disk. Opening it again is a file read: measured at **110–210 ms**.
2. **A page you swipe past costs nothing either.** A page waits 450 ms of stillness before
   fetching. Swipe through it and it is disposed first, which cancels the load. Measured:
   five fast swipes downloaded **zero** fonts.
3. **One page either side of today's is fetched quietly in the background**, once, after
   today's page is already on screen. Measured on a wiped cache: three pages came to
   471 KB, of which about **327 KB is the two extra ones**. Glancing forward or back is
   then instant — a prefetched page loaded in **22 ms**.

**The loading skeleton appears once per visit, then never again.** It exists to say "this
is loading, not broken", and that only needs saying once. By the time you have swiped past
the prefetched pages you have decided to browse, so later pages simply arrive on plain
paper. Leave the app and come back and the count starts over.

So only a page you deliberately stop on, away from today's, actually costs anything. The
whole mushaf is 91 MB and is never downloaded in bulk — that is not a thing to do to
someone on mobile data without asking.

Cold start with a warm cache, measured with `am start -W`: **1205 ms** to first frame,
page drawn 163 ms after that. Most of it is Android starting the app at all.

### Fitting the lines

A mushaf line is a fixed set of words that **must** sit on one line. It can't wrap and it
can't be cut off, because either one loses Qur'anic text. So each line is measured, and
**the type size bends** until the line fits. Lines vary in tightness, which is why it's
measured per line rather than once for the page.

Within a line, the words are pushed apart to fill the width. That spacing *is* the
justification in a mushaf, which is why each line is a row of separate words rather than
one block of text.

### Today's portion

Today's lines are at full strength. **Everything else on the page steps back to a muted
blue-grey.** That's the whole marking system.

There is deliberately **no highlight wash** over the text — no yellow band, no coloured
background. Other apps do that; it defaces the page.

An earlier version also turned the ayah numerals inside the portion to the accent colour.
It was built, looked at, and measured — deep teal against ink is 1.78:1, sage against
paper is 1.69:1, which means invisible. Removed. The dimming was doing the whole job on
its own.

## 5. Knowing where you are

**Plain version:** the app stores two things — the page you're due to read next, and how
much you read a day. From those it works out today's portion every time you open it.

**Everything is counted in half pages, not pages.** You might read "half a page on
Friday", and halves in decimals drift: add 0.5 to itself enough times and a computer
eventually lands somewhere slightly wrong. Whole numbers can't. So one page is 2 units,
half a page is 1, and the whole mushaf is 1208 units.

**Your plan can vary by weekday.** "One page a day, but Fridays are heavy" is stored as a
default plus one override, not as seven separate settings.

**Position only moves when you mark a day done.** Opening the app twice in a day shows the
same portion twice. A target that crept forward on every launch would be unusable, so
there's a test for exactly that.

**Finishing isn't an ending.** Page 604 is followed by page 1. That's what people actually
do, so the maths wraps instead of stopping.

### Surahs, and a bug the tests caught

`SurahIndex.kt` holds where all 114 surahs sit, generated from the API rather than typed
from memory, and bundled so surah names work offline and cost no data.

Two things that are less obvious than they sound:

- **A page can hold several surahs.** Ṣād ends on 458 and Az-Zumar begins on 458. Page 604
  carries three: Al-Ikhlas, Al-Falaq and An-Nas.
- **Order matters.** The first version listed surahs by number. So a portion wrapping past
  the end announced *Al-Fatihah, then An-Nas* — the reverse of the journey you take. Now
  they come back in the order you meet them. A test caught this, which is the entire
  argument for having tests on the maths.

### Half a page, on a page of lines

A mushaf page is 15 lines, and the mushaf doesn't record where its own half-way point is.
So a half-page portion lights the top half of whatever lines that page rendered, rounding
up — an odd fifteenth line reads better attached to the first half than orphaned at the
bottom. A test checks the two halves **tile the page exactly**: no line lit twice, none
missed.

Note "whatever lines that page rendered" — a page that opens a surah gives its first line
to the bismillah, so it has 14, not 15.

## 6. Colour

Your five colours from coolors.co are canon (Sacred Rule 8) — they don't get "improved"
later.

| | | |
|---|---|---|
| `#01161E` | ink | text on light, background on dark |
| `#124559` | deep teal | secondary text on light; a raised surface on dark |
| `#598392` | slate | **only** the ayahs outside today's portion |
| `#AEC3B0` | sage | accent on dark, "done" states |
| `#EFF6E0` | paper | background on light, text on dark |

**Contrast ratio** measures how far apart two colours are in brightness. 4.5:1 is the
accessibility floor for body text; below 3:1 two colours read as the same colour. Every
pair was computed, not eyeballed — the full table is in `PROFILE.md § 6b`.

Two things that matter:

- **ink on paper is 16.68:1.** That's your reading pair, and it's excellent. Dense Arabic
  needs it.
- **slate is never body text** — it fails on both backgrounds. Its low contrast is
  precisely why it's the right colour for the dimmed ayahs.

**The palette is a value ramp, not a hue wheel.** ink → deep teal → slate → sage → paper
is one journey from dark to light. Excellent for text on a ground; useless for a colour
that must pop out of a paragraph. That's why the accent works on a *button* (deep teal on
paper is 9.37:1) but failed on numerals sitting inside text.

## 7. Backups, and why they're off

Android normally backs apps up to Google Drive automatically. Wird switches that off
three times over, because the setting changed across Android versions and all three are
needed:

| File | Covers |
|---|---|
| `android:allowBackup="false"` | the blanket switch |
| `res/xml/backup_rules.xml` | Android 11 and below |
| `res/xml/data_extraction_rules.xml` | Android 12 and above |

The cost is real and accepted: **a new phone starts empty.** Task 19 adds an export you
choose to run, which is a thing you decide rather than a thing that happens to you.

---

## Building it yourself

```bash
./check.ps1
```

```bash
./gradlew.bat assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. Install it with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Machine-specific gotchas (JAVA_HOME, `local.properties`, the device rules) are in
`CLAUDE.md`.

## Where things are written down

| Question | File |
|---|---|
| What it is, who for, what's excluded, the stack, the Sacred Rules, the palette | `PROFILE.md` |
| What gets built and in what order | `PLAN.md` |
| Build commands, machine gotchas, device rules | `CLAUDE.md` |
| How a person moves through the app | `docs/app-flow.md` |
| What it looks like and why, and what got revised | `docs/ui-guidelines.md` |
| What must be true before it goes public | `docs/security-checklist.md` |

## Credits and licensing

- Qur'an text and page layout: [Quran.com API v4](https://api-docs.quran.foundation/)
- Mushaf rendering: QCF glyph fonts from the King Fahd Glorious Qur'an Printing Complex,
  via [nuqayah/qpc-fonts](https://github.com/nuqayah/qpc-fonts). **Not for commercial
  use.**
- Recitation: Abu Bakr al-Shatri
- Recitation checking: [`tarteel-ai/whisper-base-ar-quran`](https://huggingface.co/tarteel-ai/whisper-base-ar-quran),
  Apache-2.0, published by Tarteel

Private repo. Not for distribution.
