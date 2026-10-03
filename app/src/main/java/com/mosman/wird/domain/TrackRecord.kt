package com.mosman.wird.domain

import com.mosman.wird.data.decodeSchedule
import java.time.LocalDate

/**
 * A track's record, worked out from the day log and nothing else.
 *
 * **Why this exists (2026-10-03).** Each track used to carry its own `currentStreak`,
 * `totalDaysRead` and `lastCompletedDate` beside the day log, and the two drifted: Home showed
 * a streak that never dropped after a missed day, undo wound the counter back by hand and left
 * the total one too high, and a re-done day restarted the streak at 1. The day log already
 * knew all of it. Now it is the only place any of it lives.
 *
 * Days this track is not scheduled for (its [ReadingTrack.activeDays]) never break its streak:
 * a Mon–Thu madrasa track is not "missed" on a Saturday. Today unfinished never breaks it
 * either, because the day is not over. A finished day the track was not scheduled for still
 * counts; nobody should lose credit for reading on a Sunday.
 *
 * Sacred Rule 4: [Progress.totalDaysRead] never resets, and it is shown next to the streak.
 */
fun trackProgress(logs: Collection<DayLog>, track: ReadingTrack, today: LocalDate): Progress {
    val methodByDate: Map<LocalDate, Method> = logs
        .filter { it.trackId == track.id }
        .groupBy { it.date }
        .mapValues { (_, sameDay) ->
            if (sameDay.any { it.method == Method.RECITED }) Method.RECITED else Method.TAPPED
        }
    val earliest = methodByDate.keys.minOrNull()
        ?: return Progress(currentStreak = 0, totalDaysRead = 0, recitedDays = 0)

    var streak = 0
    var cursor = if (methodByDate.containsKey(today)) today else today.minusDays(1)
    while (!cursor.isBefore(earliest)) {
        when {
            methodByDate.containsKey(cursor) -> streak++
            track.isDueToday(cursor) -> break
            // Not scheduled and not read: a rest day, step over it.
        }
        cursor = cursor.minusDays(1)
    }

    return Progress(
        currentStreak = streak,
        totalDaysRead = methodByDate.size,
        recitedDays = methodByDate.count { it.value == Method.RECITED },
    )
}

/**
 * This track with its derived fields filled in from the log, so every screen that reads
 * `track.currentStreak` or `track.isCompletedToday()` is reading the log, not a stale copy.
 */
fun ReadingTrack.withProgress(logs: Collection<DayLog>, today: LocalDate): ReadingTrack {
    val p = trackProgress(logs, this, today)
    val last = logs.filter { it.trackId == id }.maxOfOrNull { it.date }
    return copy(
        currentStreak = p.currentStreak,
        totalDaysRead = p.totalDaysRead,
        lastCompletedDate = last?.toString(),
    )
}

/** The track's own plan: its daily amount and any weekday exceptions ("half on Fridays"). */
fun ReadingTrack.plan(): ReadingPlan = ReadingPlan(
    defaultUnits = dailyUnits.coerceAtLeast(1),
    weekdayUnits = weekdayUnits.filterValues { it > 0 },
)

/**
 * Today's portion for this track: its position, its plan for that weekday, its direction.
 *
 * The one place a portion is worked out. Home, the reading page, the widget and the
 * notification all call this, so they cannot disagree about what today's portion is — which
 * they did, three ways, before 2026-10-03.
 */
fun ReadingTrack.assignmentOn(date: LocalDate): Assignment =
    todaysAssignment(startUnit = positionUnit, plan = plan(), date = date, direction = direction)

/**
 * Which reminder this track follows on [today].
 *
 * A promise made today wins, for today only: *"in an hour"* means do not ask before then, so
 * it has to beat the track's own reminder as well as the default. Before 2026-10-03 it only
 * beat the default, so on any track with its own reminder "In an hour" did nothing at all.
 * Then the track's own reminder, then the default one.
 */
fun effectiveSchedule(
    track: ReadingTrack,
    commitment: Commitment?,
    fallback: NudgeSchedule,
    today: LocalDate,
): NudgeSchedule =
    commitment?.takeIf { it.appliesOn(today) }?.schedule
        ?: track.reminderScheduleRaw?.let(::decodeSchedule)
        ?: fallback
