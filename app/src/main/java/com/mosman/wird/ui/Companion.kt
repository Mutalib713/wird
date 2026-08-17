package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * The accountability companion, as it appears on Home.
 *
 * **This component exists because of one note from Mutalib**, and it is the sharpest thing
 * he said in the whole redesign: *"the companion there doesn't show it is a chat bot."*
 *
 * The design put three fixed chips — *after Isha · at 9 · not today* — under a question.
 * That reads as a **poll**: three answers, pick one, done. Nothing in it suggests you could
 * say something it did not offer, and the plain-words reply is the entire reason the
 * companion is more than a smarter notification (PROFILE.md § 5b).
 *
 * **So the fix is not decoration, it is which control is primary.** Three changes, each
 * doing a specific job:
 *
 *  1. **The input is the main affordance**, full width, with a cursor and a real prompt —
 *     the first thing your eye lands on under the question. The chips sit *beneath* it as
 *     shortcuts, which is the honest relationship: they are quick ways to type a common
 *     answer, not the set of permitted answers.
 *  2. **The question is presented as its turn** — marked, indented, addressed to you —
 *     rather than as a heading. Something said this, and is waiting.
 *  3. **The placeholder names the freedom explicitly.** "Or tell it in your own words —
 *     'not today, I'm travelling'." A blank box with no example is a box nobody types in.
 *
 * Sacred Rule 3 governs every string here: it asks, it does not push. There is no "you
 * haven't read yet", no streak, and no version of *are you sure?*
 *
 * Deliberately **not** given a name, a face, or a personality that claims to be a person.
 * PROFILE.md § 5b — it should not pretend to be human, and a bot that says "I'm proud of
 * you" about someone's deen is worse than one that says nothing.
 */
@Composable
fun Companion(
    /** What it is asking, already phrased for the time of day. */
    question: String,
    /** Quick answers. Shortcuts for typing, never the full set of options. */
    shortcuts: List<String>,
    onReply: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Its last reply, when it has said something back. Null on a fresh ask. */
    lastReply: String? = null,
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
            .clip(RoundedCornerShape(Scale.radius))
            .background(colors.surfaceRaised.copy(alpha = 0.3f))
            .padding(Scale.space4),
    ) {
        // ---- its turn ----
        Row(verticalAlignment = Alignment.Top) {
            // A small mark, not an avatar. Enough to say "this is something speaking",
            // without inventing a character to speak it.
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.accent)
            )
            Spacer(Modifier.size(Scale.space3))
            Column {
                Text(
                    text = question,
                    color = colors.textPrimary,
                    style = TextStyle(fontSize = Scale.body, fontWeight = FontWeight.Medium),
                )
                lastReply?.let {
                    Spacer(Modifier.height(Scale.space2))
                    Text(
                        text = it,
                        color = colors.textSecondary,
                        style = TextStyle(fontSize = Scale.caption),
                    )
                }
            }
        }

        Spacer(Modifier.height(Scale.space4))

        // ---- your turn: the primary control ----
        //
        // Full width and visibly a field. This is what tells you the thing can be talked
        // to, and it is why it sits above the shortcuts rather than after them.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Scale.radius))
                .border(1.dp, colors.textOutsidePortion.copy(alpha = 0.5f), RoundedCornerShape(Scale.radius))
                .padding(horizontal = Scale.space3, vertical = Scale.space1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f).defaultMinSize(minHeight = Scale.minTarget)) {
                if (typed.isEmpty()) {
                    Text(
                        // Names the freedom, and gives an example of the messy kind of
                        // sentence it is meant to take. An empty box with no example is a
                        // box nobody types in.
                        text = "Tell it in your own words…",
                        color = colors.textOutsidePortion,
                        style = TextStyle(fontSize = Scale.body),
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }
                BasicTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = Scale.body, color = colors.textPrimary),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
                )
            }
            if (typed.isNotBlank()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = colors.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Scale.radius))
                        .clickable { send(typed) }
                        .defaultMinSize(minWidth = Scale.minTarget, minHeight = Scale.minTarget)
                        .padding(Scale.space3)
                        .size(20.dp),
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

/**
 * A quick answer.
 *
 * Outlined rather than filled, so it does not compete with the input above it. In the
 * design these were the only control and read as the whole set of options; here they are
 * visibly the lighter of two ways to answer.
 */
@Composable
private fun Shortcut(label: String, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    Text(
        text = label,
        color = colors.textSecondary,
        style = TextStyle(fontSize = 13.sp),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, colors.textOutsidePortion.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 36.dp)
            .padding(horizontal = Scale.space3, vertical = Scale.space2),
    )
}
