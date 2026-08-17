package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Method
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * What you have actually done — the Recite tab.
 *
 * **Why this screen exists at all.** The design's tab set had a *Recite* tab, and reciting
 * is not a place: it happens at the foot of today's page, where you arrive having actually
 * read it, and PLAN task 6 put it there on purpose so the control is *earned* rather than
 * parked somewhere convenient. A tab pointing at that same button would either duplicate it
 * or drag it away from the page it belongs to. Mutalib chose the third option on
 * 2026-08-17: give the tab something only it can show.
 *
 * So this is the record — every day you finished, how you finished it, and your own voice
 * played back. All of it already stored; none of it was ever visible.
 *
 * **Sacred Rules 3, 4 and 6 all land on this screen at once**, which is why it is written
 * the way it is:
 *  - missed days are simply **absent**. There is no row saying you failed, no gap drawn in
 *    grey, no "you missed 3 days" summary. The list is what you did
 *  - recited and marked are **named**, never scored against each other
 *  - and the empty state is not a scolding — on day one there is nothing here yet, and that
 *    is a normal thing rather than a problem to fix
 */
@Composable
fun RecitationsScreen(
    logs: List<DayLog>,
    audioFor: (LocalDate) -> File?,
    onPlay: (File) -> Unit,
) {
    val colors = LocalWirdColors.current
    // Newest first: the thing you did most recently is the thing you want to hear back.
    val ordered = logs.sortedByDescending { it.date }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(horizontal = Scale.space6),
    ) {
        Spacer(Modifier.height(Scale.space6))
        Text("Your wird", color = colors.textPrimary, style = TextStyle(fontSize = Scale.display))
        Spacer(Modifier.height(Scale.space2))

        if (ordered.isEmpty()) {
            Text(
                text = "Nothing here yet. Once you finish a day it will be listed, and " +
                    "anything you recited you can hear back.",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.body),
            )
            return@Column
        }

        Text(
            text = "Days you missed aren't listed.",
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.caption),
        )
        Spacer(Modifier.height(Scale.space4))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(ordered, key = { it.date.toString() }) { log ->
                DayRow(log = log, audio = audioFor(log.date), onPlay = onPlay)
            }
        }
    }
}

@Composable
private fun DayRow(log: DayLog, audio: File?, onPlay: (File) -> Unit) {
    val colors = LocalWirdColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Scale.minTarget),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.padding(vertical = Scale.space2)) {
                Text(
                    text = dayLabel(log.date),
                    color = colors.textPrimary,
                    style = TextStyle(fontSize = Scale.body),
                )
                Text(
                    // Named, not scored. Sacred Rule 6 — the two are never presented as a
                    // ratio, and "marked as read" is not framed as the lesser one.
                    text = if (log.method == Method.RECITED) "Recited aloud" else "Marked as read",
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }
            // Only where a recording actually exists. A play control that does nothing is
            // worse than no control — and a recitation logged before task 6's zero-byte
            // guard could have no file behind it.
            if (audio != null && audio.exists() && audio.length() > 0) {
                TextButton(
                    onClick = { onPlay(audio) },
                    modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
                ) {
                    Text("Hear it back", color = colors.accent)
                }
            }
        }
        // A hairline rather than a card. Mutalib's note on the design: it looks boxy, and a
        // list of bordered boxes is the fastest way to get there.
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.textOutsidePortion.copy(alpha = 0.25f))
        )
    }
}

/** "Today", "Yesterday", then the date. Nobody counts back further than two days. */
private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))
    }
}
