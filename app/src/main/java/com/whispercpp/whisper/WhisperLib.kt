package com.whispercpp.whisper

/**
 * The raw bridge to whisper.cpp. **PLAN task 14.**
 *
 * ⚠ **This package name is not ours and must not be tidied.** It is `com.whispercpp.whisper`
 * rather than `com.mosman.wird.*` because **JNI links by mangled symbol name**: upstream's
 * `jni.c` exports `Java_com_whispercpp_whisper_WhisperLib_00024Companion_initContext`, and the
 * runtime finds a native function only if the class's package, class name and `$Companion`
 * suffix reproduce that string exactly.
 *
 * The alternative was renaming every symbol in their C file — and renaming them again after
 * every upgrade of a fast-moving upstream. **One oddly-placed Kotlin file is a smaller price
 * than a permanent fork**, and it is why `app/src/main/cpp/CMakeLists.txt` compiles their
 * `jni.c` directly out of the submodule.
 *
 * **Nothing in the app calls this.** It is the wire, not the appliance — [com.mosman.wird.audio
 * .Recogniser] owns thread counts, model lifetime and the fact that any of this is optional.
 */
class WhisperLib {
    companion object {

        /**
         * ⚠ **Loaded lazily, and failure is expected rather than exceptional.**
         *
         * The model is an optional download, so a phone may never have it — and on a device
         * without arm64 the library is not in the APK at all. `System.loadLibrary` throws in
         * that case, and throwing at class-load would take the whole app down over a feature
         * the reader never asked for. [available] is how everything else asks.
         */
        val available: Boolean by lazy {
            runCatching { System.loadLibrary("whisper") }.isSuccess
        }

        external fun initContext(modelPath: String): Long

        external fun freeContext(contextPtr: Long)

        external fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray)

        external fun getTextSegmentCount(contextPtr: Long): Int

        external fun getTextSegment(contextPtr: Long, index: Int): String

        external fun getSystemInfo(): String
    }
}
