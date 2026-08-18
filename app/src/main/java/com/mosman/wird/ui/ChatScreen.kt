package com.mosman.wird.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.mosman.wird.domain.Commitment
import com.mosman.wird.domain.Speaker
import com.mosman.wird.domain.Turn
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The companion, as an actual conversation.
 *
 * **PROFILE.md § 5m.** Mutalib's complaint was precise: *"make it like an actual chat bot
 * interface instead of what's there, cos the user won't know if it is a chat bot."* A card
 * with a question and three chips reads as a poll. This reads as a chat, because it is one —
 * turns that persist, its lines on the left and yours on the right, and an input pinned to
 * the bottom of the screen where every messaging app on earth has taught people to look.
 *
 * Ported from `Wird v2.dc.html`'s companion screen, which had already drawn it: the
 * commitment held in view as an object, the exchange as bubbles with a tail, and the missed
 * case written out as a fact and a question.
 *
 * **The design's own restraint is kept where it matters.** Its header comment calls this
 * *"a standing appointment, not a chat log"* — it speaks when it asks and at the time you
 * named, and not in between. Nothing here invents chatter to fill the screen.
 *
 * ⚠ **What is underneath has not changed, and that is the honest risk.** `CompanionBrain` is
 * hand-written rules understanding a handful of phrasings. A chat box invites anyone to type
 * anything, so this interface promises more than the parser can keep — which is why the
 * not-understood reply names what it *does* know instead of apologising, and why the
 * shortcuts stay on screen as a standing hint. The engine is Mutalib's call and is still
 * open: PLAN task 22.
 */
@Composable
fun ChatScreen(
    turns: List<Turn>,
    /** The live promise, if one was made and the day is not done. */
    commitment: Commitment?,
    /** When it will check back — the real armed time, not a guess. */
    checkingBackAt: String?,
    shortcuts: List<String>,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalWirdColors.current
    var typed by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        typed = ""
        onSend(t)
    }

    // New turns land at the bottom, so the view follows them there. Without this the
    // companion's reply arrives off-screen and looks like nothing happened.
    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty()) listState.animateScrollToItem(turns.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            // `safeDrawing` already includes the IME inset. Adding `imePadding()` on top of
            // it counts the keyboard twice, and the result was the input row clipped and
            // the send button unreachable the moment the keyboard opened — found on the
            // emulator, not by reading this.
            .safeDrawingPadding(),
    ) {
        // ---- header ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Scale.space4, vertical = Scale.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.accent,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .defaultMinSize(minWidth = Scale.minTarget, minHeight = Scale.minTarget)
                    .padding(Scale.space3),
            )
            Spacer(Modifier.width(Scale.space2))
            Text(
                text = "Today's check-in",
                color = colors.textPrimary,
                style = TextStyle(fontSize = Scale.body, fontWeight = FontWeight.SemiBold),
            )
        }
        Hairline()

        // ---- the conversation ----
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Scale.space4),
            verticalArrangement = Arrangement.spacedBy(Scale.space3),
        ) {
            commitment?.let { c ->
                item {
                    CommitmentCard(c, checkingBackAt)
                    Spacer(Modifier.height(Scale.space2))
                }
            }

            if (turns.isEmpty()) {
                item { Empty() }
            } else {
                items(turns) { turn -> Bubble(turn) }
            }
        }

        // ---- shortcuts, standing hints rather than the whole menu ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Scale.space4)
                .padding(bottom = Scale.space2),
            horizontalArrangement = Arrangement.spacedBy(Scale.space2),
        ) {
            shortcuts.forEach { s -> Chip(s) { send(s) } }
        }

        Hairline()

        // ---- the input, pinned where a chat's input belongs ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Scale.space4, vertical = Scale.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Scale.space2),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Scale.radius))
                    .border(
                        1.dp,
                        colors.ornament.copy(alpha = 0.45f),
                        RoundedCornerShape(Scale.radius),
                    )
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
                    textStyle = TextStyle(fontSize = Scale.body, color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Always present, dimmed until there is something to send. A send button that
            // appears and disappears makes the row jump while you are typing into it.
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
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * The promise, held in view.
 *
 * **Your own words, at display size.** Not "reminder set for 8:10 pm" — *"after Isha"*, the
 * thing you actually said. That is what makes it a promise rather than a setting, and it is
 * the entire difference § 5b is testing.
 */
@Composable
private fun CommitmentCard(commitment: Commitment, checkingBackAt: String?) {
    val colors = LocalWirdColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Scale.radius))
            .border(1.5.dp, colors.accent, RoundedCornerShape(Scale.radius))
            .padding(18.dp),
    ) {
        Text(
            text = "YOU SAID",
            color = colors.textSecondary,
            style = TextStyle(fontSize = 11.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(Scale.space2))
        Text(
            text = "“${commitment.spoken}”",
            color = colors.textPrimary,
            style = TextStyle(fontSize = 28.sp),
        )

        checkingBackAt?.let { at ->
            Spacer(Modifier.height(Scale.space4))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.ornament.copy(alpha = 0.3f))
            )
            Spacer(Modifier.height(Scale.space4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "Checking back at",
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = Scale.caption),
                )
                Text(
                    text = at,
                    color = colors.textPrimary,
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                )
            }
        }

        Spacer(Modifier.height(Scale.space3))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BreathingDot()
            Spacer(Modifier.width(Scale.space2))
            Text(
                text = "Holding since ${clockOf(commitment.madeAt)} · reminder moved to match",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
            )
        }
    }
}

/**
 * A slow pulse, and the only animation in the app.
 *
 * It earns its place by saying something no static mark can: *this is still running.* A
 * promise that looks inert is one you assume was forgotten. 2.4 seconds is deliberately
 * slower than a notification blink — it should read as breathing, not as alerting.
 */
@Composable
private fun BreathingDot() {
    val colors = LocalWirdColors.current
    val transition = rememberInfiniteTransition(label = "holding")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    Box(
        Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(colors.accent)
            .alpha(pulse)
    )
}

/**
 * One line of the exchange.
 *
 * Its turns sit left on a raised ground; yours sit right, outlined. **The corner opposite
 * the speaker is squared off** — 2dp against 6dp — which is the tail, and it is what makes a
 * pair of boxes read as two people talking rather than as a list.
 */
@Composable
private fun Bubble(turn: Turn) {
    val colors = LocalWirdColors.current
    val mine = turn.who == Speaker.YOU

    // BoxWithConstraints so a bubble can be as wide as its words *up to* 84% — a fixed
    // fillMaxWidth(0.84f) would stretch "at 9" across most of the screen.
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
                                1.5.dp,
                                colors.ornament.copy(alpha = 0.45f),
                                RoundedCornerShape(Scale.radius, Scale.radius, 2.dp, Scale.radius),
                            )
                        } else {
                            Modifier.background(colors.surfaceRaised)
                        }
                    )
                    .padding(horizontal = 15.dp, vertical = 13.dp),
            ) {
                Text(
                    text = turn.text,
                    color = if (mine) colors.textPrimary else colors.onSurfaceRaised,
                    style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
                )
            }
        }
    }
}

@Composable
private fun Empty() {
    val colors = LocalWirdColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Scale.space8)) {
        Text(
            text = "Nothing said yet.",
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.body),
        )
        Spacer(Modifier.height(Scale.space2))
        Text(
            // Says what it can take, which is the honest version of a blank screen and also
            // the mitigation for a parser that only understands a handful of phrasings.
            text = "Tell it when you're reading, or ask how you're doing. " +
                "The buttons below are the quick answers.",
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.caption, lineHeight = 20.sp),
        )
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
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

@Composable
private fun Hairline() {
    val colors = LocalWirdColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.ornament.copy(alpha = 0.2f))
    )
}

/** "6:12 pm". Lower case, because "6:12 PM" shouts in the middle of a sentence. */
internal fun clockOf(at: LocalDateTime): String =
    at.format(DateTimeFormatter.ofPattern("h:mm a")).lowercase()
