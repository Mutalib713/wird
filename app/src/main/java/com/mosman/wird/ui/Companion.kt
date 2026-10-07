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
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.Assignment
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Progress
import com.mosman.wird.domain.Speaker
import com.mosman.wird.domain.Turn
import com.mosman.wird.domain.surahs
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Option A: Habit Clarity Card + Reflection Capsule Doorway.
 *
 * Featured prominently on the Homepage.
 * Direct habit buttons on top (Yes I recited it, Remind 1 hr, Not today),
 * anchored by an attached reflection capsule that opens the full Conversational AI Chat Box.
 */
@Composable
fun HomeHabitClarityCard(
    assignment: Assignment,
    doneMethod: Method?,
    progress: Progress?,
    turns: List<Turn>,
    onMarkRead: () -> Unit,
    onRemindInHour: () -> Unit,
    onNotToday: () -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPortion: (() -> Unit)? = null,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)

    val cardBg = if (isDark) Color(0xFF111E18) else Color(0xFFFFFFFF)
    val highlight = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.95f)
    val shadow = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF8C7D6B).copy(alpha = 0.28f)

    val surah = assignment.surahs.firstOrNull()
    val surahName = surah?.name ?: "Portion"
    val pageNumber = assignment.startPage
    val streak = progress?.currentStreak ?: 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(24.dp),
                backgroundColor = cardBg,
                highlightColor = highlight,
                shadowColor = shadow,
                elevation = 7.dp,
                strokeWidth = 1.5.dp,
            )
            .padding(18.dp),
    ) {
        // 1. Top tag row + live streak badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (doneMethod == null) "TODAY'S CHECK-IN" else "TODAY'S CHECK-IN · COMPLETED",
                color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                style = TextStyle(fontSize = 11.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.ExtraBold),
            )

            // Streak badge (Matches YOUR JOURNEY flame)
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(gold.copy(alpha = 0.15f))
                    .border(0.8.dp, gold.copy(alpha = 0.35f), CircleShape)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (streak > 0) "🔥 $streak Day Streak" else "🔥 Start Streak",
                    color = if (isDark) Color(0xFFF0D590) else Color(0xFF8A6418),
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (doneMethod == null) {
            // Question
            Text(
                text = "Have you completed your portion of $surahName (Page $pageNumber) today?",
                color = colors.textPrimary,
                style = TextStyle(fontSize = 15.5.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp),
            )

            Spacer(Modifier.height(14.dp))

            // Primary Action Button: "I've read it"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = Color(0xFF245847),
                        highlightColor = Color.White.copy(alpha = 0.25f),
                        shadowColor = Color.Black.copy(alpha = 0.4f),
                        elevation = 4.dp,
                    )
                    .clickable(onClick = onMarkRead)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "I've read it",
                        color = Color.White,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Sub-actions row (2 equal columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Remind 1 hr
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clayCard(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = if (isDark) Color(0xFF172A21) else Color(0xFFF4EFE4),
                            highlightColor = Color.White.copy(alpha = if (isDark) 0.12f else 0.85f),
                            shadowColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0xFF8C7D6B).copy(alpha = 0.14f),
                            elevation = 2.dp,
                        )
                        .clickable(onClick = onRemindInHour)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ClockVectorIcon(tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Remind 1 hr",
                        color = colors.textPrimary,
                        style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                    )
                }

                // Not today
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clayCard(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = if (isDark) Color(0xFF221A1A) else Color(0xFFFAF3F3),
                            highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.8f),
                            shadowColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0xFF9E7E7E).copy(alpha = 0.15f),
                            elevation = 2.dp,
                        )
                        .clickable(onClick = onNotToday)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFE29F9F) else Color(0xFF9C4A4A),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Not today",
                        color = colors.textPrimary,
                        style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        } else {
            // Celebratory Completed State
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF245847)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Alhamdulillah! Portion complete.",
                        color = colors.textPrimary,
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = if (doneMethod == Method.RECITED) "Recited aloud · Streak protected" else "Marked as read · Streak protected",
                        color = colors.textSecondary,
                        style = TextStyle(fontSize = 12.sp),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Attached Reflection Capsule Doorway (Option A)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0xFF14271E) else Color(0xFFF1EDE1))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF7EBB6A).copy(alpha = 0.2f) else Color(0xFF245847).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                )
                .clickable(onClick = onOpenChat)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1E3A2E) else Color(0xFFDEE9E1)),
                    contentAlignment = Alignment.Center,
                ) {
                    CompanionChatVectorIcon(
                        tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wird Companion",
                        color = colors.textPrimary,
                        style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                    )
                    val preview = turns.lastOrNull { it.who == Speaker.WIRD }?.text
                        ?: "Reflect on today's portion or ask questions"
                    Text(
                        text = preview,
                        color = colors.textSecondary,
                        style = TextStyle(fontSize = 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF245847))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Open Chat",
                    color = Color.White,
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                )
                ChevronRightVectorIcon(tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
    }
}

/**
 * Today's Check-in / Companion Chatbot (Compact/Legacy fallback).
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
    onOpenPortion: (() -> Unit)? = null,
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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (turns.isEmpty()) {
                BotBubble(
                    text = question,
                    time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")),
                    isDark = isDark,
                    onOpenPortion = onOpenPortion,
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
                            onOpenPortion = onOpenPortion,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

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
                SendVectorIcon(Color.White)
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            shortcuts.forEach { s ->
                Shortcut(s) { send(s) }
            }
        }

        Spacer(Modifier.height(10.dp))

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
internal fun BotBubble(
    text: String,
    time: String,
    isDark: Boolean,
    onOpenPortion: (() -> Unit)? = null,
) {
    val tafsir = remember(text) { parseTafsirMessage(text) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF162620) else Color(0x24245847)),
            contentAlignment = Alignment.Center,
        ) {
            CompanionChatVectorIcon(tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847), modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(8.dp))
        BoxWithConstraints(modifier = Modifier.weight(1f, fill = false)) {
            if (tafsir != null) {
                TafsirCard(
                    tafsir = tafsir,
                    time = time,
                    isDark = isDark,
                    maxWidth = maxWidth,
                )
            } else {
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
                        style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
                    )
                    if (onOpenPortion != null && (text.contains("go to your portion", ignoreCase = true) || text.contains("open your portion", ignoreCase = true))) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isDark) Color(0xFF245847) else Color(0xFF1B4E3B))
                                .clickable(onClick = onOpenPortion)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            CompanionBookVectorIcon(tint = Color.White, modifier = Modifier.size(12.dp))
                            Text(
                                text = "Go to Today's Portion ›",
                                color = Color.White,
                                style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
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
}

internal data class ParsedTafsir(
    val verseRef: String,
    val translation: String,
    val translationSource: String,
    val sections: List<TafsirSectionItem>,
    val attribution: String,
)

internal data class TafsirSectionItem(
    val number: Int?,
    val title: String,
    val paragraphs: List<String>,
)

/**
 * Detect whether a paragraph is explaining a specific verse (ayah).
 * These get a distinct visual treatment so the user can tell
 * "this is an explanation of a specific ayah" apart from general commentary.
 */
private val VERSE_REF_REGEX = Regex("""\(\d+:\d+\)|[Vv]erse\s+\d+|[Aa]yah\s+\d+|[Ss]urah\s+\w+,?\s+\d+:\d+""")
private fun isVerseExplanation(para: String): Boolean = VERSE_REF_REGEX.containsMatchIn(para)

internal fun parseTafsirMessage(text: String): ParsedTafsir? {
    if (!text.contains("Tafsir Ibn Kathir (Abridged):")) return null

    return runCatching {
        val parts = text.split("━━━━━━━━━━━━━━━")
        if (parts.size < 2) return null

        val topPart = parts[0].trim()
        val bottomPart = parts[1].trim()

        val topLines = topPart.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
        val verseRef = topLines.getOrNull(0) ?: ""
        val translationRaw = topLines.getOrNull(1) ?: ""
        val translation = translationRaw.removeSurrounding("\"")
        val translationSource = topLines.getOrNull(2)?.removePrefix("— Translation:")?.trim() ?: "Saheeh International"

        val tafsirBody = bottomPart.removePrefix("Tafsir Ibn Kathir (Abridged):").trim()
        val rawSections = tafsirBody.split(Regex("\n{2,}§ ")).map { it.trim() }.filter { it.isNotBlank() }

        val sectionItems = mutableListOf<TafsirSectionItem>()
        var attribution = "— Sourced from Quran.com / Dar-us-Salam"

        rawSections.forEachIndexed { idx, rawSec ->
            var secText = if (rawSec.startsWith("§ ")) rawSec.removePrefix("§ ").trim() else rawSec
            if (secText.contains("— Sourced from")) {
                val splitAttr = secText.split(Regex("\n{2,}— Sourced from"))
                secText = splitAttr[0].trim()
                if (splitAttr.size > 1) {
                    attribution = "— Sourced from " + splitAttr[1].trim()
                }
            }

            val lines = secText.split(Regex("\n{2,}")).map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                val rawHeader = lines[0]
                val isNumbered = rawHeader.startsWith("Section ") && rawHeader.contains(":")
                val title = when {
                    isNumbered -> rawHeader.substringAfter(":").trim()
                    rawHeader.startsWith("* ") -> rawHeader.removePrefix("* ").trim()
                    rawHeader.startsWith("• ") -> rawHeader.removePrefix("• ").trim()
                    else -> rawHeader
                }

                val secNumber = if (isNumbered) {
                    Regex("Section (\\d+)").find(rawHeader)?.groupValues?.get(1)?.toIntOrNull()
                } else null
                val paras = lines.drop(1)
                sectionItems.add(TafsirSectionItem(secNumber, title, paras))
            }
        }

        if (sectionItems.isEmpty()) return null

        ParsedTafsir(
            verseRef = verseRef,
            translation = translation,
            translationSource = translationSource,
            sections = sectionItems,
            attribution = attribution,
        )
    }.getOrNull()
}

@Composable
private fun TafsirCard(
    tafsir: ParsedTafsir,
    time: String,
    isDark: Boolean,
    maxWidth: androidx.compose.ui.unit.Dp,
) {
    var expandedSections by remember { mutableStateOf(setOf(0)) }
    val allExpanded = expandedSections.size == tafsir.sections.size

    Column(
        modifier = Modifier
            .widthIn(max = maxWidth * 0.94f)
            .clayCard(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 3.dp),
                backgroundColor = if (isDark) Color(0xFF14211A) else Color(0xFFFFFFFF),
                highlightColor = Color.White.copy(alpha = if (isDark) 0.08f else 0.95f),
                shadowColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF245847).copy(alpha = 0.08f),
                elevation = 2.dp,
                strokeWidth = 1.dp,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 1. Verse Reference & Source Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF1E3D2F) else Color(0xFFD6EAE0))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(
                    text = tafsir.verseRef,
                    color = if (isDark) Color(0xFF98E2A7) else Color(0xFF1D5A40),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                )
            }
            Text(
                text = tafsir.translationSource,
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6B8074),
                style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Medium),
            )
        }

        // 2. Verse Translation Callout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDark) Color(0xFF192B22) else Color(0xFFF4F7F4))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF284D3C) else Color(0xFFD0E2D6),
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                text = "“${tafsir.translation}”",
                color = if (isDark) Color(0xFFEFF5F0) else Color(0xFF153326),
                style = TextStyle(fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Normal),
            )
        }

        // 3. Tafsir Header & Expand/Collapse Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CompanionBookVectorIcon(
                    tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "Tafsir Ibn Kathir",
                    color = if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D),
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF22362C) else Color(0xFFE3EDE6))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "${tafsir.sections.size} sections",
                        color = if (isDark) Color(0xFF9CCDAE) else Color(0xFF2E634F),
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
            }

            if (tafsir.sections.size > 1) {
                Text(
                    text = if (allExpanded) "Collapse all" else "Expand all",
                    color = if (isDark) Color(0xFF98E2A7) else Color(0xFF245847),
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .clickable {
                            expandedSections = if (allExpanded) {
                                emptySet()
                            } else {
                                tafsir.sections.indices.toSet()
                            }
                        }
                        .padding(4.dp),
                )
            }
        }

        // 4. Structured Numbered Sections (Accordion)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            tafsir.sections.forEachIndexed { index, section ->
                val isExpanded = expandedSections.contains(index)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF0F1A14) else Color(0xFFF9F8F3))
                        .border(
                            width = 1.dp,
                            color = if (isExpanded) {
                                if (isDark) Color(0xFF335746) else Color(0xFFC4D9CA)
                            } else {
                                if (isDark) Color(0x18FFFFFF) else Color(0x12000000)
                            },
                            shape = RoundedCornerShape(10.dp),
                        )
                        .animateContentSize(),
                ) {
                    // Section Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedSections = if (isExpanded) {
                                    expandedSections - index
                                } else {
                                    expandedSections + index
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Section Badge: Numbered for major topics, bullet for subtopics
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E382A) else Color(0xFFD6EAE0)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (section.number != null) "${section.number}" else "•",
                                color = if (isDark) Color(0xFF98E2A7) else Color(0xFF1F583F),
                                style = TextStyle(
                                    fontSize = if (section.number != null) 11.sp else 14.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = section.title,
                            color = if (isDark) Color(0xFFECEFE8) else Color(0xFF1B3B2F),
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f),
                            maxLines = if (isExpanded) 3 else 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    // Section Content (when expanded)
                    if (isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            section.paragraphs.forEach { para ->
                                when {
                                    para.startsWith("«") && para.endsWith("»") -> {
                                        // Arabic commentary box
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDark) Color(0xFF1A3326) else Color(0xFFE8F2EA))
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = para,
                                                color = if (isDark) Color(0xFFA6E3A1) else Color(0xFF165039),
                                                style = TextStyle(fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
                                            )
                                        }
                                    }
                                    para.startsWith("• ") || para.startsWith("* ") -> {
                                        // Bullet sub-point with indent for visual hierarchy
                                        val bulletText = if (para.startsWith("* ")) "• " + para.removePrefix("* ").trim() else para
                                        Text(
                                            text = bulletText,
                                            color = if (isDark) Color(0xFFD6DDD7) else Color(0xFF233B31),
                                            style = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    }
                                    isVerseExplanation(para) -> {
                                        // Verse explanation — distinct left accent bar so the
                                        // user can see "this is explaining a specific ayah"
                                        // vs general commentary.
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(
                                                        if (isDark) Color(0xFF8ED676).copy(alpha = 0.5f)
                                                        else Color(0xFF245847).copy(alpha = 0.3f)
                                                    )
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = para,
                                                color = if (isDark) Color(0xFFE4EDE5) else Color(0xFF1B3B2F),
                                                style = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
                                            )
                                        }
                                    }
                                    else -> {
                                        Text(
                                            text = para,
                                            color = if (isDark) Color(0xFFD6DDD7) else Color(0xFF233B31),
                                            style = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Footer & Timestamp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tafsir.attribution,
                color = if (isDark) Color(0xFF758A7E) else Color(0xFF82948A),
                style = TextStyle(fontSize = 9.5.sp),
            )
            Text(
                text = time,
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                style = TextStyle(fontSize = 9.5.sp),
            )
        }
    }
}

@Composable
internal fun UserBubble(text: String, time: String, isDark: Boolean) {
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
                    style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
                )
                Spacer(Modifier.height(3.dp))
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
        val glyphTint = if (isDark) Color(0xFFBAD3C5) else Color(0xFF204C3D)
        ShortcutGlyph(label, glyphTint)
        Text(
            text = label,
            color = glyphTint,
            style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun ShortcutGlyph(label: String, tint: Color) {
    val l = label.lowercase()
    when {
        l.contains("isha") || l.contains("night") -> {
            Canvas(modifier = Modifier.size(12.dp)) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.75f, h * 0.12f)
                    cubicTo(w * 0.35f, h * 0.15f, w * 0.18f, h * 0.48f, w * 0.28f, h * 0.82f)
                    cubicTo(w * 0.38f, h * 0.95f, w * 0.58f, h * 1.0f, w * 0.78f, h * 0.90f)
                    cubicTo(w * 0.52f, h * 0.82f, w * 0.44f, h * 0.45f, w * 0.75f, h * 0.12f)
                    close()
                }
                drawPath(path, color = tint)
            }
        }
        l.contains("hour") || l.contains("minute") -> {
            ClockVectorIcon(tint = tint, modifier = Modifier.size(12.dp))
        }
        l.contains("not") || l.startsWith("no") -> {
            Canvas(modifier = Modifier.size(11.dp)) {
                val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                val pad = size.width * 0.18f
                drawLine(color = tint, start = Offset(pad, pad), end = Offset(size.width - pad, size.height - pad), strokeWidth = stroke.width, cap = stroke.cap)
                drawLine(color = tint, start = Offset(size.width - pad, pad), end = Offset(pad, size.height - pad), strokeWidth = stroke.width, cap = stroke.cap)
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(tint)
            )
        }
    }
    Spacer(Modifier.width(5.dp))
}

internal fun whenSaid(at: LocalDateTime): String {
    val clock = at.format(DateTimeFormatter.ofPattern("h:mm a")).lowercase()
    val today = LocalDate.now()
    return when (at.toLocalDate()) {
        today -> clock
        today.minusDays(1) -> "Yesterday $clock"
        else -> "${at.format(DateTimeFormatter.ofPattern("EEE"))} $clock"
    }
}

// =========================================================================
// VECTOR ICONOGRAPHY — STRICTLY CONFORMING TO SACRED RULE 6
// Zero raw/informal emojis. Google Material & Lucide style vector drawables.
// =========================================================================

/** Vector speech bubble / chat mark icon matching Sacred Rule 6. */
@Composable
fun CompanionChatVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.18f)
            lineTo(w * 0.85f, h * 0.18f)
            quadraticTo(w * 0.95f, h * 0.18f, w * 0.95f, h * 0.28f)
            lineTo(w * 0.95f, h * 0.65f)
            quadraticTo(w * 0.95f, h * 0.75f, w * 0.85f, h * 0.75f)
            lineTo(w * 0.42f, h * 0.75f)
            lineTo(w * 0.22f, h * 0.94f)
            lineTo(w * 0.22f, h * 0.75f)
            lineTo(w * 0.15f, h * 0.75f)
            quadraticTo(w * 0.05f, h * 0.75f, w * 0.05f, h * 0.65f)
            lineTo(w * 0.05f, h * 0.28f)
            quadraticTo(w * 0.05f, h * 0.18f, w * 0.15f, h * 0.18f)
            close()
        }
        drawPath(path = path, color = tint, style = stroke)
    }
}

/** Vector plus icon matching Sacred Rule 6. */
@Composable
fun PlusVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val pad = size.width * 0.18f
        drawLine(color = tint, start = Offset(pad, size.height / 2), end = Offset(size.width - pad, size.height / 2), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(size.width / 2, pad), end = Offset(size.width / 2, size.height - pad), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

/** Vector send arrow icon matching Sacred Rule 6. */
@Composable
fun SendVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
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

/** Vector mic icon matching Sacred Rule 6. */
@Composable
fun MicVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.35f, h * 0.10f),
            size = Size(w * 0.30f, h * 0.50f),
            cornerRadius = CornerRadius(w * 0.15f, w * 0.15f),
            style = stroke,
        )
        val arcPath = Path().apply {
            moveTo(w * 0.22f, h * 0.45f)
            cubicTo(w * 0.22f, h * 0.72f, w * 0.78f, h * 0.72f, w * 0.78f, h * 0.45f)
        }
        drawPath(arcPath, color = tint, style = stroke)
        drawLine(color = tint, start = Offset(w * 0.50f, h * 0.72f), end = Offset(w * 0.50f, h * 0.90f), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(w * 0.35f, h * 0.90f), end = Offset(w * 0.65f, h * 0.90f), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

/** Vector right chevron matching Sacred Rule 6. */
@Composable
fun ChevronRightVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(12.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(w * 0.32f, h * 0.20f)
            lineTo(w * 0.68f, h * 0.50f)
            lineTo(w * 0.32f, h * 0.80f)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

/** Vector clock icon matching Sacred Rule 6. */
@Composable
fun ClockVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val r = size.minDimension * 0.42f
        drawCircle(color = tint, radius = r, center = center, style = stroke)
        drawLine(color = tint, start = center, end = Offset(center.x, center.y - r * 0.55f), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = center, end = Offset(center.x + r * 0.45f, center.y), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

/** Vector flame / streak icon matching Sacred Rule 6. */
@Composable
fun FlameVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            cubicTo(w * 0.75f, h * 0.25f, w * 0.95f, h * 0.55f, w * 0.8f, h * 0.85f)
            cubicTo(w * 0.65f, h * 1.0f, w * 0.35f, h * 1.0f, w * 0.2f, h * 0.85f)
            cubicTo(w * 0.05f, h * 0.65f, w * 0.25f, h * 0.4f, w * 0.45f, h * 0.45f)
            cubicTo(w * 0.4f, h * 0.3f, w * 0.45f, h * 0.15f, w * 0.5f, h * 0.05f)
            close()
        }
        drawPath(path, color = tint)
    }
}

/** Vector book icon matching Sacred Rule 6. */
@Composable
fun CompanionBookVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        listOf(-1f, 1f).forEach { side ->
            val outer = Offset(w * (0.5f + side * 0.42f), h * 0.22f)
            val inner = Offset(w * 0.5f, h * 0.32f)
            drawLine(tint, outer, inner, stroke.width, stroke.cap)
            drawLine(tint, outer, Offset(outer.x, h * 0.80f), stroke.width, stroke.cap)
            drawLine(tint, Offset(outer.x, h * 0.80f), Offset(w * 0.5f, h * 0.86f), stroke.width, stroke.cap)
        }
        drawLine(tint, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.5f, h * 0.86f), stroke.width, stroke.cap)
    }
}

