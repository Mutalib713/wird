package com.mosman.wird.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** What an export turned out to contain, for the sentence shown after it runs. */
data class ExportResult(
    val file: File,
    val recordings: Int,
    val bytes: Long,
)

/** What is sitting on the phone right now, for the sentence shown before you decide. */
data class DataOnDevice(
    val recordings: Int,
    val recordingBytes: Long,
    val days: Int,
)

/**
 * Everything Wird knows about you, in one file you can carry off the phone. **PLAN task 19.**
 *
 * **Sacred Rule 1 is why this exists.** Nothing here syncs anywhere, which is the promise —
 * but a promise that your data never leaves the phone is only kind if you can *get it off*
 * when you want to. Otherwise "private" and "trapped" are the same thing.
 *
 * **A zip rather than the loose folder PLAN asks for**, and the reason is practical: a folder
 * of scattered files is several things to move and easy to half-copy, while a zip is one item
 * that keeps its structure and opens as a folder on any computer. The task's intent — *get my
 * data off this device in a form I can read* — is better served by the single file.
 *
 * **It includes a README.** An export that needs explaining is an export nobody reads, and a
 * folder of `days.json` and unnamed `.m4a` files is exactly that.
 */
object Export {

    suspend fun whatIsHere(context: Context): DataOnDevice = withContext(Dispatchers.IO) {
        val audio = File(context.filesDir, "recitations").listFiles().orEmpty()
        DataOnDevice(
            recordings = audio.size,
            recordingBytes = audio.sumOf { it.length() },
            days = DayLogStore(context).all().size,
        )
    }

    suspend fun run(context: Context, today: LocalDate = LocalDate.now()): ExportResult? =
        withContext(Dispatchers.IO) {
            runCatching {
                // The cache, not files/. An export is a copy made to be carried away; keeping
                // it forever would double the storage this screen exists to help you manage,
                // and Android may reclaim cache when the phone is tight, which is correct.
                val out = File(context.cacheDir, "export").apply { mkdirs() }
                out.listFiles()?.forEach { it.delete() }
                val zip = File(out, "wird-export-$today.zip")

                var recordings = 0
                ZipOutputStream(zip.outputStream().buffered()).use { z ->
                    z.putNextEntry(ZipEntry("README.txt"))
                    z.write(readme(today).toByteArray())
                    z.closeEntry()

                    listOf("days.json", "chat.json", "bookmarks.json").forEach { name ->
                        val f = File(context.filesDir, name)
                        if (f.exists()) {
                            z.putNextEntry(ZipEntry(name))
                            f.inputStream().use { it.copyTo(z) }
                            z.closeEntry()
                        }
                    }

                    File(context.filesDir, "recitations").listFiles().orEmpty()
                        .sortedBy { it.name }
                        .forEach { f ->
                            z.putNextEntry(ZipEntry("recitations/${f.name}"))
                            f.inputStream().use { it.copyTo(z) }
                            z.closeEntry()
                            recordings++
                        }
                }

                ExportResult(zip, recordings, zip.length())
            }.getOrElse {
                Log.w(TAG, "export failed", it)
                null
            }
        }

    /**
     * Delete the recordings, keeping the record of having recited.
     *
     * **⚠ This does not falsify anything, and the distinction matters.** The day log stores
     * *how* a day was finished — `RECITED` or `TAPPED` — separately from the audio. Deleting
     * the files removes the recordings, not the fact that you recited. Sacred Rule 6 is about
     * the app never claiming you did more than you did, and this claims nothing new.
     *
     * PROFILE.md § 5a asked for this: a page a day is ~118 MB of audio a year, and the honest
     * answer is a choice offered here rather than silent growth.
     */
    suspend fun deleteRecordings(context: Context): Int = withContext(Dispatchers.IO) {
        val files = File(context.filesDir, "recitations").listFiles().orEmpty()
        files.count { it.delete() }
    }

    private fun readme(today: LocalDate) = """
        Wird export, $today

        Everything this app knows about you. It was all on your phone and nowhere else.

        days.json        One row per day you marked done. `method` is RECITED if you recorded
                         yourself reciting, or TAPPED if you marked it read. Days you missed
                         are simply absent - there is no row saying you failed.
        chat.json        What you and the check-in said to each other.
        bookmarks.json   Ayahs you saved, by verse key. "18:10" is Al-Kahf, ayah 10.
        recitations/     Your recordings, one per day, named by date. Plain MP4 audio - any
                         player opens them.

        Nothing here was uploaded anywhere to produce this file.
    """.trimIndent()

    private const val TAG = "WirdExport"
}
