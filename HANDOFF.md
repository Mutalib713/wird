# Wird — Session Handoff Notes
# Last updated: 2026-10-03 (corrected — see the note at the top of § 1)

**Read CLAUDE.md, PROFILE.md, and PLAN.md before this file.** Those are canonical.
This file records volatile session knowledge that doesn't belong in those three.

---

## 1. Who is the user

> **Corrected 2026-10-03.** The 2026-09-28 version of this file had wrong facts: his
> university, the palette, the tab names and the speech engine. Fixed below. When this file
> and PROFILE.md disagree, PROFILE.md wins.

**Mutalib Osman** — a Ghanaian student (Information Technology, KNUST). This is his personal
Qur'an companion app. He is a beginner developer who ships real projects but is still learning vocabulary.
**Never infer his expertise from his output.** Plain words first, always.

- The app greets **"Osman"** — that is the reader name he set in settings, not a different
  person.
- Device: **Pixel 6 Pro**, Android 17, model `raven`, display 1440×3120.
- Wireless ADB address changes between sessions — find it with `adb devices` / `adb mdns services`.
  (The address used to be written here; removed 2026-10-03 because the repo is public.)
- **$0/month infra** is the assumption for any tech stack decisions.

## 2. Repository state as of this handoff

- **Repo:** `c:\Users\USER\MyClaudeProjects\wird`
- **Remote:** `git@github.com:Mutalib713/wird.git` → branch `main`
- **HEAD:** check `git log -1` — this file no longer pins a commit, because it went stale
  within days last time.
- **⚠ The repo is PUBLIC** (confirmed 2026-10-03). Nothing personal or secret goes in any
  committed file from here on.
- **Working tree is clean** (tracked files). Untracked scratch files exist:
  `reset_prefs.py`, `scratch_uidump*.xml`, `tafsir_*.json`, `temp_wird_pos.xml` — all
  debug artifacts, safe to ignore or `.gitignore`.

## 3. What was built recently (this multi-week session)

### A. Interactive Feature Tour (`ToolkitTour.kt`, 497 lines)
A full-screen coachmark overlay that teaches users the Home screen features:
- **Thought-map style curly bezier arrows** — not straight lines. Organic cubic bezier
  loop-de-loop paths drawn on Canvas.
- **Spotlight cutout** punched with `CompositingStrategy.Offscreen` + `BlendMode.Clear`,
  surrounded by coral glow.
- **Floating text** positioned near each feature (not in a fixed box), with coral pill
  action buttons ("Next", "Back", "Okay I got it").
- **5 steps:** Portion Card → Action Tiles → Check-in → Companion Card → Numbers Card.
- All 5 steps verified live on device with screenshots.
- Tour is triggered from the overflow menu → "Toolkit Tour".

### B. Tafsir Commentary in Chat (`TafsirRepository.kt` + `Companion.kt`)
When the user taps "Tafsir & Translation" chip in the AI Companion chat:
- **TafsirRepository** fetches from `api.quran.com/api/v4/tafsirs/{tafsir_id}/by_ayah/{key}`
- **Section parser** groups the raw HTML into deterministic heading-based sections instead
  of naive paragraph numbering (which gave 225 unintelligible items for Al-Fatihah 1:1).
- **Cache** uses `tafsir_cache_v3/` directory (invalidated older versions).
- **UI (in `Companion.kt`):**
  - `parseTafsirMessage()` extracts the structured sections from the bot message
  - `TafsirCard()` renders inside `BotBubble` with:
    - Ayah reference pill ("Al-Fatihah 1:1")
    - Saheeh International translation quote card
    - Tafsir bar: book icon + "Tafsir Ibn Kathir · 22 sections" badge + "Expand all"/"Collapse all"
    - Expandable accordion sections with numbered circular badges and chevrons
    - Dedicated Arabic callout boxes (emerald background)
  - Section 1 is expanded by default; rest collapsed.

### C. Other features already in app (built in prior sessions within this conversation)
- **Life Spaces & parallel reading tracks** — School Mode / Home Mode
- **Auto schedule** with prayer-time-based reminders
- **Atmospheric gradient header** with Canvas mosque silhouette, crescent, flying birds
- **Floating island dock** — bottom navigation with 3 tabs (Home, Sūrahs, History)
- **Streaming audio** with floating listen bar, per-ayah repeat
- **On-device recitation checker** via whisper.cpp + ggml Tarteel models (built but unmeasured)
- **10-step beginner onboarding** (SetupScreen)
- **Home screen widget** showing today's portion
- **Privacy Pledge** card in settings
- **Showcase landing page** with product film at `showcase/`

## 4. Key architecture facts

### Source layout
```
app/src/main/java/com/mosman/wird/
├── audio/          # Whisper, PortionAudio, Recitation
├── data/           # WirdStore, TafsirRepository, ConversationStore, BookmarkStore
├── domain/         # CompanionBrain, Portion, Plan, LifeSpace, Progress
├── mushaf/         # MushafRepository, MushafPage, MushafDownloadService
├── nudge/          # NudgeScheduler, BootReceiver, CommitReceiver
├── ui/             # All composables (see below)
│   └── theme/      # Clay.kt, Palette.kt, Scale.kt, Theme.kt
├── widget/         # WirdWidget.kt
└── MainActivity.kt # Central orchestrator (1267 lines)
```

### Biggest UI files (line counts)
| File | Lines | What it does |
|---|---|---|
| SettingsScreen.kt | 3635 | All settings panels |
| SetupScreen.kt | 2542 | 10-step beginner onboarding |
| TodayScreen.kt | 2101 | Today's portion reader |
| LifeSpaceDialogs.kt | 1957 | Mode/track management dialogs |
| HomeScreen.kt | 1521 | Dashboard with cards |
| MainActivity.kt | 1267 | State + navigation orchestrator |
| Companion.kt | 1205 | AI companion chat + Tafsir |
| AtmosphericHeader.kt | 771 | Gradient header with Canvas art |
| CompanionBrain.kt | 610 | AI logic and intents |
| ToolkitTour.kt | 497 | Feature tour overlay |

### Theme/palette
- **Forest green and cream**, pinned by Mutalib 2026-10-03 — PROFILE.md § 6f has the values
  and the measured contrast. Green `#245847` on cream `#F7F5ED`; dark ground `#191A1E` with
  light green `#8ED676`; gold `#C9A24B` as ornament only.
- ⚠ The colours are hard-coded in the screens; `ui/theme/Palette.kt` still holds the old
  teal tokens. See PROFILE.md § 6f before touching colour.
- Fonts: the system default plus `FontFamily.Serif` in four places. No Latin display font and no
  Amiri are bundled (checked 2026-10-03; the earlier line here was wrong). The mushaf uses the
  per-page QCF glyph fonts, downloaded on demand.
- Clay composable = the card container used everywhere on Home.

### ADB patterns (CRITICAL for device work)
- `adb` is **NOT on PATH**. Full path: `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`
- Wireless connection drops constantly. Every command batch must start with:
  ```powershell
  $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
  & $adb connect <phone-ip>:<port>
  & $adb -s <phone-ip>:<port> shell <command>
  ```
- Use `-s <phone-ip>:<port>` flag always (avoids "more than one device" errors from stale entries).
- **Check foreground first** before any tap: `dumpsys activity activities | grep topResumedActivity`
- **This is Mutalib's daily phone.** Never send blind taps. Never change system settings.
  Screenshot → confirm what's under the coordinate → then tap.
- `uiautomator dump` beats screenshots for measuring layout (see CLAUDE.md).

### Build
- JDK 21 via Android Studio JBR. `JAVA_HOME` is NOT set in shell — `check.ps1` sets it.
- `local.properties` needs forward slashes and escaped colon for `sdk.dir`.
- compileSdk/targetSdk 36, running on Android 17 device.
- `.\check.ps1` = lint + unit tests. Must pass before builds go out.
- `.\gradlew.bat assembleDebug` → debug APK.
- Fresh APK gets copied to `showcase/downloads/wird-latest.apk`.

## 5. PLAN.md task status summary

### ✅ Done
Tasks 1-10, 18, 19, 22, 25 — all core features.

### [~] Partially done
- **12. Deep links** — Quran for Android proven, Tarteel undetermined
- **14. On-device recitation checker** — built, running, but **unmeasured** on real voice
- **15. OEM notification survival** — built, Transsion test pending
- **16. Firebase App Distribution & Icon** — icon/release packaging built, distribution pending
- **21. Commitment loop** — mechanism built, but no evidence it works yet

### ☐ Not started
- **17. Vercel landing page + feedback** (showcase/ exists but not deployed)
- **23. WhatsApp integration** (for Mutalib only)
- **24. Does it actually work?** (end-to-end walkthrough)
- **20. Thirty-day measurement** (the real test)

## 6. Things the next session should NOT forget

1. **The user's most recent feedback was about the ToolkitTour arrows:** he wanted
   "thought-map style curly loop arrows pointing directly at features, descriptions close
   to where they point." He provided a reference image (`media_1790606187452.jpg`). This
   was implemented and verified.

2. **Tafsir accordion works but expand/collapse tap wasn't regression-tested** on device.
   We saw it rendered but didn't explicitly tap-test the collapse of an expanded section
   or verify "Expand all" works on device.

3. **Regression checks NOT done:**
   - Settings → Reading Track Setup confirmation dialog
   - Home Mode / School Mode switcher
   - Adhan notification display

4. **Untracked scratch files** in repo root should probably be `.gitignore`d or deleted.

5. **Sacred Rules are in PROFILE.md § 6** — read them every session. The tone rule (#3)
   and the "no AI attribution" rule (#7) are the ones most likely to be violated.

6. **Quran.com API needs no key.** Free, open, verified.

7. **QCF fonts are per-page** (604 total). Never bundle all — Ghana mobile data constraint.

8. **whisper.cpp:** Batch mode only. Streaming is ~5× slower than real time on this device.

## 7. User preferences for working style

- **One PLAN.md task per session**, verified with evidence.
- **Every task ends with a walkthrough** in plain words, not a changelog.
- **Commit AND push after every meaningful change** — never wait for green build.
- **ZERO AI attribution** in commits, PRs, or anything in the repo. Ever.
- **Design Studio skill** auto-applies for any UI work. Palette Picker must run first.
- Color is always Mutalib's choice. Never pick colors for him.
- Vector icons for controls; emojis allowed on streak/stats figures (Sacred Rule 6, amended
  2026-10-03 at his word).
- **PowerShell** commands only (Windows OS).
- **Weak build machine** — avoid heavy parallel compilation.

## 8. Companion chat behavior rules

- The companion is scoped in PROFILE.md § 5b.
- **Never guilt-based, never disappointed, never passive-aggressive.**
- **Never produces Qur'anic text** — talks about schedule, not about the Qur'an.
- **Cannot mark a day "recited"** — only "tapped." Only a real recording counts as recitation.
- Quick reply chips at bottom of chat: "Already recited today", "Remind in 1 hour", "Not today"
- Quick topic chips: "Tafsir & Translation", "How am I doing?", "Where am I?"

---

*This file is a memory aid for session continuity. The canonical spec is always PROFILE.md.*
