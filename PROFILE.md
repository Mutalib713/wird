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

**⚠ RAISED AGAIN 2026-08-17, and not yet resolved.** Two things he said, both of which
change scope more than they look:

**✅ RESOLVED, same day.** *"For the 30 days judgement we will do it, but I just want this
to be my Qur'an app."* So the measurement still happens — **it stops being a gate.** Wird
becomes a Qur'an app he reads in, and Sacred Rule 5's *"a habit tool, not a Qur'an reader;
reading features link out"* is **reversed in full**, alongside the tab-bar reversal above.

What that changes in practice: browsing, a real reader, and the recitation review all become
in-scope rather than link-outs. **What it does not change:** the app still opens on today's
portion, and § 3's success metric is unchanged — 20 of 30 days, 10 of them recited aloud.

*The risk he is knowingly taking, stated once and then not laboured:* the 30-day number was
meant to come in **before** large effort went into a reader, because it answers whether the
habit mechanic works at all. Building both in parallel means that effort is spent before the
answer exists. He has decided that trade is worth it. **If the 30-day verdict comes back bad,
the reader is not the thing that failed** — do not let a good reader disguise a habit
mechanic that did not work.

**1. "I want this app to be my go-to app for my Qur'an rather than using another app."**
That is this parked item, verbatim, and it reverses the core of Sacred Rule 5 — *"a habit
tool, not a Qur'an reader; reading features link out."* **The 30-day condition above is
the only thing standing in the way, and it exists for a reason:** v1 has not yet proved he
uses it, and a full reader is the biggest possible way to spend effort before knowing that.
Not refused — *sequenced*. Worth asking him directly whether he wants to spend the 30 days
first or reopen now with that risk understood.

**2. "I won't actually send the audio to anyone."** Two real consequences:

- **Task 11 (share a recording to WhatsApp) loses its reason to exist.** It was built on
  the assumption that sending a recitation to someone was the accountability. If nothing is
  ever sent, that task should be **cut**, not built. Confirm before deleting it.
- **He wants the recording analysed instead:** *"when I finish the audio then it compiles
  and shows me where I did mistakes, where I can tap on that section and see if it's a
  tajweed mistake or any kind."*

  ⚠ **This sits on the NOT IN V1 list twice, and only one of those refusals still holds:**
  - *"Live word-by-word recitation following — measured at ~5× slower than real time."*
    **This objection does not apply.** He is asking for **post-hoc batch analysis** — record,
    finish, then review — which is exactly what task 14 already does and is tractable.
  - *"Tajweed scoring or pronunciation correction. That is Tarteel's job and it does it
    better."* **This objection does still apply**, and it is a quality argument rather than
    a technical one. Task 14's model (`whisper-base-ar-quran`) is trained to transcribe, not
    to judge tajweed. **It can plausibly tell you where you diverged from the expected
    words; it cannot reliably tell you whether a madd was held long enough.** Promising
    tajweed feedback it cannot deliver would be the worst failure this app could have —
    wrong correction on the Qur'an is worse than none.

  **The honest middle, and what should be scoped first:** task 14 already knows the assigned
  passage, so *"here is where what you said stopped matching what was expected, tap to hear
  it back"* is achievable. **Naming the error type is not**, on this model. Whether that
  narrower version is worth building is his call.

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

   **⚠ REVERSED 2026-08-17 — "no menu in between" no longer holds.** Mutalib reviewed
   three designs (Claude Design *before* and *after*, plus a Figma direction) and chose the
   variant with **cards and a five-tab bottom bar** — `TODAY · SŪRAHS · READ · RECITE ·
   MORE`. He said plainly: *"reopen rule 5, i like the tabs."*

   The argument he accepted, and the one against, both recorded so this is not re-litigated
   from scratch later:
   - **Against:** his own Phase 0 numbers say the *decision* is the problem — 8 of 14 missed
     days were procrastination, not forgetting — and a five-tab bar adds decisions in front
     of the reading. That is why the rule existed.
   - **For:** the app has grown since 2026-08-15. There are now genuinely five places to be,
     and hiding them behind a tap-to-reveal bar is the weakness he already spotted himself
     in task 5e when he asked whether it should be a hamburger. The rule described a smaller
     app than the one that now exists.

   **What survives the reversal:** the app still *opens* on today's portion — the Today tab
   is the launch tab, not a dashboard in front of it. Tabs are a way back, not a gate.
   **Still refused:** bookmarks, in-text search, tafsir, translations.

   **Confirmed 2026-08-17 after viewing the design rendered:** the chosen variant is the
   one with the **bottom** tab bar (`TODAY · SŪRAHS · READ · RECITE · MORE`), not the
   top-tab variant on the neighbouring screen. He also said "we will change the designs
   tho" — so **the mockups are a direction, not a specification.** What is fixed is what is
   written here; the pixels are expected to move.

### 5c. Verified against the design, 2026-08-17

Checked by rendering the file rather than reading its tokens, after the token-only reading
misled once already:

- ✅ **No wash is painted over the Arabic. Sacred Rule 5's other half survives.** The
  reading page is ink on cream, untouched, and an ayah is marked by a **gold rule in the
  margin** plus a circled gold numeral. A different mechanism from "everything else
  recedes", but the same principle: nothing is laid on top of the Qur'an.
- ✅ **The reading surface stays cream inside an otherwise dark app**, which is exactly what
  the contrast maths forced independently — gold is unreadable on cream (2.06:1) and AAA on
  midnight (7.79:1). ⚠ **Consequence not yet built: the app's default theme must flip to
  dark.** Wird currently defaults to light. Dark becomes the app; cream becomes the page.
- ⚠ **The companion is not a screen** — it lives on Today rather than behind a tab.
  **But NOT above the portion. Overruled by Mutalib the same day:** *"today's portion should
  be first."* The design put the greeting and the companion card first and pushed the
  portion below them, which is the front door losing its place to a greeting. Portion first,
  companion under it.
- ⚠ **The companion does not read as something you can talk to.** His words: *"the user
  won't know it's even a chat bot."* Three fixed reply chips look like a poll, not a
  conversation, and the plain-words input is the part that makes it a companion at all. It
  has to invite a reply, not just offer buttons.

### 5d. Direction notes — the design is a reference, not a spec

Mutalib, 2026-08-17, having seen it rendered:

- **"That design looks boxy."** Everything is a bordered card stacked on another bordered
  card. Take its colour, its type and its restraint; **do not take its box-per-thing
  layout.** Rules, spacing and hierarchy can separate things without a border round each one.
- **Element order is ours to set**, not the mockup's. Today's portion first.
- ✅ **The mushaf page keeps what it already has.** *"The design of the font and pages of
  the Qur'an, maintain the ones we did earlier — I like those ones."* The QCF per-page
  glyph fonts, the fifteen-line layout, the lit/receded treatment and the ayah-range
  headline all **stay exactly as built**. The redesign is chrome only. This is the part of
  the app that took the most work to get right and it is not being reopened.
- **A name is now asked for.** Approved 2026-08-17 — the design greets "Good evening,
  Amina" and he confirmed he wants it. Wird has never had a name, an account, or anything
  to hold one. **It is a local string and nothing more**: no account, no sync, no
  validation, skippable, and it never leaves the phone. Sacred Rule 1 is untouched by it.
- Red earns exactly one place: the small record dot on "Recite it aloud".

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

   **⚠ REVERSED 2026-08-17, by Mutalib, on seeing it rendered.** He chose the Claude Design
   direction on its colour and type: *"i like the claude design one we can see the font and
   colours."* The five-colour ink/teal/slate/sage/paper palette below is **superseded**, and
   § 6b is kept as the record of what it was rather than what to build.

   **The new direction:** gold `#C9A24B` on cream `#FBF9F3`, with midnight `#14101F` — a
   classical illuminated-mushaf register rather than the paper-and-ink one. Type is
   Cinzel / Playfair Display / EB Garamond / Oswald, with **Amiri** carrying the Arabic.

   **Two things a later session must check rather than assume**, because they were flagged
   at the time and not yet resolved:
   - The token names in the source (`--curtain`, `--crimson`, `--evergreen`, `--snow`) are
     inherited from a **Nutcracker poster design system** the design was built on top of.
     In the rendered screens the red is barely used — 5 elements in total — but **it is
     there, and nothing in Wird is an error worth alarming someone about.** Strip what is
     unused rather than porting the whole token set.
   - **The contrast pairs in § 6b were computed for the old palette and do not transfer.**
     Gold on cream is the pairing to measure first; it is the one most likely to fail AA.
   - The rule itself is not abolished: **the new palette is canon in the same way**, and
     changing it again needs the same explicit approval.

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

### 5e. Settings follows the design's *after* variant — decided 2026-08-17, not yet built

Mutalib, having seen both: *"the settings side, go to the after Claude Design — that one is
way better."* He is right, and the difference is structural rather than decorative.

**What is built now (the *before* pattern):** every control is inline. Each row shows its
name, its explanation, and its chips all at once. Readable — that rebuild fixed the real
complaint — but **long**, because five groups of inline chips is a screen you scroll rather
than scan.

**What the *after* variant does:** each setting collapses to **one row showing its current
value**, with a chevron to drill in and change it:

> **Lighter days**
> `FRIDAY AND SUNDAY · HALF A PAGE`  ›

The value *is* the caption. You scan the whole of settings in one screen and see what
everything is currently set to, and only the thing you came to change costs a tap. This is
what iOS and Android settings actually do, and it is why they stay legible at forty rows.

**What building it needs:** a detail screen per setting and navigation state to reach it —
the current screen has neither. The inline controls already written become the bodies of
those detail screens, so **the rebuild is not wasted**; it gets moved rather than replaced.

**Also still outstanding on this screen:** the *after* variant styles rows with the serif
display face for titles and small-caps for the value line. That depends on the fonts being
bundled, which has not happened yet.

### 5f. Home is page-first — decided 2026-08-17

Asked directly, because the design assumed the opposite and it changes the shape of the
app. His answer: *"page-first home, keep the mushaf as the front door."*

**So the Home tab stays the mushaf page.** You open Wird and you are reading. The design's
Home is a dashboard — greeting, portion card, stats — with the page behind an
"OPEN THE PAGE →" button; **that half is not taken.**

**Why it matters, and why it was worth asking rather than assuming:** this is the one thing
that survived every other reversal today. Sacred Rule 5 lost "no menu in between" to the tab
bar and "not a Qur'an reader" to the full-reader decision, and what stayed both times was
*the app opens on today's portion*. § 2's number is the reason — **8 of 14 missed days were
procrastination**, so every screen between him and the page is one more place to stop.

**What still gets built, and where it goes:** the greeting, the two numbers and the
companion are all wanted. They live **above the page, reached by scrolling up** from it, or
as a sheet — not as a screen in front of it. Combine the *before* and *after* variants for
their look; take neither's navigation.

**⚠ Open, and the sharpest note he has given: the companion does not read as something you
can talk to.** Three fixed chips (*after Isha · at 9 · not today*) look like a poll, and
nothing signals a reply is possible. The fix is not decoration — it is making **the input
the primary affordance**, with the chips as shortcuts *under* it rather than instead of it,
and the companion's messages appearing as its own turns so something is visibly addressing
you and waiting. Design this properly rather than bolting it on.

### 5g. Home is a dashboard after all — reversed 2026-08-17, same day as § 5f

§ 5f recorded "page-first, keep the mushaf as the front door", built as a companion band
above the page. Seeing it, he rejected it: *"it shouldn't be like that, it should be like
the design in the Claude Design… this looks bad. Like a complete home screen, like how the
Claude Design did theirs."*

**So § 5f is superseded. Home is a dashboard; the mushaf page sits behind "Open the page →".**
The band was reverted rather than left in.

**Why it did not work, which is worth keeping so it is not retried:** a band above the page
is neither thing. It is not a home screen — too thin to carry the greeting, the two numbers
and the day's state — and it is not a clean page either, because something permanent is now
parked above the Qur'an. Half-measures between "page first" and "dashboard" land in the gap.

**What Home must contain**, from the design's own Today screen, in its order:

1. Eyebrow: weekday and the Hijri date — `THURSDAY · 3 RABĪʿ AL-AWWAL`
2. Greeting in the serif display face — `Good evening, <name>` (the name § 5c approved)
3. The companion card, already built in `Companion.kt` — question, input, shortcuts
4. **Today's portion card:** surah and ayah range, `3 ayahs · half a page · page 293`, the
   surah name in Arabic, a progress bar, `JUZ' 15 · 41% THROUGH`, and **`OPEN THE PAGE →`**
5. The two numbers as a row of figures: `7 IN A ROW · 23 DAYS READ · 5 RECITED`
6. `MARKING IT DONE` — the three controls
7. `YOUR WIRD` — the last few days, which the History tab already renders

**What survives from § 5f and must not be lost:** § 2's finding still stands — 8 of 14 missed
days were procrastination, so every extra step before reading costs something. The dashboard
earns its place only if `OPEN THE PAGE →` is unmissable and one tap. **Watch this at the
30-day measurement:** if days start being missed after the dashboard lands, this is the first
suspect.

### 5h. The companion's real scope — decided 2026-08-18

Asked directly what it should do beyond "are you reading today?". He chose **all of it**,
and added one thing that was not on the list:

1. **Your reading, fully** — "how am I doing?", "what did I read last week?", "make Fridays
   lighter", "I'm travelling till Sunday", "move my reminder later"
2. **Navigate and act** — "open Al-Kahf", "play today's portion", "mark it done". A text
   layer over what the app already does
3. **Explain the Qur'an** — meaning of ayahs, religious questions
4. **Encouragement** — noticing patterns, checking in
5. **His own addition: a daily verse, with its meaning and a reflection**

**1, 2 and 4 are unblocked.** They talk about his wird and drive the app; nothing there
touches Qur'anic content. Build them first — they are also the ones that serve § 5b's actual
purpose, which is the procrastination half of § 2's finding.

**3 and 5 reverse § 5b's boundary** (*"it talks about the schedule, not about the Qur'an…
Wird is not a scholar and must never sound like one"*). He was shown that and chose it
anyway, so it is approved — **but Sacred Rule 2 is not reversed and still binds absolutely.**
That rule is not about tone, it is about truth:

> **Qur'anic text comes from a verified source and is never generated by a model.**

**Therefore, the only acceptable implementation:**

- **The ayah, its translation and its tafsir are FETCHED, never generated.** The Quran.com
  API already serves translations and tafsirs, free and keyless — the same API this app
  already uses for page layout. Every one carries its **translator or mufassir named on
  screen**, so the reader knows whose reading they are getting.
- **A model may summarise or rephrase a fetched tafsir. It may not author one.** The
  difference is the whole rule.
- **"Reflection" is the one genuinely generated part**, and it is the dangerous one. If it
  ships it must be **visibly the app's own prompt to think, never presented as meaning** —
  a question rather than a claim. "What does relying on Allah look like for you this week?"
  is safe; "this ayah means…" is not, unless quoted from a named source.
- ⚠ **A wrong tafsir is worse than a wrong tajweed label**, and unlike tajweed the reader
  usually cannot tell. This is the highest-risk thing in the whole app.

**Sequencing:** 1, 2 and 4 first, because they are unblocked and they are what the feature
was approved for. 3 and 5 after, and not until the fetch-and-attribute path is built.

### 5i. Our fonts, their layout — decided 2026-08-18

*"Nah, use our font like that, just the design I want."*

**The five display faces in the Claude Design source — Cinzel, Playfair Display, EB Garamond,
Oswald, Amiri — are NOT being bundled.** Wird keeps the system faces it already uses.

Why this is the better call, and not just the cheaper one:

- **Five bundled families is real weight** on an app that already ships a 154 KB glyph font
  *per mushaf page*, for readers on Ghanaian mobile data. § 10 says size is measured, not
  assumed, and this is size we would be adding for chrome rather than for the Qur'an.
- The app's one piece of genuinely distinctive typography is the mushaf itself, and that is
  a real KFGQPC face already. Dressing the chrome in a theatre-poster serif does not make
  the Qur'an look better; it competes with it.

**What IS taken from the design:** layout, structure, hierarchy, spacing, the colour
direction, and the patterns — grouped settings rows, the value-plus-chevron pattern, the
dashboard's order, the icon-over-label tab bar.

**How the remaining porting works, at his direction:** he points at a screen in the design
and says what to take and what to leave. *"I will take you to what to take out and what to
add from that design."* So a session should **ask rather than infer** which parts of a screen
he wants — inferring is what produced the boxy band, the wrong element order, and a
dashboard that had to be reverted.

**Not an option:** running the design's HTML in a WebView. It would cost the mushaf renderer,
which is native, uses `Typeface.createFromFile` per page, and is the one thing he has said
explicitly must not change (§ 5d).

### 5j. The before/after, settled — and what Home actually takes. 2026-08-18

Three prior sessions guessed at which mockup was which and two of them had to be reverted.
This records the answer in his own words so it is never guessed again.

**There are four design files, not two,** and they are not all the same generation:

| File | Palette | Front door | Carries |
|---|---|---|---|
| `Wird.dc.html` (59 KB) | sage/teal | — | the earliest sketch |
| `Wird v2.dc.html` (78 KB) | sage/teal | the page | a Memorisation screen, recitation-check states, a chat companion |
| `Wird App.dc.html` (110 KB) | gold/cream/midnight | a dashboard | "Good evening, Amina", a five-tab bottom bar |
| `wird-design.html` (1.9 MB) | gold/cream/midnight | — | **the newest export, and the one he is looking at** |

All four now live in `design-source/`. They were only in `~/Downloads` before, which is not
a place a project's canonical reference should live.

**`wird-design.html` is newer than the zip** and is the only one holding *both* variants side
by side. That is what he means by "two designs":

- **BEFORE** — captioned *"CARDS, AND A BOTTOM TAB BAR"*. Has the greeting
  `Good evening, / Amina`, and five bottom tabs.
- **AFTER** — captioned *"NOW · TOOLBAR, TABS, PAGE CHROME"*. His words:
  *"the after one is the one on the right, the ones with the tabs on top with today surah
  and juz."* Has a `WIRD` toolbar, a `TODAY · SŪRAH · JUZʾ` top strip, **and no greeting at
  all** — which is exactly why the greeting had to be carried across from the before.

**What Home takes, decided 2026-08-18 and built the same day:**

1. **The after's order and hierarchy** — toolbar, portion, companion, numbers, the week.
2. **The before's greeting**, `Good evening, <name>`, the name in the accent colour on its
   own line as the mockup has it.
3. **Our colours, unchanged.** *"but maintain our colours tho."* The gold/cream/midnight of
   § 6/8 stands; nothing is taken from any file's palette.
4. **The top strip is NOT taken.** Asked, because it is a navigation decision and § 5i says
   ask. His answer: *"i just want the bottom tabs."* The after has no bottom bar — the strip
   *is* its navigation — so running both would have put "Sūrah" on one screen twice.
   ⚠ **Consequence, recorded so it is not lost: Juzʾ now has nowhere to live.** The app still
   cannot browse by juzʾ at all. That is a real gap, and it is a deliberate one.

**Two smaller things the after got right, and both are now in:**
- The page number is **a figure beside the portion, not words inside it**. It is glanced at,
  and a number buried in a sentence has to be read instead.
- The recited count **stopped being a third figure**. The old row put "5 RECITED" directly
  above "5 recited aloud, 18 marked as read" — the same number twice, in two shapes.

**Still not taken, and deferred deliberately:** the after also carries the three done
controls (`RECITE IT ALOUD · I READ IT · LISTEN TO IT`) on Home. They live at the foot of
the page today because the recorder's state — eight `remember`s, a `Recitation`, a
`PortionAudio` and a `listen()` — all sits inside `TodayScreen`. Moving them means hoisting
that state to `MainActivity`, which is a real refactor of the app's most-reverted screen.
**Bolting on a half-version is how this screen got rejected before.** It is its own change.

### 5k. The name — built 2026-08-18

§ 5c approved this on 2026-08-17 and nothing was built; `HomeScreen` carried a comment
saying so. Now done.

**It is one string in SharedPreferences and it does exactly one job:** the greeting says a
name. It is not an account, it is not a profile, and Sacred Rule 1 means it never leaves the
phone — the setup screen says that out loud, because a text box asking your name is the
shape of a signup form and this app has none.

**Asked first, and skippable.** First because the greeting is the first thing Home draws, so
asking later would mean one launch that greets you as nobody. Skippable because the fallback
— the bare hour, which is what shipped before — already reads fine, and "Good evening,
friend" reads worse than "Good evening". **Skip is a full-width control, not a grey word in
a corner.**

### 5l. ⚠ Memorisation is an ONBOARDING QUESTION, not a screen. Corrected 2026-08-18

A session read "the memorisation screen" and found the Hifdh screen in `Wird v2.dc.html` —
a full revision mode with *"Due today · An-Naba 1–20"*, per-surah progress bars and *"Recite
from memory"*. **That is not what he meant**, and he corrected it before anything was built:

> *"the memorization i was talking about was a page on the onboarding — we ask if u are
> memorizing the quran or u are reading, cos some people memorize and some to look in the
> mushaf to read."*

**So the ask is one question at first run: are you memorising, or reading from the mushaf?**
Much smaller than the screen, and a better idea — it is a fact about the reader that the
whole app could act on, rather than a second mode bolted beside the first.

**⬜ NOT BUILT, and blocked on a decision that is his:** *what actually changes when someone
answers "memorising"?* Plausible answers — smaller daily portions, revision of what is
already memorised rather than onward reading, the recitation check comparing from memory
with the page hidden, or nothing at all in v1 beyond recording the answer. **Storing an
answer the app ignores is worse than not asking**, because it promises something. This needs
its own conversation before it is built.

⚠ Note also that `Wird v2.dc.html`'s own index files Memorisation under a group called
**"Beyond v1"** — the designer reached the same conclusion independently.

### 5m. The companion becomes a real chat — approved 2026-08-18, not yet built

> *"for the chat bot side it's not only that we discuss about it, it has more features, so
> mk it like an actual chat bot interface instead of what's there, cos the user won't know
> if it is a chat bot."*

He is right about the symptom — § 5f already recorded that three fixed chips read as a poll.
This is the fix, and it goes further than § 5f's: the *interface* becomes a chat, not just a
better-arranged card.

**`Wird v2.dc.html` has already drawn it**, and it is good: a `← Today's check-in` header; a
card holding the commitment as an object (**YOU SAID** *"after Isha"* · Checking back at
**8:10 pm** · a breathing dot reading *"Holding since 6:12 pm · reminder moved to match"*);
real bubbles, its turns left and yours right; a text field pinned to the bottom reading
*"Reply in your own words"*; and the missed case written out — *"You said 8:10. It is 8:40.
Still tonight?"*, which the file annotates **"A fact and a question. No streak talk, nothing
disappointed, nothing red."**

⚠ **One tension to resolve rather than inherit:** that design's own header comment calls it
*"a standing appointment, not a chat log"* — it speaks twice and no more, on purpose. He is
asking for something more conversational. Those are different products, and the design's
restraint was a deliberate answer to Sacred Rule 3.

**⬜ The engine is still undecided, and stays his call — PLAN task 22.** Asked directly on
2026-08-18; his answer was *"we will decide when we get there."* So it is open, and the
honest risk is written down now rather than discovered later: **a chat box invites anyone to
type anything, and `CompanionBrain` is hand-written rules that understand about seven
phrasings.** Making it look like a chatbot raises the promise; the rules underneath do not
rise with it. § 5h's approved tafsir and daily verse are flatly impossible on rules.

### 5n. Tabs, confirmed unchanged. 2026-08-18

*"the tabs there will be four right — home, surah, history and more."* Confirmed. That is
exactly what `TabBar.kt` already ships. No change; recorded so it stops being re-asked.
