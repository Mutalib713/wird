package com.mosman.wird.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.Commitment
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Speaker
import com.mosman.wird.domain.Turn
import com.mosman.wird.ui.theme.LocalWirdColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Companion Chat screen for the new look (PROFILE § 5bj, PLAN task 28).
 * Matches screen 10 ("Companion - Chat") in the design brief and prototype/index.html.
 */
@Composable
fun ChatScreen(
    turns: List<Turn>,
    commitment: Commitment?,
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
    onOpenPortion: (() -> Unit)? = null,
) {
    val colors = LocalWirdColors.current
    var typed by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    BackHandler(onBack = onBack)

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        typed = ""
        onSend(t)
    }

    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty()) {
            listState.animateScrollToItem(turns.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.field)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // ---- Top Header: Back + Leaf avatar + Title & Subtitle ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = WirdIcons.Back,
                    contentDescription = "Back",
                    tint = colors.ink,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.disc),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = WirdIcons.Leaf,
                    contentDescription = null,
                    tint = colors.action,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Wird Companion",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    ),
                )
                Text(
                    text = "Verses and your plan work offline",
                    fontSize = 12.sp,
                    color = colors.ink2,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.rule),
        )

        // ---- Conversation Stream ----
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Initial welcoming turn if turns are empty
            if (turns.isEmpty()) {
                item {
                    val portionDesc = if (surahName != null && pageNumber != null) {
                        "Page $pageNumber ($surahName)"
                    } else if (pageNumber != null) {
                        "Page $pageNumber"
                    } else {
                        "your portion"
                    }
                    val welcomeText = "Assalamu Alaikum! Today's portion is $portionDesc. Ask about any verse, or adjust your daily plan and reminder times."
                    NewLookBotMessage(
                        text = welcomeText,
                        onOpenPortion = onOpenPortion,
                    )
                }
            } else {
                items(turns) { turn ->
                    if (turn.who == Speaker.YOU) {
                        NewLookUserMessage(text = turn.text)
                    } else {
                        NewLookBotMessage(
                            text = turn.text,
                            onOpenPortion = onOpenPortion,
                        )
                    }
                }
            }
        }

        // ---- Suggestions row ----
        if (shortcuts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                shortcuts.forEach { chipText ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.card)
                            .clickable { send(chipText) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 13.sp,
                            color = colors.ink,
                        )
                    }
                }
            }
        }

        // ---- Bottom Input Bar ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(colors.card)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = colors.ink,
                    ),
                    cursorBrush = SolidColor(colors.action),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (typed.isEmpty()) {
                            Text(
                                text = "Ask, or tell me something",
                                fontSize = 15.sp,
                                color = colors.ink2,
                            )
                        }
                        innerTextField()
                    },
                )
            }

            Spacer(Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (typed.isNotBlank()) colors.action else colors.chip)
                    .clickable(enabled = typed.isNotBlank()) { send(typed) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = WirdIcons.Send,
                    contentDescription = "Send",
                    tint = if (typed.isNotBlank()) colors.onAction else colors.ink2,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun NewLookUserMessage(text: String) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 6.dp,
                    ),
                )
                .background(colors.action)
                .padding(horizontal = 16.dp, vertical = 11.dp),
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = colors.onAction,
            )
        }
    }
}

@Composable
private fun NewLookBotMessage(
    text: String,
    onOpenPortion: (() -> Unit)? = null,
) {
    val colors = LocalWirdColors.current
    val isVerseResponse = text.contains("━━━━━━━━━━━━━━━") || (text.contains(":") && text.contains("— Translation:"))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 24.dp),
    ) {
        if (isVerseResponse) {
            val parts = text.split("━━━━━━━━━━━━━━━")
            val mainPart = parts.firstOrNull()?.trim() ?: text
            val tafsirPart = if (parts.size > 1) parts[1].trim() else null

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.rule),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = mainPart,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = colors.ink,
                    )

                    if (!tafsirPart.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.rule),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = tafsirPart,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            color = colors.ink2,
                        )
                    }

                    if (onOpenPortion != null) {
                        Spacer(Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.action)
                                .clickable { onOpenPortion() }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = "Open in Qur'an",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.onAction,
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = text,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = colors.ink,
            )
        }
    }
}
