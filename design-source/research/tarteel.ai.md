# Design Map

## Spacing Scale
4 · 8 · 12 · 16 · 24 · 32 · 64px (base 4px). Most used: 16px (63), 12px (44). Section gap 64px.

## Font Hierarchy
- Hero: 96px / 96px, weight 400, Open Sauce Sans
- H2: 48px / 52.8px, weight 400, Open Sauce Sans
- Lead: 24px, Inter
- Body: 16px / 24px, weight 600, Inter
- Small: 14px, weight 500, Inter
- Caption: 12px, Inter
- Display steps x2.0 (96 to 48 to 24); body steps x1.14 to x1.5

## Color Palette
- Background #FFFFFF; band #F3F4F6 (15.6% of surface); near-white #FBFBFB (11.3%)
- Hero ground #000000 (30.8%, under video)
- Text primary #09090B; text secondary #6B7280; border #E5E7EB
- Accent: none in the interface. The only saturated colour is the green highlight inside the app footage.

## Image Ratios
- Hero: full-bleed video, 1440x900 viewport
- Video thumbnails: 4:3 at 400px
- Feature cards: ~0.88:1 (~325x370px)

## Component Tokens
- Radius: 9999px for everything pressable (17 uses), 12px for cards (20), 8px for images inside cards (4)
- Shadows: none
- Grid: 2 columns x 561.6px, 32px gap, ~1155px content, ~135px side margins
- Motion: transform / opacity / clip-path, 0.1 to 0.3s, cubic-bezier(0.4, 0, 0.2, 1); prefers-reduced-motion respected; :focus-visible present

---

# Taste DNA

### Let the Qur'an carry the colour
- **Trigger**: When choosing a brand colour for a Qur'an app.
- **Decision**: A black, white and cool-grey interface where the only colour is the green highlight inside the recorded app, over a brand green spread across buttons and headings.
- **Reason**: The text being memorised is the subject. The highlight that tells a reciter where they are is the one place colour carries meaning, so nothing else competes with it.
- **Evidence**: CSS accents only #09090B and #FFFFFF; grey bands #F3F4F6 at 15.6% of surface; green appears only in the hero footage.

### No shadows, no illustrations: photography does the depth
- **Trigger**: When building a page in a category full of floating mockups and drop-shadowed cards.
- **Decision**: Zero box-shadows and a full-bleed video of a real phone in a real hand, over rendered device frames and elevated cards.
- **Reason**: A real hand on a real mushaf screen says people use this. Flat cards keep the page from reading as an advert.
- **Evidence**: 0 box-shadows detected; hero footage covers 30.8% of the measured surface; cards are white 12px on a #F3F4F6 band with no elevation.

### Headline set light, not loud
- **Trigger**: When the hero needed one line to land at 1440px.
- **Decision**: 96px at weight 400 with line-height 1.0, over a bold 700 display.
- **Reason**: Large-and-light reads as calm confidence; large-and-bold reads as a sales pitch, the wrong tone for a devotional product.
- **Evidence**: h1 96px/96px weight 400 Open Sauce Sans; next display step 48px (x2.0); body Inter 16px at 500-600.
