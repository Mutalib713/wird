package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.Speaker
import com.mosman.wird.domain.Turn
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * The companion on Home — a chat, in the space a card has.
 *
 * **PROFILE.md § 5m.** Mutalib's note twice over: first *"the companion doesn't show it is a
 * chat bot"*, then *"make it like an actual chat bot interface instead of what's there."*
 * The answer to the second is [ChatScreen], a real conversation on its own screen. This is
 * its window onto Home, and it has to do two jobs that pull against each other.
 *
 *  1. **Read unmistakably as a conversation** — so the last turns show as bubbles, tails and
 *     sides included, not as a question with chips under it.
 *  2. **Not add a step before committing.** § 2 found 8 of 14 missed days were
 *     procrastination, so the input stays *here*. You can answer without opening anything.
 *     Tapping the header opens the full conversation; nothing makes you.
 *
 * Deliberately **not** given a name, a face, or a personality claiming to be a person —
 * § 5b. A bot that says "I'm proud of you" about someone's deen is worse than one that says
 * nothing.
 */
@Composable
fun Companion(
    /** What it is asking, already phrased for the time of day. */
    question: String,
    /** The conversation so far. The last exchange is what shows here. */
    turns: List<Turn>,
    /** Quick answers. Shortcuts for typing, never the full set of options. */
    shortcuts: List<String>,
    onReply: (String) -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    var typed by remember { mutableStateOf("") }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        typed = ""
        onReply(t)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, colors.ornament.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = Scale.space4),
    ) {
        // ---- header: says what this is, and is the way in ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Scale.radius))
                .clickable(onClick = onOpenChat)
                .defaultMinSize(minHeight = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(colors.accent))
            Spacer(Modifier.width(Scale.space2))
            Text(
                text = "TODAY'S CHECK-IN",
                color = colors.textSecondary,
                style = TextStyle(fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Open  ›",
                color = colors.accent,
                style = TextStyle(fontSize = Scale.caption),
            )
        }

        Spacer(Modifier.height(Scale.space3))

        // ---- the last exchange, as bubbles ----
        //
        // Its question is a turn like any other, so a fresh day and a running conversation
        // look like the same object rather than two different components.
        val recent = remember(turns, question) {
            turns.takeLast(2).ifEmpty { listOf(Turn(Speaker.WIRD, question, java.time.LocalDateTime.now())) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Scale.space2)) {
            recent.forEach { MiniBubble(it) }
        }

        Spacer(Modifier.height(Scale.space3))

        // ---- your turn: still the primary control, still here ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Scale.space2),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Scale.radius))
                    .border(1.dp, colors.ornament.copy(alpha = 0.45f), RoundedCornerShape(Scale.radius))
                    .defaultMinSize(minHeight = Scale.minTarget)
                    .padding(horizontal = Scale.space3, vertical = Scale.space2),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (typed.isEmpty()) {
                    Text(
                        text = "Reply in your own words",
                        color = colors.textOutsidePortion,
                        style = TextStyle(fontSize = Scale.body),
                    )
                }
                BasicTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = Scale.body, color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                modifier = Modifier
                    .size(Scale.minTarget)
                    .clip(RoundedCornerShape(Scale.radius))
                    .background(colors.accent)
                    .alpha(if (typed.isBlank()) 0.4f else 1f)
                    .clickable(enabled = typed.isNotBlank()) { send(typed) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Send",
                    tint = colors.surface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(Scale.space3))

        // ---- shortcuts, underneath, plainly secondary ----
        Row(horizontalArrangement = Arrangement.spacedBy(Scale.space2)) {
            shortcuts.forEach { s -> Shortcut(s) { send(s) } }
        }
    }
}

/** A bubble at card size. Same tails and sides as [ChatScreen], less padding. */
@Composable
private fun MiniBubble(turn: Turn) {
    val colors = LocalWirdColors.current
    val mine = turn.who == Speaker.YOU

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cap = maxWidth * 0.84f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = cap)
                    .clip(
                        if (mine) {
                            RoundedCornerShape(Scale.radius, Scale.radius, 2.dp, Scale.radius)
                        } else {
                            RoundedCornerShape(Scale.radius, Scale.radius, Scale.radius, 2.dp)
                        }
                    )
                    .then(
                        if (mine) {
                            Modifier.border(
                                1.dp,
                                colors.ornament.copy(alpha = 0.45f),
                                RoundedCornerShape(Scale.radius, Scale.radius, 2.dp, Scale.radius),
                            )
                        } else {
                            Modifier.background(colors.surfaceRaised)
                        }
                    )
                    .padding(horizontal = Scale.space3, vertical = Scale.space2),
            ) {
                Text(
                    text = turn.text,
                    color = if (mine) colors.textPrimary else colors.onSurfaceRaised,
                    style = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
                )
            }
        }
    }
}

/**
 * A quick answer.
 *
 * Outlined rather than filled, so it does not compete with the input above it. In the
 * original design these were the only control and read as the whole set of options; here
 * they are visibly the lighter of two ways to answer.
 */
@Composable
private fun Shortcut(label: String, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    Text(
        text = label,
        color = colors.textSecondary,
        style = TextStyle(fontSize = 13.sp),
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, colors.ornament.copy(alpha = 0.45f), CircleShape)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 38.dp)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}
