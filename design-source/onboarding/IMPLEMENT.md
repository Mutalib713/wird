# Wird onboarding and setup: the build brief

> **Superseded on 2026-10-07. Do not build this.** Mutalib chose a new first-time experience
> (seven intro screens, the track setup steps and an app tour) as part of the new look of the
> whole app. See PROFILE.md § 5bj and PLAN task 28. This folder stays for the record.

**For:** whoever builds this (Antigravity first). **Designed:** 2026-10-03.
**Status:** superseded; never built.

## In plain words

A new reader sees a splash screen, then five short screens that explain the app, then picks one
of two ways to begin: set it up step by step (five more screens) or start with defaults. After the
first day they finish, Wird asks once whether they want a daily reminder.

Everything in this folder describes those screens:

| File | What it is |
|---|---|
| `onboarding-flow.webp` | The whole flow on one board. Blue lines are taps, dashed grey lines happen by themselves. |
| `onboarding-flow.html` | The same board as a web page. Every size, colour and word on it is exact; open it to zoom or inspect. |
| `frames/*.webp` | Each screen on its own at 2× (822 × 1782 px = 411 × 891 dp). Build from these. |
| `tokens.json` | Every colour with its hex and how it was mixed. |
| `flow.src.html` + `build.py` | Source of the board. `python build.py` fills in the Qur'an text from `app/src/main/assets`, so no verse on the board was typed by hand. Viewing the board needs internet for its fonts. |

**1 px on the board = 1 dp in the app. Font px = sp.** The frames are a Pixel 6 Pro (411 × 891 dp).

## Rules for this repo (from `CLAUDE.md`, all of them apply to you)

1. **Never add AI attribution** to commits or PRs: no `Co-Authored-By`, no "Generated with".
2. Run `.\check.ps1` (lint with warnings as errors, plus unit tests). It must print `check: PASS`
   before you commit. `JAVA_HOME` is not set in the shell; `check.ps1` sets it itself.
3. Commit and push after each meaningful step, with an honest message about its state.
4. **Qur'anic text is never typed or generated.** It comes from `app/src/main/assets/arabic/{page}.json`
   and translations from `assets/translations/{20|32|57}/{page}.json`. The board's verses came
   from those files.
5. Icons are vector (Material Icons or drawables). **No emoji on buttons.** Emoji are allowed only
   on streak and stats figures (🔥 📖 🎙️ ❄️).
6. The repo is public: no personal details or secrets in any file.
7. **Do not touch the domain logic** (`domain/`, `data/`, `nudge/`) beyond the wiring listed below.
   Bug fixes there are queued for Claude after this lands, and editing the same files at the same
   time makes commits collide.

## What to build

**Replace** in `ui/SetupScreen.kt`: `Step0AnimatedSplash`, `Step1WhyWird`, `Step2LearningStyle`,
`Step3CoreFeatures`, `Step4Gateway`, `Step5Name`, `Step6ReadingGoalAndOrder`,
`Step7ReadingPosition`, `Step8AyahAndTarget`, `Step9Blessing`, and the "STEP n OF 9" header.

**Keep, unchanged:** `FullMushafPagePicker` (opened from Setup 4), `SurahList` (reused in Setup 3),
the `SetupScreen(onDone = ...)` signature, `ToolkitTour`, and Home.

**New:** a splash, five intro screens in a pager, a gateway, five setup steps, and a one-time
reminder sheet shown after the first finished day.

**Theme:** put the colours below in one place (for example `ui/theme/Dawn.kt`) and use them by
name. No hex inside a composable. The existing setup code hard-codes hex everywhere; don't copy that.

## Tokens

### Colour (Mutalib's pinned palette, PROFILE § 6f, and tints mixed from it)

| Token | Hex | Recipe | Used for |
|---|---|---|---|
| cream | `#F7F5ED` | pinned | the ground |
| deep | `#17382D` | pinned | headlines, body ink |
| ink2 | `#4B5551` | pinned | secondary text (7.5:1 on white) |
| green | `#245847` | pinned | buttons, selection, progress |
| gold | `#C9A24B` | pinned | ornament only, never text on light |
| goldtext | `#8E6900` | pinned | gold text on light (5.1:1) |
| sky-top | `#4E7768` | green + 20% cream | top of the sky |
| sky-upper | `#819D90` | green + 44% cream | sky, a third of the way down |
| sky-glow | `#EFE6D0` | cream + 18% gold | the dawn glow |
| sky-bottom | `#B6C4BA` | cream + 31% green | mist at the bottom of the splash |
| sky-mid | `#D2D5C5` | sky-glow + 50% sky-bottom | where glow meets mist |
| mosque | `#597F70` | cream + 75% green | mosque silhouette (drawn at 70% opacity) |
| tile | `#F5F1E5` | cream + 5% gold | inner tiles |
| chip | `#DEE2D9` | cream + 12% green | chips, selected fills, tonal button |
| bubble-me | `#D1D9CF` | cream + 18% green | the reader's chat bubbles |
| rule | `#E2E2DA` | cream + 12% ink2 | hairlines, unselected outlines |
| dot-off | `#C7C8C1` | cream + 28% ink2 | inactive pager dots, sheet grip |
| soon-edge | `#E2D0A4` | cream + 45% gold | dashed edge of the Coming soon box |
| review | `#D9A441` at 25% | the app's existing `review` token | "may need another look" mark |
| leaf | `#7EBB6A` | the leaf `AtmosphericHeader` already draws | wordmark leaf on dark sky |

`tokens.json` has the exact values; if a hex here disagrees with it, `tokens.json` wins.

### Type: the phone's own fonts, so the app gains no font files

| Role | Compose | Size / line height | Weight |
|---|---|---|---|
| Splash wordmark | `FontFamily.Serif` (Noto Serif) | 56 sp | Bold |
| Band headline | `FontFamily.Serif` | 28 / 32 sp, `LineBreak.Heading` | SemiBold |
| Card question | `FontFamily.Serif` | 20 / 26 sp | SemiBold |
| Sheet title | `FontFamily.Serif` | 24 / 30 sp, `LineBreak.Heading` | SemiBold |
| Body | `FontFamily.Default` (Roboto) | 16 / 24 sp | Normal |
| Notes, rows | `FontFamily.Default` | 14 / 20 sp | Normal |
| Eyebrow, labels | `FontFamily.Default`, all caps, letterSpacing 0.12 em | 12 / 16 sp | Bold |
| Buttons | `FontFamily.Default` | 16 sp | Bold |
| Qur'an in tiles | default Arabic (Noto Naskh Arabic) | 22 / 44 sp | Normal |
| Hadith, translations | `FontFamily.Serif` italic | 14–16 sp, line height 1.5 | Normal |

`LineBreak.Heading` balances headline lines so no single word sits alone on line two.

Transliterated names keep their hyphen together: write `Al‑Fātiḥah` (U+2011, the
non-breaking hyphen) wherever a name sits in running text, so it never breaks after "Al-".

### Shape and space (4-dp grid)

- Screen side margin 16 dp for cards and footers, 24 dp for band text.
- Cards: radius 24, padding 20, 1 dp `rule` border, shadow 0 6 18 at `deep` 8%.
- Tiles radius 14, padding 12 × 16. Options and inputs radius 12–16.
- Buttons 56 dp tall, fully rounded. Every tap target at least 48 dp.
- 12 dp between items in a card, 8 between chips.
- Intro band: 276 dp tall (31% of the screen). Setup band: 220 dp. The card starts 14 dp below the band.

## Screen by screen

The words below are final. Use them exactly. Sample data on the board (the name Amina, the three
example tracks and their streak numbers) is illustrative; real values come from state.

### Splash (`frames/00-splash`)

The full-screen dawn sky, a crescent, the mosque and minaret silhouette fading into mist, the app
icon, the wordmark, a hadith, and one button. Positions are measured from Mutalib's reference
image, as a share of screen height, so they hold on any phone:

| Element | Top | Size |
|---|---|---|
| App icon (the launcher icon, rounded 24%) | 33.6% | 15.1% of height, square |
| Wordmark "Wırd." (leaf on the ı, gold full stop) | 50.5% | 56 sp |
| "Your daily Qur'an companion" | 59.4% | 19 sp, `deep` |
| Hadith | 64.5% | 16 sp serif italic, `ink2`, 44 dp side margins |
| Button | 79.7% | 64 dp tall, 34 dp from each side |

- Sky: a vertical gradient `sky-top` 0% → `sky-upper` 11% → `sky-glow` 24% → `sky-glow` 56% →
  `sky-mid` 78% → `sky-bottom` 100%, with a cream radial glow centred at 50% × 50%.
- Wordmark: like Home's (`W`, a dotless `ı` with the leaf path from `AtmosphericHeader`, `rd`), plus
  a `gold` full stop. On the splash the leaf sits **on top of** the ı stem, where a dot would be,
  not across it, and is drawn in `green`.
- Hadith, word for word: **“The most beloved deed to Allah is the most regular and constant even if
  it were little.”** Caption: **SAHIH AL-BUKHARI 6464** (12 sp, bold, letter-spaced).
- Button: **See how Wird works** with a forward arrow icon. It opens intro 1.
- No pager dots on the splash. It does not advance by itself.

### Intro 1–5 (`frames/01` to `05`): a horizontal pager

Shared layout: the intro band (sky, wordmark "Wırd" white 24 sp at top-left, **Skip** at top-right,
then eyebrow and headline bottom-aligned in the band), a white card, five pager dots, and a full-width
button. Swiping works as well as the button. **Skip** on any intro screen goes to the gateway.

| # | Eyebrow | Headline | Card question |
|---|---|---|---|
| 1 | WELCOME TO WIRD | A little Qur'an, every day. | What is a wird (وِرْد)? |
| 2 | READING TRACKS | A routine that fits your life. | What is a reading track? |
| 3 | THE WHOLE QUR'AN | Read any sūrah, any time. | Your track keeps its place |
| 4 | RECITE & REVIEW | Say your portion out loud. | How Recite & Review works |
| 5 | THE COMPANION | Ask, and plan in plain words. | What can the Companion do? |

**Card 1:** "Your wird is the portion of Qur'an you promise to read each day. It can be two pages,
one page or a few verses. Small and steady is the point." Then a tile with Al-Fātiḥah 1–5 from the
assets, verses 2–4 in `deep` and the rest dimmed (`deep` at 32%), each verse followed by a small
ring with its number in Arabic-Indic digits, caption "Wird lights up today's portion on the page."
Then the note: "Miss a day and your streak starts again. Your total days read never goes down."

**Card 2:** "One goal with its own days, starting point, daily amount and reminder. Have as many as
you like. Each one keeps its own streak." Three example rows: Daily Reading · Every day · 1 page ·
🔥 12; Juz' ʿAmma · Mon–Thu · 10 verses · 🔥 24; Weekend Review · Sat–Sun · 2 pages · 🔥 8. Note:
"Read from Al-Fātiḥah forward, or from An-Nās back the way many madrasas teach."

**Card 3:** "Want Al-Kahf on Friday or Al-Mulk at night? Open it from the Sūrahs tab and read freely.
Your track's place doesn't move." A tile with 112:1–2 in Arabic and Saheeh International's English
under it (from the assets), caption "Saheeh International". Chips: 4 reciters · English · Hausa ·
Transliteration · Bookmarks.

**Card 4:** "Read aloud from the page or from memory, at home or on a trotro. Wird listens on your
phone and marks any verse that may need another look." A tile with 112:1–3, verses 1–2 in `deep`,
verse 3 with the `review` mark behind it, and the legend "May need another look". Then: "It works
offline after a one-time download: 42 MB, or 78 MB for the more accurate model. Wird never uploads
your voice." Note: "Read it another way? Tap Mark done. Both count, and your history keeps them apart."

**Card 5:** "Ask about any ayah, or change your plan by saying it." A short chat: the reader asks
"What does Al-Ikhlas verse 1 mean?"; Wird answers with 112:1 in Saheeh International and the
source line "Saheeh International · Tafsir Ibn Kathir when online ›"; the reader says
"half a page on Fridays"; Wird answers "Fridays are half a page now." (that is the app's real reply,
from `CompanionReply`). Then a dashed box: **COMING SOON** "Answers that show the verses, hadith and
sources behind them, so you can check them yourself."

Buttons: **Continue** on 1–4. **Set up my wird** on 5 (goes to the gateway).

### Gateway (`frames/06-gateway`)

Intro band with a back arrow instead of the wordmark. Eyebrow **SET UP YOUR WIRD**, headline
**Two ways to begin.** Card:

- "Step by step is five short screens: your name, what the track is for, where you start and how
  much each day."
- Tile **QUICK START USES** with three gold-dot bullets: "Read, 1 page a day" · "Juz' ʿAmma, from
  An-Nās back towards Al-Fātiḥah" · "A reminder after Maghrib, if you want one". Under them: "Change
  any of it later in Settings."
- Lock icon: "No account and no email. Your name, plans and recordings stay on this phone."
- Download icon: "Some things use data: mushaf pages as you open them, and audio, the review model
  and tafsir when you ask for them."

Buttons: **Set it up step by step** (filled, goes to Setup 1) and **Quick start with defaults**
(tonal: `chip` fill, `deep` text; goes straight to Setup 5 with the defaults filled in).

The old gateway said "100% Offline Guarantee" and "Never consumes data". That was false and must not
come back.

### Setup band (Setup 1–5)

220 dp. Top row: back arrow (48 dp target), a five-segment progress bar (4 dp tall: done segments
white, the current one `gold`, the rest white at 35%), and **Skip** on Setup 1 only. Then the eyebrow
**STEP n OF 5** and the headline. No moon on setup screens. The progress bar needs the semantics
"Step n of 5".

### Setup 1 · Your name (`frames/07`)

Headline **What should Wird call you?** One outlined field labelled **Your name** (24 characters,
words capitalised), focused, keyboard up. Note: "Wird uses it to greet you on Home. It stays on this
phone." **Continue** rides above the keyboard (`imePadding`). **Skip** leaves the name empty.

### Setup 2 · What it's for (`frames/08`)

Headline **What is this track for?** Three single-choice cards (radio on the left):

- **Read** · Tilāwah: "A portion a day, with the mushaf open." (default) → `TrackType.TILAWAH`
- **Memorize** · Ḥifẓ: "Learn new verses by heart, then say them back." → `TrackType.HIFZ`
- **Review** · Murāja'ah: "Keep what you've memorized fresh." → `TrackType.REVISION`

`readingMode` stays as today: `MEMORISING` for Ḥifẓ, otherwise `READING`.

Then **YOUR INTENTION** (optional): a text field prefilled "Build a daily Qur'an habit" and a
scrolling row of suggestion chips: "Read consistently", "Memorize new verses", "Finish the whole
Qur'an" (tapping one fills the field). At the bottom of the card: "You can add more tracks later,
each with its own goal." Button **Continue**.

### Setup 3 · Where you start (`frames/09`) ⚠ needs Mutalib's yes before building, see the end

Headline **Where do you start?**

- **WHICH WAY?** A two-option segmented control: **An-Nās → Al-Fātiḥah** (default,
  `ReadingDirection.TOWARDS_FATIHAH`) and **Al-Fātiḥah → An-Nās** (`TOWARDS_NAS`). Under it, for
  the first option: "Madrasa order: the short sūrahs first, working back. Each sūrah is still read
  from its first ayah." For the second: "From the beginning to the end of the mushaf."
- **START FROM**, presets that change with the direction:
  - An-Nās → Al-Fātiḥah: **Juz' ʿAmma, from An-Nās** · "Page 604 · the short sūrahs" with a
    RECOMMENDED badge (sūrah 114, ayah 1, page 604), then **Yā-Sīn** · "Page 440" (sūrah 36,
    ayah 1, page 440).
  - Al-Fātiḥah → An-Nās: **From the beginning** · "Al-Fātiḥah, page 1" (sūrah 1, page 1), then
    **Juz' ʿAmma, from An-Naba'** · "Page 582" (sūrah 78, page 582), then **Yā-Sīn** · "Page 440".
- A search field, "Search any sūrah by name or number", over the existing `SurahList` (juz'
  headings, number, name, ayah count, page).

Tapping a preset or a sūrah selects it and goes to Setup 4, as today. The button reads
**Continue with {name}**.

The old Yā-Sīn preset used page 442. Yā-Sīn starts on page 440 (`SurahIndex`); 442 is two pages in.

### Setup 4 · How much a day (`frames/10`)

Headline **How much each day?**

- **STARTING AYAH:** the sūrah name, "Ayah {n} of {total} · page {p}", and a 64 × 48 dp number field
  (1 to the sūrah's ayah count). Under it, the text button **Pick it on the page instead** with a
  book icon. It opens the existing `FullMushafPagePicker`, unchanged.
- **EACH DAY:** a 2 × 2 grid: **Half a page** (1 unit), **1 page** (2 units, default), **2 pages**
  (4 units), **A number of verses**. Choosing the last shows "[10] verses a day" with a number field,
  which feeds `customVerses` exactly as the current code does.
- Note: "A page is 15 lines of the mushaf."
- At the bottom of the card, a tile: **Your first day:** followed by the real first portion for the
  choices made, e.g. "page 604, which holds Al-Ikhlāṣ, Al-Falaq and An-Nās." **Compute this from the
  domain** (the same assignment logic the app uses for Home). Never write it by hand. If the domain
  gives a strange answer here, that is a bug to report, not to paper over.

Button **Continue**.

### Setup 5 · Bismillah (`frames/11`)

Headline **Bismillah, {name}.** (or **Bismillah.** with no name).

- The du'a in Arabic, exactly as the current code has it: اللَّهُمَّ اجْعَلِ القُرْآنَ رَبِيعَ قُلُوبِنَا
  (24 sp, `goldtext`). Under it, in serif italic: **“O Allah, make the Qur'an the spring of our
  hearts.”** The old English added "the light of our chests, and a steadfast companion in this life",
  which the Arabic shown doesn't say. Don't bring it back.
- **YOUR WIRD** summary rows: Track (track name · Read/Memorize/Review), Starts, Order, Each day,
  Intention (only if set), Reminder: **Wird asks after your first day**.
- Note: "Mushaf pages download as you open them. To read with no data at all, download all 604 once
  in Settings."

Text button **Change something** (goes back one step, answers kept), then the button **Start
reading**, which calls `onDone(...)` exactly as `Step9Blessing.onLaunch` does today and lands on
Home with the tour.

### Quick start defaults

Read (`TILAWAH`, `READING`), `TOWARDS_FATIHAH`, sūrah 114 ayah 1 page 604, 2 units (1 page), intention
"Build a daily Qur'an habit", no name. Today's code uses sūrah 78 page 582 with `TOWARDS_FATIHAH`.
That walks from An-Naba' to Al-Mursalāt and never reaches sūrahs 79–114, so it changes **if Mutalib
says yes** (see the end).

### After day 1 · the reminder (`frames/13`): new

A bottom sheet over Home, shown **once**, the first time any day is finished (marked done or
recited). Persist a flag so it never shows twice.

- Eyebrow **YOUR FIRST DAY IS DONE**, title **Want a reminder tomorrow?**
- "Wird can send one notification a day for {track name}, about 30 minutes after Maghrib, with that
  day's portion in it. Nothing else."
- "To work out when Maghrib is, Wird asks for your rough location once. Without it, the reminder
  comes at 8:00 pm."
- **Remind me after Maghrib**: keep the schedule at `NudgeSchedule.Default`, then ask for
  `POST_NOTIFICATIONS` (Android 13 and up; older phones have no such dialog), then
  `ACCESS_COARSE_LOCATION` if it isn't granted, then re-arm (`NudgeScheduler.arm`).
- **Not now**: set the schedule to `NudgeSchedule.Off`. Wird does not ask again by itself; Settings
  has the switch.

Wiring:

- Remove the call to `askForWhatTheReminderNeeds()` from the end of setup in `MainActivity`. The
  ask moves here.
- **No reminder may fire before the reader says yes here or turns one on in Settings.** On
  Android 12 and lower there is no notification permission, so the schedule itself must stay off
  until then.

The Android dialogs (`frames/14`, `15`) are the system's own. Wird can't style them. They are on
the board to show the order.

## Behaviour that applies everywhere

- **Back:** system back and the back arrow go one step back with answers kept. From intro 1, back
  goes to the splash. From the gateway it goes to intro 5.
- **Small screens and large fonts:** the card's content scrolls inside the card; the band and the
  button stay put. Check 360 × 640 dp with the system font at 130%.
- **TalkBack:** Skip says "Skip the introduction"; the pager reads "Screen n of 5"; the setup
  progress reads "Step n of 5"; each Qur'an tile gets a description such as "Al-Fātiḥah, verses 1 to
  5. Verses 2 to 4 are today's portion." Radio cards and the segmented control are single-choice
  groups.
- **Dark mode:** not designed yet. Build these screens in the light palette for now. Do not invert
  colours to make a dark version; a night design comes later.
- **No motion requirements.** If you animate, the pager slides, and nothing loops or blocks.

## Done when

1. `.\check.ps1` prints `check: PASS`.
2. On the emulator (`wird_pixel6pro`), a fresh install shows every screen above with the exact
   words, and both paths (step by step and quick start) end on Home with the tour.
3. `uiautomator dump` shows no node narrower than its text, and every button at least 48 dp tall.
4. Finishing day 1 shows the reminder sheet once; finishing day 2 does not show it again.
5. No reminder fires on a fresh install before the sheet is answered.
6. No hex colour appears inside a composable in the new code.

## Not in this job (Claude does these after you, so don't touch them)

- Page-based portions in madrasa order starting at 114:1 need a domain fix so day 1 is the whole of
  page 604. It's in `domain/Portion.kt`. If the "Your first day" tile looks wrong, leave it and report
  it.
- Word search, the README, and the rest of the in-app wording (tour, About, Settings).

## Needs Mutalib's yes before building

1. Quick start begins at An-Nās (page 604) and works back, instead of An-Naba'.
2. "Which way" and "where to start" on one screen (Setup 3), since the way decides the start.
3. The splash button says **See how Wird works** (his reference image used a generic label).
4. The phone's own fonts (Noto Serif, Roboto, Noto Naskh Arabic) instead of the Literata and Amiri
   in the earlier mockups, so the app gains no font files.
