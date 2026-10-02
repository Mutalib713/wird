# Wird

A *wird* (ورد) is the daily portion of the Qur'an a person commits to reciting.

Wird is an Android companion app designed to build and maintain a consistent recitation habit. It tracks which page and verse you are on, renders that page exactly as it is printed in the Madani mushaf, nudges you after prayers or at custom times, audits your recitation out loud using on-device artificial intelligence, and manages parallel reading tracks for daily reading, memorization, and revision.

There are no accounts, no cloud servers, no ads, and no tracking. All recitation recordings, transcriptions, reading history, and schedules remain exclusively on your phone.

## Why Wird exists

Most people who struggle to maintain a daily Qur'an habit do not fail simply because they forget. Forgetting is the smaller half of the problem. The larger half is procrastination and the absence of accountability.

In a madrasa (school) or with a Qur'an teacher, you had someone who expected you. You had to sit down, open your mouth, and recite out loud. Leaving school often breaks that habit because willpower alone is fragile against a busy schedule.

Wird is an honest digital stand-in for that discipline:

1. **Done means saying it out loud:** Anyone can tap a checkmark. Wird gives you a built-in voice auditor that listens as you recite and checks your words against the mushaf.
2. **Honest tracking:** The app logs recited days and tapped days separately. It never pretends listening to an audio file or tapping a button is the same as opening your mouth and reciting.
3. **Gentle accountability without guilt:** If you miss a day, your streak resets, but your total days read never decreases. The app never lectures you, sends passive-aggressive notifications, or says you failed.

## Everything the app has now

### 1. Parallel reading tracks
Real life has different routines. A student may have one reading goal during weekdays and a different target on weekends, or may balance daily reading (*tilāwah*) with memorization (*ḥifẓ*) and revision (*murāja'ah*).

Wird supports multiple parallel reading tracks:
* **Track disciplines:** Set each track as Tilāwah (reading from the page), Ḥifẓ (memorization), or Murāja'ah (revision from memory).
* **Independent schedules:** Assign specific days of the week to each track (for example, Mon–Thu for madrasa revision, Sat–Sun for personal reading, or Daily).
* **Independent targets and positions:** Each track keeps its own place in the Qur'an, its own daily target (half-page, 1 page, 2 pages, or custom verse counts), and its own reading direction (towards An-Nas from front to back, or towards Al-Fatihah from back to front).
* **Automatic or manual scheduling:**
  * In **Automatic mode**, the app automatically activates the track due today based on the day of the week.
  * In **Manual mode**, you tap the track badge at any time to switch active tracks.
* **Sequential continuation banner:** If your active track is completed today and another track is also due today, a clean home-screen banner invites you to continue directly into the next track.
* **Track name uniqueness:** Track names are validated in real time. If a name is already in use, the editor alerts you and prevents accidental duplicate tracks. Existing saved tracks are automatically deduplicated on load.

### 2. On-device voice recitation auditor (Whisper AI)
Instead of merely checking off a reading, tap **Recite** to recite aloud.

* **100% on-device speech recognition:** Powered by a customized Whisper speech model (`tarteel-ai/whisper-base-ar-quran`), running locally via on-device machine learning inference. No audio recordings or transcripts are ever uploaded to any server.
* **Selectable AI models:** Choose between **Whisper Accurate** (higher precision Arabic phoneme recognition, recommended) and **Whisper Compact** (faster, lightweight model for older phones).
* **Real-time audio level meter:** Visual feedback shows your microphone input while reciting.
* **Word-by-word diffing:** After you finish reciting, your words are compared directly against the Uthmani text of the assigned portion:
  * Accurately recited words are displayed in dark, crisp text.
  * Missed, substituted, or skipped words are clearly highlighted in soft coral.
  * A clear word accuracy percentage score is calculated.
* **Clean history storage:** Recitation audio and transcripts are stored locally. In your Wird history, recordings are neatly tucked behind an expandable "See recitation" toggle so your daily review stays clean.

### 3. The authentic Madani mushaf page
Wird does not render generic Arabic computer text. A printed Madani mushaf is typeset so that every line ends flush at the margin and every page ends on an ayah completion. Standard computer fonts cannot reproduce this typesetting.

* **King Fahd Complex QCF v1 glyph fonts:** Every one of the 604 pages in the mushaf uses its own dedicated font file containing the exact hand-drawn glyphs for that specific page.
* **Line-by-line justification:** The app measures each line individually and adjusts spacing so words fit edge-to-edge without wrapping or breaking verses.
* **Portion dimming:** Today's assigned lines appear in full ink contrast. The remaining verses on the page are softly dimmed to a muted blue-grey. There are no defacing yellow highlight bands over the sacred text.
* **Zero false text guarantee:** If a font file cannot load, the app refuses to display the page and explains why, rather than allowing Android to substitute a generic system font that would draw incorrect Qur'anic words.
* **Distraction-free reading:** The mushaf page contains no permanent buttons. Tap once to bring up the navigation and audio bar; tap again and the interface hides completely.
* **Full Qur'an browsing:** Access all 114 sūrahs grouped into 30 juz' sections, complete with English meanings, revelation place, verse counts, and starting pages.
* **Adjustable ayah text size:** Toggle custom text size and adjust the slider to comfortably fit your vision, paired with an interactive live page preview dialog.

### 4. Solar-aligned and custom recurring reminders
A fixed clock alarm like 7:00 PM is wrong for half the year because sunset shifts by hours across seasons.

* **Solar prayer calculation:** The app computes local prayer times (Fajr, Dhuhr, Asr, Maghrib, Isha) completely on the phone using astronomical solar formulas (solar declination and the equation of time). It requires no internet access, no external API keys, and no monthly service fees.
* **After Prayers reminders:** Schedule your reminder relative to the prayers you select (such as Fajr, Asr, or Maghrib), with delays of 10 minutes, 15 minutes, 30 minutes, or a custom delay from 1 to 180 minutes.
* **Set Your Own Time with smart repeat intervals:** Pick a specific clock time with repeat intervals: Once, Every 1 hour, Every 2 hours, Every 3 hours, or a custom interval from 1 to 12 hours until you complete your recitation.
* **Detailed notification copy:** Notifications announce the specific track and portion:
  > *Daily Reading · Time to recite Al-Kahf (Ayahs 1–20) · Page 293*
* **Nudge Diagnostics:** A built-in troubleshooting panel tests Android exact alarm permissions (`SCHEDULE_EXACT_ALARM`), notification channel status, and power-saving settings.
* **OEM battery survival guide:** Transsion phones (Tecno, Infinix, itel), Xiaomi devices, and Samsung handsets aggressively kill background processes. The app detects your phone manufacturer and provides step-by-step instructions to prevent reminders from being blocked.

### 5. Offline audio recitations
Listen to today's assigned verses when you need to hear proper pronunciation or review while commuting:

* **Available reciters:**
  * Abu Bakr al-Shatri (Murattal, Hafs an Asim)
  * Mishary Rashid Alafasy (Murattal, melodic and clear)
  * Mahmoud Khalil Al-Husary (Classical master of tajweed)
  * Abdul Basit Abdul Samad (Celebrated Egyptian reciter)
* **Audio quality options:** Choose between **Standard (64 kbps)** (optimized for low mobile data usage, about 1.1 MB per page) and **High (128 kbps)** (2.3 MB per page).
* **Offline forever:** Audio files are downloaded once per page and cached locally. Playback works smoothly in airplane mode.
* **Honest boundaries:** Listening to an audio recitation never marks your portion as done. Listening is helpful study, but it is not reciting.

### 6. Wird AI companion and reflection cards
The companion is an interactive guide designed to help you maintain your habit:

* **Per-track conversation context:** When you switch reading tracks, the companion starts a fresh conversation specific to that track's history, schedule, and goals.
* **Conversational schedule adjustments:** Change your plan naturally without digging through settings menus. Tell the companion:
  * *"Make it half a page on Fridays"*
  * *"I am travelling until Sunday"* (pauses reminders until you return without breaking your total days read)
  * *"Move my reminder to 9:00 PM from now on"*
* **Quick reply chips:** Quick-response buttons let you report status in one tap: *"Already recited today"*, *"Remind in 1 hour"*, or *"Not today"*.
* **Tafsir reflection cards:** Expandable classical reflection cards provide contextual commentary and historical background on the sūrahs and ayahs in your current portion.
* **Sacred Rule 3:** The companion never expresses frustration, guilt, or judgment when you miss a day. A missed day is acknowledged calmly, and tomorrow remains a clean start.

### 7. Tactile claymorphism interface
The interface uses tactile claymorphism with physical depth, soft debossed surfaces, and clean elevation:

* **Sanctuary light and dark palettes:**
  * **Warm Cream:** A calm daytime palette with natural forest green accents.
  * **Night Dark:** A deep green nocturnal palette with high-contrast text.
* **Night mode brightness sliders:** Tailor your reading comfort with an ayah text contrast slider and a deep black OLED background toggle.
* **Clean vector iconography:** All icons are clean vector drawables from Google Material Icons and Font Awesome vectors. The app strictly avoids informal emojis in UI components, dialogs, buttons, and status indicators.
* **Adaptive layout:** Built with responsive spacing that scales properly from 320px compact screens to high-density modern displays.

### 8. Complete privacy and data sovereignty
Your spiritual habits belong to you alone:

* **Zero account creation:** No email addresses, phone numbers, passwords, or logins.
* **No cloud database:** Your reading progress, streak logs, voice audio files, and companion notes are stored solely in local app storage on your phone.
* **No automatic cloud backups:** Android cloud backup is explicitly disabled (`android:allowBackup="false"` and `data_extraction_rules.xml`) to prevent private recordings or journals from silently syncing to Google Drive.
* **Data export and import:** You can export your full reading history and track configurations to a JSON file at any time, or import it onto a new phone.

## Technical architecture

Wird is built entirely in **Kotlin** using **Jetpack Compose** for modern, declarative user interfaces.

```
External Sources (Download Only):
   quran.com API   ──► Mushaf line layouts and surah metadata
   qpc-fonts repo  ──► QCF v1 glyph font files (cached permanently)
   qurancdn audio  ──► Reciter verse MP3s (cached on demand)
   whisper models  ──► On-device speech recognition models
                             │
                             ▼
                    Saved to local app storage
                             │
                             ▼
     Completely functional offline with zero internet access
```

### Key libraries and tools

| Component | Technology | Purpose |
|---|---|---|
| **Language** | Kotlin 2.x | Safe, concise application logic |
| **UI Framework** | Jetpack Compose | Declarative UI with tactile claymorphism styling |
| **Speech AI** | TFLite / ONNX Runtime | On-device Arabic speech-to-text inference |
| **Audio** | Android MediaPlayer & MediaRecorder | Offline recitations and local voice capture |
| **Background Alarms** | AlarmManager & BroadcastReceiver | Battery-efficient solar and clock reminders |
| **Data Storage** | Android SharedPreferences & JSON | Fast, zero-overhead local data persistence |
| **Mushaf Engine** | Custom Canvas & Per-Page Typography | Exact 15-line Madani mushaf justification |

## Building and verification

### Prerequisites
* Android Studio Ladybug or newer
* Android SDK 35 (compileSdk 35, minSdk 26)
* JDK 17 or JDK 21 (configured in Android Studio)
* Gradle 8.x+ (bundled via `gradlew`)

### Quick commands

1. **Run full lint and unit tests:**
   ```powershell
   ./check.ps1
   ```
   Ensures all code passes lint inspections (`warningsAsErrors`) and unit tests before committing.

2. **Assemble debug APK:**
   ```powershell
   ./gradlew assembleDebug
   ```
   Generates the installable package at `app/build/outputs/apk/debug/app-debug.apk`.

3. **Install to connected device via ADB:**
   ```powershell
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## Documentation index

| Topic | File Reference |
|---|---|
| Product rules, sacred constraints, and scope | [PROFILE.md](file:///c:/Users/USER/MyClaudeProjects/wird/PROFILE.md) |
| Development roadmap and implementation plan | [PLAN.md](file:///c:/Users/USER/MyClaudeProjects/wird/PLAN.md) |
| Developer workflow, commands, and device setup | [CLAUDE.md](file:///c:/Users/USER/MyClaudeProjects/wird/CLAUDE.md) |
| User interface specifications and tokens | [docs/ui-guidelines.md](file:///c:/Users/USER/MyClaudeProjects/wird/docs/ui-guidelines.md) |
| End-to-end screen navigation and interactions | [docs/app-flow.md](file:///c:/Users/USER/MyClaudeProjects/wird/docs/app-flow.md) |
| Privacy and security verification checklist | [docs/security-checklist.md](file:///c:/Users/USER/MyClaudeProjects/wird/docs/security-checklist.md) |

## Credits and licensing

* **Qur'an text and line layout:** [Quran.com API v4](https://api-docs.quran.foundation/)
* **Mushaf typography:** QCF glyph fonts from the King Fahd Glorious Qur'an Printing Complex, via [nuqayah/qpc-fonts](https://github.com/nuqayah/qpc-fonts). Used for personal worship and educational purposes. Not for commercial distribution.
* **On-device speech recognition:** [`tarteel-ai/whisper-base-ar-quran`](https://huggingface.co/tarteel-ai/whisper-base-ar-quran) (Apache-2.0 license, published by Tarteel AI).
* **Audio recitations:** Abu Bakr al-Shatri, Mishary Rashid Alafasy, Mahmoud Khalil Al-Husary, and Abdul Basit Abdul Samad, sourced via Qur'an Foundation CDNs.
* **Vector iconography:** Google Material Icons (Apache-2.0) and Font Awesome Free vectors (CC BY 4.0).
