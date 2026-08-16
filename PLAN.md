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

- [ ] **5f. Walk Milestone 1 on the phone, once, deliberately**
  Everything below is built and `check`-green, but a batch of it has never been eyeballed
  on the device — his phone was in use each time, and the device rule says leave it.
  Worth one deliberate pass rather than trusting that code which compiles works.
  - settings survive a force-stop (theme, daily amount, lighter days, position)
  - the chrome bar hides on a second tap
  - the bar shows the right surah, page and juz, and **updates as you swipe** (fixed, unseen)
  - "Today's portion" actually jumps back (fixed, unseen)
  - go-to-surah actually jumps (fixed, unseen)
  - the first-run bar appears and withdraws after 3.5s
  *Done when:* each line above is confirmed, or a bug is written down.

## Milestone 2 — The loop

- [ ] **6. Mark done — record or tap**
  Both paths. Audio to app-private storage. Which method was used is logged on the
  `DayEntry`.
  *Done when:* both paths work and a recording survives a reboot and an app kill.

- [ ] **7. Streak, total days read, and the honest split**
  Two numbers side by side, never one alone. "22 marked, 4 recited" visible without
  digging.
  *Done when:* a simulated 10-day history with gaps produces correct numbers in the QA
  suite, and the split renders on screen.

- [ ] **8. Prayer-time nudge timing**
  Compute prayer times for the user's location, offset the nudge from one of them
  instead of using a fixed hour. Prayer times stay internal — not a user-facing feature.
  *Done when:* the nudge fires at the right offset from Maghrib for Accra, checked
  against a known prayer-time source.

- [ ] **9. Abu Bakr al-Shatri audio for today's portion**
  Reciter id 4. Per-ayah MP3s fetched on demand, cached, playable offline afterwards.
  *Done when:* today's portion plays with the phone in airplane mode, after one online
  fetch.

- [ ] **10. Home screen widget**
  Jetpack Glance. Today's portion and done/not-done. Updates daily. The nudge that
  cannot be swiped away or killed.
  *Done when:* the widget sits on the Pixel home screen showing the correct portion, and
  still shows the right thing the next morning.

## Milestone 3 — Levers and honesty

- [ ] **11. Share a recording to WhatsApp**
  One tap, Android share sheet, nobody else installs anything.
  *Done when:* a voice note lands in a real WhatsApp chat from the phone.

- [ ] **12. ⚠ Deep links out to Quran for Android and Tarteel**
  *Risky because:* the target apps may accept no deep link at all.
  *Done when:* the buttons open the right ayah in the right app — **or** we have proof
  they cannot, the buttons are deleted, and PROFILE.md § 11 records why.

- [ ] **13. ⚠ Plain-words setup**
  "One page a day, half on Fridays, I'm travelling next week" → a real schedule.
  *Risky because:* it needs a model, and a dropdown does most of this. If it costs more
  than it earns, cut it and say so.
  *Done when:* five messy sentences from Mutalib produce the correct schedule.

- [ ] **14. ⚠ On-device recitation checker**
  whisper.cpp Android AAR + `tarteel-ai/whisper-base-ar-quran` (Apache-2.0). **Batch,
  not streaming.** Compare against the assigned passage, which is known — this is
  matching against a given answer, not open transcription, so it tolerates a high error
  rate. Runs over recordings already captured.
  *Risky because:* accuracy on his voice, on his phone, in his room, is unmeasured. The
  published 5.75% WER is on clean professional recitation.
  *Done when:* it reliably separates a real recitation from silence and from unrelated
  speech, on Mutalib's own recordings, with the measured numbers written down. If it
  cannot, that is a real result — record it and drop the feature.

## Milestone 4 — Testers

- [ ] **15. ⚠ OEM notification survival**
  Detect the manufacturer. Show the exact steps for that phone (Tecno/Infinix: Battery
  Lab → disable power saving for this app; Phone Master → auto-start management). Add a
  "did the last nudge arrive?" self-check.
  *Risky because:* it can only be proven on a real Transsion phone.
  *Done when:* verified on an actual Tecno or Infinix, not on the Pixel.

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

- [ ] **19. Export everything**
  All recordings plus the day log, into one folder the user can copy off the phone.
  *Done when:* the folder appears and opens on a computer.

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
