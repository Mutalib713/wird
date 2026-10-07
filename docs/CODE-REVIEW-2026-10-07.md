# Wird code review, 7 October 2026

A full read of the app's Kotlin code (33,263 lines in 82 files) for bugs, risky code and things
worth restructuring. Written for Antigravity, which will make the fixes, and for Mutalib, who
decides the open questions in Part 6.

The repo was clean at the time (`main`, commit `1a8146a`): lint passes with warnings treated as
errors, and all 134 unit tests pass. Everything below was found by reading the code. Where a
finding names a line, the line numbers are from that commit.

## In plain words first

- **The record of days can go wrong.** Most serious: if the app stays open past midnight, the
  next "Mark Done" is saved on yesterday's date. Two Settings screens can also quietly undo a
  change made a minute earlier, such as your reading position.
- **Reminders misbehave with more than one track.** A Monday-to-Thursday track still reminds you
  at the weekend. Tapping any reminder switches the app from automatic track choice to manual,
  for good.
- **A few buttons don't do what they say.** "Yes, I recited it · Streak +1" on Home marks
  nothing. Bookmarks open the wrong page. "Listen" sometimes plays one verse from the wrong half
  of the page. Leaving the page while recording throws the recording away without asking.
- **Restructuring.** The biggest jobs: colours typed straight into the code about 1,900 times, a
  few very long screen files, and leftover code from the removed "Life Spaces" feature.

## How to read each item

Every item has an ID (B1 to B33 for bugs, R1 to R8 for restructuring). Each one says:

- **What happens**, the way a reader would notice it
- **Where** (file and line)
- **Fix**, specific enough to act on
- **Prove it**, the test or emulator check that shows the fix works

**Priority:** *Must* (can write a false or wrong record, or loses data), *Should* (a feature does
the wrong thing), *Could* (polish or cost).

Items marked **AI phase** touch the Companion's language rules or the Whisper recitation check.
Claude is reworking those next, so Antigravity should leave them alone to avoid two agents editing
the same files.


## Part 1. The record of days (fix these first)

### B1. A day marked after midnight is saved on yesterday's date · Must

**What happens:** Leave Wird open in the background overnight and open it in the morning: Home
still shows yesterday. Tapping Mark Done then saves a row for yesterday, moves the position on, and
leaves today unmarked. The app is supposed to never hold a false record.

**Where:** `MainActivity.kt:163`, `val today = LocalDate.now()` inside `setContent`. It is only
recomputed when some state changes and the screen redraws. Nothing redraws on resume or at
midnight. Every callback (`onMarkRead`, `onDone`, `onUndo`) captures the stale value.

**Fix:**

1. Keep `today` in state: `var today by remember { mutableStateOf(LocalDate.now()) }`.
2. Update it on every resume (a `LifecycleEventObserver` for `ON_RESUME`, or `LifecycleResumeEffect`).
3. Add a `LaunchedEffect` that sleeps until the next midnight, updates `today`, and loops.
4. When `today` changes, call `refresh()` and `nudgeWidget()`.

R3 (moving this state into a ViewModel) solves it more cleanly. If R3 comes first, do it there.

**Prove it:** On the emulator only (never the Pixel), with `adb root`, open Wird, send it to the
background, change the emulator's date to the next day, bring Wird back: Home must show the new
day as not yet marked. Then mark it and check `days.json` has the new date. Set the emulator's
date back afterwards; other sessions use it.

### B2. Settings saves an old copy of the track and undoes recent changes · Must

**What happens:** In one visit to Settings, change the reading position, then change the daily
target. The position goes back to what it was. The same happens with Reading order followed by
Daily target, and with the reminder dialog.

**Where:** `ui/SettingsScreen.kt:218` loads `activeTrack` once. `SettingsScreen.kt:1466`
(Daily target) and `:1574` (reminder) save `activeTrack.copy(...)`, a full copy made before the
other changes, which went through `MainActivity` and never reached this local copy.

**Fix:** Do R2 first (an `editTrack(id) { ... }` call that reads the current track inside the
store and changes only the named fields). Then change these two saves, and the Edit Track save, to
`store.editTrack(track.id) { it.copy(dailyUnits = units, customTargetVerses = verses) }` and so on.
After any save, reload: `activeTrack = store.activeTrack()`.

**Prove it:** A unit test on `WirdStore` (the `DayLogStore(File)` pattern shows how to run store
code on a temp folder): save a position, then run an `editTrack` that changes only the daily target
from an older copy's id, and assert the position is unchanged. On the emulator: position then
daily target, and the position stays.

### B3. Editing a track changes its plan and position · Must

**What happens:** Open a track in "Manage tracks", change only its name, save. If its daily amount
was a page and a half or three pages (the Companion can set those), it becomes "10 verses a day" or
"30 verses a day". The position jumps to the top of its page, so a half page is read twice. A page
track also gets "first sūrah on the page, ayah 1" written in as its start verse, so Home shows the
wrong ayah.

**Where:** `ui/LifeSpaceDialogs.kt:487` treats any amount other than ½, 1 or 2 pages as a custom
verse target. `:489` invents a verse count from it. The save at `:1451–1461` always writes
`positionUnit = (startPage - 1) * 2` and the start verse from the dialog's defaults
(`:481–485`).

**Fix:**

- `isCustomTarget` starts true only when `track.customTargetVerses != null`. Show any other page
  amount as a page amount, labelled with `unitsLabel()` from `domain/Plan.kt`.
- Keep a `pickedStart` that is null until the reader actually uses the position picker. On save,
  `positionUnit = pickedStart?.unit ?: track.positionUnit` and the start verse is
  `pickedStart?.verse ?: track.startVerse`.
- Save through `editTrack` (R2) with only the fields the dialog changed.

**Prove it:** Pull the "what changes on save" logic into a pure function and unit-test it: a 3-unit
track renamed keeps `dailyUnits = 3`, `customTargetVerses = null`, its `positionUnit` and its null
start verse.

### B4. The day log is not written safely · Must

**What happens:** `days.json` is the only record of reading. If the app is killed while writing
it, the next launch finds it unreadable, moves it to `days.corrupt.json`, and shows every streak
and total as zero. A second corruption replaces that saved copy, so the history is gone for good.
Export never includes the saved copy, although the code says it is kept for export.

**Where:** `data/DayLogStore.kt:224` (`file.writeText`) and `:200` (rename). The same pattern is in
`ConversationStore.kt` and `BookmarkStore.kt`.

**Fix:**

- Write through `androidx.core.util.AtomicFile`: `startWrite`, write, then `finishWrite`, or
  `failWrite` on error. Do this in all three stores, through one small shared helper.
- On a read failure, rename to `days.corrupt-<timestamp>.json` so nothing is ever overwritten.
- Show a plain notice on History when a corrupt copy exists. Any new sentence goes through the
  humanizer gate.
- Include `*.corrupt*.json` in the export (see B26).

**Prove it:** Unit tests with `DayLogStore(File)`:

- garbage in `days.json` gives an empty list and keeps the file under a timestamped name;
- a second corruption keeps both copies;
- a write is never partial: interrupt it and check the old file survives.

### B5. Undo after the first day forgets the starting ayah · Should

**What happens:** Setup at Ya-Sin 1 (partway down page 440). Mark day one done, then undo. The
position returns to the top of page 440 with no start ayah, so the portion now begins with the end
of Fatir.

**Where:** `MainActivity.kt:899–903` and `data/WirdStore.kt:772` (`rewindTrack` restores the unit
only; `advanceTrack` had already cleared the start verse).

**Fix:** When marking a page portion done, also save the start verse it began from in the day row.
`DayLogStore` already has `firstVerse`, so set it for page portions too. On undo, restore it:
`rewindTrack(id, startUnit, startVerse)`.

**Prove it:** Unit test: advance a track with a start verse, rewind it using the saved row, and the
start verse is back.


## Part 2. Reminders

### B6. A track's days are ignored by reminders · Must

**What happens:** A "Juz' ʿAmma, Monday to Thursday" track still gets a reminder on Friday,
Saturday and Sunday.

**Where:** `nudge/NudgeScheduler.kt:97` arms every track without checking `isDueToday`, and
`nudge/NudgeReceiver.kt:66` posts for every track in the alarm.

**Fix:**

1. Add a pure function in `domain/`, for example `nextReminderOnDueDay(track, schedule, from,
   coordinates, away)`. It calls `nextAwake`, skips forward a day at a time until the date is one of
   the track's active days, and gives up after 8 tries.
2. Use it in all three branches of `arm()`: prayer, clock time and the 8 pm fallback.
3. In `NudgeReceiver`, skip any track where `!track.isDueToday(today)`.

**Prove it:** Unit test of the new function: a Monday–Thursday track evaluated on Saturday evening
gives Monday's time. A track with no active days set (meaning every day) is unchanged.

### B7. Tapping a reminder turns off automatic track choice · Must · needs D1

**What happens:** In automatic mode, Home opens whichever track is due today. Tapping any reminder
calls `store.setActiveTrack`, which also switches the mode to manual and pins that track. From then
on Home keeps opening that one track every day, until the reader finds the setting.

**Where:** `MainActivity.kt:121–125` and `:136–139`, calling `data/WirdStore.kt:686`.

**Fix (recommended, waiting on D1):** Open the reminder's track for this visit only. Keep a
one-off override in `MainActivity` state (or the ViewModel, R3) that `activeTrack` uses until the
app is closed or the day changes, and do not call `setActiveTrack` from the notification path.

**Prove it:** On the emulator: two tracks, automatic mode. Fire a reminder, tap it, then check
`track_schedule_mode` in `shared_prefs/wird_position.xml` is still `AUTOMATIC`.

### B8. "In an hour" on one track's reminder moves every track's reminder · Should · needs D5

**What happens:** With two tracks, tapping "In an hour" on Track A's notification also moves Track
B's reminder, and B's own schedule is ignored for the rest of the day.

**Where:** one app-wide `commitment` in `data/WirdStore.kt:187`, applied to every track by
`domain/TrackRecord.kt:121` (`effectiveSchedule`). `nudge/CommitReceiver.kt:68` writes it.

**Fix (recommended):**

- Store commitments per track: key `commitment_<trackId>`.
- `effectiveSchedule(track, commitmentFor(track.id), ...)`.
- `CommitReceiver` writes the commitment for its own `EXTRA_TRACK_ID`. The chat writes one for
  the active track.
- Read the old app-wide key once as the active track's, then delete it.

**Prove it:** Unit test of `effectiveSchedule` with two tracks and one commitment: only the
committed track changes.

### B9. The download service can restart a 90 MB download nobody asked for · Should

**What happens:** Start the 42 MB recitation model download on mobile data. If Android kills the
app, it restarts the service with no instructions. The service then starts downloading the whole
mushaf (about 87 MB). On Android 12 and later, going foreground again from a background restart
may also fail.

**Where:** `mushaf/MushafDownloadService.kt:100–136` returns `START_STICKY`. A sticky restart
delivers a null intent, which falls through to the mushaf branch.

**Fix:** Return `START_NOT_STICKY`. Both downloads already resume from where they stopped when
started again: pages skip what is cached, and the model restarts cleanly. If the download stopped
early, post a finished-style notification that says so and offers to continue.

**Prove it:** On the emulator, start the model download, then `adb shell am kill com.mosman.wird`.
No mushaf download notification may appear.

### B10. Model and page downloads block each other without saying so · Could

**What happens:** While "Download all pages" runs, tapping "Get the recitation checker" does
nothing visible, and the reverse is true too.

**Where:** `mushaf/MushafDownloadService.kt:148` and `:107` return early when the other job is
active.

**Fix:** Expose a `busy` state from the service's companion object. Settings and the reading page
then disable the second button with a short reason, or queue the second download to start when the
first ends.

**Prove it:** Emulator: start one, try the other, see the reason on screen.


## Part 3. Reading, listening and navigation

### B11. Leaving the page while recording deletes the recording · Must · needs D3

**What happens:** Recite for eight minutes, then swipe back or turn the phone sideways. The
recording is deleted, nothing is marked, and nothing is said.

**Where:** `ui/TodayScreen.kt:245`: `onDispose { recitation.release() }`, and `release()` calls
`cancel()`, which deletes the file. Rotation recreates the activity because `MainActivity` has no
`configChanges` and orientation is unlocked by default.

**Fix:**

1. Add `BackHandler(enabled = recording)` in `TodayScreen` that opens the existing
   `ClayConfirmDialog` with three choices: keep reciting, stop and keep, discard. The wording needs
   D3 and the humanizer gate.
2. Add `android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden"`
   to `MainActivity` in the manifest. Compose already handles those changes, so nothing is
   recreated.
3. If the screen still goes away while recording (for example the system takes it), stop and
   keep the file rather than deleting it.

**Prove it:** On the emulator, start a recording, rotate, keep talking, stop: the day is marked
RECITED with one file. Start again and press back: the dialog appears.

### B12. Bookmarks and recent pages open the wrong page · Must

**What happens:** From Home's menu, Bookmarks & Recents: tapping a bookmark on Al-Baqarah 2:255
opens page 2 (the sūrah's first page). Worse, the page opens in "today's portion" mode, which only
allows today's pages and two either side, so from a fresh launch the reader lands on today's page
instead.

**Where:** `MainActivity.kt:1239–1254` never sets `isWirdSession = false` (it starts `true`), and
`:1247–1248` uses the sūrah's `firstPage`. The Sūrahs tab's `onOpenBookmark`
(`MainActivity.kt:1029–1035`) has the same first-page mistake.

**Fix:**

- Set `isWirdSession = false` in both Bookmarks callbacks.
- Open `VerseIndex.pageOf(surah to ayah)` for a bookmark, in both places.

**Prove it:** Emulator: bookmark 2:255 from the reading page, go Home, open Bookmarks & Recents,
tap it. Page 42 opens, and the swipe is not limited.

### B13. "Listen" plays one verse from the wrong half of the page · Should

**What happens:** On a half-page plan, every other day today's portion is the bottom half of a
page. Tapping Listen plays the page's first verse, which is yesterday's half, and stops.

**Where:** `ui/TodayScreen.kt:513–518`. With no ayah chosen, the start defaults to the first verse
on the page. When that verse isn't in today's list, the code plays a list of just that verse.

**Fix:** When `startAt == null`, start at today's first verse on this page (`verses.first()`). Keep
the "play only that one ayah" behaviour for an ayah the reader explicitly tapped outside today.

**Prove it:** Unit-test the list-building step as a pure function: a second-half portion with
`startAt = null` gives today's verses from index 0. On the emulator, a half-page day: Listen plays
the bottom half.

### B14. The Companion's "open Al-Kahf" opens today's page instead · Should · AI phase

**What happens:** Typing "open Al-Kahf" in the chat replies "Opening Al-Kahf." and shows today's
page.

**Where:** `MainActivity.kt:618–623` doesn't set `isWirdSession = false`.

**Fix:** Set it, as the Sūrahs tab does. This is one line in `MainActivity`; Claude will do it with
the Companion rework, but it is safe for Antigravity to do it alongside B12 if that is easier. Say
which in the commit.

### B15. Home's "Yes, I recited it · Streak +1" does nothing · Must · needs D2

**What happens:** The check-in card's main button sends "Already did it" to the Companion, whose
mark-done action is deliberately empty. Nothing is marked and the chat replies "You haven't
recited today's portion yet."

**Where:** `ui/HomeScreen.kt:257–259` and the empty `MarkDone` branch at `MainActivity.kt:609–617`.

**Fix (waiting on D2):** Either make the button mark the day as read (`onMarkRead`, saved as
TAPPED and shown as "Marked as read", never as recited) and drop "Streak +1", or make it open the
page to recite. The label must say what actually happens either way. New wording goes through the
humanizer gate.

### B16. History's streak disagrees with Home's · Should

**What happens:** A Monday-to-Thursday track read every scheduled day shows a 12-day streak on Home
and a 4-day streak on History, because History breaks the streak every weekend. Choosing a track
filter on History changes the list but not the numbers.

**Where:** `ui/RecitationsScreen.kt:104` uses `progressOf` over every track's rows, without the
rest-day rule, and is not keyed on the filter.

**Fix:** With a track selected, show `trackProgress(logs, track, today)`. With "All" selected,
show total days read (distinct dates) and leave the streak to each track, or label the number
clearly. Default the filter to the active track.

**Prove it:** Unit test for whichever "All" rule is chosen. On the emulator, the History number
equals Home's for the same track.

### B17. Home shows the wrong target and progress in madrasa order · Could

**What happens:** In madrasa order (towards Al-Fātiḥah), a half page a day on page 604 shows a
target of "1 page". At the start of a long sūrah, the progress bar shows it almost finished.

**Where:** `ui/HomeScreen.kt:518–526` takes the amount from the day's page span. Lines `:552–555`
count madrasa progress from the sūrah's last page, but madrasa order reads each sūrah forwards from
ayah 1 (PROFILE § 5bf).

**Fix:**

- The target pill: `activeTrack.plan().unitsOn(today.dayOfWeek)` through `unitsLabel()`, or the
  verse target.
- Progress: use the forward formula (`startPage - firstPage`) in both directions.

### B18. "1 pages" for a page and a half · Could

**What happens:** The widget, Home's detail line and the target pill print "1 pages" for a page and
a half.

**Where:** `widget/WirdWidget.kt:200` (`detailOf`), `ui/HomeScreen.kt:1373` (`portionDetail`) and
`:518` each rewrite the amount wording.

**Fix:** Use `unitsLabel()` from `domain/Plan.kt` in all three, capitalised where needed.

### B19. Font, layout and audio files are cached unsafely · Should

**What happens:** If the app dies while saving a page's font, the half-written file can be big
enough to pass the size check. The page can then draw with missing words, the failure Sacred Rule 2
exists to prevent. Recitation audio has the same pattern.

**Where:** `mushaf/MushafRepository.kt:98, 306, 326` and `audio/PortionAudio.kt:250`.

**Fix:** Write to `name.part`, then rename, the way `audio/ModelDownload.kt` already does. Use the
shared helper from B4 if it fits.


## Part 4. Smaller bugs

### B20. A promise containing " | " is cut short · Could

**Where:** `data/WirdStore.kt:198`, `raw.split(FIELD)`. **Fix:** `raw.split(FIELD, limit = 3)`.
**Prove it:** unit test with spoken text `after isha | inshallah`.

### B21. Export says "everything" but leaves out tracks and settings · Should · needs D4

**Where:** `data/Export.kt:116–131`. The zip has days, chats, bookmarks and recordings, but not the
tracks, positions, plans, reminders or name stored in SharedPreferences, nor the corrupt-file copy
from B4. **Fix:** Add a `settings.json` holding the tracks (`toJson()` already exists) and the
reminder, and add `*.corrupt*.json`. Make the README list what is really inside. The README text
goes through the humanizer gate.

### B22. The emoji on Quick Start · Could

**Where:** `ui/SetupScreen.kt:1206`, `"⚡ Quick Start with Defaults"`. `wird/CLAUDE.md` allows emojis
on streak and stats figures only. **Fix:** a vector icon, or let the onboarding rebuild
(`design-source/onboarding/IMPLEMENT.md`) replace the screen.

### B23. Slow lookups: every track read re-reads the day log · Could

**Where:** `data/WirdStore.kt:523` and `:642`. `activeTrack()`, `positionUnit`, `plan`,
`startVerse` and `readingDirection` each re-read and re-parse `days.json` and the track list.
`refresh()` reads the log three times. **Fix:** Keep the parsed rows in memory inside
`DayLogStore`, reloaded only when the file's modified time or length changes or after its own
writes. **Prove it:** count reads in a unit test with a spy file, or time a refresh on the
emulator with a year of fake rows.

### B24. Companion plan changes are ignored on verse-target tracks · Should · AI phase

"Two pages a day" on a "10 verses a day" track replies that it is done and changes nothing, because
`customTargetVerses` wins (`MainActivity.kt:636–651`). Claude will fix this with the Companion
work.

### B25. "Playing today's portion" but nothing plays · Should · AI phase

`domain/CompanionReply.kt:54`. The Listen action only opens the page. Claude will fix this with
the Companion work.

### B26. Short sūrah names match inside other words · Should · AI phase

`domain/CompanionBrain.kt:563–571`. The sentence is matched with spaces removed, so "it's a
disaster" contains "Sad". Claude will fix this with the Companion work.

### B27. Any "what is" or "reflect" becomes a tafsir · Should · AI phase

`domain/CompanionBrain.kt:234–290`. "I want to reflect at 9 pm" returns today's tafsir instead of
booking 9 pm. Claude will fix this with the Companion work.

### B28. The recitation check's timeout cannot stop Whisper · Should · AI phase

`MainActivity.kt:475–481` and `audio/Recogniser.kt:177`. The native call can't be interrupted, and
re-recording during a check can load a second model. Claude will fix this in the Whisper work.

### B29. Two different Arabic letter-folding rules · Should · AI phase

`domain/RecitationCheck.kt:112` folds without the hamza seats (ؤ, ئ) that `domain/QuranSearch.kt`
folds, so the check can mark a correctly recited word as different. Claude will merge them in the
Whisper work.

### B30. Review marks leak between tracks and days · Could · AI phase

`MainActivity.kt:241`. `reviewVerses` is one set for the whole app and is lost on restart. Claude
will tie it to the day and track in the Whisper work.

### B31. Whole-object track saves (the cause of B2 and B3)

See R2.

### B32. Dead Life Spaces code

See R5.

### B33. The widget can say "TODAY, DONE" the next morning · Could

**Where:** `widget/WirdWidget.kt` updates only when the app changes something, and Android's
30-minute refresh does not run while the phone sleeps. **Fix:** schedule an inexact update just
after midnight (AlarmManager, or WorkManager if added) that calls `refreshWidget`. This pairs with
B1.


## Part 5. Restructuring

### R1. Colours are typed into the code instead of named once

About 1,900 hard-coded colours (`Color(0xFF245847)` alone appears 163 times) and 71 places that decide dark
mode by comparing a background colour to a hex value
(`colors.surface == Color(0xFF212121) || ...`). `ui/theme/Palette.kt` still describes the old teal
palette.

**Fix:** Put the pinned palette into `Palette.kt` as named tokens with light and dark values: cream
`#F7F5ED`, green `#245847`, deep `#17382D`, grey text `#4B5551`, gold `#C9A24B`, gold text
`#8E6900`, plus the dark-mode greens already in use. Add `isDark` to `LocalWirdColors`. Then
replace screen by screen.

**Careful:** the palette is Mutalib's pinned choice. Do not change any value, only where it is written down.
Check light and dark screenshots before and after each screen.

### R2. One safe way to change a track

`WirdStore.updateTrack(track)` overwrites the whole saved track with whatever copy the caller holds.
That is why B2 and B3 happen.

**Fix:** Add `fun editTrack(id: String, change: (ReadingTrack) -> ReadingTrack)`. It reads the
current saved track, applies `change`, and saves. Move every caller to it, then make
`updateTrack` private.

### R3. MainActivity holds the whole app

`MainActivity.kt` is 1,338 lines of state and logic inside one composable. Rotation throws all of
it away (see B11).

**Fix:** A `WirdViewModel` holding today, the tracks, the log version, the chat turns and the check
state as `StateFlow`s, with `MainActivity` only drawing them. This also gives B1 and B7 a natural
home.

### R4. Five screen files are too long to work in safely

Line counts: `SettingsScreen.kt` 3,475, `SetupScreen.kt` 2,585 (being replaced by the onboarding
rebuild), `LifeSpaceDialogs.kt` 2,490, `TodayScreen.kt` 2,028, `HomeScreen.kt` 1,482.

**Fix:** Split by section into files in the same package (Settings rows, each dialog, the reading
page's audio dock, Home's cards). Move logic out of composables into `domain/` where it can be unit
tested.

### R5. Remove the dead Life Spaces code

Life Spaces were flattened on 1 October. Still in the code:

- `LifeSpaceManagerDialog` and `LifeSpacePickerDialog`;
- the freeze and unfreeze UI in `LifeSpaceBar.kt`;
- `setTrackFrozen`;
- the no-op stubs at the end of `WirdStore.kt` (`addLifeSpace`, `setSpaceFrozen` and the rest).

**Fix:** Delete them. Keep only the one-time reader for the old `life_spaces_json` key, which an
old install may still have, and keep parsing `isFrozen` so old saves load.

### R6. A dead setting

Setup still writes `store.readingMode` (`MainActivity.kt:770`), but the mode now comes from the
track's type and nothing reads the stored value. Remove the write and the property.

### R7. Missing system signals for reminders

There are no receivers for time zone or clock changes, or for the exact-alarm permission being
granted. After travel the reminder fires at the old local time until the app is opened. After
granting exact alarms it stays inexact until the app is opened.

**Fix:** Receive `ACTION_TIMEZONE_CHANGED`, `ACTION_TIME_CHANGED` and
`AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, and call `NudgeScheduler.arm`.

### R8. The notification uses a system icon

`nudge/NudgeReceiver.kt:121` uses `android.R.drawable.ic_menu_agenda`. Use a monochrome version of
the app's own icon.


## Part 6. Decisions only Mutalib can make

- **D1 (B7).** Tapping a track's reminder: open that track for this visit only (recommended), or
  switch to it until you change it back?
- **D2 (B15).** Home's "Yes, I recited it": mark the day as read (shown as "Marked as read", never
  as recited), or take you to the page to recite? Either way the "Streak +1" label goes unless it is
  true.
- **D3 (B11).** Leaving the page mid-recording: ask with three choices (keep reciting, stop and
  keep, discard)? The wording needs his OK.
- **D4 (B21).** Should the export include your tracks, reminders and name in a `settings.json`?
- **D5 (B8).** A promise made from one track's reminder: for that track only (recommended), or for
  all tracks?
- **Still open from before:** a release signing key, the debug APK on the public showcase site, and
  the old APK copies in git history.


## Part 7. How to work through this

**Order:**

1. Records: B1, B4, R2 then B2 and B3, then B5.
2. Reminders: B6, B9, B10, then B7 and B8 once D1 and D5 are answered.
3. Reading page: B12, B13, B17, B18, B19, then B11 once D3 is answered.
4. The rest: B16, B20, B23, B33, R5, R6, R7, R8, then B15 and B21 once D2 and D4 are answered.
5. R1, R3 and R4 last, one screen or file at a time, each in its own commit.

**Rules that apply to every change** (from `CLAUDE.md` and `PROFILE.md`):

- Read `PROFILE.md` and `PLAN.md` first. `PROFILE.md` wins over everything else.
- Run `.\check.ps1` (lint with warnings as errors, then the unit tests) before every commit. It must
  print `check: PASS`.
- Commit and push after each fix, one fix per commit, naming its ID (for example "B12: open
  bookmarks on the ayah's page").
- **No AI attribution anywhere**: no `Co-Authored-By`, no "Generated with ...", in commits, pull
  requests or files.
- The repo is public: no personal details, device serials, LAN addresses or keys in any file.
- Never change the pinned palette's values (R1 moves them, it doesn't change them).
- Qur'anic text is never generated or retyped. The search files in `assets/search/` are
  generated: after changing a bundled text or a fold, set `WIRD_WRITE_SEARCH_INDEX=1` and run
  `QuranSearchTest`.
- A recorded day and a tapped day stay separate everywhere (Sacred Rule 6), and nothing may write a
  day that was not read.
- Every new or changed sentence a reader sees passes the humanizer gate
  (`python C:/Users/USER/.claude/skills/humanizer/scripts/gate.py <file> --profile ux-microcopy`).
- **Devices:** `emulator-5554` is shared with other sessions. Check which app is in front before
  driving it, and put it back after. Mutalib's Pixel is his daily phone: never touch it without
  asking him first.

**What "done" means for each fix:** the test named under *Prove it* exists and passes,
`check.ps1` passes, the emulator check (where there is one) was actually run, and the commit
message names the ID.
