package com.mosman.wird

import com.mosman.wird.data.DayLogStore
import com.mosman.wird.data.SafeFile
import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Method
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class DayLogStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun garbageInDaysJsonGivesEmptyListAndKeepsTimestampedFile() {
        val dir = tempFolder.newFolder()
        val daysFile = File(dir, "days.json")
        daysFile.writeText("corrupt garbage {not json")

        val store = DayLogStore(dir)
        val logs = store.all()

        assertEquals(emptyList<DayLog>(), logs)
        val corruptFiles = dir.listFiles { _, name -> name.startsWith("days.corrupt-") && name.endsWith(".json") }
        assertEquals(1, corruptFiles?.size ?: 0)
        assertEquals("corrupt garbage {not json", corruptFiles!![0].readText())
        assertFalse(daysFile.exists())
        assertTrue(store.hasCorruptFiles())
    }

    @Test
    fun secondCorruptionKeepsBothCopies() {
        val dir = tempFolder.newFolder()
        val daysFile = File(dir, "days.json")
        daysFile.writeText("first corrupt file")

        val store1 = DayLogStore(dir)
        store1.all()

        daysFile.writeText("second corrupt file")
        val store2 = DayLogStore(dir)
        store2.all()

        val corruptFiles = dir.listFiles { _, name -> name.startsWith("days.corrupt-") && name.endsWith(".json") }
        assertEquals(2, corruptFiles?.size ?: 0)
        assertTrue(corruptFiles!!.any { it.readText() == "first corrupt file" })
        assertTrue(corruptFiles.any { it.readText() == "second corrupt file" })
    }

    @Test
    fun writeIsNeverPartialWhenInterrupted() {
        val dir = tempFolder.newFolder()
        val daysFile = File(dir, "days.json")
        val store = DayLogStore(dir)

        store.markDone(
            date = LocalDate.of(2026, 10, 1),
            method = Method.TAPPED,
            trackId = "track-1",
            trackName = "Main",
        )
        val originalContent = daysFile.readText()
        assertTrue(originalContent.contains("2026-10-01"))

        // Simulate an interrupted write using AtomicFile failWrite
        val atomic = androidx.core.util.AtomicFile(daysFile)
        val stream = atomic.startWrite()
        stream.write("partial content".toByteArray())
        atomic.failWrite(stream)

        // The original file content must still be intact
        assertEquals(originalContent, daysFile.readText())
    }
}
