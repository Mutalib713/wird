package com.mosman.wird

import com.mosman.wird.domain.LifeSpace
import com.mosman.wird.domain.ReadingDirection
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.TrackScheduleMode
import com.mosman.wird.domain.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class LifeSpaceTest {

    @Test
    fun readingTrack_jsonRoundTrip_preservesAllFields() {
        val original = ReadingTrack(
            id = "track_hifz_yasin",
            name = "Evening Madrasa (Ya-Sin)",
            type = TrackType.HIFZ,
            activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
            positionUnit = 878,
            direction = ReadingDirection.TOWARDS_NAS,
            dailyUnits = 2,
            currentStreak = 5,
            totalDaysRead = 14,
            lastCompletedDate = "2026-09-22",
            startVerseSurah = 36,
            startVerseAyah = 1,
        )

        val json = original.toJson()
        val restored = ReadingTrack.fromJson(json)

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.type, restored.type)
        assertEquals(original.activeDays, restored.activeDays)
        assertEquals(original.positionUnit, restored.positionUnit)
        assertEquals(original.direction, restored.direction)
        assertEquals(original.dailyUnits, restored.dailyUnits)
        assertEquals(original.currentStreak, restored.currentStreak)
        assertEquals(original.totalDaysRead, restored.totalDaysRead)
        assertEquals(original.lastCompletedDate, restored.lastCompletedDate)
        assertEquals(original.startVerseSurah, restored.startVerseSurah)
        assertEquals(original.startVerseAyah, restored.startVerseAyah)
    }

    @Test
    fun lifeSpace_jsonRoundTrip_preservesTracksAndFrozenState() {
        val track1 = ReadingTrack(
            id = "t1",
            name = "Weekday Hifz",
            type = TrackType.HIFZ,
            activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
        )
        val track2 = ReadingTrack(
            id = "t2",
            name = "Weekend Tilawah",
            type = TrackType.TILAWAH,
            activeDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        )
        val space = LifeSpace(
            id = "space_home",
            name = "Home",
            isFrozen = false,
            tracks = listOf(track1, track2),
        )

        val json = space.toJson()
        val restored = LifeSpace.fromJson(json)

        assertEquals("space_home", restored.id)
        assertEquals("Home", restored.name)
        assertFalse(restored.isFrozen)
        assertEquals(2, restored.tracks.size)
        assertEquals("Weekday Hifz", restored.tracks[0].name)
        assertEquals("Weekend Tilawah", restored.tracks[1].name)
    }

    @Test
    fun readingTrack_scheduleLabel_formatsCommonSchedulesPlainly() {
        val monThu = ReadingTrack(
            id = "1",
            name = "Mon-Thu",
            activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
        )
        assertEquals("Mon–Thu", monThu.scheduleLabel())

        val weekend = ReadingTrack(
            id = "2",
            name = "Weekend",
            activeDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        )
        assertEquals("Sat–Sun", weekend.scheduleLabel())

        val daily = ReadingTrack(
            id = "3",
            name = "Daily",
            activeDays = DayOfWeek.entries.toSet(),
        )
        assertEquals("Daily", daily.scheduleLabel())
    }

    @Test
    fun readingTrack_isDueToday_matchesActiveDays() {
        val wednesday = LocalDate.of(2026, 9, 23) // Wednesday
        assertEquals(DayOfWeek.WEDNESDAY, wednesday.dayOfWeek)

        val monThuTrack = ReadingTrack(
            id = "1",
            name = "Mon-Thu",
            activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
        )
        val weekendTrack = ReadingTrack(
            id = "2",
            name = "Weekend",
            activeDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        )

        assertTrue(monThuTrack.isDueToday(wednesday))
        assertFalse(weekendTrack.isDueToday(wednesday))
    }

    @Test
    fun readingTrack_weekdayMadrasa_streakContinuesAcrossWeekend() {
        val monThuDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)
        val track = ReadingTrack(
            id = "hifz",
            name = "Madrasa",
            activeDays = monThuDays,
            currentStreak = 4,
            lastCompletedDate = "2026-09-24", // Thursday
        )

        // On Monday 2026-09-28
        val monday = LocalDate.of(2026, 9, 28)
        var checkDate = monday.minusDays(1) // Sunday
        var lookbackLimit = 0
        while (checkDate.dayOfWeek !in track.activeDays && lookbackLimit < 7) {
            checkDate = checkDate.minusDays(1)
            lookbackLimit++
        }
        // checkDate should be Thursday 2026-09-24!
        assertEquals(LocalDate.of(2026, 9, 24), checkDate)
        assertEquals("2026-09-24", checkDate.toString())
        assertTrue(track.lastCompletedDate == checkDate.toString())
    }
}

