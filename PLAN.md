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

- [ ] **2. App shell on the phone**
  One screen. Hardcoded portion text. Installs and opens on the Pixel 6 Pro over USB.
  *Done when:* the screen is visible on the actual phone.

- [ ] **3. A notification fires and opens that screen** ← the "it's live" gate
  Scheduled local notification, tapping it deep-links to the portion screen.
  Request `SCHEDULE_EXACT_ALARM` with a screen that explains why, and detect when it has
  been refused.
  *Done when:* a real notification arrives on the Pixel and the tap lands on the right
  screen. **No feature work happens before this passes.**

## Milestone 1 — The page

- [ ] **4. ⚠ Render the real mushaf page**
  QCF v2 glyph fonts, one per page, fetched on demand and cached. Line layout from
  `api.quran.com/api/v4/verses/by_page/{n}?words=true`, words grouped by
  `page-{page}-line-{line}`, rendered right-to-left. Today's portion at full opacity,
  the rest of the page dimmed.
  *Risky because:* glyph codes live in a font private-use area and Compose rendering is
  unproven. If Compose cannot do it, try a WebView, and if that fails, fall back to
  plain Uthmani text and record the failure in PROFILE.md § 11.
  *Also decides:* TTF at ~372 KB/page, or woff2 at ~80 KB plus an on-device decoder.
  Measure both over a real 3G connection — this is ~11 MB/month versus ~2.4 MB/month for
  a one-page-a-day reader, which is a real cost to a Ghanaian student.
  *Watch out:* a wrong font URL in that repo returns a 14-byte body containing
  "404: Not Found", not an HTTP error. Check byte counts, not just success.
  *Done when:* page 453 renders on the Pixel and matches the printed mushaf, with ayahs
  1–8 lit and 9–16 dimmed.

- [ ] **5. Position, target, and today's portion**
  Store where you are. Store pages per day with per-weekday overrides. Compute today's
  assignment. Handle surah boundaries and the end of the mushaf wrapping to page 1.
  *Done when:* setting page 453 with a one-page target yields the right range, verified
  against the physical mushaf, and the QA suite covers a surah boundary.

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
