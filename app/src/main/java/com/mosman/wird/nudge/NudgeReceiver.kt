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

        // **Tomorrow's reminder is set before anything else can go wrong.** An alarm
        // fires once; if this method returned without arming the next one the reminder
        // would quietly be a one-off, and that failure looks exactly like the app
        // working until the second morning.
        NudgeScheduler.arm(context)

        // Sacred Rule 3. Someone who has already read today does not need reminding that
        // they read today — that is a notification whose only content is a small demand
        // for attention, which is the thing this app promised not to be.
        if (days.isDone(today)) {
            Log.i(TAG, "already read today, staying quiet")
            return
        }

        Nudge.createChannel(context)

        val store = WirdStore(context)
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

        // Sacred Rule 3: no guilt. It says what today's portion is and gets out of the
        // way. No streak count, no "don't break it", no question you have to answer.
        val notification = NotificationCompat.Builder(context, Nudge.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Today's wird")
            .setContentText(portion)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(Nudge.NOTIFICATION_ID, notification)

        Log.i(TAG, "notification posted: $portion")
    }

    companion object {
        const val TAG = "WirdNudge"
    }
}
