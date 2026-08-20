package com.mosman.wird.audio

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetching a recitation model. **PLAN task 14, on the terms he set.**
 *
 * *"For the audio and models the user has to download it themselves"* — so this is never
 * automatic, never on first launch, and never on his behalf. Someone asks for it in Settings,
 * having been shown what it weighs.
 *
 * ⚠ **It downloads beside the target and renames at the end**, which is the whole reason this
 * is not four lines. A 42 MB download interrupted at 40 MB leaves a file that exists, has a
 * plausible name and is useless — and whisper.cpp handed a truncated model does not politely
 * refuse, it can take the process down in native code where no Kotlin `catch` can reach.
 * **A partial file must never be able to wear the finished file's name.**
 *
 * The same trap the fonts and the recitation audio both hit earlier in this project: a CDN error
 * page arrives with a 200 and a believable filename. Here the guard is the byte count, checked
 * against what the server said it would send.
 */
object ModelDownload {

    /**
     * Download [model] into [into], reporting bytes as they arrive.
     *
     * @return true when the finished file is in place and the right size.
     */
    suspend fun fetch(
        model: RecitationModel,
        into: File,
        onProgress: (done: Long, total: Long) -> Unit = { _, _ -> },
    ): Boolean = withContext(Dispatchers.IO) {
        val target = File(into, model.fileName)
        if (target.exists() && target.length() > MIN_PLAUSIBLE) {
            Log.i(TAG, "${model.name} already here")
            return@withContext true
        }

        val partial = File(into, model.fileName + ".part")
        runCatching { partial.delete() }

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(model.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 30_000
                // HuggingFace answers a redirect to its CDN, and without this the body is the
                // redirect page rather than the model.
                instanceFollowRedirects = true
            }
            if (connection.responseCode !in 200..299) {
                Log.w(TAG, "${model.name} refused: HTTP ${connection.responseCode}")
                return@withContext false
            }

            val expected = connection.contentLengthLong
            var written = 0L

            connection.inputStream.use { source ->
                partial.outputStream().use { sink ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        // Cancellation is checked here rather than left to the coroutine, so a
                        // stopped download stops mid-file instead of after the last byte.
                        if (!currentCoroutineContext().isActive) {
                            Log.i(TAG, "${model.name} cancelled at $written bytes")
                            return@withContext false
                        }
                        val read = source.read(buffer)
                        if (read < 0) break
                        sink.write(buffer, 0, read)
                        written += read
                        onProgress(written, expected)
                    }
                }
            }

            // ⚠ The two checks that stop a broken file being trusted. `expected` is -1 when the
            // server sends no length, so a plain equality test would reject a good download.
            if (expected > 0 && written != expected) {
                Log.w(TAG, "${model.name} truncated: $written of $expected")
                partial.delete()
                return@withContext false
            }
            if (written < MIN_PLAUSIBLE) {
                Log.w(TAG, "${model.name} was only $written bytes - not a model")
                partial.delete()
                return@withContext false
            }

            // Only now does it get the real name. Anything that reads `installed()` before this
            // moment sees no model rather than half of one.
            val renamed = partial.renameTo(target)
            Log.i(TAG, "${model.name} downloaded: $written bytes, renamed=$renamed")
            renamed
        } catch (e: Exception) {
            Log.w(TAG, "${model.name} failed", e)
            runCatching { partial.delete() }
            false
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    private const val TAG = "WirdModel"

    /** Even the smaller model is 42 MB. Anything under 10 is an error page. */
    private const val MIN_PLAUSIBLE = 10L * 1024 * 1024
}
