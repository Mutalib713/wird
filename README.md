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

## Status: the loop closes, and the app grew a front door

**Wird is now the thing it was described as, and then some.** It knows where you are, shows
today's portion on a real mushaf page, reminds you at a moment that follows the sun, and lets
you finish the day by reciting it out loud. Since then it has also become a Qur'an app you
could use for reading, which was a deliberate reversal rather than scope creep. See
`PROFILE.md § 5`.

| | |
|---|---|
| ✅ **Milestone 0** | It builds, it installs, and a scheduled notification fires and opens the page. |
| ✅ **Milestone 1** | The mushaf renders; it knows where you are; you can browse, jump, and change your mind. |
| 🔸 **Milestone 2** | Done: marking a day (6), the streak and the honest split (7), prayer-time timing (8), Shatri's recitation offline (9). Left: the home-screen widget (10). |
| 🔸 **Milestone 4b** | The companion exists and holds a real conversation. What powers its understanding is still undecided (task 22). |

What the first two milestones gave you:

- **Setup asks the question you can answer.** What to call you, whether you read from the
  mushaf or from memory, then your surah, the ayah you are on, and how much a day.
- **The real Madani mushaf page**, in its own per-page font, with the bismillah where the
  printed page puts it.
- **Today's portion in dark ink; everything else pale**, on every page, no exceptions, with
  the ayah range named in words so nothing depends on telling two shades apart.
- **Swipe to any page, jump to any surah**, with "Today's portion" to come back.
- **A page that carries no chrome.** Tap it to reveal the bar; tap again and it goes.
- **Done means you said it out loud.** Record yourself reciting, or tap the quieter route,
  and the app never blurs which of the two you did.
- **A reminder that follows the sun**, computed on the phone with no network and no key.
- **Shatri's recitation**, fetched once and playable offline afterwards.

What the sessions since added:

- **A home screen**: greeting, today's portion, your numbers, and the week behind you.
- **A companion you can talk to**, which turns "after Isha" into a real alarm.
- **The whole Qur'an, browsable**, grouped into juz' with each surah's meaning beside it.
- **Light and dark**, following your phone unless you say otherwise.

## The shape of the whole thing

There is no server. Nothing is uploaded. The app talks to three places on the internet, and
only ever to *download*:

```
   quran.com API   ──►  which words are on page 453, and on which line
   qpc-fonts repo  ──►  the font that draws those words
   qurancdn audio  ──►  Shatri reciting the ayahs you were assigned
                            │
                            ▼
                   saved to the phone's own storage
                            │
                            ▼
                   drawn or played, works offline forever after
```

That's it. Your reading history, your position, your name, your conversation with the
companion and your recordings never leave the phone. This isn't a privacy feature bolted on
— it's Sacred Rule 1 in `PROFILE.md`, and it's enforced in the build itself (see *Backups*
below).

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

### 3c. Hearing it recited (task 9)

**Plain version:** tap play and Abu Bakr al-Shatri recites today's portion. The first time,
it downloads one small MP3 per ayah. After that it works with no signal at all.

**The one number that shaped this.** A page of audio is **2.27 MB at 128 kbps**. The page's
own font is 154 KB — so *the recitation costs about fifteen times what the page costs*,
every day. That is 68 MB a month against 34 at 64 kbps. On Ghanaian mobile data that is a
real difference, so you chose to put the switch in settings rather than take either
default, and the lighter one leads.

**Only the ayahs you were asked to read.** A half-page portion fetches and recites half a
page. It works this out using the same "which lines are lit" rule the screen uses, rather
than a second copy of it — task 5f's bug was one rule living in two places and drifting.

**Listening does not mark the day done.** It sits beside "Recite it out loud" and "I read
it" but completes nothing, because hearing someone else recite is not reading and Sacred
Rule 6 turns on never blurring that.

**What I got wrong, and it nearly shipped.** The airplane-mode test was supposed to be a
formality. It failed — the app said *"Couldn't get the recitation. Check your connection"*
with all thirteen files already on the phone.

The cause had nothing to do with the network. Stopping playback set a flag called
`cancelled` to true, and only *starting* playback set it back to false — but starting
happens **after** the download step, and the download step gave up the moment it saw that
flag. So **listening worked exactly once per app launch**, and every attempt after that
blamed your connection.

Two things are worth taking from that. First, the failure was invisible online: it would
have looked like a flaky network, which is the most believable excuse an app can offer
someone on mobile data. Second, **no unit test would have caught it** — it lives in the
ordering between a media player, a coroutine and Android's own state. It was found by
running the thing on a real phone with the network switched off, which is exactly why the
plan insists on that rather than accepting a green build.

It is a counter now instead of a boolean, which also fixed a second problem nobody had hit
yet: two quick taps used to fight over the same player.

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

## 6. Getting around

**This section used to be called "without a home screen", and that is no longer true.** It is
worth saying why it changed rather than quietly editing it, because the reasoning was sound
and it was the app underneath that moved.

The original argument: Wird opens straight onto today's portion, because the Phase 0 numbers
said the *decision* was the problem. 8 of the last 14 missed days were "saw it, didn't open
it", so every screen placed before the reading is another place to bounce off.

That argument still holds. What changed is that the app grew past what it described. Once
there was a whole Qur'an to browse, a history to look at and a companion to answer, "no menu"
described a smaller app than the one that existed. So there is a home screen now, and the
rule it has to earn its place against is unchanged: **`Open the page →` is the loudest thing
on it, full width, one tap.** If missed days go up, that screen is the first suspect.

### Three tabs, at the top

**Home · Sūrahs · History**, under a bar carrying the app's name and a `⋮`.

They sit at the top rather than the bottom, which is the second reversal here. At the bottom,
tabs are thumb targets and want mass, which is why Wird had icons above labels. At the top
they are a heading: you read them once and read past them, so weight there competes with the
screen's own title. Hence an underline rather than a filled pill. The active tab is still
accented **and** bolder **and** underlined, so colour is never the only signal.

**There were four tabs; "More" is gone.** Settings was never somewhere you visit alongside
your wird. It is a drawer you open, change one thing in, and leave, and it was spending a
quarter of the bar on that. It lives behind the `⋮` now, next to a night-mode checkbox.

### The page still carries nothing

Tap the mushaf and a slim bar slides down: where you are, a back arrow, play, and a `⋮`
holding night mode, "read something else" and settings. Tap again and it goes. That is what
Kindle and Quran for Android both do, and it is the only arrangement that keeps a permanent
mark off the Qur'an.

**The top bar hides here too**, because the page is the one screen that should have nothing
parked above it.

**That combination once produced a dead end**, and it is the clearest bug this project has
shipped: the page hides the bar, the bottom bar it replaced was gone, and the app had no
back handling at all, so opening today's portion left you stuck, and the system back gesture
quit the app instead of returning. Fixed with both a visible arrow and real back handling.
The lesson is not about back buttons: **two changes that are each safe alone can remove the
last route out**, and only using the thing finds it.

## 7. Colour

**The palette has been pinned three times.** All three are in `PROFILE.md`: § 6b the
original, § 6c the gold, § 6d the current one. They stay because a colour decision that gets reversed
is worth keeping as a record of *why*, not deleting.

The current palette is **teal and manila, taken from Quran for Android** and pinned on
2026-08-18. It was read from that project's own `colors.xml` rather than eyedropped from
screenshots.

| | light | dark |
|---|---|---|
| app background | `#FAF8F7` | `#212121` |
| **the mushaf page** | `#FFF4CB` | `#1A1A1A` |
| main text | `#212529` | `#FFFFFF` |
| secondary text | `#656E76` | `#B5B5B5` |
| accent | `#00767F` | `#B2DFDB` |
| juz' section band | `#DEE2E6` | `#424242` |

**Contrast ratio** measures how far apart two colours are in brightness. 4.5:1 is the floor
for body text; below 3:1 two colours read as the same colour. Every pair here was computed,
not eyeballed.

Three things worth knowing:

- **The page has its own colour, and that is the point.** Warm manila inside near-white
  chrome; near-black inside dark grey at night. The app is the room and the mushaf is the lit
  page on the table. Earlier versions of this file claimed that was already true. It wasn't:
  the code painted the page with the same colour as every other screen, and § 6d is where it
  became real.
- **Two of the borrowed colours were darkened, and the reason is measurable.** Their detail
  grey was 4.43:1 on their own background and their teal was 4.27:1, both under the floor.
  That is fine in their app, where those roles carry short labels; Wird puts whole sentences
  in them. Darkened to 4.90 and 5.09. **Their dark palette needed no correction at all.**
- **The accent is a different colour in each theme.** Deep teal on light, pale teal on dark.
  The palette before this one had a single gold that measured **2.06:1 on cream**, literally
  unreadable, which forced the whole app to default to dark. Lightening the accent for the
  dark ground instead is the honest fix, and it is why the app can now follow your phone.

**One rule this palette carries.** On the light juz' band, the secondary grey is **3.99:1 and
fails**. Section labels there use the main text colour. On the dark band either works.

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

## 9. Home, and the shape of a day

Home is a dashboard, and it took three attempts to get there. The first version was
page-first with a thin band above the reading; that was rejected as neither thing. The
second reproduced the design's *text order* but none of its structure, and read as one long
column. `PROFILE.md § 5o` keeps that mistake written down because it is repeatable: **read a
design's structure, not its strings.** Sections are divided by edges, not by empty space.

What it carries, top to bottom:

1. **Where you are**: surah, ayah, page.
2. **The date**, weekday plus the Hijri date, computed on the device.
3. **A greeting**, with your name if you gave one.
4. **Today's portion**: the surah, how much, whether it is done, the page as a figure, and a
   bar showing how far through the mushaf you are. Then three ways to finish, and
   `Open the page →`.
5. **The check-in**, while the day is unfinished.
6. **Your numbers**: the streak, the total, and the split written out.
7. **This week**: the last four days, each with the page it covered, and a marker that
   separates a recitation from a tap at a glance.

**Three rules govern this screen and none of them are cosmetic:**

- **Today's portion does not change when you finish it.** The position advances on done, so a
  screen computed from the live position would rewrite what today *was* the moment you marked
  it. A finished day records what it covered and shows that until tomorrow.
- **A streak of nought is never announced.** Someone who missed yesterday does not need a zero
  held up to them.
- **Missed days are absent, and the screen says so.** "Days you missed aren't listed. There is
  no row saying you failed."

### The name, and how you read

Setup asks two things before the Qur'an ones, and both are small on purpose.

**Your name** is one string on this phone, used for the greeting and nothing else. It is
skippable, and the Skip is a full-width control rather than a grey word in a corner, because a skip
nobody can find is not a choice. If you skip it, the greeting is just the hour, which is what
it was before the feature existed.

**Whether you read from the mushaf or recite from memory** changes what the app calls things:
"Recite it out loud" becomes "Recite from memory", "I read it" becomes "I revised it". That
is small today and it is meant to be. The larger consequence is checking a recitation
against memory rather than against the page, and that lands with task 14. The setup screen
says only what is true.

## 10. The companion

The one feature aimed at the larger half of the problem. Phase 0 found ~6 missed days to
forgetting and ~8 to procrastination; the nudge, the widget and the streak all address the
first. This addresses the second.

**It is a conversation, and that was the second attempt.** The first was a question with three
chips under it, which reads as a poll: three answers, pick one, nothing suggesting you could
say anything else. Now: turns that persist to a file, its lines left and yours right, and an
input pinned to the bottom where every messaging app has taught people to look.

**The mechanic is the promise, not the chat.** Say "after Isha" and it repeats your own words
back and holds them in view: `YOU SAID "after Isha"`, the real alarm time, and a breathing
dot. Your own sentence at display size is something you can fail to keep; "reminder set for
8:00 pm" is a setting.

What it understands: a time, a refusal, "already did it", how you are doing, where you are,
open a surah, play today's portion — and, since PLAN task 22, **the plan itself**.

### Talking to it instead of hunting for the setting

Four things used to live only in Settings and now also answer to a sentence:

| You type | What moves |
|---|---|
| "one page a day", "make it half a page" | how much you read every day |
| "half on Fridays", "make Fridays lighter" | one weekday, leaving the rest alone |
| "move my reminder to 9 from now on" | the routine |
| "I'm travelling till Sunday", "I'm back" | quiet for a few days, then back on its own |

**You can say three of them in one breath.** "One page a day, half on Fridays, I'm travelling
next week" is read clause by clause and all three land. If one clause is not understood it gets
quoted back rather than dropped: *"One page a day from now on. I didn't catch 'explain surah
yasin to me'."* A parser that silently ignores a third of your sentence is worse than one that
admits it, because nothing tells you which third went missing.

**Being away does not write anything into your record.** No reminders until the day you named,
and nothing at all in `days.json` for the days you were gone. They are days you did not read,
the same as any other. Your streak restarts and your total days read never moves — that was
Mutalib's call, and the reason is that a day in that file should mean exactly one thing.

**The reminder comes back by itself.** A pause does not switch the alarm off, it sets it for
the far side of the trip. Saying "travelling till Sunday" on a Tuesday arms the alarm for
Sunday evening, so it arrives even if the app is never opened in between. Settings shows a
*Paused while you're away · back Sunday* row while it is running, and tapping it ends the pause.

### A promise about tonight is not a change of routine

⚠ **This was a real bug, and it had been live.** Replying "in an hour" wrote that time into the
**daily** reminder, so one three o'clock answer quietly made four o'clock the reminder time
every day afterwards. Nothing on screen said so.

The commitment now carries its own alarm and expires with the day it was made; the routine
underneath is untouched and returns tomorrow. Changing the routine has to be said in words —
"from now on", "every day", "move my reminder". When a sentence could mean either, it is read
as tonight only: a one-off that should have been permanent costs you one repeat, while a
permanent change that should have been one-off rewrites something you never asked to move.

**What it cannot do, stated plainly because the interface promises otherwise.** Underneath
is a set of hand-written rules understanding a handful of phrasings, not a language model. A
chat box invites anyone to type anything, so this interface promises more than the parser can
keep. Ask it what a surah means and it shrugs. The reply names what it *does* know rather
than apologising, and the shortcut buttons stay on screen, but that is mitigation and not a
fix.

**What powers it is decided: rules now, Gemini later** — his words, *"for now lets use the free
private offline then later we use gemini."* The rules cost nothing, run offline, need no key,
and cannot invent anything, which matters when the thing being edited is a record of someone's
worship. The model waits for PLAN task 24's verdict on whether the companion helps at all,
because paying for an engine before that evidence exists risks paying for something that gets
deleted. And when it lands, Sacred Rule 2 does not soften: the ayah, its translation and its
tafsir are fetched from a named source, never generated.

**Sacred Rule 3 governs every line it says.** No guilt, no disappointment, no "you broke your
streak". Saying "not today" gets "That's fine. It'll be here tomorrow" and changes nothing.
There is a test that fails if a refusal reply ever contains the words *streak*, *failed*,
*sure?* or *missed*, and it now runs over every reply the plan-editing sentences produce too.

## 11. The whole Qur'an

The Sūrahs tab is the visible half of reversing "a habit tool, not a Qur'an reader". Browsing
costs nothing: today's portion only moves when a day is marked done, never by reading
elsewhere, and the screen says so.

Built to match Quran for Android, because that is the app the reference screenshots came from:

- **Grouped into juz'**, each a full-width band with the juz' number and its opening page.
- **The meaning beside the name**, so it reads `An-Nisa (The Women)`.
- **Where it was revealed and how many verses**, underneath.
- **The opening page as a bare number.** Not "pp. 22–49". You tap a surah to reach its
  beginning, so where it ends is not the number you need.

**None of that data was typed.** The surah columns come from the Quran.com `/chapters`
endpoint and the juz' pages from `/verses/by_juz`, taking the page of each juz's first verse.
The regeneration was diffed against the previous file before replacing it, 114 rows with zero
mismatches, and the juz' numbers independently match the reference screenshots.

**One thing the data got wrong and a person caught:** six surahs are named for a person or a
word, and the API's translated name is that same word, so the list read "Hud (Hud)". The
parenthetical is now dropped whenever the meaning folds to the same string as the name.

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
- The mushaf and flame icons on Home: [Font Awesome Free 6](https://fontawesome.com/icons/book-quran)
  `book-quran`, `fire-flame-curved`, `sun` and `repeat`, icons licensed **CC BY 4.0**,
  Copyright 2024 Fonticons, Inc. Unmodified path data, rewrapped as an Android
  vector. Every other mark in the app is drawn in code; this one is not, and the reason is
  in PROFILE.md § 5ag — a Canvas path is the right tool for a mark and the wrong tool for an
  illustration.

Private repo. Not for distribution.
