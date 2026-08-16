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

## Status — the loop closes

**Wird is now the thing it was described as.** It knows where you are, shows today's
portion on a real mushaf page, reminds you at a moment that follows the sun, and lets you
finish the day by reciting it out loud.

| | |
|---|---|
| ✅ **Milestone 0** | It builds, it installs, and a scheduled notification fires and opens the page. |
| ✅ **Milestone 1** | The mushaf renders; it knows where you are; you can browse, jump, and change your mind. |
| 🔸 **Milestone 2** | Done: marking a day (6), the streak and the honest split (7), prayer-time timing (8). Left: the reciter's audio (9) and the home-screen widget (10). |

What Milestones 1 and 2 gave you:

- **Setup asks the question you can answer.** Pick your surah from a searchable list, see
  the page, tap the ayah you are on. Type the number instead if you know it.
- **The real Madani mushaf page**, in its own per-page font, with the bismillah where the
  printed page puts it.
- **Today's portion in dark ink; everything else pale** — on every page, no exceptions —
  and the ayah range named in words so nothing depends on telling two shades apart.
- **Swipe to any page, jump to any surah**, with "Today's portion" to come back.
- **A page that carries no chrome.** Tap it to reveal the bar; tap again and it goes.
- **Done means you said it out loud.** Record yourself reciting, or tap "I read it" —
  both allowed, both logged separately, and the split shown honestly.
- **The streak next to the total days read**, which never resets, and a streak of nought
  is never held up to you.
- **A reminder that follows sunset**, not a clock hour — thirty minutes after Maghrib by
  default, movable to any prayer, any fixed hour, or off.
- **Settings**: paper or ink, how much a day, lighter days, when to be reminded, and
  where you are in the mushaf.

⚠ **One thing is built and unwatched:** the nudge has not been seen to *fire* since task 8
rewired it. The timing is verified three separate ways and the alarm is registered on the
phone, but the notification itself last fired under task 3's code. Confirm it the next
time it goes off.

Everything after that is in `PLAN.md`.

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

### 3b. When the nudge arrives — following the sun (task 8)

**Plain version:** a fixed hour like "remind me at 7pm" is wrong half the year, because
7pm is broad daylight in one season and long after dark in another. So instead of a clock
hour, Wird hangs the reminder off a moment you already know: **thirty minutes after
Maghrib.** As sunset moves through the year, the reminder moves with it, and you never
touch a setting.

**The one idea worth remembering:** the app *calculates* prayer times rather than *asking
a server* for them. Where the sun is, for any date and any point on Earth, is settled
arithmetic — about forty lines of it. That means no API key, no monthly cost, no data
used, and it still works with the phone in airplane mode in a year's time. It is also why
none of this can break when someone else's free service shuts down.

**The thing that made this safe.** Different Islamic bodies define the twilight prayers
differently, so Fajr and Isha genuinely disagree depending on whose method you pick. We
measured that: across five published methods, Accra's Fajr ranged over nineteen minutes.
**Maghrib was 18:14 in every single one.** Maghrib *is* sunset, and sunset is astronomy
rather than convention — so anchoring on Maghrib means the setting nobody will ever open
is almost unable to do harm.

**How we know the maths is right.** Not by trusting it. Every value is checked against the
[Aladhan API](https://aladhan.com/prayer-times-api) — a completely separate implementation
— for two cities and both solstices. All twenty-four matched **to the minute, with zero
error**, and those exact numbers are QA checks 10 and 11. Then a third check landed by
accident: **Muslim Pro, already installed on the phone**, had its own adhan alarms at
12:11, 15:25 and 19:28 — our Dhuhr, Asr and Isha for that spot, exactly.

**Why it asks for location.** Sunset depends on where you are. Accra and Kumasi are only
130 km apart and still differ by seven minutes. You chose coarse location over guessing a
city from the phone's timezone, and the first real test proved you right: the phone
reported **6.68, −1.58 — Kumasi, not Accra**. The timezone guess would have said 18:44;
your actual position says 18:50.

Terms:

- **Coarse location** — the low-accuracy kind, roughly which neighbourhood you're in
  rather than which building. It is all sunset needs, it works indoors, and it doesn't
  wake the GPS radio and eat battery.
- **Declination** — how far north or south of the equator the sun stands on a given day.
  It's what makes days long in June and short in December.
- **Equation of time** — the gap between clock noon and actual solar noon, up to about
  sixteen minutes, because Earth's orbit is an ellipse rather than a circle.

**What it deliberately will not do.** It never shows you a prayer timetable, a countdown,
or an adhan. Wird is not a prayer app — you already have one. You pick a landmark you know
("after Maghrib") and the app quietly does the arithmetic. **No computed prayer time is
ever printed on screen**, and there's a test that fails if any label leaks one.

**When it can't do what you asked**, it says so rather than guessing. No location yet →
it falls back to the phone's timezone. Timezone it doesn't recognise → a plain 8pm, and
the settings screen tells you that's what happened. It will not invent a latitude: you can
work out longitude from a timezone, but not latitude, and a wrong latitude produces a
reminder that is confidently an hour out in December.

**What I got wrong, and what it cost.** Two things.

First, the smaller one: I asserted in a test that tomorrow's Maghrib would be a minute
earlier than today's. It isn't — in mid-August, Accra's sunset barely moves. The test
failed, which is exactly what a test is for; the fix was to prove "recomputed daily, not
just plus-24-hours" using the September equinox instead, where sunset really does shift a
minute a day.

Second, the one that matters: **wiring this up revealed that nothing in the app had been
scheduling a nudge at all.** Task 3 proved the alarm worked using a temporary button that
fired it fifteen seconds later, and that button was deleted during the task 4/5 rewrite.
Everything since then has been real machinery sitting idle. The receiver was also still
hardcoded to page 453, so had it ever fired, it would have announced the wrong portion.
The lesson is general: **a gate task proved with a throwaway trigger leaves nothing
behind.** Both are fixed, and the reminder is now armed from three overlapping places —
when the app opens, after each nudge fires, and after a reboot — because a reminder that
silently stops is worse than no reminder at all.

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

## 6. Getting around, without a home screen

Wird opens straight onto today's portion. There is no menu in front of it, on purpose:
the Phase 0 numbers said the *decision* was the problem — 8 of the last 14 missed days
were "saw it, didn't open it" — so every screen placed before the reading is another
place to bounce off.

That leaves a question: where do the controls live?

**The page carries none.** Tap it and a slim bar slides down with where you are (surah,
page, juz) and the ways out (another surah, settings). Tap again and it goes. That is what
Kindle and Quran for Android both do, and it is the only arrangement that takes more
controls without eating into the page.

**The gesture is invisible, so it gets taught.** The bar is showing the first time you
open the app and withdraws after 3.5 seconds. Once, ever. The alternative — a permanent
hamburger icon — announces itself but parks a mark on the Qur'an forever, and ☰ promises
a menu of destinations this app does not have.

Two bugs worth remembering, both found by using the app rather than by testing it:

- **The bar's surah name never changed.** The update was guarded by "only if this is the
  page we're on", and during a swipe those two are briefly out of step, so it was dropped
  every time. Now every drawn page is remembered by number and the bar looks up the one
  you are on.
- **"Today's portion" and go-to-surah did nothing.** `rememberPagerState` reads its
  starting page *once*; changing it later moves nothing. Both controls only ever worked on
  first composition. Jumping is now an explicit instruction, and it carries a counter —
  because swiping away from page 440 and back means a jump to 440 looks identical to the
  last one, which is exactly the case that failed.

**When something is named `initialX`, assume it is read once.**

### How many buttons actually fit across a phone

Settings laid all seven weekday buttons in one row. The last two of them, Saturday and
Sunday, could not be tapped at all.

The arithmetic is worth carrying around, because it settles this every time. Your Pixel is
1440 pixels wide. But pixels are not the unit layouts get written in: Android uses **dp**,
*density-independent pixels*, a unit that stays the same physical size on every screen, so
a 48dp button is the same amount of fingertip on a cheap phone as on an expensive one. Your
phone packs 3.5 real pixels into every dp. So 1440 pixels is 411dp of room, and the screen's
own 24dp margins leave 363dp to build in.

A button has a floor of **48dp**, roughly the pad of an adult finger, which is what
`Scale.minTarget` is for. Material widens its buttons to 58dp, and there is a 4dp gap
between each. So a day costs 66dp. Seven days want 458dp. There is 363dp. It never fit.

**What a `Row` does when it runs out of room is the half that bites.** It doesn't shrink
things to cope. It doesn't complain either. It hands out space in order, first come first
served, and whatever is still queueing gets whatever is left, which by then is nothing.
Saturday got a sliver and broke onto two lines. Sunday got zero. Something zero pixels wide
isn't merely off the edge of the screen; it never enters the accessibility tree, so someone
using TalkBack couldn't reach it either. Two days, quietly unselectable, in a setting whose
whole job is picking days.

Four then three now. The prayer buttons directly below had already hit this and been split
the same way. The note beside them even said a sideways-scrolling row is no fix, because
the option on the end simply stays hidden. The weekday row got written the naive way anyway.

**A rule written as a comment beside one row does not protect the row above it.**

## 7. Colour

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

## 8. Backups, and why they're off

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
