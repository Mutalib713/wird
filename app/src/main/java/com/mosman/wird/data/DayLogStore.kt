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

    /** One entry per day, newest last. */
    fun all(): List<DayLog> = read().map { it.log }

    /** The recording for a day, if that day was recited and the file is still there. */
    fun audioFor(date: LocalDate): File? =
        read().firstOrNull { it.log.date == date }
            ?.audio
            ?.let { File(audioDir, it) }
            ?.takeIf { it.exists() && it.length() > 0 }

    fun isDone(date: LocalDate): Boolean = read().any { it.log.date == date }

    fun methodFor(date: LocalDate): Method? = read().firstOrNull { it.log.date == date }?.log?.method

    /**
     * What a finished day actually covered.
     *
     * Recorded because "today's portion" must not change the moment you finish it. The
     * position advances on done, and if the screen recomputed from the live position the
     * page you had just read would go pale and the confirmation would vanish — which is
     * precisely what happened the first time this was tested.
     */
    fun coveredOn(date: LocalDate): Pair<Int, Int>? =
        read().firstOrNull { it.log.date == date }
            ?.let { row -> row.startUnit?.let { s -> row.units?.let { u -> s to u } } }

    /** Where a new recording should be written. Named by date, so a day has one. */
    fun audioFileFor(date: LocalDate): File = File(audioDir, "recitation-$date.m4a")

    /**
     * Mark a day done.
     *
     * Reciting wins over tapping, and marking a day twice does not create a second row —
     * the same rule `progressOf` applies to the maths, applied here so the file on disk
     * never disagrees with the numbers on screen.
     */
    fun markDone(
        date: LocalDate,
        method: Method,
        audio: File? = null,
        startUnit: Int? = null,
        units: Int? = null,
    ) {
        val rows = read().toMutableList()
        val existing = rows.indexOfFirst { it.log.date == date }
        val row = Row(
            log = DayLog(date, method),
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
            )
        } else {
            rows += row
        }
        write(rows)
    }

    /** Undo today. For the moment someone taps by accident. */
    fun clear(date: LocalDate) {
        val rows = read().filterNot { it.log.date == date }
        audioFileFor(date).delete()
        write(rows)
    }

    // ---- persistence -------------------------------------------------------

    private data class Row(
        val log: DayLog,
        val audio: String?,
        val startUnit: Int? = null,
        val units: Int? = null,
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
                            ),
                            audio = o.optString("audio").ifEmpty { null },
                            startUnit = if (o.has("startUnit")) o.getInt("startUnit") else null,
                            units = if (o.has("units")) o.getInt("units") else null,
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
                        r.audio?.let { put("audio", it) }
                        r.startUnit?.let { put("startUnit", it) }
                        r.units?.let { put("units", it) }
                    }
            )
        }
        file.writeText(arr.toString(2))
    }

    private companion object { const val TAG = "WirdDays" }
}
