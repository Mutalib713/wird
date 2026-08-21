package com.mosman.wird.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * The Qur'an's words, for comparison only. **PLAN task 14.**
 *
 * ⚠ **Nothing here is ever displayed.** The page on screen is drawn from the QCF mushaf font,
 * exactly as it always has been; this text exists solely to be lined up against what a speech
 * model heard. That distinction is why the bundled spelling is the plain `imlaei` one rather
 * than the Uthmani orthography the mushaf prints — see `tools/fetch-arabic.py`.
 *
 * **Bundled, 1.36 MB across 604 files**, one per page, the same shape as [Translations]. A
 * lookup is a filename rather than an index, and nothing holds the whole Qur'an in memory on a
 * Transsion phone.
 *
 * ⚠ **Verified by its own count, not by an exit code.** The fetch asserts 6,236 verses, which is
 * the Qur'an's, and refuses to finish otherwise. Two earlier fetch runs in this project died
 * halfway and reported success.
 */
class ArabicText(private val context: Context) {

    private val cache = mutableMapOf<Int, List<Pair<String, String>>>()

    /**
     * Every word on [page], in reading order, each tagged with the ayah it belongs to.
     *
     * Empty when the page is missing, which the caller must treat as "cannot check" rather than
     * "nothing was recited" — the difference between the app failing and the reader failing.
     */
    suspend fun wordsOn(page: Int): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        cache[page]?.let { return@withContext it }

        val words = runCatching {
            context.assets.open("arabic/$page.json").use { stream ->
                val array = JSONArray(stream.bufferedReader().readText())
                buildList {
                    for (i in 0 until array.length()) {
                        val verse = array.getJSONObject(i)
                        val key = verse.getString("v")
                        verse.getString("t")
                            .split(' ')
                            .filter { it.isNotBlank() }
                            .forEach { add(key to it) }
                    }
                }
            }
        }.getOrElse {
            Log.w(TAG, "no bundled Arabic for page $page", it)
            emptyList()
        }

        cache[page] = words
        words
    }

    /** Every word across a portion's pages, in order. */
    suspend fun wordsAcross(pages: List<Int>): List<Pair<String, String>> =
        pages.flatMap { wordsOn(it) }

    private companion object { const val TAG = "WirdArabic" }
}
