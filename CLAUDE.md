# Wird — session rules

Read `PROFILE.md` and `PLAN.md` before touching anything. PROFILE.md is canonical.

## Rules

- Commit AND push after every meaningful change. Never wait for a green build — commit
  progress and describe honestly what state it's in.
- **NEVER add AI attribution** to commits, PRs, or anything landing in this repo. No
  `Co-Authored-By`, no "Generated with Claude Code".
- Sacred Rules in PROFILE.md § 6 need Mutalib's explicit approval to change.
- `DRY_RUN=1` is the default for anything that sends or uploads.
- **Never link a personal WhatsApp number to an unofficial gateway.** (2026-07: Green
  API got his personal number temp-restricted for ~6h with zero messages sent.)
- Verify with evidence — test output, measurements, a real screen on the real phone.
  Not claims. Prefer JS/adb measurements over screenshots; screenshots have been flaky
  on this machine.
- One PLAN.md task per session. Tick the box, commit, push.
- If a decision isn't answered by PROFILE.md, **stop and ask** rather than picking one.
- ⚠ tasks may fail. A ⚠ task that turns out to be impossible is a real result — write it
  into PROFILE.md § 11 and delete the feature. Never fake one green.

## Commands

```powershell
.\check.ps1                      # lint + unit tests — must pass before any build goes out
.\gradlew.bat assembleDebug      # debug APK
.\gradlew.bat installDebug       # install to the Pixel over USB
adb devices                      # confirm the phone is attached first
```

## Machine notes

- Lenovo i7-1165G7, 15.7GB RAM, **no Avast** — do **not** add `--max-workers=1` or the
  JKS truststore workaround. Those were the old laptop. Only reach for them if Gradle
  actually misbehaves.
- **`JAVA_HOME` is not set in the shell on this machine.** `gradle.properties`'
  `org.gradle.java.home` only picks the JVM the *build* runs on — `gradlew.bat` still
  needs a JVM to launch itself, and fails with exit 49 without one. `check.ps1` sets
  `JAVA_HOME` to the Android Studio JBR itself for exactly this reason. Any new script
  that shells out to Gradle must do the same.
- **`local.properties` needs `sdk.dir=C\:/Users/...`** — forward slashes *and* an
  escaped drive colon. A `.properties` file treats a lone backslash as an escape, so
  `C:\Users\...` silently becomes an invalid path and Gradle fails with
  `java.io.IOException: Invalid file path` during lint; and lint's `PropertyEscape`
  check separately requires the `:` to be escaped. Both halves bit us on 2026-08-14.
- **Lint runs with `warningsAsErrors = true`**, with exactly three checks disabled and a
  written reason for each in `app/build.gradle.kts`. If you need to disable a fourth,
  write down why — a silently growing disable list is how a check stops meaning
  anything.
- android-36.1, ~34s incremental builds. First clean build with a cold daemon is ~1m10s;
  a cold `assembleDebug` is ~2m15s.
- **The Pixel 6 Pro runs Android 17** (`ro.build.version.release` = 17, model `raven`,
  serial `1A131FDEE006MD`). We build against compileSdk/targetSdk 36, so compatibility
  modes apply on that device. This matters more than it looks: **the phone you develop
  on is at the newest end of the range and your testers are at the oldest.** Anything
  about notifications, alarms or background work behaves differently at both ends, and
  passing on the Pixel proves nothing about a Tecno.
- adb lives at `C:/Users/USER/AppData/Local/Android/Sdk/platform-tools/adb.exe` and is
  not on PATH. Useful proofs:
  `adb shell dumpsys activity activities | grep topResumedActivity` (is it really on
  screen), `adb shell pidof com.mosman.wird`, `adb logcat -d -t 200 | grep FATAL`.
- Pixel 6 Pro over USB. AGP 9 has a built-in-Kotlin gotcha — check it before blaming
  the build script.

## Gotchas specific to this project

- **The Quran.com API needs no key.** `api.quran.com/api/v4/verses/by_page/{n}?words=true`.
  Verified against page 453 on 2026-08-14: 16 verses, per-word `line_number`,
  `page_number`, `code_v1`, `code_v2`.
- **QCF fonts are one file per page**, 604 total. Fetch only pages actually read. Never
  bundle all of them — Ghana mobile data, and app size is a first-class concern.
- **KFGQPC fonts are not for commercial use.** Free personal and tester use is fine.
  Money attached needs permission from the King Fahd Complex.
- **Android 14+ denies exact alarms by default** unless the app is a clock or calendar.
  Measured on a fresh install: `adb shell cmd appops get com.mosman.wird
  SCHEDULE_EXACT_ALARM` → `Default mode: default`, and `canScheduleExactAlarms()` is
  false. Never cache that value — re-read it on every schedule, because it can change
  underneath you (it did once during task 3, for reasons we never established).
- **Never send a blind `adb shell input tap`.** During task 3 a WhatsApp call banner
  appeared in the same instant as our notification and the tap opened the incoming-call
  screen. It did not answer it, but it could have. Screenshot, confirm what is under the
  coordinate, then tap — and do not drive the screen while the phone is in use.
- **Tecno / Infinix / itel freeze background work.** Anything scheduled must be verified
  on a real Transsion phone, never only on the Pixel.
- **whisper.cpp on Android: batch is fast, streaming is ~5× slower than real time.**
  Always batch. Never stream.
- Qur'anic text is **never** generated by a model. Verified source only. No exceptions.

## UI work

Any UI work runs the **design-studio** skill first, including the palette picker —
colour is Mutalib's choice, not a session's. `docs/ui-guidelines.md` gets filled the
first time that runs.

The mushaf page itself is a reproduction, not a design decision. Everything around it
is design work.
