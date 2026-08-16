# Wird — PROFILE.md

**Canonical spec.** Every session reads this file first. Nothing in `docs/` or `PLAN.md`
may contradict it; when they disagree, this file wins and the other gets fixed.

Created 2026-08-14 after a Phase 0 interrogation. Owner: Mutalib Osman.

---

## 1. WHAT

Wird is a daily Qur'an portion coach for Android. It knows which page you are on,
decides what you are reading today, shows that page as it is actually printed in the
Madani mushaf, nudges you at a moment you are likely to be free, and treats "done" as
something you say out loud rather than something you tick.

A *wird* (ورد) is the daily portion of Qur'an a person commits to reading. That is the
whole app.

It is **not** a Qur'an reader. It shows today's page and links out to Quran for Android
and Tarteel for everything else.

## 2. WHO

Mutalib, and later a small circle of testers — Ghanaian students, mostly on Tecno,
Infinix and itel phones.

**What he does today instead:** Quran for Android on the phone, a physical mushaf in
class, and his position held in his head. Nothing reminds him. Nothing checks on him.

**The finding Phase 0 turned on.** Over two weeks he missed roughly 14 days: about 6 to
forgetting, about 8 to procrastination or being carried away. So *forgetting is the
smaller half of the problem* and a reminder alone fixes at best 6 of 14.

The one thing that historically worked: **he used to go and recite to a teacher.** School
stopped that, and the habit went with it. Reciting aloud to someone who expects you is
the mechanic this app is a stand-in for. Everything he named as working — right after a
prayer, a streak, seeing a friend recite — is the same family: pressure from outside his
own willpower.

## 3. SUCCESS METRIC

**Personal:** read on **20 of the next 30 days, at least 10 of them recited aloud**
rather than tapped. Baseline is roughly zero of the last 14.

**Testers:** of the testers who install, **how many are still marking days at day 14.**

Nothing else is a success metric. Not installs, not stars, not screens built.

## 4. V1 SCOPE

1. Set position (surah + ayah, or page) and a target in pages per day
2. One screen that opens straight to today's portion — no dashboard, no home screen
3. **The real mushaf page**, rendered with QCF glyph fonts and line layout from the
   Quran.com API, with today's portion lit and the rest of the page dimmed
4. Abu Bakr al-Shatri recitation audio for today's portion, cached — the lazy-day
   escape hatch, so the ask can drop to "just listen"
5. Nudge timed off prayer times rather than a fixed clock hour
6. Mark done: **record yourself reciting, or tap.** Both allowed, both logged
   separately, and the split shown honestly
7. One-tap share of a recording to WhatsApp — nobody else installs anything
8. Streak **and** total days read, side by side
9. Home screen widget: today's portion, done or not done
10. Deep links out to Quran for Android and Tarteel — or proof they are impossible and
    the buttons are deleted
11. OEM notification survival: detect the manufacturer, walk the user through their
    phone's specific battery settings
12. Firebase App Distribution build; a one-page Vercel site with install link and
    feedback; in-app feedback to WhatsApp

## 5. NOT IN V1

Explicit exclusions. No session builds these "helpfully."

- **Tajweed scoring or pronunciation correction.** That is Tarteel's job and it does it
  better. Link out.
- **Live word-by-word recitation following.** Measured at ~5× slower than real time
  on-device. Not worth it, probably not ever.
- ~~A chatbot of any kind~~ — **REOPENED 2026-08-16 with Mutalib's explicit approval.**
  See § 5b below. The exclusion stood for four months and is being lifted deliberately,
  not drifted past.
- Hifdh / memorisation tracking
- Full Qur'an reader — browse anywhere, bookmarks, search, translations, tafsir
- Prayer times as a user-facing feature (used internally for nudge timing only)
- Qibla, Hadith, Dua library, or anything else that turns this into the bloated
  all-in-one that r/muslimtechnet is tired of
- Accounts, servers, cloud sync, or anyone's data leaving their own phone
- Public Play Store launch
- iOS

**Refined 2026-08-16, with Mutalib's explicit approval, on the prayer-times line above.**
Asked at the start of task 8; he answered "for now lets make it magrib it should be able
to be changed to anytime the user wants."

- **Still refused:** a prayer timetable, a next-prayer countdown, adhan audio, qibla, or
  any screen whose subject is prayer times. Wird is not a prayer app and Muslim Pro is
  already on his phone doing that job well.
- **Allowed:** choosing which prayer the reminder follows, and by how much. The reader
  picks a landmark they already know; **no computed prayer time is ever printed on
  screen**, and a QA check asserts that no label can leak one. The only clock time the
  settings screen will show is the fixed-hour fallback, which is not a prayer time.
- Prayer times remain internal in the sense that matters: they exist to place a
  notification, and nothing in the app reads them for their own sake.

### Recorded for later, deliberately not killed

**A full mushaf reader with Tarteel-style live correction inside Wird.** Mutalib wants
this and said so plainly. It is parked, not deleted. Blocked on: (a) mushaf image and
font licensing, (b) v1 first proving he actually uses this for 30 days. Reopening it is
his call and needs no permission — but it does not happen before the 30-day measurement.

## 5b. THE ACCOUNTABILITY COMPANION — approved 2026-08-16

Mutalib reopened the "no chatbot" exclusion after Phase 0 interrogation on 2026-08-16 and
said GO. This section is what he approved. It is **scope, not a Sacred Rule** — it can be
cut if it does not earn its place, and § 5b's own success test below is how that is decided.

### Why it exists

§ 2 records the finding this rests on: over two weeks he missed ~14 days, **~6 to
forgetting and ~8 to procrastination**. Every feature built so far — the nudge, the widget,
the streak — addresses forgetting, which is *the smaller half*. Nothing in the app yet
speaks to the 8.

What historically worked was reciting to a teacher: a person who expected him. This is an
attempt at that, and it is the first feature aimed squarely at the larger half of the
problem.

### What it does

1. Asks whether today's wird is done
2. He replies in ordinary words — "after Isha", "I'll do it at 9", "not today, I'm
   travelling", "already did it"
3. **It changes the app** — re-arms the reminder for the time he named, adjusts the
   schedule, or marks the day
4. If he named a time and did not show, it notices

Three jobs, in the order he chose them: **commitment** (naming a time to something that
checks back — the one aimed at procrastination), then **rescheduling without menus**, then
**presence**.

### ⚠ The assumption the whole thing rests on

**Does this create real pressure when you know it is a bot?** Unproven. Phase 0 research
found every AI-accountability project with near-zero traction, and the sharpest comment came
from someone who built a *human-powered* version instead:

> "AI might not be able to deliver the feeling of being held accountable to some people. A
> huge part of having an accountability partner is having somebody there that you don't want
> to disappoint."

That is this feature's thesis, doubted by someone who bet against it with their own build.
**Nothing here is worth polishing until that assumption is tested on Mutalib himself.**

### Success test — decided before building, on purpose

Over 30 days with the companion live, **does the procrastination half move?** Baseline is
~8 missed days in 14 to procrastination. If naming a time to the app does not reduce that,
the feature is theatre and gets cut regardless of how good it feels to use. Days marked
after a commitment was made are logged so this is measurable rather than a vibe.

### Two surfaces

- **In the app** — for everyone, including testers.
- **On WhatsApp — for Mutalib only, and never in the tester build.** His decision,
  2026-08-16. See § 9 for why this is safe and what it costs.

### Boundaries

- **Sacred Rule 3 applies hardest here.** Never guilt-based, never disappointed, never
  passive-aggressive, never "you broke your streak". A companion that makes him feel worse
  is a worse outcome than no companion.
- **Sacred Rule 2 stands: it never produces Qur'anic text.** It talks about the schedule,
  not about the Qur'an. It does not quote ayahs, explain them, or answer religious
  questions — Wird is not a scholar and must never sound like one.
- **It may not mark a day recited.** It can mark a day *tapped* if he says he read it, and
  it can move the schedule. Only a real recording can be a recitation. Sacred Rule 6.
- No streaks, guilt, or personality that pretends to be a person. It should not claim to
  be human or to have feelings about his performance.

## 6. SACRED RULES

Decisions no future session may reopen without Mutalib's explicit approval.

1. **Recordings never leave the device** unless the user explicitly shares them. This is
   now a promise to testers, not just to Mutalib.
2. **Qur'anic text comes from a verified source and is never generated by a model.** No
   exceptions, no "the model probably knows this ayah."
3. **The tone is never guilt-based.** This is his deen, not a language app. Too gentle
   beats too pushy, every time.
4. **The streak is always shown next to total days read**, which never resets. The day
   after a streak breaks is when people delete habit apps.
5. **This is a habit tool, not a Qur'an reader.** Reading features link out.

   **Refined 2026-08-15, with Mutalib's explicit approval.** He asked for full browsing
   and a home screen; we settled on the smaller version and he chose it knowingly:

   - **Today's portion is the front door.** The app opens on it. There is no home screen
     and no menu in between — his own Phase 0 numbers say the decision is the problem, so
     every screen added before the reading is a place to bounce off.
   - **Allowed:** swiping to the pages either side of today's, and a "go to surah"
     control that reuses the setup picker. That covers reciting something other than
     today's portion, and handing the phone to someone else.
   - **Still refused:** a home screen, bookmarks, search within the text, tafsir,
     translations — everything that would make this a Qur'an reader.
   - He said "maybe I might change my mind" about going further. If he does, it is a
     deliberate re-scoping conversation, not a feature someone adds quietly.

   **No user-chosen highlight colour.** Raised and declined on 2026-08-15. There is no
   highlight by design — the portion is marked by everything else stepping back, because
   painting a wash over the Qur'an is what defaces it. An arbitrary colour would also
   break the contrast pairs computed in § 6b, and the failure mode is Arabic that is hard
   to read. A light/dark toggle is the real setting hiding in that request, and it is
   being built.
6. **The done-method split is always shown honestly** — "22 marked, 4 recited." The app
   never lets the user believe they did more than they did.
7. **No AI attribution in any commit, PR, or anything else that lands in the repo.**
8. **The palette below is canon.** Pinned by Mutalib on 2026-08-14. It does not get
   "improved", extended with a sixth colour, or swapped for something a later session
   likes better.

## 6b. PALETTE — pinned, canon

Mutalib's own choice, brought from
[coolors.co](https://coolors.co/palette/01161e-124559-598392-aec3b0-eff6e0). Used
exactly as given — not re-derived through `palette.py`, because regenerating from a seed
would produce different colours and quietly overrule the pick.

| Hex | Name | Relative luminance |
|---|---|---|
| `#01161E` | ink | 0.0067 |
| `#124559` | deep teal | 0.0511 |
| `#598392` | slate | 0.2043 |
| `#AEC3B0` | sage | 0.5116 |
| `#EFF6E0` | paper | 0.8964 |

### Measured contrast, every pair

Computed 2026-08-14, WCAG 2.1 relative luminance. AA body needs 4.5:1, AA large 3.0:1,
AAA 7.0:1.

| Pair | Ratio | Verdict |
|---|---|---|
| ink / paper | **16.68:1** | AAA. This is the mushaf pairing, both modes. |
| ink / sage | 9.90:1 | AAA |
| deep teal / paper | 9.37:1 | AAA |
| deep teal / sage | 5.56:1 | AA body |
| ink / slate | 4.48:1 | **fails AA body by 0.02** |
| slate / paper | 3.72:1 | fails AA body, passes large |
| deep teal / slate | 2.52:1 | fails |
| slate / sage | 2.21:1 | fails |
| sage / paper | 1.69:1 | fails — effectively invisible together |
| ink / deep teal | 1.78:1 | fails — effectively invisible together |

### Roles

**Slate is the trap.** It is the colour that looks like "muted secondary text" and it
fails AA body on *both* backgrounds. It is never body text. It has exactly one job:
**the ayahs outside today's portion.** Dimming is the intent there, so 3.72:1 is a
feature — present if you look, receding if you do not.

Light mode (the reading default — paper):

| Token | Colour | Against | Ratio |
|---|---|---|---|
| `surface` | paper | — | — |
| `textPrimary` | ink | paper | 16.68:1 |
| `textSecondary` | deep teal | paper | 9.37:1 |
| `textDimmed` (out-of-portion ayahs) | slate | paper | 3.72:1 |
| `accent` | deep teal | paper | 9.37:1 |
| `done` | sage | — | ink on sage 9.90:1 |

Dark mode:

| Token | Colour | Against | Ratio |
|---|---|---|---|
| `surface` | ink | — | — |
| `textPrimary` | paper | ink | 16.68:1 |
| `textSecondary` | sage | ink | 9.90:1 |
| `textDimmed` | slate | ink | 4.48:1 |
| `accent` | sage | ink | 9.90:1 |
| `surfaceRaised` (cards) | deep teal | paper text on it 9.37:1 | — |

**Deep teal must never carry text on the dark surface** — 1.78:1, invisible. In dark mode
it is a raised surface only, with paper text on top.

## 7. STACK & ARCHITECTURE

Chosen by Mutalib on 2026-08-14 from a menu of three, as "A now, revisit if iPhone
testers show up."

| Piece | Choice | Why |
|---|---|---|
| App | **Native Android — Kotlin + Jetpack Compose** | The nudge and the recording are the product, and both are native strengths. He already ships Kotlin (Thrum, morning-manna-widget) with a working toolchain. |
| Widget | **Jetpack Glance** | Already built one for morning-manna. Probably the most reliable nudge surface available on Android. |
| Qur'an text + layout | **Quran.com API v4**, free, no key | `verses/by_page/{n}?words=true` returns per-word `line_number`, `page_number`, `code_v1`, `code_v2`. Verified live against page 453 on 2026-08-14: 16 verses, matching the printed page. |
| Mushaf rendering | **QCF v2 glyph fonts**, one file per page | How quran.com itself renders. Fetch only the pages actually read; never all 604. |
| Recitation audio | **Abu Bakr al-Shatri**, reciter id 4 | Free per-ayah MP3s via Quran.com API / everyayah.com. |
| Recitation checking | **whisper.cpp Android AAR** + `tarteel-ai/whisper-base-ar-quran` (Apache-2.0) | On-device, batch not streaming. Batch measured fast on Android; streaming ~5× slower than real time. |
| Tester distribution | **Firebase App Distribution** (Spark, free) | 500 testers per project. No $25 yet. Play Console can come later. |
| Landing + feedback | **One Vercel page**, noindex until launch | His home turf, $0, gives a URL to paste into a GMSA group. |
| Recitation audio | **Abu Bakr al-Shatri, per-ayah MP3s**, two bitrates | 64 kbps from everyayah.com (~1.1 MB/page, default) or 128 kbps from `verses.quran.com` (~2.3 MB/page). Measured 2026-08-16 on page 442: 2,376,926 B at 128 kbps for 13 ayahs — **~15× the 154 KB page font**. Reader's choice, in settings. 32 kbps does not exist for this reciter. Cached to disk with a 50 MB LRU cap. |
| Prayer times | **Computed on device**, standard PrayTimes.org solar geometry | No API, no key, no network, works offline forever. Verified against the Aladhan API for Accra and Kumasi across both solstices: all 24 values matched to the minute, zero error (task 8, 2026-08-16). |
| Location | **`ACCESS_COARSE_LOCATION`, plain `LocationManager`** | Mutalib's pick on 2026-08-16 over a timezone lookup. No Play Services dependency, so no size cost. Asked once and cached; rounded to 2dp (~1 km); never uploaded. Falls back to timezone, then to a fixed hour, and says which it used. |
| Backend | **None** | No server, no accounts, no sync. Everything local. |

**Cost: $0/month.** No paid API, no server, no database.

**Revisit trigger:** real iPhone tester demand after the 30-day measurement. That is a
rebuild decision made on evidence, not guessed at now.

## 8. DATA MODEL

All local. Room database plus files in app-private storage.

| Entity | Fields | Where |
|---|---|---|
| `Plan` | pagesPerDay, perWeekdayOverrides, startPage, createdAt | Room |
| `Position` | currentPage, currentSurah, currentAyah, updatedAt | Room |
| `DayEntry` | date, assignedPageStart, assignedPageEnd, status (done/missed), method (recited/tapped), recordingId?, completedAt | Room |
| `Recording` | id, filePath, durationMs, createdAt, checkResult? | File in app-private dir; row in Room |
| `CheckResult` | recordingId, matched (bool), confidence, modelVersion, checkedAt | Room |
| Cached page | pageNumber, layoutJson, fontFile | App-private cache dir |
| Cached audio | ayahKey, mp3 | App-private cache dir |

Nothing here syncs anywhere. Export (task 15) writes the lot to a folder the user can
copy off the phone.

## 9. INTEGRATIONS & KEYS

| Integration | Key needed | DRY_RUN behaviour |
|---|---|---|
| Quran.com API v4 | **None** | n/a — read-only, public |
| everyayah.com audio | **None** | n/a |
| QCF fonts (nuqayah/qpc-fonts, QUL) | **None** | n/a |
| whisper model weights | **None** — Apache-2.0 | n/a |
| Firebase App Distribution | `FIREBASE_APP_ID`, service account JSON | `DRY_RUN=1` → build locally, do not upload |
| Feedback → WhatsApp | none (intent to the user's own WhatsApp) | `DRY_RUN=1` → compose the message, do not send |
| **Companion on WhatsApp — MUTALIB ONLY, never in the tester build** | Meta WhatsApp **Cloud API** (official), free test number | `DRY_RUN=1` → log the message, do not send |

**The WhatsApp companion, researched 2026-08-16 before any commitment:**

- **It is safe for his account this time, and the reason matters.** The 2026-07 Green API
  incident restricted his personal number because that number was linked *as the gateway*.
  Here his personal number is simply **the customer messaging a business account** — ordinary
  WhatsApp use, no gateway, no linking, no risk. That is the whole difference.
- **It is genuinely $0 for one user.** Meta's free test number allows unlimited messages to
  and from **up to 5 verified numbers**. No template fees, no per-message billing, no number
  to buy. ⚠ This is *only* true at this scale — the 1 October 2026 per-message pricing change
  and the template requirement both bite the moment it serves anyone but him, which is the
  hard reason it stays personal-only.
- **⚠ It requires a server, and that breaks § 7's "Backend: None".** WhatsApp delivers
  inbound replies *only* by pushing to a publicly reachable HTTPS endpoint with a valid
  certificate, answering within 5–10 seconds. There is no polling and no inbox to read. A
  Vercel function on the free tier covers it, but **Wird gains a backend for one user**, and
  that must never quietly become a backend for everyone.
- **The state has to be shared**, which is the real engineering cost: if he tells WhatsApp
  "I'll do it at 8", the phone has to learn it. That is a second source of truth for the
  position, and **sync is where correctness bugs live** — in an app whose entire value is an
  honest record.

`.env.example` names every one. **`DRY_RUN=1` is the default** for anything that sends
or uploads. Never link a personal WhatsApp number to an unofficial gateway — see the
2026-07 Green API incident.

## 10. CONSTRAINTS

- **Budget: $0/month.** A $25 Play Console fee is a later, separate decision.
- **Build machine:** Lenovo i7-1165G7, 15.7GB RAM, no Avast. Android Studio + SDK + adb
  working since 2026-08-01, JBR as JAVA_HOME, android-36.1, ~34s incremental builds.
  Pixel 6 Pro over USB. `--max-workers=1` is *not* needed on this machine (that was the
  old Avast laptop) — only reach for it if Gradle actually misbehaves.
- **Testers are on Tecno / Infinix / itel.** All Transsion, all aggressive about killing
  background work. Anything scheduled must be verified on one of those phones, not just
  on the Pixel.
- **Ghana mobile data.** Fetch pages and audio on demand and cache. App size is a
  first-class concern and gets measured, not assumed.

  **Measured 2026-08-14 (task 1), `nuqayah/qpc-fonts`:**

  | Asset | Size |
  |---|---|
  | `mushaf-v2/QCF2001.ttf` | 618,744 B |
  | `mushaf-v2/QCF2002.ttf` | 357,932 B |
  | `mushaf-v2/QCF2453.ttf` | 349,420 B |
  | `mushaf-v2/QCF2604.ttf` | 163,044 B |
  | `mushaf-v2/QCF2BSML.ttf` (verse-number symbols) | 327,564 B |
  | **`mushaf-woff2/QCF_P453.woff2`** | **79,620 B — 77% smaller than the same page's TTF** |

  Mean ≈ 372 KB per page as TTF, so **all 604 pages would be roughly 214 MB.** Bundling
  the mushaf is off the table — it is not a preference, it is arithmetic. Fetch per page,
  cache, and never prefetch the whole book.

  At ~372 KB/page a one-page-a-day reader costs about 11 MB of data a month. The woff2
  set would cut that to about 2.4 MB. See § 11 for the catch.

  Real directory names in that repo: `mushaf`, `mushaf-v1.5`, `mushaf-v2`,
  `mushaf-v4-hafs`, `mushaf-v4-warsh`, `mushaf-woff`, `mushaf-woff2`, `text-mushafs`,
  `various`. Note the naming differs per set — v2 is `QCF2453.ttf` but the woff2 set is
  `QCF_P453.woff2`. Guessed paths return a 14-byte "404: Not Found" body rather than an
  HTTP error, so **check the byte count, not just that the download succeeded.**
- **Licensing:** the KFGQPC mushaf fonts are **not for commercial use.** Personal and
  free-tester use is fine. Anything with money attached needs permission from the King
  Fahd Complex. This constraint does not expire.

## 11. RISKS & OPEN QUESTIONS

| Risk | What would resolve it |
|---|---|
| ⚠⚠ **The companion's core assumption is unproven and may simply be false.** Does a bot create real pressure when you know it is a bot? Phase 0 found near-zero traction across every AI-accountability project, and a builder who chose humans over AI said the feeling of not wanting to disappoint someone *is* the mechanic. | Test the cheap version first — the commitment loop with no model at all — before spending anything on intelligence. § 5b's 30-day test decides it. **"It does not work" is a legitimate outcome and gets it cut.** |
| ⚠ **The WhatsApp companion gives Wird a backend and a second source of truth for the position.** Sync bugs, in an app whose entire value is an honest record, are the worst class of bug it can have. | The phone stays authoritative for position and the day log. WhatsApp may *propose* a change; the phone applies it. Never the reverse. |
| ⚠ **Companion scope.** Larger than tasks 10–20 combined, and Milestone 2 still has the widget outstanding. | Sequenced after the widget. The no-model commitment loop is deliberately first, so the assumption is tested before the expensive parts are built. |
| **He stops using it in two weeks.** The likely failure and not a technical one. | The 30-day measurement. Guarded by unfakeable recordings, prayer-time timing, the honest split, and the widget. |
| **Scope drift into the full Qur'an app** before v1 proves anything. He has said his mind is already there. | NOT IN V1 above, plus the parked-not-killed note. Revisit after day 30. |
| **He cannot recite aloud where he actually reads** — he uses the mushaf in class. | Two weeks of real use. If most reading is silent, the recording mechanic is weaker than it looks. |
| ~~Exact alarms denied by default~~ **CONFIRMED task 3, 2026-08-14.** Measured twice on a fresh install: `cmd appops get com.mosman.wird SCHEDULE_EXACT_ALARM` → `Default mode: default`, and `canScheduleExactAlarms()` returns false. The nudge still fires via `setAndAllowWhileIdle`, and the UI states which kind you got. | Resolved in code. The widget (task 10) remains the more reliable surface. |
| ⚠ **Unexplained: something granted the exact-alarm appop mid-session.** It went from `default` to `Uid mode: allow` during the first test run and I could not determine what did it. **Ruled out by experiment:** granting `POST_NOTIFICATIONS` via `pm grant` does *not* move it. Most likely the system settings screen was opened, but that is not proven. | Matters only if it can flip *back* without the user knowing. `Nudge.canScheduleExact()` is re-read on every schedule rather than cached, so the app tracks reality either way. Watch for it during the 30-day run. |
| ⚠ **Tecno/Infinix freeze background work.** | Task 15, verified on a real Transsion phone. |
| ⚠ **The dev phone is at one extreme of the Android range and the testers are at the other.** The Pixel 6 Pro runs Android 17; Transsion budget phones in Ghana are typically on 12–14. Alarms, notifications and background limits differ at both ends, so a green result on the Pixel is not evidence about a Tecno. | Tasks 3 and 15 both need a second device. Until one exists, any notification claim is provisional and must be labelled as such. |
| ⚠ **QCF glyph rendering on Android Compose is unproven.** Glyph codes live in a font private-use area. | Task 4. If Compose cannot render them, fall back to a `WebView` or to plain Uthmani text and say so. |
| ~~Font payload unmeasured~~ **RESOLVED task 1, 2026-08-14.** ~372 KB/page TTF, ~214 MB for all 604. See § 10. | — |
| ⚠ **woff2 would cut data cost by 77%, but Android cannot load it directly.** `Typeface` wants TTF/OTF. Using woff2 means decompressing on device or rendering the page in a WebView. | Task 4 decides: TTF at ~372 KB/page, or woff2 at ~80 KB plus a decoder. Measure both on a real 3G connection before choosing. |
| ⚠ **Whisper accuracy on his voice, in his room, is unknown.** The 5.75% WER is on clean professional recitation and the model card lists no limitations. | Task 13, measured on real recordings. |
| **Deep links into Quran for Android / Tarteel may not exist.** | Task 11. If they do not, the buttons get deleted, not faked. |
| ~~Nothing in the app scheduled a nudge~~ **FOUND AND FIXED task 8, 2026-08-16.** Task 3's temporary "fire in 15 seconds" button was removed in the task 4/5 rewrite, and from then until task 8 the alarm machinery was real and idle. `NudgeReceiver` was also still hardcoded to page 453. **The lesson worth keeping: a gate task proved with a throwaway trigger leaves nothing behind.** | Resolved. `NudgeScheduler.arm` is now called from three overlapping places, and the receiver reads the real position. |
| ⚠ **The nudge has not been seen to FIRE with the task-8 receiver.** Timing is proven three ways, but re-arming tomorrow, staying quiet on a day already read, and reading the real position are proven only by unit test and by log. | Watch the next time it fires. Cheap to confirm and it must not be assumed. |
| ⚠ **The reminder depends on a permission Transsion testers are the likeliest to refuse.** Refusal is handled — timezone, then a fixed hour — but a refused fix in Kumasi is 6 minutes out, and in an unlisted timezone there is no fix at all. | Task 15, on a real Transsion phone. Watch how many testers actually grant it. |
| ~~Task 9's audio has never played on the phone~~ **RESOLVED 2026-08-16. Airplane-mode test passed** — 1.17 MB fetched once, then played from disk with DNS dead and the app cold-launched. **And it caught a shipping bug:** a `cancelled` boolean that `stop()` set and only `play()` cleared, where `play()` runs *after* the fetch — so listening worked exactly once per launch and blamed the network for it. Now a monotonic `run` token. | Closed. The lesson stands: **the on-device test is not a formality, and a plausible error message is the most dangerous kind of failure.** |
| ⬜ **The 50 MB audio LRU prune has never fired.** It needs roughly twenty pages of listening history to trigger, so nothing has evicted yet. | Watch it around task 19 (export), where a retention choice is due anyway. |
| ⚠ **OPEN, and Mutalib's to answer: does listening count as doing your wird?** Today it does not — the control plays and marks nothing. He picked "both places" for it, and that option was worded "a third way to finish", which points at yes. Saying yes means a **third `Method`, `LISTENED`**, alongside RECITED and TAPPED — touching the day log, the progress line ("4 recited, 5 listened, 19 marked") and Sacred Rule 6's split. | Not decided unilaterally, because the wrong answer writes a false record into the one thing this app exists to keep honest. Ask before task 10. |
| **The 678-byte error page, hit a second time.** § 10 records it for fonts; task 9 hit it again on the audio CDNs, with a hand-built malformed URL returning HTTP 200 and identical byte counts for 13 different ayahs. | Byte-count guards now on both. **The tell is identical sizes across files that should differ** — worth checking for on any future CDN. |
| **Muslim Pro's Maghrib alarm is 3 minutes later than ours** (18:23 vs 18:20 on 2026-08-16, same phone, same place). Its Dhuhr, Asr and Isha match us exactly, so this is not a general disagreement. | Most likely its own deliberate delay on the Maghrib adhan, which is a common convention. Not chased — the nudge is a reading reminder, not a call to prayer, and 3 minutes does not matter to it. Recorded so nobody later reads it as a bug in our arithmetic. |

## 12. VERIFICATION

- **Check command:** `check.ps1` → `gradlew.bat lint testDebugUnitTest`
- **QA suite:** `app/src/test/.../WirdQaTest.kt` — starts small and only grows. First
  three checks: portion arithmetic across a surah boundary, streak maths across a
  missed day, and done-method split counting.
- **Before any distribution build:** `check` passes, and the change is verified on the
  Pixel over USB with evidence (measurement or output, not a claim).
- **Before any tester build:** the above, plus verified on a Transsion phone for
  anything touching notifications or background work.
- **Before the first public deploy:** `docs/security-checklist.md` worked end to end and
  the ticks committed.
