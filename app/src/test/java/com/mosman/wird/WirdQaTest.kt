package com.mosman.wird

import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.assignPortion
import com.mosman.wird.domain.progressOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
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
}
