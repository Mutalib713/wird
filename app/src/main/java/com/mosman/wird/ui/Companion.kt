package com.mosman.wird.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Today's Check-in / Companion Chatbot.
 *
 * Faithfully matches the approved claymorphism reference design:
 * - Crisp white / deep clay card container
 * - Header row with TODAY'S CHECK-IN and right caption
 * - WhatsApp-style chat bubble stream (Wird bubble left with companion avatar; user bubble right in mint green)
 * - Clay pill input bar with circular green send button
 * - Quick reply chips (After 'Isha, In an hour, Not today)
 * - "Open full conversation ›" link
 */
@Composable
fun Companion(
    question: String,
    turns: List<Turn>,
    shortcuts: List<String>,
    onReply: (String) -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
    isDone: Boolean = false,
) {
    val colors = LocalWirdColors.current
    var typed by remember { mutableStateOf("") }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        typed = ""
        onReply(t)
    }

    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val containerBg = if (isDark) Color(0xFF111E18) else Color(0xFFFFFFFF)
    val highlight = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.95f)
    val shadow = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF8C7D6B).copy(alpha = 0.28f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(24.dp),
                backgroundColor = containerBg,
                highlightColor = highlight,
                shadowColor = shadow,
                elevation = 7.dp,
                strokeWidth = 1.5.dp,
            )
            .padding(18.dp),
    ) {
        // Top label row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "TODAY'S CHECK-IN",
                color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                style = TextStyle(fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.ExtraBold),
            )
            Text(
                text = if (isDone) "Completed today" else "Only until you finish today",
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                style = TextStyle(fontSize = 10.5.sp),
            )
        }

        Spacer(Modifier.height(12.dp))

        // WhatsApp-style conversation stream
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (turns.isEmpty()) {
                // Initial companion question bubble
                BotBubble(
                    text = question,
                    time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")),
                    isDark = isDark,
                )
            } else {
                val recent = turns.takeLast(3)
                recent.forEach { turn ->
                    if (turn.who == Speaker.YOU) {
                        UserBubble(
                            text = turn.text,
                            time = whenSaid(turn.at),
                            isDark = isDark,
                        )
                    } else {
                        BotBubble(
                            text = turn.text,
                            time = whenSaid(turn.at),
                            isDark = isDark,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Chat Input Row: Clay pill input + Dark green circular send button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clayPill(
                        shape = RoundedCornerShape(999.dp),
                        backgroundColor = if (isDark) Color(0xFF0F1613) else Color(0xFFF1EDE3),
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.08f else 0.8f),
                        shadowColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF8C7D6B).copy(alpha = 0.14f),
                        elevation = 2.dp,
                    )
                    .defaultMinSize(minHeight = 40.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (typed.isEmpty()) {
                    Text(
                        text = "Type your reply...",
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF8B9E93),
                        style = TextStyle(fontSize = 12.5.sp),
                    )
                }
                BasicTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 12.5.sp,
                        color = if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D),
                    ),
                    cursorBrush = SolidColor(if (isDark) Color(0xFF8ED676) else Color(0xFF245847)),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clayPill(
                        shape = CircleShape,
                        backgroundColor = if (isDark) Color(0xFF2A6350) else Color(0xFF245847),
                        elevation = 3.dp,
                    )
                    .clickable(enabled = typed.isNotBlank()) { send(typed) }
                    .semantics { contentDescription = "Send" },
                contentAlignment = Alignment.Center,
            ) {
                SendGlyph(Color.White)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Quick Reply Shortcut Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            shortcuts.forEach { s ->
                Shortcut(s) { send(s) }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Open full conversation link
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenChat)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Open full conversation  ›",
                color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun BotBubble(text: String, time: String, isDark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        // Companion avatar mark
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF162620) else Color(0x24245847)),
            contentAlignment = Alignment.Center,
        ) {
            CompanionMarkIcon(tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847))
        }
        Spacer(Modifier.width(8.dp))
        BoxWithConstraints(modifier = Modifier.weight(1f, fill = false)) {
            Column(
                modifier = Modifier
                    .widthIn(max = maxWidth * 0.88f)
                    .clayCard(
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 3.dp),
                        backgroundColor = if (isDark) Color(0xFF18231E) else Color(0xFFFFFFFF),
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.08f else 0.95f),
                        shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF245847).copy(alpha = 0.08f),
                        elevation = 2.dp,
                        strokeWidth = 1.dp,
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Text(
                    text = text,
                    color = if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D),
                    style = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = time,
                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                    style = TextStyle(fontSize = 9.5.sp),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun UserBubble(text: String, time: String, isDark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        BoxWithConstraints {
            Column(
                modifier = Modifier
                    .widthIn(max = maxWidth * 0.82f)
                    .clayCard(
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 3.dp),
                        backgroundColor = if (isDark) Color(0xFF1F4638) else Color(0xFFD8EADB),
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.8f),
                        shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF245847).copy(alpha = 0.12f),
                        elevation = 2.dp,
                        strokeWidth = 1.dp,
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Text(
                    text = text,
                    color = if (isDark) Color(0xFFE2F4E4) else Color(0xFF103225),
                    style = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = time,
                    color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                    style = TextStyle(fontSize = 9.5.sp),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun CompanionMarkIcon(tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.20f)
            lineTo(w * 0.88f, h * 0.20f)
            quadraticTo(w * 0.96f, h * 0.20f, w * 0.96f, h * 0.30f)
            lineTo(w * 0.96f, h * 0.65f)
            quadraticTo(w * 0.96f, h * 0.75f, w * 0.88f, h * 0.75f)
            lineTo(w * 0.38f, h * 0.75f)
            lineTo(w * 0.20f, h * 0.92f)
            lineTo(w * 0.20f, h * 0.75f)
            lineTo(w * 0.12f, h * 0.75f)
            quadraticTo(w * 0.04f, h * 0.75f, w * 0.04f, h * 0.65f)
            lineTo(w * 0.04f, h * 0.30f)
            quadraticTo(w * 0.04f, h * 0.20f, w * 0.12f, h * 0.20f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun Shortcut(label: String, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    Row(
        modifier = Modifier
            .clayPill(
                shape = CircleShape,
                backgroundColor = if (isDark) Color(0xFF182821) else Color(0xFFF4EFE4),
                highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.85f),
                shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF8C7D6B).copy(alpha = 0.16f),
                elevation = 2.dp,
            )
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 36.dp)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShortcutGlyph(label)
        Text(
            text = label,
            color = if (isDark) Color(0xFFBAD3C5) else Color(0xFF204C3D),
            style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun ShortcutGlyph(label: String) {
    val l = label.lowercase()
    when {
        l.contains("isha") || l.contains("night") -> Text(text = "🌙", fontSize = 11.sp)
        l.contains("hour") || l.contains("minute") -> Text(text = "⏰", fontSize = 11.sp)
        l.contains("not") || l.startsWith("no") -> Text(text = "✕", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        else -> Text(text = "·", fontSize = 11.sp)
    }
    Spacer(Modifier.width(5.dp))
}

@Composable
private fun SendGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val plane = Path().apply {
            moveTo(w * 0.15f, h * 0.50f)
            lineTo(w * 0.90f, h * 0.15f)
            lineTo(w * 0.62f, h * 0.88f)
            lineTo(w * 0.48f, h * 0.58f)
            close()
        }
        drawPath(plane, color = tint)
    }
}

private fun whenSaid(at: LocalDateTime): String {
    val clock = at.format(DateTimeFormatter.ofPattern("h:mm a")).lowercase()
    val today = LocalDate.now()
    return when (at.toLocalDate()) {
        today -> clock
        today.minusDays(1) -> "Yesterday $clock"
        else -> "${at.format(DateTimeFormatter.ofPattern("EEE"))} $clock"
    }
}
