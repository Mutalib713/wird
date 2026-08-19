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

**His ideas of 2026-08-19, placed rather than built.** All four came in one message; two are
blocked on the recitation checker, and the two that are not are recorded here with the order he
gave them: *"4 is a later thing and 3 is later but sooner."*

**⏳ 3. Ramadan asks about your plan — later, but sooner.**
Around Ramadan the companion asks what you intend for the month, and holds you to it the way it
holds you to a time. It fits the machinery that already exists — a plan, a schedule, commitments
— and it is the one month of the year when almost every Muslim reader already has a target in
their head. ⚠ **It is also dated work**: it is worth nothing outside a six-week window, so it has
to be finished before Ramadan or wait a year. That is what makes it "sooner" despite being later.

**⏳ 4. A separate revision track — later, and the largest change in the app.**
*"You can start something that is separate from your daily portion and it won't affect it, or
even you can pause it."* Revise from here to there, running alongside the daily wird, with its
own position and its own pause.

⚠ **This is the biggest structural change Wird has had since it was built, and the reason is one
sentence: the app currently has exactly one position.** `positionUnit`, one plan, one streak, one
widget, one export. Two readings at once means two of each, and every screen that says "today's
portion" has to say *which*. That is a data-model change, not a feature — which is precisely why
it is recorded here rather than started.

**⛔ 1 and 2 are blocked on task 14, not scheduled.**
*Finishing a sūrah congratulates you and offers to hear the whole thing back*, and *a weekly
check-in to recite the week's learning*. Both hinge on the words "so it checks for mistakes",
which is the half of task 14 that needs whisper — 45-57 MB, unproven on his voice. **They cannot
be honestly promised before that measurement exists**, and building the congratulation without
the checking would be the feature with its reason removed, which is how task 11 died.


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

| ⚠ **Task 12, half-resolved 2026-08-18. Quran for Android: PROVEN. Tarteel: UNDETERMINED.** Their manifest exports `QuranForwarderActivity` with `<data android:scheme="quran"/>`, and the activity splits the URI string on `/`, taking the first numeric segment as sura and the second as ayah — so `quran://18/10` opens Al-Kahf 10. Read from their GPL source, not guessed. **Tarteel is closed source and publishes no scheme**, and nothing in its listing or docs documents one. | The Quran for Android link is built and the action appears **only when that app is installed**. Tarteel is **not built**: absence of a published scheme is not proof there is none, and a button that merely launched its home screen would look like a deep link without being one. **What would settle it:** install the APK and read its manifest with `aapt dump xmltree`. |
| ⬜ **The deep link's *positive* path has never run.** Verified on the emulator that with no handler installed the button is correctly **absent** — the toolbar shows Bookmark, Play, Share, Close and nothing else. But nobody has yet seen it appear and open the right ayah, because Quran for Android cannot be installed on this emulator. | Check it on the first real phone that has Quran for Android. The URI shape is unit-tested against their documented parsing (check 37), so the risk is in resolution and `<queries>` visibility rather than in the string. |

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

### 5o. ⚠ The first Home port failed, and why. 2026-08-18

Shown the built screen, Mutalib rejected it: *"you didn't implement the same things, yours
looks terrible and not arranged well, it's not even divided, check the page and do it well."*

**The diagnosis, because the mistake is repeatable and worth naming.** The port was built by
reading the design's **text order** off the rendered page — the list of strings, top to
bottom — and reproducing that order with `Text` and `Spacer`. The order was right. Everything
that held it *apart* was missing, because none of it is text and none of it showed up in a
text dump.

**What a text reading cannot see, and what was therefore absent:**

| The design has | The first port had |
|---|---|
| Toolbar on its own raised ground, full-bleed | Two lines of text floating on the page |
| Portion as a section closed by a full-width hairline | A rounded translucent card |
| A progress bar through the mushaf | Nothing |
| The sūrah numeral badge (`١٨ · SŪRAH`) | Nothing |
| The three done controls, in the section | Nothing |
| Companion card at radius 14, chips as **full pills** | Radius 6, chips as rounded rectangles |
| A gold dot separating the two figures | A gap |
| `THIS WEEK` in a tinted band over ruled rows | Plain text and loose rows |
| A left marker per row: **filled = recited, hollow = tapped** | Only the words said which |

**The rule this produces, and it should outlive this screen: read a design's STRUCTURE, not
its STRINGS.** `get_page_text` gives the order and nothing else. The structure lives in
computed borders, backgrounds, radii and padding, and it has to be read out of the DOM
deliberately — which is what fixed it: walking the after phone's element tree and dumping
`borderTopWidth`, `backgroundColor`, `borderRadius` and `padding` per node.

**Sections are separated by edges, not by empty space.** That is the whole difference between
a screen that reads as divided and one that reads as a long column of text.

**What was ported, and what was refused:**
- ✅ The structure above, in full.
- ❌ **The crimson.** The design fills its recite control with `rgb(86,12,21)` — the
  Nutcracker `--crimson` § 6/8 already ruled on: *strip what is unused rather than porting
  the whole token set.* Our accent carries it instead.
- ⚠ **The juzʾ number** is in the design's toolbar and is not here. Home does not have it
  without loading the page, and § 10 says a number is measured, not guessed.
- ⚠ **Only "I read it" completes the day from Home.** Recite and Listen open the page,
  because the recorder and player both live in `TodayScreen` — see § 5j. Honest, not ideal.

**Verified on the emulator in both themes**, which was an accident worth keeping: a stray tap
during testing left the app in Paper, and the layout held — with `accent` correctly resolving
to ink-soft rather than gold, exactly as § 6b requires, since gold on cream is 2.06:1.

### 5p. Tajweed and mistake-marking — his question, answered 2026-08-18

He asked whether the tajweed / mistake-correction idea was recorded. **It is** — § 5a, in his
own words, and the verdict there still stands. Repeated here because he had to ask, which
means it was buried:

- **Post-hoc analysis is fine.** The old objection *"live word-by-word following is ~5×
  slower than real time"* does **not** apply — he wants record → finish → review, which is
  what task 14 already is.
- **Naming the error type is not.** `whisper-base-ar-quran` transcribes; it does not judge
  tajweed. It can say *where you stopped matching the expected words*. It cannot say whether
  a madd was held long enough. Promising tajweed feedback it cannot deliver would be the
  worst failure this app could have.

**And the memorising/reading question (§ 5l) now has its answer:** he said it *"has to do with
the audio"*. It selects what the check compares against — **reciting from memory, with no
page, versus reading from the mushaf.** That is a real, buildable distinction and it removes
the § 5l blocker. It lands with task 14, not before it.

### 5q. The companion is a chat now — built 2026-08-18

§ 5m approved it; this is what shipped. `ChatScreen.kt`, `Companion.kt`, plus
`domain/Conversation.kt`, `data/ConversationStore.kt` and `domain/CompanionReply.kt`.

**What makes it read as a chat rather than a card with an input:**
- **Turns persist.** `chat.json`, plain JSON beside `days.json`, capped at 120 turns. A
  conversation that forgets itself between launches is a form, not a chat.
- **Sides and tails.** Its lines left on a raised ground, yours right and outlined, with the
  corner *opposite* the speaker squared to 2dp against 6dp. That squared corner is what makes
  two boxes read as two people talking.
- **Bubbles wrap to their words**, capped at 84% width via `BoxWithConstraints`. A fixed
  `fillMaxWidth(0.84f)` would stretch "at 9" across most of the screen.
- **The input is pinned to the bottom of the screen**, where every messaging app has taught
  people to look.
- **The promise is held in view** — `YOU SAID "after Isha"`, the real armed time, and a
  breathing dot reading *"Holding since 3:33 am · reminder moved to match"*. Your own words
  at display size, not "reminder set for 8:00 pm": that is what makes it a promise rather
  than a setting, and it is the whole of what § 5b is testing.

**Where it lives, and why both.** The chat is a full screen reached from Home's card. Home
keeps the input, so a commitment is still zero taps away — § 2's procrastination finding
means anything added between him and answering costs something. The card shows the last two
turns as bubbles so the two surfaces are visibly the same object.

**One owner for the loop.** `MainActivity.said()` logs your line, understands it, acts, then
logs the reply. Both surfaces only forward raw text. Your line is written **before** the
action runs, so a crash mid-action still leaves what you said on disk — PLAN task 21 cannot
measure commitments made against commitments kept if the making was never recorded.
`replyFor` moved out of `HomeScreen` into the domain for the same reason: one sentence, two
screens, one log.

**⚠ The gap is unchanged and is the honest risk.** `CompanionBrain` is still hand-written
rules. A chat box invites anything; the parser understands a handful of phrasings. Mitigated,
not solved: the not-understood reply points at the shortcut chips rather than apologising,
and the chips stay on screen as a standing hint. **Typing *"what does this surah mean"* gets
a shrug** — which is § 5h's approved feature and is flatly impossible on rules. **PLAN task
22's engine question is still open and still his.**

**Three things found by running it, not by reading it:**
1. **The send button was unreachable with the keyboard open.** `safeDrawingPadding()` already
   includes the IME inset; adding `imePadding()` counted the keyboard twice. Fixed.
2. **`org.json` is a throwing stub on the JVM unit-test classpath**, which silently made
   `DayLogStore` and `ConversationStore` — the two classes holding this app's actual records —
   untestable without an emulator. Fixed with a `testImplementation("org.json:json")`; nothing
   reaches the APK. Android's suggested alternative, `isReturnDefaultValues = true`, makes the
   stubs return null instead of throwing, which turns "this never ran" into a passing test.
3. **The humanizer gate failed on the first draft** — four em dashes, then fourteen curly
   quotes once those were fixed. The real fix was not punctuation: the strings were listing
   options in prose while the shortcut chips sat directly underneath showing those same
   options. They now point at the chips. Shorter, and it stopped saying everything twice.
   ⚠ One of the em dashes (*"Alright — I'll ask again"*) predated this session and had never
   been gated, because until now the companion's strings had never been run through it.

**QA checks 21–24 added** (24 tests total): the promise is repeated back rather than
acknowledged, the not-understood reply carries an example and a pointer, Sacred Rule 3 is
asserted by *banned words* on a refusal (`streak`, `failed`, `sure?`, `missed`), and the log
round-trips, rejects whitespace and stays capped.

### 5r. Memorising or reading — built 2026-08-18

§ 5l recorded the ask and left it blocked on one question: *what actually changes when
someone answers "memorising"?* His answer settled it — *"it has to do with the audio"*, i.e.
it selects what a recitation is checked against: from memory with no page, or read from the
mushaf.

**⚠ That check is PLAN task 14 and does not exist yet.** So this ships as the question, the
setting, and the wording — and the substantive behaviour lands with task 14, where his answer
says it belongs.

**§ 5l's own warning was the thing to design around:** *storing an answer the app ignores is
worse than not asking, because it promises something.* Two decisions follow from it:

1. **It changes what the app calls things, today, on every screen that names the action.**
   Not nothing: a memoriser pressing a button labelled *"Recite it out loud"* while
   deliberately not looking at the page is being described wrongly by their own app.

   | | Reading | Memorising |
   |---|---|---|
   | recording route | Recite it out loud | **Recite from memory** |
   | tap route | I read it | **I revised it** |

2. **The setup screen does not oversell it.** It says *"So Wird uses the right words for what
   you're doing"* and *"You can change this later in More"* — both true. Promising a
   memorisation mode that does not exist would be the failure § 5l named.

**The design settled the shape, so this is not invented.** `Wird v2.dc.html`'s hifdh screen
says it in one line, kept verbatim in the code: *"the same loop, pointed at revision.
Reciting aloud is still how a day gets marked."* This is a lens on one mechanic, not a second
app — which is also why the Memorisation *screen* in that file stays unbuilt and stays filed
under its own "Beyond v1".

**Where it lives:** step two of first run, after the name and before the sūrah. Two cards
rather than a toggle, because a toggle implies one side is the default and this question has
no default worth implying. Changeable afterwards at **More → Reading → How you read**,
following § 5e's value-and-chevron pattern.

**Defaults to READING** — the larger group, and the value every existing screen already
assumes, so anyone who upgrades into this or never sees the question gets exactly the app
they had.

**What was deliberately NOT done:** the amount step still offers the same options in both
modes. Smaller portions for memorisers is plausible and might be right, but nobody decided
it, and § 5i says ask rather than infer. The `mode` parameter was removed from that screen
rather than left unused as a hint that something was half-built.

**QA check 25** asserts the thing that actually matters here: **Sacred Rule 6 survives the
rename.** Whatever the buttons are called, the recording route and the tap route must never
describe each other — the test fails if the tap label ever contains the word "recite".

**Verified on the emulator**, fresh install, choosing *From memory*: Home renders "Recite from
memory" and "I revised it", and More shows `How you read · FROM MEMORY`. The name was skipped
on that run, which also confirmed the fallback — the greeting reads a bare "Good morning" with
no invented name.

## 6d. PALETTE — teal and manila, after Quran for Android. Pinned 2026-08-18

**Sacred Rule 8 reversed a second time, in two days, on Mutalib's explicit word.** He was
told plainly in the asking that this was the second reversal and that it costs Wird its own
look in exchange for someone else's. He chose it anyway. It is his call, it is canon, and
§ 6c stays as the record of the gold direction rather than as something to drift back to.

**Read from source, not from screenshots** —
[`values/colors.xml`](https://github.com/quran/quran_android/blob/main/app/src/main/res/values/colors.xml)
and `values-night/colors.xml` in quran/quran_android (GPL-3.0). Colour *values* are data, not
code, so nothing here is a derivative of their source; the debt is named rather than hidden.

### Light

| Role | Hex | Measured |
|---|---|---|
| surface (chrome) | `#FAF8F7` | theirs |
| **page (mushaf)** | `#FFF4CB` | theirs — warm manila, not white |
| textPrimary | `#212529` | 14.57:1 AAA |
| textSecondary | `#656E76` | 4.90:1 AA — **darkened from theirs** |
| accent | `#00767F` | 5.09:1 AA — **darkened from theirs** |
| textOutsidePortion | `#848B93` | 3.13:1 on the page, deliberately |
| surfaceRaised | `#F1F3F5` | secondary 4.67, accent 4.84 |
| band (Juz′ rows) | `#DEE2E6` | theirs |

### Dark — adopted whole, no correction needed

| Role | Hex | Measured |
|---|---|---|
| surface | `#212121` | theirs |
| page | `#1A1A1A` | ours, just off the chrome |
| textPrimary | `#FFFFFF` | 16.10:1 AAA |
| textSecondary | `#B5B5B5` | 7.85:1 AAA |
| accent | `#B2DFDB` | 11.10:1 AAA |
| surfaceRaised | `#303030` | theirs |
| band | `#424242` | theirs |

### ⚠ The measurement that changed two of their colours

**Their light palette does not meet AA for body text on two roles**, computed rather than
assumed:

- their detail grey `#6C757D` on their surface `#FAF8F7` → **4.43:1**, under the 4.5 floor
- their teal `#00838F` on the same surface → **4.27:1**, also under it

This is not a criticism of their app — those roles carry short labels there, and both clear
the 3.0 large-text floor. **Wird puts whole sentences in them.** So exactly two values are
darkened and nothing else is touched. Their *dark* palette needed no adjustment at all.

### ⚠ One rule this palette carries

On the light band `#DEE2E6`, `textSecondary` is **3.99:1 and fails**. A section label sitting
on a band must use `textPrimary`. On the dark band it is 4.90:1 and either works.

### Two things this fixed on the way past

- **The mushaf finally has its own ground.** PROFILE has claimed since § 6b that "the page
  stays cream in both modes"; the code painted it with `surface` like every other screen, so
  it never did. `WirdColors.page` makes the claim true.
- **The accent is now a different colour per theme**, deep teal on light and pale teal on
  dark, which is what the old gold needed and never got. It is why the old palette had a rule
  saying gold may never be a word on the light ground.

### What the highlights are for, taken verbatim

`highlightReciting` = `#4046A646` (their `audio_highlight`) and `highlightSelected` =
`#404694A6` (their `selection_highlight`). Both 25% alpha over a solid. Not yet used — they
belong to the reciting-highlight and select-a-verse behaviours Mutalib pointed at in his
reference screenshots, which are still to build.

### 5s. The sūrah list, after Quran for Android — built 2026-08-18

Three asks in one screen, all his, all from the reference screenshots he sent.

**1. The meaning beside the name.** *"Show the meaning of the surah next to the surah name,
like Surah An-Nisa (The Women)."*

**2. Bare page numbers.** *"Just write the number, don't bring the p there"*, and *"like 22 to
49, don't do it like that, just write 22."* So `pp. 22–49` becomes `2`, and Ali 'Imran is
`50`. The range was honest but it answered a question nobody asked — you tap a sūrah to reach
its **beginning**, so where it ends is not the number you need.

**3. Juz′ section bands**, full-width, `Juz' 18` left and its start page right.

### Where the data came from, and why that mattered

`SurahIndex` needed three columns it did not have — meaning, verse count, and Makki/Madani —
plus a juz′ table it did not have at all. **None of it was typed.**

- Sūrah columns: regenerated from `/chapters?language=en`, the same endpoint the file's own
  provenance line has always named.
- Juz′ start pages: fetched one at a time from `/verses/by_juz/{n}?fields=page_number`,
  taking the page of each juz′'s first verse.

**The regeneration was diffed against the old file before it replaced it** — 114 rows, on
number, name and both page numbers: **zero mismatches.** So the new columns are additions to
data already known good, not a re-derivation that might have quietly moved something.

**And the juz′ numbers were confirmed against his own screenshots.** The API gives Juz′ 18 →
342, 19 → 362, 20 → 382, 21 → 402. All four match what Quran for Android shows him. Two
independent sources agreeing is worth more than either alone; QA check 28 asserts it.

### Two things found by building it

**"Hud (Hud)".** Six sūrahs are named for a person or a word, and the API's translated name
is that same word again — Hud, Taha, Luqman, Ya-Sin, Muhammad, Quraysh. Each read as a
stutter. Fixed with a rule rather than a list of six numbers: the parenthetical is dropped
when the meaning folds to the same string as the name, folding away case, spacing, hyphens
and diacritics, which is what makes "Ya-Sin" match "Ya Sin". **Caught by looking at the built
screen, not by reading the data.**

**The bands needed the list to run edge to edge.** `SurahsTab` had inset the whole column, so
a band would have stopped short of the screen and read as a card rather than a section rule.
All three callers of `SurahList` — the tab, setup's picker, and the jump sheet — now inset
their own headings and let the list run full width.

### ⚠ The measured rule this screen obeys

Both strings on a Juz′ band use **`textPrimary`, not `textSecondary`**. On the light band
`#DEE2E6` the secondary grey is **3.99:1 and fails AA**; primary is 11.85:1. § 6d.

### Deliberately not done

**Bands are suppressed while searching.** Four results scattered under four headings is
harder to read than four results.

**The search field is still an inline box** rather than the magnifier in a toolbar that the
reference has. That belongs to the navigation work — asks 1 and 2 — and is queued behind
this, not forgotten.

### 5t. Tabs to the top, three of them, and More becomes a menu — 2026-08-18

*"As for the tab, I think it should come top rather, like the after one in Claude Design, but
in time it will be home, surahs and history. For the home there will be that three vertical
dot menu or hamburger button with the settings and other things there."*

**Three changes, one shape.**

1. **The bar moved to the top**, modelled on Quran for Android: a title row carrying the
   app's name and an overflow, and a row of tabs under it marked by an underline.
2. **Four tabs became three** — Home · Sūrahs · History.
3. **More stopped being a place and became a menu.**

### Why "More" was the right one to lose

Settings was never a destination you visit *alongside* your wird. It is a drawer: you open
it, change one thing, and leave. It was spending **a quarter of the navigation bar** on that.
The overflow is where it belongs, and losing it is what makes three tabs possible without
hiding anything.

### Why an underline and not the old icon-over-label bar

**Because the same control means a different thing at the top of a screen.** At the bottom,
tabs are thumb targets and want mass — which is why § 5b's version had icons above labels
and Mutalib was right to ask for them. At the top they are a *heading*: you read them once
and then read past them, so weight there competes with the screen's own title. The underline
says which one without adding a second bold thing to the page.

**Colour is still not the only signal** — the active tab is accented *and* full-weight *and*
underlined, same rule as the old bar.

### What is in the overflow, and why only two things

**Night mode**, as a checkbox, and **Settings**. Both are real and both are wired to state
that already existed; nothing was invented to pad the menu out.

The night-mode checkbox is taken straight from the reference — Quran for Android puts it in
the overflow so it is toggled *where you are* rather than three taps into settings. ⚠ It reads
[isDark] rather than the stored setting, because `SYSTEM` has no answer of its own and a
toggle has to know what is actually on screen. **The reading page's own toggle is a separate
ask (7) and is not this.**

**The overflow only appears on Home.** On Sūrahs or History it would open a menu about a
screen you are not looking at.

### Two consequences that had to be handled

- **The bar hides while the mushaf is open.** The page is the one screen that should have
  nothing parked above it — Sacred Rule 5's surviving half.
- **The old bottom bar was carrying `navigationBarsPadding()`.** With it gone the shell owes
  the gesture bar its space, or the last row of every screen sits underneath it.

### Not done here

The reference also puts **search in the toolbar** as a magnifier. Wird's sūrah search is
still an inline box at the top of that list. Left alone deliberately: it is a change to the
Sūrahs screen rather than to navigation, and nobody has asked for it.

### 5u. Light and dark, matching the phone — 2026-08-18

*"For the home page it matches with the phone, but the pages you can toggle dark mode easily
there. And that ink and paper you did, the user won't know — just do it light and dark mode."*

Three changes, and the first one had been impossible until yesterday.

### 1. The default now follows the phone

`ThemeMode.SYSTEM` is the default for a fresh install.

**The reason it could not be before is gone, and it is worth being precise about why.** The
default was forced to DARK on 2026-08-17 because that palette's gold measured **2.06:1 on
cream** — unreadable — so a light-by-default app would have been one that never showed its
own accent. § 6d replaced gold with teal and measured it at **5.09:1 on the light ground,
AA**. The constraint was a property of the old colours, not a preference, and it left with
them. Verified on a fresh install with the emulator set to light: the app came up light
without being told to.

Anyone who has already chosen keeps their choice. This only changes what a new install does.

### 2. "Paper" and "Ink" are gone

Now **Light · Dark · Match phone**. He is right and the reason is plain: those words were
chosen to describe the *old* palette, where the app really was ink on paper. They described a
metaphor the user was never told about, and the metaphor no longer holds anyway — the app is
teal on near-white now, not ink on cream.

⚠ Note the roles are unchanged: `ThemeMode.LIGHT` and `ThemeMode.DARK` still mean what they
meant. **Only the words a person reads changed.**

### 3. The reading page got its own night toggle

Straight from the reference — Quran for Android puts `Search · Night mode ☐ · Go to page ·
Settings` behind one `⋮` on the reading screen, and Mutalib pointed at that screenshot for
exactly this: *"the pages, you can toggle dark mode easily there."*

**The chrome bar's two bare icons became one overflow**, holding **Night mode** (a checkbox),
**Read something else**, and **Settings**. That is a simplification the bar needed anyway: it
is already three lines of text deep and it sits over the Qur'an.

**Listen stays a direct icon, not a menu item.** Task 9 put it in the bar deliberately — it is
the only stop control, and a stop button hidden behind a menu is one you cannot find while
audio is playing.

**Both toggles read [isDark] rather than the stored setting**, because `SYSTEM` has no answer
of its own and a checkbox has to reflect what is actually being painted.

### What this finally made visible

**The mushaf now looks like a separate object.** § 6d gave the page its own colour and this
is the first screen where you can see it: manila `#FFF4CB` inside near-white `#FAF8F7` chrome
in light, and `#1A1A1A` inside `#212121` in dark. PROFILE has claimed the page was a lit
object inside the app since § 6b; until yesterday the code painted it with `surface` like
every other screen.

### 5w. The home-screen widget — PLAN task 10, built 2026-08-18, placement unverified

The last task in Milestone 2. The plan's own words for why it exists: *"the nudge that cannot
be swiped away or killed."* Every other answer to the forgetting half of § 2 is interruptible
— a notification is swiped, an alarm is lost to a battery optimiser, and on the Transsion
phones the testers carry both happen routinely. A widget is not delivered to you; it is
already on the screen you unlock.

**What it shows:** today's portion, how much, its page, and whether it is done — with which
of the two ways it was done, because Sacred Rule 6 applies on the home screen too.

**What it deliberately cannot do: mark a day.** A button there is exactly where "done"
becomes a reflex rather than a recitation. Tapping opens Wird, and that is all.

**It reads the same two stores the app does**, with no cache and no service. It also repeats
the app's rule that a finished day shows *what it covered* rather than what the position now
says, so marking a day cannot rewrite what today was.

**Correctness comes from being pushed, not from polling.** `updatePeriodMillis` is 0. The
platform clamps that setting to thirty minutes and ignores it entirely while dozing, so
relying on it would mean a widget that is right only sometimes and spends battery being
wrong. `refreshWidget()` is called from the four places that change what it displays —
marking done three ways, and undo. ⚠ **Undo needed adding separately**: it sets `doneMethod`
directly rather than re-reading it, so it missed the refresh the other three got, and the
widget would have gone on claiming a day was done after it was undone.

**Measured, per § 10: 264 KB.** The debug APK went 17.27 MB → 17.52 MB, taken by building the
same tree with and without Glance. Far less than "it ships its own runtime" implies, and less
than one mushaf page of audio at 128 kbps.

**Three things the library and the platform did not do the way the docs imply**, all found by
reading the artifact rather than guessing:
- **Glance 1.1.1 has no day/night `ColorProvider`.** Only a single-colour one. So the theme is
  resolved in `provideGlance`, which is better anyway — the widget follows *the app's* setting,
  so forcing Dark while the phone is light does not leave a light widget beside a dark app.
- **`actionStartActivity<T>()` lives in `androidx.glance.action`**, not the appwidget package,
  where only the `Intent` overload exists.
- **`targetCellWidth` / `targetCellHeight` are API 31+** and lint fails the build on them at
  minSdk 26. Split into `res/xml-v31/` rather than exempting the check. The picker showing
  "3 × 2" is the proof that versioned file is the one being read.

✅ **VERIFIED 2026-08-18, placed by hand on the emulator.** It renders `TODAY'S PORTION` /
`Al-Fatihah` / `One page · page 1` / `Not yet marked`, and `dumpsys appwidget` reports a bound
instance carrying real `RemoteViews` rather than the loading layout. ⬜ The other half of the
done-when — *"still shows the right thing the next morning"* — needs a night to pass, **and
needs a Transsion phone rather than the emulator**, since surviving the OEMs that kill
background work is the entire point of this task.

**Kept as a note on tooling, because it cost several attempts:** placing a widget over adb
does not work. `input swipe`, `input draganddrop` and a hand-built motion-event sequence all
failed to bind it — the launcher wants a real long-press-and-drag. A hand does it in five
seconds. Do not spend turns on this again.

*(The paragraph below was written before it was placed, and is kept for the record.)*

⬜ **NOT YET VERIFIED, and the task is not ticked because of it.** PLAN task 10's done-when is
*"the widget sits on the Pixel home screen showing the correct portion, and still shows the
right thing the next morning."* What is proven: the provider registers, the launcher lists it
with the right label, size and description, `check.ps1` passes, and the code paths are wired.
What is **not** proven is the thing that matters most — that it renders correctly once placed.
Placing a widget over adb means fighting the launcher's own drag-and-drop, and several
attempts with `input swipe`, `input draganddrop` and a motion-event sequence all failed to
bind it. **A hand on the screen does this in five seconds**, so that is the honest next step,
and the "next morning" half needs a night to pass regardless.

### 5x. The two highlights, after the reference. 2026-08-18

Both colours had been sitting in the palette unused since § 6d. Now they are on the page.

**Green while reciting** (`#46A646` at 25%, their `audio_highlight`) and **blue while selected**
(`#4694A6` at 25%, their `selection_highlight`).

**This replaced task 9's mechanic rather than joining it.** That version marked the ayah being
recited by *dimming every other word in the portion*. It worked, and it was wrong for a reason
worth keeping: dimming is already how the page separates today's portion from the rest of the
mushaf, so during playback the screen carried two meanings of "pale" at once. A wash is a
different channel, so the two stop competing. The reference does it this way and the reference
is right.

**⚠ The first attempt looked wrong and had to be redone.** Giving each highlighted glyph its
own background works, but the glyphs sit in a `SpaceBetween` row, so the word-gaps stayed
unpainted and the highlight read as stepping stones instead of a band. I had written a comment
calling that deliberate — it was not, it was the easy version. Now each highlighted glyph
reports its position with `onGloballyPositioned` and the row paints **one rounded rect** across
the run in `drawBehind`.

**It is per-run, never per-line, and the screenshot proves it.** A mushaf line usually carries
the end of one ayah and the start of the next. With ayah 3 playing, the green band covers only
its words and stops where ayah 4 begins on the same line. A per-line implementation would have
lit both.

### 5y. The verse toolbar — long-press an ayah

Ported from the reference: hold an ayah, it tints, and a small toolbar appears.

**Only what the app can actually do is on it.** Theirs carries bookmark, tag, share,
translation and play. Wird has **play and share**; bookmarks and translations do not exist yet,
and three dead buttons would be worse than two live ones. The row grows when the features do.

**Long-press, not tap.** A plain tap on the page still belongs to the handler that reveals the
chrome, so selecting an ayah cannot happen by accident while reading.

**Anchored to the foot, not floated over the word.** The reference pins its toolbar to the
selection, which needs the word's screen position; the page is a pager of measured glyph rows
and chasing that would be guesswork. A fixed anchor is honest and always reachable.

**⚠ Share sends a reference and a link, never Qur'anic text.** Sacred Rule 2 governs what
leaves this app as much as what enters it — and the app has no verified plain-text copy of the
words to quote, only glyph codes for a per-page font. Anything it "shared" as text would be
reconstructed. So it shares `Al-Kahf 18:10 — https://quran.com/18/10` and lets a published
source carry the words.

### 5z. Translations — decided 2026-08-18, built after bookmarks

His sequence: talk about translations, build bookmarks, then build translations. This records
the decisions so the build does not re-open them.

**⚠ The translation in his reference is not available, and this is the first thing to know.**
His screenshot shows **Dr. Mustafa Khattab (The Clear Quran)**. Quran.com's API v4 offers
**nine** English translations and Khattab is not among them — verified against the full list of
126 resources, and requesting the id directly returns a 404. The likely reason is licensing:
The Clear Quran is copyrighted, and quran.com has permission for their own site rather than for
API redistribution. **The absence is verified; the reason is inference.**

**What he chose, from real sample text rather than from descriptions:**

| Source | id | Why |
|---|---|---|
| **Saheeh International** | 20 | Plain modern English, no archaisms, no footnote debris, closest in register to the Khattab text he pointed at |
| **Gumi (Hausa)** | 32 | Sheikh Abubakar Mahmud Gumi — the standard Hausa Qur'an across northern Nigeria and Muslim northern Ghana |
| **Transliteration** | 57 | Shown as-is |

**Rejected with reasons worth keeping:** Maududi (95) carries footnote markers as literal
characters in the text, so it renders stray digits linking to nothing; stripping them loses the
commentary that is the only reason to pick it. Pickthall (19) and Yusuf Ali (22) are archaic
("Thee", "Thine aid"). Hilali & Khan (203) is heavy with parentheses.

**⚠ He chose the transliteration knowing how it looks.** Quran.com's scheme writes ʿayn as
`AA` — `Iyyaka naAAbudu wa-iyyaka nastaAAeen`. It reads as an encoding bug to anyone who does
not know the convention. **It ships unaltered.** He was offered a cleaned-up version and did
not take it, which is the right call: rewriting a published source's text is Sacred Rule 2
territory even when the text is transliteration rather than Qur'an.

**Placement: a second reading mode.** A toggle in the page's overflow — Mushaf or Translation.
**Not interleaved into the mushaf page, and that is a hard constraint rather than a preference:**
the page is drawn as fixed glyph lines from a per-page QCF font, and inserting English between
the lines destroys the thing that makes it a real mushaf. His own reference agrees — its
translation view is a separate screen.

**Bundled, not fetched.** His call, and the measurement is what made it reasonable: **3.9 KB
per page**, averaged over five pages spread across the mushaf, so **~2.3 MB for a complete
translation**. For scale, **one page** of Shatri at 128 kbps is 2.37 MB — the whole Qur'an's
translation costs less than a single page of its audio. Three sources is ~7 MB, taking the APK
from 17.5 MB to roughly 24 MB. He was told that number before choosing.

**Consequence to accept rather than discover:** bundling means changing or adding a translation
is a new app version, not a setting. That is the trade he took in exchange for it working
offline from the moment it installs, which on Ghanaian mobile data is the more valuable half.

**For later, unblocked by the same path:** § 5h's tafsir. Three English ones exist — Ibn Kathir
(Abridged) `169`, Ma'arif al-Qur'an `168`, Tazkirul Quran `817`. Same fetch-and-attribute
requirement, and § 5h's rule stands absolutely: **fetched and attributed on screen, never
generated.**

### 5aa. Bookmarks — built 2026-08-18

His sequence was translations-discussion, then bookmarks, then translations. This is the middle
one.

**Long-press an ayah → the toolbar → save it.** The store is a JSON file beside `days.json` and
`chat.json`, holding only the verse key and when it was saved.

**Only the key is stored, never the words.** "18:10" is nine bytes and it cannot drift from the
source; a copy of the Arabic sitting in a second file could. Sacred Rule 2 applies to what the
app *keeps* as much as to what it displays. There is a test asserting every stored bookmark
matches `\d+:\d+` and nothing else.

**Newest first, and no folders, tags or notes.** The reference's toolbar has a tag button;
Wird's does not, deliberately. An ayah you saved is either still worth returning to or it is
not, and every organising feature is a thing to maintain instead of a thing to read.

**Where they live: the top of the Sūrahs tab, not a fourth tab.** The reference makes BOOKMARKS
a tab beside SŪRAHS and JUZʾ, but § 5t fixed the count at three on his instruction and a fourth
was not asked for. That tab is already "read anywhere you like", so a saved ayah belongs at the
top of it. **The section disappears when empty** rather than showing an empty state — a heading
over nothing teaches you the feature exists at the cost of a permanent hole.

**⚠ A star, not the reference's ribbon.** There is no bookmark glyph in `material-icons-core` —
147 icons, checked in the artifact — and `app/build.gradle.kts` rules out
`material-icons-extended` because it is several megabytes for a handful of shapes. Filled versus
hollow star is the same idea in an icon already present.

**⬜ Known limitation, written down rather than hidden:** the saved row shows the *surah's*
opening page, not the ayah's real page. An ayah deep in Al-Baqarah displays 2. Resolving it
properly needs that ayah's page layout, which is a fetch or a cache read per row, and this is a
list meant to be glanced at. **Tapping still lands on the right ayah**, because that lookup
happens once, on open.

**⚠ A bookkeeping mistake worth recording, since this repo's commit messages carry the
reasoning:** the bookmark work was verified on the emulator but never committed on its own, and
`git add -A` swept it into **8f3f77a**, whose message describes only the Juzʾ band fix. Pushed
history was left alone rather than rewritten — CLAUDE.md prefers a new commit to an amend — so
this paragraph is the correction. **Verify, then commit, before starting the next thing.**

### 5ab. Translations — built 2026-08-18

§ 5z decided it; this is what shipped. `Translations.kt`, `TranslationScreen.kt`, a
`tools/fetch-translations.py`, and 1,812 bundled asset files.

**A second reading mode, reached from the page's ⋮: Mushaf ⇄ Translation.** Each ayah shows
its number, then its Arabic, then English, Hausa and the transliteration, each labelled — and
**an attribution bar pinned at the foot that never scrolls away**: Saheeh International,
Abubakar Mahmud Gumi, Quran.com. Sacred Rule 2 in its plainest form: translations differ
theologically, so a reader is entitled to know whose reading they have.

**The Arabic is the page's own glyphs, filtered to one ayah.** Wird holds QCF glyph codes for
a per-page font, not readable Arabic — the same fact that makes `share` send a link rather
than words. Rendering the glyphs is the only way to show Qur'anic text without reconstructing
it, and the font is already on disk for the mushaf view, so it costs no extra data.

⚠ **The lines do not match the printed mushaf here, and that is correct.** A mushaf line is a
typesetting fact about a page; an ayah is a unit of meaning. This screen is organised by ayah,
so glyphs wrap to the phone's width. The printed layout is one tap away.

**The mode is not persisted**, on purpose: the mushaf is what Wird is for, and a session that
ended in translation mode should not open there tomorrow.

### Four things that went wrong, all worth keeping

**1. ⚠ The footnote trap, which nearly shipped.** The API returns
`<sup foot_note=195932>1</sup>`. Stripping tags alone leaves the digit welded to the previous
word: *"In the name of Allāh,1 the Entirely Merciful"*. Measured before it shipped: **1,400 of
Saheeh's 6,236 verses**, so 22% of the Qur'an. **This is exactly the flaw Maududi was rejected
for in § 5z**, about to ship in the translation chosen instead of it. The fix removes the tag
*and its contents*. Verified after: **0 of 6,236**.

**2. ⚠ Never edit a regex through a shell heredoc.** Three attempts to patch the cleaner that
way turned `\b` into a literal **backspace byte** and a `\1` backreference into a **0x01
control character** — one of which would have deleted punctuation rather than a space. `cat -A`
was the only thing that showed it. Fixed by rewriting the file with the editor instead.

**3. ⚠ A retry that caught the wrong exception, twice, silently.** The fetcher caught
`urllib.error.URLError`, which does **not** cover `http.client.RemoteDisconnected` — what
quran.com throws when it has had enough. Two runs died at pages 398 and 403 with **exit code
0**, so they looked successful until the page count was checked. Now catches
`http.client.HTTPException` and `ConnectionError` too, retries six times, and paces requests.
**A completed run that wrote 403 of 604 files is the kind of failure this project is most
likely to miss.**

**4. ⚠ A regression the long-press work introduced, found here.** Adding `combinedClickable`
to every glyph meant each word **consumed** the tap — and on the reading page `onWordTap` is
null, so tapping the page stopped revealing the chrome bar. The code comment claimed the
opposite. A glyph with no tap action of its own now hands the tap to the background handler by
name.

### The size, measured three ways because the first two disagreed

| | |
|---|---|
| Text on disk | **2.66 MB** across 1,812 files |
| Compressed inside the APK | **1.30 MB** |
| **APK growth** | **136 KB** — 17.52 MB → 17.66 MB |

Those look contradictory and are not. The APK already carried **4.72 MB of zip alignment
padding**; the translation entries largely fitted into existing slack, so the file grew far
less than the data added. Verified by building the same tree with and without the assets, and
by confirming the "without" APK contains zero translation entries.

⚠ **That 136 KB is a debug build.** A release build re-aligns after R8, so the real-world
delta could land nearer the full 1.30 MB. Do not quote 136 KB as the shipping number.

**Integrity check worth repeating for any future data import:** the fetched Saheeh text has
**exactly 6,236 verses**, which is the Qur'an's verse count. A count that matches the thing
itself is worth more than a byte total.

### 5ac. Two decisions closed. 2026-08-18

**1. The companion's engine: hand-written rules now, Gemini later.**
His words: *"for now lets use the free private offline then later we use gemini."*

⚠ **Gemini specifically.** Not Claude, not an on-device model. Recorded exactly, because a
later session substituting a provider would be choosing on his behalf, and § 7 is his call.

Rules-first is also the right order on its own merits: § 5b says that if naming a time to the
app does not move the 8-of-14 procrastination days, the companion is theatre and gets cut
regardless — so paying for an engine before task 24's evidence risks paying for something that
gets deleted.

⚠ **What is therefore not promised until Gemini lands:** § 5h's tafsir, explaining and daily
reflection are impossible on a parser. Asking the chat what a surah means gets a shrug. **And
when Gemini does land, Sacred Rule 2 still binds absolutely** — the ayah, its translation and
its tafsir are FETCHED from a named source and never generated. A model may summarise a fetched
tafsir; it may not author one. That rule does not soften because the engine got better.

**2. Task 11 is deleted, at his word.** § 5a had already reasoned it out; he confirmed. The
task's whole premise was that *sending* a recitation to someone was the accountability, and he
will not be sending recordings to anyone. A generic share button would have been the feature
with its reason removed. **The share plumbing survives** — the FileProvider built for task 19
is scoped to the export folder and does the useful half.

### 5ad. Plain words instead of buttons — PLAN task 22, built 2026-08-18. Task 13 folded in

The companion could already be answered in words; what it could not do was **change anything
about the plan**. It understood a promise about tonight and nothing about how much you read,
which days are lighter, when the reminder comes, or being away. § 5h listed all four as things
he chose. This is them.

**Task 13 is closed by this and was never built separately.** Its done-when — *five messy
sentences produce the correct schedule* — is word for word task 22's, and its example sentence
is the one the tests now use. Two parsers reading the same English would have been two places
to disagree.

#### His two decisions, taken before anything was written

**1. Away days stay absent from the record.** *"I'm travelling till Sunday"* stops the reminder
and writes **nothing at all** into `days.json`. The alternative offered was logging them as
"away" so the streak could bridge across the trip; he chose the version where a day in the
record means one thing only, which is a day he actually read. Sacred Rule 6 is easier to hold
when nothing else is allowed to live in that file. The streak simply restarts, and
`totalDaysRead` — which never resets — is untouched either way.

**2. ⚠ A commitment is one-off, and that fixed a real defect.** Until today, replying *"in an
hour"* wrote that time into the **daily** reminder, so a single three o'clock answer made four
o'clock the reminder time for every day afterwards, silently and with nothing on screen saying
so. His own phone had it: the stored `nudge_schedule` read `PRAYER ISHA 30` purely because he
once tapped "After Isha" from a notification. PLAN task 21 describes the mechanic as re-arming
"for that moment", so the routine was never meant to move.

The fix is where the schedule lives. It now rides **inside the commitment**, which expires with
the day it was made, and the routine underneath is untouched. Changing the routine has to be
asked for in words: *"move my reminder to 9 from now on"*, *"remind me at 9 every day"*,
*"stop reminding me"*.

**Why one-off is the safer default when the sentence is ambiguous:** a one-off that should have
been permanent costs one repeat. A permanent change that should have been one-off rewrites a
routine nobody asked to move, and nothing tells you it happened.

#### What it understands now

| You type | What moves |
|---|---|
| "one page a day", "make it half a page" | the daily amount |
| "half on Fridays", "make Fridays lighter", "back to normal on Fridays" | one weekday |
| "move my reminder to 9 from now on", "stop reminding me" | the routine |
| "I'm travelling till Sunday", "away for 3 days", "back on Friday", "next week" | a pause |
| "I'm back" | ends the pause early |

**Three instructions in one sentence, all three applied.** A sentence is read clause by clause,
so *"one page a day, half on Fridays, I'm travelling next week"* is three edits rather than one
edit and two silences. ⚠ **A clause it cannot read is quoted back**, which is the guarantee the
whole rule-based approach rests on: *"One page a day from now on. I didn't catch 'explain surah
yasin to me'."*

**"Till Sunday" means the reminder returns ON Sunday.** English is genuinely ambiguous there, so
the rule is written down and the answer always names the day it landed on. A wrong reading then
costs one sentence to correct rather than a fortnight of silence.

**The reminder comes back on its own.** A pause does not switch the alarm off, it sets it for
the far side of the trip — measured on the emulator: saying "im travelling till sunday" on
Tuesday the 18th stored `away_period 2026-08-18..2026-08-22` and armed the alarm for
**2026-08-23T20:00**, the Sunday. Nothing has to be running for an alarm to arrive, which is the
only version of this that survives a Transsion battery manager.

**A pause is visible and reversible without typing.** Settings grows one row while it is
running — *Paused while you're away · Back Sunday · tap to end*. A silence with no explanation
on screen is indistinguishable from the app being broken, which is exactly what task 15's
self-check exists to detect.

#### Two bugs the tests caught before the phone did

1. **"300 pages a day" answered "one page a day".** The amount parser could not read 300, fell
   through to the loose word matcher, found the "a" in "a day" and confidently set one page.
   Fixed by refusing to guess whenever a digit is present: a number that was meant to be read
   and could not be is a failure, not an invitation to use a nearby word.
2. **"move my reminder to 9" was not understood at all.** The time parser knew "at 9" and "by
   9" and not "to 9", which is how people actually write it.

Both were found by check 45, which exists to assert that it *refuses* to guess. The check that
tests for silence caught more than the checks that test for behaviour.

#### ⚠ A defect I reported and then disproved

Mid-verification I measured the keyboard's touchable region starting at y=1799 while the
companion's Send button sat at y 1873–1950, and concluded the button was unreachable on Home
with the keyboard open. It is not. A tap at the accessibility tree's own coordinates sends
normally; my earlier failed taps had used coordinates from a dump taken mid-animation, and they
landed on the keyboard's letter keys instead. Home's card is inside a `verticalScroll`, and
Compose scrolls a focused text field clear of the keyboard by itself.

**The speculative fix was reverted**, and the reason is worth keeping: `dumpsys input_method`'s
touchable region is larger than the visible keys, so it is not evidence about what a tap will
hit. The thing that settled it was tapping and seeing the message land.

#### Deliberately not done, so nobody assumes it works

- **"A juz a day."** Real phrasing, especially in Ramadan, but juz boundaries are not every 20
  pages, so honouring it as a fixed page count would be an approximation dressed as precision.
  It says it did not understand instead.
- **"Make weekends lighter"** as one phrase. One weekday per clause; two clauses do it.
- **A different reminder time per weekday.** "At 9 on Fridays" sets a one-off for tonight and
  ignores the Friday, because per-day reminder times do not exist in the app to be set.
- **Anything about the Qur'an's meaning.** Unchanged and still § 5ac's answer: fetched and
  attributed, or not at all. Sacred Rule 2.

## 6e. PALETTE — what his comps added, and the ground that was reversed. 2026-08-19

He brought four mockups of Home and pointed at them screen by screen. The colour question came
first, because the comps are **green** and § 6d's pinned palette is **teal**.

**Asked before a line was written, and he chose the middle option:** *teal stays, take the warm
cream ground and the tinted tiles.* Built, measured, shipped — and then, seeing it on the phone:
*"i think i like the white palatte better than this one."* So the ground went back.

### ⚠ The reversal, recorded because the cost was real

| | Built | Now |
|---|---|---|
| ground | `#F9F6EE` warm | `#FAF8F7`, theirs, as before |
| cards | `#F3F0E7` warm | `#F1F3F5`, Bootstrap gray-100, as before |
| tiles | four tints | **kept** |

**The half that survived is the half he never objected to.** Ink on all four tile tints was
re-measured against the white card: 12.84 to 13.34, unchanged — because ink-on-tint never
depended on what sits *behind* the tint. Nothing had to move.

**The lesson is § 6d's, again:** a palette is looked at on a device, not reasoned about in a
table. Two rounds of measurement cost less than one round of shipping the wrong ground. Asking
first is what kept this to a token swap instead of a rebuild.

### The four action tiles

⚠ **A tint alone is invisible and cannot be the whole idea.** Measured: every fill is
**1.01–1.08:1** against the card behind it, which is nothing. That is § 5o's rule in a new
costume — *a fill is not an edge*. So each tile carries a tint, a hairline of its own hue, and
a coloured icon, and **the icon is what actually distinguishes them.** Colour is never the only
signal: the label says what the tile does in words.

| Tile | Fill | Icon | Measured |
|---|---|---|---|
| Recite | `#E4EFEF` | `#00767F` | 4.59 |
| I read it | `#E7EAF4` | `#33518F` | 6.44 |
| Listen | `#F6EEDA` | `#866727` | 4.56 — **darkened** from `#8A6A2B`, which came back at 4.4 |
| Open the page | `#F3E9DF` | `#9C4B2E` | 5.07 |

Dark is designed rather than inverted: the hue survives, the lightness flips, and the icon
becomes the light member of the pair. White clears AAA on all four dark grounds.

### The chat bubbles, after WhatsApp

His instruction: *"the chat side, why won't you do it like how WhatsApp does it."*

⚠ **One measurement made these tokens instead of an alpha on the accent.** The ordinary
secondary grey `#656E76` is **4.01:1 on the tinted bubble and fails**, so the timestamp inside
your own bubble needs its own darker tone — `#5B646C` at 4.66. A detail small enough to have
shipped unnoticed, on the element that repeats most.

| Role | Light | Measured |
|---|---|---|
| your bubble | `#CFE7E7` | ink 11.93 |
| its timestamp | `#5B646C` | 4.66 — darkened, see above |
| the companion's bubble | `#FFFFFF` | ink 15.43, secondary 5.19 |

**⚠ The mushaf page is untouched by all of this and stays white.** He corrected exactly that
once already (§ 6d), and a warm or tinted ground under the Qur'an is the opposite decision from
one under the chrome.

### 5ae. Home, rebuilt from his four comps. 2026-08-19

He supplied four images and said which part of each to take: **image 1 is the base**, image 2
gives **the greeting and the check-in**, image 3 gives **the streak block**, and anything he did
not name comes from image 1. Two things in image 1 were explicitly refused by him — **the bottom
tab bar and the top-right gear** — which matches where those already live after § 5t.

**§ 5o's rule survived the restyle in a different costume.** That port failed for reading the
design's *strings* and losing everything that held them apart, and the rule it produced —
*sections are separated by edges, not by empty space* — still governs. What changed is which
edge: the old screen closed a section with a full-bleed hairline; his comps close each one
inside its own bordered plate. Both are edges. Neither is a gap.

**Drawn, not shipped:** the sun beside the greeting, the mushaf on its rihāl, the medallion
around the sūrah numeral, the microphone, the book, the speech bubble, the paper plane, the
crescent and the clock on the chips. The build file refuses `material-icons-extended` — *"a
several-megabyte library and we need three glyphs"* — and § 10 measures every byte, so the
illustrations are Canvas paths costing nothing to download.

**⚠ The comp's smiling avatar is deliberately not built.** § 5b: the companion is *"deliberately
not given a name, a face, or a personality claiming to be a person"*, because a bot that emotes
about someone's deen is worse than one that says nothing. **The substitute came from his own
image 1**, which uses a speech bubble in that same card.

**The Quran for Android button, his ask, lands on PLAN task 12's machinery** — the proven
`quran://sura/ayah` forwarder, read from their GPL source. It appears only when the package
resolves. ⚠ **Still unseen working**, and verified in the negative a second time: the package is
absent from this emulator, nothing resolves `quran://`, and the button is absent from the view
tree. Three facts agreeing is not the same as one phone showing the button.

**Three things found by running it rather than reading it:**
1. **The medallion read as a clock face.** Twelve strokes pointing inward from a ring is a
   clock, on a screen that is otherwise about time. Beads *on* the ring instead of ticks
   *across* it fixed it.
2. **The greeting rendered a dangling comma over an empty 36sp line** when no name is set —
   which is a real state, since § 5k makes the name optional with a Skip. The comma belongs to
   the name now.
3. **A screenshot disagreed with the accessibility tree**, and the tree was right. Same lesson
   as § 5ad's false keyboard alarm: measure the tree, and treat a screenshot as an illustration
   rather than as evidence.

### 5af. The page keeps its own light and dark. 2026-08-19

His words, describing the app he actually reads in: *"the home and the rest of the pages
automatically matches with the phone theme and is not dependent on the page… so Quran for
Android, but home and menu are in dark mode but page is light mode."*

**He is describing two surfaces with two different jobs.** The chrome is software and should
follow the phone. The mushaf is a printed object you are looking at, and someone reading at
night with a dark launcher does not necessarily want the Qur'an inverted.

⚠ **Before this, the page's own night toggle changed the WHOLE APP's theme.** Flipping it on
the reading screen turned Home and the menus dark as well, which is precisely why the two
could never disagree — there was only ever one setting wearing two labels.

**Now:** `WirdStore.pageNight` is the page's own preference, defaulting to **light**, and
`TodayScreen` wraps its entire body in that theme. The toolbar and the verse sheet come with
it, because a dark bar over a light page is two surfaces arguing.

**Verified on the emulator with the phone in dark mode:** Home, the tab bar and the check-in
card all painted dark; the mushaf page painted light in the same run. The device's night
setting was read first and restored afterwards.

⚠ **The chrome default is `SYSTEM` and has been since § 5u** — it already follows the phone.
This emulator carries an explicit `DARK` left over from testing, which is a valid state and
not what a fresh install does.

### 5ag. The mushaf illustration, redrawn. 2026-08-19

His verdict on the first one: *"the quran image there, it looks terrible."* He was right, and
the diagnosis generalises to every drawn illustration:

**It was made of straight lines, and a book has no straight lines.** Two flat quadrilaterals
meeting at a point over a bare X read as a paper aeroplane on sticks. What makes a shape say
*book* is the **curve** — leaves sag away from the spine under their own weight, and the outer
edge is where you see it.

The redraw is built from the things that actually signal a bound mushaf: curved leaves, a
gutter rather than a point, a visible cover standing proud of the paper, three lines of text
per leaf **following the sag rather than sitting level**, and a rihāl with thickness.

⚠ **REPLACED THE SAME DAY, after a second "it looks terrible".** His follow-up settled it:
*"why not use an actual image like they did for the reference image."* Offered four routes —
generate one, take an open-licensed icon, send me his own file, or drop it — **he chose the
open-licensed icon.**

It is now **Font Awesome Free 6's `book-quran`**, CC BY 4.0, about 2KB as an Android vector,
attributed in README.md's credits. ⬜ Its cover carries a **star and crescent**, which is the
icon set's choice and not a universally loved symbol of Islam; flagged for him rather than
shipped quietly, and `book-open` from the same set is one line away.

**⚠ The generated-image route was flagged before it was offered, and the reason generalises:**
an image model asked for a mushaf will put plausible Arabic-looking squiggles on the pages.
That is garbled nonsense rendered as scripture-adjacent, and it is Sacred Rule 2's failure in
a different coat. Any future illustration of a Qur'an in this app must carry **no lettering**.

**The lesson worth more than the icon: a Canvas path is the right tool for a MARK and the
wrong tool for an ILLUSTRATION.** The crescent on a chip, the flame, the tail on a bubble, the
medallion — those are marks, a few strokes where meaning survives being crude. A book on a
stand has perspective, weight and a dozen curves that all have to agree, and hand-writing
bezier control points is not how anyone draws one. Three attempts proved it.

**The original reasoning, kept because it still applies to the marks:** § 10 measures every byte for readers on
Ghanaian mobile data, and a picture is the easiest place to spend a hundred kilobytes without
noticing. It takes its colours from tokens already measured in § 6e, so it adds a picture
without adding a colour.

### 5ah. The two sections that were invisible. 2026-08-19

He asked for the streak block and This week next. **Both were already built and neither had
ever been seen**, including by him: `Your numbers` was gated on `totalDaysRead > 0` and
`This week` on a non-empty list, so on any fresh install Home simply stopped after the
check-in and looked unfinished.

**The empty state is the one screen a mockup never shows you, and every new reader starts on
it.** A comp is always drawn with data in it. Both sections now render on day zero and say
what will fill them — *"Nothing recorded yet. Finish today and this becomes day one."* Sacred
Rule 3: that is a fact about the reader, where "no data" would be a fact about the app.

**The streak also shows at zero now.** It used to appear only above 1, so the card silently
changed shape on the second day and a reader on day one never saw the thing the app is asking
them to build. Sacred Rule 4 is what makes a nought safe to show: the streak never appears
without total days read beside it.

**Found by dumping the view tree on a clean install** — neither section was in it at all.
⚠ **And a second lesson about that tool: dump only what is on screen.** The first dump said
both sections were absent *after* the fix, because the screen had not been scrolled to them.
Scroll, then dump.

⚠ **Verified with seeded days, and the seed was deleted in the same session** — checked by
listing `files/` afterwards and confirming the app fell back to its empty state. CLAUDE.md's
rule, and Sacred Rule 6 underneath it: a seeded day claims he recited when he did not.

⬜ **Not satisfied, and said rather than hidden: the streak's flame glyph still reads as a
water droplet** at 16dp. The second attempt is asymmetric and leans, which is what separates
fire from water, and it is better — but it is not good. Options are a larger glyph, a
different mark, or none at all, and it is his call.

### 5ai. Asking what an ayah means — answered, offline. 2026-08-19

He typed *"so explain verse 1 of fatiha"* into the companion and got *"I didn't catch that."*
His report: *"i asked it to explain a verse and it couldnt."*

**Two separate failures sat behind that one reply**, and only one of them was a real limit:

1. The parser had no notion of the question at all.
2. ⚠ **It could not have matched "fatiha" against an index that spells it "Al-Fatihah".**
   Nobody types the transliteration. Sūrah matching now also tries a **core** form with the
   `al-` article and one trailing `h` removed, guarded at four letters so short cores cannot
   match inside ordinary words. "Ta-Ha" would core down to "ta", which is in half of English.

**What was built, and its ceiling stated in the same breath:** the reply is the ayah's
**bundled translation with the translator named**, and it says *"that is a translation, not a
tafsir"* as part of the answer rather than as a disclaimer bolted on. A reader who thinks they
have been handed a scholar's explanation has been misled by the shape of the reply.

**His decision when offered the ladder: *"for now offline but we will add gemini."*** So:
- **Now** — Saheeh International, bundled, offline, no key, no model, no cost.
- **Later** — Gemini may rephrase a **fetched** tafsir. It may not author one. Sacred Rule 2
  does not soften because the engine got better, and § 5h calls a wrong tafsir the highest-risk
  thing in this app precisely because the reader usually cannot tell.

⚠ **It refuses a question that named nothing.** "Explain this" has no ayah in it, and guessing
that it meant today's first verse would be the parser inventing the question it was asked — on
the one subject where being confidently wrong is worst. Check 49 asserts that, and asserts that
a question of *fiqh* is still answered with an honest miss: there is no verse to fetch and no
scholar to name.

**Two existing checks had to be rewritten rather than deleted**, and that is the honest cost of
the change: both were asserting the old limitation. One used "explain surah yasin to me" as an
example of an unreadable clause, which stopped being true the day the companion learned to read
it. **A test whose premise is a missing feature gets rewritten the day that feature arrives.**

### 5aj. "Open full conversation" already went there. 2026-08-19

He asked whether it should open the AI chatbot. **It already opens the companion**, full screen,
same conversation, same history — there is no second and smarter bot behind it. The destination
was never the problem; the intelligence was. Recorded so the question is not reopened as a
navigation change when it is a capability one.

### 5ak. The recitation controls. 2026-08-19

He held up the player in the app he reads in: *"when you start listening it should have options
like forward pause replay, it has like 1 2 3 and infinity."*

**What was there: one button.** It started the portion and a second tap stopped it. No way to
hold it, no way to hear an ayah again without restarting the whole portion, and no way to sit on
one ayah — which is the thing a memoriser does most.

**Now:** the ayah being heard is named, with its place in the portion, over back · play/pause ·
forward · stop, and a repeat control cycling **1 → 2 → 3 → ∞**.

⚠ **The repeat counts AYAHS, not portions.** Repeating a whole portion three times is listening;
repeating *one ayah* three times is how memorisation is actually done, and § 5r already gave this
app a memorising mode with nothing behind it but relabelled buttons. **This is the first thing
that mode has ever changed about behaviour.** If he meant per-portion, it is one number.

**Two behaviours taken from how players work rather than how they look:**
- **Back restarts the current ayah first.** Past two seconds in, "back" means *I missed that*,
  and only a second press means the ayah before. Judged by position rather than by counting
  presses, so it needs no timer.
- **Changing the repeat count applies to the ayah playing now**, not the next one, because the
  reason anyone reaches for it is the ayah they have not got yet.

**The compiler asked two questions worth recording.** Adding a `Paused` state made two unrelated
`when` blocks inexhaustive, which forced decisions rather than allowing drift: the page's Listen
button stopped saying "Stop" (stopping belongs to the bar now, so one button no longer means two
things), and the status line under the page went silent while paused, because the bar already
names the ayah and saying it twice in two shapes is how a screen gets noisy.

⚠ **One self-inflicted false alarm, the second this session.** Pause looked broken — the button
still read "Pause" after tapping it — and it was not: my test helper was tapping coordinates from
a stale `uiautomator dump`. Tapping the freshly-measured centre gives Play/Paused immediately.
**Same lesson as § 5ad's keyboard alarm: re-measure immediately before the tap, never from an
earlier dump.**

### 5al. The sun, and a clock that disagreed with itself. 2026-08-19

*"Check the sunshine, it was done differently."* The comp's sun is a real one; the drawn version
did not match, so it is now Font Awesome's, like the mushaf and the flame.

⚠ **It also uncovered a genuine bug: the greeting and the mark were reading different clocks.**
The words called anything before noon "morning" while the mark called anything before six
"night", so at five in the morning the screen said **"Good morning" under a crescent moon.**
Both now take the greeting as the single source of truth — if the sentence says morning, the sky
does. **The crescent stays hand-drawn**, because a disc with a second disc knocked out of it is a
mark rather than an illustration, and § 5ag's rule cuts both ways.

### 5am. Play this ayah meant play the whole chapter. 2026-08-19

His report: *"when I click on an ayah to play it should play that ayah, but this one starts the
whole chapter."*

**He is right, and it was a one-line bug behind a well-named button.** The verse toolbar's
control said *"Play this ayah"* and called `listen()`, which has only ever started the portion
from the top. The label promised something the call could not do.

**Three cases now, in the order they are cheap:**
1. **Already playing, and the ayah is loaded** — jump to it. No network, no delay, nothing
   refetched, because it is all on disk already.
2. **An ayah outside today's portion** — play **only that one**. He tapped that ayah; the
   surrounding ones were never today's reading, so a portion is not what he asked for.
3. **Otherwise** — fetch the portion and begin at that ayah.

*Verified on the emulator:* long-pressed the last line of Al-Fatihah, pressed Play this ayah,
and the bar reported **"Al-Fatihah 1:7 · 7 of 7"**. Before the fix it always said 1 of 7.

⚠ **The general shape of this bug is worth more than the fix.** A control whose *label* is
right and whose *call* is generic reads as working until someone uses it for the thing the
label promises. It survived a whole build of the recitation controls without being noticed,
because every test drove playback from the page's Listen button and never from an ayah.

### 5an. The reciting highlight was already built, and two bars were fighting. 2026-08-19

⚠ **A correction to something this session said out loud.** The last walkthrough offered to
"add" the highlight on the ayah being recited, on the strength of § 6d's note that
`highlightReciting` was *"not yet used"*. **It has been used for some time.** `MushafPageView`
paints a continuous band across the run of glyphs whose verse key matches what is playing —
per run rather than per line, because a mushaf line usually carries the end of one ayah and the
start of the next. § 6d's note went stale and this section is the correction.

**The screenshot that proved it also exposed a real defect, introduced an hour earlier.** The
verse toolbar and the new playback bar are both pinned to the bottom of the page, so with an
ayah selected they stacked: the ayah label was clipped mid-word and half the play button sat
behind a share icon.

Two fixes, because one of them is not enough:
1. **The toolbar closes when its Play is used.** Acting on a selection finishes with it, and
   that removes the collision on the path anyone actually takes.
2. **The bar steps up over the toolbar** when a verse is selected anyway, since long-pressing
   an ayah *while* something is playing is a real thing to do.

**The lesson: a screenshot taken to verify one thing is worth reading for everything else in
it.** That capture was taken to check which ayah playback started on. It answered that, showed
the highlight working, and showed a layout collision — none of which was what it was for.

### 5ao. On his phone. 2026-08-19

Installed to the Pixel 6 Pro (`1A131FDEE006MD`, Android 17) as an update rather than a fresh
install, so **his reading record was never at risk**: checked first that the existing install
came from this machine, which meant the signing key matched and no uninstall was needed.

*Verified after installing:* `files/` still holds his data, `position_unit=882` (page 442, in
Ya-Sin) is unchanged, his settings survived, and there are no crash lines. **17.7 MB debug APK**
— unshrunk, and not the number testers will see; that gets measured at task 16 with R8.

⚠ Foreground was checked before anything was sent, per CLAUDE.md. It was Wird itself.

### 5ap. Task 14, probed before it was built. 2026-08-19

**⚠ The task was written around whisper.cpp, and probing it first found the cost before a line
of Kotlin existed.** That is what a ⚠ task is for.

| Assumption the task rested on | What is actually true |
|---|---|
| whisper.cpp can be built here | **No NDK and no CMake** on this machine — a one-off 3–5 GB install |
| The Tarteel model can be loaded | **No GGML/GGUF build exists.** 290 MB of PyTorch only |
| The model is affordable | **45–57 MB quantised, against a 17.7 MB app**, on Ghanaian mobile data |

That third line is the one that matters. § 10 fetches mushaf fonts **154 KB per page** precisely
to avoid this, and a model three times the size of the whole app is not a detail.

**The re-read that changed the plan.** Task 14's own done-when is narrower than transcription:
*"reliably separates a real recitation from silence and from unrelated speech."* The first half
is a **loudness** problem and costs nothing. So the cheap half ships first and its numbers decide
whether the 50 MB is ever worth it. His call when shown the trade: *cheap check first, measure it,
then decide.*

**How it works, and why it is free.** `MediaRecorder.getMaxAmplitude()` is already polled every
80 ms to drive the recording meter, so the recitation is measured **while it is recorded** — no
decoding, no second pass, no model. ⚠ **Reading that counter resets it**, so the samples are taken
from the value the meter already holds; a second caller would see a fraction of the sound and both
would conclude the room was silent. That hazard was written into the code as a comment and then
nearly introduced anyway by adding a second `level()` — caught on the duplicate definition.

**⚠ What it can and cannot do, stated in the enum, in the reply, and here.** It separates
recitation from silence, from a pocket, and from three seconds of throat-clearing. It **cannot**
separate it from a phone call, a conversation, or the radio — nothing here listens to words. A
check that quietly implies more than it verified is worse than no check.

**The verdict never gates the day.** Marking is unaffected; the reading is a note beside it. On
evidence this thin, letting it veto a recording would be the app calling someone a liar. Sacred
Rule 3 also decides which way to lean when data is thin: the thresholds are *forgiving*, because
wrongly telling someone their recitation did not count is how a person deletes a habit app, while
wrongly accepting a poor recording costs nothing anyone can feel.

⬜ **NOT TICKED, and that is the point.** Every threshold — a 1,200 speech floor, 10 spoken
seconds, a 30% share — is **provisional and unmeasured on his voice, his phone, his room.** Task 14
asks for the measured numbers written down and there are none yet. The logging is in place
(`WirdHeard`); the numbers come from real recordings.

### 5aq. Downloads, the way Quran for Android does it. 2026-08-19

His instruction, pointing at the app he reads in: *"the app itself is like 5 or 10 MB, so when you
open it the first time it lets you download the pages first. Then for the voices and translations
you go to settings and download what you want. That's the same thing we should do."*

**Two-thirds of it was already true.** Pages have always arrived one at a time as they are read
(154 KB font plus the page's text) and audio one ayah at a time, both cached forever. What was
missing was the ability to say **"get it all now"**, and any way to see what is already here — a
cache you cannot see is one you cannot trust.

**Built:** an `ON THIS PHONE` section in Settings showing pages held out of 604 and what they
weigh, with a tap to fetch the whole mushaf. One page at a time on purpose: 604 parallel requests
would be the fastest possible way to be rate-limited by a free API that owes us nothing.
**Resumable by construction** — cached pages are skipped, so running it again fetches only what is
absent.

⬜ **Known limit, stated rather than hidden: the download is tied to the screen.** It runs in the
composable's scope, so leaving Settings stops it. At roughly four seconds a page the whole mushaf
is about 45 minutes, which is far too long to hold someone on one screen. It survives being
interrupted because it resumes, but this wants a foreground service before testers see it.

**Translations stay bundled for now, and move for testers.** His words: *"we will add more
translations with time since I already have them, then it's okay, but for the testers let them
download it like Quran for Android."* So § 5z stands for this build and becomes a download later.

**⚠ And the size objection to whisper is gone.** It was never "50 MB is too big" — it was "50 MB
shipped to every tester on Ghanaian data". As an opt-in download it is the same bargain their app
already makes for audio, and task 14's real half is buildable on those terms. He chose the order:
downloads screen first, then whisper.

### 5ar. The five-second page. 2026-08-19

**Task 14 stopped being theoretical while checking something else.** His phone held one real
recording, `recitation-2026-08-19.m4a`, 44,849 bytes, logged as `"method": "RECITED"` for a full
page of Ya-Sin.

The recorder is 64 kbps mono, so that file is **about 5.6 seconds**. A page of Qur'an takes one to
three minutes.

⚠ **This is the hole task 14 exists to close, and it is now evidence rather than argument.** The
cheap check's `MIN_SPOKEN_SECONDS = 10` would have caught it — *"Saved, 5s. Shorter than a portion
usually takes."* The day would still be marked, because the verdict never gates the day; he would
simply have been told.

**It also validates the direction of the thresholds without validating the numbers.** One file
measured by arithmetic on its bitrate is not a measurement of his voice, and the per-second loudness
samples were not captured because the recording predates the build that logs them. The next
recitation produces the first real data.

### 5as. The download moved into a service. 2026-08-19

His instruction: *"do the download, cos it's supposed to run in the background and even a
notification showing what's left."* He was right, and the previous version was honest about being
wrong — the work lived in the Settings screen's own scope, so **leaving Settings killed it.** At
four seconds a page, the whole mushaf is about forty-five minutes, and nobody sits on one screen
for forty-five minutes.

**A foreground service is Android's name for work that keeps running while a notification shows.**
The system will not quietly kill it, precisely *because* the notification means the user knows it
is happening. ⚠ **That trade is the whole reason it is the right tool** rather than a clever one:
anything trying to pull ninety megabytes invisibly would be killed by exactly the battery managers
§ 10 warns about on Transsion phones, and would deserve to be.

**Proven on his Pixel, and the proof is the part that used to fail:** started from Settings, then
**HOME pressed to leave the app entirely** — the count went 502 → 597 → 604 with the app closed.
Final state: **604 pages, 92 MB of fonts and 26 MB of page text.** It reads offline now.

**Three details worth keeping:**
- The notification counts **pages to go**, not pages done. That is the figure a person actually
  wants.
- It repaints every ten pages. Six hundred and four repaints is six hundred and four system-UI
  wakeups for a bar moving a fifth of a pixel.
- It is **cancellable from the notification**, because a forty-five minute download nobody can stop
  is a hostage situation.

⬜ **Not built, and stated rather than hidden: it does not wait for wifi.** Someone starting this on
mobile data spends ninety megabytes of it. The Settings row names the size first, but a metered-
network guard is the honest fix and it does not exist.

⬜ **Also open: the "Open in Quran for Android" action is hard-coded to their package.** He asked
what happens for someone using a different Qur'an app. **There is no cross-app standard** —
`quran://` is that app's own invention — so the honest improvement is to offer whatever app on the
phone answers that scheme rather than naming one, and to accept that apps which never adopted it
cannot be reached at all.

### 5at. ⚠ Notifications were OFF on his phone, and the app never knew. 2026-08-19

He said he did not see the download notification. Checking why found something far worse:

```
POST_NOTIFICATIONS: granted=false
AppSettings: com.mosman.wird importance=NONE
```

**Every nudge this app has armed since install fired into nothing.** The alarms were real — task
8's scheduler worked, `lastArmedFor` was written, the receiver woke — and Android threw the
notification away at the last step. The reminder is the app's entire answer to the *forgetting*
half of § 2's finding, and it has been dead on the one phone that matters.

⚠ **The app had no way to find out, and that is the lesson.** Posting a notification reports
success whether or not anyone can see it. There is no error, no exception, nothing in logcat. A
reminder app cannot verify its own core feature by doing it — it has to *ask* whether it is
allowed to, which is a different call entirely (`areNotificationsEnabled`).

**Built:** Settings now checks on every open and, when notifications are off, says so **above**
the reminder setting rather than below it — *"Notifications are off · Reminders can't arrive ·
fix"* — and the tap opens Android's own notification screen for the app. Asking again in-app is
not an option once the permission has been denied: the system stops showing the dialog, so the
settings page is the only honest route.

**Why it is read on every Settings open rather than cached at launch:** the whole point is to
notice a change made *outside* the app.

⬜ **This is also PLAN task 15's real shape.** That task's self-check compares `lastArmedFor`
against `lastNudgeFiredAt` to catch a battery manager killing the alarm. This is the *other*
failure and the more common one: the alarm runs perfectly and the notification is silently
discarded. Both belong in the same place, and only one is built.

### 5au. Two corrections to what the numbers claim. 2026-08-19

**1. The percentage is the sūrah now, not the mushaf.** His word: *"make it the percentage to
finish the chapter rather."*

⚠ **The old figure was a bookmark wearing an achievement's clothes.** Position over 1,208
half-pages read **73%** on his phone on day one, because he set his position to page 442 during
setup — before a single page had been read *in Wird*. A number that large, that early, beside a
progress bar, invites exactly the belief Sacred Rule 6 exists to prevent. A sūrah is also the
unit a reader feels: "two pages left of Al-Kahf" is a thing you can finish tonight, and finishing
is what his idea 1 wants to congratulate.

**2. ⬜ Reading direction is an open question, and a real one.** His instruction:
*"for whether it's mushaf or from memory, ask the user if it's from that sūrah upwards or
downwards."*

**He is describing how ḥifẓ is actually done and the app does not know about it.** Memorisers
commonly start at the back — Juzʾ 30, the short sūrahs — and work **towards** Al-Baqarah, so
their position moves *down* the mushaf while a reader's moves up. Wird's position only ever
advances forwards, wrapping 604 → 1, which means it is silently wrong for a whole way of using
the Qur'an, and § 5r's memorising mode currently changes nothing but labels.

**Not built, deliberately:** it changes `assignPortion`, the wrap at both ends, the widget, the
week's page column and the setup question, and it deserves its own task rather than being
squeezed into an evening. ⚠ It should be asked **at setup, next to the memorise-or-read question**,
because those two answers belong together — that is his point.
