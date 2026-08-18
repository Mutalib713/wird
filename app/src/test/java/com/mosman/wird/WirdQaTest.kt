package com.mosman.wird

import com.mosman.wird.audio.AudioQuality
import com.mosman.wird.data.BookmarkStore
import com.mosman.wird.data.ConversationStore
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.data.decodeSchedule
import com.mosman.wird.data.encodeSchedule
import com.mosman.wird.domain.CompanionAction
import com.mosman.wird.nudge.CommitReceiver
import com.mosman.wird.domain.CompanionBrain
import com.mosman.wird.domain.Coordinates
import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.JuzIndex
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.NudgeSchedule
import com.mosman.wird.domain.Prayer
import com.mosman.wird.domain.PrayerMethod
import com.mosman.wird.domain.PrayerTimes
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.Speaker
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.assignPortion
import com.mosman.wird.domain.label
import com.mosman.wird.domain.linesOn
import com.mosman.wird.domain.listLabel
import com.mosman.wird.domain.nextAfter
import com.mosman.wird.domain.pages
import com.mosman.wird.domain.progressOf
import com.mosman.wird.domain.replyFor
import com.mosman.wird.domain.surahs
import com.mosman.wird.domain.todaysAssignment
import com.mosman.wird.mushaf.Glyph
import com.mosman.wird.mushaf.MushafPage
import com.mosman.wird.ui.reciteLabel
import com.mosman.wird.ui.tapLabel
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

    // ---- 12. Recitation audio: the right ayahs, from the right place ----

    /**
     * The URL convention, which is the part that fails silently.
     *
     * Both CDNs name the file as three digits of surah then three of ayah. Getting the
     * padding wrong does not 404 — it returns a ~678-byte HTML error page with an HTTP
     * 200, which is exactly how a "working" download ends up being nothing. I built that
     * bug by hand on 2026-08-16 and every file came back the same size, which is the tell.
     */
    @Test
    fun `audio urls are zero padded to three digits on both sides`() {
        // Ya-Sin 28 — the first ayah of the page the app was sitting on when measured.
        assertEquals(
            "https://everyayah.com/data/Abu_Bakr_Ash-Shaatree_64kbps/036028.mp3",
            AudioQuality.LIGHT.urlFor(36, 28),
        )
        assertEquals(
            "https://verses.quran.com/Shatri/mp3/036028.mp3",
            AudioQuality.BETTER.urlFor(36, 28),
        )

        // The cases padding gets wrong: a one-digit surah, and a three-digit ayah.
        assertTrue(AudioQuality.LIGHT.urlFor(1, 1).endsWith("/001001.mp3"))
        assertTrue(AudioQuality.LIGHT.urlFor(2, 286).endsWith("/002286.mp3"))
        assertTrue(AudioQuality.BETTER.urlFor(114, 6).endsWith("/114006.mp3"))

        // Every generated name is exactly six digits plus the extension. A single
        // off-by-one in the format string would be caught here rather than by silence.
        (1..114).forEach { s ->
            val name = AudioQuality.LIGHT.urlFor(s, 1).substringAfterLast('/')
            assertEquals("surah $s", 10, name.length)
            assertTrue("surah $s is not all digits", name.removeSuffix(".mp3").all(Char::isDigit))
        }
    }

    /**
     * A half-page portion must fetch — and recite — only the half you were asked to read.
     *
     * This is the same "which lines are lit" rule as the display, reused rather than
     * reimplemented. Task 5f's bug was one rule living in two places and drifting apart,
     * so the audio deliberately derives its verse list from the lit lines instead of
     * asking the API what is on the page.
     */
    @Test
    fun `audio covers exactly the ayahs that are lit, and no more`() {
        // Page 440 as it really is: Fatir's last ayah on lines 1-2, then Ya-Sin.
        val page440 = MushafPage(
            page = 440,
            glyphs = buildList {
                add(glyph("35:45", line = 1))
                add(glyph("35:45", line = 2))
                (1..12).forEach { ayah -> add(glyph("36:$ayah", line = 3 + (ayah - 1) / 2)) }
            },
            surahStarts = mapOf("36:1" to "36"),
            surahName = "Fatir",
            juz = 22,
            bismillahCodes = null,
        )

        fun versesFor(lit: Set<Int>) =
            page440.glyphs.filter { it.line in lit }.map { it.verseKey }.distinct()

        // A reader who chose Ya-Sin starts at line 3, so Fatir's two lines above are not
        // today's — and must not be recited to them.
        val startLine = page440.lineOf(36, 1)!!
        val todaysLines = page440.lines.filter { it >= startLine }.toSet()
        val verses = versesFor(todaysLines)
        assertFalse("Fatir 45 is not today's portion", verses.contains("35:45"))
        assertEquals("36:1", verses.first())
        assertEquals(12, verses.size)

        // Half a page lights half the lines, so it must fetch fewer ayahs than a whole
        // page — the check that catches audio quietly ignoring the portion size.
        val page440Unit = (440 - 1) * Mushaf.UNITS_PER_PAGE
        val topHalf = assignPortion(page440Unit, units = 1).linesOn(440, page440.lines)
        val wholePage = assignPortion(page440Unit, units = 2).linesOn(440, page440.lines)
        assertTrue(
            "half a page should not fetch a whole page of audio",
            versesFor(topHalf).size < versesFor(wholePage).size,
        )

        // Every key is "surah:ayah" and parses — a malformed one would build a URL that
        // 200s with an error page rather than failing.
        verses.forEach { key ->
            val (s, a) = key.split(':').map { it.toInt() }
            assertTrue(s in 1..114)
            assertTrue(a >= 1)
        }
    }

    @Test
    fun `the audio quality choice survives being written down and read back`() {
        AudioQuality.entries.forEach { q ->
            assertEquals(q, AudioQuality.valueOf(q.name))
        }
        // The default protects the data bill; only a deliberate tap moves off it.
        assertEquals(AudioQuality.LIGHT, AudioQuality.entries.first())
        // Both labels say what they cost, because "better" with no number is not a choice.
        AudioQuality.entries.forEach { q ->
            assertTrue("${q.name} should state its size", q.perPageMb.contains("MB"))
        }
    }


    // ---- 13. The companion understands what he actually types ----

    /**
     * Phrasings taken from how he writes, not from how a parser wishes people wrote.
     *
     * This is the check that decides whether the rule-based version is worth shipping
     * before a model: if it handles the sentences he really uses, PLAN task 21 can test
     * the mechanic for the cost of a day rather than three weeks.
     */
    @Test
    fun `the companion understands commitments, refusals and questions`() {
        val evening = LocalTime.of(19, 0)
        fun u(s: String) = CompanionBrain.understand(s, evening)

        // Commitments anchored on a prayer become a real prayer schedule - task 8's
        // scheduler already turns these into alarms that move with the sun.
        val isha = u("after isha") as CompanionAction.CommitTo
        assertEquals(NudgeSchedule.AfterPrayer(Prayer.ISHA, 30), isha.schedule)
        val straight = u("straight after maghrib") as CompanionAction.CommitTo
        assertEquals(NudgeSchedule.AfterPrayer(Prayer.MAGHRIB, 0), straight.schedule)

        // "at 9" said in the EVENING means tonight, not nine tomorrow morning. This is the
        // one that would quietly ruin the feature - a reminder twelve hours late.
        val nine = u("at 9") as CompanionAction.CommitTo
        assertEquals(NudgeSchedule.AtClockTime(LocalTime.of(21, 0)), nine.schedule)
        val morning = CompanionBrain.understand("at 9", LocalTime.of(6, 0))
        assertEquals(
            NudgeSchedule.AtClockTime(LocalTime.of(9, 0)),
            (morning as CompanionAction.CommitTo).schedule,
        )
        assertEquals(
            // 9:30 PM is 21:30. My first assertion here said 9:30 and the parser was
            // right - an explicit pm must beat the time-of-day guess, not be ignored by it.
            NudgeSchedule.AtClockTime(LocalTime.of(21, 30)),
            (u("at 9:30 pm") as CompanionAction.CommitTo).schedule,
        )

        // Relative times are resolved against now.
        val hour = u("in an hour") as CompanionAction.CommitTo
        assertEquals(NudgeSchedule.AtClockTime(LocalTime.of(20, 0)), hour.schedule)

        // Refusals. Never treated as a failure - Sacred Rule 3.
        assertTrue(u("not today") is CompanionAction.NotToday)
        assertTrue(u("cant today, im travelling") is CompanionAction.NotToday)
        assertTrue(u("too tired") is CompanionAction.NotToday)

        // "already read it" is a completion even though it contains "read", which is the
        // collision that makes intent order matter.
        assertTrue(u("i already read it") is CompanionAction.MarkDone)
        assertTrue(u("just finished") is CompanionAction.MarkDone)

        assertTrue(u("how am i doing") is CompanionAction.HowAmIDoing)
        assertTrue(u("where am i") is CompanionAction.WhereAmI)
        assertTrue(u("play it") is CompanionAction.Listen)

        val kahf = u("open al-kahf") as CompanionAction.OpenSurah
        assertEquals(18, kahf.surah.number)
        assertEquals(36, (u("go to yasin") as CompanionAction.OpenSurah).surah.number)

        // **It says it did not understand rather than guessing.** The thing it edits is a
        // record of someone's worship; a wrong guess there is worse than an admission.
        assertTrue(u("what is the weather") is CompanionAction.NotUnderstood)
        assertTrue(u("") is CompanionAction.NotUnderstood)
        assertTrue(u("asdfgh") is CompanionAction.NotUnderstood)
    }

    private fun glyph(verseKey: String, line: Int) =
        Glyph(code = "x", line = line, verseKey = verseKey, isEndMarker = false)
}

/**
 * Checks 21–24 — the conversation.
 *
 * Added 2026-08-18 with [com.mosman.wird.ui.ChatScreen]. These cover the parts that are
 * pure logic and therefore actually testable on the JVM: what gets said back, and what the
 * store keeps. The screen itself was verified by driving it on the emulator, because bubble
 * geometry and keyboard insets are not things a unit test can see — the double-inset bug
 * that clipped the send button was found there and could not have been found here.
 */
class ConversationTest {

    /** Check 21 — a commitment is answered with the commitment, not with "OK". */
    @Test
    fun `it repeats the promise back in your own words`() {
        val action = CompanionBrain.understand("after Isha")
        assertTrue("'after Isha' should be a commitment", action is CompanionAction.CommitTo)
        val reply = replyFor(action, null, "")
        assertTrue(
            "the reply must contain what was promised, not just acknowledge it: $reply",
            reply.contains("after Isha"),
        )
    }

    /**
     * Check 22 — the not-understood reply names what it *does* know.
     *
     * PROFILE.md § 5m: a chat box invites anything and the parser is rules. This is the one
     * mitigation, so it is the one worth asserting. A bare apology is a dead end.
     */
    @Test
    fun `a sentence it cannot parse gets examples, not just an apology`() {
        val action = CompanionBrain.understand("what does this surah mean")
        assertTrue(action is CompanionAction.NotUnderstood)
        val reply = replyFor(action, null, "")
        assertTrue("should offer a concrete example: $reply", reply.contains("after Isha"))
        assertTrue("should point at the shortcuts: $reply", reply.contains("buttons below"))
        assertFalse("should not merely apologise: $reply", reply.trim().endsWith("catch that."))
    }

    /** Check 23 — Sacred Rule 3: a refusal is met with no guilt and no argument. */
    @Test
    fun `saying not today is answered kindly and changes nothing`() {
        val action = CompanionBrain.understand("not today")
        val reply = replyFor(action, null, "").lowercase()
        listOf("streak", "failed", "sure?", "but ", "missed").forEach { banned ->
            assertFalse("Sacred Rule 3 violated by '$banned' in: $reply", reply.contains(banned))
        }
    }

    /** Check 24 — the log keeps order, survives a round trip, and is capped. */
    @Test
    fun `the conversation round-trips and stays bounded`() {
        val dir = java.nio.file.Files.createTempDirectory("wird-chat").toFile()
        try {
            val store = ConversationStore(dir)
            assertTrue("a fresh store is empty", store.all().isEmpty())

            store.say(Speaker.YOU, "after Isha")
            store.say(Speaker.WIRD, "Alright — I'll ask again after Isha.")

            val reread = ConversationStore(dir).all()
            assertEquals("both turns survive a reload", 2, reread.size)
            assertEquals(Speaker.YOU, reread[0].who)
            assertEquals("after Isha", reread[0].text)
            assertEquals("order is kept", Speaker.WIRD, reread[1].who)

            // Blank input is not a turn — the send button is disabled for it, but the store
            // must not depend on a screen to enforce that.
            store.say(Speaker.YOU, "   ")
            assertEquals("whitespace is not a turn", 2, ConversationStore(dir).all().size)

            repeat(200) { store.say(Speaker.YOU, "line $it") }
            val capped = ConversationStore(dir).all()
            assertTrue("the log is capped, was ${capped.size}", capped.size <= 120)
            assertEquals("the newest turn is kept", "line 199", capped.last().text)
        } finally {
            dir.deleteRecursively()
        }
    }
}

/**
 * Check 25 — the reading mode renames things without ever blurring them.
 *
 * PROFILE.md § 5r. The mode is a lens on one mechanic, not a second one, so the thing worth
 * asserting is that **Sacred Rule 6 survives the rename**: whatever the buttons are called,
 * the recording route and the tap route must never end up describing each other.
 */
class ReadingModeTest {

    @Test
    fun `both modes name the recitation and the tap as different things`() {
        ReadingMode.entries.forEach { mode ->
            val recite = reciteLabel(mode)
            val tap = tapLabel(mode)
            assertTrue("$mode: recite label is empty", recite.isNotBlank())
            assertTrue("$mode: tap label is empty", tap.isNotBlank())
            assertTrue("$mode: the two routes must not share a label", recite != tap)
            assertTrue(
                "$mode: only the recording route may say 'recite' — tap said '$tap'",
                !tap.lowercase().contains("recite"),
            )
        }
    }

    @Test
    fun `memorising changes the words and reading keeps the originals`() {
        assertEquals("Recite it out loud", reciteLabel(ReadingMode.READING))
        assertEquals("I read it", tapLabel(ReadingMode.READING))
        assertEquals("Recite from memory", reciteLabel(ReadingMode.MEMORISING))
        // "I read it" would be wrong for someone deliberately not looking at the page.
        assertEquals("I revised it", tapLabel(ReadingMode.MEMORISING))
    }
}

/**
 * Checks 26–28 — the surah index and its juz′ grouping.
 *
 * The data is generated from the Quran.com API rather than typed, so what needs asserting is
 * not the values themselves but that the *generation* produced something whole: 114 complete
 * rows, thirty juz′ that cover the mushaf with no gap, and a grouping that reproduces the
 * reference Mutalib actually sent.
 */
class SurahIndexTest {

    /** Check 26 — every row is complete. A blank meaning would render as "Al-Kahf ()". */
    @Test
    fun `all 114 surahs carry a name, a meaning, verses and a sane page range`() {
        assertEquals(114, SurahIndex.all.size)
        SurahIndex.all.forEachIndexed { i, s ->
            assertEquals("numbers must run 1..114 in order", i + 1, s.number)
            assertTrue("${s.number} has no name", s.name.isNotBlank())
            assertTrue("${s.number} has no meaning", s.meaning.isNotBlank())
            assertTrue("${s.number} has ${s.verses} verses", s.verses > 0)
            assertTrue("${s.number} starts on page ${s.firstPage}", s.firstPage in 1..Mushaf.PAGES)
            assertTrue("${s.number} ends before it starts", s.lastPage >= s.firstPage)
        }
        assertEquals("An-Nisa should read as the reference does", "The Women", SurahIndex.byNumber(4)!!.meaning)
        assertEquals(176, SurahIndex.byNumber(4)!!.verses)
    }

    /**
     * Check 26b — the label never repeats itself.
     *
     * Six surahs are named for a person or a word, and the API's translated name is that
     * same word: "Hud (Hud)". Caught by looking at the built screen, not by reading the data.
     */
    @Test
    fun `a meaning that only repeats the name is dropped`() {
        assertEquals("An-Nisa (The Women)", SurahIndex.byNumber(4)!!.listLabel())
        listOf(11 to "Hud", 20 to "Taha", 31 to "Luqman",
               36 to "Ya-Sin", 47 to "Muhammad", 106 to "Quraysh").forEach { (n, expected) ->
            assertEquals("surah $n should not stutter", expected, SurahIndex.byNumber(n)!!.listLabel())
        }
        // And nothing else lost its meaning on the way past.
        assertTrue(SurahIndex.all.count { it.listLabel().contains("(") } >= 105)
    }

    /** Check 27 — the thirty juz′ cover every page, in order, with no gap. */
    @Test
    fun `juz cover the whole mushaf`() {
        assertEquals(30, JuzIndex.all.size)
        assertEquals("juz 1 starts at the first page", 1, JuzIndex.all.first().firstPage)
        JuzIndex.all.zipWithNext { a, b ->
            assertTrue("juz ${b.number} must start after juz ${a.number}", b.firstPage > a.firstPage)
        }
        // Every page resolves, including the last one.
        (1..Mushaf.PAGES).forEach { page ->
            val j = JuzIndex.of(page)
            assertTrue("page $page landed in juz ${j.number}", j.number in 1..30)
            assertTrue("page $page is before its own juz start", page >= j.firstPage)
        }
    }

    /**
     * Check 27b — exactly two juz' have no surah starting in them, and the list must still
     * show all thirty.
     *
     * Found on the emulator 2026-08-18: grouping by starting juz' alone produced a list that
     * ran 1, 3, 4, 6. That is arithmetically right — Al-Baqarah spans juz 1-3 and An-Nisa
     * spans 4-5 — and it read as a bug. This pins the two so a data change cannot make the
     * gap silently bigger.
     */
    @Test
    fun `only juz 2 and 5 have no surah beginning in them`() {
        val startsIn = SurahIndex.all.groupBy { JuzIndex.of(it.firstPage).number }
        val empty = (1..30).filterNot { startsIn.containsKey(it) }
        assertEquals("the two known gaps, and no others", listOf(2, 5), empty)

        // And each gap must be able to name what you are still inside, or the row is blank.
        empty.forEach { n ->
            val juz = JuzIndex.all.first { it.number == n }
            val ongoing = SurahIndex.on(juz.firstPage)
            assertTrue("juz $n must land inside some surah", ongoing.isNotEmpty())
        }
        assertEquals("Al-Baqarah", SurahIndex.on(JuzIndex.all.first { it.number == 2 }.firstPage).last().name)
        assertEquals("An-Nisa", SurahIndex.on(JuzIndex.all.first { it.number == 5 }.firstPage).last().name)
    }

    /**
     * Check 28 — the grouping reproduces Mutalib's reference screenshot.
     *
     * From his Quran for Android screenshot, 2026-08-18: `Juz' 18 · 342` with Al-Mu'minun,
     * An-Nur and Al-Furqan under it; `Juz' 19 · 362` with Ash-Shu'ara and An-Naml; `Juz' 20 ·
     * 382` with Al-Qasas and Al-'Ankabut. If the grouping rule is ever changed, this is what
     * says so.
     */
    @Test
    fun `surahs group under the juz their first page falls in`() {
        assertEquals(342, JuzIndex.all.first { it.number == 18 }.firstPage)
        assertEquals(362, JuzIndex.all.first { it.number == 19 }.firstPage)
        assertEquals(382, JuzIndex.all.first { it.number == 20 }.firstPage)

        fun juzOf(surah: Int) = JuzIndex.of(SurahIndex.byNumber(surah)!!.firstPage).number
        listOf(23, 24, 25).forEach { assertEquals("surah $it belongs to juz 18", 18, juzOf(it)) }
        listOf(26, 27).forEach { assertEquals("surah $it belongs to juz 19", 19, juzOf(it)) }
        listOf(28, 29).forEach { assertEquals("surah $it belongs to juz 20", 20, juzOf(it)) }
    }
}

/**
 * Checks 29–31 — bookmarks.
 *
 * The store is the whole feature: one toggle, newest-first order, and survival across a
 * restart. All three are JVM-testable now that `org.json` is a real implementation on the
 * test classpath rather than a throwing stub.
 */
class BookmarkTest {

    private fun tempStore(): Pair<BookmarkStore, java.io.File> {
        val dir = java.nio.file.Files.createTempDirectory("wird-marks").toFile()
        return BookmarkStore(dir) to dir
    }

    /** Check 29 — one control does both jobs, and it reports which it did. */
    @Test
    fun `toggle saves then unsaves, and says which`() {
        val (store, dir) = tempStore()
        try {
            assertTrue("a fresh store is empty", store.all().isEmpty())
            assertFalse("nothing is saved yet", store.has("18:10"))

            assertTrue("first toggle saves", store.toggle("18:10"))
            assertTrue(store.has("18:10"))
            assertEquals(1, store.all().size)

            assertFalse("second toggle removes", store.toggle("18:10"))
            assertFalse(store.has("18:10"))
            assertTrue(store.all().isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    /** Check 30 — newest first, because that is the only order anyone wants. */
    @Test
    fun `bookmarks come back newest first`() {
        val (store, dir) = tempStore()
        try {
            val t = java.time.LocalDateTime.of(2026, 8, 18, 9, 0)
            store.toggle("2:255", t)
            store.toggle("18:10", t.plusHours(1))
            store.toggle("36:1", t.plusHours(2))

            val order = store.all().map { it.verseKey }
            assertEquals(listOf("36:1", "18:10", "2:255"), order)
        } finally {
            dir.deleteRecursively()
        }
    }

    /** Check 31 — it survives a restart, since a bookmark that forgets itself is useless. */
    @Test
    fun `bookmarks survive a reload`() {
        val (store, dir) = tempStore()
        try {
            store.toggle("18:10")
            store.toggle("36:1")
            val reread = BookmarkStore(dir).all()
            assertEquals(2, reread.size)
            assertTrue(reread.any { it.verseKey == "18:10" })
            // And the key is all that is stored - never the words. Sacred Rule 2.
            assertTrue(
                "a bookmark must not carry Qur'anic text",
                reread.all { it.verseKey.matches(Regex("""\d+:\d+""")) },
            )
        } finally {
            dir.deleteRecursively()
        }
    }
}

/**
 * Checks 35–36 — the notification's reply buttons. **PLAN task 21.**
 *
 * These exist because of a bug caught while wiring them: PLAN lists four buttons and
 * `CompanionBrain` understands only three. Bare "Tonight" parses to `NotUnderstood`, so that
 * button would have done nothing at all and said nothing about it. A button whose phrase the
 * parser cannot read is the worst kind of dead control — it looks like it worked.
 */
class NotificationRepliesTest {

    /** Check 35 — every offered reply must actually do something. */
    @Test
    fun `every notification reply is understood by the parser`() {
        assertTrue("there must be replies to offer", CommitReceiver.REPLIES.isNotEmpty())
        assertTrue(
            "Android shows three actions; more would be hidden",
            CommitReceiver.REPLIES.size <= 3,
        )
        CommitReceiver.REPLIES.forEach { phrase ->
            val action = CompanionBrain.understand(phrase)
            assertFalse(
                "\"$phrase\" is offered as a button but parses to NotUnderstood",
                action is CompanionAction.NotUnderstood,
            )
        }
    }

    /**
     * Check 36 — the three replies mean the three different things they should.
     *
     * Two commitments that resolved to the same schedule would be two buttons doing one job,
     * which is how "Tonight" and "After Isha" collapsed in the first place.
     */
    @Test
    fun `the replies are three genuinely different answers`() {
        val evening = java.time.LocalTime.of(19, 0)
        val actions = CommitReceiver.REPLIES.map { CompanionBrain.understand(it, evening) }

        val commitments = actions.filterIsInstance<CompanionAction.CommitTo>()
        assertEquals("two of the three should be commitments", 2, commitments.size)
        assertEquals(
            "and they must not resolve to the same schedule",
            2,
            commitments.map { it.schedule }.distinct().size,
        )
        assertTrue(
            "one of the three must be the refusal",
            actions.any { it is CompanionAction.NotToday },
        )
    }
}
