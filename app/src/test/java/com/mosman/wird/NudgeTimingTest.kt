package com.mosman.wird

import com.mosman.wird.domain.Coordinates
import com.mosman.wird.domain.NudgeSchedule
import com.mosman.wird.domain.Prayer
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.nextAwake
import com.mosman.wird.domain.nextReminderOnDueDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class NudgeTimingTest {

    @Test
    fun mondayToThursdayTrackEvaluatedOnSaturdayEveningGivesMondayTime() {
        // B6: a Monday–Thursday track evaluated on Saturday evening gives Monday's time
        val zone = ZoneId.of("Africa/Accra")
        val saturday = LocalDate.of(2026, 10, 10) // Saturday
        assertEquals(DayOfWeek.SATURDAY, saturday.dayOfWeek)
        val saturdayEvening = saturday.atTime(21, 0).atZone(zone)

        val monThuTrack = ReadingTrack(
            id = "mon-thu-track",
            name = "Juz Amma Mon-Thu",
            activeDays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
            ),
        )

        val schedule = NudgeSchedule.AtClockTime(LocalTime.of(20, 0))

        val result = nextReminderOnDueDay(
            track = monThuTrack,
            schedule = schedule,
            from = saturdayEvening,
            coordinates = null,
            away = null,
        )

        val expectedDate = LocalDate.of(2026, 10, 12) // Monday
        assertEquals(DayOfWeek.MONDAY, expectedDate.dayOfWeek)
        assertEquals(expectedDate, result?.toLocalDate())
        assertEquals(LocalTime.of(20, 0), result?.toLocalTime())
    }

    @Test
    fun trackWithNoActiveDaysSetMatchesNextAwake() {
        // B6: a track with no active days set (meaning every day) is unchanged
        val zone = ZoneId.of("Africa/Accra")
        val saturday = LocalDate.of(2026, 10, 10) // Saturday
        val saturdayEvening = saturday.atTime(21, 0).atZone(zone)

        val dailyTrack = ReadingTrack(
            id = "daily-track",
            name = "Daily Reading",
            activeDays = emptySet(),
        )

        val schedule = NudgeSchedule.AtClockTime(LocalTime.of(20, 0))

        val result = nextReminderOnDueDay(
            track = dailyTrack,
            schedule = schedule,
            from = saturdayEvening,
            coordinates = null,
            away = null,
        )

        val expected = schedule.nextAwake(saturdayEvening, null, null)
        assertEquals(expected, result)
        assertEquals(LocalDate.of(2026, 10, 11), result?.toLocalDate()) // Sunday
    }

    @Test
    fun prayerScheduleOnDueDayStepsForwardToMonday() {
        val accra = Coordinates(5.6037, -0.1870)
        val zone = ZoneId.of("Africa/Accra")
        val saturday = LocalDate.of(2026, 10, 10) // Saturday
        val saturdayNight = saturday.atTime(22, 0).atZone(zone)

        val monThuTrack = ReadingTrack(
            id = "mon-thu-prayer",
            name = "Prayer Mon-Thu",
            activeDays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
            ),
        )

        val schedule = NudgeSchedule.AfterPrayer(Prayer.MAGHRIB, 30)

        val result = nextReminderOnDueDay(
            track = monThuTrack,
            schedule = schedule,
            from = saturdayNight,
            coordinates = accra,
            away = null,
        )

        assertEquals(LocalDate.of(2026, 10, 12), result?.toLocalDate()) // Monday
    }
}
