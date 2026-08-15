# UI guidelines — Wird

Written 2026-08-14 during the first design-studio Studio pass (PLAN task 4).
PROFILE.md is canonical; nothing here may contradict it, and § 6b (the pinned palette)
wins over anything below.

## Phase 0 — Product truth

- **Who / where:** one Ghanaian student, Android, often on mobile data, often at night,
  sometimes in a lecture hall where he cannot recite aloud.
- **Problem this screen solves:** "what am I reading today, and where does it start?" —
  answered before he can talk himself out of it.
- **Emotional state arriving:** usually reluctant. 8 of his last 14 misses were *saw the
  reminder, didn't open it*. He is not confused, he is unmotivated.
- **Leaving state:** having recited, or having honestly marked that he didn't.
- **Primary action, singular:** start reading. Everything else is subordinate — the done
  control does not compete with the page.
- **What he must remember:** the page was already open at the right place.
- **Surface mode** (impeccable): **Read.** Not Persuade. Nothing on this screen sells.

## Phase 1.5 — What real work taught us

Browse-only. **`/taste` did not run: the Playwright MCP server was disconnected this
session.** Stated rather than skipped.

| Source | Principle taken |
|---|---|
| [growth.design — Apple sleep notification](https://growth.design/case-studies/apple-sleep-notification) | **Banner blindness.** A notification in the same format every day gets tuned out, especially when attention is already captured. Wird's nudge must vary, or it joins the 8-of-14 he already ignores. |
| same | **Spark effect.** People act when the effort is tiny; a small gesture commits you and starts the routine. The done-action is one gesture, never a form. |
| same | **Loss aversion only works when the loss is real** — a manufactured penalty triggers reactance and gets rejected. Confirms Sacred Rule 4: the streak counts days actually read and nothing else. |
| [quran.com](https://quran.com/page/453) | Serves its Arabic faces as woff2 with `local()` **first** — checks the device before spending the user's data. Also: its reading surface is `rgb(31,33,37)`, not pure black. |
| screensdesign.com | **Rejected as a source.** Organised around paywalls, onboarding funnels and revenue, most of it behind Pro. Copying its patterns would import a monetisation logic Wird refuses. Recorded because a rejected source is still a finding. |

## Phase 2 — Direction

**Tone:** *the page, uncovered.* Reverent and quiet. The mushaf is not content inside an
app; it is the surface, and the app is a thin margin around it.

**Type:** the QCF glyph face carries the Qur'an and is not a design choice — it *is* the
page. The chrome is small enough (a handful of words and numerals) that one characterful
face carries all of it. No Inter, no Roboto, no system default as the only face.

**Colour world:** paper and ink, from the pinned palette. One accent, spent almost
nowhere.

**Signature move — the page is never painted on.** No highlight wash, no coloured
background band, no marker over Qur'anic text. Today's portion is marked two ways only:
everything *outside* it steps back to slate, and the ayah-end numerals *inside* it turn
deep teal. On a full page that is roughly eight small numerals carrying the entire accent
budget. Highlighting scripture with a yellow wash is what the other apps do; it defaces
the page.

**Anti-convergence check** (vs susu, the last thing of his I looked at): susu is a
passbook — warm paper, deep blue actions, burnt-orange rule, didone numerals for money.
Wird shares only "paper". Different world (mushaf, not ledger), different accent
behaviour (numerals only, ~1% surface, vs a structural rule), different signature move,
and a type system built around a face neither project could share. Passes.

**What we refuse:**

- Any wash, band, or highlight over Qur'anic text
- A progress bar across the page
- Streak flames, emoji anywhere in chrome, badges
- The page inside a card — the page is the surface, not cargo
- Purple/violet/indigo anything
- `Get Started`, `Continue`, `Submit` — buttons name the outcome

## Ghana floor for this screen

- Font is fetched once per page and cached on disk forever. A page you have read before
  costs nothing to open again.
- **v1 TTF is the default: 154 KB/page, ~4.5 MB/month for a page-a-day reader.** v2 is
  336 KB/page and ~9.9 MB/month for a rendering most readers will not consciously notice.
  One switch flips it — see `Mushaf.FONT_VERSION`.
- No font means no render. The screen says so and offers retry; it never falls back to a
  system Arabic face, because a substituted glyph is wrong Qur'anic text. See Sacred
  Rule 2.
- Everything after the first fetch works with the radio off.
