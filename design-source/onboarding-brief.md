# Onboarding — design brief (Studio pipeline, 2026-10-03)

The intro screens a new user sees before setting up their first reading track. Mutalib asked for
5 intro screens (2026-10-03): the 4 recommended, plus the Companion and offline Whisper explained.
The setup steps and the "Quick start with defaults" shortcut stay.

## Phase 0 — Product truth

- **Who**: Muslims in Ghana and West Africa, mostly students and young adults, on Android (often
  Tecno, Infinix, itel at 720p), on mobile data, often opening the app at night after Isha.
- **Problem this solves**: in under a minute, understand what Wird does that other Qur'an apps
  don't, then set up one track.
- **Arriving state**: curious, busy, often carrying some guilt about reading that lapsed.
  **Leaving state**: "this is small and doable, and it won't shame me."
- **Primary action**: one per screen, *Continue*; the last intro screen leads into setup.
- **What they must remember**: a small daily portion, said out loud, that the app helps you keep.
- **What the project needs**: a set-up track and a first recited day (PLAN task 20's measure).

## Phase 1 — Experience

```
splash → 1 What a wird is → 2 Reading tracks → 3 The whole Qur'an → 4 Recite & Review
       → 5 The Companion → gateway (Personalize step by step | Quick start with defaults)
       → setup steps → "your wird" summary → Home (+ tour)
```

- **Skippable**: every intro screen has Skip, which goes to the gateway.
- **Offline**: the intro needs no network. Nothing on it may depend on a download.
- **Permissions (proposal, needs Mutalib's yes)**: today notifications are asked at the end of
  setup. Both growth.design studies below say ask *after* the first moment of value. Proposal:
  ask after the first day is marked, with copy naming exactly what will be sent.
- **Edge cases**: 320dp-wide phones, large system font, a reader who skips everything,
  a reader who cannot read Arabic script (all Arabic is paired with English).

## Phase 1.5 — Real work looked at

Browse notes (one principle each):

- **growth.design — Headspace onboarding**: the splash can *give* something instead of just
  loading; frame goals positively; ask for permissions only after giving value (Headspace asked
  for notifications before the first session and lost the user).
- **growth.design — 5 onboarding mistakes**: ask for notifications after the first "aha", and say
  exactly what will be sent ("one reminder after Maghrib for this track"), never "relevant
  notifications"; keep the promised land in view on the first run.
- **screensdesign.com**: the library is now behind a paid Pro wall; could not browse it.
- **ChatGPT showcase (Mutalib's reference, measured from the image, not described)**: headline
  block at 7–15% of screen height, left-aligned serif; body to ~24%; content zone 30–75%; on the
  tracks screen 3 cards of ~10% height with ~6% gaps; pager dots at 83–87%; full-width pill CTA at
  90–95%.

Measured with `/taste` (`research/tarteel.ai.md`):

- **Let the Qur'an carry the colour**: Tarteel's interface is black/white/grey; the only colour is
  the green highlight on the recited text.
- **No shadows, no illustrations**: depth from real product imagery and background bands.
- **Headline set light, not loud**: 96px at weight 400, display steps ×2.0.

Guards: ratios transfer, values don't; **colour never comes from these sources** (Wird's palette is
pinned, PROFILE § 6f); no cloning.

## Phase 2 — Four directions to choose from (variants grid)

All four use only the pinned palette (cream `#F7F5ED`, green `#245847`, deep green `#17382D`,
secondary `#4B5551`, gold `#C9A24B` ornament / `#8E6900` text) and real Qur'an text from the app's
own assets.

- **A — Traced from ChatGPT's showcase**: its measured skeleton (top-left serif headline, cards in
  the middle band, dots + full-width pill at the bottom). Signature: three stacked track cards.
- **B — Continues the app's Home**: the green atmospheric header band with the mosque and moon
  across the top third, clay cards below, so onboarding looks like the app it opens into.
- **C — The lit verses**: each screen is built around a real mushaf excerpt where today's verses
  are in full ink and the rest are dimmed, the same highlight the app uses. Monochrome UI, colour
  only on the lit verses and the button (the Tarteel principle). Headlines large and light.
  Signature: the lit verses carry through all five screens.
- **D — The slate**: the madrasa slate (the board a student recites from) as the card, in deep
  green with cream text. Ghanaian schooling in the design, not bolted on.

Type: Literata for headlines (a face designed for long reading, made for Google Play Books) over
the system sans for body; Amiri for Arabic in the mockups (the app uses the QCF page fonts on the
real mushaf).

## Phase 2 — Decision (Mutalib, 2026-10-03)

**Direction B**, with the splash added. He picked **S1** of three splash options: his ChatGPT
reference traced (element positions and the sky's light measured from the image, colours rebuilt from
the pinned palette, the real app icon in place of AI calligraphy). He also asked for the setup screens
in the same style.

Direction line: **calm and plain / Noto Serif over Roboto, the phone's own fonts / a dawn sky in
forest green, cream and gold / the Home sky carried into every screen as a header band, with today's
portion lit inside a dimmed passage.**

## Phases 3–6 — System, build, gates

The whole flow, its tokens and the build brief are in `onboarding/`: `onboarding-flow.html` (the
board), `frames/` (each screen at 2×), `tokens.json`, and `IMPLEMENT.md` (exact words, behaviour,
wiring and acceptance checks).

Gate results, 2026-10-03:

- `design-studio/gate.py`: **PASS**, 0 block, 3 warn. The warnings are the 🔥 on streak figures,
  which Sacred Rule 6 allows. Fixed on the way: a generic button label, a missing favicon, white on
  Figma blue at 2.99:1 (board chrome, now 5.7:1), off-grid spacing.
- Humanizer on every reader-facing string: **75.9 → 96.5 / 100**, gate PASS (ux-microcopy), rhythm
  100/100. The two findings left are the quotation marks around the hadith, which are deliberate.
- impeccable `detect` (this board is HTML, so it applies): 5 findings, all defended. Roboto ×4 is
  Android's own body font, and choosing it means no font file ships. "Dark glow" is the board's dark
  canvas being read as the page: in the app that shadow falls on the light sky.

Fixed on the way, in the copy: the false "100% offline / never consumes data" claim, "Sacred Rule 1"
shown to readers, the unmeasured "under 45 seconds", a Bismillah translation longer than its Arabic,
and the Yā-Sīn preset on page 442 (it starts on 440).
