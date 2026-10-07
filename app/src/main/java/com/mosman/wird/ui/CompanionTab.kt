package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.Commitment
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Turn
import com.mosman.wird.ui.theme.LocalWirdColors

/**
 * Companion tab in the new look (PROFILE § 5bj, PLAN task 28).
 * Provides offline verse inquiries, plan and reminder adjustments,
 * honest guidance on upcoming open question capabilities, and recent conversations.
 */
@Composable
fun CompanionTab(
    turns: List<Turn>,
    commitment: Commitment?,
    checkingBackAt: String?,
    trackName: String,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    surahName: String? = null,
    pageNumber: Int? = null,
    doneMethod: Method? = null,
    streak: Int = 0,
    onOpenPortion: (() -> Unit)? = null,
) {
    val colors = LocalWirdColors.current
    var inChat by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    if (inChat) {
        ChatScreen(
            turns = turns,
            commitment = commitment,
            checkingBackAt = checkingBackAt,
            shortcuts = listOf(
                "Explain today's verses",
                "What does Ayat al-Kursi mean?",
                "Remind me after Isha",
                "Half a page on Fridays",
            ),
            surahName = surahName,
            pageNumber = pageNumber,
            doneMethod = doneMethod,
            streak = streak,
            onSend = onSend,
            onBack = { inChat = false },
            onOpenPortion = onOpenPortion,
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.field)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Companion",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                ),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ask about a verse, or change your plan and reminders.",
                fontSize = 15.sp,
                color = colors.ink2,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(16.dp))

            // Ask Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.card)
                    .padding(start = 18.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = colors.ink),
                    cursorBrush = SolidColor(colors.action),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text("Ask, or tell me something", color = colors.ink2, fontSize = 15.sp)
                        }
                        innerTextField()
                    },
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.action)
                        .clickable {
                            val msg = inputText.trim()
                            if (msg.isNotEmpty()) {
                                onSend(msg)
                                inputText = ""
                                inChat = true
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = WirdIcons.Send,
                        contentDescription = "Send",
                        tint = colors.onAction,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Works offline label & suggestions
            Text(
                text = "Works offline",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.ink,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )

            val suggestions = listOf(
                "Explain today's verses",
                "What does Ayat al-Kursi mean?",
                "Half a page on Fridays",
                "Remind me after Isha",
                "I'm travelling till Sunday",
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                suggestions.forEach { sugg ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSend(sugg)
                                inChat = true
                            },
                    ) {
                        Text(
                            text = sugg,
                            fontSize = 15.sp,
                            color = colors.ink,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Honest Cloud Notice Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.tile),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = WirdIcons.Cloud,
                        contentDescription = "Coming soon",
                        tint = colors.goldText,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Open questions, like \"What does the Qur'an say about patience?\", come next. They will need the internet.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = colors.ink,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Recent Conversations
            Text(
                text = "Recent conversations",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.ink,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { inChat = true },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(colors.disc, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = WirdIcons.Chat,
                            contentDescription = "Conversation",
                            tint = colors.action,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trackName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.ink,
                        )
                        Text(
                            text = if (commitment != null) commitment.spoken else "Reminders and reflections",
                            fontSize = 13.sp,
                            color = colors.ink2,
                        )
                    }
                    Icon(
                        imageVector = WirdIcons.ChevronRight,
                        contentDescription = "Open",
                        tint = colors.ink2,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
