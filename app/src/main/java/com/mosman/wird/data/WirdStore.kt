package com.mosman.wird.data

import android.content.Context
import androidx.core.content.edit
import com.mosman.wird.audio.AudioQuality
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.NudgeSchedule
import com.mosman.wird.domain.Prayer
import com.mosman.wird.domain.ReadingPlan
import java.time.DayOfWeek
import java.time.LocalTime

/** Paper, ink, or the phone's own setting. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * Where you are and what you've asked of yourself, kept on this phone.
 *
 * SharedPreferences rather than a database: this is a handful of integers, and a database
 * would be machinery without a job. Sacred Rule 1 — nothing here syncs anywhere, and the
 * manifest's three backup exclusions make sure Android does not quietly copy it to Drive
 * either.
 */
class WirdStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** False on a brand-new install, which is what sends the app to setup. */
    val isSetUp: Boolean get() = prefs.contains(KEY_UNIT)

    /**
     * The half-page unit you are due to start on.
     *
     * This moves only when a day is marked done (task 6), never on app launch — opening
     * the app twice in a day must show the same portion twice.
     */
    var positionUnit: Int
        get() = prefs.getInt(KEY_UNIT, 0)
        set(value) = prefs.edit { putInt(KEY_UNIT, Math.floorMod(value, Mushaf.TOTAL_UNITS)) }

    /** 1-based page you are due to start on. Setter keeps the half-page you were on. */
    var positionPage: Int
        get() = Mushaf.pageOf(positionUnit)
        set(page) {
            val clamped = page.coerceIn(1, Mushaf.PAGES)
            positionUnit = (clamped - 1) * Mushaf.UNITS_PER_PAGE
        }

    /**
     * The exact ayah the reader said they were on, when they were mid-surah.
     *
     * Portions are counted in pages, but a page is not where anyone starts. Choosing
     * Ya-Sin means starting at Ya-Sin 1, which sits partway down page 440 underneath the
     * last verse of Fatir. This remembers that, so the first day begins where the reader
     * actually is. Cleared once the position moves past it.
     */
    var startVerse: Pair<Int, Int>?
        get() {
            val s = prefs.getInt(KEY_START_SURAH, 0)
            val a = prefs.getInt(KEY_START_AYAH, 0)
            return if (s > 0 && a > 0) s to a else null
        }
        set(value) = prefs.edit {
            if (value == null) {
                remove(KEY_START_SURAH); remove(KEY_START_AYAH)
            } else {
                putInt(KEY_START_SURAH, value.first); putInt(KEY_START_AYAH, value.second)
            }
        }

    /**
     * Paper, ink, or whatever the phone is set to.
     *
     * **Default flipped to [ThemeMode.DARK] on 2026-08-17**, with the palette. The old
     * default was light and the reasoning was sound at the time — a mushaf is a paper
     * object, so the app followed the thing it stood in for.
     *
     * The new palette makes that untenable rather than merely unfashionable: gold is
     * **2.06:1 on cream**, which is unreadable, and **7.79:1 on midnight**, which is AAA.
     * The accent colour only works on a dark ground, so a light-by-default app would be one
     * that never shows its own accent. The design agrees — it is dark everywhere except the
     * page itself.
     *
     * **The page stays cream in both modes.** That is not an inconsistency; it is the whole
     * idea. The app is dark, and the mushaf is a lit page inside it.
     */
    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.DARK.name)!!)
        }.getOrDefault(ThemeMode.DARK)
        set(value) = prefs.edit { putString(KEY_THEME, value.name) }

    /**
     * What to call the reader, or null if they never said.
     *
     * **PROFILE.md § 5c approved this on 2026-08-17 and § 5j specified it on 2026-08-18.**
     * It exists for exactly one thing: the greeting on Home says "Good evening, Mutalib"
     * instead of "Good evening". That is the whole feature.
     *
     * **Null is a first-class answer, not a missing value.** Setup offers a Skip, and the
     * greeting falls back to the bare hour — which is what it did before this existed and
     * which already read fine. A name is a courtesy, and an app that will not start until
     * you identify yourself is doing something else.
     *
     * Sacred Rule 1: this is a string in SharedPreferences on this phone. It is not an
     * account, it never leaves the device, and the manifest's backup exclusions cover the
     * file it lives in.
     */
    var readerName: String?
        get() = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit {
            val trimmed = value?.trim()
            if (trimmed.isNullOrEmpty()) remove(KEY_NAME) else putString(KEY_NAME, trimmed)
        }

    /**
     * Whether the reader has been shown, once, that tapping the page reveals the chrome.
     *
     * The gesture is otherwise invisible — nothing on a clean mushaf page announces that
     * it is tappable, which Mutalib spotted when he asked whether it should be a hamburger
     * icon. A permanent icon would announce it, but at the cost of a mark parked on the
     * Qur'an forever. Showing the bar once teaches the same thing and then gets out of the
     * way.
     */
    var hasSeenChrome: Boolean
        get() = prefs.getBoolean(KEY_SEEN_CHROME, false)
        set(value) = prefs.edit { putBoolean(KEY_SEEN_CHROME, value) }

    /**
     * When the reminder arrives. Defaults to thirty minutes after Maghrib.
     *
     * Stored as one short string rather than three separate keys, so a half-written
     * change can never leave "at Fajr" paired with a clock time. Anything unparseable
     * falls back to the default instead of throwing — a corrupt preference must not stop
     * the app opening.
     */
    var nudgeSchedule: NudgeSchedule
        get() = decodeSchedule(prefs.getString(KEY_NUDGE, null))
        set(value) = prefs.edit { putString(KEY_NUDGE, encodeSchedule(value)) }

    /**
     * Which Shatri recording to fetch.
     *
     * Defaults to the lighter one. A page is ~1.1 MB at 64 kbps against ~2.3 MB at 128,
     * every day, and the people this is being built for are students on Ghanaian mobile
     * data — so the default protects the bill and the setting is there for anyone who
     * would rather spend it.
     */
    var audioQuality: AudioQuality
        get() = runCatching {
            AudioQuality.valueOf(prefs.getString(KEY_AUDIO, AudioQuality.LIGHT.name)!!)
        }.getOrDefault(AudioQuality.LIGHT)
        set(value) = prefs.edit { putString(KEY_AUDIO, value.name) }

    var plan: ReadingPlan
        get() = ReadingPlan(
            defaultUnits = prefs.getInt(KEY_DEFAULT_UNITS, Mushaf.UNITS_PER_PAGE),
            weekdayUnits = DayOfWeek.entries
                .mapNotNull { day ->
                    val v = prefs.getInt(weekdayKey(day), 0)
                    if (v > 0) day to v else null
                }
                .toMap(),
        )
        set(value) = prefs.edit {
            putInt(KEY_DEFAULT_UNITS, value.defaultUnits)
            // Rewrite every day, so removing an override actually removes it.
            DayOfWeek.entries.forEach { day ->
                val v = value.weekdayUnits[day]
                if (v == null) remove(weekdayKey(day)) else putInt(weekdayKey(day), v)
            }
        }

    private companion object {
        const val PREFS = "wird_position"
        const val KEY_UNIT = "position_unit"
        const val KEY_START_SURAH = "start_surah"
        const val KEY_START_AYAH = "start_ayah"
        const val KEY_DEFAULT_UNITS = "default_units"
        const val KEY_THEME = "theme_mode"
        const val KEY_SEEN_CHROME = "seen_chrome"
        const val KEY_NAME = "reader_name"
        const val KEY_NUDGE = "nudge_schedule"
        const val KEY_AUDIO = "audio_quality"
        fun weekdayKey(day: DayOfWeek) = "units_${day.name}"
    }
}

/** "PRAYER MAGHRIB 30", "CLOCK 20:00", or "OFF". Readable on purpose — see DayLogStore. */
internal fun encodeSchedule(schedule: NudgeSchedule): String = when (schedule) {
    is NudgeSchedule.Off -> "OFF"
    is NudgeSchedule.AtClockTime -> "CLOCK ${schedule.time}"
    is NudgeSchedule.AfterPrayer -> "PRAYER ${schedule.prayer.name} ${schedule.offsetMinutes}"
}

internal fun decodeSchedule(raw: String?): NudgeSchedule {
    if (raw == null) return NudgeSchedule.Default
    val parts = raw.split(" ")
    return runCatching {
        when (parts[0]) {
            "OFF" -> NudgeSchedule.Off
            "CLOCK" -> NudgeSchedule.AtClockTime(LocalTime.parse(parts[1]))
            "PRAYER" -> NudgeSchedule.AfterPrayer(
                prayer = Prayer.valueOf(parts[1]),
                offsetMinutes = parts[2].toInt(),
            )
            else -> NudgeSchedule.Default
        }
    }.getOrDefault(NudgeSchedule.Default)
}
