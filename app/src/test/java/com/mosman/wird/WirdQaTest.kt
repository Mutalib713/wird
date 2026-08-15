package com.mosman.wird

import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.assignPortion
import com.mosman.wird.domain.linesOn
import com.mosman.wird.domain.pages
import com.mosman.wird.domain.progressOf
import com.mosman.wird.domain.surahs
import com.mosman.wird.domain.todaysAssignment
import com.mosman.wird.mushaf.Glyph
import com.mosman.wird.mushaf.MushafPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The Wird QA suite.
 *
 * Three checks today. It only grows — every task that finds a bug adds the failing case
 * here first, then fixes it.
 */
class WirdQaTest {

    // ---- 1. Portion arithmetic, including the wrap past the end of the mushaf ----

    @Test
    fun `portion maths handles half pages, whole pages and the wrap past page 604`() {
        // Page 453 (the Surah Sad page) starts at half-page unit 904.
        val page453 = (453 - 1) * Mushaf.UNITS_PER_PAGE
        assertEquals(453, Mushaf.pageOf(page453))

        // Half a page stays on the same page.
        val half = assignPortion(startUnit = page453, units = 1)
        assertEquals(453, half.startPage)
        assertEquals(453, half.endPage)
        assertFalse(half.wrapsPastEnd)
        assertEquals(page453 + 1, half.nextStartUnit)

        // A whole page covers both halves and lands tomorrow on page 454.
        val whole = assignPortion(startUnit = page453, units = 2)
        assertEquals(453, whole.startPage)
        assertEquals(453, whole.endPage)
        assertEquals(454, Mushaf.pageOf(whole.nextStartUnit))

        // Finishing the mushaf is not an ending. Starting on the second half of page
        // 604 with a one-page target runs off the end and continues at page 1.
        val lastUnit = Mushaf.TOTAL_UNITS - 1
        assertEquals(604, Mushaf.pageOf(lastUnit))
        val wrapped = assignPortion(startUnit = lastUnit, units = 2)
        assertEquals(604, wrapped.startPage)
        assertEquals(1, wrapped.endPage)
        assertTrue(wrapped.wrapsPastEnd)
        // That portion covers unit 1207 (second half of page 604) and unit 0 (first
        // half of page 1), so tomorrow starts at unit 1 — the second half of page 1.
        assertEquals(1, wrapped.nextStartUnit)
        assertEquals(1, Mushaf.pageOf(wrapped.nextStartUnit))
    }

    // ---- 2. Streak maths across a missed day ----

    @Test
    fun `a missed day breaks the streak but never reduces total days read`() {
        val today = LocalDate.of(2026, 8, 14)
        fun day(minus: Long, method: Method = Method.TAPPED) =
            DayLog(today.minusDays(minus), method)

        // Read on today, -1, -2. Missed -3. Read on -4, -5, -6.
        val logs = listOf(day(0), day(1), day(2), day(4), day(5), day(6))

        val progress = progressOf(logs, today)
        assertEquals("streak stops at the gap", 3, progress.currentStreak)
        assertEquals("total counts every day read, gap or not", 6, progress.totalDaysRead)

        // An unfinished today must not break the streak — the day is not over yet.
        val withoutToday = logs.filter { it.date != today }
        val stillGoing = progressOf(withoutToday, today)
        assertEquals(2, stillGoing.currentStreak)
        assertEquals(5, stillGoing.totalDaysRead)

        // Missing yesterday as well does end it, and the total still stands.
        val staleStreak = progressOf(withoutToday.filter { it.date != today.minusDays(1) }, today)
        assertEquals(0, staleStreak.currentStreak)
        assertEquals(4, staleStreak.totalDaysRead)
    }

    // ---- 3. Done-method split counting ----

    @Test
    fun `the recited and tapped split is counted honestly`() {
        val today = LocalDate.of(2026, 8, 14)
        fun day(minus: Long, method: Method) = DayLog(today.minusDays(minus), method)

        val logs = listOf(
            day(0, Method.RECITED),
            day(1, Method.TAPPED),
            day(2, Method.TAPPED),
            day(3, Method.RECITED),
            day(4, Method.TAPPED),
        )

        val progress = progressOf(logs, today)
        assertEquals(5, progress.totalDaysRead)
        assertEquals(2, progress.recitedDays)
        assertEquals(3, progress.tappedDays)

        // Double submit: two entries for one date count as one day, and reciting wins
        // over tapping. Marking then reciting must not inflate the total.
        val doubleSubmitted = logs + day(1, Method.RECITED)
        val deduped = progressOf(doubleSubmitted, today)
        assertEquals("still five days", 5, deduped.totalDaysRead)
        assertEquals("the recitation counts", 3, deduped.recitedDays)
        assertEquals(2, deduped.tappedDays)
    }

    // ---- 4. The plan: per-weekday targets ----

    @Test
    fun `a weekday override changes only that day, and the position does not drift`() {
        // "One page a day, but Fridays are heavy so go easy on me."
        val plan = ReadingPlan(
            defaultUnits = 2,
            weekdayUnits = mapOf(DayOfWeek.FRIDAY to 1),
        )
        val page453 = (453 - 1) * Mushaf.UNITS_PER_PAGE

        val thursday = LocalDate.of(2026, 8, 13) // a Thursday
        val friday = LocalDate.of(2026, 8, 14)
        assertEquals(DayOfWeek.THURSDAY, thursday.dayOfWeek)
        assertEquals(DayOfWeek.FRIDAY, friday.dayOfWeek)

        assertEquals(2, todaysAssignment(page453, plan, thursday).units)
        assertEquals(1, todaysAssignment(page453, plan, friday).units)

        // Opening the app twice on the same day must give the same portion. The position
        // moves only when a day is marked done, never on a launch or a date read.
        val first = todaysAssignment(page453, plan, friday)
        val second = todaysAssignment(page453, plan, friday)
        assertEquals(first, second)
    }

    // ---- 5. Surah boundaries ----

    @Test
    fun `a portion that crosses into the next surah reports both`() {
        // Sad runs 453-458 and Az-Zumar starts on 458, so page 458 holds both.
        assertEquals(listOf(38, 39), SurahIndex.on(458).map { it.number })
        assertEquals(listOf(38), SurahIndex.on(457).map { it.number })

        val page457 = (457 - 1) * Mushaf.UNITS_PER_PAGE
        val twoPages = assignPortion(startUnit = page457, units = 4)
        assertEquals(listOf(457, 458), twoPages.pages)
        assertEquals(
            "crossing onto 458 picks up Az-Zumar",
            listOf("Sad", "Az-Zumar"),
            twoPages.surahs.map { it.name },
        )

        // A portion wholly inside one surah reports one.
        val onePage = assignPortion(startUnit = page457, units = 2)
        assertEquals(listOf("Sad"), onePage.surahs.map { it.name })

        // The wrap past the end is also a surah boundary. Page 604 is not one surah —
        // it carries the last three — so finishing the mushaf and starting again touches
        // four, and they must come back in the order you actually meet them.
        assertEquals(
            listOf("Al-Ikhlas", "Al-Falaq", "An-Nas"),
            SurahIndex.on(604).map { it.name },
        )
        val lastUnit = Mushaf.TOTAL_UNITS - 1
        val wrapped = assignPortion(startUnit = lastUnit, units = 2)
        assertEquals(listOf(604, 1), wrapped.pages)
        assertEquals(
            "reading order, not surah number — Al-Fatihah comes last here",
            listOf("Al-Ikhlas", "Al-Falaq", "An-Nas", "Al-Fatihah"),
            wrapped.surahs.map { it.name },
        )
    }

    // ---- 6. Half a page lights half the lines ----

    @Test
    fun `a half page target lights half the page's lines`() {
        val page453 = (453 - 1) * Mushaf.UNITS_PER_PAGE
        // Page 453 opens Surah Sad, so line 1 is the bismillah and the text runs 2..15.
        val rendered = (2..15).toList()

        val firstHalf = assignPortion(page453, units = 1).linesOn(453, rendered)
        val secondHalf = assignPortion(page453 + 1, units = 1).linesOn(453, rendered)
        val wholePage = assignPortion(page453, units = 2).linesOn(453, rendered)

        assertEquals("top half, rounded up", (2..8).toSet(), firstHalf)
        assertEquals("bottom half", (9..15).toSet(), secondHalf)
        assertEquals(rendered.toSet(), wholePage)

        // The two halves must tile the page exactly: no line lit twice, none missed.
        assertTrue("halves overlap", (firstHalf intersect secondHalf).isEmpty())
        assertEquals("halves leave a gap", rendered.toSet(), firstHalf + secondHalf)

        // An odd line count still tiles.
        val odd = (1..15).toList()
        val a = assignPortion(page453, 1).linesOn(453, odd)
        val b = assignPortion(page453 + 1, 1).linesOn(453, odd)
        assertEquals(8, a.size)
        assertEquals(7, b.size)
        assertEquals(odd.toSet(), a + b)

        // A page that isn't in today's portion lights nothing.
        assertTrue(assignPortion(page453, 2).linesOn(500, rendered).isEmpty())
    }

    // ---- 7. Regression: a page is not named by its first verse ----

    @Test
    fun `a page holding two surahs is named by where the portion starts`() {
        // Real shape of page 440: it opens with the LAST ayah of Fatir (35:45) and only
        // then begins Ya-Sin. Choosing Ya-Sin used to make the app announce "Fatir",
        // because the page was named after whatever verse came first on it.
        val page440 = MushafPage(
            page = 440,
            glyphs = buildList {
                add(glyph("35:45", line = 1))
                add(glyph("35:45", line = 2))
                (1..12).forEach { ayah ->
                    add(glyph("36:$ayah", line = 3 + (ayah - 1) / 2))
                }
            },
            surahStarts = mapOf("36:1" to "36"),
            surahName = "Fatir",
            juz = 22,
            bismillahCodes = null,
        )

        assertEquals("line 1 is still Fatir", 35, page440.surahNumberOn(1))
        assertEquals("Ya-Sin starts on line 3", 3, page440.lineOf(36, 1))
        assertEquals("line 3 onwards is Ya-Sin", 36, page440.surahNumberOn(3))

        // A reader who chose Ya-Sin starts at line 3, so the label is Ya-Sin, and the
        // two lines of Fatir above are not part of their portion.
        val startLine = page440.lineOf(36, 1)!!
        val portion = page440.lines.filter { it >= startLine }
        assertFalse("Fatir's lines are not today's", portion.contains(1))
        assertFalse(portion.contains(2))
        assertEquals(
            "Ya-Sin",
            SurahIndex.byNumber(page440.surahNumberOn(portion.first())!!)?.name,
        )

        // And an ayah that isn't on the page reports nothing rather than guessing.
        assertEquals(null, page440.lineOf(36, 40))
    }

    private fun glyph(verseKey: String, line: Int) =
        Glyph(code = "x", line = line, verseKey = verseKey, isEndMarker = false)
}
