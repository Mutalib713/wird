package com.mosman.wird.data

import android.content.Context
import androidx.core.content.edit
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.ReadingPlan
import java.time.DayOfWeek

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
     * Defaults to [ThemeMode.LIGHT] rather than following the system: a mushaf is a paper
     * object, and the app follows the thing it stands in for. But he reads at night, and a
     * paper-white screen in a dark room is unkind, so this is a switch rather than a rule.
     */
    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.LIGHT.name)!!)
        }.getOrDefault(ThemeMode.LIGHT)
        set(value) = prefs.edit { putString(KEY_THEME, value.name) }

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
        fun weekdayKey(day: DayOfWeek) = "units_${day.name}"
    }
}
