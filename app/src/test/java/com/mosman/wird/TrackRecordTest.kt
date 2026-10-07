package com.mosman.wird

import com.mosman.wird.data.DayLogStore
import com.mosman.wird.data.decodeSchedule
import com.mosman.wird.data.encodeSchedule
import com.mosman.wird.domain.nextAfter
import com.mosman.wird.domain.repeatLabel
import com.mosman.wird.nudge.Nudge
import java.time.ZoneId
import com.mosman.wird.domain.Commitment
import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.NudgeSchedule
import com.mosman.wird.domain.Prayer
import com.mosman.wird.domain.ReadingDirection
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.TrackType
import com.mosman.wird.domain.assignmentOn
import com.mosman.wird.domain.effectiveSchedule
import com.mosman.wird.domain.trackProgress
import com.mosman.wird.domain.withProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The day log is the one record of what was read. These pin that down, track by track.
 *
 * Written 2026-10-03, before the change, because the bugs they describe all came from the
 * same place: each track kept its own streak counter beside the day log, and the two drifted.
 * Home showed a streak that never dropped, undo left a track's total one too high, and one
 * track could see another track's day as its own.
 */
class TrackRecordTest {

    private val daily = ReadingTrack(id = "daily", name = "Daily Reading", type = TrackType.TILAWAH)
    private val monThu = ReadingTrack(
        id = "madrasa", name = "Madrasa", type = TrackType.HIFZ,
        activeDays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY),
    )

    // 2026-10-05 is a Monday.
    private val mon = LocalDate.of(2026, 10, 5)

    private fun log(date: LocalDate, track: String, method: Method = Method.TAPPED) =
        DayLog(date, method, track, track)

    // ---- the streak comes from the log, so it drops when days are missed ----------------

    @Test
    fun streak_counts_consecutive_days_for_this_track_only() {
        val logs = listOf(
            log(mon.minusDays(2), "daily"), log(mon.minusDays(1), "daily"), log(mon, "daily"),
            log(mon.minusDays(3), "madrasa"),
        )
        val p = trackProgress(logs, daily, mon)
        assertEquals(3, p.currentStreak)
        assertEquals(3, p.totalDaysRead)
    }

    @Test
    fun a_missed_day_ends_the_streak_even_if_nothing_was_done_since() {
        // Read Mon-Wed last week, nothing since. Home used to keep showing 3.
        val logs = listOf(log(mon.minusDays(7), "daily"), log(mon.minusDays(6), "daily"), log(mon.minusDays(5), "daily"))
        val p = trackProgress(logs, daily, mon)
        assertEquals(0, p.currentStreak)
        assertEquals("total days read never resets", 3, p.totalDaysRead)
    }

    @Test
    fun today_not_yet_done_does_not_break_the_streak() {
        val logs = listOf(log(mon.minusDays(2), "daily"), log(mon.minusDays(1), "daily"))
        assertEquals(2, trackProgress(logs, daily, mon).currentStreak)
    }

    @Test
    fun days_a_track_is_not_scheduled_never_break_its_streak() {
        // Mon-Thu track: last Wed and Thu done, then the weekend, then today (Mon).
        val logs = listOf(log(mon.minusDays(5), "madrasa"), log(mon.minusDays(4), "madrasa"), log(mon, "madrasa"))
        assertEquals(3, trackProgress(logs, monThu, mon).currentStreak)
    }

    @Test
    fun a_missed_scheduled_day_does_break_it() {
        // Last Wed done, last Thu (a scheduled day) missed.
        val logs = listOf(log(mon.minusDays(5), "madrasa"), log(mon, "madrasa"))
        assertEquals(1, trackProgress(logs, monThu, mon).currentStreak)
    }

    @Test
    fun recited_and_tapped_are_counted_apart() {
        val logs = listOf(log(mon.minusDays(1), "daily", Method.RECITED), log(mon, "daily", Method.TAPPED))
        val p = trackProgress(logs, daily, mon)
        assertEquals(1, p.recitedDays)
        assertEquals(1, p.tappedDays)
    }

    @Test
    fun withProgress_fills_the_derived_fields_so_every_screen_agrees() {
        val logs = listOf(log(mon.minusDays(1), "daily"), log(mon, "daily"))
        val t = daily.withProgress(logs, mon)
        assertEquals(2, t.currentStreak)
        assertEquals(2, t.totalDaysRead)
        assertEquals(mon.toString(), t.lastCompletedDate)
        assertTrue(t.isCompletedToday(mon))
    }

    @Test
    fun undoing_today_is_just_removing_the_row() {
        // The whole undo bug: a counter had to be wound back by hand and was wound back wrong.
        val before = listOf(log(mon.minusDays(1), "daily"), log(mon, "daily"))
        val after = before.filterNot { it.date == mon }
        val t = daily.withProgress(after, mon)
        assertEquals(1, t.currentStreak)
        assertEquals(1, t.totalDaysRead)
        assertFalse(t.isCompletedToday(mon))
        // And doing it again today counts as one day, continuing the streak.
        val redone = daily.withProgress(after + log(mon, "daily"), mon)
        assertEquals(2, redone.currentStreak)
    }

    // ---- the track owns its plan: amount, weekday exceptions and direction ---------------

    @Test
    fun the_portion_uses_the_tracks_weekday_exception() {
        val t = daily.copy(positionUnit = 100, dailyUnits = 2, weekdayUnits = mapOf(FRIDAY to 1))
        val friday = mon.plusDays(4)
        assertEquals(1, t.assignmentOn(friday).units)
        assertEquals(2, t.assignmentOn(mon).units)
    }

    @Test
    fun the_portion_uses_the_tracks_direction() {
        val up = daily.copy(positionUnit = 100, dailyUnits = 2, direction = ReadingDirection.TOWARDS_FATIHAH)
        assertEquals(ReadingDirection.TOWARDS_FATIHAH, up.assignmentOn(mon).direction)
    }

    @Test
    fun weekday_exceptions_survive_a_save_and_load() {
        val t = daily.copy(weekdayUnits = mapOf(FRIDAY to 1, DayOfWeek.SATURDAY to 4))
        assertEquals(t.weekdayUnits, ReadingTrack.fromJson(t.toJson()).weekdayUnits)
    }

    // ---- which reminder wins tonight ---------------------------------------------------

    private val fallback = NudgeSchedule.AfterPrayer(Prayer.MAGHRIB, 30)
    private val ownClock = NudgeSchedule.AtClockTime(LocalTime.of(21, 0))

    @Test
    fun a_promise_made_today_wins_over_a_tracks_own_reminder() {
        // "In an hour" used to do nothing on any track with its own reminder.
        val inAnHour = NudgeSchedule.AtClockTime(LocalTime.of(19, 30))
        val promise = Commitment("in an hour", LocalDateTime.of(mon, LocalTime.of(18, 30)), inAnHour)
        val track = daily.copy(reminderScheduleRaw = "CLOCK 21:00 0")
        assertEquals(inAnHour, effectiveSchedule(track, promise, fallback, mon))
    }

    @Test
    fun yesterdays_promise_is_ignored() {
        val promise = Commitment("after Isha", LocalDateTime.of(mon.minusDays(1), LocalTime.NOON), ownClock)
        assertEquals(fallback, effectiveSchedule(daily, promise, fallback, mon))
    }

    @Test
    fun a_tracks_own_reminder_beats_the_default() {
        val track = daily.copy(reminderScheduleRaw = "CLOCK 21:00 0")
        assertEquals(ownClock, effectiveSchedule(track, null, fallback, mon))
    }

    // ---- repeat reminders are typed in minutes (2026-10-03) ------------------------------

    @Test
    fun a_custom_repeat_in_minutes_survives_a_save_and_load() {
        val every45 = NudgeSchedule.AtClockTime(LocalTime.of(20, 0), repeatIntervalMinutes = 45)
        assertEquals("CLOCK 20:00 45m", encodeSchedule(every45))
        assertEquals(every45, decodeSchedule(encodeSchedule(every45)))
    }

    @Test
    fun reminders_saved_before_minutes_still_mean_hours() {
        // The old form wrote whole hours with no unit: "2" was every two hours.
        assertEquals(NudgeSchedule.AtClockTime(LocalTime.of(20, 0), 120), decodeSchedule("CLOCK 20:00 2"))
        assertEquals(NudgeSchedule.AtClockTime(LocalTime.of(21, 0), 0), decodeSchedule("CLOCK 21:00 0"))
    }

    @Test
    fun a_45_minute_repeat_fires_every_45_minutes_after_the_first() {
        val zone = ZoneId.of("Africa/Accra")
        val s = NudgeSchedule.AtClockTime(LocalTime.of(20, 0), repeatIntervalMinutes = 45)
        // At 20:50 the 20:00 and 20:45 reminders have passed; the next is 21:30.
        val next = s.nextAfter(mon.atTime(20, 50).atZone(zone), null)
        assertEquals(mon.atTime(21, 30).atZone(zone), next)
    }

    @Test
    fun each_track_has_its_own_notification_number() {
        // They all shared number 1, so a second track's reminder replaced the first.
        val a = Nudge.notificationIdFor("track_main")
        val b = Nudge.notificationIdFor("track_1790996370406")
        assertFalse(a == b)
        assertEquals(a, Nudge.notificationIdFor("track_main"))
        assertTrue(a >= 10_000 && b >= 10_000)
    }

    @Test
    fun repeat_labels_read_naturally() {
        assertEquals("45 min", repeatLabel(45))
        assertEquals("1 hr", repeatLabel(60))
        assertEquals("1 hr 30 min", repeatLabel(90))
        assertEquals("2 hrs", repeatLabel(120))
    }

    // ---- the day log answers per track, never with another track's row -------------------

    private fun tempDir(): File = Files.createTempDirectory("wird-days").toFile()

    @Test
    fun another_tracks_day_is_not_this_tracks_day() {
        val days = DayLogStore(tempDir())
        days.markDone(mon, Method.RECITED, trackId = "madrasa", trackName = "Madrasa", startUnit = 10, units = 2)
        assertNull(days.methodFor(mon, "daily"))
        assertNull(days.coveredOn(mon, "daily"))
        assertEquals(Method.RECITED, days.methodFor(mon, "madrasa"))
    }

    @Test
    fun undo_on_one_track_leaves_the_other_alone() {
        val days = DayLogStore(tempDir())
        days.markDone(mon, Method.TAPPED, trackId = "daily", trackName = "Daily")
        days.markDone(mon, Method.TAPPED, trackId = "madrasa", trackName = "Madrasa")
        days.clear(mon, "madrasa")
        assertEquals(Method.TAPPED, days.methodFor(mon, "daily"))
        assertNull(days.methodFor(mon, "madrasa"))
    }

    @Test
    fun two_tracks_recorded_on_one_day_get_two_files() {
        val days = DayLogStore(tempDir())
        assertFalse(days.audioFileFor(mon, "daily") == days.audioFileFor(mon, "madrasa"))
    }

    @Test
    fun rows_from_before_tracks_are_adopted_by_the_first_track_once() {
        val dir = tempDir()
        // A days.json written by the app before tracks existed: no trackId.
        File(dir, "days.json").writeText(
            """[{"date":"2026-10-04","method":"RECITED","startUnit":4,"units":2}]"""
        )
        val days = DayLogStore(dir)
        assertNull(days.methodFor(mon.minusDays(1), "daily"))
        assertEquals(1, days.adoptUnownedRows("daily"))
        assertEquals(Method.RECITED, days.methodFor(mon.minusDays(1), "daily"))
        assertEquals(0, days.adoptUnownedRows("daily"))
    }

    @Test
    fun effective_schedule_with_two_tracks_and_one_commitment_only_moves_committed_track() {
        // B8: With two tracks, tapping "In an hour" on Track A moves only Track A's reminder
        val trackA = ReadingTrack(
            id = "track-a",
            name = "Track A",
            reminderScheduleRaw = encodeSchedule(NudgeSchedule.AtClockTime(LocalTime.of(18, 0))),
        )
        val trackB = ReadingTrack(
            id = "track-b",
            name = "Track B",
            reminderScheduleRaw = encodeSchedule(NudgeSchedule.AtClockTime(LocalTime.of(20, 0))),
        )
        val fallback = NudgeSchedule.Default

        val commitmentA = Commitment(
            spoken = "in an hour",
            madeAt = mon.atTime(18, 30),
            schedule = NudgeSchedule.AtClockTime(LocalTime.of(19, 30)),
        )

        // Track A has a commitment today, Track B does not
        val scheduleA = effectiveSchedule(trackA, commitmentA, fallback, mon)
        val scheduleB = effectiveSchedule(trackB, null, fallback, mon)

        assertEquals(NudgeSchedule.AtClockTime(LocalTime.of(19, 30)), scheduleA)
        assertEquals(NudgeSchedule.AtClockTime(LocalTime.of(20, 0)), scheduleB)
    }
}
