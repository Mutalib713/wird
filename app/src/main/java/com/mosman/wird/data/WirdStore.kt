package com.mosman.wird.data

import android.content.Context
import androidx.core.content.edit
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.ReadingPlan
import java.time.DayOfWeek

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
        const val KEY_DEFAULT_UNITS = "default_units"
        fun weekdayKey(day: DayOfWeek) = "units_${day.name}"
    }
}
