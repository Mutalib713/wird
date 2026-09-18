package com.mosman.wird.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.Commitment
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Speaker
import com.mosman.wird.domain.Turn
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Part 2: Full Conversational AI Chat Box Architecture.
 *
 * Dedicated screen for deep reflection on today's portion, habit coaching,
 * and offline verse inquiries. Features:
 * - Clean top bar with avatar, online badge, and "+ New Chat" action
 * - Session context divider showing today's portion (e.g. Surah Ya-Sin, Page 444)
 * - Persisted turns in rounded clay message bubbles with timestamps
 * - Horizontally scrollable suggested reflection topic chips with vector icons
 * - Clay capsule composer with mic icon and vector send arrow
 */
@Composable
fun ChatScreen(
    turns: List<Turn>,
    /** The live promise, if one was made and the day is not done. */
    commitment: Commitment?,
    /** When it will check back — the real armed time, not a guess. */
    checkingBackAt: String?,
    shortcuts: List<String>,
    surahName: String? = null,
    pageNumber: Int? = null,
    doneMethod: Method? = null,
    streak: Int = 0,
    pagesLeft: Int = 0,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
    onNewChat: () -> Unit = {},
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)
    var typed by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        typed = ""
        onSend(t)
    }

    // Auto-scroll to the bottom when new messages arrive
    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty()) listState.animateScrollToItem(turns.size)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF08120D) else Color(0xFFF7F4EC))
            .safeDrawingPadding(),
    ) {
        // ---- Top Bar: Avatar, Online Dot, Title, + New Chat Button ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF11231B) else Color(0xFFFFFFFF))
                .padding(horizontal = Scale.space4, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Back button circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF182C22) else Color(0xFFF1EDE1))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))

                // Avatar with green online dot
                Box {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1A382A) else Color(0xFFDEE9E1))
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0xFF7EBB6A).copy(alpha = 0.3f) else Color(0xFF245847).copy(alpha = 0.2f),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        CompanionChatVectorIcon(
                            tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    // Green online indicator dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7EBB6A))
                            .border(1.5.dp, if (isDark) Color(0xFF11231B) else Color.White, CircleShape)
                            .align(Alignment.BottomEnd),
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Wird Companion",
                        color = colors.textPrimary,
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "Qur'an Reflection · Always Private",
                        color = colors.textSecondary,
                        style = TextStyle(fontSize = 11.sp),
                    )
                }
            }

            // Right: "+ New Chat" Action Pill
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(gold.copy(alpha = 0.15f))
                    .border(0.8.dp, gold.copy(alpha = 0.35f), CircleShape)
                    .clickable(onClick = onNewChat)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                PlusVectorIcon(tint = gold, modifier = Modifier.size(12.dp))
                Text(
                    text = "New Chat",
                    color = if (isDark) Color(0xFFF0D590) else Color(0xFF8A6418),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
        Hairline()

        // ---- Conversation Stream ----
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Scale.space4),
            verticalArrangement = Arrangement.spacedBy(Scale.space3),
        ) {
            // 1. Session Context Divider
            item {
                val portionLabel = if (surahName != null && pageNumber != null) {
                    "$surahName (Page $pageNumber)"
                } else surahName ?: pageNumber?.let { "Page $it" } ?: "Daily Portion"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.ornament.copy(alpha = 0.25f)),
                    )
                    Text(
                        text = "TODAY'S REFLECTION · $portionLabel",
                        color = colors.textSecondary,
                        style = TextStyle(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                        modifier = Modifier
                            .background(if (isDark) Color(0xFF08120D) else Color(0xFFF7F4EC))
                            .padding(horizontal = 12.dp),
                    )
                }
            }

            // 2. Active Commitment Card
            commitment?.let { c ->
                item {
                    CommitmentCard(c, checkingBackAt)
                    Spacer(Modifier.height(Scale.space2))
                }
            }

            // 3. Conversation Turns
            if (turns.isEmpty()) {
                item {
                    val initialGreeting = "Assalamu Alaikum! Today's portion is Page ${pageNumber ?: 1}${surahName?.let { " from $it" } ?: ""}. How did your recitation go today, and what would you like to reflect on?"
                    BotBubble(
                        text = initialGreeting,
                        time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")),
                        isDark = isDark,
                    )
                }
            } else {
                items(turns) { turn ->
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

        // ---- Suggested Topics / Quick Chips Row ----
        val chips = remember(shortcuts) {
            if (shortcuts.isNotEmpty()) shortcuts else listOf(
                "Tafsir of verse",
                "How am I doing?",
                "Where am I?",
                "Already recited today",
                "Remind in 1 hour",
                "Not today",
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Scale.space4, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chips.forEach { chipText ->
                ChatTopicChip(label = chipText, onClick = { send(chipText) })
            }
        }

        Hairline()

        // ---- Bottom Clay Composer ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF102119) else Color(0xFFFFFFFF))
                .padding(horizontal = Scale.space4, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Rounded input capsule
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clayPill(
                        shape = RoundedCornerShape(999.dp),
                        backgroundColor = if (isDark) Color(0xFF09130E) else Color(0xFFF1EDE3),
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.08f else 0.85f),
                        shadowColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF8C7D6B).copy(alpha = 0.16f),
                        elevation = 2.dp,
                    )
                    .defaultMinSize(minHeight = 44.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (typed.isEmpty()) {
                            Text(
                                text = "Type your reflection or ask a question...",
                                color = if (isDark) Color(0xFF8FA597) else Color(0xFF8B9E93),
                                style = TextStyle(fontSize = 13.sp),
                            )
                        }
                        BasicTextField(
                            value = typed,
                            onValueChange = { typed = it },
                            textStyle = TextStyle(
                                fontSize = 13.5.sp,
                                color = if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D),
                            ),
                            cursorBrush = SolidColor(if (isDark) Color(0xFF8ED676) else Color(0xFF245847)),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    MicVectorIcon(
                        tint = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Circular emerald send button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clayPill(
                        shape = CircleShape,
                        backgroundColor = if (isDark) Color(0xFF2A6350) else Color(0xFF245847),
                        elevation = 3.dp,
                    )
                    .alpha(if (typed.isBlank()) 0.45f else 1f)
                    .clickable(enabled = typed.isNotBlank()) { send(typed) }
                    .semantics { contentDescription = "Send" },
                contentAlignment = Alignment.Center,
            ) {
                SendVectorIcon(
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Suggested Topic Chip with vector iconography conforming to Sacred Rule 6.
 */
@Composable
private fun ChatTopicChip(label: String, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val l = label.lowercase()

    Row(
        modifier = Modifier
            .clayPill(
                shape = CircleShape,
                backgroundColor = if (isDark) Color(0xFF16251E) else Color(0xFFF1ECE1),
                highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.85f),
                shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF8C7D6B).copy(alpha = 0.15f),
                elevation = 2.dp,
            )
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 36.dp)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val glyphTint = if (isDark) Color(0xFFBAD3C5) else Color(0xFF204C3D)
        when {
            l.contains("tafsir") || l.contains("explain") || l.contains("verse") -> {
                CompanionBookVectorIcon(tint = glyphTint, modifier = Modifier.size(13.dp))
            }
            l.contains("how") || l.contains("doing") || l.contains("streak") -> {
                FlameVectorIcon(tint = glyphTint, modifier = Modifier.size(13.dp))
            }
            l.contains("where") || l.contains("position") || l.contains("page") -> {
                CompanionBookVectorIcon(tint = glyphTint, modifier = Modifier.size(13.dp))
            }
            l.contains("recited") || l.contains("already") || l.contains("done") -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = glyphTint,
                    modifier = Modifier.size(13.dp),
                )
            }
            l.contains("hour") || l.contains("remind") || l.contains("time") -> {
                ClockVectorIcon(tint = glyphTint, modifier = Modifier.size(13.dp))
            }
            l.contains("not") || l.contains("skip") || l.contains("no") -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = glyphTint,
                    modifier = Modifier.size(13.dp),
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(glyphTint),
                )
            }
        }
        Text(
            text = label,
            color = glyphTint,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

/**
 * The promise, held in view.
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
                    .background(colors.ornament.copy(alpha = 0.3f)),
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
 * A slow breathing pulse for the commitment state.
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
            .alpha(pulse),
    )
}

@Composable
private fun Hairline() {
    val colors = LocalWirdColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.ornament.copy(alpha = 0.2f)),
    )
}

/** "6:12 pm". Lower case, because "6:12 PM" shouts in the middle of a sentence. */
internal fun clockOf(at: LocalDateTime): String =
    at.format(DateTimeFormatter.ofPattern("h:mm a")).lowercase()

