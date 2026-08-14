package com.mosman.wird.domain

import java.time.LocalDate

/** How a day was marked done. */
enum class Method { RECITED, TAPPED }

/** A day that was completed. Missed days are simply absent — there is no row for them. */
data class DayLog(val date: LocalDate, val method: Method)

/**
 * Streak and totals, returned together on purpose.
 *
 * Sacred Rule 4: the streak is never shown on its own. [totalDaysRead] never resets, so
 * a broken streak does not wipe the evidence that you read on forty days.
 *
 * Sacred Rule 6: [recitedDays] and [tappedDays] stay separate, so the app cannot let
 * anyone believe they recited more than they did.
 */
data class Progress(
    val currentStreak: Int,
    val totalDaysRead: Int,
    val recitedDays: Int,
) {
    val tappedDays: Int get() = totalDaysRead - recitedDays
}

/**
 * Compute progress from a set of completed days.
 *
 * An unfinished today does not break the streak — the day is not over yet. The streak
 * therefore counts back from today if today is done, and from yesterday if it is not.
 *
 * Two entries for the same date count as one day, and reciting wins over tapping. That
 * is the double-submit case from docs/app-flow.md, handled here rather than trusted to
 * the UI.
 */
fun progressOf(logs: Collection<DayLog>, today: LocalDate): Progress {
    val methodByDate: Map<LocalDate, Method> = logs
        .groupBy { it.date }
        .mapValues { (_, sameDay) ->
            if (sameDay.any { it.method == Method.RECITED }) Method.RECITED else Method.TAPPED
        }

    var cursor = if (methodByDate.containsKey(today)) today else today.minusDays(1)
    var streak = 0
    while (methodByDate.containsKey(cursor)) {
        streak++
        cursor = cursor.minusDays(1)
    }

    return Progress(
        currentStreak = streak,
        totalDaysRead = methodByDate.size,
        recitedDays = methodByDate.count { it.value == Method.RECITED },
    )
}
