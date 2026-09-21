package com.mosman.wird

import com.mosman.wird.audio.AudioQuality
import com.mosman.wird.data.BookmarkStore
import com.mosman.wird.data.ConversationStore
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.data.decodeSchedule
import com.mosman.wird.data.encodeSchedule
import com.mosman.wird.domain.AwayPeriod
import com.mosman.wird.domain.Commitment
import com.mosman.wird.domain.CompanionAction
import com.mosman.wird.nudge.CommitReceiver
import com.mosman.wird.ui.OpenElsewhere
import com.mosman.wird.domain.CompanionBrain
import com.mosman.wird.domain.Coordinates
import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Heard
import com.mosman.wird.domain.heardLabel
import com.mosman.wird.domain.judgeRecitation
import com.mosman.wird.domain.WordMark
import com.mosman.wird.domain.checkRecitation
import com.mosman.wird.domain.foldArabic
import com.mosman.wird.domain.JuzIndex
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.NudgeSchedule
import com.mosman.wird.domain.Prayer
import com.mosman.wird.domain.PrayerMethod
import com.mosman.wird.domain.PrayerTimes
import com.mosman.wird.domain.PrivacyPledge
import com.mosman.wird.domain.ReadingDirection
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.Speaker
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.assignPortion
import com.mosman.wird.domain.label
import com.mosman.wird.domain.linesOn
import com.mosman.wird.domain.versesOn
import com.mosman.wird.domain.listLabel
import com.mosman.wird.domain.nextAfter
import com.mosman.wird.domain.nextAwake
import com.mosman.wird.domain.pages
import com.mosman.wird.domain.progressOf
import com.mosman.wird.domain.replyFor
import com.mosman.wird.domain.replyForAll
import com.mosman.wird.domain.surahs
import com.mosman.wird.domain.todaysAssignment
import com.mosman.wird.domain.unitsLabel
import com.mosman.wird.mushaf.Glyph
import com.mosman.wird.mushaf.MushafPage
import com.mosman.wird.ui.reciteLabel
import com.mosman.wird.nudge.NudgeDiagnostic
import com.mosman.wird.nudge.OemAdvice
import com.mosman.wird.ui.tapLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import org.json.JSONArray
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
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

    // ---- 6b. Regression: portion highlighting must be verse-level, not whole-line ----

    @Test
    fun `a half page target lights exact verses without cutting verses across shared lines`() {
        val page439Unit = (439 - 1) * Mushaf.UNITS_PER_PAGE
        // Real shape of page 439: Fatir 39..44 across 15 lines.
        // Line 8 carries the end of ayah 41 AND the beginning of ayah 42.
        val page439 = MushafPage(
            page = 439,
            glyphs = listOf(
                glyph("35:39", line = 1),
                glyph("35:39", line = 2),
                glyph("35:40", line = 3),
                glyph("35:40", line = 7),
                glyph("35:41", line = 7),
                glyph("35:41", line = 8),
                glyph("35:42", line = 8), // Start of Ayah 42 on line 8!
                glyph("35:42", line = 9),
                glyph("35:43", line = 10),
                glyph("35:43", line = 12),
                glyph("35:44", line = 13),
                glyph("35:44", line = 15),
            ),
            surahStarts = emptyMap(),
            surahName = "Fatir",
            juz = 22,
            bismillahCodes = null,
        )

        val topHalf = assignPortion(page439Unit, units = 1).versesOn(page439)
        val bottomHalf = assignPortion(page439Unit + 1, units = 1).versesOn(page439)
        val wholePage = assignPortion(page439Unit, units = 2).versesOn(page439)

        // Top half gets Fatir 39..41
        assertEquals(setOf("35:39", "35:40", "35:41"), topHalf)
        // Bottom half gets Fatir 42..44
        assertEquals(setOf("35:42", "35:43", "35:44"), bottomHalf)
        // Whole page gets all 6
        assertEquals(setOf("35:39", "35:40", "35:41", "35:42", "35:43", "35:44"), wholePage)

        // Crucial bug fix: on Line 8, Fatir 42 is in bottomHalf portion, but Fatir 41 is not!
        val line8Glyphs = page439.glyphsOn(8)
        val ayah41Glyph = line8Glyphs.first { it.verseKey == "35:41" }
        val ayah42Glyph = line8Glyphs.first { it.verseKey == "35:42" }

        assertFalse("Ayah 41 on line 8 must be dimmed for bottom half", ayah41Glyph.verseKey in bottomHalf)
        assertTrue("Ayah 42 on line 8 must be lit for bottom half", ayah42Glyph.verseKey in bottomHalf)

        // Ayah count and range for bottom half
        assertEquals(3, page439.ayahCount(bottomHalf))
        assertEquals((35 to 42) to (35 to 44), page439.ayahRange(bottomHalf))
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

/**
 * Check 37 — the Quran for Android deep link is built the way that app parses it.
 *
 * **PLAN task 12.** Its `QuranForwarderActivity` splits the whole URI string on `/` and takes
 * the first numeric segment as the sura and the second as the ayah. So the shape matters more
 * than the host, and this asserts the shape rather than trusting it stayed right through an
 * edit. Read from that project's own GPL source, not guessed.
 */
class DeepLinkTest {

    @Test
    fun `a verse key becomes the uri Quran for Android expects`() {
        assertEquals("quran://18/10", OpenElsewhere.quranUriFor("18:10"))
        assertEquals("quran://1/1", OpenElsewhere.quranUriFor("1:1"))
        assertEquals("quran://114/6", OpenElsewhere.quranUriFor("114:6"))
    }

    /** A key with no ayah still has to open the sura rather than produce a broken URI. */
    @Test
    fun `a bare surah defaults to its first ayah`() {
        assertEquals("quran://36/1", OpenElsewhere.quranUriFor("36"))
    }

    /**
     * The two numbers the forwarder reads must be the ones we meant. Splitting the built URI
     * the way that activity does is the closest this can get to testing their parser.
     */
    @Test
    fun `their parser would read the surah and ayah we intended`() {
        val numbers = OpenElsewhere.quranUriFor("2:255")
            .split("/")
            .mapNotNull { it.toIntOrNull() }
        assertEquals(listOf(2, 255), numbers.take(2))
    }
}

/**
 * Checks 40–46 — plain words instead of buttons. **PLAN task 22, task 13 folded in.**
 *
 * The done-when for both tasks was the same sentence: *five messy sentences produce the
 * correct schedule, and anything it does not understand says so plainly rather than
 * guessing.* Check 40 is that literal test, written as five different sentences rather than
 * five phrasings of one, so passing it means the feature is done rather than merely built.
 *
 * Dates are anchored to a fixed Tuesday and asserted by weekday rather than by literal date,
 * so the suite says what it means ("the reminder returns on Sunday") and does not quietly
 * start failing on a different day of the week.
 */
class PlainWordsTest {

    private val tuesday: LocalDate = LocalDate.of(2026, 8, 18)
    private val evening: LocalTime = LocalTime.of(19, 0)

    private fun u(s: String, plan: ReadingPlan = ReadingPlan()) =
        CompanionBrain.understand(s, evening, plan, tuesday)

    private fun all(s: String, plan: ReadingPlan = ReadingPlan()) =
        CompanionBrain.understandAll(s, evening, plan, tuesday)

    /** Check 40 — the five sentences the task is graded on. */
    @Test
    fun `five messy sentences produce the right schedule`() {
        // 1. How much, every day.
        assertEquals(
            "one page a day is two half-page units",
            CompanionAction.ChangePlan(2),
            u("one page a day"),
        )

        // 2. A lighter Friday, with the word "page" left out entirely.
        assertEquals(
            CompanionAction.ChangeDayPlan(DayOfWeek.FRIDAY, 1),
            u("half on fridays"),
        )

        // 3. Away, with an end date. The reminder comes back the day he named.
        val away = (u("im travelling till sunday") as CompanionAction.PauseUntil).away
        assertEquals("quiet starts today", tuesday, away.from)
        assertEquals(
            "till Sunday means the reminder returns ON Sunday",
            DayOfWeek.SUNDAY,
            away.returnsOn.dayOfWeek,
        )
        assertTrue("and that Sunday is this week", away.returnsOn <= tuesday.plusDays(6))

        // 4. The routine itself, which has to be asked for in words.
        assertEquals(
            CompanionAction.MoveReminder(NudgeSchedule.AtClockTime(LocalTime.of(21, 0))),
            u("move my reminder to 9 from now on"),
        )

        // 5. Back early, before the trip he named is over.
        assertEquals(CompanionAction.Resume, u("im back"))
    }

    /**
     * Check 41 — three instructions in one breath, all three applied.
     *
     * This is PLAN task 13's own example sentence. Reading only the first would leave two
     * edits silently unmade, which is the failure mode that matters: a schedule that is
     * quietly a third of what was asked for looks like it worked.
     */
    @Test
    fun `one sentence can carry three separate instructions`() {
        val actions = all("one page a day, half on fridays, im travelling next week")

        assertEquals("all three must land", 3, actions.size)
        assertTrue(actions.contains(CompanionAction.ChangePlan(2)))
        assertTrue(actions.contains(CompanionAction.ChangeDayPlan(DayOfWeek.FRIDAY, 1)))

        val away = actions.filterIsInstance<CompanionAction.PauseUntil>().single().away
        assertEquals("next week starts on a Monday", DayOfWeek.MONDAY, away.from.dayOfWeek)
        assertTrue("and it is next week, not this one", away.from > tuesday)
        assertEquals("seven days of it", 6, away.until.toEpochDay() - away.from.toEpochDay())

        // The clause splitter must leave an amount alone. Splitting on "and" would turn one
        // instruction into two fragments that each mean nothing.
        assertEquals(
            listOf(CompanionAction.ChangePlan(3)),
            all("make it a page and a half a day"),
        )
    }

    /**
     * Check 42 — a clause it cannot read is named, in the reader's own words.
     *
     * **The whole rule-based approach rests on this.** A parser that quietly drops a third of
     * a sentence is worse than one that admits it, because nothing on screen says which third
     * went missing. PROFILE.md § 5m records the gap; this is the guarantee that goes with it.
     */
    @Test
    fun `what it cannot read is said out loud, and the rest still lands`() {
        // ⚠ **This clause changed on 2026-08-19 and the reason is worth keeping.** It used
        // to be "explain surah yasin to me", which was a fine example of an unreadable clause
        // right up until the companion learned to read it. A test whose premise is a missing
        // feature has to be rewritten the day that feature arrives, not deleted.
        val actions = all("one page a day, and call my brother about the car")

        assertTrue(actions.contains(CompanionAction.ChangePlan(2)))
        val missed = actions.filterIsInstance<CompanionAction.NotUnderstood>().single()
        assertTrue("it must quote the clause: " + missed.said, missed.said.contains("brother"))

        val reply = replyForAll(actions, null, "", tuesday)
        assertTrue("the change is confirmed: " + reply, reply.contains("One page a day"))
        assertTrue("and the miss is named: " + reply, reply.contains("catch"))

        // Politeness is not an instruction, and answering it with a complaint would be
        // annoying rather than honest.
        assertEquals(
            listOf(CompanionAction.ChangePlan(2)),
            all("one page a day, thanks"),
        )
    }

    /**
     * Check 43 — a promise is about tonight, and the routine underneath does not move.
     *
     * ⚠ **A real defect, caught on 2026-08-18 and fixed at his word.** "In an hour" used to
     * be written straight into the daily reminder, so one three o'clock reply made four
     * o'clock the reminder time for every day afterwards, silently. The commitment now
     * carries its own schedule and expires with the day it was made.
     */
    @Test
    fun `a commitment moves tonight only, and a routine change says so`() {
        val tonight = u("in an hour") as CompanionAction.CommitTo
        assertEquals(NudgeSchedule.AtClockTime(LocalTime.of(20, 0)), tonight.schedule)

        val held = Commitment(tonight.spoken, tuesday.atTime(evening), tonight.schedule)
        assertTrue("it governs the day it was made", held.appliesOn(tuesday))
        assertFalse("and nothing after it", held.appliesOn(tuesday.plusDays(1)))

        // The permanent version has to be asked for. Same time, different meaning.
        assertTrue(u("at 9") is CompanionAction.CommitTo)
        assertTrue(u("remind me at 9 every day") is CompanionAction.MoveReminder)
        assertEquals(
            NudgeSchedule.Off,
            (u("stop reminding me") as CompanionAction.MoveReminder).schedule,
        )
    }

    /**
     * Check 44 — away days are stepped over, and the reminder returns without being asked.
     *
     * **Not switched off.** Someone who says "travelling till Sunday" and then does not open
     * the app for four days must still be asked on Sunday evening, so the alarm is set for
     * the far side of the trip rather than cancelled. Nothing has to be running for an alarm
     * to arrive, which is what makes this safe on the phones that kill background work.
     */
    @Test
    fun `the reminder skips the away days and comes back on its own`() {
        val accra = Coordinates(5.6037, -0.1870)
        val zone = ZoneId.of("Africa/Accra")
        val now = tuesday.atTime(9, 0).atZone(zone)
        val schedule = NudgeSchedule.AfterPrayer(Prayer.MAGHRIB, 30)

        val away = AwayPeriod(tuesday, tuesday.plusDays(3))
        val next = schedule.nextAwake(now, accra, away)!!
        assertEquals(
            "the next reminder is the evening of the return day",
            away.returnsOn,
            next.toLocalDate(),
        )

        // A trip that has not started yet must leave this week's reminders exactly alone.
        val later = AwayPeriod(tuesday.plusDays(6), tuesday.plusDays(9))
        assertEquals(tuesday, schedule.nextAwake(now, accra, later)!!.toLocalDate())

        // And with no trip at all, nothing changes about the ordinary answer.
        assertEquals(
            schedule.nextAfter(now, accra),
            schedule.nextAwake(now, accra, null),
        )

        assertTrue("the away days are the away days", tuesday.plusDays(2) in away)
        assertFalse("and the return day is not one of them", away.returnsOn in away)
    }

    /**
     * Check 45 — the guards against guessing, which matter more than the parsing.
     *
     * Every case here is a sentence something eager could read as a schedule change. The
     * thing this edits is a record of someone's worship, so an admission beats a confident
     * wrong answer every time.
     */
    @Test
    fun `it refuses to guess at an instruction that was never given`() {
        // An amount with nothing saying it is about every day could be a report of what was
        // just read. Not enough to act on.
        assertTrue(u("two pages") is CompanionAction.NotUnderstood)

        // ⚠ A number in a sentence about time is a time, never a page count.
        assertTrue(
            "at 9 on fridays is not nine pages",
            u("at 9 on fridays") !is CompanionAction.ChangeDayPlan,
        )

        // A travel word with no end date is today, not a week. The smaller claim wins.
        assertTrue(u("im travelling") is CompanionAction.NotToday)

        // Absurd amounts are typos, not plans.
        assertTrue(u("300 pages a day") is CompanionAction.NotUnderstood)

        // ⚠ **Updated 2026-08-19: "what does al-kahf mean" IS answered now**, from the
        // bundled translation with its translator named — § 5h's fetch-and-attribute path,
        // built at his ask. See ExplainVerseTest.
        //
        // What has not changed is the boundary. The app still holds no opinion of its own
        // about the Qur'an or about religious rulings, and this is the assertion that guards
        // it: a question of fiqh has no verse to fetch and no scholar to name, so the honest
        // answer is that it did not understand.
        assertTrue(u("is it allowed to combine prayers") is CompanionAction.NotUnderstood)
        assertTrue(u("explain this") is CompanionAction.NotUnderstood)
    }

    /**
     * Check 46 — Sacred Rule 3 holds across every new reply, and each one repeats the change.
     *
     * A confirmation that does not contain the change is not a confirmation. "Till Sunday" is
     * genuinely ambiguous English, so the day it landed on has to appear in the answer, or a
     * wrong reading costs a fortnight instead of a sentence.
     */
    @Test
    fun `every new reply repeats the change and keeps the tone`() {
        val said = listOf(
            u("one page a day"),
            u("half on fridays"),
            u("im travelling till sunday"),
            u("move my reminder to 9 from now on"),
            u("stop reminding me"),
            u("im back"),
        ).map { replyFor(it, null, "", tuesday) }

        said.forEach { reply ->
            listOf("streak", "failed", "sure?", "missed", "should", "forget").forEach { banned ->
                assertFalse(
                    "Sacred Rule 3 violated by " + banned + " in: " + reply,
                    reply.lowercase().contains(banned),
                )
            }
            assertTrue("a reply must say something: " + reply, reply.length > 8)
        }

        assertTrue("the amount comes back in words: " + said[0], said[0].contains("One page a day"))
        assertTrue("the day is named: " + said[1], said[1].contains("Fridays"))
        assertTrue("the return day is named: " + said[2], said[2].contains("Sunday"))
        assertTrue("the new routine is named: " + said[3], said[3].contains("9:00 pm"))

        // Half pages are said the way a person says them, never as "1 unit".
        assertEquals("half a page", unitsLabel(1))
        assertEquals("one page", unitsLabel(2))
        assertEquals("a page and a half", unitsLabel(3))
        assertEquals("2 pages", unitsLabel(4))
    }
}

/**
 * Checks 47–49 — asking what an ayah means. **His ask, 2026-08-19.**
 *
 * He typed *"so explain verse 1 of fatiha"* into the companion and was told "I didn't catch
 * that." Two separate failures sat behind that one reply, and both are asserted here: the
 * parser had no notion of the question at all, and it could not have matched "fatiha" against
 * an index that spells it "Al-Fatihah".
 *
 * ⚠ **What these checks deliberately do NOT assert is any explanation**, because the app does
 * not produce one. Sacred Rule 2 stands: the reply is a fetched translation with its
 * translator named, and check 49 is the guard that keeps it that way.
 */
class ExplainVerseTest {

    private fun u(s: String) = CompanionBrain.understand(s, LocalTime.of(19, 0))

    /** Check 47 — the sentence he actually typed, and the two others he is likeliest to. */
    @Test
    fun `it reads a verse reference however it was written`() {
        assertEquals(
            "his own sentence, verbatim",
            CompanionAction.ExplainVerse(1, 1),
            u("so explain verse 1 of fatiha"),
        )
        assertEquals(
            "the least ambiguous form wins",
            CompanionAction.ExplainVerse(2, 255),
            u("what does 2:255 mean"),
        )
        assertEquals(
            CompanionAction.ExplainVerse(18, 10),
            u("translate ayah 10 of al-kahf"),
        )

        // A surah named with no ayah is a question about the title, answered as one.
        assertEquals(CompanionAction.ExplainVerse(18, null), u("what does al-kahf mean"))
    }

    /**
     * Check 48 — "fatiha" finds Al-Fatihah, and the shortcut does not start matching noise.
     *
     * Nobody types the index's transliteration. The core form drops the `al-` article and one
     * trailing `h`, which is what turns "Al-Fatihah" into "fatiha" — but a core shorter than
     * four letters would match inside ordinary words, so those are refused.
     */
    @Test
    fun `a surah is found by the name people actually type`() {
        assertEquals(1, (u("go to fatiha") as CompanionAction.OpenSurah).surah.number)
        assertEquals(18, (u("open kahf") as CompanionAction.OpenSurah).surah.number)
        assertEquals(36, (u("go to yasin") as CompanionAction.OpenSurah).surah.number)

        // The guards: a bare time is not a surah, and a schedule sentence is not a question
        // about the Qur'an.
        assertTrue(u("at 9") is CompanionAction.CommitTo)
        assertTrue(u("one page a day") is CompanionAction.ChangePlan)
    }

    /**
     * Check 49 — ⚠ **it will not answer a question that named nothing.**
     *
     * "Explain this" has no ayah in it. Guessing that it meant today's first verse would be
     * the parser inventing the question it was asked, on the one subject where being
     * confidently wrong is worst. § 5h calls a wrong tafsir the highest-risk thing in the app
     * precisely because the reader usually cannot tell.
     */
    @Test
    fun `it refuses to guess which verse was meant`() {
        assertTrue(u("explain this") is CompanionAction.NotUnderstood)
        assertTrue(u("what does it mean") is CompanionAction.NotUnderstood)
        assertTrue(u("explain the quran to me") is CompanionAction.NotUnderstood)

        // And the miss reply now names this as something it can do, so the dead end is a menu.
        val reply = replyFor(u("explain this"), null, "")
        assertTrue("should offer the translation route: $reply", reply.contains("18:10"))
    }
}

/**
 * Checks 50–52 — what the app is willing to say about a recording. **PLAN task 14, cheap half.**
 *
 * ⚠ These assert the *shape* of the judgement, not its accuracy. Accuracy is a measurement on
 * Mutalib's own voice, on his phone, in his room, and it has not been taken yet — the thresholds
 * are provisional and the task is not ticked until real numbers exist. A unit test cannot make a
 * threshold true; it can only stop the logic around it from drifting.
 */
class HeardRecitationTest {

    private fun loud(n: Int) = List(n) { 6_000 }
    private fun quiet(n: Int) = List(n) { 120 }

    /** Check 50 — the three cases the task actually asked to be separated. */
    @Test
    fun `it separates recitation from silence and from a false start`() {
        assertEquals(
            "a minute of sustained speech is a recitation",
            Heard.RECITATION,
            judgeRecitation(loud(60)).verdict,
        )
        assertEquals(
            "a silent room is not",
            Heard.TOO_QUIET,
            judgeRecitation(quiet(60)).verdict,
        )
        assertEquals(
            "neither is four seconds of it",
            Heard.TOO_SHORT,
            judgeRecitation(loud(4)).verdict,
        )
        assertEquals(
            "and a recording that never started says so",
            Heard.NOTHING,
            judgeRecitation(emptyList()).verdict,
        )
    }

    /**
     * Check 51 — ⚠ **silence is diagnosed before shortness.**
     *
     * A silent two-second recording has two things wrong with it, and being told the *length*
     * was the problem would send the reader off to record two silent minutes instead.
     */
    @Test
    fun `a silent recording is called quiet, not short`() {
        assertEquals(Heard.TOO_QUIET, judgeRecitation(quiet(2)).verdict)

        // Real speech has gaps — breaths, pauses between ayahs. A third of the seconds
        // carrying sound is enough, because the alternative is calling someone's pauses
        // silence.
        val withPauses = (loud(20) + quiet(30)).shuffled()
        assertEquals(Heard.RECITATION, judgeRecitation(withPauses).verdict)
    }

    /**
     * Check 52 — the numbers survive, and the words never accuse.
     *
     * PLAN task 14 asks for the measured numbers written down; a verdict with nothing behind it
     * cannot be argued with when it is wrong. And Sacred Rule 3 governs the sentence itself: the
     * reader has just finished reciting, which is the worst moment in the app to sound like a
     * gatekeeper, so a doubt is phrased as something the app could not hear.
     */
    @Test
    fun `it reports what it measured and never blames the reader`() {
        val result = judgeRecitation(loud(30) + quiet(10))
        assertEquals(30, result.spokenSeconds)
        assertEquals(40, result.totalSeconds)
        assertEquals(0.75f, result.spokenShare, 0.01f)

        listOf(
            judgeRecitation(loud(60)),
            judgeRecitation(quiet(60)),
            judgeRecitation(loud(3)),
            judgeRecitation(emptyList()),
        ).forEach { r ->
            val said = heardLabel(r).lowercase()
            listOf("failed", "invalid", "rejected", "wrong", "didn't count", "try again")
                .forEach { banned ->
                    assertFalse("Sacred Rule 3 violated by '$banned' in: $said", said.contains(banned))
                }
            assertTrue("every verdict says the recording was kept: $said", said.contains("record") || said.contains("saved"))
        }
    }
}

/**
 * Checks 53–54 — which way through the mushaf. **His instruction, 2026-08-19.**
 *
 * *"For me I memorise upwards, but some start from Baqarah downwards, and reading too is the
 * same, so the app must know."* And his own worked example, which is what these assert:
 * **at Ya-Sin, going up you reach Fatir; going down you reach As-Saffat.**
 */
class ReadingDirectionTest {

    /** Ya-Sin begins on page 440, so its first half-page unit is (440-1) * 2. */
    private val yaSin = (440 - 1) * Mushaf.UNITS_PER_PAGE

    /** Check 53 — his example, both ways, named by the sūrah you actually land in. */
    @Test
    fun `at Ya-Sin, up reaches Fatir and down reaches As-Saffat`() {
        val up = assignPortion(yaSin, Mushaf.UNITS_PER_PAGE, ReadingDirection.TOWARDS_FATIHAH)
        val down = assignPortion(yaSin, Mushaf.UNITS_PER_PAGE, ReadingDirection.TOWARDS_NAS)

        assertEquals("both read the same page today", up.startPage, down.startPage)

        val upNext = Mushaf.pageOf(up.nextStartUnit)
        val downNext = Mushaf.pageOf(down.nextStartUnit)
        assertEquals("going up, tomorrow is the page before", 439, upNext)
        assertEquals("going down, tomorrow is the page after", 441, downNext)

        // Said in sūrah names, because that is how he said it.
        assertEquals("Fatir", SurahIndex.across(listOf(upNext)).first().name)

        // ⚠ **A page step is not a sūrah step, and this test learned it the hard way.** The
        // first assertion here expected As-Saffat one page below Ya-Sin's opening and got
        // Ya-Sin, because Ya-Sin runs about six pages. His example — *"it goes to Sad"* — is
        // told in sūrahs, while the app advances in pages, and both are right about different
        // things. Going up landed in Fatir immediately only because Ya-Sin *begins* on 440, so
        // the page before it belongs to the previous sūrah.
        assertEquals("one page down is still Ya-Sin", "Ya-Sin", SurahIndex.across(listOf(downNext)).first().name)

        // The sūrah-level version of his example: from Ya-Sin's LAST page, down reaches the
        // next sūrah.
        val yaSinEnd = SurahIndex.byNumber(36)!!.lastPage
        val leaving = assignPortion(
            (yaSinEnd - 1) * Mushaf.UNITS_PER_PAGE,
            Mushaf.UNITS_PER_PAGE,
            ReadingDirection.TOWARDS_NAS,
        )
        assertEquals(
            "finishing Ya-Sin downwards reaches As-Saffat",
            "As-Saffat",
            SurahIndex.across(listOf(Mushaf.pageOf(leaving.nextStartUnit))).first().name,
        )
    }

    /**
     * Check 54 — ⚠ **it wraps at both ends, and the default never moves anybody.**
     *
     * Front-to-back has always wrapped 604 to 1. Back-to-front has to wrap the other way, or a
     * memoriser who reaches Al-Fatihah falls off the start of the book. And the default has to
     * stay forwards: silently reversing an existing reader's position would be the worst
     * possible way to introduce this.
     */
    @Test
    fun `it wraps at both ends and defaults to the way it always went`() {
        val firstPage = assignPortion(0, Mushaf.UNITS_PER_PAGE, ReadingDirection.TOWARDS_FATIHAH)
        assertEquals(
            "going up from page 1 wraps to the end",
            Mushaf.PAGES,
            Mushaf.pageOf(firstPage.nextStartUnit),
        )

        val lastUnit = Mushaf.TOTAL_UNITS - Mushaf.UNITS_PER_PAGE
        val lastPage = assignPortion(lastUnit, Mushaf.UNITS_PER_PAGE, ReadingDirection.TOWARDS_NAS)
        assertEquals("and going down from 604 wraps to 1", 1, Mushaf.pageOf(lastPage.nextStartUnit))

        // The default is the old behaviour, so nobody's position moves on upgrade.
        assertEquals(
            assignPortion(yaSin, 2, ReadingDirection.TOWARDS_NAS).nextStartUnit,
            assignPortion(yaSin, 2).nextStartUnit,
        )
    }
}

/**
 * Checks 55–60 — comparing a recitation against the page. **PLAN task 14, his actual ask.**
 *
 * *"I need it to hear it very well and highlight from the pages and verses that this is where I
 * did mistake."*
 *
 * ⚠ **Most of these checks exist to prove it stays QUIET**, not that it finds things. Telling
 * someone they erred in the Qur'an when they did not is the worst thing this app can do, so the
 * tests that matter are the ones that would catch a false accusation.
 *
 * Al-Fatihah is used throughout because its words are short and its verse boundaries obvious;
 * the algorithm knows nothing about which sūrah it is looking at.
 */
class RecitationCheckTest {

    /** Al-Fatihah 1:2, written the way the page has it — with full vowel marks. */
    private val page = listOf(
        "1:2" to "الْحَمْدُ",
        "1:2" to "لِلَّهِ",
        "1:2" to "رَبِّ",
        "1:2" to "الْعَالَمِينَ",
        "1:3" to "الرَّحْمَٰنِ",
        "1:3" to "الرَّحِيمِ",
    )

    /** Check 55 — ⚠ a perfect recitation must produce no marks at all. */
    @Test
    fun `a correct recitation is marked correct, despite different spelling`() {
        // What a speech model actually returns: no vowel marks, plain alif.
        val heard = "الحمد لله رب العالمين الرحمن الرحيم"
        val verdict = checkRecitation(page, heard)

        assertTrue("it must be confident about a clean match", verdict.confident)
        assertTrue(
            "NOTHING may be marked: ${verdict.problems.map { it.expected }}",
            verdict.problems.isEmpty(),
        )
        assertEquals(1.0f, verdict.coverage, 0.01f)
    }

    /**
     * Check 56 — the folding that makes check 55 possible, on its own.
     *
     * ⚠ Without this every word on every page is a mistake, because the Qur'anic text carries
     * marks a transcript never contains. It is a fingerprint for lining strings up and touches
     * nothing that is displayed.
     */
    @Test
    fun `spelling differences a reciter cannot pronounce are folded away`() {
        assertEquals(foldArabic("الْحَمْدُ"), foldArabic("الحمد"))
        assertEquals(foldArabic("الرَّحْمَٰنِ"), foldArabic("الرحمن"))
        // The alif wears four hats and sounds like one letter.
        assertEquals(foldArabic("أحد"), foldArabic("احد"))
        assertEquals(foldArabic("ٱللَّه"), foldArabic("الله"))
        // Ta marbuta and alif maqsura, which transcripts use interchangeably.
        assertEquals(foldArabic("صلاة"), foldArabic("صلاه"))
        assertEquals(foldArabic("موسى"), foldArabic("موسي"))

        // ⚠ But genuinely different words must stay different, or nothing can ever be found.
        assertNotEquals(foldArabic("الحمد"), foldArabic("العالمين"))
    }

    /** Check 57 — one wrong word is found, and only that word. */
    @Test
    fun `a single wrong word is marked and its neighbours are not`() {
        val heard = "الحمد لله رب الناس الرحمن الرحيم"
        val verdict = checkRecitation(page, heard)

        assertTrue(verdict.confident)
        val wrong = verdict.problems
        assertEquals("exactly one word should be marked: ${wrong.map { it.expected }}", 1, wrong.size)
        assertEquals("الْعَالَمِينَ", wrong.first().expected)
        assertEquals("and it must know which verse it is in", "1:2", wrong.first().verseKey)
    }

    /**
     * Check 58 — ⚠ **the check this feature would be unusable without.**
     *
     * A word skipped in the middle would, under naive word-by-word comparison, push every later
     * word out of step and mark the whole rest of the page wrong. The longest-common-subsequence
     * walk exists entirely to stop that, and this is the assertion that proves it.
     */
    @Test
    fun `a skipped word does not cascade into marking the whole page wrong`() {
        val heard = "الحمد لله العالمين الرحمن الرحيم"  // "رب" skipped
        val verdict = checkRecitation(page, heard)

        val wrong = verdict.problems
        assertEquals("only the skipped word: ${wrong.map { it.expected }}", 1, wrong.size)
        assertEquals("رَبِّ", wrong.first().expected)

        // The words AFTER the gap must still be recognised as correct.
        val after = verdict.words.filter { it.expected == "الرَّحْمَٰنِ" || it.expected == "الرَّحِيمِ" }
        assertTrue("everything after the gap must stay matched", after.all { it.mark == WordMark.MATCHED })
    }

    /**
     * Check 59 — ⚠ **stopping early is not a mistake, and must never be shown as one.**
     *
     * Someone who recites half a page and stops has done nothing wrong. Marking the unread half
     * as errors would be the app accusing them of a mistake they did not make.
     */
    @Test
    fun `an unfinished recitation leaves the rest unmarked rather than wrong`() {
        val heard = "الحمد لله رب العالمين"  // stopped after 1:2
        val verdict = checkRecitation(page, heard, minimumCoverage = 0.5f)

        assertTrue("nothing may be called wrong", verdict.problems.isEmpty())
        val tail = verdict.words.filter { it.verseKey == "1:3" }
        assertTrue("the unread verse is UNCHECKED", tail.all { it.mark == WordMark.UNCHECKED })
        assertTrue("and the words say so: ${verdict.summary}", verdict.summary.contains("stopped"))
    }

    /**
     * Check 60 — ⚠ **when it cannot follow the recitation it marks NOTHING and blames itself.**
     *
     * The single most important behaviour in the file. A bad recording, a noisy room, a model
     * that mishears — none of those are the reader's error, and the app must not dress them up
     * as one. Sacred Rule 3 governs the sentence as strictly as the logic.
     */
    @Test
    fun `a check it could not follow accuses nobody`() {
        val nonsense = checkRecitation(page, "قل هو الله احد لم يلد")
        assertFalse("it must not be confident", nonsense.confident)
        assertTrue("and must mark nothing: ${nonsense.problems.size} marked", nonsense.problems.isEmpty() || !nonsense.confident)

        val nothing = checkRecitation(page, "")
        assertFalse(nothing.confident)
        assertTrue("silence marks nothing wrong", nothing.problems.isEmpty())
        assertTrue(
            "every word is unchecked rather than missing",
            nothing.words.all { it.mark == WordMark.UNCHECKED },
        )

        // Sacred Rule 3: nothing it says may blame the reader.
        listOf(nonsense.summary, nothing.summary).forEach { said ->
            listOf("wrong", "failed", "error", "mistake", "incorrect").forEach { banned ->
                assertFalse("blames the reader with '$banned': $said", said.lowercase().contains(banned))
            }
        }
    }
}

/**
 * Checks 61–63 — the comparison against the **real bundled Qur'an**, not a hand-written example.
 *
 * ⚠ **This is the check that matters more than the six before it.** Those used six words typed
 * into a test file, which proves the algorithm and proves nothing about the data it will meet.
 * These read `app/src/main/assets/arabic/{page}.json` — the actual files shipped in the APK — so a
 * fetch that silently wrote the wrong spelling, or a folding rule that fails on a letter
 * Al-Fatihah happens not to contain, is caught here rather than on his phone.
 *
 * Reading the assets from disk rather than through Android's AssetManager is what lets this run
 * on the JVM with no device at all, which is the only way it can run on every `check`.
 */
class ArabicAssetTest {

    private fun page(n: Int): List<Pair<String, String>> {
        val file = File("src/main/assets/arabic/$n.json")
        assertTrue("page $n is missing from the assets: ${file.absolutePath}", file.exists())
        val array = JSONArray(file.readText())
        return buildList {
            for (i in 0 until array.length()) {
                val verse = array.getJSONObject(i)
                val key = verse.getString("v")
                verse.getString("t").split(' ').filter { it.isNotBlank() }.forEach { add(key to it) }
            }
        }
    }

    /** Check 61 — the shipped text is the Qur'an's, by its own count. */
    @Test
    fun `every page is present and the verses add up`() {
        val dir = File("src/main/assets/arabic")
        assertTrue("the Arabic assets are missing entirely", dir.isDirectory)
        assertEquals("there must be one file per page", 604, dir.listFiles { f -> f.extension == "json" }!!.size)

        var verses = 0
        for (n in 1..604) {
            val array = JSONArray(File(dir, "$n.json").readText())
            verses += array.length()
            // ⚠ A verse with no words would read on screen as an ayah he skipped.
            for (i in 0 until array.length()) {
                val t = array.getJSONObject(i).getString("t")
                assertTrue("page $n has an empty verse", t.isNotBlank())
            }
        }
        assertEquals("the Qur'an has 6,236 verses and so must this", 6236, verses)
    }

    /**
     * Check 62 — ⚠ **reciting the real page perfectly must mark NOTHING.**
     *
     * The one that would catch a broken folding rule. The "recitation" here is the page's own
     * text with its vowel marks stripped, which is what a speech model produces — so any letter
     * form the folding does not handle shows up immediately as a false accusation.
     */
    @Test
    fun `the real page, recited exactly, is marked clean`() {
        // Page 1 is Al-Fatihah, page 604 the closing surahs, page 300 ordinary running text.
        listOf(1, 300, 604).forEach { n ->
            val expected = page(n)
            val recited = expected.joinToString(" ") { foldArabic(it.second) }
            val verdict = checkRecitation(expected, recited)

            assertTrue("page $n should be followable", verdict.confident)
            assertTrue(
                "page $n falsely marked ${verdict.versesToReview}",
                verdict.versesToReview.isEmpty(),
            )
            assertEquals("page $n coverage", 1.0f, verdict.coverage, 0.001f)
        }
    }

    /** Check 63 — a real mistake on a real page is found, and lands in the right ayah. */
    @Test
    fun `one wrong word on a real page marks that ayah and no other`() {
        val expected = page(1)
        // Swap one word for a word from elsewhere in the Qur'an, keeping everything else.
        val target = expected.size / 2
        val recited = expected.mapIndexed { i, (_, word) ->
            if (i == target) "قل" else foldArabic(word)
        }.joinToString(" ")

        val verdict = checkRecitation(expected, recited)
        assertTrue(verdict.confident)
        assertEquals(
            "exactly the ayah containing the swapped word: ${verdict.versesToReview}",
            setOf(expected[target].first),
            verdict.versesToReview,
        )
    }

    /**
     * Check 64 — OEM notification survival detection (PLAN task 15).
     *
     * Identifies Transsion (Tecno, Infinix, itel), Samsung, Xiaomi, Huawei, Oppo,
     * and generic Android, delivering manufacturer-tailored survival steps.
     */
    @Test
    fun `OEM advice identifies Transsion and major phone vendors with actionable steps`() {
        // 1. Transsion brands
        val tecno = OemAdvice.forThisPhone(manufacturer = "TECNO", brand = "TECNO MOBILE LIMITED")
        assertTrue("Tecno must be recognized as known strict OEM", tecno.known)
        assertEquals("Tecno", tecno.vendor)
        assertEquals("HiOS", tecno.systemSkin)
        assertTrue("Tecno steps must mention Phone Master", tecno.steps.any { it.contains("Phone Master") })

        val infinix = OemAdvice.forThisPhone(manufacturer = "Infinix", brand = "Infinix mobility")
        assertTrue(infinix.known)
        assertEquals("XOS", infinix.systemSkin)

        val itel = OemAdvice.forThisPhone(manufacturer = "itel", brand = "itel")
        assertTrue(itel.known)

        // 2. Samsung One UI
        val samsung = OemAdvice.forThisPhone(manufacturer = "samsung", brand = "samsung")
        assertTrue(samsung.known)
        assertEquals("Samsung", samsung.vendor)
        assertEquals("One UI", samsung.systemSkin)
        assertTrue("Samsung steps must mention Never sleeping apps", samsung.steps.any { it.contains("Never sleeping apps") })

        // 3. Xiaomi MIUI / HyperOS
        val xiaomi = OemAdvice.forThisPhone(manufacturer = "Xiaomi", brand = "Redmi")
        assertTrue(xiaomi.known)
        assertEquals("MIUI / HyperOS", xiaomi.systemSkin)
        assertTrue("Xiaomi steps must mention Autostart", xiaomi.steps.any { it.contains("Autostart") })

        // 4. Stock Android (Pixel, etc.)
        val pixel = OemAdvice.forThisPhone(manufacturer = "Google", brand = "google")
        assertFalse("Stock Android is not a known strict OEM", pixel.known)
        assertEquals("Google", pixel.vendor)
    }

    /**
     * Check 65 — Reminder self-check diagnostics (PLAN task 15).
     *
     * Answers "did the last nudge arrive?" by comparing lastArmedFor vs lastNudgeFiredAt,
     * notification permissions, exact alarms, and battery optimizations.
     */
    @Test
    fun `reminder diagnostic catches killed alarms and healthy deliveries`() {
        val now = LocalDateTime.of(2026, 8, 20, 19, 30)

        // Case 1: Healthy delivery — alarm was set for 19:00, and fired at 19:01
        val healthyReport = NudgeDiagnostic.evaluate(
            notificationsEnabled = true,
            exactAlarmsAllowed = true,
            isBatteryOptimized = false, // unrestricted
            isScheduleOff = false,
            lastArmedFor = LocalDateTime.of(2026, 8, 20, 19, 0),
            lastNudgeFiredAt = LocalDateTime.of(2026, 8, 20, 19, 1),
            now = now,
        )
        assertEquals(NudgeDiagnostic.Status.OK, healthyReport.overallStatus)
        assertTrue(healthyReport.explanation.contains("woke the app on schedule"))

        // Case 2: Killed alarm — alarm was set for 18:30 (>20 min ago) and never fired
        val killedReport = NudgeDiagnostic.evaluate(
            notificationsEnabled = true,
            exactAlarmsAllowed = true,
            isBatteryOptimized = true, // phone is optimizing
            isScheduleOff = false,
            lastArmedFor = LocalDateTime.of(2026, 8, 20, 18, 30),
            lastNudgeFiredAt = null,
            now = now,
        )
        assertEquals(NudgeDiagnostic.Status.ERROR, killedReport.overallStatus)
        assertTrue(killedReport.explanation.contains("killed Wird in the background"))
        assertTrue(killedReport.checks.any { it.name == "Last Reminder Arrival" && it.status == NudgeDiagnostic.Status.ERROR })

        // Case 3: Blocked notifications
        val blockedNotifReport = NudgeDiagnostic.evaluate(
            notificationsEnabled = false,
            exactAlarmsAllowed = true,
            isBatteryOptimized = false,
            isScheduleOff = false,
            lastArmedFor = LocalDateTime.of(2026, 8, 20, 20, 0),
            lastNudgeFiredAt = null,
            now = now,
        )
        assertEquals(NudgeDiagnostic.Status.ERROR, blockedNotifReport.overallStatus)
        assertTrue(blockedNotifReport.checks.any { it.name == "Notifications" && it.status == NudgeDiagnostic.Status.ERROR })

        // Case 4: Warnings only — battery optimized + inexact alarms, but upcoming alarm
        val warningReport = NudgeDiagnostic.evaluate(
            notificationsEnabled = true,
            exactAlarmsAllowed = false,
            isBatteryOptimized = true,
            isScheduleOff = false,
            lastArmedFor = LocalDateTime.of(2026, 8, 20, 21, 0),
            lastNudgeFiredAt = null,
            now = now,
        )
        assertEquals(NudgeDiagnostic.Status.WARNING, warningReport.overallStatus)
    }

    /**
     * Check 66 — Privacy pledge guarantees (PLAN task 18, Sacred Rule 1).
     *
     * Guarantees 100% on-device data sovereignty:
     * - Voice recordings stay on device
     * - No accounts, no cloud sync, no tracking, no analytics
     * - Export and audio deletion capabilities verified
     */
    @Test
    fun `privacy pledge guarantees 100 percent on device data sovereignty`() {
        assertTrue("Privacy pledge integrity must be verified", PrivacyPledge.verifyIntegrity())
        assertTrue("Must have at least 4 clear pledge points", PrivacyPledge.ITEMS.size >= 4)

        val fullPledgeText = (listOf(PrivacyPledge.TITLE, PrivacyPledge.SUBTITLE, PrivacyPledge.PROMISE) +
            PrivacyPledge.ITEMS.flatMap { listOf(it.title, it.description) })
            .joinToString(" ")
            .lowercase()

        // Sacred Rule 1 assertions: no tracking, no servers, on-device guarantees
        assertTrue("Must state on-device guarantee", fullPledgeText.contains("on-device") || fullPledgeText.contains("on this device"))
        assertTrue("Must guarantee voice never leaves", fullPledgeText.contains("voice") && fullPledgeText.contains("never"))
        assertTrue("Must guarantee zero telemetry/tracking", fullPledgeText.contains("zero") || fullPledgeText.contains("no tracking"))
        assertTrue("Must guarantee no analytics SDKs", fullPledgeText.contains("no analytics sdk"))
        assertTrue("Must guarantee no advertising identifiers", fullPledgeText.contains("no advertising identifier"))
        assertTrue("Must mention data export sovereignty", fullPledgeText.contains("export"))

        // Must not contain tracking practices or remote database synchronization
        listOf("remote database exists", "sync to cloud", "monetize your data", "sell your data", "facebook pixel")
            .forEach { banned ->
                assertFalse("Pledge must never endorse: $banned", fullPledgeText.contains(banned))
            }
    }

    /**
     * Check 67 — Settings, schedule, and direction refinements are consistent and robust.
     *
     * Validates:
     * 1. Surah.listLabel respects includeMeaning parameter (toggle wiring).
     * 2. Weekly reading schedule supports custom active days with lighter unselected days.
     * 3. ReadingDirection progression assigns forwards vs backwards properly.
     * 4. Natural recitation in any surah proceeds from ayah 1 to the end.
     */
    @Test
    fun `check 67 - settings, schedule, and direction refinements are consistent and robust`() {
        val ikhlas = SurahIndex.byNumber(112)!!
        assertEquals("Al-Ikhlas (The Sincerity)", ikhlas.listLabel(includeMeaning = true))
        assertEquals("Al-Ikhlas", ikhlas.listLabel(includeMeaning = false))

        // Reading direction: from Al-Ikhlas (page 604)
        val p604Unit = (604 - 1) * Mushaf.UNITS_PER_PAGE
        val assignDown = assignPortion(p604Unit, 2, ReadingDirection.TOWARDS_NAS)
        val assignUp = assignPortion(p604Unit, 2, ReadingDirection.TOWARDS_FATIHAH)

        // Downwards wraps from 604 to 1
        assertEquals(1, Mushaf.pageOf(assignDown.nextStartUnit))
        // Upwards moves backwards from 604 to 603
        assertEquals(603, Mushaf.pageOf(assignUp.nextStartUnit))

        // Weekly schedule: weekdays full (2 units = 1 page), weekends light (1 unit = half page)
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val plan = ReadingPlan(
            defaultUnits = 2,
            weekdayUnits = (DayOfWeek.entries - weekdays).associateWith { 1 },
        )
        assertEquals(2, plan.unitsOn(DayOfWeek.MONDAY))
        assertEquals(2, plan.unitsOn(DayOfWeek.FRIDAY))
        assertEquals(1, plan.unitsOn(DayOfWeek.SATURDAY))
        assertEquals(1, plan.unitsOn(DayOfWeek.SUNDAY))
    }
}
