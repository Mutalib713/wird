package com.mosman.wird.data

import android.content.Context
import android.util.Log
import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Method
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * Every day you finished, and how.
 *
 * A plain JSON file rather than a database: this is one short row per day, and the whole
 * year fits in a few kilobytes. It is also the thing task 19 exports, and a file you can
 * read with your own eyes is a better promise than a table you have to be shown.
 *
 * Missed days are simply absent. There is no row saying you failed.
 */
class DayLogStore(filesDir: File) {

    /** The app's own constructor. The [File] one exists so the tests can use a temp folder. */
    constructor(context: Context) : this(context.filesDir)

    private val file = File(filesDir, "days.json")
    private val audioDir = File(filesDir, "recitations").apply { mkdirs() }

    /** One entry per day session, newest last. */
    fun all(): List<DayLog> = read().map { it.log }

    // **Every lookup below is per track, and strict.** Until 2026-10-03 each one fell back to
    // "any row for that date" when the track had none, so a track you had not read showed
    // another track's day as done, its portion, and its recording. A track's day is the row
    // with that track's id, and nothing else. Rows from before tracks existed are handed to
    // a track once, by [adoptUnownedRows], instead of being matched by every track forever.

    private fun rowFor(rows: List<Row>, date: LocalDate, trackId: String): Row? =
        rows.firstOrNull { it.log.date == date && it.log.trackId == trackId }

    /** The recording for a day and track, if that day was recited and the file is still there. */
    fun audioFor(date: LocalDate, trackId: String): File? =
        rowFor(read(), date, trackId)?.audio
            ?.let { File(audioDir, it) }
            ?.takeIf { it.exists() && it.length() > 0 }

    fun isDone(date: LocalDate, trackId: String): Boolean = rowFor(read(), date, trackId) != null

    fun methodFor(date: LocalDate, trackId: String): Method? = rowFor(read(), date, trackId)?.log?.method

    /**
     * For a day whose portion was a number of verses: the first verse and how many. Null for a
     * page portion. Kept so a finished verse portion still shows exactly what was read.
     */
    fun coveredVerses(date: LocalDate, trackId: String): Pair<Pair<Int, Int>, Int>? =
        rowFor(read(), date, trackId)?.let { r ->
            val first = r.firstVerse?.split(':')?.mapNotNull { it.toIntOrNull() }
            val count = r.verseCount
            if (first != null && first.size == 2 && count != null && count > 0) (first[0] to first[1]) to count else null
        }

    /** What a finished day actually covered, as (start unit, units). */
    fun coveredOn(date: LocalDate, trackId: String): Pair<Int, Int>? =
        rowFor(read(), date, trackId)?.let { r -> r.startUnit?.let { s -> r.units?.let { u -> s to u } } }

    /**
     * Where a new recording should be written: one file per day **and track**. Two tracks
     * recited on the same day used to share one file, so the second overwrote the first.
     */
    fun audioFileFor(date: LocalDate, trackId: String): File =
        File(audioDir, "recitation-$date-$trackId.m4a")

    fun transcriptionFor(date: LocalDate, trackId: String): String? =
        rowFor(read(), date, trackId)?.transcription

    fun saveTranscription(date: LocalDate, text: String, trackId: String) {
        val rows = read().toMutableList()
        val i = rows.indexOfFirst { it.log.date == date && it.log.trackId == trackId }
        if (i >= 0) {
            rows[i] = rows[i].copy(transcription = text)
            write(rows)
        }
    }

    /**
     * Mark a day done for one track.
     *
     * Reciting wins over tapping, and marking a day twice for the same track does not create a
     * second row.
     */
    fun markDone(
        date: LocalDate,
        method: Method,
        audio: File? = null,
        startUnit: Int? = null,
        units: Int? = null,
        trackId: String,
        firstVerse: Pair<Int, Int>? = null,
        verseCount: Int? = null,
        trackName: String? = null,
    ) {
        val rows = read().toMutableList()
        val existing = rows.indexOfFirst { it.log.date == date && it.log.trackId == trackId }
        val row = Row(
            log = DayLog(date, method, trackId, trackName),
            audio = audio?.name,
            startUnit = startUnit,
            units = units,
            firstVerse = firstVerse?.let { "${it.first}:${it.second}" },
            verseCount = verseCount,
        )
        if (existing >= 0) {
            val was = rows[existing]
            // Never downgrade a recitation to a tap.
            val keepRecited = was.log.method == Method.RECITED && method == Method.TAPPED
            rows[existing] = if (keepRecited) was else row.copy(
                audio = row.audio ?: was.audio,
                startUnit = row.startUnit ?: was.startUnit,
                units = row.units ?: was.units,
                firstVerse = row.firstVerse ?: was.firstVerse,
                verseCount = row.verseCount ?: was.verseCount,
                transcription = was.transcription,
            )
        } else {
            rows += row
        }
        write(rows)
    }

    /**
     * Undo one track's day. For the moment someone taps by accident.
     *
     * Only that track's row goes, and only its own recording. This used to be called without a
     * track and removed **every** track's row for the day.
     */
    fun clear(date: LocalDate, trackId: String) {
        val rows = read()
        val gone = rowFor(rows, date, trackId) ?: return
        gone.audio?.let { File(audioDir, it).delete() }
        write(rows.filterNot { it === gone })
    }

    /**
     * Give every row with no track to [ownerId]. Returns how many it moved.
     *
     * Rows written before reading tracks existed (before 2026-09-23) have no track id. They
     * were all the one reading plan there was, which is the track the app made from it.
     * Run once; after that every row has an owner and lookups can be strict.
     */
    fun adoptUnownedRows(ownerId: String): Int {
        val rows = read()
        val unowned = rows.count { it.log.trackId == null }
        if (unowned == 0) return 0
        write(rows.map { r -> if (r.log.trackId == null) r.copy(log = r.log.copy(trackId = ownerId)) else r })
        return unowned
    }

    // ---- persistence -------------------------------------------------------

    private data class Row(
        val log: DayLog,
        val audio: String?,
        val startUnit: Int? = null,
        val units: Int? = null,
        /** "sūrah:ayah" of a verse portion's first verse. Null for a page portion. */
        val firstVerse: String? = null,
        val verseCount: Int? = null,
        val transcription: String? = null,
    )

    private fun read(): List<Row> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Row(
                            log = DayLog(
                                date = LocalDate.parse(o.getString("date")),
                                method = Method.valueOf(o.getString("method")),
                                trackId = o.optString("trackId").ifEmpty { null },
                                trackName = o.optString("trackName").ifEmpty { null },
                            ),
                            audio = o.optString("audio").ifEmpty { null },
                            startUnit = if (o.has("startUnit")) o.getInt("startUnit") else null,
                            units = if (o.has("units")) o.getInt("units") else null,
                            firstVerse = o.optString("firstVerse").ifEmpty { null },
                            verseCount = if (o.has("verseCount")) o.getInt("verseCount") else null,
                            transcription = o.optString("transcription").ifEmpty { null },
                        )
                    )
                }
            }
        }.getOrElse {
            // A corrupt log must not take the app down, and must not silently look like
            // "you have never read". Keep the file for task 19 to export and say so.
            Log.w(TAG, "days.json unreadable, keeping it aside", it)
            file.renameTo(File(file.parentFile, "days.corrupt.json"))
            emptyList()
        }
    }

    private fun write(rows: List<Row>) {
        val arr = JSONArray()
        rows.sortedBy { it.log.date }.forEach { r ->
            arr.put(
                JSONObject()
                    .put("date", r.log.date.toString())
                    .put("method", r.log.method.name)
                    .apply {
                        r.log.trackId?.let { put("trackId", it) }
                        r.log.trackName?.let { put("trackName", it) }
                        r.audio?.let { put("audio", it) }
                        r.startUnit?.let { put("startUnit", it) }
                        r.units?.let { put("units", it) }
                        r.firstVerse?.let { put("firstVerse", it) }
                        r.verseCount?.let { put("verseCount", it) }
                        r.transcription?.let { put("transcription", it) }
                    }
            )
        }
        file.writeText(arr.toString(2))
    }

    private companion object { const val TAG = "WirdDays" }
}
