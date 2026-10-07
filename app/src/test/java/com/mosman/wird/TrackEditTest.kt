package com.mosman.wird

import android.content.SharedPreferences
import com.mosman.wird.data.DayLogStore
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.PickedPosition
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.TrackEditChanges
import com.mosman.wird.domain.TrackType
import com.mosman.wird.domain.applyTrackEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TrackEditTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun pureFunctionRenamingThreeUnitTrackPreservesPlanAndPosition() {
        // B3: a 3-unit track renamed keeps dailyUnits = 3, customTargetVerses = null, its positionUnit and its null start verse
        val original = ReadingTrack(
            id = "test-track",
            name = "Morning Reading",
            type = TrackType.TILAWAH,
            dailyUnits = 3,
            customTargetVerses = null,
            positionUnit = 15,
            startVerseSurah = null,
            startVerseAyah = null,
        )

        val changes = TrackEditChanges(
            name = "Renamed Morning Reading",
            type = original.type,
            intention = original.intention,
            isCustomTarget = false,
            customTargetVerses = null,
            dailyUnits = original.dailyUnits,
            activeDays = original.activeDays,
            direction = original.direction,
            pickedStart = null,
        )

        val updated = applyTrackEdit(original, changes)

        assertEquals("Renamed Morning Reading", updated.name)
        assertEquals(3, updated.dailyUnits)
        assertNull(updated.customTargetVerses)
        assertEquals(15, updated.positionUnit)
        assertNull(updated.startVerseSurah)
        assertNull(updated.startVerseAyah)
    }

    @Test
    fun pureFunctionWithPickedPositionUpdatesPositionAndStartVerse() {
        val original = ReadingTrack(
            id = "test-track",
            name = "Hifdh",
            type = TrackType.HIFZ,
            dailyUnits = 2,
            positionUnit = 0,
            startVerseSurah = 1,
            startVerseAyah = 1,
        )

        val changes = TrackEditChanges(
            name = "Hifdh",
            type = original.type,
            intention = original.intention,
            isCustomTarget = false,
            customTargetVerses = null,
            dailyUnits = 2,
            activeDays = original.activeDays,
            direction = original.direction,
            pickedStart = PickedPosition(unit = 40, verse = 2 to 255),
        )

        val updated = applyTrackEdit(original, changes)

        assertEquals(40, updated.positionUnit)
        assertEquals(2, updated.startVerseSurah)
        assertEquals(255, updated.startVerseAyah)
    }

    @Test
    fun editTrackInWirdStoreChangesDailyTargetWithoutResettingPosition() {
        // B2 / R2: save a position, then run an editTrack that changes only daily target from an older copy's id,
        // and assert position is unchanged.
        val fakePrefs = FakeSharedPreferences()
        val dir = tempFolder.newFolder()
        val days = DayLogStore(dir)
        val store = WirdStore(fakePrefs, days)

        val track = ReadingTrack(
            id = "track-a",
            name = "Surah Study",
            dailyUnits = 2,
            positionUnit = 18,
        )
        store.addTrack(track)

        // Verify initial state
        val initial = store.getReadingTracks().first { it.id == "track-a" }
        assertEquals(18, initial.positionUnit)
        assertEquals(2, initial.dailyUnits)

        // Edit daily target only
        store.editTrack("track-a") { it.copy(dailyUnits = 4) }

        // Assert position is completely unchanged
        val afterEdit = store.getReadingTracks().first { it.id == "track-a" }
        assertEquals(18, afterEdit.positionUnit)
        assertEquals(4, afterEdit.dailyUnits)
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = values
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = values[key] as? Set<String> ?: defValues
        override fun getInt(key: String, defValue: Int): Int = (values[key] as? Number)?.toInt() ?: defValue
        override fun getLong(key: String, defValue: Long): Long = (values[key] as? Number)?.toLong() ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = (values[key] as? Number)?.toFloat() ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = key in values
        override fun edit(): SharedPreferences.Editor = Editor(values)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class Editor(private val target: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private var clearAll = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor { changes[key] = value; return this }
            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor { changes[key] = values; return this }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor { changes[key] = value; return this }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor { changes[key] = value; return this }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor { changes[key] = value; return this }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { changes[key] = value; return this }
            override fun remove(key: String): SharedPreferences.Editor { changes[key] = this; return this }
            override fun clear(): SharedPreferences.Editor { clearAll = true; return this }
            override fun commit(): Boolean {
                if (clearAll) target.clear()
                changes.forEach { (k, v) ->
                    if (v === this) target.remove(k) else target[k] = v
                }
                return true
            }
            override fun apply() { commit() }
        }
    }
}
