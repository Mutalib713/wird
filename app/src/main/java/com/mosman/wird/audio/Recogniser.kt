package com.mosman.wird.audio

import android.content.Context
import android.util.Log
import com.whispercpp.whisper.WhisperLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Which recitation model is on the phone, if any. **PLAN task 14, his choice of both sizes.**
 *
 * *"For the audio and models the user has to download it themselves"*, and asked which size:
 * **offer both, let each person choose.** Someone on mobile data takes [TINY]; someone on wifi
 * with a better phone takes [BASE].
 *
 * Both are Apache-2.0 conversions of `tarteel-ai/whisper-base-ar-quran`, which was trained on
 * Qur'anic recitation rather than general Arabic — the reason a model this small is worth
 * anything here at all.
 */
enum class RecitationModel(
    val label: String,
    val fileName: String,
    val megabytes: Int,
    val url: String,
) {
    TINY(
        label = "Smaller",
        fileName = "tarteel-tiny-q8_0.bin",
        megabytes = 42,
        url = "https://huggingface.co/ram-a-dhan/tarteel-whisper-quran-ggml/resolve/main/" +
            "tarteel-ai-whisper-tiny-ar-quran-ggml-q8_0.bin",
    ),
    BASE(
        label = "More accurate",
        fileName = "tarteel-base-q8_0.bin",
        megabytes = 78,
        url = "https://huggingface.co/ram-a-dhan/tarteel-whisper-quran-ggml/resolve/main/" +
            "tarteel-ai-whisper-base-ar-quran-ggml-q8_0.bin",
    ),
}

/**
 * Listening to a recitation, on the phone, with no network. **PLAN task 14's expensive half.**
 *
 * ### What this is for, and what it is not
 *
 * [HeardRecitation] already answers *"was that a recitation at all?"* from loudness alone, for
 * free. This answers the harder question — **what words were said** — and it is the only part of
 * Wird that can tell a recitation from a phone call.
 *
 * ⚠ **It still does not mark anyone's day.** Sacred Rule 6: only a real recording is a
 * recitation, and what this adds is evidence about that recording, never a verdict about a
 * person.
 *
 * ### Offline, and that is the point rather than a nice property
 *
 * Everything runs in-process through whisper.cpp. **Sacred Rule 1 says recordings never leave
 * the device**, so sending audio to a server was never available — which is also why the model
 * has to be downloaded rather than called.
 *
 * ### Why it may simply not be here
 *
 * The model is optional and large, the native library is arm64-only, and an old 32-bit phone
 * has neither. **Every entry point answers null rather than throwing**, and the caller's job is
 * to say "not available" rather than to fail.
 */
class Recogniser(private val context: Context) {

    private val dir = File(context.filesDir, "models").apply { mkdirs() }

    fun fileFor(model: RecitationModel) = File(dir, model.fileName)

    /**
     * The model this phone will actually use, or null.
     *
     * **The larger one wins when both are present**, because someone who downloaded BASE after
     * TINY was asking for accuracy, and the download they made second is the answer.
     */
    fun installed(): RecitationModel? =
        listOf(RecitationModel.BASE, RecitationModel.TINY).firstOrNull { plausible(fileFor(it)) }

    /**
     * ⚠ **Size-checked, because a half-downloaded model is worse than none.**
     *
     * whisper.cpp handed a truncated file does not politely refuse — it can crash the process in
     * native code, where there is no Kotlin exception to catch. The same trap the fonts and the
     * recitation audio both hit: a CDN error page arrives with a 200 and a plausible name.
     */
    private fun plausible(file: File) = file.exists() && file.length() > MIN_PLAUSIBLE_BYTES

    /** Whether a recitation could be checked right now, without downloading anything. */
    fun ready(): Boolean = WhisperLib.available && installed() != null

    /**
     * Transcribe a recording. Null when it cannot be done, with the reason logged.
     *
     * ⚠ **Slow, and deliberately not on the main thread.** Even batched, a minute of audio takes
     * real seconds on a mid-range phone. The thread count is capped at four: whisper scales with
     * cores, but a phone that gives every core to this is a phone that stutters, and the
     * recitation is already finished — nobody is waiting on a stopwatch.
     */
    suspend fun transcribe(recording: File): String? = withContext(Dispatchers.Default) {
        if (!WhisperLib.available) {
            Log.i(TAG, "no native library on this device")
            return@withContext null
        }
        val model = installed() ?: run {
            Log.i(TAG, "no model downloaded")
            return@withContext null
        }

        val audio = AudioToPcm.read(recording) ?: run {
            Log.w(TAG, "could not decode ${recording.name}")
            return@withContext null
        }
        if (audio.size < AudioToPcm.WHISPER_HZ / 2) {
            Log.i(TAG, "under half a second of audio, nothing to transcribe")
            return@withContext null
        }

        var ctx = 0L
        try {
            ctx = WhisperLib.initContext(fileFor(model).absolutePath)
            if (ctx == 0L) {
                Log.w(TAG, "whisper refused the model file")
                return@withContext null
            }

            val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
            val started = System.currentTimeMillis()
            WhisperLib.fullTranscribe(ctx, threads, audio)

            val text = buildString {
                repeat(WhisperLib.getTextSegmentCount(ctx)) { append(WhisperLib.getTextSegment(ctx, it)) }
            }.trim()

            val seconds = audio.size / AudioToPcm.WHISPER_HZ
            val took = (System.currentTimeMillis() - started) / 1000.0
            // PLAN task 14 asks for measured numbers rather than impressions, and this is the
            // line that produces them: how long the audio was against how long it took.
            Log.i(TAG, "transcribed ${seconds}s in ${took}s on $threads threads (${model.name}): $text")
            text.ifEmpty { null }
        } catch (e: Throwable) {
            Log.w(TAG, "transcription failed", e)
            null
        } finally {
            if (ctx != 0L) runCatching { WhisperLib.freeContext(ctx) }
        }
    }

    private companion object {
        const val TAG = "WirdWhisper"

        /** Even the smallest model is 42 MB; anything under 10 is an error page or a stub. */
        const val MIN_PLAUSIBLE_BYTES = 10L * 1024 * 1024
    }
}
