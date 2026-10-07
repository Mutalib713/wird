package com.mosman.wird.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * B33: Inexact wake-up just after midnight to refresh the home screen widget.
 *
 * Without this, a widget that displayed "TODAY, DONE" yesterday keeps showing yesterday's
 * completed state all night until the user opens the app the next day, because Android's
 * standard 30-minute widget refresh is suspended during device Doze.
 */
class MidnightWidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "midnight widget refresh alarm fired")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                refreshWidget(context)
            } catch (t: Throwable) {
                Log.w(TAG, "failed to refresh widget at midnight", t)
            } finally {
                schedule(context)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val TAG = "WirdWidgetMidnight"
        const val ACTION_MIDNIGHT_REFRESH = "com.mosman.wird.MIDNIGHT_WIDGET_REFRESH"
        private const val REQUEST_CODE = 2001

        /**
         * Next midnight plus one minute to ensure LocalDate has rolled over.
         */
        fun nextMidnightRefresh(now: ZonedDateTime): ZonedDateTime {
            return now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusMinutes(1)
        }

        fun schedule(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            val target = nextMidnightRefresh(now)
            val triggerAtMillis = target.toInstant().toEpochMilli()

            val intent = Intent(context, MidnightWidgetReceiver::class.java).apply {
                action = ACTION_MIDNIGHT_REFRESH
            }
            val pending = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            am.setAndAllowWhileIdle(AlarmManager.RTC, triggerAtMillis, pending)
            Log.i(TAG, "scheduled next midnight widget refresh for $target")
        }

        fun cancel(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            val intent = Intent(context, MidnightWidgetReceiver::class.java).apply {
                action = ACTION_MIDNIGHT_REFRESH
            }
            val pending = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            am.cancel(pending)
        }
    }
}
