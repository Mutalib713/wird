package com.mosman.wird.nudge

import android.content.Context
import android.util.Log
import com.mosman.wird.data.PlaceSource
import com.mosman.wird.data.Where
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.NudgeSchedule
import com.mosman.wird.domain.nextAfter
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * What happened when we last tried to set the reminder.
 *
 * A sealed result rather than a boolean because the interesting cases are the awkward
 * ones, and the settings screen has to be able to say which happened. "It didn't work" is
 * the sort of silence that makes people think the app is broken — see the exact-alarm
 * handling this mirrors.
 */
sealed interface Armed {

    /** Set, and for the reason the reader asked for. */
    data class At(
        val time: ZonedDateTime,
        val exact: Boolean,
        val source: PlaceSource,
    ) : Armed

    /**
     * Set, but not the way they asked.
     *
     * Either we do not know where the phone is, or the chosen prayer has no time at this
     * latitude this week. Either way a fixed hour is used instead, and the screen says so.
     */
    data class AtFallback(val time: ZonedDateTime, val exact: Boolean) : Armed

    /** Switched off by the reader. Nothing pending. */
    data object OffByChoice : Armed
}

/**
 * Works out when the next reminder is due and sets it.
 *
 * **This is the piece task 3 never had.** Task 3 proved the alarm plumbing works using a
 * temporary button that fired the nudge fifteen seconds later, and that button was
 * removed during the task 4/5 rewrite. From then until now nothing in the app scheduled
 * anything — the machinery was real and idle. `arm` is what actually makes it a daily
 * reminder.
 *
 * It is called from three places, deliberately overlapping, because a reminder that
 * silently stops is worse than no reminder:
 *  - every time the app opens
 *  - after each nudge fires, to set tomorrow's
 *  - after a reboot, which wipes every alarm the phone had
 */
object NudgeScheduler {

    /**
     * Used when "after Maghrib" cannot be honoured.
     *
     * Eight in the evening: past sunset everywhere in the tropics all year, and a
     * defensible evening hour anywhere else. It is a stand-in, never a silent default —
     * whenever this is used the app says that it is being used.
     */
    val FALLBACK_TIME: LocalTime = LocalTime.of(20, 0)

    fun arm(context: Context, now: ZonedDateTime = ZonedDateTime.now()): Armed {
        val schedule = WirdStore(context).nudgeSchedule

        if (schedule is NudgeSchedule.Off) {
            Nudge.cancel(context)
            Log.i(NudgeReceiver.TAG, "reminder is off, nothing scheduled")
            return Armed.OffByChoice
        }

        val place = Where.best(context)
        val wanted = schedule.nextAfter(now, place?.coordinates)

        if (wanted != null && place != null) {
            val exact = Nudge.schedule(context, wanted.toInstant().toEpochMilli())
            Log.i(
                NudgeReceiver.TAG,
                "next nudge $wanted (${if (exact) "exact" else "inexact"}, ${place.source})",
            )
            // Remembered so the self-check can compare it against when the alarm actually
            // ran. PLAN task 15.
            WirdStore(context).lastArmedFor = wanted.toLocalDateTime()
            return Armed.At(wanted, exact, place.source)
        }

        // Either no coordinates, or a prayer with no time this week. Say which.
        val fallbackAt = NudgeSchedule.AtClockTime(FALLBACK_TIME).nextAfter(now, null)!!
        val exact = Nudge.schedule(context, fallbackAt.toInstant().toEpochMilli())
        Log.w(
            NudgeReceiver.TAG,
            "no prayer time available (place=$place), falling back to $fallbackAt",
        )
        WirdStore(context).lastArmedFor = fallbackAt.toLocalDateTime()
        return Armed.AtFallback(fallbackAt, exact)
    }
}
