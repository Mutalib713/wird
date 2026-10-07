package com.mosman.wird

import com.mosman.wird.data.TranslationSource
import com.mosman.wird.data.shown
import com.mosman.wird.data.versesIn
import com.mosman.wird.domain.QuranSearch
import com.mosman.wird.domain.QuranSearch.Field
import com.mosman.wird.domain.VerseIndex
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Word search (PLAN task 26), against the real bundled text.
 *
 * Written 2026-10-04 from what readers reported in Quran for Android between 2014 and 2019:
 * "قمر" found nothing while "القمر" and "والقمر" found different verses, hamza spellings didn't
 * meet, and exact matches came after partial ones.
 */
class QuranSearchTest {

    private companion object {
        val dirs = mapOf(
            Field.ARABIC to "arabic",
            Field.ENGLISH to "translations/20",
            Field.HAUSA to "translations/32",
            Field.TRANSLITERATION to "translations/57",
        )

        /** One page file through org.json, independent of the app's own reader. */
        fun pageFile(field: Field, page: Int): List<Pair<String, String>> {
            val arr = JSONArray(File("src/main/assets/${dirs.getValue(field)}/$page.json").readText())
            return List(arr.length()) { i -> arr.getJSONObject(i).let { it.getString("v") to it.getString("t") } }
        }

        /** Every verse's four texts, exactly as the app shows them (through [shown]). */
        val verses: List<QuranSearch.VerseText> by lazy {
            val out = ArrayList<QuranSearch.VerseText>(6236)
            for (page in 1..604) {
                val en = pageFile(Field.ENGLISH, page).toMap()
                val ha = pageFile(Field.HAUSA, page).toMap()
                val tr = pageFile(Field.TRANSLITERATION, page).toMap()
                for ((key, arabic) in pageFile(Field.ARABIC, page)) {
                    val (s, a) = key.split(':').map(String::toInt)
                    out += QuranSearch.VerseText(
                        key = s to a,
                        arabic = arabic,
                        english = shown(TranslationSource.SAHEEH, en[key].orEmpty()),
                        hausa = shown(TranslationSource.HAUSA, ha[key].orEmpty()),
                        transliteration = shown(TranslationSource.TRANSLITERATION, tr[key].orEmpty()),
                    )
                }
            }
            out
        }
        val byKey by lazy { verses.associateBy { it.key } }

        /** What the app searches: the four shipped files. */
        val index: QuranSearch.Index by lazy { QuranSearch.Index(Field.entries.associateWith { shipped(it).readText() }) }

        fun shipped(field: Field) = File("src/main/assets/${QuranSearch.Index.fileOf(field)}")
    }

    /** Search, then show every match as the app shows one, on the verse's own words. */
    private fun hits(query: String) =
        QuranSearch.search(index, query).map { QuranSearch.hit(it, byKey.getValue(it.key).of(it.field)) }

    private fun keys(query: String) = QuranSearch.search(index, query).map { it.key }.toSet()

    @Test
    fun the_shipped_index_is_the_bundled_texts_folded() {
        val folded = QuranSearch.Index.of(verses).texts
        for (field in Field.entries) {
            val file = shipped(field)
            // Regenerate after changing a text or a fold: set WIRD_WRITE_SEARCH_INDEX=1, run this test.
            if (System.getenv("WIRD_WRITE_SEARCH_INDEX") == "1") file.apply { parentFile?.mkdirs() }.writeText(folded.getValue(field))
            val now = file.takeIf { it.exists() }?.readText().orEmpty()
            if (now != folded.getValue(field)) {
                val line = now.lines().zip(folded.getValue(field).lines()).indexOfFirst { (a, b) -> a != b }
                fail("$file is stale from line ${line + 1}. Set WIRD_WRITE_SEARCH_INDEX=1 and run this test to rewrite it.")
            }
        }
    }

    @Test
    fun the_whole_quran_is_indexed() {
        assertEquals(6236, index.keys.size)
        assertEquals(verses.map { it.key }, index.keys)
    }

    @Test
    fun each_result_reads_its_words_from_the_page_the_app_opens() {
        // SearchTexts reads a result's words from the page file VerseIndex names for it.
        for (field in Field.entries) {
            val onPage = (1..604).associateWith { page -> pageFile(field, page).map { it.first }.toSet() }
            for (key in index.keys) {
                assertTrue("$key in ${dirs[field]}", "${key.first}:${key.second}" in onPage.getValue(VerseIndex.pageOf(key)))
            }
        }
    }

    @Test
    fun the_quick_reader_reads_every_file_as_org_json_does() {
        for (field in Field.entries) for (page in 1..604) {
            val raw = File("src/main/assets/${dirs.getValue(field)}/$page.json").readText()
            assertEquals("${dirs[field]}/$page", pageFile(field, page), versesIn(raw))
        }
    }

    @Test
    fun a_word_is_found_with_or_without_the_letters_written_onto_it() {
        val bare = keys("قمر")
        assertTrue("the moon is in the Qur'an", bare.isNotEmpty())
        assertEquals(bare, keys("القمر"))
        assertEquals(bare, keys("والقمر"))
    }

    @Test
    fun the_word_exactly_as_typed_comes_first() {
        val first = hits("القمر").first()
        assertEquals(4, first.rank)
        assertTrue(first.highlight.isNotEmpty())
    }

    @Test
    fun hamza_and_vowel_marks_do_not_stop_a_match() {
        assertTrue("2:255 says la ilaha", (2 to 255) in keys("اله"))
        assertTrue("2:124 names Ibrahim", (2 to 124) in keys("ابراهيم"))
        assertEquals(keys("الرحمن"), keys("الرَّحْمَٰنِ"))
    }

    @Test
    fun the_highlight_lands_on_the_matched_letters() {
        for (hit in hits("قمر").take(20)) {
            assertTrue("${hit.key} has a highlight", hit.highlight.isNotEmpty())
            for (range in hit.highlight) {
                val marked = hit.text.substring(range.first, range.last + 1)
                assertTrue("'$marked' in ${hit.key}", QuranSearch.foldArabic(marked).text.contains("قمر"))
            }
        }
        for (hit in hits("mercy").take(20)) {
            for (range in hit.highlight) {
                assertEquals("${hit.key}", "mercy", hit.text.substring(range.first, range.last + 1).lowercase())
            }
        }
    }

    @Test
    fun words_other_than_the_indexed_ones_get_no_highlight() {
        val match = QuranSearch.search(index, "mercy").first()
        assertTrue(QuranSearch.hit(match, "Some other words entirely").highlight.isEmpty())
    }

    @Test
    fun english_hausa_and_transliteration_are_searched_too() {
        val mercy = QuranSearch.search(index, "mercy")
        assertTrue(mercy.isNotEmpty())
        assertEquals(Field.ENGLISH, mercy.first().field)

        val hausa = QuranSearch.search(index, "Ubangijin")
        assertTrue(hausa.isNotEmpty())
        assertTrue(hausa.all { it.field == Field.HAUSA })

        assertTrue("1:1 by sound", (1 to 1) in keys("raheem"))
        assertTrue("and spelled another way", (1 to 1) in keys("rahim"))
        assertTrue("112:1 by its opening words", (112 to 1) in keys("qul huwa allahu ahad"))
    }

    @Test
    fun hausa_quotation_marks_are_real_marks_not_html() {
        val found = hits("Makaɗaĩci")
        val hit = found.first { it.key == 112 to 1 }
        assertFalse(hit.text.contains("&quot;"))
        assertTrue(hit.text.contains('"'))
        // 12:39 is quoted from its first character to its last; it still shows, highlighted.
        for (each in found) {
            assertFalse("${each.key}", each.text.contains("&quot;"))
            assertTrue("${each.key} has a highlight", each.highlight.isNotEmpty())
        }
        assertTrue(found.first { it.key == 12 to 39 }.text.startsWith("\"Yã abõkaina"))
    }

    @Test
    fun hausa_letters_that_arrived_broken_read_as_hausa() {
        // Quran.com's Gumi writes Ƙ, Ɗ and Ɓ as ¡, ¦ and ¥; repaired where read (2026-10-07).
        assertTrue(verses.none { v -> v.hausa.any { it == '¡' || it == '¦' || it == '¥' } })
        assertTrue(byKey.getValue(2 to 85).hausa.contains("Rãnar Ƙiyãma"))
        assertTrue(byKey.getValue(30 to 41).hausa.startsWith("Ɓarnã"))
        // The Day of Resurrection is found by its Hausa name now; before, those verses were missed.
        assertTrue("2:85 by Ƙiyãma", (2 to 85) in keys("kiyama"))
        assertTrue("43:57, Ɗan Maryama", (43 to 57) in keys("dan maryama"))
    }

    @Test
    fun a_combining_accent_stays_with_its_letter() {
        // The Hausa file writes the disjointed letters with a combining tilde: "A. L̃. M̃." (2:1).
        assertEquals("a l m ", QuranSearch.foldLatin("A. L̃. M̃.").text)
    }

    @Test
    fun one_letter_is_too_little_to_search() {
        assertFalse(QuranSearch.searchable("ق"))
        assertFalse(QuranSearch.searchable("a"))
        assertTrue(QuranSearch.searchable("قمر"))
        assertTrue(QuranSearch.search(index, "a").isEmpty())
    }
}
