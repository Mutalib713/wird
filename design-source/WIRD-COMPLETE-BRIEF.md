# Wird — complete design brief

Design the full UI for **Wird**, an Android app (Kotlin + Jetpack Compose). Hi-fi design +
interactive prototype. Don't worry about whether it works — I want the UI/UX.

**Design everything in here, including the parts marked "not built yet" and "beyond v1".**
It's cheaper to design ahead than to retrofit, and several of the future features change
what today's screens need to leave room for.

---

# PART 1 — What it is and why

A *wird* (ورد) is the daily portion of Qur'an a person commits to reading. That's the whole
app.

Wird knows which page you're on, decides what you're reading today, shows that page as it's
actually printed in the Madani mushaf, reminds you at a moment you're likely to be free,
and treats **"done" as something you say out loud rather than something you tick.**

## The finding everything rests on

I tracked myself for two weeks and missed about 14 days: **roughly 6 to forgetting, and
about 8 to procrastination.**

That split is the single most important fact about this app. A reminder alone fixes at best
6 of 14 — so a notification-plus-checkbox app solves less than half my real problem.

The one thing that historically worked: **I used to go and recite to a teacher.** School
stopped that and the habit went with it. Reciting aloud to someone who expects you is the
mechanic this app is a stand-in for.

## Who uses it

Me first, then a small circle of testers — Ghanaian university students, mostly on Tecno,
Infinix and itel phones. All Transsion, all aggressive about killing background apps. Tight
mobile data. Small screens.

## How success is measured

I read on **20 of the next 30 days, at least 10 of them recited aloud** rather than tapped.
For testers: how many are still marking days at day 14. Not installs, not stars, not screens
built.

## The core loop

1. A reminder arrives at a time that follows the sun, not a clock hour
2. You open it and land **directly** on today's portion — no home screen, no dashboard
3. You read the real printed mushaf page
4. You finish by **reciting it aloud into the phone**, or tapping, or listening
5. Your position advances and two numbers update

Every screen that isn't part of that loop is a place to bounce off.

---

# PART 2 — What the app does today

## 2.1 Setup (first run only)

Asks the question a person can actually answer. Nobody knows they're on "page 440" — they
know they're "somewhere in Ya-Sin".

- Pick your surah from a searchable list of all 114. Search tolerates punctuation and
  spelling — typing `yasin` matches `Ya-Sin`
- The app shows that page; **tap the actual ayah you're on**
- Or type the ayah number — the app knows each surah's length and won't let you type 400
  into a 3-ayah surah
- Choose how much a day: **half a page / one page / two pages**

Stored in half-page units, never fractions, so repeated halves can't drift.

## 2.2 Today's portion — the front door

The app opens straight here, every time.

- The **real Madani mushaf page**, drawn with the per-page QCF glyph font so it matches the
  printed book line for line, including where each line breaks
- **Today's portion at full strength, the rest of the page receded.** Every page, no
  exceptions
- The portion is also **named in words** ("Ya-Sin 28–40, 13 ayahs") so nothing depends on
  telling two shades apart
- A portion can start partway down a page and can span two pages
- Reading past page 604 wraps to page 1 and carries on. Finishing the Qur'an is not an ending

**Swipe left/right** to any page. **Jump to any surah.** When you're off today's page, a way
back appears.

## 2.3 The page carries no permanent controls

Otherwise completely clean. **Tap it and a bar appears; tap again and it goes.** The bar
shows where you are and how it's going, and holds: listen, go to a surah, settings.

Shown once on the very first run for 3.5 seconds to teach the gesture, then never again.

## 2.4 Marking the day done — the point of the app

Three ways, and the app never pretends they're the same:

| Way | What happens |
|---|---|
| **Recite it out loud** | Records you. Primary action |
| **I read it** | Marks the day. Logged explicitly as a tap |
| **Listen to it** | Plays a professional reciter. Currently marks nothing — undecided |

While recording: a live level meter and elapsed time pinned to the **top** of the screen —
because the moment you start reciting you scroll up to read, so anything at the bottom goes
out of sight.

Recordings are AAC in MP4, mono 64 kbps. **A recording too short to be real is deleted and
nothing is logged** — the app says nothing was saved rather than recording a recitation that
doesn't exist.

**Undo** puts the position back exactly, including across the 604→1 wrap.

**Today's portion doesn't change when you finish it.** A finished day remembers what it
covered, so marking done doesn't rewrite what today *was* underneath you.

## 2.5 The two numbers

Under the done control, and in the bar so it's reachable without finishing:

> *"7 in a row, 23 days read"* / *"5 recited, 18 marked as read"*

- **The streak never appears without the total**, and the total never resets
- **A streak of zero is never announced.** Someone who missed yesterday doesn't need a zero
  held up — they need to see they've read on 23 days
- The split is **written out**, never a ratio — "4 recited, 19 marked", not "4 of 23". A
  ratio invites you to read it as a score. This is a record, not a target
- A day marked and later recited counts **once**, as recited. Never the other way
- **Missed days are simply absent.** There is no row saying you failed

## 2.6 The reminder

Not a fixed clock hour — a fixed hour is wrong half the year. It hangs off a prayer time,
**30 minutes after Maghrib by default**, and moves with the sun through the year.

- Changeable to any of the five prayers with an offset, or a plain fixed time, or off
- Prayer times are **computed on the phone** — no API, no key, no network, works offline
  forever
- Needs coarse location. If refused it falls back to timezone, then a fixed hour, **and says
  on screen which one it's using**
- If you've already read today, it stays quiet
- Re-arms on app open, after each fire, and after reboot

**Prayer times are never displayed.** You pick a landmark you already know; the app does the
arithmetic silently. This is not a prayer-times app.

## 2.7 Listening — Abu Bakr al-Shatri

The escape hatch, so on a bad day the ask can drop to *just listen*.

- One MP3 per ayah, fetched on demand, then **cached and playable with no signal at all**
- Only the ayahs you were assigned — a half-page portion plays half a page
- Names the ayah as it plays ("Playing Ya-Sin 29 · 2 of 13")
- **The ayah being recited is marked while you hear it**, and the rest of the portion recedes
- Two quality options because data cost is real: **~1.1 MB per page** or **~2.3 MB**. For
  scale, the page's own font is 154 KB — audio is the most expensive thing the app downloads
- Cache is capped so it can't grow forever

## 2.8 Settings

How it looks · how much a day · **lighter days** (pick weekdays, give them a smaller amount)
· when to remind you · audio quality · where you are in the mushaf, shown in words with a way
to move.

Changing the daily amount applies **immediately**, including to today, and says so.

## 2.9 Sharing and export

- **One tap sends a recording to WhatsApp** as a voice note. Nobody else installs anything
- **Export everything** — all recordings plus the day log — into one folder you can copy off
  the phone

## 2.10 Home screen widget

Today's portion and done/not-done, updating daily. The reminder that can't be swiped away.

## 2.11 Surviving Transsion phones

Detects the manufacturer and shows the exact steps for *that* phone (Tecno/Infinix: Battery
Lab → disable power saving; Phone Master → auto-start management), plus a "did the last
reminder actually arrive?" self-check.

## 2.12 What the app stores

All local. No account, no server, nothing syncs.

| What | Detail |
|---|---|
| Position | where you are in half-page units, plus the exact ayah if you started mid-surah |
| Plan | pages per day, plus per-weekday overrides |
| Day log | one row per day: date, method, what it covered. **Plain readable JSON**, so you can read your own record with your own eyes |
| Recordings | audio files in app-private storage |
| Cached pages | page layout + the per-page glyph font |
| Cached audio | one MP3 per ayah |
| Settings | theme, reminder schedule, audio quality, location fix |

**Recordings never leave the device** unless you explicitly share them. A promise to testers,
not just to me.

---

# PART 3 — Beyond v1 (design these too)

Not built, some not decided. Each says how settled it is, so you know where to invent and
where to follow.

## 3.1 The accountability companion — **the biggest one, and wide open**

**Status: being scoped now. Nothing built. Treat as open.**

The feature the research actually points at. My missed days split **6 forgetting / 8
procrastination**, and a reminder only addresses the first number. What worked was reciting
to a teacher — a person who expected me.

**The loop:**
1. It asks whether today's wird is done
2. I reply in ordinary words — *"after Isha"*, *"I'll do it at 9"*, *"not today, I'm
   travelling"*, *"already did it"*
3. It **changes the app** — re-arms the reminder for the time I named, adjusts the schedule,
   or marks the day
4. If I named a time and didn't show, it notices

**Three jobs, in priority order:** commitment (naming a time to something that checks back —
this is the one aimed at procrastination) · rescheduling without menus · presence (something
that expects you).

**The honest open question, and it is central:** does this work when you know it's a bot?
Someone who built a *human-powered* version said the quiet part out loud — that a huge part
of an accountability partner is *having somebody there you don't want to disappoint*, and AI
may not deliver that feeling. Every AI-accountability project I could find had near-zero
traction.

**So the design problem is not "a chat UI". It is: what makes this feel like being expected,
rather than nagged by software?** That's the real brief, and it deserves your best thinking.

**Tone, non-negotiable:** never guilt-based. Not disappointed, not passive-aggressive, never
"you broke your streak".

**Two surfaces:** in the app for everyone; **on WhatsApp for me only, never in the tester
build.** WhatsApp is plain text and needs no visual design, but the *voice* has to work in
both.

## 3.2 The recitation checker result — **highest-stakes screen in the app**

**Status: planned, not built. Mechanic settled, the moment is not.**

Right now "recite it out loud" records me but **nothing listens** — I could record silence
and it would count. An on-device model (whisper, running locally, **no audio leaving the
phone**) compares the recording against the ayahs I was actually assigned. Because the app
already knows the expected passage, this is matching against a known answer rather than open
transcription, which tolerates a much higher error rate.

It is **a gate on honesty, not a teacher.** No tajweed scoring, no correction. The only
question is "was this a real recitation of roughly this passage?"

**Why it's the hardest screen to design:** there is now a result and it can be negative.
Being told your recitation didn't match could feel like being marked wrong *on the Qur'an* —
the worst thing this app could do to anyone. It must be possible to say "that didn't sound
like it" without shame and without implying religious judgement.

**States needed:** checking · matched · didn't match · too quiet or noisy to tell · model
unsure. **And an open question you can help answer: is a day still "done" if the recitation
didn't match?**

## 3.3 Full mushaf reader with live correction

**Status: explicitly wanted, deliberately deferred until the app proves itself over 30 days.
Also blocked on font/image licensing.**

Read anywhere in the Qur'an, not just today's page, with the app following as you recite and
flagging where you drifted. Today the app deliberately refuses to be a reader and links out.

Worth designing because it's the most likely thing to change the app's shape later — if it
arrives, "today's portion is the front door" has to survive it. **Show how a reader can exist
without the daily portion losing primacy.**

## 3.4 Memorisation (hifdh) tracking

**Status: excluded from v1 as scope, not refused on principle.**

What you've memorised, what's solid, what's slipping — revision scheduling rather than
reading. Same "say it out loud" mechanic, would reuse the recitation checker. Design it as a
*mode*, not a second app.

## 3.5 Exists or planned, but never designed at all

- **The app icon.** Nothing exists. Sits on a launcher beside Muslim Pro and Tarteel
- **Home screen widget**
- **Export**, including where a "your recordings are using 118 MB" retention decision lives
- **OEM survival guidance** and the "did the last reminder arrive?" self-check
- **Feedback**, and the plain sentence promising recordings never leave the phone
- **A one-page website** — what it is, install link, feedback form
- **Empty, error and offline states everywhere** — no connection, page won't load, font won't
  load, first day with no history, a day marked then undone

---

# PART 4 — Design constraints

## 4.1 The palette — please keep this

Five colours, my own pick from coolors.co. This one is genuinely fixed:

| Hex | Name | Job |
|---|---|---|
| `#01161E` | ink | text on paper; the dark-mode ground |
| `#124559` | deep teal | secondary text, accent, primary buttons |
| `#598392` | slate | **only** the ayahs outside today's portion |
| `#AEC3B0` | sage | "done", selected chips, the chrome bar |
| `#EFF6E0` | paper | the reading ground |

**Measured contrast:** ink/paper 16.68:1 · ink/sage 9.90:1 · teal/paper 9.37:1 · teal/sage
5.56:1 · slate/paper 3.72:1.

**Slate is never body text** — it fails AA on both grounds, and receding is its entire job.

**Dark mode is not an inversion.** Deep teal disappears on ink (1.78:1), so it stops carrying
text and becomes the raised surface; sage takes over as the accent.

Everything else — type, layout, spacing, motion, the icon — is yours. Go as far as you want.

## 4.2 Current tokens (reference, not a constraint)

Type: 13 / 16 / 20 / 25sp on a 1.25 ratio, body never under 16sp. The Arabic line sits
outside the scale at 28sp — its size is driven by fitting 15 lines on a phone, not a ratio.

Spacing: 4 / 8 / 12 / 16 / 24 / 32dp. Corner radius 6dp. Minimum touch target 48dp.

## 4.3 Rules a design has to respect

1. **The tone is never guilt-based.** This is my deen, not a language app. No streak-loss
   warnings, no "don't break your chain", no red, no shame
2. **The streak is always shown next to total days read**, and the total never resets. A
   streak of zero is never announced
3. **No highlight painted over the Qur'an.** Today's portion is marked by everything *else*
   stepping back. Painting a wash over it is what defaces it
4. **A habit tool, not a Qur'an reader.** No bookmarks, in-text search, tafsir or
   translations — those link out
5. **Today's portion is the front door.** No home screen — the *decision* is what stops
   people reading
6. **Recordings never leave the phone**
7. **Qur'anic text comes from a verified source and is never generated by a model**
8. Budget is **$0/month**. Everything runs on free, public, keyless sources

## 4.4 The rule most at risk from a redesign

**No highlight over the Qur'an**, rule 3 above. Highlighting a selected range is the obvious
move for any designer, which is exactly why it's flagged twice. The same applies to the
recitation checker: **do not mark a mistaken word in red on the Qur'an.**

---

# PART 5 — Deliberately refused (I'd suggest not designing these)

Not oversights. Each was decided with a reason, and a good-looking mockup is how a decision
like this gets quietly overturned. If you think one is wrong, argue it in words rather than
in pixels.

| Refused | Why |
|---|---|
| Tajweed scoring / pronunciation correction | Tarteel does it better. Link out |
| Live word-by-word following | **Measured** at ~5× slower than real time on-device. Technically dead, not deferred |
| Qibla, Hadith, Dua library | The bloated all-in-one that Muslim tech communities are tired of |
| Prayer timetable, countdown, adhan | Muslim Pro already does it well. Prayer times stay internal — no computed prayer time is ever shown |
| Bookmarks, in-text search, tafsir, translations | Would make it a Qur'an reader |
| Accounts, cloud sync, anything leaving the phone | A promise made to testers |
| Leaderboards or public streaks | Tone. This is someone's deen, not a competition |

---

# PART 6 — Current state, and where I'd most like your eye

**Built and working on a real phone:** setup, the mushaf page, position and portion maths,
swiping and jumping, the tap-to-reveal bar, marking done by recording or tapping, undo, the
streak and split, the prayer-timed reminder, and the reciter audio with offline playback.

**Not built:** the widget, WhatsApp share, export, deep links out, OEM guidance, both AI
features — **and there is no app icon at all.**

## Where a fresh eye helps most

- **The app icon.** Nothing exists
- **The accountability companion** — what makes it feel like being expected, not nagged
- **The recitation-check result**, especially the negative case
- **The moment right after you finish reciting.** Currently a plain sage box saying "Recited
  today". It's the emotional peak of the whole app and the flattest thing in it
- **Sage is doing three jobs** — "read today", "chip selected", "raised surface". Probably one
  too many for one colour
- **Settings is now five stacked chip rows** and reading like a form. Seven weekday chips
  already had to break onto two rows to fit a phone
- **Dark mode has never been designed** — only contrast-checked
- **The chrome bar uses two icons.** Words were the original call ("a row of little glyphs
  over a Qur'an page needs explaining"). Worth revisiting either way

## Screenshots

Five attached: today's portion · chrome bar revealed · foot of page with the done controls ·
settings · settings with the reminder off. Pixel 6 Pro, 1440×3120, paper mode.

Note the foot-of-page shot has no streak line visible — today wasn't marked done yet, and the
line only appears once there's a real number. Normally there's a "7 in a row, 23 days read"
line under those buttons.
