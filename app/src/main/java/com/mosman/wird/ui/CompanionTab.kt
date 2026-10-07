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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
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
    onOpenChat: () -> Unit = {},
    onStartNewChat: (String) -> Unit = onSend,
    onClearChat: () -> Unit = {},
) {
    val colors = LocalWirdColors.current
    var inputText by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var conversationsCleared by remember { mutableStateOf(false) }
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
                text = "Ask questions, explore and understand the Qur'an better.",
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
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = WirdIcons.Search,
                    contentDescription = null,
                    tint = colors.ink2,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = colors.ink),
                    cursorBrush = SolidColor(colors.action),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text("Ask a question...", color = colors.ink2, fontSize = 15.sp)
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
                                onStartNewChat(msg)
                                inputText = ""
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = WirdIcons.Send,
                        contentDescription = "Send",
                        tint = colors.onAction,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Suggested label & suggestions matching master design
            Text(
                text = "Suggested",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.ink,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )

            val suggestions = listOf(
                "What does the Qur'an say about patience?",
                "Why is Surah Al-Kahf important?",
                "What does the Qur'an say about Jesus?",
                "Explain this ayah",
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
                                onStartNewChat(sugg)
                            },
                    ) {
                        Text(
                            text = sugg,
                            fontSize = 15.sp,
                            color = colors.ink,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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

            // Recent Conversations header with Clear action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent conversations",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.ink,
                )
                if (!conversationsCleared) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showClearConfirmDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = WirdIcons.Trash,
                            contentDescription = "Clear",
                            tint = colors.ink2,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Clear",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink2,
                        )
                    }
                }
            }

            if (conversationsCleared) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No recent conversations. Ask a question or pick a prompt above to start a fresh chat.",
                        fontSize = 14.sp,
                        color = colors.ink2,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Conversation 1: Active reflection
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChat() },
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
                                    text = "The meaning of ${surahName ?: "Al-Fatihah"}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.ink,
                                )
                                Text(
                                    text = if (commitment != null) commitment.spoken else "2 days ago",
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

                    // Conversation 2
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartNewChat("Patience in Islam") },
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
                                    text = "Patience in Islam",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.ink,
                                )
                                Text(
                                    text = "3 days ago",
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

                    // Conversation 3
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartNewChat("About Surah Maryam") },
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
                                    text = "About Surah Maryam",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.ink,
                                )
                                Text(
                                    text = "4 days ago",
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
                }
            }

            if (showClearConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showClearConfirmDialog = false },
                    title = {
                        Text(
                            text = "Clear recent conversations?",
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.ink,
                            ),
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to clear your chat history? This will clear recent conversations and reset your active reflection session.",
                            fontSize = 14.sp,
                            color = colors.ink2,
                            lineHeight = 20.sp,
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                conversationsCleared = true
                                showClearConfirmDialog = false
                                onClearChat()
                            },
                        ) {
                            Text(
                                text = "Clear",
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirmDialog = false }) {
                            Text(
                                text = "Cancel",
                                color = colors.ink,
                            )
                        }
                    },
                    containerColor = colors.card,
                    shape = RoundedCornerShape(18.dp),
                )
            }

            Spacer(Modifier.height(80.dp))
        }
}
