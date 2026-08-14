# Wird

A *wird* (ورد) is the daily portion of Qur'an a person commits to reading.

Wird is an Android app that knows which page you're on, shows that page as it's actually
printed in the Madani mushaf, nudges you at a moment you're likely to be free, and treats
"done" as something you say out loud rather than something you tick.

It is not a Qur'an reader. It shows today's page and links out to Quran for Android and
Tarteel for everything else.

## Why

Reminders fix forgetting. Forgetting was the smaller half of the problem. The thing that
actually worked was having a teacher to recite to — someone who expected you, and in
front of whom you had to open your mouth. School ended that. Wird is a stand-in for it.

## Status

Pre-v1. Nothing built yet. See `PLAN.md`.

## Where things are written down

| Question | File |
|---|---|
| What it is, who for, what's excluded, the stack, the Sacred Rules | `PROFILE.md` |
| What gets built and in what order | `PLAN.md` |
| How a person moves through the app | `docs/app-flow.md` |
| What it looks like and why | `docs/ui-guidelines.md` |
| What must be true before it goes public | `docs/security-checklist.md` |

## Credits and licensing

- Qur'an text and page layout: [Quran.com API v4](https://api-docs.quran.foundation/)
- Mushaf rendering: QCF glyph fonts from the King Fahd Glorious Qur'an Printing Complex,
  via [nuqayah/qpc-fonts](https://github.com/nuqayah/qpc-fonts). **Not for commercial
  use.**
- Recitation: Abu Bakr al-Shatri
- Recitation checking: [`tarteel-ai/whisper-base-ar-quran`](https://huggingface.co/tarteel-ai/whisper-base-ar-quran),
  Apache-2.0, published by Tarteel

Private repo. Not for distribution.
