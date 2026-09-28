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
    private val cacheDir = File(context.cacheDir, "tafsir_cache_v3").apply { mkdirs() }

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
        val headings = HEADER_REGEX.findAll(html).toList()

        if (headings.isEmpty()) {
            val cleanBody = stripAndUnescape(html)
            val paras = cleanBody.split(Regex("\n{2,}"))
                .map { it.replace(Regex("[ \\t]+"), " ").trim() }
                .filter { it.isNotBlank() }

            if (paras.isEmpty()) return ""
            if (paras.size <= 3) {
                return "§ Section 1: Commentary\n\n" + paras.joinToString("\n\n")
            }
            val mid = (paras.size + 1) / 2
            val sec1 = paras.subList(0, mid).joinToString("\n\n")
            val sec2 = paras.subList(mid, paras.size).joinToString("\n\n")
            return "§ Section 1: Overview & Commentary\n\n$sec1\n\n§ Section 2: Scholarly Insights\n\n$sec2"
        }

        val sections = mutableListOf<Pair<String, List<String>>>()
        val pendingTitles = mutableListOf<String>()

        // Check if there was text before the first heading
        if (headings.first().range.first > 0) {
            val preText = html.substring(0, headings.first().range.first)
            val cleanPre = stripAndUnescape(preText)
            val preParas = cleanPre.split(Regex("\n{2,}"))
                .map { it.replace(Regex("[ \\t]+"), " ").trim() }
                .filter { it.isNotBlank() }
            if (preParas.isNotEmpty()) {
                sections.add("Overview" to preParas)
            }
        }

        for (i in headings.indices) {
            val hMatch = headings[i]
            val rawTitle = hMatch.groupValues[1]
            val cleanTitle = unescapeHtml(rawTitle.replace(HTML_TAG_REGEX, " ")).trim()

            val contentStart = hMatch.range.last + 1
            val contentEnd = if (i + 1 < headings.size) headings[i + 1].range.first else html.length
            val rawContent = if (contentStart < contentEnd) html.substring(contentStart, contentEnd) else ""

            val cleanContent = stripAndUnescape(rawContent)
            val paras = cleanContent.split(Regex("\n{2,}"))
                .map { it.replace(Regex("[ \\t]+"), " ").trim() }
                .filter { it.isNotBlank() }

            if (paras.isEmpty()) {
                if (cleanTitle.isNotBlank()) {
                    pendingTitles.add(cleanTitle)
                }
            } else {
                val fullTitle = if (pendingTitles.isNotEmpty()) {
                    val combined = (pendingTitles + cleanTitle).joinToString(" · ")
                    pendingTitles.clear()
                    combined
                } else {
                    cleanTitle
                }
                sections.add(fullTitle to paras)
            }
        }

        val result = StringBuilder()
        sections.forEachIndexed { index, (title, paras) ->
            if (index > 0) result.append("\n\n")
            val sectionNum = index + 1
            result.append("§ Section $sectionNum: $title\n\n")
            result.append(paras.joinToString("\n\n"))
        }

        return result.toString().trim()
    }

    private fun stripAndUnescape(raw: String): String {
        var text = raw
        // 1. Format Arabic commentary containers
        text = ARABIC_DIV_REGEX.replace(text) { match ->
            val arabic = match.groupValues[1].replace(HTML_TAG_REGEX, " ").trim()
            if (arabic.isNotBlank()) "\n\n« $arabic »\n\n" else ""
        }
        // 2. Convert standard tags to readable spacing
        text = text
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</li>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "• ")
            .replace(Regex("</blockquote>", RegexOption.IGNORE_CASE), "\n\n")
        // 3. Strip remaining HTML tags
        text = text.replace(HTML_TAG_REGEX, " ")
        // 4. Unescape HTML entities
        return unescapeHtml(text)
    }

    private fun unescapeHtml(str: String): String {
        return str
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace("&#8217;", "'")
            .replace("&#8218;", ",")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&#8212;", "—")
            .replace("&#8211;", "–")
    }

    companion object {
        private const val TAG = "WirdTafsir"
        private val HEADER_REGEX = Regex("<h[1-6][^>]*>(.*?)</h[1-6]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val ARABIC_DIV_REGEX = Regex("<div[^>]*class=[\"'][^\"']*arabic[^\"']*[\"'][^>]*>(.*?)</div>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val HTML_TAG_REGEX = Regex("<[^>]+>")
    }
}
