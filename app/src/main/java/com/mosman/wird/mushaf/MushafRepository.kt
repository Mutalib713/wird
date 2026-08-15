package com.mosman.wird.mushaf

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** What the Today screen can be showing. Built as a set, not discovered later. */
sealed interface PageState {
    data object Loading : PageState
    data class Ready(val page: MushafPage, val typeface: Typeface) : PageState
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

    suspend fun load(page: Int): PageState = withContext(Dispatchers.IO) {
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
            PageState.Ready(layout, tf)
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
                "?words=true&per_page=50&word_fields=$field,line_number,char_type_name"
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

        for (i in 0 until verses.length()) {
            val v = verses.getJSONObject(i)
            val key = v.getString("verse_key")
            if (key.substringAfter(':') == "1") {
                surahStarts[key] = key.substringBefore(':')
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
        return MushafPage(page, glyphs, surahStarts)
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
