# Wird, the new look: design brief

Studio tier, redesign with a brand lock: new layout and navigation from Mutalib's reference
images (7 Oct 2026), pinned green palette kept. Decisions so far are in PROFILE.md § 5bj.

## Phase 0. Product truth

- **Who:** Muslims in Ghana keeping a daily Qur'an habit. Android, often a Tecno, Infinix or itel
  on mobile data, read in sunlight and at night. Mutalib is the first user.
- **Problem:** finishing today's portion, and coming back tomorrow. Most missed days are put off,
  not forgotten (PROFILE § 2).
- **Arriving:** a few spare minutes, often tired or distracted, sometimes guilty about missed days.
  **Leaving:** "that's done", calm, no guilt.
- **Primary action:** start today's portion (Recite & Review, or open the page).
- **What they remember:** today's portion is one tap away, and the app never scolds.
- **What the project needs:** days actually read (PLAN task 20), and recited days counted apart
  from tapped ones.

## Phase 1. Experience

### Navigation (decided)

Four tabs: **Home · Qur'an · Companion · Wird.** Settings opens from the gear on Home. The
reading page stays a full-screen place above the tabs, as today.

### Flows

- Home → Recite & Review → reading page with recorder → stop → day marked RECITED → check runs.
- Home → Mark Done → day marked TAPPED → card shows "Marked as read".
- Home → track chip → Switch Track sheet → pick → Home shows that track.
- Qur'an → Continue → last page read. Qur'an → search → word search (task 26).
  Qur'an → Sūrahs / Juz' / Page → reading page in browse mode.
- Companion → ask, or tap a suggestion → chat. Before Gemini: translations and tafsir, plan and
  reminder changes; open questions get a plain "not yet".
- Wird → Tracks: edit, create. Wird → Progress: days read, recordings, transcriptions.

### What each screen shows, mapped to real data

| Screen element (his image) | Wird data behind it |
|---|---|
| "Assalamu Alaikum, Mutalib" | `readerName`; with no name, just "Assalamu Alaikum" |
| Hijri date | `HijrahDate` (already on Home) |
| Track chip "Daily Reading ▾" | active track; sheet lists `getReadingTracks()` |
| "Al-Fātiḥah 1–7", "Page 1 → Page 1" | `assignmentOn(today)`: sūrahs, ayah range, start and end page |
| "1 page" chip | `unitsLabel()` or the verse target |
| "~5 min" chip | **no data yet** (open question Q3) |
| "0 / 1 completed" | tracks due today and done (new; counts due tracks) |
| streak pill, "Your Journey" | `trackProgress()`: streak, total days, recited days |
| "Recite rate 76%" | recited days ÷ days read |
| "Total progress 30%" (Wird tab) | how far the track's position is through 604 pages, in its direction |
| Qur'an → Continue Reading | `recentPages.first()` |
| Quick Access: Bookmarks, Recent, Downloads | bookmarks, recent pages, the downloads in Settings today |
| Companion: Recent conversations | one conversation per track today (`chat_<track>.json`) |
| Wird → Progress | today's History screen |

### Kept features that the images don't show (they must not disappear)

- Automatic or manual track choice (a row in the Switch Track sheet).
- Undo a marked day (on the reading page, as today).
- The reading page itself, listening, the recorder: unchanged by this task.

### States matrix

| View | Empty | Loading | Error | Offline / slow | Partial |
|---|---|---|---|---|---|
| Home | first day: streak 0, "Your first day" | none (all local) | n/a | works offline | a track with no days yet |
| Switch Track | one track: sheet still opens, shows "Create new track" | none | n/a | works | n/a |
| Qur'an | no recent page: Continue opens today's portion | page layout fetch on first open | "Couldn't get page N" (exists) | pages not downloaded say so | some pages cached |
| Sūrahs | n/a (bundled list) | none | n/a | works | n/a |
| Companion | no chats: suggestions only | tafsir fetch spinner | "Tafsir is offline" (exists) | translations work offline; tafsir and open questions say they need internet | n/a |
| Wird / Tracks | one track | none | n/a | works | n/a |
| Wird / Progress | no days: "Days you read show here" | none | corrupt log notice (B4) | works | recordings deleted, days kept |

### Edge cases

Longest track name 50 characters; 10 tracks in the sheet; a portion spanning 3 sūrahs
(madrasa order on page 604); reader name of 30 characters; Hijri date unavailable; dark mode;
font scale 1.3.

## Phase 1.5. Real work looked at (7 Oct 2026)

Browse (screensdesign.com, growth.design):

- **Hallow** (prayer app): one daily item as a big card with one play action; a mini player sits
  above the tab bar so audio carries across tabs.
- **Muslim Pro, "Ask AiDeen":** every AI answer carries its reference line ("Reference: Sahih
  Muslim") and a standing note on what it answers from. Trust shown inside the answer.
- **Muslim App:** Home leads with the one time-bound thing (next prayer, countdown); the rest are
  small tiles below.
- **Grammarly onboarding (growth.design):** every setup answer must visibly change what comes
  next; an answer that goes nowhere teaches people to skip questions.

Measured (`taste/quran.com.md`):

- Arabic only through the book's own fonts, never the UI face.
- No photographs on the path to reading; imagery only on optional learning content.
- One action colour, 0% of surfaces.
- Text in two levels does most of the work: 16 / 12 at a 0.75 ratio, ink and grey.

## Phase 2. Direction

- **Tone:** quiet, warm, unhurried. A prayer mat, not a dashboard.
- **Type:** a book serif for screen titles and the greeting (his images; Literata is the working
  pick, open question Q2) over the phone's own sans for everything else; Arabic only in mushaf
  fonts or a named Arabic face, never the UI font.
- **Colour world:** the pinned palette (PROFILE § 6f): cream field `#F7F5ED`, deep green ink
  `#17382D`, green `#245847` for actions only, grey text `#4B5551`, gold `#C9A24B` and gold
  text `#8E6900` for streak and highlights. Dark: the greens already in the app.
- **Signature move:** the header carries the time of day. The sky behind the greeting follows
  the real hour (Fajr pale, Maghrib amber, night dark). The app already draws this in
  `AtmosphericHeader.kt`; it becomes the one picture in the app, and it sits above the reading,
  not on it.
- **What we refuse:** photographs on reading surfaces; emojis outside streak and stats (Sacred
  Rule 6); "Verify" or any word that claims more than the check can know; guilt copy; a
  progress number nobody can define.

## His answers, 7 Oct 2026 (Home options sheet, `home-variants.png`)

1. **Header: B, a photograph**, with the book-serif titles (Literata). Needs a licensed photo for
   light and one for dark (or one photo with a night treatment). Candidates are Ghanaian mosques
   (Larabanga, Ghana National Mosque) and one general dawn mosque; he picks.
2. **"~5 min": left out** until recitation time per page is measured. The chip shows the amount.
3. **Arabic verses in the Companion and search:** the mushaf's own script (QCF page font) when
   that page is on the phone, the bundled modern spelling otherwise.
4. **"Recite & Review"** stays. Not "Verify".

## The questions as asked

1. Header art: the app's own drawn sky (time of day), a photograph, or no picture.
2. Title font: a bundled book serif or the phone's own font.
3. "~5 min": drop it, or show it once there is a measured basis.
4. Arabic in the chat card and search results: the mushaf's own page font (needs that page
   downloaded) or the app's bundled modern spelling (works offline, not the Uthmani script).
5. "Recite & Verify" (his image) or "Recite & Review" (his 3 Oct choice).

## First-time experience (8 Oct 2026)

Built into the same prototype: Settings → or the panel's "Start as a new user", or open
`prototype/index.html#new`. Traced from his images 4 and 6 (7 intro screens, track setup, review,
microphone, "ready", 6-step tour). The answers build the track that Home then shows.

**Product truth.** Someone who just installed Wird, often from a friend's link, on mobile data.
The moment that proves the app is worth it: Home showing a first portion sized to them
("Al-Ikhlas 1 – An-Nas 6, page 604, half a page"). Everything before that is in the way unless it
changes what that portion is.

**Browse, one principle each.**
- Grammarly onboarding (growth.design): every answer visibly changes what comes next. Here: a
  running summary under the progress bar, a live pace line on the amount step ("the whole Qur'an in
  about 20 months"), and the "you'll start on page N" card.
- Hopper permissions (growth.design): ask when the need is clear, and let the person trigger it.
  The microphone screen explains first; Android's own window only opens on "Allow the microphone".
- Headspace onboarding (growth.design): no notification request before any value. Matches his
  earlier decision: Android's notification permission comes after the first finished day.
- No new `/taste` run: the type, colour and spacing system is the one measured and pinned for the
  main tabs this week. Onboarding inherits it rather than setting a direction.

**Where the build differs from his images, and why.**
1. "Recite & Verify" → "Recite & Review" (PROFILE § 5be).
2. "Why did Jesus die?" removed (PROFILE § 5bj). The Companion intro shows what works offline today,
   plus a plainly labelled "Coming next" for open questions.
3. The reading-order labels in image 4 are swapped (it calls Al-Fātiḥah → An-Nās the African order).
   The app's own approved wording is used: An-Nās → Al-Fātiḥah is the usual madrasa order in Ghana.
4. Order is asked **before** the starting point, so the start screen can suggest An-Nās or
   Al-Fātiḥah to match. His images ask start first.
5. A name question ("What should we call you?", optional) sits on the "ready" screen. His images
   have no step for it, but Home greets by name.
6. "Get Started" (banned generic label) → "See how it works", with "Skip the introduction".
7. Intro screen 3 shows a week of three tracks instead of repeating screen 2's track list.
8. No streak numbers on the example tracks: they would be invented figures.
9. The microphone screen comes after the review and before "Your Wird is ready", as in image 6.
10. Intention is used: it shows on "Your Wird is ready" and under the track on the Wird tab.

**Waiting on him.**
- "Use the defaults instead" makes: Daily Qur'an, Read, every day, An-Nās → Al-Fātiḥah from page 604,
  1 page, 30 minutes after Maghrib. Today's app starts quick start at An-Naba' (page 582) reading
  toward Al-Fātiḥah, which walks into Juz' 29 rather than through Juz' 'Amma. Page 604 is the
  pending question from 3 Oct.
- Found in today's SetupScreen.kt: the Ya-Sin preset says page 442; SurahIndex says 440.

**Gates, 8 Oct.** design-studio gate.py 0 block, 2 warn (no share tags on a local file; a few 2px
off-grid values). Humanizer ux-microcopy on all 320 first-run strings: 100/100, pass. impeccable:
1 finding (dots animated their width), fixed; re-run clean.
