package com.mosman.wird.mushaf

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mosman.wird.MainActivity
import com.mosman.wird.domain.Mushaf
import java.io.File
import com.mosman.wird.audio.RecitationModel
import com.mosman.wird.audio.ModelDownload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Fetching the whole mushaf, in the background, with a notification you can watch.
 *
 * **His instruction, 2026-08-19:** *"do the download, cos it's supposed to run in the background
 * and even a notification showing what's left."* He is right, and the version before this was
 * honest about being wrong: the download lived in the Settings screen's own scope, so **leaving
 * Settings killed it**. At roughly four seconds a page, the whole mushaf is about forty-five
 * minutes, and no one is going to sit on one screen for forty-five minutes.
 *
 * ### Why a foreground service and not a background job
 *
 * A **foreground service** is Android's name for work that keeps running with a notification
 * showing while it does — the same machinery a music player or a file download uses. The system
 * will not quietly kill it, because the notification means the user knows it is happening.
 *
 * ⚠ **That last part is the whole bargain, and it is why this is the right tool rather than a
 * clever one.** Android hands out survival in exchange for visibility. Anything that tried to
 * download ninety megabytes invisibly would be killed by exactly the battery managers § 10 warns
 * about on Transsion phones — and would deserve to be.
 *
 * ### What it promises, and what it does not
 *
 * - **Resumable, by construction.** [MushafRepository.downloadAll] skips pages already on disk,
 *   so stopping it costs nothing but time. Killing the notification and starting again picks up
 *   exactly where it left off.
 * - **Cancellable from the notification**, because a forty-five minute download someone cannot
 *   stop is a hostage situation, not a feature.
 * - ⬜ **It does not wait for wifi.** Someone who starts it on mobile data will spend about
 *   ninety megabytes of it. The Settings row says so before the tap; a proper metered-network
 *   guard is worth having and is not built.
 */
class MushafDownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // ⚠ **Models come through here for the same reason pages do, and it was a real bug.**
        // The model download first shipped inside the Settings screen's own coroutine scope,
        // so walking away from Settings cancelled 78 MB mid-flight. Mutalib hit exactly this
        // on 2026-08-20 - the download survived only because he happened to stay on the screen
        // for four minutes watching a row that told him nothing.
        val wanted = intent?.getStringExtra(EXTRA_MODEL)
        if (wanted != null) {
            startModel(RecitationModel.valueOf(wanted))
            return START_STICKY
        }

        // Already running: a second tap must not start a second sweep over 604 pages.
        if (job?.isActive == true) return START_STICKY

        createChannel()
        startForeground(NOTIFICATION_ID, notification(0, Mushaf.PAGES))

        job = scope.launch {
            val repo = MushafRepository(applicationContext)
            var lastShown = 0
            val failed = repo.downloadAll { done, total ->
                // Repainting a notification 604 times is 604 wakeups of the system UI for a bar
                // that moves a fifth of a pixel. Every ten pages is often enough to look alive.
                if (done - lastShown >= 10 || done == total) {
                    lastShown = done
                    notify(notification(done, total))
                }
            }
            notify(done(failed))
            Log.i(MushafRepository.TAG, "whole-mushaf download finished, $failed missing")
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
        return START_STICKY
    }

    /** A recitation model, with its own progress and its own finishing line. */
    private fun startModel(model: RecitationModel) {
        if (job?.isActive == true) return
        createChannel()
        startForeground(NOTIFICATION_ID, modelNotification(model, 0, 0))

        job = scope.launch {
            var lastPercent = -1
            val ok = ModelDownload.fetch(
                model = model,
                into = File(applicationContext.filesDir, "models"),
            ) { done, total ->
                // Percent rather than every buffer: a 78 MB file arrives in 64 KB pieces, and
                // repainting the notification 1,200 times says nothing a bar does not.
                val percent = if (total > 0) (done * 100 / total).toInt() else -1
                if (percent != lastPercent) {
                    lastPercent = percent
                    notify(modelNotification(model, done, total))
                }
            }
            notify(modelDone(model, ok))
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun modelNotification(model: RecitationModel, done: Long, total: Long): Notification {
        val stop = PendingIntent.getService(
            this,
            2,
            Intent(this, MushafDownloadService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val mb = (total.takeIf { it > 0 } ?: (model.megabytes * 1024L * 1024L))
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Getting the recitation checker")
            .setContentText("${done / 1024 / 1024} of ${mb / 1024 / 1024} MB")
            .setProgress(100, if (mb > 0) (done * 100 / mb).toInt() else 0, mb <= 0)
            .setOngoing(true)
            .addAction(0, "Stop", stop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun modelDone(model: RecitationModel, ok: Boolean): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(if (ok) "Wird can listen now" else "That download didn't finish")
            .setContentText(
                if (ok) "${model.label} is on your phone. Nothing you record is uploaded."
                else "Nothing was kept. Open Settings to try again."
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun notification(done: Int, total: Int): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, MushafDownloadService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Getting the mushaf")
            // The figure people actually want is how much is left, not how much is done.
            .setContentText("${total - done} pages to go")
            .setProgress(total, done, false)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * The last word, and it stays after the download does.
     *
     * A progress notification that simply vanishes leaves someone wondering whether it finished
     * or died. This one says which, and a partial result says how to finish it — which is true,
     * because the download resumes.
     */
    private fun done(failed: Int): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(if (failed == 0) "The mushaf is on your phone" else "Mushaf mostly downloaded")
            .setContentText(
                if (failed == 0) "All 604 pages. It reads offline now."
                else "$failed pages didn't arrive. Tap Mushaf pages again to finish."
            )
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun notify(n: Notification) {
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, n)
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Downloads",
            // Low: a download is something to glance at, never something to interrupt for.
            // The daily nudge is the only thing in this app allowed to be loud.
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Progress while the mushaf is being fetched" }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "wird_downloads"
        private const val NOTIFICATION_ID = 4201
        private const val ACTION_STOP = "com.mosman.wird.STOP_DOWNLOAD"
        private const val EXTRA_MODEL = "com.mosman.wird.MODEL"

        /** Fetch a recitation model, surviving whatever screen asked for it. */
        fun startModel(context: Context, model: RecitationModel) {
            context.startForegroundService(
                Intent(context, MushafDownloadService::class.java)
                    .putExtra(EXTRA_MODEL, model.name)
            )
        }

        /** Begin, or do nothing if it is already going. */
        fun start(context: Context) {
            // minSdk is already above 26, so the pre-Oreo branch lint flagged here was dead
            // code guarding against a version this app cannot run on.
            context.startForegroundService(Intent(context, MushafDownloadService::class.java))
        }
    }
}
