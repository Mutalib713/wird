package com.mosman.wird.domain

import java.time.DayOfWeek
import java.time.LocalDate
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

    /**
     * "after Isha", "at 9", "in an hour" — hold me to this, **today**.
     *
     * ⚠ **One-off, corrected 2026-08-18 at Mutalib's word.** Until then a commitment
     * overwrote the daily reminder permanently, so a three o'clock *"in an hour"* quietly
     * made four o'clock the reminder time for every day after it. PLAN task 21 describes
     * this as re-arming "for that moment"; a promise about tonight is not a change of
     * routine. Permanent changes are [MoveReminder], and they have to be asked for.
     */
    data class CommitTo(val schedule: NudgeSchedule, val spoken: String) : CompanionAction

    /** "not today", "too tired" — no reading today, and no guilt about it. */
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

    /** "one page a day", "make it half a page". How much every day, in half pages. */
    data class ChangePlan(val units: Int) : CompanionAction

    /** "half on Fridays", "two pages on Saturdays". A null [units] means back to the usual. */
    data class ChangeDayPlan(val day: DayOfWeek, val units: Int?) : CompanionAction

    /** "move my reminder to 9", "stop reminding me". The routine, not just tonight. */
    data class MoveReminder(val schedule: NudgeSchedule) : CompanionAction

    /** "I'm travelling till Sunday". Quiet for a stretch of days, then back on its own. */
    data class PauseUntil(val away: AwayPeriod) : CompanionAction

    /** "I'm back". Ends a pause early. */
    data object Resume : CompanionAction

    /**
     * "what does 1:1 mean", "explain verse 5 of Al-Kahf", "what does Al-Kahf mean".
     *
     * ⚠ **This returns a TRANSLATION, never an explanation**, and the distinction is Sacred
     * Rule 2 rather than pedantry. The words come from the bundled Saheeh International text
     * with the translator named on screen; nothing here writes, summarises or interprets. A
     * tafsir would have to be fetched from a named scholar, and § 5h calls a wrong one the
     * highest-risk thing in the app — unlike a wrong tajweed mark, the reader cannot tell.
     *
     * A null [ayah] means a sūrah was named without one. That answers with the meaning of the
     * sūrah's **name** — "Al-Kahf means The Cave" — which is a fact about the title, not about
     * the contents, and is said that way.
     */
    data class ExplainVerse(val surah: Int, val ayah: Int?) : CompanionAction

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
 * they do not, no model was going to save it.
 *
 * **PLAN task 22 widened it from answering the nudge to editing the plan** — how much, which
 * days, when the reminder comes, and being away. Task 13 folded in here rather than becoming
 * a second parser, because "one page a day, half on Fridays" is the same sentence problem
 * whether it is typed during setup or typed six weeks later. His stack decision, 2026-08-18:
 * *"for now lets use the free private offline then later we use gemini."*
 *
 * It costs nothing, runs offline, needs no key, and **cannot invent anything** — which
 * matters more here than in most apps, because the thing it edits is a record of someone's
 * worship.
 *
 * **What it will never do:** anything about the Qur'an's meaning. PROFILE.md § 5h approved
 * tafsir and translation, but only *fetched and attributed* from a published source — never
 * from a model, and never from a parser like this one.
 */
object CompanionBrain {

    /**
     * Everything one sentence asked for, in the order it asked.
     *
     * **A person writes three instructions in one breath.** PLAN task 13's own example is
     * *"One page a day, half on Fridays, I'm travelling next week"*, which is three separate
     * edits wearing one sentence. Reading only the first would silently drop two of them,
     * and a schedule that is quietly a third of what was asked for is worse than one that
     * admits it did not follow.
     *
     * So a sentence with breaks in it is read clause by clause. **A clause it cannot read
     * stays in the list** as [CompanionAction.NotUnderstood] rather than being dropped,
     * because [replyForAll] then names it out loud. That is the one guarantee this parser
     * has to keep.
     */
    fun understandAll(
        said: String,
        now: LocalTime = LocalTime.now(),
        plan: ReadingPlan = ReadingPlan(),
        today: LocalDate = LocalDate.now(),
    ): List<CompanionAction> {
        val clauses = clauses(said)
        if (clauses.size < 2) return listOf(understand(said, now, plan, today))

        val read = clauses.map { it to understand(it, now, plan, today) }
        val understood = read
            .map { it.second }
            .filter { it !is CompanionAction.NotUnderstood }
            .distinct()

        // Nothing landed, so those commas were punctuation rather than a list. Read the
        // whole thing again as one sentence and let it fail as one sentence.
        if (understood.isEmpty()) return listOf(understand(said, now, plan, today))

        val missed = read
            .filter { it.second is CompanionAction.NotUnderstood && !isPleasantry(it.first) }
            .map { CompanionAction.NotUnderstood(it.first) }

        return understood + missed
    }

    /** One instruction, read from the whole string. */
    fun understand(
        said: String,
        now: LocalTime = LocalTime.now(),
        plan: ReadingPlan = ReadingPlan(),
        today: LocalDate = LocalDate.now(),
    ): CompanionAction {
        val t = said.lowercase().trim()
        if (t.isEmpty()) return CompanionAction.NotUnderstood(said)

        // Order matters, and every line of it is a collision found by writing it down.
        // "already read it" is a completion, not a commitment, though it contains "read".
        // "travelling till Sunday" is a pause, not a refusal, though it contains
        // "travelling". "I'm back on Friday" is a pause, not a resume, though it contains
        // "I'm back". The most specific reading is always tested first.
        done(t)?.let { return it }
        pause(t, today)?.let { return it }
        resume(t)?.let { return it }
        decline(t)?.let { return it }
        progress(t)?.let { return it }
        listen(t)?.let { return it }
        explain(t)?.let { return it }
        open(t)?.let { return it }
        moveReminder(t, now)?.let { return it }
        dayPlan(t, plan)?.let { return it }
        planChange(t, plan)?.let { return it }
        commit(t, now)?.let { return it }
        return CompanionAction.NotUnderstood(said)
    }

    // ---- what was said ----

    private fun done(t: String): CompanionAction? = when {
        t.has(
            "already did", "already read", "already done", "i've read", "ive read",
            "i have read", "just read", "just finished", "finished it", "i read it",
            "i did it", "recited", "i recited", "recited it", "i recited it", "yes, i recited it",
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
        return matchSurah(t)?.let { CompanionAction.OpenSurah(it) }
    }

    /**
     * "explain verse 1 of Fatiha", "what does 2:255 mean", "translate 18:10".
     *
     * **Asking is not enough on its own — it has to name something.** A bare "what does this
     * mean" has no ayah in it, and guessing that it meant today's first verse would be the
     * parser inventing a question it was not asked. That falls through to [CompanionAction
     * .NotUnderstood], whose reply now names this as one of the things it *can* do.
     */
    private fun explain(t: String): CompanionAction? {
        val asking = t.has(
            "explain", "what does", "what is", "meaning of", "translate", "translation of",
            "means", "tafsir", "tafseer",
        )
        if (!asking) return null

        // "2:255" is the least ambiguous form and the cheapest to read, so it wins.
        Regex("\\b(\\d{1,3}):(\\d{1,3})\\b").find(t)?.let { m ->
            val surah = m.groupValues[1].toInt()
            val ayah = m.groupValues[2].toInt()
            SurahIndex.byNumber(surah)?.let { return CompanionAction.ExplainVerse(surah, ayah) }
        }

        // Otherwise a sūrah by name, with an ayah number somewhere near it if there is one.
        val surah = matchSurah(t) ?: return null

        val ayah = Regex("(?:verse|ayah|aya|ayat)\\s+(\\d{1,3})").find(t)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("(\\d{1,3})(?:st|nd|rd|th)?\\s+(?:verse|ayah)").find(t)?.groupValues?.get(1)?.toIntOrNull()
        return CompanionAction.ExplainVerse(surah.number, ayah)
    }

    /**
     * "after Isha", "at 9", "in an hour" — tonight only.
     *
     * A named prayer becomes a real prayer-anchored schedule, which is why this earns its
     * place: task 8's scheduler already turns "after Isha" into an alarm that moves with the
     * sun, so the sentence lands on machinery that exists rather than on a promise.
     */
    private fun commit(t: String, now: LocalTime): CompanionAction? =
        readTime(t, now)?.let { CompanionAction.CommitTo(it.first, it.second) }

    /**
     * The routine, not tonight. **Saying so is the whole difference.**
     *
     * A time with no marker of permanence is read as a promise about today, which is the
     * commoner thing to mean and the safer thing to be wrong about: a one-off that should
     * have been permanent costs one repeat, while a permanent change that should have been
     * one-off rewrites a routine nobody asked to move.
     */
    private fun moveReminder(t: String, now: LocalTime): CompanionAction? {
        if (t.has("stop reminding", "no more reminders", "turn off the remind", "stop the remind") &&
            !t.contains("today")
        ) {
            return CompanionAction.MoveReminder(NudgeSchedule.Off)
        }

        val permanent = t.has(
            "from now on", "every day", "everyday", "always", "move my remind",
            "change my remind", "set my remind", "make my remind", "move the remind",
            "remind me every", "instead of", "usually", "normally",
        )
        if (!permanent) return null
        return readTime(t, now)?.let { CompanionAction.MoveReminder(it.first) }
    }

    /**
     * "half on Fridays", "make Fridays lighter", "back to normal on Fridays".
     *
     * The day is found first and then **struck out of the sentence**, so the amount reads by
     * the same rules whether it was written before the day or after it. That is cheaper than
     * a pattern per phrasing, and it does not fall over on the order people actually type.
     */
    private fun dayPlan(t: String, plan: ReadingPlan): CompanionAction? {
        val day = DAYS.entries.firstOrNull { t.contains(it.key) }?.value ?: return null
        val rest = DAYS.keys.fold(t) { acc, name -> acc.replace(name, " ") }
            .replace(Regex("\\b(on|for|every|each|the|make|s)\\b"), " ")

        if (rest.has("back to normal", "normal", "as usual", "same as", "like the rest")) {
            return CompanionAction.ChangeDayPlan(day, null)
        }
        val units = readUnits(rest) ?: relativeUnits(rest, plan.unitsOn(day)) ?: return null
        return CompanionAction.ChangeDayPlan(day, units)
    }

    /**
     * "one page a day", "make it half a page".
     *
     * ⚠ **An amount on its own is not an instruction.** Bare "two pages" in reply to *"reading
     * today?"* could as easily be a report of what was just read, so this asks for a word that
     * says it is about every day: "a day", "daily", "make it", "from now on". Guessing would
     * rewrite a plan on the strength of a sentence that never asked.
     */
    private fun planChange(t: String, plan: ReadingPlan): CompanionAction? {
        val meansEveryDay = t.has(
            "a day", "per day", "each day", "every day", "everyday", "daily",
            "make it", "change it", "change to", "i want", "id like", "i'd like",
            "lets do", "let's do", "from now on", "instead",
        )
        if (!meansEveryDay) return null
        val units = readUnits(t) ?: relativeUnits(t, plan.defaultUnits) ?: return null
        return CompanionAction.ChangePlan(units)
    }

    /**
     * "I'm travelling till Sunday", "away for 3 days", "back on Friday", "next week".
     *
     * **"Till Sunday" means the reminder comes back on Sunday**, not the day after. English
     * is genuinely ambiguous there, so the rule is written down here and [replyForAll] says
     * the day back out loud, which makes a wrong reading cost one sentence to correct.
     *
     * A travel word with no end date is **not** a pause. "I'm travelling" on its own falls
     * through to [decline] and means today, which is the smaller claim to make about someone
     * else's week.
     */
    private fun pause(t: String, today: LocalDate): CompanionAction? {
        val awayWord = t.has(
            "travelling", "traveling", "away", "out of town", "on a trip", "not around",
            "wont be around", "won't be around", "back on", "no reminders",
        )
        if (!awayWord) return null

        val nextWeek = t.contains("next week")
        val returnsOn: LocalDate? = when {
            nextWeek -> nextWeekday(DayOfWeek.MONDAY, today).plusDays(7)
            t.contains("this week") -> nextWeekday(DayOfWeek.MONDAY, today)
            else -> Regex("(?:till|til|until|by|back on|back)\\s+(?:the\\s+)?([a-z']+)")
                .find(t)
                ?.groupValues?.get(1)
                ?.let { word -> DAYS.entries.firstOrNull { it.key == word }?.value }
                ?.let { nextWeekday(it, today) }
        }

        val from: LocalDate
        val until: LocalDate
        when {
            // "Travelling next week" starts next Monday. Silencing this week as well would
            // take days he never asked for.
            returnsOn != null && nextWeek -> {
                from = nextWeekday(DayOfWeek.MONDAY, today)
                until = returnsOn.minusDays(1)
            }
            returnsOn != null -> {
                from = today
                until = returnsOn.minusDays(1)
            }
            else -> {
                val days = Regex("for\\s+(a|an|one|two|three|\\d{1,2})\\s*(day|week)").find(t)
                    ?.let { m ->
                        val n = WORDS[m.groupValues[1]] ?: m.groupValues[1].toIntOrNull() ?: 1
                        if (m.groupValues[2] == "week") n * 7 else n
                    } ?: return null
                from = today
                until = today.plusDays((days - 1).toLong())
            }
        }

        if (until.isBefore(from) || until.isAfter(today.plusDays(MAX_AWAY_DAYS))) return null
        return CompanionAction.PauseUntil(AwayPeriod(from, until))
    }

    private fun resume(t: String): CompanionAction? = when {
        t.has(
            "im back", "i'm back", "i am back", "back now", "back early", "im home",
            "i'm home", "resume", "start again", "reminders back on",
        ) -> CompanionAction.Resume
        else -> null
    }

    // ---- reading the pieces ----

    /** A schedule and the words that asked for it, or null if no time was named. */
    private fun readTime(t: String, now: LocalTime): Pair<NudgeSchedule, String>? {
        PRAYERS.forEach { (word, prayer) ->
            if (t.contains(word)) {
                val offset = if (t.has("right after", "straight after")) 0 else 30
                return NudgeSchedule.AfterPrayer(prayer, offset) to "after ${prayer.label}"
            }
        }

        Regex("\\b(?:at|by|to)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?").find(t)?.let { m ->
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
                    return NudgeSchedule.AtClockTime(at) to "at $at"
                }
            }
        }

        Regex("in\\s+(an|a|\\d{1,2})\\s*(hour|hr|minute|min)").find(t)?.let { m ->
            val n = when (val g = m.groupValues[1]) {
                "a", "an" -> 1
                else -> g.toIntOrNull() ?: 1
            }
            val minutes = if (m.groupValues[2].startsWith("h")) n * 60 else n
            val at = now.plusMinutes(minutes.toLong())
            return NudgeSchedule.AtClockTime(LocalTime.of(at.hour, at.minute)) to
                if (minutes >= 60) "in ${minutes / 60} hour" else "in $minutes minutes"
        }

        return null
    }

    /**
     * How much, in half pages.
     *
     * Half pages rather than fractions because [ReadingPlan] counts in them, and because
     * halves stored as 0.5 drift: enough of them add up to 1.4999 in something that has to
     * land exactly on a page boundary.
     */
    private fun readUnits(t: String): Int? {
        val n = { s: String -> WORDS[s] ?: s.toIntOrNull() }
        if (Regex("half\\s+(?:a\\s+)?page").containsMatchIn(t)) return 1
        Regex("($NUMBER)\\s+and\\s+a\\s+half\\s+pages?").find(t)
            ?.let { m -> n(m.groupValues[1])?.let { return sane(it * 2 + 1) } }
        Regex("($NUMBER)\\s+pages?\\s+and\\s+a\\s+half").find(t)
            ?.let { m -> n(m.groupValues[1])?.let { return sane(it * 2 + 1) } }
        Regex("($NUMBER)\\s+pages?").find(t)
            ?.let { m -> n(m.groupValues[1])?.let { return sane(it * 2) } }
        return null
    }

    /**
     * "half", "two", "lighter" — an amount with the word "page" left out.
     *
     * Only ever consulted after [readUnits] has failed, and deliberately blind to digits:
     * "at 9 on Fridays" must not become nine pages on Fridays. A bare number in a sentence
     * about time is a time.
     */
    private fun relativeUnits(t: String, current: Int): Int? = when {
        t.has("lighter", "less", "shorter", "smaller", "easier") -> maxOf(1, current / 2)
        t.has("heavier", "more", "longer", "bigger") -> sane(current * 2)

        // ⚠ **A digit anywhere means a number was meant to be read, and was not.** Found
        // by the check that says it must not guess: "300 pages a day" reached here, matched
        // the "a" in "a day", and confidently answered "one page a day". A number this
        // cannot parse has to fail, not fall back on a word that happened to sit nearby.
        t.any(Char::isDigit) -> null

        else -> Regex("\\b(half|one|two|three|four|five)\\b").find(t)?.let { m ->
            val word = m.groupValues[1]
            if (word == "half") 1 else WORDS[word]?.let { sane(it * 2) }
        }
    }

    private fun sane(units: Int): Int? = units.takeIf { it in 1..MAX_UNITS }

    /** The next date with this weekday, never today — "till Friday" said on Friday is a week. */
    private fun nextWeekday(day: DayOfWeek, today: LocalDate): LocalDate {
        var d = today.plusDays(1)
        while (d.dayOfWeek != day) d = d.plusDays(1)
        return d
    }

    /**
     * One sentence, split where a person actually put a break.
     *
     * ⚠ **"and" is only a break when it is not part of an amount.** Splitting "a page and a
     * half" would turn one instruction into two halves that each mean nothing, so the
     * lookahead leaves that one alone.
     */
    private fun clauses(said: String): List<String> =
        said.split(Regex("[,;]|\\bthen\\b|\\band\\b(?!\\s+(?:a\\s+)?half)"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun isPleasantry(clause: String) =
        clause.lowercase().trim().trim('.', '!') in PLEASANTRIES

    private fun String.has(vararg needles: String) = needles.any { this.contains(it) }

    /** Surah names get written a dozen ways; compare on letters alone. */
    private fun plain(s: String) = s.lowercase().filter(Char::isLetterOrDigit)

    /**
     * A sūrah named in a sentence, however it was spelled.
     *
     * ⚠ **"fatiha" did not match "Al-Fatihah", and that is how this was found** — he typed
     * *"so explain verse 1 of fatiha"* and the parser shrugged. Nobody types the index's
     * transliteration. Two forms are therefore tried: the full name, and a **core** with the
     * `al-` article and one trailing `h` removed, which is what turns "Al-Fatihah" into
     * "fatiha".
     *
     * The core is only used when it is at least four letters long. Shorter than that and it
     * starts matching inside unrelated words — "Ta-Ha" would core down to "ta", which appears
     * in half the sentences anyone types.
     *
     * Longest name first, so a short name inside a longer one cannot win.
     */
    private fun matchSurah(t: String): Surah? {
        val text = plain(t)
        val byName = SurahIndex.all.sortedByDescending { it.name.length }
        byName.firstOrNull { text.contains(plain(it.name)) }?.let { return it }
        return byName.firstOrNull { s ->
            val core = plain(s.name).removePrefix("al").removeSuffix("h")
            core.length >= 4 && text.contains(core)
        }
    }

    /** Twenty pages a day is already more than anyone in § 2 reads. Past that is a typo. */
    private const val MAX_UNITS = 40
    private const val MAX_AWAY_DAYS = 60L
    private const val NUMBER = "\\d{1,2}|one|two|three|four|five|a|an"

    private val WORDS = mapOf(
        "a" to 1, "an" to 1, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
    )

    /** Said out of politeness, not as an instruction. Dropped without comment. */
    private val PLEASANTRIES = setOf(
        "thanks", "thank you", "ok", "okay", "please", "cool", "sure",
        "inshallah", "in sha allah", "insha allah", "bismillah", "alhamdulillah",
    )

    private val PRAYERS = listOf(
        "fajr" to Prayer.FAJR,
        "dhuhr" to Prayer.DHUHR, "zuhr" to Prayer.DHUHR,
        "asr" to Prayer.ASR,
        "maghrib" to Prayer.MAGHRIB,
        "isha" to Prayer.ISHA, "esha" to Prayer.ISHA,
    )

    /** Jumu'ah is Friday, and it is likelier than "Friday" in his own writing. */
    private val DAYS = linkedMapOf(
        "monday" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY,
        "jumuah" to DayOfWeek.FRIDAY,
        "jummah" to DayOfWeek.FRIDAY,
        "jumaa" to DayOfWeek.FRIDAY,
        "juma" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY,
    )
}
