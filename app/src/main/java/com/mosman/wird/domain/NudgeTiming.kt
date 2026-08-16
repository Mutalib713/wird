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

    /** A fixed hour, for a reader who wants one. */
    data class AtClockTime(val time: LocalTime) : NudgeSchedule

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
 *
 * Null means one of three things, and the caller has to handle all of them out loud: the
 * reminder is switched off, we do not know where the phone is ([at] is null), or the
 * chosen prayer genuinely has no time at this latitude right now.
 *
 * A prayer that has already passed today rolls to tomorrow rather than firing late. That
 * matters more than it looks — this function runs every time the app opens, and someone
 * who opens Wird at nine in the evening must not be handed a reminder for the Maghrib
 * that went three hours ago.
 */
fun NudgeSchedule.nextAfter(
    now: ZonedDateTime,
    at: Coordinates?,
    method: PrayerMethod = PrayerMethod.MUSLIM_WORLD_LEAGUE,
): ZonedDateTime? = when (this) {

    is NudgeSchedule.Off -> null

    is NudgeSchedule.AtClockTime -> {
        val todayAt = now.toLocalDate().atTime(time).atZone(now.zone)
        if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
    }

    // Without coordinates there is no sunset to offset from, and guessing one would put
    // the reminder confidently in the wrong hour. The caller falls back to a fixed time
    // and says that is what it did.
    is NudgeSchedule.AfterPrayer -> {
        if (at == null) {
            null
        } else {
            (0L..SEARCH_DAYS).firstNotNullOfOrNull { dayOffset ->
                val date = now.toLocalDate().plusDays(dayOffset)
                PrayerTimes.compute(date, at, now.zone, method)[prayer]
                    // Built from the prayer's own date and then offset, so an offset
                    // large enough to cross midnight lands on the right day instead of
                    // wrapping back round to the morning.
                    ?.let {
                        date.atTime(it).atZone(now.zone)
                            .plusMinutes(offsetMinutes.toLong())
                    }
                    ?.takeIf { it.isAfter(now) }
            }
        }
    }
}

/**
 * The schedule in words, for the settings screen.
 *
 * Deliberately never shows a prayer time. PROFILE.md § 5 keeps prayer times out of v1 as
 * a feature, and "18:44" on screen is a timetable however it got there. The reader picks
 * a landmark they already know; the app does the arithmetic and keeps it.
 */
fun NudgeSchedule.label(): String = when (this) {
    is NudgeSchedule.Off -> "No reminder"
    is NudgeSchedule.AtClockTime -> "At ${clockLabel(time)}"
    is NudgeSchedule.AfterPrayer -> when {
        offsetMinutes == 0 -> "At ${prayer.label}"
        offsetMinutes < 0 -> "${minutesLabel(-offsetMinutes)} before ${prayer.label}"
        else -> "${minutesLabel(offsetMinutes)} after ${prayer.label}"
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
