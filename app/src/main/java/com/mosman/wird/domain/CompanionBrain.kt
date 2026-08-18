package com.mosman.wird.domain

import java.time.LocalTime

/**
 * What the companion decided you meant.
 *
 * A closed set, on purpose. The companion can only ever ask for things this app already
 * does — it is a way of saying them in words, not a second set of powers. That is also what
 * makes it safe to ship before any model exists: an unrecognised sentence lands in
 * [NotUnderstood] and says so, rather than guessing at someone's wird.
 */
sealed interface CompanionAction {

    /** "after Isha", "at 9", "in an hour" — hold me to this. */
    data class CommitTo(val schedule: NudgeSchedule, val spoken: String) : CompanionAction

    /** "not today", "I'm travelling" — no reading today, and no guilt about it. */
    data object NotToday : CompanionAction

    /** "already did it", "I read it". */
    data object MarkDone : CompanionAction

    /** "how am I doing", "what's my streak". */
    data object HowAmIDoing : CompanionAction

    /** "where am I", "what page". */
    data object WhereAmI : CompanionAction

    /** "open Al-Kahf", "go to Ya-Sin". */
    data class OpenSurah(val surah: Surah) : CompanionAction

    /** "play it", "listen to today's". */
    data object Listen : CompanionAction

    /** Said nothing it recognised. Says so plainly rather than guessing. */
    data class NotUnderstood(val said: String) : CompanionAction
}

/**
 * Turns a sentence into something the app can do.
 *
 * **Deliberately rule-based, and that is the point of shipping it this way first.** PLAN
 * task 21: the assumption worth testing is whether naming a time to something that checks
 * back changes Mutalib's behaviour — not whether a model can parse English. If a handful of
 * phrasings move his 8-of-14 procrastination days, a model is obviously worth adding. If
 * they do not, no model was going to save it. The stack choice for that model is his to
 * make and has not been made.
 *
 * It also costs nothing, runs offline, needs no key, and **cannot invent anything** — which
 * matters more here than in most apps, because the thing it edits is a record of someone's
 * worship.
 *
 * **What it will never do:** anything about the Qur'an's meaning. PROFILE.md § 5h approved
 * tafsir and translation, but only *fetched and attributed* from a published source — never
 * from a model, and never from a parser like this one.
 */
object CompanionBrain {

    fun understand(said: String, now: LocalTime = LocalTime.now()): CompanionAction {
        val t = said.lowercase().trim()
        if (t.isEmpty()) return CompanionAction.NotUnderstood(said)

        // Order matters. "already read it" is a completion, not a commitment, even though
        // it contains "read" — so the most specific readings are tested first.
        done(t)?.let { return it }
        decline(t)?.let { return it }
        progress(t)?.let { return it }
        listen(t)?.let { return it }
        open(t)?.let { return it }
        commit(t, now)?.let { return it }
        return CompanionAction.NotUnderstood(said)
    }

    private fun done(t: String): CompanionAction? = when {
        t.has(
            "already did", "already read", "already done", "i've read", "ive read",
            "i have read", "just read", "just finished", "finished it", "i read it",
            "i did it",
        ) -> CompanionAction.MarkDone
        t == "done" || t == "finished" -> CompanionAction.MarkDone
        else -> null
    }

    private fun decline(t: String): CompanionAction? = when {
        t.has(
            "not today", "cant today", "can't today", "no today", "skip today",
            "not tonight", "travelling", "traveling", "too tired", "maybe tomorrow",
        ) -> CompanionAction.NotToday
        t == "no" || t == "nope" -> CompanionAction.NotToday
        else -> null
    }

    private fun progress(t: String): CompanionAction? = when {
        t.has(
            "how am i doing", "my streak", "how many days", "hows it going",
            "how's it going", "my progress", "how am i",
        ) -> CompanionAction.HowAmIDoing
        t.has(
            "where am i", "what page", "which page", "where was i", "where did i stop",
        ) -> CompanionAction.WhereAmI
        else -> null
    }

    private fun listen(t: String): CompanionAction? = when {
        t.has("play it", "play today", "listen to", "read it to me", "let me listen") ->
            CompanionAction.Listen
        t == "play" || t == "listen" -> CompanionAction.Listen
        else -> null
    }

    private fun open(t: String): CompanionAction? {
        if (!t.has("open", "go to", "take me to", "show me", "jump to")) return null
        // Longest name first, so a short name inside a longer one cannot win.
        val surah = SurahIndex.all
            .sortedByDescending { it.name.length }
            .firstOrNull { plain(t).contains(plain(it.name)) }
        return surah?.let { CompanionAction.OpenSurah(it) }
    }

    /**
     * "after Isha", "at 9", "in an hour".
     *
     * A named prayer becomes a real prayer-anchored schedule, which is why this earns its
     * place: task 8's scheduler already turns "after Isha" into an alarm that moves with the
     * sun, so the sentence lands on machinery that exists rather than on a promise.
     */
    private fun commit(t: String, now: LocalTime): CompanionAction? {
        PRAYERS.forEach { (word, prayer) ->
            if (t.contains(word)) {
                val offset = if (t.has("right after", "straight after")) 0 else 30
                return CompanionAction.CommitTo(
                    NudgeSchedule.AfterPrayer(prayer, offset),
                    spoken = "after ${prayer.label}",
                )
            }
        }

        Regex("""\b(?:at|by)\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""").find(t)?.let { m ->
            val raw = m.groupValues[1].toIntOrNull()
            val minute = m.groupValues[2].toIntOrNull() ?: 0
            val suffix = m.groupValues[3]
            if (raw != null) {
                // "at 9" said in the evening means nine tonight, not nine tomorrow. People
                // say the hour they mean, not the twenty-four-hour clock face.
                val hour = when {
                    suffix == "am" -> if (raw == 12) 0 else raw
                    suffix == "pm" -> if (raw == 12) 12 else raw + 12
                    raw in 1..11 && now.hour >= 12 -> raw + 12
                    else -> raw
                }
                if (hour in 0..23 && minute in 0..59) {
                    val at = LocalTime.of(hour, minute)
                    return CompanionAction.CommitTo(
                        NudgeSchedule.AtClockTime(at),
                        spoken = "at $at",
                    )
                }
            }
        }

        Regex("""in\s+(an|a|\d{1,2})\s*(hour|hr|minute|min)""").find(t)?.let { m ->
            val n = when (val g = m.groupValues[1]) {
                "a", "an" -> 1
                else -> g.toIntOrNull() ?: 1
            }
            val minutes = if (m.groupValues[2].startsWith("h")) n * 60 else n
            val at = now.plusMinutes(minutes.toLong())
            return CompanionAction.CommitTo(
                NudgeSchedule.AtClockTime(LocalTime.of(at.hour, at.minute)),
                spoken = if (minutes >= 60) "in ${minutes / 60} hour" else "in $minutes minutes",
            )
        }

        return null
    }

    private fun String.has(vararg needles: String) = needles.any { this.contains(it) }

    /** Surah names get written a dozen ways; compare on letters alone. */
    private fun plain(s: String) = s.lowercase().filter(Char::isLetterOrDigit)

    private val PRAYERS = listOf(
        "fajr" to Prayer.FAJR,
        "dhuhr" to Prayer.DHUHR, "zuhr" to Prayer.DHUHR,
        "asr" to Prayer.ASR,
        "maghrib" to Prayer.MAGHRIB,
        "isha" to Prayer.ISHA, "esha" to Prayer.ISHA,
    )
}
