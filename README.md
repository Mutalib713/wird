# Wird

A *wird* (وِرْد) is the portion of the Qur'an you commit to reading each day. It can be two pages,
one page or a few verses. Small and steady is the point.

Wird is an Android app that helps you keep that portion. It shows today's verses on the real
mushaf page, reminds you at a time that fits your day, lets you recite out loud and marks any verse
that may need another look, and remembers every day you read. There is no account and no Wird
server. Your reading history and your recordings stay on your phone.

## Why it exists

Most people who drop a daily Qur'an habit don't forget. They put it off, and nobody is waiting for
them. In a madrasa, a teacher expected you to sit down and recite. Wird is a quiet stand-in for that:

1. **Saying it counts.** You can recite your portion out loud and the app listens on your phone.
2. **It records honestly.** A recited day and a tapped day are both kept, and kept apart. Listening
   to a recording never counts as reading.
3. **It doesn't shame you.** Miss a day and your streak starts again, but your total days read never
   goes down. The app never scolds.

## What it does

### Today's portion, on the real page

Wird draws each of the 604 pages of the Madani mushaf with the King Fahd Complex page fonts (QCF),
so every line ends where the printed page ends it. Today's verses are in full ink and the rest of the
page is dimmed. If a page's font can't load, the app says so instead of drawing the words in a
different font.

A page's layout and font download the first time you open it and are kept after that. To read with
no data at all, download all 604 pages once in Settings.

### Reading tracks

A track is one goal with its own days, starting point, daily amount and reminder. Keep as many as
you like (for example *Daily Reading* every day, *Juz' ʿAmma* Monday to Thursday, *Weekend Review*
on Saturday and Sunday). Each keeps its own place and its own streak.

- **Goal:** read (*tilāwah*), memorize (*ḥifẓ*) or review (*murāja'ah*).
- **Amount:** half a page, one page, two pages, or a number of verses, with exceptions for single
  weekdays ("half a page on Fridays").
- **Order:** forwards from Al-Fātiḥah, or madrasa order, from An-Nās back towards Al-Fātiḥah.
  In madrasa order each sūrah is still read from its first ayah. At half a page a day, Juz' ʿAmma
  comes one short sūrah a day (An-Nās, then Al-Falaq, then Al-Ikhlāṣ); at a page a day it is
  page 604, then 603, then 602.
- **Switching:** in automatic mode Home opens the track that is due today; in manual mode you pick.

### Recite & Review

Tap **Recite & Review** and read your portion aloud, from the page or from memory. Wird records it,
and a speech model running on your phone writes down what it heard. That is lined up against the
portion's text, and any verse where words seem to be missing is marked in amber on the page. If too
little of the portion was heard, it marks nothing and tells you why.

- The model is a one-time download: 42 MB, or 78 MB for the more accurate one. After that it works
  offline, and Wird never uploads your recording.
- It is a hint, not a verdict. It has been tested on short clips and in unit tests, not yet on a
  full page recited in a real room (PLAN task 14).
- Already read it another way? Tap **Mark Done**. Both count, and your history shows which was which.

### Reminders

- **After a prayer:** prayer times are worked out on the phone from your rough location. Without
  location, the reminder comes at 8:00 pm and Settings says so.
- **At a time you choose,** repeating every so many minutes (5 to 720) until the day is read.
- Each track has its own notification, naming its portion: *Daily Reading · Time to recite Al-Kahf
  (Ayahs 1–20) · Page 293*. You can reply from the notification itself.
- Some phones (Tecno, Infinix, itel, Xiaomi, Samsung) stop apps working in the background. Settings
  shows the steps for your phone, and *Did my reminder ring?* checks what happened.

### Listening

Hear today's verses from Abu Bakr al-Shatri, Mishary Rashid Alafasy, Mahmoud Khalil al-Husary or
Abdul Basit Abdul Samad, at 64 kbps (about 1.1 MB a page) or 128 kbps (about 2.3 MB a page). Audio
is downloaded once per page and plays offline after that. Listening never marks a day as done.

### Translations

English (Saheeh International), Hausa (Abubakar Mahmud Gumi) and a transliteration are in the app,
so they work offline. Settings picks which ones the reading page opens with.

### Word search

The search box on the Sūrahs tab finds sūrahs by name or number, then every verse that holds your
words, in Arabic, English, Hausa or transliteration. Arabic can be typed with or without vowel
marks, and a word is found with letters attached to it too: "قمر" finds القمر and والقمر. The
transliteration matches by sound, so "rahim" finds *arraheemi*. Tap a verse to open its page. It
works offline. It can't yet find a word's whole family from its root (كتب, كاتب, مكتوب).

### The Companion

A chat that understands short, plain requests. It runs on the phone, with no internet and no AI
model.

- "half a page on Fridays": changes your plan for that day of the week.
- "I'm travelling until Sunday": pauses reminders without touching your total.
- "move my reminder to 9 pm": moves the reminder.
- "what does Al-Ikhlas verse 1 mean?": shows the translation, plus Tafsir Ibn Kathir (abridged,
  Dar-us-Salam) when you are online.
- "how am I doing?", "where am I?", "open Al-Kahf".

### Also

A home-screen widget with today's portion, bookmarks and recently read pages, a history of every
day read, and **Export everything**: your days, recordings, chats and bookmarks in one ZIP file you
can open on a computer.

## Privacy

- No account, no email, no Wird server, and no analytics or advertising code.
- Android backup is switched off for the app (`allowBackup="false"`), so recordings and notes don't
  copy themselves to Google Drive.
- Your reading history, recordings, notes and settings are stored only on the phone, until you
  export them.
- The app uses the internet only to download: page layouts and tafsir (Quran.com API), page fonts
  (nuqayah/qpc-fonts on GitHub), audio (everyayah.com and the Quran.com audio CDN) and the
  recitation model (Hugging Face). Those requests ask for pages, verses and files; none of them
  carries your history or your voice.

## How it's built

Kotlin and Jetpack Compose. Speech recognition is whisper.cpp built with the Android NDK, running
ggml conversions of Tarteel AI's Qur'an model. Reminders use AlarmManager. Data is kept in
SharedPreferences and JSON files.

```
Downloaded once, then kept on the phone:
   Quran.com API    ──► page layouts, sūrah details, tafsir
   qpc-fonts        ──► one QCF font per mushaf page
   audio CDNs       ──► reciters' verse files
   Hugging Face     ──► the recitation model
Bundled in the app:
   Arabic text and three translations
   the same four texts folded for word search (assets/search/)
```

The word search files are generated, not written by hand. After changing a bundled text or the
way search folds letters, rewrite them: set `WIRD_WRITE_SEARCH_INDEX=1` and run `QuranSearchTest`.
Without that, `check.ps1` fails and names the stale file.

### Building

You need Android Studio (its bundled JDK 21), Android SDK 36 (compileSdk and targetSdk 36,
minSdk 26) and the Android NDK for whisper.cpp.

```powershell
./check.ps1                 # lint (warnings as errors) and unit tests; must pass before a commit
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Documents

| Topic | File |
|---|---|
| What the app is for, its rules and scope | [PROFILE.md](PROFILE.md) |
| The task plan | [PLAN.md](PLAN.md) |
| How to work on this repo | [CLAUDE.md](CLAUDE.md) |
| Every screen, as images | [docs/screens/README.md](docs/screens/README.md) |
| Screen flow | [docs/app-flow.md](docs/app-flow.md) |
| UI rules and tokens | [docs/ui-guidelines.md](docs/ui-guidelines.md) |
| Privacy checklist | [docs/security-checklist.md](docs/security-checklist.md) |
| The new onboarding, ready to build | [design-source/onboarding/IMPLEMENT.md](design-source/onboarding/IMPLEMENT.md) |

## Credits and licences

- **Qur'an text, translations and page layouts:** [Quran.com API v4](https://api-docs.quran.foundation/).
- **Mushaf fonts:** the King Fahd Glorious Qur'an Printing Complex's QCF fonts, via
  [nuqayah/qpc-fonts](https://github.com/nuqayah/qpc-fonts). For personal worship and study, not
  for commercial use.
- **Speech recognition:** [`tarteel-ai/whisper-base-ar-quran`](https://huggingface.co/tarteel-ai/whisper-base-ar-quran)
  (Apache-2.0, Tarteel AI), in ggml form from
  [ram-a-dhan/tarteel-whisper-quran-ggml](https://huggingface.co/ram-a-dhan/tarteel-whisper-quran-ggml),
  run with [whisper.cpp](https://github.com/ggerganov/whisper.cpp).
- **Recitations:** Abu Bakr al-Shatri, Mishary Rashid Alafasy, Mahmoud Khalil al-Husary and Abdul
  Basit Abdul Samad, from [everyayah.com](https://everyayah.com) and the Quran.com audio CDN.
- **Icons:** Google Material Icons (Apache-2.0) and Font Awesome Free (CC BY 4.0): the Qur'an
  book, flame, sun and repeat icons.
