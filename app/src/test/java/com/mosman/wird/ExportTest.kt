package com.mosman.wird

import com.mosman.wird.data.Export
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.TrackType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class ExportTest {

    @Test
    fun buildSettingsJsonIncludesAllFieldsAndTracks() {
        val track = ReadingTrack(
            id = "track_1",
            name = "Morning Wird",
            type = TrackType.TILAWAH,
            activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            positionUnit = 10,
            dailyUnits = 2,
        )

        val jsonString = Export.buildSettingsJson(
            readerName = "Mutalib",
            themeMode = "SYSTEM",
            trackScheduleMode = "AUTOMATIC",
            manualActiveTrackId = null,
            nudgeScheduleRaw = "prayer:MAGHRIB+15",
            tracks = listOf(track.toJson()),
        )

        val json = JSONObject(jsonString)
        assertEquals("Mutalib", json.getString("name"))
        assertEquals("SYSTEM", json.getString("theme"))
        assertEquals("AUTOMATIC", json.getString("trackScheduleMode"))
        assertFalse(json.has("manualActiveTrackId"))
        assertEquals("prayer:MAGHRIB+15", json.getString("reminderSchedule"))

        val tracksArr = json.getJSONArray("tracks")
        assertEquals(1, tracksArr.length())
        val trackJson = tracksArr.getJSONObject(0)
        assertEquals("track_1", trackJson.getString("id"))
        assertEquals("Morning Wird", trackJson.getString("name"))
        assertEquals("TILAWAH", trackJson.getString("type"))
        assertEquals(10, trackJson.getInt("positionUnit"))
    }

    @Test
    fun buildSettingsJsonOmitsNameWhenNull() {
        val jsonString = Export.buildSettingsJson(
            readerName = null,
            themeMode = "DARK",
            trackScheduleMode = "MANUAL",
            manualActiveTrackId = "track_2",
            nudgeScheduleRaw = "clock:20:00",
            tracks = emptyList(),
        )

        val json = JSONObject(jsonString)
        assertFalse(json.has("name"))
        assertEquals("DARK", json.getString("theme"))
        assertEquals("MANUAL", json.getString("trackScheduleMode"))
        assertEquals("track_2", json.getString("manualActiveTrackId"))
        assertEquals("clock:20:00", json.getString("reminderSchedule"))
        assertEquals(0, json.getJSONArray("tracks").length())
    }
}
