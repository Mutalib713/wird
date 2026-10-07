package com.mosman.wird.audio

import android.content.Context
import android.util.Log
import com.mosman.wird.data.WirdStore
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
    /** The exact size of the right file, in bytes. */
    val bytes: Long,
    /** The right file's SHA-256, as Hugging Face publishes it for this exact version. */
    val sha256: String,
) {
    TINY(
        label = "Smaller",
        fileName = "tarteel-tiny-q8_0.bin",
        megabytes = 42,
        url = MODEL_REPO + "tarteel-ai-whisper-tiny-ar-quran-ggml-q8_0.bin",
        bytes = 43_537_433L,
        sha256 = "ef01ab441b004f9e6f1ea98d397b43452b3a45efab7c3928ab37af655dde8b52",
    ),
    BASE(
        label = "More accurate",
        fileName = "tarteel-base-q8_0.bin",
        megabytes = 78,
        url = MODEL_REPO + "tarteel-ai-whisper-base-ar-quran-ggml-q8_0.bin",
        bytes = 81_768_585L,
        sha256 = "7b22bd61ef18112fe92cb2518f37ef4896b85be90a371d4dd43e74749768730f",
    ),
}

/**
 * The model files, pinned to one version of the repository rather than "main".
 *
 * ⚠ **Before 2026-10-04 the app downloaded whatever "main" held that day and trusted it if it was
 * over 10 MB.** Anyone able to change that repository could have changed what runs inside the app.
 * Now the address names one commit, and [ModelDownload] refuses a file whose size or SHA-256 differs
 * from what Hugging Face published for it. Both values were read from the Hugging Face API for this
 * commit (last changed 2026-05-06, licence Apache-2.0). Updating the model means updating all three.
 */
private const val MODEL_REPO =
    "https://huggingface.co/ram-a-dhan/tarteel-whisper-quran-ggml/resolve/4a96d8bb5535a4b6e6f6abd5655b8711a95ae538/"

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

    /** Every model actually sitting on this phone. */
    fun downloaded(): List<RecitationModel> =
        RecitationModel.entries.filter { plausible(it) }

    /**
     * The model this phone will actually use, or null.
     *
     * ⚠ **The reader's choice wins, and that replaced a rule that quietly broke the feature.**
     * The first version always preferred the larger model when both were present. It sounded
     * sensible and it meant *"let me try the smaller one instead"* silently did nothing — which
     * is the one thing PLAN task 14 exists to let him do, since the whole question is whether
     * 78 MB buys anything 42 MB does not.
     *
     * With no choice recorded it falls back to whatever is here, largest first, so a phone with
     * one model needs no decision at all.
     */
    fun installed(): RecitationModel? {
        val here = downloaded()
        if (here.isEmpty()) return null
        val chosen = WirdStore(context).chosenModel
            ?.let { name -> here.firstOrNull { it.name == name } }
        return chosen ?: here.maxByOrNull { it.megabytes }
    }

    /** Remember which of the downloaded models to use. */
    fun choose(model: RecitationModel) {
        WirdStore(context).chosenModel = model.name
    }

    /**
     * ⚠ **Size-checked, because a half-downloaded model is worse than none.**
     *
     * whisper.cpp handed a truncated file does not politely refuse — it can crash the process in
     * native code, where there is no Kotlin exception to catch. The same trap the fonts and the
     * recitation audio both hit: a CDN error page arrives with a 200 and a plausible name.
     *
     * The size must be exactly the right file's. Its SHA-256 is checked once, when it downloads;
     * reading 80 MB again on every launch would cost seconds on a Tecno.
     */
    private fun plausible(model: RecitationModel) = fileFor(model).let { it.exists() && it.length() == model.bytes }

    /** Whether a recitation could be checked right now, without downloading anything. */
    fun ready(): Boolean = WhisperNative.available && installed() != null

    /**
     * Transcribe a recording. Null when it cannot be done, with the reason logged.
     *
     * ⚠ **Slow, and deliberately not on the main thread.** Even batched, a minute of audio takes
     * real seconds on a mid-range phone. The thread count is capped at four: whisper scales with
     * cores, but a phone that gives every core to this is a phone that stutters, and the
     * recitation is already finished — nobody is waiting on a stopwatch.
     */
    suspend fun transcribe(recording: File): String? = withContext(Dispatchers.Default) {
        // Logged before any work, so a run that dies later still proves it started. Without
        // this, "nothing in the log" cannot be told apart from "never called".
        Log.i(TAG, "check requested for ${recording.name}, ${recording.length()} bytes")
        if (!WhisperNative.available) {
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
            ctx = WhisperNative.initContext(fileFor(model).absolutePath)
            if (ctx == 0L) {
                Log.w(TAG, "whisper refused the model file")
                return@withContext null
            }

            val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
            val started = System.currentTimeMillis()
            Log.i(TAG, "transcribing ${audio.size / AudioToPcm.WHISPER_HZ}s on $threads threads…")
            WhisperNative.fullTranscribe(ctx, threads, audio)

            val text = buildString {
                repeat(WhisperNative.getTextSegmentCount(ctx)) { append(WhisperNative.getTextSegment(ctx, it)) }
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
            if (ctx != 0L) runCatching { WhisperNative.freeContext(ctx) }
        }
    }

    private companion object {
        const val TAG = "WirdWhisper"
    }
}
