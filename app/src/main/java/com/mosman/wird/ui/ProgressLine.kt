package com.mosman.wird.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.mosman.wird.domain.Progress
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * How it is going, in two lines that cannot lie.
 *
 * **Sacred Rule 4:** the streak never appears on its own. Total days read never resets,
 * so a broken run cannot wipe the evidence that you read on forty days.
 *
 * **Sacred Rule 6:** recited and marked are counted separately and both are shown. The
 * app must never let anyone believe they recited more than they did — that is the number
 * the whole idea rests on.
 *
 * **Sacred Rule 3:** a streak of zero is not announced. Someone who missed yesterday does
 * not need a nought held up to them; they need to see that they have read on twenty-three
 * days and that today is available. Nothing here scolds.
 */
@Composable
fun ProgressLine(progress: Progress, modifier: Modifier = Modifier) {
    val colors = LocalWirdColors.current
    if (progress.totalDaysRead == 0) return

    Column(modifier = modifier.fillMaxWidth().padding(top = Scale.space4)) {
        Text(
            text = headline(progress),
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.caption),
        )
        Text(
            text = split(progress),
            color = colors.textOutsidePortion,
            style = TextStyle(fontSize = Scale.caption),
        )
    }
}

private fun headline(p: Progress): String {
    val total = if (p.totalDaysRead == 1) "1 day read" else "${p.totalDaysRead} days read"
    return when {
        p.currentStreak <= 1 -> total
        else -> "${p.currentStreak} in a row, $total"
    }
}

/**
 * The number this app exists to keep honest.
 *
 * Written out in full rather than as a ratio: "4 of 23" invites you to read it as a score
 * to improve, and this is a record, not a target.
 */
private fun split(p: Progress): String {
    val recited = if (p.recitedDays == 1) "1 recited" else "${p.recitedDays} recited"
    val marked = if (p.tappedDays == 1) "1 marked as read" else "${p.tappedDays} marked as read"
    return when {
        p.tappedDays == 0 -> recited
        p.recitedDays == 0 -> marked
        else -> "$recited, $marked"
    }
}
