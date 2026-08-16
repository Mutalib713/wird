package com.mosman.wird

import com.mosman.wird.data.decodeSchedule
import com.mosman.wird.data.encodeSchedule
import com.mosman.wird.domain.Coordinates
import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.NudgeSchedule
import com.mosman.wird.domain.Prayer
import com.mosman.wird.domain.PrayerMethod
import com.mosman.wird.domain.PrayerTimes
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.assignPortion
import com.mosman.wird.domain.label
import com.mosman.wird.domain.linesOn
import com.mosman.wird.domain.nextAfter
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
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The Wird QA suite.
 *
 * It only grows — every task that finds a bug adds the failing case here first, then
 * fixes it. Checks 10 and 11 arrived with task 8 and are the only ones that assert
 * against numbers from outside this codebase: the prayer times came from the Aladhan API,
 * so they check the algorithm rather than checking it against itself.
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

    // ---- 8. Marking done moves the position, and undo puts it back exactly ----

    @Test
    fun `finishing a day advances the position, and undo returns it`() {
        val plan = ReadingPlan(defaultUnits = 2)
        val monday = LocalDate.of(2026, 8, 17)

        // Ya-Sin begins on page 440.
        var position = (440 - 1) * Mushaf.UNITS_PER_PAGE
        val assignment = todaysAssignment(position, plan, monday)

        // Done: the position moves to where tomorrow starts.
        position = assignment.nextStartUnit
        assertEquals("a one-page day lands on the next page", 441, Mushaf.pageOf(position))

        // Undo: back by exactly what the day covered, not by a guess.
        position = Math.floorMod(position - assignment.units, Mushaf.TOTAL_UNITS)
        assertEquals(440, Mushaf.pageOf(position))
        assertEquals("undo is exact, not approximate", (440 - 1) * Mushaf.UNITS_PER_PAGE, position)

        // The same round trip has to survive the wrap past the end of the mushaf, which
        // is where a naive minus would produce a negative unit and crash or misplace.
        var atEnd = Mushaf.TOTAL_UNITS - 1
        val last = todaysAssignment(atEnd, plan, monday)
        atEnd = last.nextStartUnit
        assertEquals("finishing 604 continues at page 1", 1, Mushaf.pageOf(atEnd))
        atEnd = Math.floorMod(atEnd - last.units, Mushaf.TOTAL_UNITS)
        assertEquals("and undo goes back over the wrap", 604, Mushaf.pageOf(atEnd))
        assertEquals(Mushaf.TOTAL_UNITS - 1, atEnd)

        // A half-page day moves half a page, so two undos are not needed for one done.
        val half = todaysAssignment(position, ReadingPlan(defaultUnits = 1), monday)
        val afterHalf = half.nextStartUnit
        assertEquals(440, Mushaf.pageOf(afterHalf))
        assertEquals(position + 1, afterHalf)
    }

    // ---- 9. Ten days with gaps, and the split that must stay honest ----

    @Test
    fun `ten days with two gaps produce the right streak, total and split`() {
        val today = LocalDate.of(2026, 8, 16)
        fun day(minus: Long, m: Method) = DayLog(today.minusDays(minus), m)

        // A realistic fortnight: read today and the two before, missed one, read four
        // more, missed two, read three. Recited on some, tapped on others.
        val logs = listOf(
            day(0, Method.RECITED),
            day(1, Method.TAPPED),
            day(2, Method.RECITED),
            // gap at 3
            day(4, Method.TAPPED),
            day(5, Method.TAPPED),
            day(6, Method.RECITED),
            day(7, Method.TAPPED),
            // gap at 8 and 9
            day(10, Method.RECITED),
            day(11, Method.TAPPED),
            day(12, Method.TAPPED),
        )

        val p = progressOf(logs, today)
        assertEquals("the streak stops at the first gap", 3, p.currentStreak)
        assertEquals("the total counts every day, gaps and all", 10, p.totalDaysRead)
        assertEquals(4, p.recitedDays)
        assertEquals(6, p.tappedDays)
        assertEquals("the split always accounts for every day", p.totalDaysRead, p.recitedDays + p.tappedDays)

        // The number this app exists to keep honest: a day marked and later recited counts
        // once, and counts as recited. It can never go the other way.
        val upgraded = progressOf(logs + day(1, Method.RECITED), today)
        assertEquals(10, upgraded.totalDaysRead)
        assertEquals(5, upgraded.recitedDays)
        assertEquals(5, upgraded.tappedDays)

        val notDowngraded = progressOf(logs + day(0, Method.TAPPED), today)
        assertEquals("a tap cannot undo a recitation", 4, notDowngraded.recitedDays)
    }

    // ---- 10. Prayer times match a known source ----

    /**
     * The check task 8 exists to pass.
     *
     * Every expected value below came from the Aladhan API (Muslim World League) on
     * 2026-08-16 — an independent implementation, not this one. Two cities because
     * Mutalib moves between Pig Farm in Accra and KNUST in Kumasi, and both solstices
     * because a bug in the declination term would hide completely at an equinox.
     *
     * Exact equality, not a tolerance. A minute of slack here would hide a systematic
     * error, and there is no reason to grant slack to arithmetic that matched perfectly.
     */
    @Test
    fun `prayer times match the reference source for Accra and Kumasi`() {
        val accra = Coordinates(5.6037, -0.1870)
        val kumasi = Coordinates(6.6885, -1.6244)
        val ghana = ZoneId.of("Africa/Accra")

        fun check(
            label: String,
            at: Coordinates,
            date: LocalDate,
            fajr: String, sunrise: String, dhuhr: String,
            asr: String, maghrib: String, isha: String,
        ) {
            val times = PrayerTimes.compute(date, at, ghana)
            assertEquals("$label Fajr", LocalTime.parse(fajr), times[Prayer.FAJR])
            assertEquals("$label sunrise", LocalTime.parse(sunrise), times.sunrise)
            assertEquals("$label Dhuhr", LocalTime.parse(dhuhr), times[Prayer.DHUHR])
            assertEquals("$label Asr", LocalTime.parse(asr), times[Prayer.ASR])
            assertEquals("$label Maghrib", LocalTime.parse(maghrib), times[Prayer.MAGHRIB])
            assertEquals("$label Isha", LocalTime.parse(isha), times[Prayer.ISHA])
        }

        check(
            "Accra today", accra, LocalDate.of(2026, 8, 16),
            "04:45", "05:56", "12:05", "15:20", "18:14", "19:21",
        )
        check(
            "Accra midwinter", accra, LocalDate.of(2026, 12, 21),
            "04:50", "06:05", "11:59", "15:21", "17:53", "19:03",
        )
        check(
            "Accra midsummer", accra, LocalDate.of(2026, 6, 21),
            "04:33", "05:49", "12:03", "15:30", "18:16", "19:28",
        )
        check(
            "Kumasi today", kumasi, LocalDate.of(2026, 8, 16),
            "04:49", "06:01", "12:11", "15:25", "18:21", "19:28",
        )

        // The 7-minute gap between the two cities is the whole reason Mutalib chose
        // location over a timezone lookup. If this ever collapses to zero, the longitude
        // has stopped being used.
        val accraMaghrib = PrayerTimes.compute(LocalDate.of(2026, 8, 16), accra, ghana)[Prayer.MAGHRIB]!!
        val kumasiMaghrib = PrayerTimes.compute(LocalDate.of(2026, 8, 16), kumasi, ghana)[Prayer.MAGHRIB]!!
        assertEquals(7, Duration.between(accraMaghrib, kumasiMaghrib).toMinutes())
    }

    /**
     * Maghrib is sunset, and sunset is astronomy rather than convention.
     *
     * Measured across five published methods on 2026-08-16: Accra returned 18:14 in
     * every one, while Fajr ranged over nineteen minutes. This is why the default anchor
     * being Maghrib matters — it makes the one setting nobody will ever look at almost
     * unable to do harm.
     */
    @Test
    fun `the calculation method moves Fajr and Isha but never Maghrib`() {
        val accra = Coordinates(5.6037, -0.1870)
        val ghana = ZoneId.of("Africa/Accra")
        val date = LocalDate.of(2026, 8, 16)

        val methods = listOf(
            PrayerMethod(fajrAngle = 18.0, ishaAngle = 17.0), // Muslim World League
            PrayerMethod(fajrAngle = 15.0, ishaAngle = 15.0), // ISNA
            PrayerMethod(fajrAngle = 19.5, ishaAngle = 17.5), // Egyptian
        )

        val maghribs = methods.map { PrayerTimes.compute(date, accra, ghana, it)[Prayer.MAGHRIB] }
        assertEquals("Maghrib must not move with the method", 1, maghribs.distinct().size)
        assertEquals(LocalTime.of(18, 14), maghribs.first())

        val fajrs = methods.map { PrayerTimes.compute(date, accra, ghana, it)[Prayer.FAJR] }
        assertTrue("Fajr should move with the method", fajrs.distinct().size > 1)
    }

    // ---- 11. The nudge lands at the right offset, on the right day ----

    @Test
    fun `the nudge fires at the chosen offset and rolls to tomorrow once it has passed`() {
        val accra = Coordinates(5.6037, -0.1870)
        val ghana = ZoneId.of("Africa/Accra")
        // Maghrib in Accra on this date is 18:14, verified against Aladhan above.
        val maghrib = ZonedDateTime.of(LocalDate.of(2026, 8, 16), LocalTime.of(18, 14), ghana)

        val schedule = NudgeSchedule.Default
        assertEquals(Prayer.MAGHRIB, schedule.prayer)
        assertEquals(30, schedule.offsetMinutes)

        // Morning of the same day: today's Maghrib is still to come.
        val morning = ZonedDateTime.of(LocalDate.of(2026, 8, 16), LocalTime.of(9, 0), ghana)
        assertEquals(maghrib.plusMinutes(30), schedule.nextAfter(morning, accra))

        // One minute before it is due: still today.
        assertEquals(
            maghrib.plusMinutes(30),
            schedule.nextAfter(maghrib.plusMinutes(29), accra),
        )

        // One minute after: today is gone, and it must not fire late.
        val next = schedule.nextAfter(maghrib.plusMinutes(31), accra)!!
        assertEquals(LocalDate.of(2026, 8, 17), next.toLocalDate())
        assertEquals(LocalTime.of(18, 44), next.toLocalTime())

        // Zero offset lands exactly on the prayer.
        assertEquals(
            maghrib,
            NudgeSchedule.AfterPrayer(Prayer.MAGHRIB, 0).nextAfter(morning, accra),
        )

        // **Tomorrow is recomputed, not today plus 24 hours.** Mid-August is the worst
        // place to prove that — Accra's sunset barely moves — so this uses the September
        // equinox, where Aladhan has Maghrib at 17:58 on the 20th and 17:57 on the 21st.
        // A naive `plusDays(1)` would give 18:28 for both.
        val sept20 = ZonedDateTime.of(LocalDate.of(2026, 9, 20), LocalTime.of(9, 0), ghana)
        assertEquals(LocalTime.of(18, 28), schedule.nextAfter(sept20, accra)!!.toLocalTime())
        assertEquals(
            LocalTime.of(18, 27),
            schedule.nextAfter(sept20.plusDays(1), accra)!!.toLocalTime(),
        )
    }

    @Test
    fun `an offset large enough to cross midnight lands on the next day, not the morning`() {
        val accra = Coordinates(5.6037, -0.1870)
        val ghana = ZoneId.of("Africa/Accra")
        val morning = ZonedDateTime.of(LocalDate.of(2026, 8, 16), LocalTime.of(9, 0), ghana)

        // Isha is 19:21. Six hours past it is 01:21 the following morning — the case
        // where naive arithmetic wraps back to 01:21 *today*, which is in the past.
        val late = NudgeSchedule.AfterPrayer(Prayer.ISHA, offsetMinutes = 360)
        val next = late.nextAfter(morning, accra)!!
        assertEquals(LocalDate.of(2026, 8, 17), next.toLocalDate())
        assertEquals(LocalTime.of(1, 21), next.toLocalTime())
        assertTrue("a nudge in the past is not a nudge", next.isAfter(morning))
    }

    @Test
    fun `a reminder with nowhere to compute from, and one switched off, both say so`() {
        val ghana = ZoneId.of("Africa/Accra")
        val evening = ZonedDateTime.of(LocalDate.of(2026, 8, 16), LocalTime.of(21, 0), ghana)

        // No coordinates: prayer-based timing is impossible, and the domain refuses to
        // invent a sunset. NudgeScheduler is what turns this into a fixed hour, out loud.
        assertEquals(null, NudgeSchedule.Default.nextAfter(evening, at = null))

        // Off means off, coordinates or not.
        assertEquals(
            null,
            NudgeSchedule.Off.nextAfter(evening, Coordinates(5.6037, -0.1870)),
        )

        // A fixed hour needs no location at all, and one already past rolls to tomorrow.
        val eight = NudgeSchedule.AtClockTime(LocalTime.of(20, 0))
        val next = eight.nextAfter(evening, at = null)!!
        assertEquals(LocalDate.of(2026, 8, 17), next.toLocalDate())
        assertEquals(LocalTime.of(20, 0), next.toLocalTime())
    }

    @Test
    fun `the schedule survives being written down and read back`() {
        val cases = listOf(
            NudgeSchedule.Default,
            NudgeSchedule.AfterPrayer(Prayer.FAJR, 0),
            NudgeSchedule.AfterPrayer(Prayer.ISHA, -15),
            NudgeSchedule.AtClockTime(LocalTime.of(6, 30)),
            NudgeSchedule.Off,
        )
        cases.forEach { assertEquals(it, decodeSchedule(encodeSchedule(it))) }

        // A preference that has never been set, and one that has been corrupted, both
        // give the default rather than throwing on a screen the user cannot get past.
        assertEquals(NudgeSchedule.Default, decodeSchedule(null))
        assertEquals(NudgeSchedule.Default, decodeSchedule("PRAYER NOT_A_PRAYER 30"))
        assertEquals(NudgeSchedule.Default, decodeSchedule("CLOCK half past four"))
        assertEquals(NudgeSchedule.Default, decodeSchedule(""))
    }

    @Test
    fun `the schedule reads as a sentence, and never prints a prayer time`() {
        assertEquals("30 minutes after Maghrib", NudgeSchedule.Default.label())
        assertEquals("At Fajr", NudgeSchedule.AfterPrayer(Prayer.FAJR, 0).label())
        assertEquals("An hour after Isha", NudgeSchedule.AfterPrayer(Prayer.ISHA, 60).label())
        assertEquals("2 hours after Asr", NudgeSchedule.AfterPrayer(Prayer.ASR, 120).label())
        assertEquals("15 minutes before Dhuhr", NudgeSchedule.AfterPrayer(Prayer.DHUHR, -15).label())
        assertEquals("At 8:00 pm", NudgeSchedule.AtClockTime(LocalTime.of(20, 0)).label())
        assertEquals("At 6:05 am", NudgeSchedule.AtClockTime(LocalTime.of(6, 5)).label())
        assertEquals("At 12:00 pm", NudgeSchedule.AtClockTime(LocalTime.of(12, 0)).label())
        assertEquals("At 12:30 am", NudgeSchedule.AtClockTime(LocalTime.of(0, 30)).label())
        assertEquals("No reminder", NudgeSchedule.Off.label())

        // PROFILE.md § 5: prayer times are internal. No label may leak one.
        val anchored = Prayer.entries.map { NudgeSchedule.AfterPrayer(it, 30).label() }
        assertTrue(
            "a label printed a clock time",
            anchored.none { it.contains(":") },
        )
    }

    private fun glyph(verseKey: String, line: Int) =
        Glyph(code = "x", line = line, verseKey = verseKey, isEndMarker = false)
}
