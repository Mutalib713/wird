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
                .take(MAX_PARAS_PER_SECTION)
                .map { trimParagraph(it) }

            if (paras.isEmpty()) return ""
            return "§ Section 1: Commentary\n\n" + paras.joinToString("\n\n")
        }

        data class RawSection(val title: String, val paras: List<String>, val isMajor: Boolean)
        val sections = mutableListOf<RawSection>()
        val pendingTitles = mutableListOf<String>()

        // Check if there was text before the first heading
        if (headings.first().range.first > 0) {
            val preText = html.substring(0, headings.first().range.first)
            val cleanPre = stripAndUnescape(preText)
            val preParas = cleanPre.split(Regex("\n{2,}"))
                .map { it.replace(Regex("[ \\t]+"), " ").trim() }
                .filter { it.isNotBlank() }
            if (preParas.isNotEmpty()) {
                sections.add(RawSection("Overview", preParas, isMajor = true))
            }
        }

        for (i in headings.indices) {
            val hMatch = headings[i]
            val level = hMatch.groupValues[1].toIntOrNull() ?: 2
            val rawTitle = hMatch.groupValues[2]
            val cleanTitle = unescapeHtml(rawTitle.replace(HTML_TAG_REGEX, " ")).trim()
            val isMajor = level <= 2

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
                sections.add(RawSection(fullTitle, paras, isMajor = isMajor))
            }
        }

        // Reduce content: cap sections and paragraphs per section, trim long paragraphs.
        // User feedback: tafsir was too lengthy — keep what's essential.
        val trimmed = sections.take(MAX_SECTIONS).map { raw ->
            raw.copy(paras = raw.paras.take(MAX_PARAS_PER_SECTION).map { trimParagraph(it) })
        }

        val result = StringBuilder()
        var majorCount = 0
        trimmed.forEachIndexed { index, raw ->
            if (index > 0) result.append("\n\n")
            if (raw.isMajor || majorCount == 0) {
                majorCount++
                result.append("§ Section $majorCount: ${raw.title}\n\n")
            } else {
                // Sub-point under the same topic: use * with space before per user directive
                result.append("§ * ${raw.title}\n\n")
            }
            result.append(raw.paras.joinToString("\n\n"))
        }

        return result.toString().trim()
    }

    /** Trim a paragraph that is too long, cutting at the last sentence boundary. */
    private fun trimParagraph(para: String): String {
        if (para.length <= MAX_PARA_LENGTH) return para
        // Cut at the last sentence-ending punctuation before the limit.
        val cut = para.substring(0, MAX_PARA_LENGTH)
        val lastSentence = cut.lastIndexOfAny(charArrayOf('.', '!', '?'))
        return if (lastSentence > MAX_PARA_LENGTH / 2) {
            cut.substring(0, lastSentence + 1)
        } else {
            cut.trimEnd() + "…"
        }
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
        private const val MAX_SECTIONS = 5
        private const val MAX_PARAS_PER_SECTION = 4
        private const val MAX_PARA_LENGTH = 400
        private val HEADER_REGEX = Regex("<h([1-6])[^>]*>(.*?)</h[1-6]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val ARABIC_DIV_REGEX = Regex("<div[^>]*class=[\"'][^\"']*arabic[^\"']*[\"'][^>]*>(.*?)</div>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val HTML_TAG_REGEX = Regex("<[^>]+>")
    }
}
