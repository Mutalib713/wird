package com.mosman.wird.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill

/**
 * In-App Toolkit Spotlight Tour.
 *
 * Appears directly on top of the real HomeScreen interface after the user completes
 * onboarding, and can be re-launched anytime from the HomeScreen overflow menu (⋮).
 *
 * Explains the 6 core pillars plainly:
 * 1. Your Daily Wird (Portion) Card
 * 2. The Sūrahs Section (Free Reading, independent of your daily wird bookmark)
 * 3. Offline Voice Auditor (Whisper AI - 100% on-device speech check)
 * 4. The AI Chat Companion (Ask for translations, Tafsir, or pause schedule)
 * 5. Reading Modes & Auto-Schedule (Home vs Campus / Ramadan)
 * 6. Settings & Overflow Menu (Custom reminders, audio, and tools)
 *
 * Sacred Rule 6 & 9: Strictly vector iconography with zero raw emojis.
 */
@Composable
fun ToolkitSpotlightOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val totalSteps = 6

    val emerald = Color(0xFF8ED676)
    val gold = Color(0xFFC9A24B)
    val darkCard = Color(0xFF10261C)

    // Pulsing aura animation for the spotlight indicator
    val transition = rememberInfiniteTransition(label = "spotlight_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE805120B))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* prevent clicking through to HomeScreen underneath */ },
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Visual indicator pointing to the active tool's general screen area
        when (stepIndex) {
            0 -> {
                // Pointing to the Daily Wird Portion Card (Center of screen)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 170.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TargetHaloHighlight(
                        label = "TODAY'S WIRD PORTION",
                        borderColor = emerald,
                        pulseAlpha = pulseAlpha,
                        pulseScale = pulseScale,
                    )
                }
            }
            1 -> {
                // Pointing to Sūrahs Tab in Top Navigation / Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 120.dp, end = 120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TargetHaloHighlight(
                        label = "SŪRAHS SECTION (FREE READING)",
                        borderColor = gold,
                        pulseAlpha = pulseAlpha,
                        pulseScale = pulseScale,
                    )
                }
            }
            2 -> {
                // Pointing to Whisper Voice Auditor / Action Tiles
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 290.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TargetHaloHighlight(
                        label = "OFFLINE VOICE AUDITOR (WHISPER AI)",
                        borderColor = Color(0xFF60A5FA),
                        pulseAlpha = pulseAlpha,
                        pulseScale = pulseScale,
                    )
                }
            }
            3 -> {
                // Pointing to AI Chat Companion Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 370.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TargetHaloHighlight(
                        label = "AI CHAT COMPANION & REFLECTION",
                        borderColor = Color(0xFFF472B6),
                        pulseAlpha = pulseAlpha,
                        pulseScale = pulseScale,
                    )
                }
            }
            4 -> {
                // Pointing to Reading Modes & Auto-Schedule Capsule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 85.dp, start = 20.dp, end = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TargetHaloHighlight(
                        label = "READING MODES & AUTO-SCHEDULE",
                        borderColor = emerald,
                        pulseAlpha = pulseAlpha,
                        pulseScale = pulseScale,
                    )
                }
            }
            5 -> {
                // Pointing to Overflow Menu (⋮) in Top Right
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, end = 24.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    TargetHaloHighlight(
                        label = "SETTINGS & OVERFLOW (⋮)",
                        borderColor = gold,
                        pulseAlpha = pulseAlpha,
                        pulseScale = pulseScale,
                    )
                }
            }
        }

        // Bottom Coachmark Floating Card
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(26.dp),
                        backgroundColor = darkCard,
                        highlightColor = Color.White.copy(alpha = 0.14f),
                        shadowColor = Color.Black.copy(alpha = 0.7f),
                        elevation = 8.dp,
                        strokeWidth = 1.5.dp,
                    )
                    .border(1.5.dp, emerald.copy(alpha = 0.65f), RoundedCornerShape(26.dp))
                    .padding(20.dp),
            ) {
                Column {
                    // Header: Step Chip & Skip Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clayPill(
                                    shape = RoundedCornerShape(999.dp),
                                    backgroundColor = Color(0xFF163828),
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "TOOL ${stepIndex + 1} OF $totalSteps",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                color = emerald,
                            )
                        }

                        TextButton(onClick = onDismiss) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Skip Tour",
                                    color = Color(0xFFA1B8AB),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Skip",
                                    tint = Color(0xFFA1B8AB),
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Step Title & Body Content with smooth cross-fade animation
                    AnimatedContent(
                        targetState = stepIndex,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                        label = "tour_step_content",
                    ) { targetStep ->
                        val info = getTourStepInfo(targetStep)
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(info.iconBg)
                                        .border(1.dp, info.accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    info.icon(info.accentColor)
                                }
                                Text(
                                    text = info.title,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = info.description,
                                fontSize = 13.sp,
                                color = Color(0xFFC7DED2),
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (stepIndex > 0) {
                            OutlinedButton(
                                onClick = { stepIndex-- },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B5740)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFA1B8AB),
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Back",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (stepIndex < totalSteps - 1) {
                                    stepIndex++
                                } else {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(if (stepIndex > 0) 1.6f else 1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emerald,
                                contentColor = Color(0xFF071C12),
                            ),
                        ) {
                            Text(
                                text = if (stepIndex < totalSteps - 1) "Next Tool ›" else "Finish Tour ✓",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetHaloHighlight(
    label: String,
    borderColor: Color,
    pulseAlpha: Float,
    pulseScale: Float,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(2.dp, borderColor.copy(alpha = pulseAlpha), RoundedCornerShape(18.dp))
                .background(borderColor.copy(alpha = 0.08f * pulseAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF091A12))
                    .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = label,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = borderColor,
                    letterSpacing = 0.6.sp,
                )
            }
        }
    }
}

private data class TourStepInfo(
    val title: String,
    val description: String,
    val accentColor: Color,
    val iconBg: Color,
    val icon: @Composable (Color) -> Unit,
)

private fun getTourStepInfo(step: Int): TourStepInfo = when (step) {
    0 -> TourStepInfo(
        title = "Your Daily Wird Card",
        description = "This is your committed daily portion. Tap 'Start Today's Wird' or 'Recite' anytime to open the authentic 15-line Madani reader or mark your recitation done. This card keeps your habit steady without pressure.",
        accentColor = Color(0xFF8ED676),
        iconBg = Color(0xFF163526),
        icon = { tint -> VectorBookIcon(tint) },
    )
    1 -> TourStepInfo(
        title = "The Sūrahs Section (Free Reading)",
        description = "Completely separate from your daily portion! Whenever you want to read Surah Al-Kahf on Friday, Al-Mulk before sleeping, or study any Surah, open it here without moving or altering your daily wird bookmark.",
        accentColor = Color(0xFFE5C365),
        iconBg = Color(0xFF2E2713),
        icon = { tint -> VectorSurahIcon(tint) },
    )
    2 -> TourStepInfo(
        title = "Offline Voice Auditor (Whisper AI)",
        description = "Tap the microphone to recite out loud by heart. Our on-device Whisper AI listens and checks your words against the Quran to catch mistakes. 100% offline — zero data bundles used.",
        accentColor = Color(0xFF60A5FA),
        iconBg = Color(0xFF13273C),
        icon = { tint -> VectorMicIcon(tint) },
    )
    3 -> TourStepInfo(
        title = "The AI Chat Companion",
        description = "Chat with your quiet companion anytime. Ask for English translations or Tafsir (explanations) of verses, ask 'What is my portion today?', or say 'I'm travelling this week' to pause without streak guilt.",
        accentColor = Color(0xFFF472B6),
        iconBg = Color(0xFF2E1623),
        icon = { tint -> VectorChatIcon(tint) },
    )
    4 -> TourStepInfo(
        title = "Reading Modes & Auto-Schedule",
        description = "Tap the mode capsule to switch between Home Mode (e.g. 6:00 PM reminders) and School / Campus (e.g. 8:30 PM reminders). The Auto toggle automatically switches modes based on your day and prayer times!",
        accentColor = Color(0xFF8ED676),
        iconBg = Color(0xFF163526),
        icon = { tint -> VectorModeIcon(tint) },
    )
    else -> TourStepInfo(
        title = "Settings & Overflow Menu (⋮)",
        description = "Tap the three dots (⋮) anytime to customize per-mode reminders, change translations, adjust font sizes, or re-run this Toolkit Tour!",
        accentColor = Color(0xFFE5C365),
        iconBg = Color(0xFF2E2713),
        icon = { tint ->
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

// Vector icons implemented cleanly without any emojis
@Composable
private fun VectorBookIcon(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        listOf(-1f, 1f).forEach { side ->
            val outer = Offset(w * (0.5f + side * 0.40f), h * 0.24f)
            val inner = Offset(w * 0.5f, h * 0.32f)
            drawLine(tint, outer, inner, stroke.width, stroke.cap)
            drawLine(tint, outer, Offset(outer.x, h * 0.78f), stroke.width, stroke.cap)
            drawLine(tint, Offset(outer.x, h * 0.78f), Offset(w * 0.5f, h * 0.84f), stroke.width, stroke.cap)
        }
        drawLine(tint, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.5f, h * 0.84f), stroke.width, stroke.cap)
    }
}

@Composable
private fun VectorSurahIcon(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = tint,
            radius = w * 0.42f,
            style = Stroke(width = w * 0.09f),
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * 0.5f),
            end = Offset(w * 0.8f, h * 0.5f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun VectorMicIcon(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.34f, h * 0.08f),
            size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.48f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.16f),
        )
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.20f, h * 0.32f),
            size = androidx.compose.ui.geometry.Size(w * 0.60f, h * 0.42f),
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round),
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.74f),
            end = Offset(w * 0.5f, h * 0.92f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun VectorChatIcon(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.2f)
            lineTo(w * 0.85f, h * 0.2f)
            lineTo(w * 0.85f, h * 0.7f)
            lineTo(w * 0.45f, h * 0.7f)
            lineTo(w * 0.25f, h * 0.9f)
            lineTo(w * 0.25f, h * 0.7f)
            lineTo(w * 0.15f, h * 0.7f)
            close()
        }
        drawPath(path = path, color = tint, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))
    }
}

@Composable
private fun VectorModeIcon(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.55f)
            lineTo(w * 0.15f, h * 0.35f)
            close()
        }
        drawPath(path = path, color = tint, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))
        drawLine(tint, Offset(w * 0.2f, h * 0.6f), Offset(w * 0.5f, h * 0.78f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, h * 0.78f), Offset(w * 0.8f, h * 0.6f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    }
}
