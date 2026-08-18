package com.mosman.wird.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * A translation source, named on screen wherever its words appear.
 *
 * **PROFILE.md § 5z, and the naming is not decoration.** Sacred Rule 2 says Qur'anic material
 * comes from a verified source; a reader seeing English under an ayah is entitled to know
 * *whose* English it is, because translations differ and the differences are theological. So
 * the attribution travels with the text and there is no code path that renders one without
 * the other.
 *
 * The ids are Quran.com's own resource ids, kept rather than renumbered so the bundled assets
 * can be checked back against the API at any time.
 */
enum class TranslationSource(val id: Int, val label: String, val by: String) {
    SAHEEH(20, "English", "Saheeh International"),
    HAUSA(32, "Hausa", "Abubakar Mahmud Gumi"),
    TRANSLITERATION(57, "Transliteration", "Quran.com"),
}

/** One ayah's line in one source. */
data class TranslatedVerse(val verseKey: String, val text: String)

/**
 * The bundled translations, read from `assets/translations/{id}/{page}.json`.
 *
 * **Bundled rather than fetched — his call, § 5z.** It works offline from the moment the app
 * installs, which on Ghanaian mobile data is the half that matters. The cost he accepted:
 * changing or adding a translation is a new app version rather than a setting.
 *
 * **One file per page, and that is why the whole thing stays cheap.** Reading a page parses a
 * few kilobytes; nothing ever holds a complete translation in memory, which matters on the
 * Transsion phones the testers carry.
 *
 * ⚠ **Nothing here can fall back to the network, and that is deliberate.** If an asset is
 * missing the screen says so. A silent fetch would mean the app sometimes shows text from a
 * source it cannot name with certainty, and § 5z bundled precisely to avoid that.
 */
class Translations(private val context: Context) {

    private val cache = mutableMapOf<Pair<Int, Int>, List<TranslatedVerse>>()

    suspend fun page(page: Int, source: TranslationSource): List<TranslatedVerse> =
        withContext(Dispatchers.IO) {
            cache[source.id to page]?.let { return@withContext it }

            val parsed = runCatching {
                context.assets.open("translations/${source.id}/$page.json").use { stream ->
                    val arr = JSONArray(stream.bufferedReader().readText())
                    buildList {
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            add(TranslatedVerse(o.getString("v"), o.getString("t")))
                        }
                    }
                }
            }.getOrElse {
                // A missing or malformed asset is a build problem, not a user problem, so it
                // is logged loudly and shown as an absence rather than as an error the reader
                // could act on.
                Log.w(TAG, "no bundled translation for ${source.id}/$page", it)
                emptyList()
            }

            cache[source.id to page] = parsed
            parsed
        }

    private companion object { const val TAG = "WirdTranslations" }
}
