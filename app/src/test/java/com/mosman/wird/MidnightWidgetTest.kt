package com.mosman.wird

import com.mosman.wird.widget.MidnightWidgetReceiver
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class MidnightWidgetTest {

    @Test
    fun nextMidnightRefreshCalculatesNextDayOneMinutePastMidnight() {
        val zone = ZoneId.of("Africa/Accra")
        val afternoon = ZonedDateTime.of(
            LocalDate.of(2026, 10, 7),
            LocalTime.of(15, 30),
            zone,
        )

        val refresh = MidnightWidgetReceiver.nextMidnightRefresh(afternoon)

        assertEquals(LocalDate.of(2026, 10, 8), refresh.toLocalDate())
        assertEquals(LocalTime.of(0, 1), refresh.toLocalTime())
        assertEquals(zone, refresh.zone)
    }

    @Test
    fun nextMidnightRefreshCalculatesNextDayWhenCalledJustBeforeMidnight() {
        val zone = ZoneId.of("Africa/Accra")
        val lateNight = ZonedDateTime.of(
            LocalDate.of(2026, 10, 7),
            LocalTime.of(23, 59, 59),
            zone,
        )

        val refresh = MidnightWidgetReceiver.nextMidnightRefresh(lateNight)

        assertEquals(LocalDate.of(2026, 10, 8), refresh.toLocalDate())
        assertEquals(LocalTime.of(0, 1), refresh.toLocalTime())
    }

    @Test
    fun nextMidnightRefreshCalculatesNextDayWhenCalledJustAfterMidnight() {
        val zone = ZoneId.of("Africa/Accra")
        val justAfterMidnight = ZonedDateTime.of(
            LocalDate.of(2026, 10, 8),
            LocalTime.of(0, 0, 30),
            zone,
        )

        val refresh = MidnightWidgetReceiver.nextMidnightRefresh(justAfterMidnight)

        assertEquals(LocalDate.of(2026, 10, 9), refresh.toLocalDate())
        assertEquals(LocalTime.of(0, 1), refresh.toLocalTime())
    }
}
