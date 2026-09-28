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
    private val cacheDir = File(context.cacheDir, "tafsir_cache_v2").apply { mkdirs() }

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
        var text = html

        // 1. Format headings into distinct section banners
        text = HEADER_REGEX.replace(text) { match ->
            val title = match.groupValues[1].replace(HTML_TAG_REGEX, " ").trim()
            if (title.isNotBlank()) "\n\n§ $title\n\n" else "\n\n"
        }

        // 2. Format Arabic commentary anchors
        text = ARABIC_DIV_REGEX.replace(text) { match ->
            val arabic = match.groupValues[1].replace(HTML_TAG_REGEX, " ").trim()
            if (arabic.isNotBlank()) "\n\n« $arabic »\n\n" else ""
        }

        // 3. Convert paragraphs, linebreaks and list items
        text = text
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</li>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "• ")
            .replace(Regex("</blockquote>", RegexOption.IGNORE_CASE), "\n\n")

        // 4. Strip remaining HTML tags
        text = text.replace(HTML_TAG_REGEX, " ")

        // 5. Unescape HTML entities
        text = text
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

        // 6. Split into clean paragraphs
        val rawParagraphs = text.split(Regex("\n{2,}"))
            .map { it.replace(Regex("[ \\t]+"), " ").trim() }
            .filter { it.isNotBlank() }

        // 7. Structure into numbered sections for pleasant mobile reading
        val formattedSections = mutableListOf<String>()
        var pointNum = 1
        for (para in rawParagraphs) {
            when {
                para.startsWith("§ ") -> {
                    formattedSections.add(para)
                }
                para.startsWith("«") && para.endsWith("»") -> {
                    formattedSections.add(para)
                }
                para.startsWith("• ") -> {
                    formattedSections.add(para)
                }
                para.length > 30 && rawParagraphs.size > 1 -> {
                    formattedSections.add("${pointNum++}. $para")
                }
                else -> {
                    formattedSections.add(para)
                }
            }
        }

        return formattedSections.joinToString("\n\n").trim()
    }

    companion object {
        private const val TAG = "WirdTafsir"
        private val HEADER_REGEX = Regex("<h[1-6][^>]*>(.*?)</h[1-6]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val ARABIC_DIV_REGEX = Regex("<div[^>]*class=[\"'][^\"']*arabic[^\"']*[\"'][^>]*>(.*?)</div>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val HTML_TAG_REGEX = Regex("<[^>]+>")
    }
}
