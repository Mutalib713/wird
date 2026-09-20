package com.mosman.wird.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

fun isWifi(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

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
    fun urlFor(reciter: String = "Abu Bakr al-Shatri", surah: Int, ayah: Int): String {
        val file = "%03d%03d.mp3".format(surah, ayah)
        return when (reciter) {
            "Mishary Rashid Alafasy" -> when (this) {
                LIGHT -> "https://everyayah.com/data/Alafasy_64kbps/$file"
                BETTER -> "https://everyayah.com/data/Alafasy_128kbps/$file"
            }
            "Mahmoud Khalil Al-Husary" -> when (this) {
                LIGHT -> "https://everyayah.com/data/Husary_64kbps/$file"
                BETTER -> "https://everyayah.com/data/Husary_128kbps/$file"
            }
            "Abdul Basit Abdul Samad" -> when (this) {
                LIGHT -> "https://everyayah.com/data/Abdul_Basit_Murattal_64kbps/$file"
                BETTER -> "https://verses.quran.com/AbdulBaset/Murattal/mp3/$file"
            }
            else -> when (this) { // "Abu Bakr al-Shatri" default
                LIGHT -> "https://everyayah.com/data/Abu_Bakr_Ash-Shaatree_64kbps/$file"
                BETTER -> "https://verses.quran.com/Shatri/mp3/$file"
            }
        }
    }

    /** Overload for callers specifying only surah and ayah. Defaults to Abu Bakr al-Shatri. */
    fun urlFor(surah: Int, ayah: Int): String = urlFor("Abu Bakr al-Shatri", surah, ayah)
}

/** What the listen control is doing. Built as a set rather than discovered later. */
sealed interface AudioState {
    data object Idle : AudioState

    /** Downloading. [done] of [total] ayahs are on disk, tracking [bytesDownloaded] and [totalBytes]. */
    data class Fetching(
        val done: Int,
        val total: Int,
        val bytesDownloaded: Long = 0L,
        val totalBytes: Long = 0L,
    ) : AudioState

    /** [index] is 0-based, so the screen can say "3 of 13". */
    data class Playing(val verseKey: String, val index: Int, val total: Int) : AudioState

    /**
     * Held, not finished. **His ask, 2026-08-19**, from the player in the app he reads in.
     *
     * A separate state rather than a boolean on [Playing] because the bar has to draw a
     * different button, and a screen that has to ask "playing, but is it really?" is how a
     * play button ends up showing a pause icon while nothing is playing.
     */
    data class Paused(val verseKey: String, val index: Int, val total: Int) : AudioState

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

    /**
     * Which listen is the current one.
     *
     * A plain `cancelled` boolean was the first version of this and it was wrong in a way
     * no unit test would have caught: [stop] set it true, but only [play] set it back —
     * and [play] runs *after* [ensureCached]. So the second listen of any session died on
     * the first line of the fetch and reported "check your connection", with every file
     * already sitting on disk. **Listening worked exactly once per launch.** Found on the
     * phone, 2026-08-16, during the airplane-mode test that was supposed to be a formality.
     *
     * A counter fixes the ordering and one more thing besides: two quick taps now
     * supersede each other cleanly instead of both writing to the same player.
     */
    private var run = 0

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
    fun isCached(verses: List<String>, quality: AudioQuality, reciter: String = "Abu Bakr al-Shatri"): Boolean {
        val dir = dirFor(reciter, quality)
        return verses.all { key ->
            val surah = key.substringBefore(':').toIntOrNull() ?: return@all false
            val ayah = key.substringAfter(':').toIntOrNull() ?: return@all false
            val f = File(dir, "%03d%03d.mp3".format(surah, ayah))
            f.exists() && f.length() >= MIN_PLAUSIBLE_BYTES
        }
    }

    fun uncachedBytesEstimate(verses: List<String>, quality: AudioQuality, reciter: String = "Abu Bakr al-Shatri"): Long {
        val dir = dirFor(reciter, quality)
        val count = verses.count { key ->
            val surah = key.substringBefore(':').toIntOrNull() ?: return@count false
            val ayah = key.substringAfter(':').toIntOrNull() ?: return@count false
            val f = File(dir, "%03d%03d.mp3".format(surah, ayah))
            !f.exists() || f.length() < MIN_PLAUSIBLE_BYTES
        }
        val perAyah = if (quality == AudioQuality.LIGHT) 85_000L else 135_000L
        return (count * perAyah).coerceAtLeast(0L)
    }

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
        reciter: String = "Abu Bakr al-Shatri",
        onProgress: (done: Int, total: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit = { _, _, _, _ -> },
    ): List<File>? = withContext(Dispatchers.IO) {
        val mine = ++run
        val dir = dirFor(reciter, quality)
        val files = mutableListOf<File>()

        var existingBytes = 0L
        var uncachedCount = 0
        verses.forEach { key ->
            val surah = key.substringBefore(':').toIntOrNull()
            val ayah = key.substringAfter(':').toIntOrNull()
            if (surah != null && ayah != null) {
                val f = File(dir, "%03d%03d.mp3".format(surah, ayah))
                if (f.exists() && f.length() >= MIN_PLAUSIBLE_BYTES) {
                    existingBytes += f.length()
                } else {
                    uncachedCount++
                }
            }
        }

        val perAyah = if (quality == AudioQuality.LIGHT) 85_000L else 135_000L
        var totalBytesEstimate = existingBytes + (uncachedCount * perAyah).coerceAtLeast(100_000L)
        var bytesDownloaded = existingBytes

        onProgress(0, verses.size, bytesDownloaded, totalBytesEstimate)

        verses.forEachIndexed { i, key ->
            // Superseded by a later listen, or stopped. Not a failure — just not ours.
            if (mine != run) return@withContext null
            val surah = key.substringBefore(':').toIntOrNull() ?: return@withContext null
            val ayah = key.substringAfter(':').toIntOrNull() ?: return@withContext null

            val f = File(dir, "%03d%03d.mp3".format(surah, ayah))
            if (!f.exists() || f.length() < MIN_PLAUSIBLE_BYTES) {
                if (f.exists()) f.delete()
                val bytes = getBytes(quality.urlFor(reciter, surah, ayah)) { chunk ->
                    bytesDownloaded += chunk
                    if (bytesDownloaded > totalBytesEstimate) {
                        totalBytesEstimate = bytesDownloaded + 50_000L
                    }
                    onProgress(i, verses.size, bytesDownloaded, totalBytesEstimate)
                } ?: return@withContext null

                // A 678-byte HTML error page arrives with a 200 from these CDNs — the same
                // trap the fonts had. Trust the byte count, never the status line.
                if (bytes.size < MIN_PLAUSIBLE_BYTES) {
                    Log.w(TAG, "$key was only ${bytes.size} bytes — not audio")
                    return@withContext null
                }
                f.writeBytes(bytes)
            }
            files += f
            onProgress(i + 1, verses.size, bytesDownloaded, totalBytesEstimate)
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
        /**
         * Where to begin. **Added 2026-08-19**: tapping Play on an ayah used to start the
         * portion from the top, which is the opposite of what tapping *that* ayah means.
         */
        startIndex: Int = 0,
    ) {
        // Release whatever was playing without bumping [run] — the fetch that got us here
        // holds the current token, and cancelling it now would stop the thing we are
        // starting.
        releasePlayer()
        this.files = files
        this.verses = verses
        this.onVerse = onVerse
        this.onFinished = onFinished
        played = 0
        playFrom(run, startIndex.coerceIn(0, (files.size - 1).coerceAtLeast(0)), files, verses, onVerse, onFinished)
    }

    /**
     * Move to an ayah that is already loaded.
     *
     * Public so the verse toolbar can hand playback to a different ayah **without refetching
     * the portion** — everything is already on disk, so jumping should be instant.
     */
    fun goTo(index: Int) = jumpTo(index)

    /**
     * How many times each ayah is heard before the next one. **1, 2, 3, or [FOREVER].**
     *
     * ⚠ **Per ayah, not per portion**, and that is the choice worth writing down. Repeating a
     * whole portion three times is listening; repeating *one ayah* three times is how memorisation
     * is actually done, and § 5r already gives this app a memorising mode. Mutalib pointed at a
     * player showing "1 2 3 and infinity" and this is the reading of it that earns its place.
     *
     * Changing it mid-ayah takes effect on the ayah you are hearing, not the next one, because
     * the reason anyone reaches for this control is that the ayah playing right now is the one
     * they have not got yet.
     */
    var repeatEach: Int = 1
        set(value) {
            field = value
            played = 0
        }

    /** How many times the current ayah has finished. */
    private var played = 0

    private var files: List<File> = emptyList()
    private var verses: List<String> = emptyList()
    private var onVerse: ((Int) -> Unit)? = null
    private var onFinished: (() -> Unit)? = null
    private var at: Int = 0

    /** Hold it where it is. Nothing is released, so [resume] picks up mid-ayah. */
    fun pause() {
        runCatching { player?.pause() }
    }

    fun resume() {
        runCatching { player?.start() }
    }

    /**
     * The next ayah, the previous one, or this one from the top.
     *
     * **[previous] restarts the current ayah first**, the way every music player does: pressing
     * back once means "I missed that", and only pressing it again means "the one before".
     * Judged by how far in we are rather than by counting presses, so it needs no timer.
     */
    fun next() = jumpTo(at + 1)

    fun previous() {
        val restarting = runCatching { (player?.currentPosition ?: 0) > RESTART_MS }.getOrDefault(false)
        jumpTo(if (restarting) at else at - 1)
    }

    fun replay() = jumpTo(at)

    private fun jumpTo(index: Int) {
        if (files.isEmpty()) return
        val target = index.coerceIn(0, files.size - 1)
        played = 0
        releasePlayer()
        playFrom(run, target, files, verses, onVerse ?: return, onFinished ?: return)
    }

    private fun playFrom(
        mine: Int,
        index: Int,
        files: List<File>,
        verses: List<String>,
        onVerse: (Int) -> Unit,
        onFinished: () -> Unit,
    ) {
        if (mine != run || index >= files.size) {
            releasePlayer()
            onFinished()
            return
        }
        at = index
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
                    // The repeat count is read here rather than captured, so changing it
                    // while an ayah is playing applies to that ayah.
                    played++
                    val again = repeatEach == FOREVER || played < repeatEach
                    if (!again) played = 0
                    playFrom(mine, if (again) index else index + 1, files, verses, onVerse, onFinished)
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

    /** Stop, and make any fetch or chain still in flight stand down. */
    fun stop() {
        run++
        releasePlayer()
        played = 0
        at = 0
    }

    private fun releasePlayer() {
        runCatching { player?.release() }
        player = null
    }

    fun release() = stop()

    // ---- disk ----

    private fun dirFor(reciter: String, quality: AudioQuality): File {
        val safe = reciter.lowercase().replace("[^a-z0-9]".toRegex(), "_")
        return File(File(File(context.filesDir, "recitation-audio"), safe), quality.name).apply { mkdirs() }
    }

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

    private fun getBytes(url: String, onChunk: ((Int) -> Unit)? = null): ByteArray? {
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
            val buffer = ByteArray(8192)
            val out = ByteArrayOutputStream()
            conn.inputStream.use { input ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    onChunk?.invoke(read)
                }
            }
            out.toByteArray()
        } catch (e: Exception) {
            Log.w(TAG, "audio fetch failed: $url", e)
            null
        } finally {
            conn?.disconnect()
        }
    }

    // One companion object, because a class only gets one — the second one this gained by
    // accident took the whole file down with unresolved references to its own constants.
    companion object {
        /** Repeat this ayah until told otherwise. The infinity on his player. */
        const val FOREVER = 0

        /** Past this far in, "back" means restart rather than go back one. */
        private const val RESTART_MS = 2_000

        private const val TAG = "WirdRecital"

        /** The error pages these CDNs return with a 200 are around 678 bytes. */
        private const val MIN_PLAUSIBLE_BYTES = 4_000L

        /** ~20 pages at the heavier bitrate. Enough to revisit, bounded enough to forget. */
        private const val MAX_CACHE_BYTES = 50L * 1024 * 1024
    }
}
