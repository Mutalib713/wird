package com.mosman.wird.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * How good the recitation audio should sound, against what it costs to fetch.
 *
 * **Measured 2026-08-16 on page 442 (Ya-Sin 28–40, 13 ayahs):** 2,376,926 B at 128 kbps
 * against roughly half that at 64. For scale, the mushaf page's own font is 154 KB — so
 * audio is the most expensive thing this app will ever download, by an order of
 * magnitude, and it is downloaded again for every new page.
 *
 * Mutalib chose "let me pick" on 2026-08-16, defaulting to [LIGHT].
 */
enum class AudioQuality(val label: String, val perPageMb: String) {

    /**
     * 64 kbps mono. About 1.1 MB a page, ~34 MB a month for a page a day.
     *
     * The same bitrate Wird already records *his* recitations at, which he has listened
     * back to and been happy with. Only everyayah.com publishes Shatri at this size; the
     * Quran.com API points exclusively at the 128 kbps set.
     */
    LIGHT("Lighter on data", "~1.1 MB a page"),

    /**
     * 128 kbps. About 2.3 MB a page, ~68 MB a month.
     *
     * Served by Quran.com's own CDN, which answered from a Ghanaian edge when this was
     * measured (`CDN-RequestCountryCode: GH`), so it should be the quicker of the two for
     * the people who will actually use this.
     */
    BETTER("Better audio", "~2.3 MB a page");

    /** everyayah and Quran.com agree on the filename: three digits of each. */
    fun urlFor(surah: Int, ayah: Int): String {
        val file = "%03d%03d.mp3".format(surah, ayah)
        return when (this) {
            LIGHT -> "https://everyayah.com/data/Abu_Bakr_Ash-Shaatree_64kbps/$file"
            BETTER -> "https://verses.quran.com/Shatri/mp3/$file"
        }
    }
}

/** What the listen control is doing. Built as a set rather than discovered later. */
sealed interface AudioState {
    data object Idle : AudioState

    /** Downloading. [done] of [total] ayahs are on disk. */
    data class Fetching(val done: Int, val total: Int) : AudioState

    /** [index] is 0-based, so the screen can say "3 of 13". */
    data class Playing(val verseKey: String, val index: Int, val total: Int) : AudioState

    data class Failed(val reason: String) : AudioState
}

/**
 * Abu Bakr al-Shatri reciting today's portion.
 *
 * PROFILE.md § 4 calls this "the lazy-day escape hatch, so the ask can drop to *just
 * listen*". That is the whole design brief: on a day when reading is not going to happen,
 * hearing it should still be possible.
 *
 * One MP3 per ayah, fetched on demand and kept. Chained with plain [MediaPlayer] rather
 * than ExoPlayer — a media library would be several megabytes of dependency to play a
 * list of files in order, and app size is a first-class concern here.
 *
 * **Nothing here uploads anything**, and the fetch is a plain GET of a public file.
 */
class PortionAudio(private val context: Context) {

    private var player: MediaPlayer? = null
    private var cancelled = false

    /**
     * Make sure every ayah in [verses] is on disk, reporting progress as it goes.
     *
     * Already-cached ayahs cost nothing, so this is free from the second listen onward —
     * which is what makes the airplane-mode promise true.
     *
     * @param verses verse keys, "36:28" style, in reading order.
     * @return the files in the same order, or null if any could not be fetched. All or
     * nothing on purpose: a portion with a hole in the middle would stop halfway through
     * and look like a crash.
     */
    suspend fun ensureCached(
        verses: List<String>,
        quality: AudioQuality,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<File>? = withContext(Dispatchers.IO) {
        val dir = dirFor(quality)
        val files = mutableListOf<File>()

        verses.forEachIndexed { i, key ->
            if (cancelled) return@withContext null
            val surah = key.substringBefore(':').toIntOrNull() ?: return@withContext null
            val ayah = key.substringAfter(':').toIntOrNull() ?: return@withContext null

            val f = File(dir, "%03d%03d.mp3".format(surah, ayah))
            if (!f.exists() || f.length() < MIN_PLAUSIBLE_BYTES) {
                if (f.exists()) f.delete()
                val bytes = getBytes(quality.urlFor(surah, ayah)) ?: return@withContext null
                // A 678-byte HTML error page arrives with a 200 from these CDNs — the same
                // trap the fonts had. Trust the byte count, never the status line.
                if (bytes.size < MIN_PLAUSIBLE_BYTES) {
                    Log.w(TAG, "$key was only ${bytes.size} bytes — not audio")
                    return@withContext null
                }
                f.writeBytes(bytes)
            }
            files += f
            onProgress(i + 1, verses.size)
        }

        Log.i(TAG, "portion cached: ${files.size} ayahs, ${files.sumOf { it.length() } / 1024} KB")
        prune(dir, keep = files)
        files
    }

    /**
     * Play [files] back to back, in order.
     *
     * Chained on completion rather than concatenated: each ayah is its own file, and
     * knowing which one is playing is what lets the screen name the ayah you are hearing.
     */
    fun play(
        files: List<File>,
        verses: List<String>,
        onVerse: (index: Int) -> Unit,
        onFinished: () -> Unit,
    ) {
        stop()
        cancelled = false
        playFrom(0, files, verses, onVerse, onFinished)
    }

    private fun playFrom(
        index: Int,
        files: List<File>,
        verses: List<String>,
        onVerse: (Int) -> Unit,
        onFinished: () -> Unit,
    ) {
        if (cancelled || index >= files.size) {
            stop()
            onFinished()
            return
        }
        onVerse(index)
        player = MediaPlayer().apply {
            runCatching {
                // Same reasoning as the recording playback: without this Android logs
                // usage=USAGE_UNKNOWN, guesses, and the volume keys may not reach it.
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(files[index].absolutePath)
                setOnCompletionListener {
                    runCatching { release() }
                    player = null
                    playFrom(index + 1, files, verses, onVerse, onFinished)
                }
                prepare()
                start()
            }.onFailure {
                Log.w(TAG, "could not play ${verses.getOrNull(index)}", it)
                stop()
                onFinished()
            }
        }
    }

    fun stop() {
        cancelled = true
        runCatching { player?.release() }
        player = null
    }

    fun release() = stop()

    // ---- disk ----

    private fun dirFor(quality: AudioQuality) =
        File(File(context.filesDir, "recitation-audio"), quality.name).apply { mkdirs() }

    /**
     * Keep the cache from growing without limit.
     *
     * A page a day at 128 kbps is 830 MB a year if nothing is ever deleted, which is not a
     * thing to do to someone's phone quietly. Oldest-touched go first, and whatever the
     * current portion needs is never a candidate — deleting the thing you are about to
     * play would be a strange way to save space.
     */
    private fun prune(dir: File, keep: List<File>) {
        val keepNames = keep.map { it.name }.toSet()
        val all = dir.listFiles()?.toList() ?: return
        var total = all.sumOf { it.length() }
        if (total <= MAX_CACHE_BYTES) return

        all.filter { it.name !in keepNames }
            .sortedBy { it.lastModified() }
            .forEach { f ->
                if (total <= MAX_CACHE_BYTES) return@forEach
                total -= f.length()
                f.delete()
            }
        Log.i(TAG, "pruned audio cache to ${total / 1024} KB")
    }

    private fun getBytes(url: String): ByteArray? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Wird/0.1 (Android)")
            }
            if (conn.responseCode !in 200..299) {
                Log.w(TAG, "HTTP ${conn.responseCode} for $url")
                return null
            }
            conn.inputStream.use { it.readBytes() }
        } catch (e: Exception) {
            Log.w(TAG, "audio fetch failed: $url", e)
            null
        } finally {
            conn?.disconnect()
        }
    }

    private companion object {
        const val TAG = "WirdRecital"

        /** The error pages these CDNs return with a 200 are around 678 bytes. */
        const val MIN_PLAUSIBLE_BYTES = 4_000L

        /** ~20 pages at the heavier bitrate. Enough to revisit, bounded enough to forget. */
        const val MAX_CACHE_BYTES = 50L * 1024 * 1024
    }
}
