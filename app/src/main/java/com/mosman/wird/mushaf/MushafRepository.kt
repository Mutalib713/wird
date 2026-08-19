package com.mosman.wird.mushaf

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** What the Today screen can be showing. Built as a set, not discovered later. */
sealed interface PageState {
    data object Loading : PageState
    data class Ready(
        val page: MushafPage,
        val typeface: Typeface,
        /** Null when this page opens no surah, or when the bismillah font didn't load. */
        val bismillahTypeface: Typeface?,
    ) : PageState
    /** [retryable] false means something is wrong with the page itself, not the network. */
    data class Failed(val reason: String, val retryable: Boolean) : PageState
}

/**
 * Fetches a mushaf page's layout and its matching glyph font, and caches both on disk.
 *
 * Sacred Rule 2 lives here: if the font will not load, this returns [PageState.Failed]
 * and the screen says so. It never renders the glyph codes with a system Arabic face —
 * those codepoints sit in Arabic Presentation Forms-A, so a fallback font would happily
 * draw *real but wrong* Arabic. Silently showing the wrong Qur'anic text is far worse
 * than showing nothing.
 */
class MushafRepository(private val context: Context) {

    private val fontDir = File(context.filesDir, "qcf").apply { mkdirs() }
    private val pageDir = File(context.filesDir, "pages").apply { mkdirs() }

    /**
     * True when this page can be drawn without touching the network.
     *
     * Lets the pager skip its settle delay for pages already on disk: a page you have
     * seen before should appear the instant you swipe to it, and waiting to find out
     * whether to download something you already have is waiting for nothing.
     */
    fun isCached(page: Int): Boolean {
        val layout = File(pageDir, "p$page-${Mushaf.FONT_VERSION.name}.json")
        val font = File(fontDir, "${Mushaf.FONT_VERSION.name}-$page.ttf")
        return layout.exists() && font.exists() && font.length() >= Mushaf.MIN_PLAUSIBLE_FONT_BYTES
    }

    suspend fun load(page: Int): PageState = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        fun elapsed() = System.currentTimeMillis() - started
        try {
            val layout = layoutFor(page) ?: return@withContext PageState.Failed(
                "Couldn't get the layout for page $page.", retryable = true,
            )
            val font = fontFor(page) ?: return@withContext PageState.Failed(
                "Couldn't get the font for page $page.", retryable = true,
            )
            val tf = runCatching { Typeface.createFromFile(font) }.getOrNull()
                ?: run {
                    // A corrupt cache should not be permanent.
                    font.delete()
                    return@withContext PageState.Failed(
                        "The font for page $page didn't load. Cleared it — try again.",
                        retryable = true,
                    )
                }
            // Only fetched for pages that open a surah, and its absence degrades the
            // header rather than failing the page.
            val afterFont = elapsed()
            val bismillahTf = if (layout.bismillahCodes != null) bismillahTypeface() else null
            Log.i(TAG, "load($page) cached=${isCached(page)} font+layout=${afterFont}ms total=${elapsed()}ms")
            PageState.Ready(layout, tf, bismillahTf)
        } catch (e: Exception) {
            Log.w(TAG, "load($page) failed", e)
            PageState.Failed("No connection.", retryable = true)
        }
    }

    // ---- page layout -------------------------------------------------------

    private fun layoutFor(page: Int): MushafPage? {
        val cache = File(pageDir, "p$page-${Mushaf.FONT_VERSION.name}.json")
        val body = if (cache.exists()) {
            cache.readText()
        } else {
            val field = Mushaf.FONT_VERSION.apiField
            val url = "https://api.quran.com/api/v4/verses/by_page/$page" +
                "?words=true&per_page=50&fields=juz_number" +
                "&word_fields=$field,line_number,char_type_name"
            val text = get(url) ?: return null
            cache.writeText(text)
            text
        }
        return runCatching { parse(page, body) }.getOrElse {
            Log.w(TAG, "parse failed for page $page", it)
            cache.delete()
            null
        }
    }

    private fun parse(page: Int, body: String): MushafPage {
        val field = Mushaf.FONT_VERSION.apiField
        val verses = JSONObject(body).getJSONArray("verses")
        val glyphs = mutableListOf<Glyph>()
        val surahStarts = mutableMapOf<String, String>()
        var juz = 0
        var openingChapter = 0

        for (i in 0 until verses.length()) {
            val v = verses.getJSONObject(i)
            val key = v.getString("verse_key")
            if (i == 0) {
                juz = v.optInt("juz_number", 0)
            }
            if (key.substringAfter(':') == "1") {
                surahStarts[key] = key.substringBefore(':')
                if (openingChapter == 0) openingChapter = key.substringBefore(':').toInt()
            }
            val words = v.getJSONArray("words")
            for (j in 0 until words.length()) {
                val w = words.getJSONObject(j)
                val code = w.optString(field, "")
                if (code.isEmpty()) continue
                glyphs += Glyph(
                    code = code,
                    line = w.optInt("line_number", 0),
                    verseKey = key,
                    isEndMarker = w.optString("char_type_name") == "end",
                )
            }
        }
        require(glyphs.isNotEmpty()) { "no glyphs on page $page" }

        val chapterId = glyphs.first().verseKey.substringBefore(':').toInt()
        val chapter = chapter(chapterId)
        val opensASurah = openingChapter != 0
        val bismillah = if (opensASurah && chapter(openingChapter).second) {
            bismillahCodes()
        } else {
            null
        }

        return MushafPage(
            page = page,
            glyphs = glyphs,
            surahStarts = surahStarts,
            surahName = chapter.first,
            juz = juz,
            bismillahCodes = bismillah,
        )
    }

    /** @return name, and whether a bismillah precedes it. At-Tawbah is the one without. */
    private fun chapter(id: Int): Pair<String, Boolean> {
        val cache = File(pageDir, "chapter-$id.json")
        val body = if (cache.exists()) cache.readText() else {
            val t = get("https://api.quran.com/api/v4/chapters/$id") ?: return "" to false
            cache.writeText(t); t
        }
        return runCatching {
            val c = JSONObject(body).getJSONObject("chapter")
            c.getString("name_simple") to c.optBoolean("bismillah_pre", false)
        }.getOrElse { "" to false }
    }

    /** See [Mushaf.BISMILLAH_CODES] — three glyphs belonging to the bismillah font. */
    private fun bismillahCodes(): String = Mushaf.BISMILLAH_CODES

    /**
     * Which page a given ayah sits on.
     *
     * People know they are "in Sad, around ayah 25". Nobody knows they are on page 453,
     * so setup asks the question they can answer and this turns it into the one the app
     * needs. Cached, because an ayah does not move.
     */
    suspend fun pageOfVerse(surah: Int, ayah: Int): Int? = withContext(Dispatchers.IO) {
        val cache = File(pageDir, "verse-$surah-$ayah.json")
        val body = if (cache.exists()) cache.readText() else {
            val t = get(
                "https://api.quran.com/api/v4/verses/by_key/$surah:$ayah?fields=page_number",
            ) ?: return@withContext null
            cache.writeText(t); t
        }
        runCatching {
            JSONObject(body).getJSONObject("verse").getInt("page_number")
        }.getOrNull()
    }

    /**
     * Quietly fetch the pages either side of where the reader is, so a glance forwards or
     * back is instant instead of a skeleton.
     *
     * Deliberately small: **one page each way, three in total.** Measured on a wiped
     * cache — 439, 440 and 441 came to 471 KB, of which roughly **327 KB is the two extra
     * pages**, the rest being today's, which you were fetching regardless. It started at
     * two each way and Mutalib cut it: five pages is more than anyone glances at, and
     * every extra page is someone's data. It is not a background download of the
     * mushaf either: 604 pages would be 91 MB, which is not a thing to do to someone on
     * mobile data without asking.
     *
     * Anything already cached costs nothing, so this is a no-op from the second day on
     * unless the portion has moved.
     */
    suspend fun prefetchAround(page: Int, radius: Int = 1) = withContext(Dispatchers.IO) {
        // Fully qualified on purpose: there are two objects called Mushaf — this package's
        // one holds font settings, and the domain one holds the book's shape. Inside this
        // file the wrong one wins, which is exactly the kind of collision worth naming
        // rather than working around silently.
        val pageCount = com.mosman.wird.domain.Mushaf.PAGES
        val wanted = ((page - radius)..(page + radius))
            .map { ((it - 1).mod(pageCount)) + 1 }
            .filter { !isCached(it) }
        if (wanted.isEmpty()) return@withContext
        Log.i(TAG, "prefetching ${wanted.size} pages around $page")
        wanted.forEach { p ->
            // One at a time, and failures are ignored: this is a convenience, and a page
            // that does not arrive now will simply be fetched when the reader turns to it.
            runCatching { load(p) }
        }
    }

    /** How many of the 604 pages are already on this phone, and what they weigh. */
    fun cached(): Pair<Int, Long> {
        val pages = (1..com.mosman.wird.domain.Mushaf.PAGES).count { isCached(it) }
        // orEmpty() is for collections, not arrays - listFiles() returns Array<File>? and
        // needs its own fallback.
        val files = (fontDir.listFiles() ?: emptyArray()) + (pageDir.listFiles() ?: emptyArray())
        val bytes = files.sumOf { it.length() }
        return pages to bytes
    }

    /**
     * Fetch the whole mushaf, page by page. **His instruction, 2026-08-19**, after pointing at
     * how Quran for Android works: *"when you first open the app it downloads the pages for you."*
     *
     * **Why the app should do this rather than a cable.** Pages already arrive one at a time as
     * they are read, which is the right default on Ghanaian data — but it means someone about to
     * lose signal cannot prepare, and there was no way to say "get it all now". This is that.
     *
     * ⚠ **One page at a time, on purpose.** 604 parallel requests would be faster on wifi and
     * would also be the fastest possible way to be rate-limited by a free API that owes us
     * nothing, and to bury a phone on a slow connection. Failures are counted rather than
     * thrown: a page that does not arrive is simply still missing, and the reader is told how
     * many, which is the honest report and also a resumable one — running it again fetches only
     * what is absent.
     *
     * @param onProgress done and total, for a progress bar that reflects real work.
     * @return how many pages failed. Zero means the whole mushaf is on the phone.
     */
    suspend fun downloadAll(
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Int = withContext(Dispatchers.IO) {
        val total = com.mosman.wird.domain.Mushaf.PAGES
        var failed = 0
        (1..total).forEach { p ->
            if (!currentCoroutineContext().isActive) return@withContext failed
            if (!isCached(p)) {
                val ok = runCatching { load(p) }.getOrNull()
                if (ok !is PageState.Ready) failed++
            }
            onProgress(p, total)
        }
        Log.i(TAG, "whole mushaf requested: ${total - failed}/$total pages present")
        failed
    }

    /**
     * A page's layout without fetching its font.
     *
     * The recitation audio needs to know which ayahs sit on which lines, and nothing more
     * — it never draws anything. Going through [load] would pull a 154 KB glyph font for a
     * page that may not be on screen, which on a two-page portion is a third of a megabyte
     * spent to answer a question the cached JSON already holds.
     *
     * Returns null rather than throwing: no layout simply means no audio for that page,
     * and the caller says so.
     */
    suspend fun layoutOnly(page: Int): MushafPage? = withContext(Dispatchers.IO) {
        layoutFor(page)
    }

    /** How many ayahs a surah has, so setup can stop someone typing 400 into Al-Kawthar. */
    suspend fun ayahCount(surah: Int): Int? = withContext(Dispatchers.IO) {
        val cache = File(pageDir, "chapter-$surah.json")
        val body = if (cache.exists()) cache.readText() else {
            val t = get("https://api.quran.com/api/v4/chapters/$surah") ?: return@withContext null
            cache.writeText(t); t
        }
        runCatching {
            JSONObject(body).getJSONObject("chapter").getInt("verses_count")
        }.getOrNull()
    }

    private fun bismillahTypeface(): Typeface? {
        val f = File(fontDir, "bismillah.ttf")
        if (!f.exists() || f.length() < Mushaf.MIN_PLAUSIBLE_FONT_BYTES) {
            val bytes = getBytes(Mushaf.BISMILLAH_FONT_URL) ?: return null
            if (bytes.size < Mushaf.MIN_PLAUSIBLE_FONT_BYTES) return null
            f.writeBytes(bytes)
            Log.i(TAG, "cached bismillah font: ${bytes.size} bytes")
        }
        return runCatching { Typeface.createFromFile(f) }.getOrNull()
    }

    // ---- font --------------------------------------------------------------

    private fun fontFor(page: Int): File? {
        val f = File(fontDir, "${Mushaf.FONT_VERSION.name}-$page.ttf")
        if (f.exists() && f.length() >= Mushaf.MIN_PLAUSIBLE_FONT_BYTES) return f
        if (f.exists()) f.delete()

        val bytes = getBytes(Mushaf.FONT_VERSION.fontUrl(page)) ?: return null
        // The 14-byte "404: Not Found" body arrives with a 200 on some mirrors. Trust the
        // byte count, never the status line.
        if (bytes.size < Mushaf.MIN_PLAUSIBLE_FONT_BYTES) {
            Log.w(TAG, "font for page $page was only ${bytes.size} bytes — not a font")
            return null
        }
        f.writeBytes(bytes)
        Log.i(TAG, "cached font page $page: ${bytes.size} bytes")
        return f
    }

    // ---- plumbing ----------------------------------------------------------

    private fun get(url: String): String? = open(url) { it.readBytes().toString(Charsets.UTF_8) }
    private fun getBytes(url: String): ByteArray? = open(url) { it.readBytes() }

    private fun <T> open(url: String, read: (java.io.InputStream) -> T): T? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                // api.quran.com 403s the default Java agent.
                setRequestProperty("User-Agent", "Wird/0.1 (Android)")
            }
            if (conn.responseCode !in 200..299) {
                Log.w(TAG, "HTTP ${conn.responseCode} for $url")
                return null
            }
            conn.inputStream.use(read)
        } catch (e: Exception) {
            Log.w(TAG, "request failed: $url", e)
            null
        } finally {
            conn?.disconnect()
        }
    }

    companion object { const val TAG = "WirdMushaf" }
}
