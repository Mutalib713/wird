package com.mosman.wird.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Recording yourself reciting, and hearing it back.
 *
 * AAC in an MP4 container: small, and it is the format WhatsApp accepts as a voice note
 * without re-encoding, which task 11 will need.
 *
 * Nothing here uploads anything. The file lands in app-private storage, which the
 * manifest excludes from cloud backup and device transfer three times over.
 */
class Recitation(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var target: File? = null

    val isRecording: Boolean get() = recorder != null


    /** @return true if recording actually started. */
    fun start(into: File): Boolean {
        stop()
        return runCatching {
            into.parentFile?.mkdirs()
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            // Voice, not music. 64 kbps mono is clear for speech and keeps a ten-minute
            // recitation under 5 MB — this stays on a phone that also holds fonts.
            r.setAudioChannels(1)
            r.setAudioSamplingRate(44_100)
            r.setAudioEncodingBitRate(64_000)
            r.setOutputFile(into.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            target = into
            true
        }.getOrElse {
            Log.w(TAG, "could not start recording", it)
            runCatching { recorder?.release() }
            recorder = null
            false
        }
    }

    /**
     * Stop and keep the file.
     *
     * @return the file if it holds something, or null if it does not. A recorder stopped
     * within a moment of starting throws and leaves a zero-byte file behind; treating
     * that as a successful recitation would be the app lying about the one thing it is
     * supposed to be honest about.
     */
    fun stop(): File? {
        val r = recorder ?: return null
        recorder = null
        val f = target
        target = null
        runCatching { r.stop() }.onFailure { Log.w(TAG, "recorder stopped too early", it) }
        runCatching { r.release() }
        return f?.takeIf { it.exists() && it.length() > MIN_PLAUSIBLE_BYTES }
            ?.also { Log.i(TAG, "recorded ${it.length()} bytes") }
            ?: run {
                f?.delete()
                null
            }
    }

    /**
     * How loud it is hearing you, 0 to 1.
     *
     * Real amplitude from the microphone, not a decorative animation. A pulse that beats
     * on a timer looks identical whether the mic is working or muted; one that follows
     * your voice is evidence. If this stays flat while you recite, something is wrong and
     * you find out now rather than when you play it back.
     *
     * `getMaxAmplitude` reports the peak since the last call and resets, so this is only
     * meaningful when polled steadily.
     */
    fun level(): Float {
        val r = recorder ?: return 0f
        val peak = runCatching { r.maxAmplitude }.getOrDefault(0)
        // The scale is 0..32767 and speech sits low in it, so a straight ratio barely
        // moves. Square-root opens up the quiet end where a voice actually lives.
        return kotlin.math.sqrt((peak / 32_767f).coerceIn(0f, 1f))
    }

    /** Throw away whatever is being recorded. */
    fun cancel() {
        val f = target
        stop()
        f?.delete()
    }

    fun play(file: File, onFinished: () -> Unit = {}) {
        stopPlaying()
        player = MediaPlayer().apply {
            runCatching {
                // Without this Android logs `usage=USAGE_UNKNOWN` and guesses: the volume
                // keys may not reach it and it can come out of the earpiece rather than
                // the speaker. Saying it is speech, played as media, makes the phone
                // behave the way anyone expects when they press volume up.
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener { onFinished(); stopPlaying() }
                prepare()
                start()
            }.onFailure {
                Log.w(TAG, "could not play back", it)
                stopPlaying()
                onFinished()
            }
        }
    }

    fun stopPlaying() {
        runCatching { player?.release() }
        player = null
    }

    fun release() {
        cancel()
        stopPlaying()
    }

    private companion object {
        const val TAG = "WirdAudio"
        /** An MP4 header alone is around 1 KB. Anything this small holds no speech. */
        const val MIN_PLAUSIBLE_BYTES = 2_000L
    }
}
