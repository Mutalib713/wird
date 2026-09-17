# Wird — PLAN.md

Read `PROFILE.md` first. Every task below is one session's work, ends with something
observable, and is committed and pushed before the next one starts.

⚠ = genuinely risky. The task may fail, and failing is an acceptable outcome as long as
we learn the real answer and write it down. Never fake a ⚠ task green.

---

## Milestone 0 — Skeleton

- [x] **1. Repo scaffolding and the check command** — done 2026-08-14
  Gradle project, Kotlin + Compose, `check.ps1` running `gradlew.bat lint
  testDebugUnitTest`, `WirdQaTest.kt` with its first three checks, `.env.example`,
  `.gitignore`. **Also measure the QCF font payload** and record real byte sizes in
  PROFILE.md § 10.
  *Done when:* `check` output is green and pasted into the session, and PROFILE.md
  carries real font sizes instead of a guess.

  *Note on the QA checks:* the third check was written up as "portion arithmetic across
  a surah boundary." Page-based portions do not care about surah boundaries — the
  boundary that can actually break the maths is the **wrap from page 604 to page 1**, so
  that is what the test covers. Half-page targets are held as integer half-page units
  rather than fractional pages, because 0.5 + 0.5 + 0.5 drifts and integers do not.

- [x] **2. App shell on the phone** — done 2026-08-14
  One screen. Installs and opens on the Pixel 6 Pro over USB.
  *Done when:* the screen is visible on the actual phone.
  *Evidence:* `topResumedActivity=com.mosman.wird/.MainActivity`, process alive, no
  crash lines in logcat, screenshot captured. Debug APK 11,035,672 B (10.5 MB) — that
  is unshrunk debug output; the release build with R8 will be a fraction of it, and the
  number that matters for testers gets measured at task 16.
  *Note:* the position and target are hardcoded, but the portion is computed by
  `assignPortion`, so this also proved the domain layer runs on device rather than only
  in a JVM test.

- [x] **3. A notification fires and opens that screen** ← the "it's live" gate — done 2026-08-14
  Scheduled local notification, tapping it deep-links to the portion screen.
  Request `SCHEDULE_EXACT_ALARM` with a screen that explains why, and detect when it has
  been refused.
  *Done when:* a real notification arrives on the Pixel and the tap lands on the right
  screen. **No feature work happens before this passes.**
  *Evidence:* `WirdNudge: nudge fired` / `notification posted: Page 453` from a real
  AlarmManager alarm; `dumpsys alarm` shows `*walarm*:com.mosman.wird/.nudge.NudgeReceiver`;
  channel `wird_daily` importance 4; heads-up banner rendered "Today's wird / Page 453";
  Mutalib tapped it and confirmed the screen showed "Opened from the reminder."
  A force-stop plus plain launch does *not* show that state, so the flag is not sticky.
  `am broadcast` from adb was refused — the receiver is `exported=false` as intended.

## Milestone 1 — The page

- [x] **4. ⚠ Render the real mushaf page** — done 2026-08-15, risk resolved
  *Evidence:* QCF glyphs render correctly in Compose via
  `Typeface.createFromFile` → `FontFamily(typeface)`. No WebView needed, no fallback to
  plain Uthmani. Page 453 draws all 14 lines on one screen with lines 2–8 at full ink and
  9–15 in slate. `WirdMushaf: cached font page 453: 160704 bytes`.
  *Font decision, measured across all 604 pages:* **v1 TTF**, 154 KB/page, ~4.5 MB/month
  for a page-a-day reader. v2 is 336 KB/page (~9.9 MB/month) and **has no woff2 build at
  all**, so the small option only exists at v1. One switch — `Mushaf.FONT_VERSION` —
  moves both the font URL and the API glyph field.
  *Two corrections to this task's own assumptions:* glyph codes are **not** in a private
  use area, they are Arabic Presentation Forms-A (U+FB51–FC13 for v1) — which makes font
  fallback dangerous rather than merely ugly, since a system Arabic face would draw real
  but wrong text. And the task-1 "woff2 is 77% smaller" measurement compared v1 woff2
  against v2 TTF, two different fonts. The repo also moved: `mustafa0x/qpc-fonts` →
  `nuqayah/qpc-fonts`.
  *Still open:* the ornamental surah header (mushaf line 1) is not drawn — the surah name
  sits in the chrome instead. Light mode is built but not yet seen on a real screen.

- [x] **5. Position, target, and today's portion** — done 2026-08-15, verified on the phone
  Store where you are. Store pages per day with per-weekday overrides. Compute today's
  assignment. Handle surah boundaries and the end of the mushaf wrapping to page 1.
  *Done when:* setting page 453 with a one-page target yields the right range, verified
  against the physical mushaf, and the QA suite covers a surah boundary.
  *Evidence:* QA suite grew 3 → 6, all passing. New checks cover per-weekday targets,
  surah boundaries (including the four surahs touched by wrapping 604 → 1), and the
  half-page line split tiling a page exactly with no line lit twice or missed.
  `SurahIndex` generated from the API's `/chapters`, 114 entries, bundled so surah names
  cost no data.
  *A test caught a real bug:* `SurahIndex.across` sorted by surah number, so a portion
  wrapping past the end announced Al-Fatihah *before* An-Nas — a journey nobody takes.
  Now returns surahs in the order you meet them.
  *Verified on the phone in 5a:* setup ran end to end and stored a real position.

- [x] **5a. Tap an ayah to say where you are** — done 2026-08-15
  *Evidence, on the phone:* scrolled to Ya-Sin, tapped a word in ayah 5, screen read
  "Starting at Ya-Sin 5, page 440" and the number box auto-filled with 5. Stored
  `start_surah=36 start_ayah=5 position_unit=878`. Search also verified: typing "yasin"
  matches "Ya-Sin", since punctuation is stripped from both sides.

- [x] **5b. Swipe between pages, and go to a surah** — done 2026-08-15
  *Evidence:* swiping right turned 440 → 441, and page 441 rendered at full ink because
  it is not part of today's portion. Footer shows "Read something else" on today's page
  and "Back to today's portion" elsewhere. Page number sits centred at the foot.
  *Measured caveat:* `beyondViewportPageCount = 0` does **not** mean nothing loads ahead.
  One swipe composed 441 *and* 442, so a flick can cost two pages rather than one. That
  is the pager settling, not a policy failure, and it is written into the file so nobody
  later claims zero prefetch.
  *Still to do in 5c:* setup can be completed by a mis-tap and there is currently no way
  back to change your position without clearing app data.

- [x] **5d. First render, measured** — done 2026-08-15
  The "5+ seconds of skeleton" was wrong on both counts. Measured with `am start -W`:
  cold launch **1205 ms** to first frame, page drawn **163 ms** after that, and
  `load()` itself 110–210 ms from a warm cache. My earlier figure included adb's own
  round-trip plus a 450 ms settle delay that was wrongly applying to pages already on
  disk. Cached pages now skip that delay entirely.
  *Also added:* the two pages either side of today's are fetched in the background after
  today's page is on screen — verified by deleting 438 and 442 from the cache and
  watching them refill, with today's page still loading first.

- [x] **5c. Settings** — built 2026-08-15, on-phone check pending
  Reachable from the foot of today's page, next to "Read something else". Returns there.
  - **How it looks:** Paper / Ink / Match phone. Dark was already built and
    contrast-checked; it needed a switch, not a design. Stored, so it survives a restart.
  - **How much a day:** the same three amounts as setup.
  - **Go easier on some days:** pick weekdays, give them their own amount. The domain has
    supported per-weekday targets since task 5; nothing exposed them until now.
  - **Where you are:** shown in words ("Ya-Sin 5, page 440") with a button that reopens
    the setup flow, which fixes the 5b note about a mis-tap being unrecoverable.
  *Deviation from the original wording, on purpose:* the plan said changing the amount
  should affect tomorrow rather than today. It applies immediately instead, and the screen
  says so. Changing "one page" to "two pages" and having today stay at one page would look
  broken; the start of the portion never moves, so nothing shifts underneath you.
  *Not included:* choosing a highlight colour. See Sacred Rule 5.
  *⬜ Still to verify on the phone:* every setting surviving a restart. TikTok was in the
  foreground at install time and the device rule says don't.

  **Overflow bug found and fixed 2026-08-16**, reported by Mutalib from a screenshot and
  then measured with `uiautomator dump`. "Go easier on some days" laid seven weekday chips
  across one Row. Measured on the Pixel: chip centres 231px apart, so a 66dp pitch — 48dp
  minimum target, widened to Material's 58dp button floor, plus the 4dp gap. Seven of them
  need 458dp and the screen offers 363dp inside the margins. **Saturday broke onto two
  lines and Sunday was given zero width**, which means it was missing from the
  accessibility tree entirely, not merely off-screen — unreachable by touch *and* by
  TalkBack. Now four then three, matching the prayer rows directly below it.
  *Verified on the phone:* all seven chips measure **exactly 58.0 × 48.0 dp**, so the
  minimum target survived, with the 4dp gap between rows landing as measured (chip bottom
  1775px, next chip top 1789px). The wider row uses 260dp of the 363dp available — 103dp
  of slack, which is what makes it safe on a 360dp Transsion screen as well, where the
  old single row would have failed even harder. And the days *work*: picking Sunday, then
  Saturday, then unpicking each in turn drove the amount sub-row and the hint through
  every state in the right order. That is the part that counts — the bug was never that
  the row looked wrong, it was that two days could not be chosen.
  *The lesson worth keeping:* the prayer section had already hit this and solved it, with
  a comment saying a sideways-scrolling row "hides the option on the end". The weekday row
  was written the naive way anyway. **A decision recorded as a comment next to one row does
  not protect the row above it** — a chip row that must fit a fixed set is a rule, and it
  belongs somewhere both can see.

- [x] **5e. The page carries no permanent chrome** — done 2026-08-15
  Mutalib's observation: settings and "read something else" sitting at the foot of the
  page does not survive the app filling out — you would scroll to the end of the Qur'an to
  reach settings, and task 6 adds the most important control of all.
  Tap the page and a slim bar appears with the ways out; tap again and it goes. The bar
  uses words rather than icons: three one-off actions, and a row of little glyphs over a
  Qur'an page would need explaining.
  *Done when:* tapping shows and hides the bar, and the page carries nothing otherwise.
  *Evidence:* tap verified on the phone — the bar appears with "Read something else" and
  "Settings". **Hiding on the second tap is not yet verified**; the phone was picked up
  mid-test. No crash: process alive, no FATAL lines.
  *Revisit at task 6:* the bar uses `surfaceRaised`, which in light mode is the same sage
  as `done`. Once sage means "you have read today", a sage chrome bar may muddy it.

  *Follow-up, same day:* Mutalib asked whether it should be a hamburger icon. It would
  announce itself, which a clean page cannot — but a permanent icon is a mark parked on
  the Qur'an forever, which is the one thing the direction refuses, and ☰ promises a menu
  of destinations this app does not have. Settled on teaching the gesture instead: the bar
  is **shown on the very first run and withdraws after 3.5 seconds**, once ever, stored in
  `hasSeenChrome`. The real weakness he had spotted was not words-versus-icon, it was that
  the tap was undiscoverable.

- [x] **5f. Walked Milestone 1 on the phone** — done 2026-08-16
  Five of six confirmed on the device, one bug found and fixed, one item not testable.
  - ✅ **settings survive a force-stop** — position, theme and the chrome flag all came
    back (`position_unit=878`, `start_surah=36`, `start_ayah=5`, light theme)
  - ✅ **the bar hides on a second tap** — pixel at (300,240) goes sage → paper
  - ✅ **the bar tracks the page** — swiped 440 → 443 and it read "Ya-Sin, Page 443,
    Juz' 23", with "Today's portion" appearing because we were off it
  - ✅ **"Today's portion" jumps** — landed back on 440, "Ya-Sin 4–12, Juz' 22"
  - ✅ **go-to-surah jumps** — Al-Fatihah, page 1, everything pale since it is not today's
  - ⬜ **the first-run bar** — not testable from outside. `run-as` cannot write
    `shared_prefs` on this device (permission denied), and clearing app data to force it
    would wipe the stored position. Verified by code only. Test it the next time the app
    is installed fresh, which task 16 does anyway.

  **Bug found and fixed:** the chrome bar said **"Fatir"** while the page said
  "Ya-Sin 4–12". It was reading `page.surahName`, which is the surah of the page's *first*
  verse — and page 440 opens with Fatir's last ayah. The same bug Mutalib reported in
  setup, reappearing somewhere new because the rule lived inside one composable instead of
  a shared function. It is `surahLabelFor()` now, used by both, and the "what is lit" rule
  was hoisted alongside it so the bar and the page cannot disagree again.

## Milestone 2 — The loop

- [x] **6. Mark done — record or tap** — built 2026-08-16, tap path verified on the phone
  At the foot of the page, where you arrive when you have actually finished reading.
  "Recite it out loud" is the primary; "I read it" is the quieter second route, and every
  tap is logged as a tap. Sacred Rule 6.
  *Storage:* `days.json` in app-private storage, one short row per day — plain JSON so it
  can be read with your own eyes, and so task 19 has something to export. Audio is AAC in
  MP4, mono 64 kbps, which is what WhatsApp takes as a voice note without re-encoding
  (task 11). Missed days are simply absent; there is no row saying you failed.
  *Evidence:* tapping "I read it" wrote
  `{date, method: TAPPED, startUnit: 880, units: 2}` and advanced the position 880 → 882.
  Undo removed the row and put the position back to 880 exactly.
  *A design bug this found:* today's portion was computed from the live position, so
  marking done instantly rewrote what today *was* — the page you had just read went pale
  and the confirmation vanished. A finished day now records what it covered, and the
  screen shows that until tomorrow.
  *A zero-byte recording is not a recitation.* `MediaRecorder` throws if stopped within a
  moment of starting and leaves an empty file; the app deletes it and says nothing was
  saved, rather than logging a recitation that does not exist.
  *Recording verified 2026-08-16, by Mutalib reciting into it.* `rec update src:MIC` at
  05:43:53 to `rec stop` at 05:44:36 — a 43-second recitation. The file is a real MP4
  (`ftyp mp42 / isom`), 339,986 bytes, which at 64 kbps mono is about 42 seconds: the file
  size and the session length agree, so it captured sound rather than silence. Playback
  started with no errors and the screen showed "Recited today" with "Hear it back".
  *Fixed after watching the audio log:* playback ran as `usage=USAGE_UNKNOWN`, so Android
  was guessing — the volume keys might not reach it and it could come out of the earpiece.
  Now declared as speech played as media.
  *⚠ Storage, measured:* 340 KB for 43 seconds. A page a day for a year is **~118 MB of
  recordings**. That is fine for one person on a Pixel and is not fine forever. Task 19
  (export) should be the moment a retention choice appears — keep the last N, or export
  and clear — rather than letting it grow silently.

- [x] **7. Streak, total days read, and the honest split** — done 2026-08-16
  Two quiet lines under the done control, where you land having finished:
  `7 in a row, 23 days read` / `5 recited, 18 marked as read`.
  *Sacred Rule 4:* the streak never appears alone, and the total never resets.
  *Sacred Rule 6:* recited and marked are separate and both shown.
  *Sacred Rule 3, and a decision worth keeping:* **a streak of nought is not announced.**
  Someone who missed yesterday does not need a zero held up to them; they need to see they
  have read on twenty-three days. The line shows the total alone until a run is worth
  naming, and nothing here scolds.
  *Also:* the split is written out — "4 recited, 19 marked" — rather than as "4 of 23".
  A ratio invites you to read it as a score to improve. This is a record, not a target.
  *Evidence:* marked today on the phone and the line read "1 day read / 1 marked as read"
  — streak correctly silent at one, recitation correctly absent. Undo cleared it.
  QA check 9 covers a ten-day history with two gaps: streak 3, total 10, 4 recited,
  6 marked, and the split always accounting for every day. It also asserts a day marked
  and later recited counts once as recited, and that a tap can never undo a recitation.

- [x] **8. Prayer-time nudge timing** — done 2026-08-16, verified on the phone
  Computed on the device, not fetched: no API, no key, no network, works in airplane
  mode a year from now. The standard PrayTimes.org solar geometry, ~40 lines.
  *Verified against a known source three ways:*
  1. **Aladhan API (Muslim World League)** for Accra and Kumasi, across both solstices
     and today — **all 24 values matched to the minute, zero error.** QA checks 10–11
     assert them exactly rather than with a tolerance.
  2. **On the phone:** `next nudge 2026-08-16T18:50Z[Africa/Accra] (inexact, LOCATION)`,
     and `dumpsys alarm` shows `origWhen=2026-08-16 18:50:00.000` against
     `*walarm*:com.mosman.wird/.nudge.NudgeReceiver`. Aladhan at the phone's own stored
     fix gives Maghrib 18:20, so +30 = 18:50. They agree.
  3. **Muslim Pro, already installed on the same phone**, has adhan alarms at 12:11,
     15:25 and 19:28 — our Dhuhr, Asr and Isha for that location, exactly. Its Maghrib
     alarm is 18:23 against our 18:20; most likely its own few-minute delay on the
     Maghrib adhan, which is a common convention, but it is a 3-minute gap and is
     recorded rather than explained away.

  **Mutalib's two decisions, both asked rather than assumed:**
  - Default is 30 minutes after Maghrib, **and it can be moved** — any of the five
    prayers with an offset, a fixed hour, or off entirely. This makes the anchor
    user-facing, which brushes against § 5; PROFILE.md now records the refinement.
  - **Coarse location** over a timezone lookup. *This turned out to be the right call
    and the evidence is unusually direct:* his phone's real fix is **6.68, −1.58 —
    Kumasi, not Accra.** The timezone path computed 18:44; the real fix computed 18:50.
    Six minutes, on the very first test, exactly the case he chose it for.

  *Measured, and why the default anchor is the safe one:* Maghrib came out at **18:14 in
  all five published calculation methods** while Fajr ranged over nineteen minutes.
  Maghrib is sunset, and sunset is astronomy rather than convention — so the one setting
  nobody will ever open is nearly unable to do harm. A QA check asserts this.

  **The thing this task actually found: nothing had been scheduling a nudge at all.**
  Task 3 proved the alarm plumbing with a temporary "fire in 15 seconds" button, and
  that button was removed in the task 4/5 rewrite. From then until now the machinery was
  real and idle. `NudgeReceiver` was also still hardcoded to page 453, so it would have
  announced the wrong portion to anyone it reached. Both fixed.

  *Also verified on the phone:* switching the reminder Off logs
  `reminder is off, nothing scheduled` and leaves **no pending alarm**; switching it back
  restores `18:50`. The settings screen prints no prayer time at any point, and a QA
  check asserts no label can leak one.

  *⬜ Not yet proven on the phone:* the nudge **firing** with the new receiver code. It
  is set for 18:50 tonight. Task 3 proved alarm → notification → tap, but the three new
  things in the receiver — re-arming tomorrow, staying quiet on a day already read, and
  reading the real position — have only been proven by unit test and by reading the log.
  Confirm the next time it fires.

  *Recorded, not explained:* `BootReceiver` logs `boot completed` on the first launch
  after a force-stop, with no reboot involved (uptime 1h35m). Isolated by experiment — it
  does **not** happen when the app is merely brought to the foreground. Almost certainly
  the queued `BOOT_COMPLETED` being delivered when the app leaves Android's "stopped"
  state. Harmless: arming is idempotent, and `dumpsys` shows exactly one alarm.

- [x] **9. Abu Bakr al-Shatri audio for today's portion** — done 2026-08-16, verified in
  airplane mode, one real bug found and fixed
  Reciter id 4. Per-ayah MP3s fetched on demand, cached, playable offline afterwards.
  *Done when:* today's portion plays with the phone in airplane mode, after one online
  fetch.

  *Verified against the real API before a line was written:* reciter 4 **is** Shatri —
  `/recitations/4/by_page/442` returns `Shatri/mp3/036028.mp3`. Both
  `verses.quran.com` and `audio.qurancdn.com` serve it, and BunnyCDN answered from a
  Ghanaian edge (`CDN-RequestCountryCode: GH`).

  **⚠ The pagination trap:** that endpoint defaults to `per_page=10`. Page 442 has 13
  ayahs, so the obvious call silently returns a portion three ayahs short. `per_page=50`
  fixes it. In the end the app does not use this endpoint at all — it derives verse keys
  from the cached page layout it already has, which is both free and offline.

  *⚠ Measured, and the reason this needed a decision:* one page of audio is
  **2,376,926 B at 128 kbps** — Ya-Sin 28–40, thirteen ayahs, averaging 178 KB each.
  The mushaf page's own font is 154 KB, so **audio is ~15× the cost of the page it reads**,
  every day. A page a day is ~68 MB/month at 128 kbps against ~34 MB at 64. Mutalib chose
  "let me pick in settings" over either default, so both ship and the light one is default.
  32 kbps does not exist for this reciter (404), so 64 is genuinely the floor.

  *A bug I built by hand and should have spotted a line sooner:* my first size measurement
  used `036` + `28` instead of `036028`, and every one of the thirteen files came back
  **exactly 678 bytes** — an HTML error page served with an HTTP 200. Identical byte counts
  across thirteen different ayahs is the tell, and I read past it. This is the same trap
  PROFILE.md § 10 already records for the fonts, hit again in a new place, which is why
  `MIN_PLAUSIBLE_BYTES` now guards the audio too and why QA check 12 asserts the padding.

  *Decisions worth keeping:*
  - **Listening marks nothing.** The control sits beside "Recite it out loud" and
    "I read it" but does not complete the day. Hearing someone else recite is not reading,
    and Sacred Rule 6 turns on the app never blurring that. **A `LISTENED` method is
    Mutalib's call, not a side effect of building playback** — see the open question below.
  - **In both places, per his answer:** the chrome bar (one tap from anywhere, because
    "the lazy-day escape hatch" cannot live at the bottom of the thing you are avoiding)
    and the foot. The bar's icon becomes a stop while it plays.
  - **Only the lit ayahs.** A half-page portion fetches and recites half a page. The verse
    list is derived from the same "what is lit" rule the display uses, not reimplemented —
    task 5f's bug was one rule living in two places.
  - **`layoutOnly()`** was added so audio can find verse keys without pulling a 154 KB
    glyph font for a page that may not be on screen.
  - **50 MB LRU cache.** A page a day at 128 kbps is 830 MB a year if nothing is deleted.
    Whatever the current portion needs is never evicted.
  - Plain chained `MediaPlayer`, no ExoPlayer — several MB of dependency to play files in
    order, on an app where size is a first-class concern.

  **✅ Airplane-mode test PASSED, and it was not a formality — it caught a shipping bug.**

  *First run, online:* `portion cached: 13 ayahs, 1170 KB` — 1.17 MB for Ya-Sin 28–40 at
  64 kbps, against the ~1.1 MB predicted. All thirteen files landed as `036028.mp3` …
  `036040.mp3`, correct six-digit padding. `dumpsys audio` confirmed a real player:
  `state:started`, `usage=USAGE_MEDIA content=CONTENT_TYPE_SPEECH`, 22050 Hz.

  *Then airplane mode on*, `ping verses.quran.com` → `unknown host`, app relaunched cold:
  **it played from disk, and kept playing.** The bar read "Playing Ya-Sin 29 · 2 of 13".

  ### ⚠ The bug the test found — listening worked exactly once per launch

  Between those two runs it **failed**, saying *"Couldn't get the recitation. Check your
  connection."* — with all thirteen files sitting on disk. It had nothing to do with the
  network; the error message simply guessed.

  `stop()` set a `cancelled` boolean true. `play()` set it back to false. But `play()`
  runs *after* `ensureCached`, and `ensureCached` bailed on `cancelled` at its first line.
  **So the second listen of any session died before touching the disk, and blamed the
  network for it.** Online it would have looked identical, and "check your connection" is
  exactly the message someone on Ghanaian mobile data would believe.

  Fixed with a monotonic `run` token instead of a boolean: `stop()` bumps it, and any
  fetch or playback chain holding an older token stands down. That also fixes a second
  latent bug — two quick taps now supersede each other cleanly rather than both driving
  the same player. A deliberate stop mid-download no longer reports a failure either.

  *Re-verified after the fix, still in airplane mode:* stop → 0 players; listen again →
  1 player and a fresh `portion cached` line; and a third cycle for good measure. All
  three passed where the first would have failed.

  **Honest note on the QA suite:** this bug did not get a regression test, which breaks
  this project's usual rule. It lives in ordering between `MediaPlayer`, a coroutine and
  Android state, and testing it on the JVM would mean extracting the token into a class
  invented for the test. Check 12 covers the URL padding — the *other* silent failure —
  and this one is instead written down here and in the code comment on `run`. **It was
  found by running the thing on a real phone, which is the only reason the plan asks for
  that at all.**

  *⬜ Still unproven:* the 50 MB LRU prune, which needs ~20 pages of history to trigger.

- [x] **10. Home screen widget** — built 2026-08-18, rendering verified, overnight pending
  Jetpack Glance. Today's portion and done/not-done. The nudge that cannot be swiped away
  or killed.

  *Verified on the emulator, placed by hand:* the widget shows `TODAY'S PORTION` /
  `Al-Fatihah` / `One page · page 1` / `Not yet marked`, and `dumpsys appwidget` reports a
  bound instance with real `RemoteViews` rather than the loading layout.

  *Decisions worth keeping:*
  - **It cannot mark a day.** A button on a home screen is where "done" becomes a reflex
    rather than a recitation. Tapping opens the app. Sacred Rule 6.
  - **It is pushed, never polled.** `updatePeriodMillis` is 0 — the platform clamps it to
    thirty minutes and ignores it while dozing, so it would be right only sometimes and
    spend battery being wrong. `refreshWidget()` fires from the four paths that change
    what it shows. ⚠ Undo needed wiring separately and would have left the widget claiming
    a day was done after it was undone.
  - **264 KB**, measured by building the same tree with and without Glance (17.27 → 17.52
    MB debug). Less than one page of recitation audio.

  ⬜ **Still open:** *"still shows the right thing the next morning."* That needs a night to
  pass, and it needs checking on a real Transsion phone rather than the emulator — the
  whole point of this task is surviving the OEMs that kill background work.

## Milestone 3 — Levers and honesty

- [x] ~~**11. Share a recording to WhatsApp**~~ — **DELETED 2026-08-18, at his word**
  Cut rather than built. § 5a reasoned it out first and he confirmed it: the task's whole
  premise was that *sending* a recitation to someone was the accountability, and he decided he
  will not be sending recordings to anyone. A generic share button would have been the feature
  without the reason for it.
  *Kept, because it is the useful half:* the share plumbing exists anyway — a FileProvider
  scoped to the export folder, built for task 19.

- [~] **12. ⚠ Deep links** — half-resolved 2026-08-18. One proven and built, one undetermined and refused
  *Risky because:* the target apps may accept no deep link at all. That risk was real and it
  split the task in two.

  **Quran for Android — PROVEN, built.** Its manifest exports `QuranForwarderActivity` with
  `<data android:scheme="quran"/>`, and that activity splits the URI on `/` and takes the first
  numeric segment as sura, the second as ayah. So `quran://18/10` opens Al-Kahf 10. Read from
  their GPL source rather than guessed, and check 37 asserts the shape Wird builds.

  **Tarteel — UNDETERMINED, deliberately not built.** Closed source, no published scheme,
  nothing in its listing or docs. **That is not proof it has none**, so this is not the "proof
  they cannot" the task allows for — it is honestly unknown. A button that merely launched
  Tarteel's home screen would look like a deep link without being one, which is worse than no
  button. Settling it needs the APK and `aapt dump xmltree`.

  **The action only exists when the app does.** Wird asks the package manager, and shows no
  button when the target is missing — rather than one that opens the Play Store, which is an
  advert wearing a feature's clothes. On API 30+ that needs a `<queries>` entry, scoped to the
  one scheme rather than `QUERY_ALL_PACKAGES`.

  ⬜ **Verified only in the negative, now twice.** On the emulator, with nothing handling
  `quran://`, the toolbar correctly shows Bookmark, Play, Share, Close and no more. **2026-08-19:
  Mutalib asked for the same action on Home's portion card and it was built** — three facts
  agreeing that its absence is correct (package not installed, `resolve-activity` finds no
  activity, button absent from the view tree). **Nobody has still seen the button appear and open
  an ayah**, because that app cannot be installed here. First real phone that has it settles it.

- [x] ~~**13. ⚠ Plain-words setup**~~ — **FOLDED INTO TASK 22, done 2026-08-18**
  Not cut and not skipped: built once, in the place it belongs. This task and task 22 had the
  same done-when — *five messy sentences produce the correct schedule* — and this task's own
  example sentence is the one check 41 now runs. Two parsers reading the same English would
  have been two places to disagree about someone's plan.
  *The risk it flagged was real and the answer is written down:* a dropdown does do most of
  this, so **the setup screen keeps its pickers**. What typing adds is the part a picker cannot
  do at all, which is changing three things at once, six weeks later, without hunting for the
  screen they live on. See PROFILE.md § 5ad.

- [~] **14. ⚠ On-device recitation checker** — built and running 2026-08-20; **UNMEASURED, so not ticked**
  whisper.cpp Android AAR + `tarteel-ai/whisper-base-ar-quran` (Apache-2.0). **Batch,
  not streaming.** Compare against the assigned passage, which is known — this is
  matching against a given answer, not open transcription, so it tolerates a high error
  rate. Runs over recordings already captured.
  *Risky because:* accuracy on his voice, on his phone, in his room, is unmeasured. The
  published 5.75% WER is on clean professional recitation.
  *Done when:* it reliably separates a real recitation from silence and from unrelated
  speech, on Mutalib's own recordings, with the measured numbers written down. If it
  cannot, that is a real result — record it and drop the feature.

  **✅ The cheap half shipped first** (2026-08-19): `judgeRecitation` separates a recitation
  from a silent room, a pocket and a false start using loudness alone — no model, no download.
  Checks 50–52. ⚠ Its thresholds are guesses until measured on his voice.

  **✅ The expensive half is built and runs on his phone** (2026-08-20). whisper.cpp v1.9.2 as a
  pinned submodule, upstream's `jni.c` unmodified, **engine costs 1.2 MB** (APK 17.7 → 18.9).
  Proof from his Pixel: `native=true`, `NEON=1 ARM_FMA=1 OPENMP=1 REPACK=1`. Both Apache-2.0
  Tarteel models offered as optional downloads (42 MB / 78 MB) through the foreground service,
  and the reader picks which is in use. Finishing a recitation now transcribes and logs it.

  **✅ First real numbers, 2026-08-20:** 7s of audio transcribed in **0.847s** on his Pixel with
  TINY — **8.3x faster than real time** — and correct: he recited Ya-Sin, it returned **يس**. A
  72x speedup came from `add_compile_options(-O3)`, because ggml's hot code lives in a separate
  `ggml-cpu` target that a flag on `whisper` never reached. PROFILE.md § 5ba.

  **✅ Arabic reference text bundled and LCS comparison built** (2026-08-20/21, commits `c3ff362` and `33654a7`):
  All 604 pages bundled as clean Tanzil imlaei text in `app/src/main/assets/arabic/*.json` via `ArabicText.kt`
  (6,236 verses verified, Sacred Rule 2). `checkRecitation` compares heard words against the expected page words
  using a longest-common-subsequence diff walk with `foldArabic`. A real false-positive bug (pause marks on page 300
  falsely marked 4 ayahs of Al-Kahf) was found and fixed by stripping pause marks. Amber ayah highlighting
  (`versesToReview`) is wired to `MushafPageView`, with a 70% coverage floor (`MIN_COVERAGE = 0.70f`) below
  which it stays quiet and marks nothing. 63 QA checks pass.

  **✅ Whisper and ggml native logging routed to logcat** (2026-09-07):
  `wird_whisper.c` routes internal whisper/ggml errors and diagnostics directly to Android logcat via
  `whisper_log_set` and `ggml_log_set` under the `WirdNative` tag, enabling immediate diagnosis of model loading
  issues (including why BASE refused to initialize).

  ⬜ **STILL NOT TICKED — ON-DEVICE MEASUREMENT PENDING.**
  While the pipeline is end-to-end complete and verified by unit tests, a real recitation over a full portion
  (e.g., page 439/440) still needs to be run through the checker on Mutalib's own voice and phone to measure:
  1. Real-time factor (seconds of audio vs transcription latency on full portion)
  2. Word accuracy / coverage and false error rate
  3. Logcat diagnostic on BASE model loading via `WirdNative`

  **Next step:** Install the updated build on the Pixel, run "Check it" on a recitation, and record the measured numbers.

- [x] **25. Home, rebuilt from his own comps** — done 2026-08-19, verified on the emulator
  He supplied four mockups and named what to take from each: image 1 the base, image 2 the
  greeting and the check-in, image 3 the streak block, the rest from image 1. The comps' bottom
  tab bar and top-right gear were refused by him, matching § 5t.
  *Also his, in the same pass:* WhatsApp-style chat bubbles, the mushaf-on-a-rihāl illustration,
  and an **Open in Quran for Android** button on the portion card (task 12's deep link).
  *Colour, asked before building and then reversed by him:* the comps are green, the palette is
  pinned teal. He chose *"teal stays, warm cream ground, tinted tiles"*, saw it, and preferred
  the white ground. Ground reverted; **the tiles and bubbles stayed** and were re-measured
  against the white card. PROFILE.md § 6e and § 5ae.
  *Evidence:* `check: PASS`, design-studio gate 0 block, humanizer 100.0/85 on the strings,
  every new colour pair measured before it was used. Three defects found by running it: a
  medallion that read as a clock face, a dangling comma over an empty line when no name is set,
  and a screenshot that disagreed with the accessibility tree.
  *Second pass, 2026-08-19, all four at his word:* the mushaf illustration redrawn (the first
  was straight lines, and a book has no straight lines); **the reading page given its own
  light/dark, split from the chrome** — his instruction, after Quran for Android: *"home and
  menu are in dark mode but page is light mode"*, verified on the emulator in one run; and the
  **streak and This week sections, which had never been visible to anyone** because both were
  gated on having data. They now render on day zero. PROFILE.md § 5af, § 5ag, § 5ah.

  ⬜ **Not yet seen by him on his own phone**, and the Quran button cannot appear here at all.
  ⬜ **The streak's flame glyph still reads as a water droplet** at 16dp. Said rather than
  hidden; a larger mark, a different one, or none is his call.

## Milestone 4 — Testers

- [~] **15. ⚠ OEM notification survival** — built 2026-09-17, on-device Transsion test pending
  Detect the manufacturer. Show the exact steps for that phone (Tecno/Infinix: Battery
  Lab → disable power saving for this app; Phone Master → auto-start management). Add a
  "did the last nudge arrive?" self-check.
  *Risky because:* it can only be proven on a real Transsion phone.
  *Done when:* verified on an actual Tecno or Infinix, not on the Pixel.

  **✅ The buildable half shipped (2026-09-17):**
  - `OemAdvice.kt` enhanced with exact vendor detection for Transsion (Tecno HiOS, Infinix XOS,
    itel), Samsung (One UI), Xiaomi (MIUI/HyperOS), Huawei (EMUI), Oppo/Realme/Vivo, and stock Android.
    Provides step-by-step guidance tailored to each brand with direct intent launching of vendor auto-start
    activities with fallback to system battery saver settings.
  - `NudgeDiagnostic.kt` built: evaluates notification permissions, exact alarms, battery optimization
    status, scheduled alarm state, and answers "did the last reminder arrive?" by comparing `lastArmedFor`
    vs `lastNudgeFiredAt` (>20m grace window detects killed alarms).
  - Tactile clay UI added to Settings under Reminders: dynamic status badge, vendor guidance modal,
    and self-check report with actionable fix shortcuts.
  - QA checks 64 and 65 pass; lint 0 errors.

  ⬜ **UNPROVEN ON REAL TRANSSION PHONE — PENDING TESTER DEVICE.**

- [ ] **16. Firebase App Distribution**
  Spark plan, free. Build goes out to a tester group. `DRY_RUN=1` builds locally without
  uploading. **Also: the app icon** — deferred from task 1 because it belongs to the
  design-studio pass, and the `MissingApplicationIcon` lint check is disabled until it
  exists. Re-enable that check once it does.
  *Done when:* someone who is not Mutalib installs it and opens it, and the launcher
  shows a real icon.

- [ ] **17. Vercel landing page + feedback**
  One page: what it is, install link, feedback form. `noindex` until launch.
  *Done when:* the live URL loads and a submitted message reaches Mutalib.

- [ ] **18. In-app feedback and the privacy line**
  Feedback goes to WhatsApp. A plain sentence stating that recordings never leave the
  phone — because testers are now trusting him with that.
  *Done when:* a message reaches him from a tester's phone and the privacy line is
  visible in-app.

- [x] **19. Export everything** — done 2026-08-18, verified by opening the file on this computer
  Everything Wird knows about you in one zip: `days.json`, `chat.json`, `bookmarks.json`,
  every recording, and a README that explains each of them.

  *Evidence:* built on the emulator, pulled off with `adb exec-out run-as ... cat`, and
  opened here — 4 entries, 81,482 bytes, README first. That is the done-when, done on a
  real computer rather than asserted.

  *A zip rather than the loose folder this task asked for.* A folder is several things to
  move and easy to half-copy; a zip is one item that keeps its structure and opens as a
  folder anywhere. The intent — *get my data off this device in a form I can read* — is
  better served by the single file.

  *The retention choice PROFILE.md § 5a asked for, in the same place:* **Delete recordings**,
  offered only when there are any. ⚠ **Verified that it does not falsify anything** — after
  deleting, `days.json` still reads `"method":"RECITED"`. The log stores *how* a day was
  finished separately from the audio, so removing the files removes recordings, not the fact
  that you recited. Sacred Rule 6 holds.

  *Sacred Rule 1 is why this exists at all.* A promise that your data never leaves the phone
  is only kind if you can get it off when you want to; otherwise private and trapped are the
  same thing. The FileProvider is scoped to exactly one directory — the export folder in the
  cache — so nothing else can ever be handed to another app.

## Milestone 4b — The companion

Approved 2026-08-16 (PROFILE.md § 5b). **The order here is the whole point:** the cheapest
version tests the assumption first, and nothing expensive gets built until it holds.

- [~] **21. ⚠ The commitment loop** — mechanism built and verified 2026-08-18; the evidence it exists for does not exist yet
  The nudge stops being a statement and becomes a question. Reply from the notification:
  *After Isha · In an hour · Tonight · Not today.* Each is a button, not typing. Picking one
  re-arms the reminder for that moment — **task 8's scheduler already turns "after Isha"
  into a real alarm**, so this is mostly wiring. If the named time passes unmarked, it asks
  once more, gently, and then stops.
  *Risky because:* this is the experiment. If naming a time to the app changes nothing, the
  chat and the model would not have saved it, and this cost a day instead of three weeks.
  *Every commitment is logged* — what was promised, whether it was kept — because task 24
  cannot measure what was not recorded.
  *Done when:* a week of real use exists, with the kept-vs-broken numbers written down.
  **Not "when it works" — when there is evidence about whether it helps.**

  **⚠ Which is why this is not ticked.** The machinery works; the task is not about machinery.
  It stays open until there are numbers.

  *Built and verified on the emulator 2026-08-18:* the nudge posts with `actions=3` under the
  title **"Reading today?"**; tapping **After Isha** logged `committed to after Isha, re-armed`,
  moved `nudge_schedule` to `PRAYER ISHA 30`, stored the commitment, wrote both turns to
  `chat.json`, and cleared the notification.

  *Three replies, not four.* PLAN listed "Tonight" and it is deliberately absent: `CompanionBrain`
  does not understand the bare word, so the button would have silently done nothing; it is
  redundant with "After Isha" given Wird's after-Maghrib default; and Android shows three
  actions. **Check 35 now asserts every offered reply parses to something**, which is the guard
  against that whole class of dead button.

  *The buttons send phrases, not codes* — the same words a person would type — so the
  notification and the chat can never mean different things, and a reply from the lock screen
  lands in the same conversation.

- [x] **22. Plain words instead of buttons** — done 2026-08-18, verified on the emulator
  Typing replaces the four buttons for everything that is not tonight's promise: how much you
  read, which days are lighter, when the reminder comes, and being away. **Task 13 is folded in
  and closed by this** — same done-when, same parser, one feature instead of two.
  **Stack, his words 2026-08-18:** *"for now lets use the free private offline then later we use
  gemini."* Hand-written rules ship; the model waits for task 24's verdict. ⚠ Gemini
  specifically, not Claude and not an on-device model.

  *Done when — met.* Check 40 is the five sentences, written as five different sentences rather
  than five phrasings of one: `one page a day` → two half-page units · `half on fridays` → that
  weekday only · `im travelling till sunday` → quiet, returning **on** Sunday · `move my
  reminder to 9 from now on` → the routine, not tonight · `im back` → ends it early. Checks
  41–46 cover the rest: three instructions in one sentence, the unread clause being quoted back,
  the one-off rule, the away arithmetic, the refusals to guess, and Sacred Rule 3 across every
  new reply. **46 checks, `check: PASS`.**

  *Evidence on a real Android runtime, not just the JVM.* Typed into the companion on the
  emulator: `a page and a half a day, half on fridays` moved `default_units` 2→3 and wrote
  `units_FRIDAY 1` in one message; `im travelling till sunday` stored
  `away_period 2026-08-18..2026-08-22` and armed the alarm for **2026-08-23T20:00**, the Sunday
  he named; `im back` cleared it and pulled the alarm back to the 19th. Read out of
  `shared_prefs` and `chat.json` with `run-as`, not asserted.

  ⚠ **It fixed a defect while it was in there.** A commitment used to overwrite the *daily*
  reminder, so one "in an hour" made that time permanent, silently. His own install had
  `nudge_schedule PRAYER ISHA 30` for exactly that reason. The schedule now rides inside the
  commitment and expires with the day. PROFILE.md § 5ad.

  ⚠ **What rules still cannot do, so it is not promised:** § 5h's tafsir, translation-explaining
  and daily reflection. Asking the chat what a surah means gets a shrug until Gemini lands, and
  **Sacred Rule 2 binds when it does** — the ayah, translation and tafsir are FETCHED and
  attributed, never generated. Also not done, deliberately: "a juz a day" (juz boundaries are
  not every 20 pages, so honouring it would be an approximation dressed as precision),
  "weekends" as one word, and per-weekday reminder times.

- [ ] **23. ⚠ WhatsApp, for Mutalib only**
  Meta Cloud API, free test number, his number as a verified recipient. **Never in the
  tester build** — a build flag, not a preference, so it cannot ship by accident.
  *Risky because:* it hands Wird a backend and a second source of truth. The phone stays
  authoritative; WhatsApp proposes, the phone applies.
  *Done when:* he replies "after Isha" in WhatsApp and the phone's reminder actually moves —
  **and the tester build is verified to contain none of it.**

- [ ] **24. Does it actually work?**
  Thirty days against PROFILE.md § 5b: **did the procrastination half move?** Baseline is
  ~8 of 14 missed days to procrastination. Commitments made vs kept, and missed days with
  and without a commitment.
  *Done when:* real numbers exist and there is an honest verdict. **"It was theatre, cut it"
  is a legitimate outcome** and the feature gets removed rather than kept because it was
  work.

**✅ ANSWERED 2026-08-18: rules now, Gemini later.** See task 22 above. The remaining open
question is not *which engine* but *whether the companion earns one at all* — task 24.

## Milestone 5 — The verdict

- [ ] **20. Thirty-day measurement**
  Against PROFILE.md § 3: 20 of 30 days, at least 10 recited; and how many testers are
  still marking days at day 14.
  *Done when:* real numbers exist and there is an honest written verdict on whether to
  keep going, change the mechanic, or stop. "Stop" is a legitimate outcome.

---

## Not started, deliberately

Everything under NOT IN V1 in PROFILE.md. In particular the full mushaf reader with
live correction — parked, not killed, and not before task 20.
