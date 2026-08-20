package com.mosman.wird.audio

/**
 * The bridge to whisper.cpp. **Ours, in our own package.** PLAN task 14.
 *
 * ⚠ **This replaced `com.whispercpp.whisper.WhisperLib`, and the reason is one line of C.**
 * whisper.cpp ships an Android example whose JNI file hardcodes `params.language = "en"`. It
 * was compiled unmodified to avoid a fork — a decision that looked careful and was wrong, since
 * **a demo carries a demo's assumptions.** Wird records Qur'anic Arabic and runs a model trained
 * on Qur'anic Arabic; telling whisper the audio is English asks the wrong question of it.
 *
 * `app/src/main/cpp/wird_whisper.c` is sixty lines calling their library. That is the ordinary
 * way to use a C library, and it means the parameters that decide what this feature *is* — the
 * language, and that it transcribes rather than translates — live in this repo where they can
 * be read, changed and blamed.
 */
object WhisperNative {

    /**
     * ⚠ **Loaded lazily, and failing is an ordinary outcome.**
     *
     * The model is an optional download and the library is arm64-only, so a phone may simply
     * not have it. `System.loadLibrary` throws in that case, and doing that at class-load would
     * take the whole app down over a feature the reader never asked for.
     */
    val available: Boolean by lazy {
        runCatching { System.loadLibrary("whisper") }.isSuccess
    }

    external fun initContext(modelPath: String): Long

    external fun freeContext(contextPtr: Long)

    /** Returns 0 on success. Anything else means whisper refused the audio. */
    external fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray): Int

    external fun getTextSegmentCount(contextPtr: Long): Int

    external fun getTextSegment(contextPtr: Long, index: Int): String

    external fun getSystemInfo(): String
}
