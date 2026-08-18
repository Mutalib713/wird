package com.mosman.wird.data

import android.util.Log
import com.mosman.wird.domain.Speaker
import com.mosman.wird.domain.Turn
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime

/**
 * The conversation, on this phone.
 *
 * **Plain JSON, same as `days.json` and for the same reasons:** it can be read with your own
 * eyes, it needs no database, and task 19 (export everything) gets something it can hand you
 * without a migration. Sacred Rule 1 — it never leaves the device.
 *
 * **Capped at [MAX_TURNS].** A chat that grows forever is a file that grows forever, on an
 * app whose whole discipline is that size is measured rather than assumed. The oldest turns
 * are dropped, which is the right end to lose: this is a running check-in, not an archive,
 * and § 5m's design note is explicit that the companion *"only speaks twice"* a day.
 */
class ConversationStore(private val filesDir: File) {

    private val file = File(filesDir, "chat.json")

    fun all(): List<Turn> = read()

    /** Append a line and trim the front. Returns the conversation as it now stands. */
    fun say(who: Speaker, text: String, at: LocalDateTime = LocalDateTime.now()): List<Turn> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return read()
        val next = (read() + Turn(who, trimmed, at)).takeLast(MAX_TURNS)
        write(next)
        return next
    }

    /** For the moment someone wants a clean slate. Nothing else in the app depends on it. */
    fun clear() {
        file.delete()
    }

    // ---- persistence -------------------------------------------------------

    private fun read(): List<Turn> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Turn(
                            who = Speaker.valueOf(o.getString("who")),
                            text = o.getString("text"),
                            at = LocalDateTime.parse(o.getString("at")),
                        )
                    )
                }
            }
        }.getOrElse {
            // Same rule the day log follows: a corrupt file must not take the app down, and
            // must not silently look like "you never spoke to it". Keep it aside and say so.
            Log.w(TAG, "chat.json unreadable, keeping it aside", it)
            file.renameTo(File(filesDir, "chat.corrupt.json"))
            emptyList()
        }
    }

    private fun write(turns: List<Turn>) {
        val arr = JSONArray()
        turns.forEach { t ->
            arr.put(
                JSONObject()
                    .put("who", t.who.name)
                    .put("text", t.text)
                    .put("at", t.at.toString())
            )
        }
        runCatching { file.writeText(arr.toString(2)) }
            .onFailure { Log.w(TAG, "couldn't write chat.json", it) }
    }

    private companion object {
        const val TAG = "WirdChat"

        /** Roughly a month of a companion that speaks twice a day, plus your replies. */
        const val MAX_TURNS = 120
    }
}
