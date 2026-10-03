package com.mosman.wird.nudge

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mosman.wird.MainActivity
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.assignmentOn
import com.mosman.wird.domain.ayahRangeIn
import com.mosman.wird.domain.pages
import java.time.LocalDate

/** Fires when the alarm goes off, and posts the notification. */
class NudgeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "nudge fired at ${System.currentTimeMillis()}")

        val today = LocalDate.now()
        val store = WirdStore(context)

        // **Tomorrow's reminder is set before anything else can go wrong.** An alarm
        // fires once; if this method returned without arming the next one the reminder
        // would quietly be a one-off, and that failure looks exactly like the app
        // working until the second morning.
        NudgeScheduler.arm(context)

        // **PLAN task 15's self-check, and it must be stamped before every early return.**
        // The question it answers is "did the alarm run at all?", not "was a notification
        // shown". A receiver that woke and then deliberately stayed quiet - because the day
        // was already read - has still proved the alarm survived the battery manager.
        //
        // It was first written below the already-read return, which would have reported a
        // killed alarm on exactly the days the reader had done best.
        store.lastNudgeFiredAt = java.time.LocalDateTime.now()

        // **Away days are silent, and this is the second lock on that.** The scheduler
        // already arms for the far side of the trip, so nothing should fire in here at all;
        // this catches the alarm that was already set when the pause was made. A reminder
        // arriving on a day someone told the app they were travelling is the exact thing
        // Sacred Rule 3 is about. PLAN task 22.
        val away = store.away
        if (away != null && today in away) {
            Log.i(TAG, "away until ${away.until}, staying quiet")
            return
        }

        Nudge.createChannel(context)

        if (!store.isSetUp) {
            Log.i(TAG, "not set up yet, staying quiet")
            return
        }

        // Every track this alarm was for. Older alarms carry one id; very old ones none.
        val allTracks = store.getReadingTracks()
        val wanted = intent.getStringExtra(Nudge.EXTRA_TRACK_IDS)?.split(',')?.filter { it.isNotBlank() }
            ?: listOfNotNull(intent.getStringExtra(Nudge.EXTRA_TRACK_ID))
        val tracks = wanted.mapNotNull { id -> allTracks.firstOrNull { it.id == id } }
            .ifEmpty { listOf(store.activeTrack(today)) }

        for (track in tracks) {
            // Sacred Rule 3. A track already read today stays quiet.
            if (track.isCompletedToday(today)) {
                Log.i(TAG, "track '${track.name}' already read today, staying quiet")
                continue
            }
            post(context, track, today)
        }
    }

    /** One track's notification, under that track's own number. */
    private fun post(context: Context, track: com.mosman.wird.domain.ReadingTrack, today: LocalDate) {
        // The same portion Home shows: this track's position, its weekday plan and its
        // direction. It used to ignore the direction, so a back-to-front track was told the
        // wrong page.
        val assignment = track.assignmentOn(today)
        val surahs = com.mosman.wird.domain.SurahIndex.across(assignment.pages)
        val primarySurah = surahs.firstOrNull() ?: com.mosman.wird.domain.SurahIndex.on(assignment.startPage).firstOrNull()
        val ayahRange = if (primarySurah != null) {
            // Today's verses only: a half-page portion used to be announced as the whole page.
            assignment.ayahRangeIn(
                primarySurah.number,
                track.startVerseSurah?.let { s -> track.startVerseAyah?.let { a -> s to a } },
            )
        } else null

        val pageSpan = if (assignment.startPage == assignment.endPage) {
            "Page ${assignment.startPage}"
        } else {
            "Pages ${assignment.startPage}–${assignment.endPage}"
        }

        val contentText = if (primarySurah != null && ayahRange != null) {
            "Time to recite ${primarySurah.name} ($ayahRange) · $pageSpan"
        } else if (primarySurah != null) {
            "Time to recite ${primarySurah.name} · $pageSpan"
        } else {
            "Time to recite $pageSpan"
        }

        // Each track's own number, and request codes built from it: with shared codes, a second
        // track's notification rewrote the first one's tap and reply buttons to its own track.
        val id = Nudge.notificationIdFor(track.id)

        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Nudge.EXTRA_FROM_NUDGE, true)
                .putExtra(Nudge.EXTRA_TRACK_ID, track.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, Nudge.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Your Daily Wird · ${track.name}")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)

        CommitReceiver.REPLIES.forEachIndexed { i, phrase ->
            val reply = PendingIntent.getBroadcast(
                context,
                // Distinct per button and per track, or they overwrite each other.
                id * 4 + i,
                Intent(context, CommitReceiver::class.java)
                    .putExtra(CommitReceiver.EXTRA_SAID, phrase)
                    .putExtra(Nudge.EXTRA_TRACK_ID, track.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, phrase, reply)
        }

        context.getSystemService(NotificationManager::class.java).notify(id, builder.build())
        Log.i(TAG, "notification posted for '${track.name}': $contentText")
    }

    companion object {
        const val TAG = "WirdNudge"
    }
}
