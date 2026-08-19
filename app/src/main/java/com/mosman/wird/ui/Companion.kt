package com.mosman.wird.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.Speaker
import com.mosman.wird.domain.Turn
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The companion on Home — a conversation, in the space a card has.
 *
 * **Rebuilt 2026-08-19 to Mutalib's image 2**, which he named specifically: the section label
 * with a caption on the right, the turns carrying *who said it and when*, and a way through
 * to the full conversation at the foot rather than tucked in the header.
 *
 * It still has to do the two jobs that pull against each other:
 *
 *  1. **Read unmistakably as a conversation** — his first note was that it did not look like
 *     one at all.
 *  2. **Not add a step before committing.** § 2 found 8 of 14 missed days were procrastination,
 *     so the input stays *here*. You can answer without opening anything. Tapping through to
 *     the full conversation is offered; nothing makes you.
 *
 * **What changed from the opposed-bubble version, and what that cost.** § 5q made the turns
 * sit on opposite sides with a squared corner each, because two boxes on two sides read as two
 * people talking. His comp stacks them in one column and labels each with a speaker and a
 * time instead. That is a real trade: the sides said "conversation" without a word, and the
 * labels say it explicitly while also answering *when*, which the sides never could. Going
 * with his comp, and keeping one half of the old signal — **your turn is outlined, Wird's is
 * filled** — so the two voices are still distinguishable with the labels ignored.
 *
 * ⚠ **One thing in the comp is deliberately not built: the face.** Image 2 gives the
 * companion a round smiling avatar. PROFILE.md § 5b rules that out in as many words —
 * *"deliberately not given a name, a face, or a personality claiming to be a person"* — and
 * the reason is not squeamishness: a bot that emotes about someone's deen is worse than one
 * that says nothing. It gets a mark, not a face. Say so rather than ship it quietly.
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
            .clip(RoundedCornerShape(Scale.card))
            // ⚠ **Tinted, and that is the fix for his first look at it.** Built on the plain
            // card colour it was the same plate as the portion above it, so the two ran
            // together into one long block. His comps tint this card specifically - it is the
            // one section that is a conversation rather than a record, and it should not look
            // like the rest. The tint is § 6e's teal, measured: ink 13.14, accent 4.59.
            .background(colors.recite.fill)
            .border(1.dp, colors.recite.edge, RoundedCornerShape(Scale.card))
            .padding(Scale.space4),
    ) {
        // ---- what this is, and how long it lasts ----
        //
        // The caption is his comp's, and it is worth having because it answers the question
        // the card otherwise provokes: why is this here some days and not others? Sacred
        // Rule 3 — someone who has already read today is not asked whether they are going to.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "TODAY'S CHECK-IN",
                color = colors.textSecondary,
                style = TextStyle(fontSize = 10.5.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Only until you finish today",
                color = colors.textSecondary,
                style = TextStyle(fontSize = 10.5.sp),
            )
        }

        Spacer(Modifier.height(Scale.space3))

        // ---- the question, beside the mark ----
        Row(verticalAlignment = Alignment.Top) {
            CompanionMark()
            Spacer(Modifier.width(Scale.space3))
            Text(
                text = question,
                color = colors.textPrimary,
                style = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
            )
        }

        // ---- the last exchange, each turn saying who and when ----
        val recent = remember(turns) { turns.takeLast(2) }
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(Scale.space3))
            Column(verticalArrangement = Arrangement.spacedBy(Scale.space2)) {
                recent.forEach { TurnBlock(it) }
            }
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
                    .clip(RoundedCornerShape(Scale.radius * 2))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.cardEdge, RoundedCornerShape(Scale.radius * 2))
                    .defaultMinSize(minHeight = Scale.minTarget)
                    .padding(horizontal = Scale.space3, vertical = Scale.space2),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (typed.isEmpty()) {
                    Text(
                        text = "Type your reply",
                        color = colors.textSecondary,
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
                    // Round, and the glyph points away rather than up - his comps' shape,
                    // and the one every messaging app has taught people to look for.
                    .size(Scale.minTarget)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .alpha(if (typed.isBlank()) 0.4f else 1f)
                    .clickable(enabled = typed.isNotBlank()) { send(typed) }
                    .semantics { contentDescription = "Send" },
                contentAlignment = Alignment.Center,
            ) {
                SendGlyph(colors.surfaceRaised)
            }
        }

        Spacer(Modifier.height(Scale.space3))

        // ---- shortcuts, underneath, plainly secondary ----
        Row(horizontalArrangement = Arrangement.spacedBy(Scale.space2)) {
            shortcuts.forEach { s -> Shortcut(s) { send(s) } }
        }

        // ---- the way through to the whole thing ----
        Spacer(Modifier.height(Scale.space3))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Scale.radius))
                .clickable(onClick = onOpenChat)
                .defaultMinSize(minHeight = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Open full conversation  ›",
                color = colors.accent,
                style = TextStyle(fontSize = Scale.caption, fontWeight = FontWeight.Medium),
            )
        }
    }
}

/**
 * One turn, the way a messaging app draws one. **His instruction, 2026-08-19: "why won't you
 * do it like how WhatsApp does it".**
 *
 * Four things make a bubble read as a bubble rather than as a labelled box, and the version
 * this replaces had none of them:
 *
 *  1. **It hugs its words.** Capped at 78% of the width and no wider than it needs — "at 9"
 *     is a small bubble. Full-width blocks are a form; bubbles are a conversation.
 *  2. **Sides.** Yours right and tinted, the companion's left and white. That is the
 *     arrangement every phone here already has muscle memory for.
 *  3. **A tail.** The corner nearest the speaker's own edge is squared to 3dp against 16dp,
 *     which is what points a bubble at whoever said it.
 *  4. **The time sits inside**, small and faint at the bottom, instead of being a label
 *     stacked above the text.
 *
 * ⚠ **The timestamp inside your own bubble uses its own colour**, and that is not fussiness:
 * the ordinary secondary grey measures **4.01:1** on the tinted bubble and fails. § 6e's
 * `bubbleMineDetail` is the darkened version at 4.66. The white bubble keeps the normal grey,
 * where it is 5.19.
 */
@Composable
private fun TurnBlock(turn: Turn) {
    val colors = LocalWirdColors.current
    val mine = turn.who == Speaker.YOU
    val r = Scale.card
    val tail = 3.dp
    val shape = if (mine) {
        RoundedCornerShape(r, r, tail, r)
    } else {
        RoundedCornerShape(r, r, r, tail)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cap = maxWidth * 0.78f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = cap)
                    .clip(shape)
                    .background(if (mine) colors.bubbleMine else colors.bubbleTheirs)
                    .padding(horizontal = Scale.space3, vertical = Scale.space2),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = turn.text,
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
                )
                Text(
                    text = whenSaid(turn.at),
                    color = if (mine) colors.bubbleMineDetail else colors.textSecondary,
                    style = TextStyle(fontSize = 10.sp),
                )
            }
        }
    }
}

/**
 * The companion's mark. **A speech bubble, not a face.**
 *
 * His image 2 puts a round smiling avatar here, and PROFILE.md § 5b rules that out in as many
 * words: *"deliberately not given a name, a face, or a personality claiming to be a person"*.
 * A bot that emotes about someone's deen is worse than one that says nothing.
 *
 * The substitute is taken from his own comps rather than invented — **image 1 uses a speech
 * bubble in this same card.** It says "this is a conversation" without implying anybody is on
 * the other end of it. Drawn rather than shipped, like every other glyph here.
 */
@Composable
private fun CompanionMark() {
    val colors = LocalWirdColors.current
    Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(34.dp)) {
            drawCircle(color = colors.surfaceRaised)
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = colors.accent,
                topLeft = Offset(w * 0.24f, h * 0.26f),
                size = Size(w * 0.52f, h * 0.34f),
                cornerRadius = CornerRadius(w * 0.10f),
                style = Stroke(width = w * 0.065f),
            )
            // The tail, which is the whole difference between a bubble and a rectangle.
            drawLine(
                color = colors.accent,
                start = Offset(w * 0.38f, h * 0.58f),
                end = Offset(w * 0.34f, h * 0.74f),
                strokeWidth = w * 0.065f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = colors.accent,
                start = Offset(w * 0.34f, h * 0.74f),
                end = Offset(w * 0.50f, h * 0.58f),
                strokeWidth = w * 0.065f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * The glyph in front of a shortcut, or nothing.
 *
 * His comps put a small mark on each chip — a crescent, a clock, a cross — and they earn their
 * place: three chips of plain text at one size get read one after another, while three chips
 * with distinct marks are picked out at a glance. That matters on the one control § 2's
 * procrastination finding says has to be answerable without thinking about it.
 *
 * Matched on the phrase rather than on an enum, because [Companion] is handed plain strings —
 * the same reason `CommitReceiver` sends phrases instead of codes. One vocabulary, and anything
 * unrecognised simply gets no glyph.
 */
@Composable
private fun ShortcutGlyph(label: String) {
    val colors = LocalWirdColors.current
    val tint = colors.textSecondary
    val l = label.lowercase()
    when {
        l.contains("isha") || l.contains("maghrib") || l.contains("tonight") ->
            Canvas(Modifier.size(13.dp)) {
                // A crescent: a disc with a second disc knocked out of it.
                drawCircle(color = tint, radius = size.minDimension * 0.46f)
                drawCircle(
                    color = colors.surfaceRaised,
                    radius = size.minDimension * 0.40f,
                    center = Offset(size.width * 0.74f, size.height * 0.34f),
                )
            }

        l.contains("hour") || l.contains("minute") || l.contains("later") ->
            Canvas(Modifier.size(13.dp)) {
                val r = size.minDimension * 0.42f
                val stroke = size.minDimension * 0.12f
                drawCircle(color = tint, radius = r, style = Stroke(width = stroke))
                drawLine(tint, center, Offset(center.x, center.y - r * 0.55f), stroke, StrokeCap.Round)
                drawLine(tint, center, Offset(center.x + r * 0.45f, center.y), stroke, StrokeCap.Round)
            }

        l.contains("not") || l.startsWith("no") -> Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp),
        )

        else -> return
    }
    Spacer(Modifier.width(6.dp))
}

/**
 * A quick answer.
 *
 * Outlined rather than filled, so it does not compete with the input above it. In the comps
 * these are the only control and read as the whole set of options; here they are visibly the
 * lighter of two ways to answer.
 */
@Composable
private fun Shortcut(label: String, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.cardEdge, CircleShape)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 38.dp)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShortcutGlyph(label)
        Text(
            text = label,
            color = colors.textSecondary,
            style = TextStyle(fontSize = 13.sp),
        )
    }
}

/** A paper plane. The core icon set has no send glyph, so it is drawn like the others. */
@Composable
private fun SendGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val plane = Path().apply {
            moveTo(w * 0.10f, h * 0.52f)
            lineTo(w * 0.90f, h * 0.14f)
            lineTo(w * 0.60f, h * 0.88f)
            lineTo(w * 0.47f, h * 0.60f)
            close()
        }
        drawPath(plane, color = tint)
    }
}

/**
 * "8:15 pm" today, "Yesterday 8:15 pm" before that.
 *
 * Inside a bubble the day is usually noise — a conversation you are having now is about now.
 * It only earns its place once the turn is not from today, which is exactly when the reader
 * would otherwise misread an old promise as a fresh one.
 */
private fun whenSaid(at: LocalDateTime): String {
    val clock = at.format(DateTimeFormatter.ofPattern("h:mm a")).lowercase()
    val today = LocalDate.now()
    return when (at.toLocalDate()) {
        today -> clock
        today.minusDays(1) -> "Yesterday $clock"
        else -> "${at.format(DateTimeFormatter.ofPattern("EEE"))} $clock"
    }
}
