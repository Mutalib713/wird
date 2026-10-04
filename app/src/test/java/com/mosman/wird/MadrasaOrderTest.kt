package com.mosman.wird

import com.mosman.wird.domain.MadrasaOrder
import com.mosman.wird.domain.ReadingDirection
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.TrackType
import com.mosman.wird.domain.VerseIndex
import com.mosman.wird.domain.assignmentOn
import com.mosman.wird.domain.pages
import com.mosman.wird.domain.portionKeys
import com.mosman.wird.domain.surahs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.LocalDate

/**
 * Madrasa order with a page target: from An-Nās back towards Al-Fātiḥah, so many pages a day.
 *
 * Written 2026-10-04, after replaying the old page walk over the whole mushaf. It guessed the
 * sūrah from the page, and on the 41 pages two sūrahs share it guessed wrong: from An-Nās at a page
 * a day it bounced between pages 599 and 600 from day 7, and the quick start (An-Naba') went round
 * pages 582, 583 and 584 from day 4. Starting at An-Nās also lit only An-Nās on page 604, so
 * Al-Falaq and Al-Ikhlāṣ were never read.
 */
class MadrasaOrderTest {

    private val way = ReadingDirection.TOWARDS_FATIHAH

    /** One day from [start]: its verses, in reading order. */
    private fun day(start: Pair<Int, Int>, units: Int): List<Pair<Int, Int>> =
        VerseIndex.walk(start, MadrasaOrder.versesFor(start, units), way)

    /** "114:1-6, 113:1-5" for a run of verses. */
    private fun label(verses: List<Pair<Int, Int>>): String {
        val runs = mutableListOf<Triple<Int, Int, Int>>()
        for ((s, a) in verses) {
            val last = runs.lastOrNull()
            if (last != null && last.first == s) runs[runs.size - 1] = Triple(s, last.second, a)
            else runs += Triple(s, a, a)
        }
        return runs.joinToString(", ") { (s, from, to) -> if (from == to) "$s:$from" else "$s:$from-$to" }
    }

    /** The first [n] days from [start]. */
    private fun days(start: Pair<Int, Int>, units: Int, n: Int): List<List<Pair<Int, Int>>> {
        val out = mutableListOf<List<Pair<Int, Int>>>()
        var at = start
        repeat(n) {
            val today = day(at, units)
            out += today
            at = VerseIndex.next(today.last(), way)
        }
        return out
    }

    @Test
    fun a_page_from_an_nas_is_all_of_page_604_read_in_madrasa_order() {
        val first = day(114 to 1, units = 2)
        assertEquals("114:1-6, 113:1-5, 112:1-4", label(first))
        assertEquals(listOf(604), first.map { VerseIndex.pageOf(it) }.distinct())
    }

    @Test
    fun half_a_page_in_juz_amma_is_one_short_surah_a_day() {
        assertEquals(
            listOf("114:1-6", "113:1-5", "112:1-4", "111:1-5"),
            days(114 to 1, units = 1, n = 4).map(::label),
        )
    }

    @Test
    fun a_page_a_day_walks_down_the_surahs_and_never_turns_back() {
        val days = days(114 to 1, units = 2, n = 60)
        assertEquals(listOf(604, 603, 602), days.take(3).map { d -> d.maxOf { VerseIndex.pageOf(it) } })
        // The old walk went 604 … 600, 599, 600, 599, … from day 7. Pages may rise inside a long
        // sūrah, which is read forwards; the sūrah a day starts in never may.
        days.map { it.first().first }.zipWithNext().forEach { (today, tomorrow) ->
            assertTrue("went back up from sūrah $today to $tomorrow", tomorrow <= today)
        }
    }

    @Test
    fun the_quick_start_no_longer_goes_round_pages_582_to_584() {
        val surahs = days(78 to 1, units = 2, n = 12).map { it.first().first }
        assertEquals(78, surahs.first())
        surahs.zipWithNext().forEach { (today, tomorrow) ->
            assertTrue("came back to sūrah $tomorrow after $today", tomorrow <= today)
        }
        assertTrue("never reached Al-Mursalāt", 77 in surahs)
    }

    @Test
    fun after_ya_sin_comes_fatir() {
        val after = days(36 to 1, units = 2, n = 20).first { it.first().first != 36 }
        assertEquals(35 to 1, after.first())
    }

    @Test
    fun every_verse_comes_up_exactly_once_before_the_cycle_starts_again() {
        val starts = listOf(114 to 1, 78 to 1, 36 to 1, 2 to 255, 1 to 1)
        for (start in starts) for (units in listOf(1, 2, 4)) {
            val seen = HashSet<Pair<Int, Int>>()
            var at = start
            var day = 0
            while (seen.size < 6236) {
                day++
                if (day > 2_000) fail("$start at $units half-pages never finished the mushaf")
                val today = day(at, units)
                for (v in today) {
                    if (seen.size == 6236) break // the next cycle has begun inside today's run
                    if (!seen.add(v)) fail("$start at $units half-pages read $v twice by day $day")
                }
                at = VerseIndex.next(today.last(), way)
            }
        }
    }

    // ---- through the track, the way Home, the widget and the reminder ask for it ----------

    private val madrasa = ReadingTrack(
        id = "madrasa", name = "Juz' ʿAmma", type = TrackType.HIFZ,
        direction = ReadingDirection.TOWARDS_FATIHAH, dailyUnits = 2,
        positionUnit = (604 - 1) * 2, startVerseSurah = 114, startVerseAyah = 1,
    )

    // 2026-10-05 is a Monday.
    private val mon = LocalDate.of(2026, 10, 5)

    @Test
    fun a_madrasa_track_reads_al_falaq_and_al_ikhlas_on_its_first_day() {
        val a = madrasa.assignmentOn(mon)
        assertNotNull("a madrasa page target is measured out in verses", a.verses)
        assertEquals(listOf("An-Nas", "Al-Falaq", "Al-Ikhlas"), a.surahs.map { it.name })
        assertTrue("112:1" in a.portionKeys())
        assertEquals(listOf(604), a.pages)
        assertEquals(111 to 1, a.nextVerse)
    }

    @Test
    fun a_weekday_exception_still_applies_in_madrasa_order() {
        val halfOnFriday = madrasa.copy(weekdayUnits = mapOf(FRIDAY to 1))
        assertEquals(6, halfOnFriday.assignmentOn(mon.plusDays(4)).verses?.size)
        assertEquals(15, halfOnFriday.assignmentOn(mon).verses?.size)
    }

    @Test
    fun a_page_target_towards_an_nas_is_still_a_page_portion() {
        val forward = madrasa.copy(direction = ReadingDirection.TOWARDS_NAS, startVerseSurah = null, startVerseAyah = null)
        assertNull(forward.assignmentOn(mon).verses)
    }
}
