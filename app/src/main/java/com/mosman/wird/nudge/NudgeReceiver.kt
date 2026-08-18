package com.mosman.wird.nudge

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mosman.wird.MainActivity
import com.mosman.wird.data.DayLogStore
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.todaysAssignment
import java.time.LocalDate

/** Fires when the alarm goes off, and posts the notification. */
class NudgeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "nudge fired at ${System.currentTimeMillis()}")

        val today = LocalDate.now()
        val days = DayLogStore(context)
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

        // Sacred Rule 3. Someone who has already read today does not need reminding that
        // they read today — that is a notification whose only content is a small demand
        // for attention, which is the thing this app promised not to be.
        if (days.isDone(today)) {
            Log.i(TAG, "already read today, staying quiet")
            return
        }

        Nudge.createChannel(context)

        if (!store.isSetUp) {
            Log.i(TAG, "not set up yet, staying quiet")
            return
        }

        val assignment = todaysAssignment(
            startUnit = store.positionUnit,
            plan = store.plan,
            date = today,
        )
        val portion = if (assignment.startPage == assignment.endPage) {
            "Page ${assignment.startPage}"
        } else {
            "Pages ${assignment.startPage}–${assignment.endPage}"
        }

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Nudge.EXTRA_FROM_NUDGE, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // **The nudge became a question on 2026-08-18 - PLAN task 21.**
        //
        // It used to be a statement: here is today's portion, goodbye. That addresses
        // forgetting, which section 2 found is the SMALLER half of the problem - about 6 of
        // 14 missed days against 8 lost to procrastination. A statement gives someone who
        // has already seen it nothing new to do.
        //
        // Asking, and letting the answer be one tap, is the experiment: does naming a time
        // to something that checks back change anything? Section 5b says that if it does
        // not, the companion is theatre and gets cut.
        //
        // Sacred Rule 3 still governs every word. It asks; it does not push. No streak, no
        // "don't break it", and "Not today" is a real answer that costs nothing.
        val builder = NotificationCompat.Builder(context, Nudge.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Reading today?")
            .setContentText(portion)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)

        CommitReceiver.REPLIES.forEachIndexed { i, phrase ->
            val reply = PendingIntent.getBroadcast(
                context,
                // A distinct request code per action, or every button would overwrite the
                // one before it and all three would send the same phrase.
                100 + i,
                Intent(context, CommitReceiver::class.java)
                    .putExtra(CommitReceiver.EXTRA_SAID, phrase),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, phrase, reply)
        }

        val notification = builder.build()

        context.getSystemService(NotificationManager::class.java)
            .notify(Nudge.NOTIFICATION_ID, notification)

        Log.i(TAG, "notification posted: $portion")
    }

    companion object {
        const val TAG = "WirdNudge"
    }
}
