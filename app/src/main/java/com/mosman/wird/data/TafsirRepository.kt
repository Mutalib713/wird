package com.mosman.wird.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Verified offline/online repository for scholarly Tafsir (Ibn Kathir).
 *
 * Adheres strictly to Sacred Rule 2:
 * - Tafsir is fetched and attributed from published scholarly sources (Ibn Kathir Abridged via Quran.com API).
 * - Never generated or hallucinated by a model.
 * - Cached locally on disk and memory so once retrieved, it costs 0 MB data forever.
 */
class TafsirRepository(private val context: Context) {

    private val memCache = ConcurrentHashMap<String, String>()
    private val cacheDir = File(context.cacheDir, "tafsir_cache").apply { mkdirs() }

    suspend fun getTafsir(surah: Int, ayah: Int): String? = withContext(Dispatchers.IO) {
        val key = "$surah:$ayah"
        memCache[key]?.let { return@withContext it }

        val diskFile = File(cacheDir, "${surah}_${ayah}.txt")
        if (diskFile.exists() && diskFile.length() > 0) {
            val cached = runCatching { diskFile.readText(Charsets.UTF_8) }.getOrNull()
            if (!cached.isNullOrBlank()) {
                memCache[key] = cached
                return@withContext cached
            }
        }

        // Fetch from Quran.com API (Tafsir Ibn Kathir id 169 / en-tafisr-ibn-kathir)
        val fetched = fetchRemoteTafsir(surah, ayah)
        if (!fetched.isNullOrBlank()) {
            memCache[key] = fetched
            runCatching { diskFile.writeText(fetched, Charsets.UTF_8) }
            return@withContext fetched
        }

        null
    }

    private fun fetchRemoteTafsir(surah: Int, ayah: Int): String? {
        val urlStr = "https://api.quran.com/api/v4/tafsirs/en-tafisr-ibn-kathir/by_ayah/$surah:$ayah"
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 7_000
                readTimeout = 10_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Wird/0.1 (Android)")
            }
            if (conn.responseCode !in 200..299) {
                Log.w(TAG, "HTTP ${conn.responseCode} for $urlStr")
                return null
            }
            val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(raw)
            val tafsirObj = json.optJSONObject("tafsir") ?: return null
            val rawHtml = tafsirObj.optString("text", "")
            if (rawHtml.isBlank()) return null

            cleanHtml(rawHtml)
        } catch (e: Exception) {
            Log.w(TAG, "Tafsir fetch failed for $surah:$ayah: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun cleanHtml(html: String): String {
        // Strip HTML tags
        val noTags = html.replace(HTML_TAG_REGEX, " ")
        // Unescape common HTML entities
        val unescaped = noTags
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace("&#8217;", "'")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&#8212;", "—")
        // Normalize whitespace and newlines
        return unescaped
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    companion object {
        private const val TAG = "WirdTafsir"
        private val HTML_TAG_REGEX = Regex("<[^>]+>")
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
