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
class DayLogStore(context: Context) {

    private val file = File(context.filesDir, "days.json")
    private val audioDir = File(context.filesDir, "recitations").apply { mkdirs() }

    /** One entry per day session, newest last. */
    fun all(): List<DayLog> = read().map { it.log }

    /** The recording for a day and track, if that day was recited and the file is still there. */
    fun audioFor(date: LocalDate, trackId: String? = null): File? {
        val rows = read()
        val matchingRow = if (trackId != null) {
            rows.firstOrNull { it.log.date == date && it.log.trackId == trackId }
                ?: rows.firstOrNull { it.log.date == date }
        } else {
            rows.firstOrNull { it.log.date == date }
        }
        return matchingRow?.audio
            ?.let { File(audioDir, it) }
            ?.takeIf { it.exists() && it.length() > 0 }
    }

    fun isDone(date: LocalDate): Boolean = read().any { it.log.date == date }

    fun isDone(date: LocalDate, trackId: String?): Boolean =
        if (trackId == null) isDone(date) else read().any { it.log.date == date && it.log.trackId == trackId }

    fun methodFor(date: LocalDate, trackId: String? = null): Method? {
        val rows = read()
        return if (trackId != null) {
            rows.firstOrNull { it.log.date == date && it.log.trackId == trackId }?.log?.method
                ?: rows.firstOrNull { it.log.date == date }?.log?.method
        } else {
            rows.firstOrNull { it.log.date == date }?.log?.method
        }
    }

    /**
     * What a finished day actually covered.
     */
    fun coveredOn(date: LocalDate, trackId: String? = null): Pair<Int, Int>? {
        val rows = read()
        val row = if (trackId != null) {
            rows.firstOrNull { it.log.date == date && it.log.trackId == trackId }
                ?: rows.firstOrNull { it.log.date == date }
        } else {
            rows.firstOrNull { it.log.date == date }
        }
        return row?.let { r -> r.startUnit?.let { s -> r.units?.let { u -> s to u } } }
    }

    /** Where a new recording should be written. Named by date and track so each track has its own. */
    fun audioFileFor(date: LocalDate, trackId: String? = null): File =
        if (trackId != null) File(audioDir, "recitation-$date-$trackId.m4a") else File(audioDir, "recitation-$date.m4a")

    fun transcriptionFor(date: LocalDate, trackId: String? = null): String? {
        val rows = read()
        return if (trackId != null) {
            rows.firstOrNull { it.log.date == date && it.log.trackId == trackId }?.transcription
                ?: rows.firstOrNull { it.log.date == date }?.transcription
        } else {
            rows.firstOrNull { it.log.date == date }?.transcription
        }
    }

    fun saveTranscription(date: LocalDate, text: String, trackId: String? = null) {
        val rows = read().toMutableList()
        val existing = if (trackId != null) {
            val idx = rows.indexOfFirst { it.log.date == date && it.log.trackId == trackId }
            if (idx >= 0) idx else rows.indexOfFirst { it.log.date == date }
        } else {
            rows.indexOfFirst { it.log.date == date }
        }
        if (existing >= 0) {
            rows[existing] = rows[existing].copy(transcription = text)
            write(rows)
        }
    }

    /**
     * Mark a day done.
     *
     * Reciting wins over tapping, and marking a day twice for the same track does not create a second row.
     */
    fun markDone(
        date: LocalDate,
        method: Method,
        audio: File? = null,
        startUnit: Int? = null,
        units: Int? = null,
        trackId: String? = null,
        trackName: String? = null,
    ) {
        val rows = read().toMutableList()
        val existing = if (trackId != null) {
            rows.indexOfFirst { it.log.date == date && (it.log.trackId == trackId || it.log.trackId == null) }
        } else {
            rows.indexOfFirst { it.log.date == date }
        }
        val row = Row(
            log = DayLog(date, method, trackId, trackName),
            audio = audio?.name,
            startUnit = startUnit,
            units = units,
        )
        if (existing >= 0) {
            val was = rows[existing]
            // Never downgrade a recitation to a tap.
            val keepRecited = was.log.method == Method.RECITED && method == Method.TAPPED
            rows[existing] = if (keepRecited) was else row.copy(
                audio = row.audio ?: was.audio,
                startUnit = row.startUnit ?: was.startUnit,
                units = row.units ?: was.units,
                transcription = was.transcription,
            )
        } else {
            rows += row
        }
        write(rows)
    }

    /** Undo today. For the moment someone taps by accident. */
    fun clear(date: LocalDate, trackId: String? = null) {
        val rows = read().filterNot {
            it.log.date == date && (trackId == null || it.log.trackId == null || it.log.trackId == trackId)
        }
        audioFileFor(date, trackId).delete()
        write(rows)
    }

    // ---- persistence -------------------------------------------------------

    private data class Row(
        val log: DayLog,
        val audio: String?,
        val startUnit: Int? = null,
        val units: Int? = null,
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
                        r.transcription?.let { put("transcription", it) }
                    }
            )
        }
        file.writeText(arr.toString(2))
    }

    private companion object { const val TAG = "WirdDays" }
}
