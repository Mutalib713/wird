package com.mosman.wird.domain

import android.content.Context
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap

/**
 * Fast lookup of verse keys and ayah ranges on mushaf pages.
 * Reads directly from bundled assets/arabic/{page}.json (< 2 KB per page).
 */
object PageVerses {

    private val cache = ConcurrentHashMap<Int, List<String>>()

    /**
     * Verse keys on [page] in reading order (e.g. "36:1", "36:2").
     */
    fun keysOn(context: Context, page: Int): List<String> {
        cache[page]?.let { return it }
        val keys = runCatching {
            context.assets.open("arabic/$page.json").use { stream ->
                val text = stream.bufferedReader().readText()
                val array = JSONArray(text)
                buildList(array.length()) {
                    for (i in 0 until array.length()) {
                        add(array.getJSONObject(i).getString("v"))
                    }
                }
            }
        }.getOrDefault(emptyList())

        if (keys.isNotEmpty()) {
            cache[page] = keys
        }
        return keys
    }

    /**
     * Ayah range for [surahNumber] across [pages].
     * Example outputs: "Ayahs 1–12", "Ayah 45".
     */
    fun ayahRange(context: Context, pages: List<Int>, surahNumber: Int): String? {
        val allKeys = pages.flatMap { keysOn(context, it) }
        val prefix = "$surahNumber:"
        val surahAyahs = allKeys
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.substringAfter(':').toIntOrNull() }

        if (surahAyahs.isEmpty()) return null
        val min = surahAyahs.minOrNull() ?: return null
        val max = surahAyahs.maxOrNull() ?: return null

        return if (min == max) "Ayah $min" else "Ayahs $min–$max"
    }
}
