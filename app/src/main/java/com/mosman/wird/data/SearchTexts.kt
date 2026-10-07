package com.mosman.wird.data

import android.content.Context
import android.util.Log
import com.mosman.wird.domain.QuranSearch
import com.mosman.wird.domain.QuranSearch.Field
import com.mosman.wird.domain.VerseIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * What word search reads: the folded index, and the verses it shows.
 *
 * The index is four bundled files in `assets/search/`, one per language: every verse's Arabic,
 * English, Hausa and transliteration, folded for matching when the app was built
 * (QuranSearchTest fails if they fall behind the texts). The words shown under a result come
 * from the same page files the reading page uses, read only for the results on screen, so a
 * result always reads the same as the page it opens. Nothing is fetched: search works with the
 * radio off.
 *
 * The index is read when the search box opens, not at launch, so readers who never search pay
 * nothing for it.
 *
 * ⚠ Measure the first search after any change here (PLAN task 26 asks for under a second on the
 * emulator); the log lines tagged WirdSearch give the numbers. The history is on
 * [QuranSearch.Index].
 */
object SearchTexts {

    /** Outlives the screen that asked, so a read half done when the reader backs out is kept. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var building: Deferred<QuranSearch.Index>? = null

    /** Starts reading the index if nothing has yet. Cheap to call again. */
    fun prepare(context: Context) {
        start(context)
    }

    suspend fun index(context: Context): QuranSearch.Index = start(context).await()

    /**
     * Every verse holding [query], best first. The log line says how long the reader waited and
     * how much of that was the index still being read.
     */
    suspend fun search(context: Context, query: String): List<QuranSearch.Match> {
        val asked = System.nanoTime()
        val index = index(context)
        val ready = System.nanoTime()
        val found = withContext(Dispatchers.Default) { QuranSearch.search(index, query) }
        Log.i(TAG, "search: ${found.size} verses in ${(System.nanoTime() - asked) / 1_000_000} ms, ${(ready - asked) / 1_000_000} ms of it waiting for the index")
        return found
    }

    /**
     * [matches] with their words, read from the page files they sit on. Each page file is read
     * once, and several at a time.
     */
    suspend fun hits(context: Context, matches: List<QuranSearch.Match>): List<QuranSearch.Hit> = withContext(Dispatchers.Default) {
        val started = System.nanoTime()
        val app = context.applicationContext
        val paths = matches.map { pathOf(it.field, it.key) }
        val pages = paths.distinct().map { path -> async(Dispatchers.IO) { path to pageVerses(app, path) } }.awaitAll().toMap()
        val hits = matches.mapIndexed { i, m ->
            val text = pages.getValue(paths[i])["${m.key.first}:${m.key.second}"].orEmpty()
            QuranSearch.hit(m, if (m.field == Field.HAUSA) decodeEntities(text) else text)
        }
        Log.i(TAG, "words for ${hits.size} results from ${pages.size} pages in ${(System.nanoTime() - started) / 1_000_000} ms")
        hits
    }

    /** A failed read counts as cancelled, so the next search tries again. */
    @Synchronized
    private fun start(context: Context): Deferred<QuranSearch.Index> =
        building?.takeUnless { it.isCancelled }
            ?: scope.async { build(context.applicationContext) }.also { building = it }

    private suspend fun build(context: Context): QuranSearch.Index = coroutineScope {
        val started = System.nanoTime()
        val texts = Field.entries.map { field ->
            async {
                // A checkout that turned \n into \r\n would leave a \r on every verse's last word.
                field to context.assets.open(QuranSearch.Index.fileOf(field))
                    .use { String(it.readBytes(), Charsets.UTF_8) }.replace("\r\n", "\n")
            }
        }.awaitAll().toMap()
        val index = QuranSearch.Index(texts)
        // Logged once per app run: on a tester's phone the log is the only place this shows up.
        Log.i(TAG, "search index: ${texts.values.sumOf { it.length }} characters in ${(System.nanoTime() - started) / 1_000_000} ms")
        index
    }

    private const val TAG = "WirdSearch"

    /** The page file holding [key]'s text in [field], the one the reading page shows. */
    private fun pathOf(field: Field, key: Pair<Int, Int>): String {
        val page = VerseIndex.pageOf(key)
        return when (field) {
            Field.ARABIC -> "arabic/$page.json"
            Field.ENGLISH -> "translations/${TranslationSource.SAHEEH.id}/$page.json"
            Field.HAUSA -> "translations/${TranslationSource.HAUSA.id}/$page.json"
            Field.TRANSLITERATION -> "translations/${TranslationSource.TRANSLITERATION.id}/$page.json"
        }
    }

    /** Page files read for results lately, each as verse key to text; "Show more" reuses them. */
    private val recent = object : LinkedHashMap<String, Map<String, String>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Map<String, String>>?) = size > 64
    }

    private fun pageVerses(context: Context, path: String): Map<String, String> =
        synchronized(recent) { recent[path] }
            ?: versesIn(context.assets.open(path).use { String(it.readBytes(), Charsets.UTF_8) }).toMap()
                .also { synchronized(recent) { recent[path] = it } }
}

/**
 * The verses in one bundled text file, as (verse key, text) pairs.
 *
 * Every file has one shape, `[{"v":"2:255","t":"…"},…]`, so this reads it directly rather than
 * building an org.json map for every verse. QuranSearchTest checks it against org.json on all
 * 2,416 files, so a file in another shape fails the build, not the app.
 */
internal fun versesIn(json: String): List<Pair<String, String>> {
    val out = ArrayList<Pair<String, String>>(16)
    var at = 0
    while (true) {
        val v = json.indexOf(KEY, at)
        if (v < 0) return out
        val keyStart = v + KEY.length
        val keyEnd = json.indexOf('"', keyStart)
        val t = json.indexOf(TEXT, keyEnd)
        require(t >= 0) { "verse ${json.substring(keyStart, keyEnd)} has no text" }
        val textStart = t + TEXT.length
        val textEnd = stringEnd(json, textStart)
        out += json.substring(keyStart, keyEnd) to unescape(json, textStart, textEnd)
        at = textEnd + 1
    }
}

private const val KEY = "\"v\":\""
private const val TEXT = "\"t\":\""

/** The index of the quote that closes the string starting at [from]. */
private fun stringEnd(json: String, from: Int): Int {
    var quote = json.indexOf('"', from)
    while (true) {
        require(quote >= 0) { "a text from index $from never closes" }
        // Escaped when an odd number of backslashes runs up to it.
        var slashes = 0
        while (json[quote - 1 - slashes] == '\\') slashes++
        if (slashes % 2 == 0) return quote
        quote = json.indexOf('"', quote + 1)
    }
}

/** JSON's backslash escapes; the English file has 3,434 of them, all `\"`. */
private fun unescape(json: String, from: Int, to: Int): String {
    val slash = json.indexOf('\\', from)
    if (slash < 0 || slash >= to) return json.substring(from, to)
    val out = StringBuilder(to - from)
    var i = from
    while (i < to) {
        val c = json[i]
        if (c != '\\') {
            out.append(c)
            i++
            continue
        }
        when (val e = json[i + 1]) {
            'u' -> {
                out.append(json.substring(i + 2, i + 6).toInt(16).toChar())
                i += 6
                continue
            }
            'n' -> out.append('\n')
            't' -> out.append('\t')
            'r' -> out.append('\r')
            'b' -> out.append('\b')
            'f' -> out.append('\u000C')
            else -> out.append(e)
        }
        i += 2
    }
    return out.toString()
}
