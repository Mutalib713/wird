package com.mosman.wird.data

import android.util.Log
import com.mosman.wird.domain.Bookmark
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime

/**
 * Ayahs you saved, on this phone.
 *
 * Plain JSON beside `days.json` and `chat.json`, for the same three reasons: it can be read
 * with your own eyes, it needs no database, and PLAN task 19 (export everything) gets
 * something it can hand you without a migration. Sacred Rule 1 — it never leaves the device.
 *
 * **Newest first, because that is the only order anyone wants.** A bookmark list sorted by
 * surah is a table of contents you already have; sorted by when you saved it, it is a record
 * of what you were thinking about.
 *
 * **No folders, no tags, no notes.** The reference's toolbar has a tag button and Wird does
 * not, deliberately: an ayah you saved is either still worth returning to or it is not, and
 * every organising feature is a thing to maintain instead of a thing to read.
 */
class BookmarkStore(private val filesDir: File) {

    private val file = File(filesDir, "bookmarks.json")

    /** Newest first. */
    fun all(): List<Bookmark> = read().sortedByDescending { it.savedAt }

    fun has(verseKey: String): Boolean = read().any { it.verseKey == verseKey }

    /**
     * Save it, or unsave it. Returns true when it is now saved.
     *
     * One control rather than two: the toolbar has room for a single bookmark icon, and a
     * separate "remove" would mean knowing the state before you could act on it.
     */
    fun toggle(verseKey: String, at: LocalDateTime = LocalDateTime.now()): Boolean {
        val current = read()
        val existing = current.firstOrNull { it.verseKey == verseKey }
        return if (existing != null) {
            write(current - existing)
            false
        } else {
            write(current + Bookmark(verseKey, at))
            true
        }
    }

    // ---- persistence -------------------------------------------------------

    private fun read(): List<Bookmark> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Bookmark(
                            verseKey = o.getString("verse"),
                            savedAt = LocalDateTime.parse(o.getString("at")),
                        )
                    )
                }
            }
        }.getOrElse {
            // Same rule the other two stores follow: a corrupt file must not take the app
            // down, and must not silently look like "you never saved anything".
            Log.w(TAG, "bookmarks.json unreadable, keeping it aside", it)
            file.renameTo(File(filesDir, "bookmarks.corrupt.json"))
            emptyList()
        }
    }

    private fun write(marks: List<Bookmark>) {
        val arr = JSONArray()
        marks.forEach { b ->
            arr.put(JSONObject().put("verse", b.verseKey).put("at", b.savedAt.toString()))
        }
        runCatching { file.writeText(arr.toString(2)) }
            .onFailure { Log.w(TAG, "couldn't write bookmarks.json", it) }
    }

    private companion object { const val TAG = "WirdBookmarks" }
}
