package com.mosman.wird.domain

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
 * Sacred Rule 3 governs every line. A refusal gets no argument and no guilt — the app says
 * fine and gets out of the way, because the day someone is told off is the day they delete a
 * habit app.
 */
fun replyFor(
    action: CompanionAction,
    progress: Progress?,
    positionLabel: String,
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

    // **The most important string in the companion**, and the reason it is written this way.
    //
    // A chat box invites anyone to type anything, and what is underneath is a handful of
    // hand-written rules — PROFILE.md § 5m records that gap honestly. So the failure reply
    // does not apologise and does not pretend; it names what it *does* understand, which
    // turns a dead end into a menu. An "I don't understand" with no examples is how someone
    // decides the feature is broken and stops typing.
    is CompanionAction.NotUnderstood ->
        "I didn't catch that. Name a time like after Isha, or use one of the buttons below."
}
