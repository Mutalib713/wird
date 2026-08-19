package com.mosman.wird.domain

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * What the companion says back.
 *
 * **Moved out of `HomeScreen` on 2026-08-18** when the chat became a real conversation
 * ([com.mosman.wird.ui.ChatScreen]): the same sentence now has to appear in two places and be
 * written to the log, so it cannot live inside one screen's private helpers. Pure function,
 * so it is testable without Compose.
 *
 * **It repeats the commitment in its own words rather than saying "OK".** That is the whole
 * mechanic: *"I'll hold you to after Isha"* is a thing you can fail to do, and being told
 * your own promise back is what makes it one. "Saved" would turn this into a form.
 *
 * **PLAN task 22 made that repetition load-bearing rather than nice.** The parser now edits
 * the plan itself, and it reads amounts and dates out of ordinary English that is genuinely
 * ambiguous — "till Sunday" being the clearest case. Saying the result back in full is what
 * turns a wrong reading into one sentence to correct instead of a schedule that is quietly
 * wrong for a fortnight.
 *
 * Sacred Rule 3 governs every line. A refusal gets no argument and no guilt — the app says
 * fine and gets out of the way, because the day someone is told off is the day they delete a
 * habit app.
 */
fun replyFor(
    action: CompanionAction,
    progress: Progress?,
    positionLabel: String,
    today: LocalDate = LocalDate.now(),
): String = when (action) {
    is CompanionAction.CommitTo -> "Alright. I'll ask again ${action.spoken}."
    is CompanionAction.NotToday -> "That's fine. It'll be here tomorrow."
    is CompanionAction.MarkDone -> "Good. Marked as read."
    is CompanionAction.Listen -> "Playing today's portion."
    is CompanionAction.OpenSurah -> "Opening ${action.surah.name}."
    is CompanionAction.WhereAmI ->
        if (positionLabel.isNotEmpty()) "You're at $positionLabel." else "Let me check the page."
    is CompanionAction.HowAmIDoing -> progress?.let { p ->
        val total = if (p.totalDaysRead == 1) "1 day" else "${p.totalDaysRead} days"
        val streak = if (p.currentStreak > 1) "${p.currentStreak} in a row, " else ""
        "$streak$total read. ${p.recitedDays} recited aloud, ${p.tappedDays} marked."
    } ?: "Nothing recorded yet."

    // The amount is said back in words rather than confirmed, for the reason in the header:
    // "a page and a half" is how you find out it heard "one and a half pages" as three.
    is CompanionAction.ChangePlan ->
        "${sentenceCase(unitsLabel(action.units))} a day from now on."

    is CompanionAction.ChangeDayPlan -> {
        val day = "${dayName(action.day)}s"
        if (action.units == null) {
            "$day go back to your usual amount."
        } else {
            "$day are ${unitsLabel(action.units)} now."
        }
    }

    is CompanionAction.MoveReminder -> when (action.schedule) {
        is NudgeSchedule.Off -> "Reminders are off. Ask any time to turn them back on."
        else -> "From now on I'll ask ${lowerFirst(action.schedule.label())}."
    }

    // **The return day is named, never the length of the trip.** "Away for 6 days" is
    // arithmetic the reader has to do to check; a weekday is something they can just read.
    is CompanionAction.PauseUntil -> {
        val back = dayLabel(action.away.returnsOn, today)
        if (action.away.from == today) {
            "No reminders till $back. I'll ask again then."
        } else {
            "Quiet from ${dayLabel(action.away.from, today)}. I'll ask again on $back."
        }
    }

    is CompanionAction.Resume -> "Reminders are back on."

    // Answered upstairs, because the words live in a bundled asset and reading one is IO.
    // Returning an empty string here would put a blank turn in the conversation, so this
    // action is filtered out before [replyForAll] ever sees it.
    is CompanionAction.ExplainVerse -> ""

    // **The most important string in the companion**, and the reason it is written this way.
    //
    // A chat box invites anyone to type anything, and what is underneath is a handful of
    // hand-written rules — PROFILE.md § 5m records that gap honestly. So the failure reply
    // does not apologise and does not pretend; it names what it *does* understand, which
    // turns a dead end into a menu. An "I don't understand" with no examples is how someone
    // decides the feature is broken and stops typing.
    is CompanionAction.NotUnderstood ->
        "I didn't catch that. Name a time like after Isha, ask for a translation like 18:10, " +
            "or use one of the buttons below."
}

/**
 * One answer to a sentence that asked for several things. **PLAN task 22.**
 *
 * ⚠ **Anything it could not read is named, in the reader's own words.** That is the promise
 * the whole rule-based approach rests on: three instructions typed in one breath must not
 * come back as two edits and a silence. The clause is quoted rather than described, so it is
 * obvious which part to rewrite.
 *
 * The full not-understood sentence is used only when *nothing* landed. Repeating "name a time
 * like after Isha" after two instructions have just worked would be advice about a problem
 * the reader does not have.
 */
fun replyForAll(
    actions: List<CompanionAction>,
    progress: Progress?,
    positionLabel: String,
    today: LocalDate = LocalDate.now(),
): String {
    if (actions.size <= 1) {
        val only = actions.firstOrNull() ?: return replyFor(
            CompanionAction.NotUnderstood(""), progress, positionLabel, today,
        )
        return replyFor(only, progress, positionLabel, today)
    }

    val (missed, understood) = actions.partition { it is CompanionAction.NotUnderstood }
    val done = understood.joinToString(" ") { replyFor(it, progress, positionLabel, today) }
    if (missed.isEmpty()) return done

    val quoted = missed
        .filterIsInstance<CompanionAction.NotUnderstood>()
        .joinToString(" ") { "I didn't catch \"${it.said}\"." }
    return if (done.isEmpty()) quoted else "$done $quoted"
}

/** "Friday". The plural is the caller's business, since only some sentences want one. */
private fun dayName(day: java.time.DayOfWeek): String =
    day.getDisplayName(TextStyle.FULL, Locale.UK)

/**
 * "Sunday", or "Sunday 30 Aug" once a bare weekday would be ambiguous.
 *
 * A week ahead is the line: past that, "Sunday" could be either of two Sundays, and a
 * reminder that returns on the wrong one is exactly the failure this is written to prevent.
 *
 * Public because the settings screen has to name the same day the chat named. Two places
 * saying different days about one pause is the bug this is shaped to avoid.
 */
fun dayLabel(date: LocalDate, today: LocalDate): String {
    val name = dayName(date.dayOfWeek)
    val within = date.toEpochDay() - today.toEpochDay() in 0..6
    return if (within) name else "$name ${date.dayOfMonth} ${
        date.month.getDisplayName(TextStyle.SHORT, Locale.UK)
    }"
}

private fun sentenceCase(s: String) = s.replaceFirstChar { it.uppercase() }

private fun lowerFirst(s: String) = s.replaceFirstChar { it.lowercase() }
