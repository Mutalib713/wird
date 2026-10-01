package com.mosman.wird.domain

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * When the reminder arrives.
 *
 * Mutalib's decision, 2026-08-16: the default follows Maghrib, and it can be moved to
 * anything the reader wants. That is why this is a sealed set rather than a single
 * prayer — "after a prayer" and "at a time on the clock" are genuinely different kinds of
 * answer, and someone who just wants nine o'clock should not have to express that as an
 * offset from Isha.
 */
sealed interface NudgeSchedule {

    /**
     * [offsetMinutes] after [prayer]. Negative values mean before it, which the domain
     * handles and the settings screen does not currently offer.
     */
    data class AfterPrayer(
        val prayer: Prayer = Prayer.MAGHRIB,
        val offsetMinutes: Int = 30,
    ) : NudgeSchedule

    /**
     * Reminders anchored to one or more daily prayers (e.g. Fajr, Asr, Isha),
     * with [offsetMinutes] after each prayer.
     */
    data class AfterPrayers(
        val prayers: Set<Prayer> = setOf(Prayer.MAGHRIB),
        val offsetMinutes: Int = 15,
    ) : NudgeSchedule

    /**
     * A fixed hour on the clock, with optional repeating reminders if not yet read today.
     * [repeatIntervalHours]: 0 for once (no repeat), 1 for every 1 hr, 2 for every 2 hrs, 3 for every 3 hrs.
     */
    data class AtClockTime(
        val time: LocalTime,
        val repeatIntervalHours: Int = 0,
    ) : NudgeSchedule

    /**
     * No reminder at all.
     *
     * Sacred Rule 3 — the tone is never guilt-based. A daily notification you can only
     * silence by digging through Android's own settings is the pushiest thing this app
     * could do, so switching it off lives in plain sight.
     */
    data object Off : NudgeSchedule

    companion object {
        /**
         * Thirty minutes after Maghrib. In Accra today that is 18:44, with Isha at 19:21
         * — a clear window, at the hour he said he is most likely to be free and settled.
         */
        val Default = AfterPrayer()
    }
}

/**
 * How far ahead to look for a workable time before giving up.
 *
 * A week is generous everywhere the sun sets normally. It exists for the one case that
 * cannot happen in Ghana: above roughly 48° of latitude in midsummer the sky never gets
 * dark enough for Fajr or Isha to have a time at all, sometimes for months. Seven days is
 * not enough to search past that, and it is not meant to be — [nextAfter] returns null,
 * and the caller decides what to do instead, out loud.
 */
private const val SEARCH_DAYS = 7L

/**
 * The next moment this schedule calls for, or null if there isn't one.
 */
fun NudgeSchedule.nextAfter(
    now: ZonedDateTime,
    at: Coordinates?,
    method: PrayerMethod = PrayerMethod.MUSLIM_WORLD_LEAGUE,
): ZonedDateTime? = when (this) {

    is NudgeSchedule.Off -> null

    is NudgeSchedule.AtClockTime -> {
        val todayAt = now.toLocalDate().atTime(time).atZone(now.zone)
        if (todayAt.isAfter(now)) {
            todayAt
        } else if (repeatIntervalHours > 0) {
            var candidate = todayAt
            while (!candidate.isAfter(now) && candidate.toLocalDate() == now.toLocalDate()) {
                candidate = candidate.plusHours(repeatIntervalHours.toLong())
            }
            if (candidate.isAfter(now) && candidate.toLocalDate() == now.toLocalDate()) {
                candidate
            } else {
                todayAt.plusDays(1)
            }
        } else {
            todayAt.plusDays(1)
        }
    }

    is NudgeSchedule.AfterPrayer -> {
        if (at == null) {
            null
        } else {
            (0L..SEARCH_DAYS).firstNotNullOfOrNull { dayOffset ->
                val date = now.toLocalDate().plusDays(dayOffset)
                PrayerTimes.compute(date, at, now.zone, method)[prayer]
                    ?.let {
                        date.atTime(it).atZone(now.zone)
                            .plusMinutes(offsetMinutes.toLong())
                    }
                    ?.takeIf { it.isAfter(now) }
            }
        }
    }

    is NudgeSchedule.AfterPrayers -> {
        if (at == null || prayers.isEmpty()) {
            null
        } else {
            (0L..SEARCH_DAYS).firstNotNullOfOrNull { dayOffset ->
                val date = now.toLocalDate().plusDays(dayOffset)
                val times = PrayerTimes.compute(date, at, now.zone, method)
                prayers
                    .mapNotNull { p ->
                        times[p]?.let {
                            date.atTime(it).atZone(now.zone)
                                .plusMinutes(offsetMinutes.toLong())
                        }
                    }
                    .filter { it.isAfter(now) }
                    .minOrNull()
            }
        }
    }
}

/**
 * The next reminder, with a stretch of away days stepped over. **PLAN task 22.**
 */
fun NudgeSchedule.nextAwake(
    now: ZonedDateTime,
    at: Coordinates?,
    away: AwayPeriod?,
    method: PrayerMethod = PrayerMethod.MUSLIM_WORLD_LEAGUE,
): ZonedDateTime? {
    val first = nextAfter(now, at, method) ?: return null
    if (away == null || first.toLocalDate() !in away) return first
    return nextAfter(away.returnsOn.atStartOfDay(now.zone), at, method)
}

/**
 * The schedule in words, for the settings screen.
 */
fun NudgeSchedule.label(): String = when (this) {
    is NudgeSchedule.Off -> "No reminder"
    is NudgeSchedule.AtClockTime -> {
        val rep = if (repeatIntervalHours > 0) " (repeats every ${repeatIntervalHours}h)" else ""
        "At ${clockLabel(time)}$rep"
    }
    is NudgeSchedule.AfterPrayer -> when {
        offsetMinutes == 0 -> "At ${prayer.label}"
        offsetMinutes < 0 -> "${minutesLabel(-offsetMinutes)} before ${prayer.label}"
        else -> "${minutesLabel(offsetMinutes)} after ${prayer.label}"
    }
    is NudgeSchedule.AfterPrayers -> {
        val prayerNames = prayers.sortedBy { it.ordinal }.joinToString(", ") { it.label }
        if (offsetMinutes == 0) "At $prayerNames"
        else "${minutesLabel(offsetMinutes)} after $prayerNames"
    }
}

private fun minutesLabel(minutes: Int): String = when {
    minutes % 60 == 0 && minutes >= 60 -> {
        val hours = minutes / 60
        if (hours == 1) "An hour" else "$hours hours"
    }
    else -> "$minutes minutes"
}

/** 24-hour times read as instructions; this is how a person says the time. */
private fun clockLabel(time: LocalTime): String {
    val hour = when (time.hour % 12) {
        0 -> 12
        else -> time.hour % 12
    }
    val suffix = if (time.hour < 12) "am" else "pm"
    return "$hour:%02d $suffix".format(time.minute)
}
