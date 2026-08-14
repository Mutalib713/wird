package com.mosman.wird.nudge

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * The nudge: a scheduled local notification that opens today's portion.
 *
 * Task 3 fires it at a time we pass in. Task 8 replaces that with an offset from a
 * prayer time. Nothing else about this file should need to change for that.
 */
object Nudge {

    const val CHANNEL_ID = "wird_daily"
    const val NOTIFICATION_ID = 1
    const val EXTRA_FROM_NUDGE = "com.mosman.wird.FROM_NUDGE"

    private const val PREFS = "wird_nudge"
    private const val KEY_NEXT_AT = "next_at"

    /** Whether Android will honour an exact alarm from this app right now. */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java)
        return am.canScheduleExactAlarms()
    }

    /**
     * Intent for the system screen where the user grants exact alarms.
     *
     * From Android 14 this permission is denied by default unless the app is a clock or
     * a calendar. Wird is neither, so this screen is the only way in — and the user has
     * to walk there themselves.
     */
    fun exactAlarmSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(android.net.Uri.parse("package:${context.packageName}"))
    }

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily reminder",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Your portion for the day."
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /**
     * Schedule the nudge for [triggerAtMillis].
     *
     * Returns true if it was scheduled exactly, false if Android downgraded it to an
     * inexact alarm. The caller is expected to tell the user which one they got —
     * silently drifting is the failure mode that makes people think the app is broken.
     */
    fun schedule(context: Context, triggerAtMillis: Long): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, NudgeReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val exact = canScheduleExact(context)
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        } else {
            // Still fires in Doze, just not at a precise minute. Better than nothing and
            // far better than crashing, which is what setExact does without permission.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }

        // Alarms do not survive a reboot, so remember when this one was for.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_NEXT_AT, triggerAtMillis)
            .apply()

        return exact
    }

    /** The time the next nudge is set for, or null if none is pending. */
    fun nextAt(context: Context): Long? {
        val at = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_NEXT_AT, 0L)
        return if (at > 0L) at else null
    }

    /** Re-arm after a reboot. Skips anything already in the past. */
    fun rescheduleAfterBoot(context: Context) {
        val at = nextAt(context) ?: return
        if (at <= System.currentTimeMillis()) return
        schedule(context, at)
    }
}
