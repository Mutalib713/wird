package com.mosman.wird.nudge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Alarms do not survive a reboot. Without this, a phone that restarts overnight loses
 * the nudge silently, which looks exactly like the app not working.
 *
 * It recomputes rather than restoring the time it had stored. Prayer times move by about
 * a minute a day, so a phone that was off for a week would come back with a stale sunset
 * — and recomputing costs less than a millisecond.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.i(NudgeReceiver.TAG, "boot completed, re-arming nudge")
        NudgeScheduler.arm(context)
    }
}
