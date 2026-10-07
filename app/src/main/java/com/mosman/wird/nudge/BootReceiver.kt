package com.mosman.wird.nudge

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Alarms do not survive a reboot, and scheduled times become stale when the timezone
 * or clock changes, or when the user grants exact-alarm permission in system settings.
 *
 * This receiver catches those system signals and re-arms the reminder immediately.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (!isSupportedAction(action)) return

        Log.i(NudgeReceiver.TAG, "system signal received ($action), re-arming nudge")
        NudgeScheduler.arm(context)
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED) {
            com.mosman.wird.widget.MidnightWidgetReceiver.schedule(context)
        }
    }

    companion object {
        const val ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

        fun isSupportedAction(action: String?): Boolean = when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> true
            else -> false
        }
    }
}
