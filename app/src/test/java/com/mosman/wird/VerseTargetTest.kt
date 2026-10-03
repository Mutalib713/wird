package com.mosman.wird

import com.mosman.wird.data.DayLogStore
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.ReadingDirection
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.VerseIndex
import com.mosman.wird.domain.assignmentOn
import com.mosman.wird.domain.ayahRangeIn
import com.mosman.wird.domain.pages
import com.mosman.wird.domain.portionKeys
import com.mosman.wird.domain.todaysAssignment
import com.mosman.wird.domain.verseAssignment
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

/**
 * Daily targets in verses ("10 verses a day"), and the one rule for which verses are today's.
 *
 * Written 2026-10-03. Before it, a custom verse target was saved and shown on Home but the
 * portion was still worked out in pages, onboarding dropped the number, and Settings rounded it
 * to pages. Separately, the recitation check compared against every word on the page, so a
 * correct half-page recitation always came out "unreliable".
 */
class VerseTargetTest {

    private val today = LocalDate.of(2026, 10, 5)

    // ---- the page table must match the text the app actually ships ----------------------

    @Test
    fun every_page_lists_exactly_the_verses_in_the_bundled_text() {
        var total = 0
        for (page in 1..Mushaf.PAGES) {
            val arr = JSONArray(File("src/main/assets/arabic/$page.json").readText())
            val shipped = (0 until arr.length()).map { arr.getJSONObject(it).getString("v") }
            val indexed = VerseIndex.versesOn(page).map { "${it.first}:${it.second}" }
            assertEquals("page $page", shipped, indexed)
            total += indexed.size
        }
        assertEquals(6236, total)
    }

    @Test
    fun known_verses_are_on_their_printed_pages() {
        assertEquals(1, VerseIndex.pageOf(1 to 1))
        assertEquals(42, VerseIndex.pageOf(2 to 255))
        assertEquals(293, VerseIndex.pageOf(18 to 1))
        assertEquals(440, VerseIndex.pageOf(36 to 1))
        assertEquals(604, VerseIndex.pageOf(114 to 6))
    }

    // ---- walking N verses in either direction ------------------------------------------

    @Test
    fun forwards_crosses_into_the_next_surah() {
        val ten = VerseIndex.walk(1 to 1, 10, ReadingDirection.TOWARDS_NAS)
        assertEquals((1..7).map { 1 to it } + listOf(2 to 1, 2 to 2, 2 to 3), ten)
    }

    @Test
    fun towards_fatihah_reads_each_surah_forwards_then_steps_back_a_surah() {
        // The Ghanaian madrasa order: finish Al-Falaq, then Al-Ikhlas from its first ayah.
        val v = VerseIndex.walk(113 to 4, 3, ReadingDirection.TOWARDS_FATIHAH)
        assertEquals(listOf(113 to 4, 113 to 5, 112 to 1), v)
    }

    @Test
    fun both_ends_wrap() {
        assertEquals(1 to 1, VerseIndex.next(114 to 6, ReadingDirection.TOWARDS_NAS))
        assertEquals(114 to 1, VerseIndex.next(1 to 7, ReadingDirection.TOWARDS_FATIHAH))
    }

    // ---- a verse track's portion -------------------------------------------------------

    private val tenADay = ReadingTrack(
        id = "hifz", name = "Hifz", customTargetVerses = 10, startVerseSurah = 36, startVerseAyah = 1,
        positionUnit = (440 - 1) * Mushaf.UNITS_PER_PAGE,
    )

    @Test
    fun a_verse_target_gives_exactly_that_many_verses() {
        val a = tenADay.assignmentOn(today)
        assertEquals(10, a.verses!!.size)
        assertEquals(36 to 1, a.verses!!.first())
        assertEquals(36 to 10, a.verses!!.last())
        assertEquals(36 to 11, a.nextVerse)
        assertEquals("Ayahs 1–10", a.ayahRangeIn(36))
    }

    @Test
    fun tomorrow_starts_on_the_page_of_the_next_verse() {
        val a = tenADay.assignmentOn(today)
        assertEquals(VerseIndex.unitOf(36 to 11), a.nextStartUnit)
    }

    @Test
    fun a_verse_portion_lists_every_page_it_touches() {
        val a = verseAssignment(2 to 1, 20, ReadingDirection.TOWARDS_NAS)
        // Page 2 is 2:1-5, page 3 is 2:6-16, page 4 starts at 2:17.
        assertEquals(listOf(2, 3, 4), a.pages)
    }

    @Test
    fun ten_verses_from_al_baqarah_1_are_exactly_1_to_10_across_two_pages() {
        // Page 2 shows 2:1-5 lit; page 3 must light 2:6-10 and leave 2:11-16 dim.
        val a = verseAssignment(2 to 1, 10, ReadingDirection.TOWARDS_NAS)
        assertEquals((1..10).map { "2:$it" }, a.portionKeys())
        assertEquals(listOf(2, 3), a.pages)
    }

    // ---- the one rule for "today's verses" -----------------------------------------------

    @Test
    fun a_half_page_portion_is_half_the_page_not_the_whole_page() {
        // Page 1 has 7 verses; its first half is the top four. The check used to expect all 7.
        val half = todaysAssignment(startUnit = 0, plan = ReadingPlan(defaultUnits = 1), date = today)
        assertEquals(listOf("1:1", "1:2", "1:3", "1:4"), half.portionKeys())
    }

    @Test
    fun a_page_portion_from_a_mid_page_start_verse_begins_there() {
        // Ya-Sin starts partway down page 440, under the end of Fatir.
        val a = todaysAssignment(startUnit = (440 - 1) * 2, plan = ReadingPlan(defaultUnits = 2), date = today)
        val keys = a.portionKeys(startVerse = 36 to 1)
        assertEquals("36:1", keys.first())
    }

    // ---- a finished verse day keeps its verses --------------------------------------------

    @Test
    fun the_day_log_remembers_a_verse_portion() {
        val days = DayLogStore(Files.createTempDirectory("wird-verses").toFile())
        days.markDone(today, Method.TAPPED, trackId = "hifz", firstVerse = 36 to 1, verseCount = 10)
        assertEquals((36 to 1) to 10, days.coveredVerses(today, "hifz"))
        days.markDone(today, Method.TAPPED, trackId = "daily", startUnit = 0, units = 2)
        assertNull(days.coveredVerses(today, "daily"))
    }
}
