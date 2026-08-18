package com.mosman.wird.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri

/**
 * Handing an ayah to another Qur'an app. **PLAN task 12.**
 *
 * ### What was actually established, and what was not
 *
 * **Quran for Android: proven.** Its manifest exports `QuranForwarderActivity` with
 * `<data android:scheme="quran"/>`, and the activity splits the whole URI string on `/` and
 * takes the first numeric segment as the sura and the second as the ayah. So `quran://18/10`
 * opens Al-Kahf 10. Read from the project's own GPL source rather than guessed.
 *
 * ⚠ **Tarteel: undetermined, and therefore not built.** It is closed source, publishes no
 * deep-link scheme, and nothing in its store listing or docs documents one. That is *not*
 * proof it has none — only that it cannot be established from outside. A button that merely
 * launched Tarteel's home screen would look like a deep link and not be one, which is worse
 * than no button. **What would settle it:** install the APK and read its manifest with
 * `aapt dump xmltree`. Recorded in PROFILE.md § 11.
 *
 * ### The rule this follows
 *
 * **The action only exists when the app does.** Wird asks the package manager, and if Quran
 * for Android is not installed there is no button — rather than a button that opens the Play
 * Store, which is an advert wearing a feature's clothes.
 *
 * On API 30 and up that question needs a `<queries>` entry in the manifest, or the package
 * is invisible and every check returns "not installed" whatever is really there.
 */
object OpenElsewhere {

    /** Quran for Android's application id. */
    const val QURAN_ANDROID = "com.quran.labs.androidquran"

    /**
     * `quran://18/10` for Al-Kahf 10.
     *
     * The forwarder takes the first two numbers it finds in the string, so the shape matters
     * more than the host: a bare `quran://18` would open the sura at ayah 1.
     */
    fun quranUriFor(verseKey: String): String {
        val surah = verseKey.substringBefore(':')
        val ayah = verseKey.substringAfter(':', "1")
        return "quran://$surah/$ayah"
    }

    /** Null when the app is missing, so the caller can simply not offer the action. */
    fun intentFor(context: Context, verseKey: String): Intent? {
        val intent = Intent(Intent.ACTION_VIEW, quranUriFor(verseKey).toUri())
        val resolves = context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()
        return if (resolves) intent else null
    }
}
