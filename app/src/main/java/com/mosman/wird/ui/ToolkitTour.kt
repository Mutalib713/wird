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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill

/**
 * Interactive Feature Spotlight Coachmark Tour.
 *
 * Dynamically points to each live feature on the screen with:
 * - A glowing pulsing target frame wrapping the real UI element.
 * - A directional pointer arrow (▲ pointing up or ▼ pointing down) connecting to the feature.
 * - Dynamic vertical card positioning (moves to top or bottom) so it NEVER covers the feature
 *   and NEVER collides with the bottom navigation dock.
 * - Back, Next, and Skip controls with zero emoji clutter.
 */
@Composable
fun ToolkitSpotlightOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var stepIndex by remember { mutableIntStateOf(0) }

    val emerald = Color(0xFF8ED676)
    val darkCard = Color(0xFF0D2218)

    // Pulsing aura animation for the spotlight indicator
    val transition = rememberInfiniteTransition(label = "spotlight_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    val steps = remember {
        listOf(
            // 1. Daily Portion Card (Center-Upper screen)
            SpotlightStep(
                tag = "1 OF 5 • CORE HABIT",
                title = "Your Daily Wird Card",
                subtitle = "Committed reading, streak protected",
                description = "This is your committed daily portion. Tap 'Start Today's Wird' or 'Mushaf' anytime to open the authentic 15-line Madani reader or mark your reading done.",
                accentColor = Color(0xFF8ED676),
                targetTop = 250.dp,
                targetHeight = 210.dp,
                targetStart = 16.dp,
                targetWidth = 380.dp,
                targetCornerRadius = 24.dp,
                cardPlacement = CardPosition.BOTTOM,
                arrowOffsetDp = 190.dp,
                arrowPointingUp = true,
                cardBottomPadding = 110.dp,
                cardTopPadding = 0.dp,
            ),
            // 2. Offline Voice Auditor (Recite Button)
            SpotlightStep(
                tag = "2 OF 5 • VOICE AI",
                title = "Recite Out Loud (Voice AI)",
                subtitle = "100% Offline Whisper AI checks your words",
                description = "Tap 'Recite' to recite by heart. Our built-in Whisper AI checks your words against the Quran in real-time. Completely private with zero data bundles used.",
                accentColor = Color(0xFF60A5FA),
                targetTop = 472.dp,
                targetHeight = 88.dp,
                targetStart = 22.dp,
                targetWidth = 88.dp,
                targetCornerRadius = 18.dp,
                cardPlacement = CardPosition.BOTTOM,
                arrowOffsetDp = 48.dp,
                arrowPointingUp = true,
                cardBottomPadding = 110.dp,
                cardTopPadding = 0.dp,
            ),
            // 3. Wird AI Companion & Tafsir (Chat Card at bottom)
            SpotlightStep(
                tag = "3 OF 5 • SCHOLARLY TAFSIR",
                title = "Wird AI Companion",
                subtitle = "Numbered Tafsir Ibn Kathir & Translations",
                description = "Tap 'Open Chat' to view numbered Tafsir Ibn Kathir, ask translation questions, or pause your schedule when travelling without streak guilt.",
                accentColor = Color(0xFFFBBF24),
                targetTop = 728.dp,
                targetHeight = 64.dp,
                targetStart = 20.dp,
                targetWidth = 372.dp,
                targetCornerRadius = 18.dp,
                cardPlacement = CardPosition.TOP,
                arrowOffsetDp = 290.dp, // Points toward the 'Open Chat >' button on the right
                arrowPointingUp = false,
                cardBottomPadding = 0.dp,
                cardTopPadding = 230.dp,
            ),
            // 4. Reading Modes & Auto-Schedule (Top pill)
            SpotlightStep(
                tag = "4 OF 5 • SMART ROUTINE",
                title = "School & Home Modes",
                subtitle = "Adapts to campus schedules & Ramadan",
                description = "Switch between Home Mode (evening reminders) and School / Campus Mode (night reminders). Auto-Schedule dynamically adjusts your daily portions when exams or busy days arrive.",
                accentColor = Color(0xFF34D399),
                targetTop = 64.dp,
                targetHeight = 44.dp,
                targetStart = 16.dp,
                targetWidth = 260.dp,
                targetCornerRadius = 22.dp,
                cardPlacement = CardPosition.TOP,
                arrowOffsetDp = 60.dp,
                arrowPointingUp = true,
                cardBottomPadding = 0.dp,
                cardTopPadding = 120.dp,
            ),
            // 5. Floating Navigation Dock (Bottom Capsule)
            SpotlightStep(
                tag = "5 OF 5 • THREE-TAB DOCK",
                title = "Floating Tab Navigation",
                subtitle = "Everything in thumb reach",
                description = "Jump effortlessly between Today's Portion, Free Surah reading (independent of your daily bookmark), and your voice recitation recordings anytime.",
                accentColor = Color(0xFFA78BFA),
                targetTop = 790.dp,
                targetHeight = 66.dp,
                targetStart = 46.dp,
                targetWidth = 320.dp,
                targetCornerRadius = 999.dp,
                cardPlacement = CardPosition.BOTTOM,
                arrowOffsetDp = 190.dp,
                arrowPointingUp = false,
                cardBottomPadding = 100.dp,
                cardTopPadding = 0.dp,
            ),
        )
    }

    val currentStep = steps[stepIndex]

    // Dark semi-transparent scrim covering the entire screen
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xD2020A05))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* consume background taps so nothing underneath is clicked */ },
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Dynamic Spotlight Frame wrapping the real target element
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(160)) },
            label = "spotlight_target",
        ) { step ->
            Box(
                modifier = Modifier
                    .offset(x = step.targetStart, y = step.targetTop)
                    .size(width = step.targetWidth, height = step.targetHeight),
            ) {
                // Pulsing glowing highlight box
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(step.targetCornerRadius))
                        .background(step.accentColor.copy(alpha = 0.08f * pulseAlpha))
                        .border(
                            width = 2.dp,
                            color = step.accentColor.copy(alpha = pulseAlpha),
                            shape = RoundedCornerShape(step.targetCornerRadius),
                        ),
                )
            }
        }

        // Dynamic Coachmark Pointer Card (Placed above or below target, never colliding with tabs)
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            label = "coachmark_card",
            modifier = Modifier.fillMaxSize(),
        ) { step ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = step.cardTopPadding,
                        bottom = step.cardBottomPadding,
                        start = 18.dp,
                        end = 18.dp,
                    ),
                contentAlignment = if (step.cardPlacement == CardPosition.TOP) Alignment.TopCenter else Alignment.BottomCenter,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Upward Pointer Arrow (if card sits below the feature)
                    if (step.arrowPointingUp) {
                        PointerArrow(
                            pointingUp = true,
                            color = darkCard,
                            borderColor = step.accentColor,
                            modifier = Modifier
                                .offset(x = step.arrowOffsetDp - 12.dp)
                                .padding(bottom = 0.dp),
                        )
                    }

                    // Main Coachmark Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clayCard(
                                shape = RoundedCornerShape(24.dp),
                                backgroundColor = darkCard,
                                highlightColor = Color.White.copy(alpha = 0.12f),
                                shadowColor = Color.Black.copy(alpha = 0.75f),
                                elevation = 10.dp,
                                strokeWidth = 1.5.dp,
                            )
                            .border(1.5.dp, step.accentColor.copy(alpha = 0.70f), RoundedCornerShape(24.dp))
                            .padding(18.dp),
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
                                            backgroundColor = Color(0xFF142B20),
                                        )
                                        .padding(horizontal = 10.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = step.tag,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp,
                                        color = step.accentColor,
                                    )
                                }

                                TextButton(onClick = onDismiss) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Skip Tour",
                                            color = Color(0xFFA1B8AB),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Skip Tour",
                                            tint = Color(0xFFA1B8AB),
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Title & Subtitle
                            Text(
                                text = step.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = step.subtitle,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = step.accentColor.copy(alpha = 0.9f),
                            )

                            Spacer(Modifier.height(10.dp))

                            // Plain-Language Description
                            Text(
                                text = step.description,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFFC7DED2),
                            )

                            Spacer(Modifier.height(14.dp))

                            // Dot Progress Indicators + Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Dots (● ○ ○ ○ ○)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    repeat(steps.size) { index ->
                                        val isCurrent = stepIndex == index
                                        Box(
                                            modifier = Modifier
                                                .height(6.dp)
                                                .width(if (isCurrent) 18.dp else 6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isCurrent) step.accentColor else Color(0xFF234735)
                                                ),
                                        )
                                    }
                                }

                                // Buttons (Back & Next / Finish)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (stepIndex > 0) {
                                        OutlinedButton(
                                            onClick = { stepIndex-- },
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF28543E)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFFA1B8AB),
                                            ),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "Back",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }

                                    val isLast = stepIndex == steps.size - 1
                                    Button(
                                        onClick = {
                                            if (isLast) {
                                                onDismiss()
                                            } else {
                                                stepIndex++
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = emerald,
                                            contentColor = Color(0xFF061A10),
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = if (isLast) "Finish" else "Next",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (isLast) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = if (isLast) "Finish" else "Next",
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Downward Pointer Arrow (if card sits above the feature)
                    if (!step.arrowPointingUp) {
                        PointerArrow(
                            pointingUp = false,
                            color = darkCard,
                            borderColor = step.accentColor,
                            modifier = Modifier
                                .offset(x = step.arrowOffsetDp - 12.dp)
                                .padding(top = 0.dp),
                        )
                    }
                }
            }
        }
    }
}

private enum class CardPosition {
    TOP, BOTTOM
}

private data class SpotlightStep(
    val tag: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColor: Color,
    val targetTop: Dp,
    val targetHeight: Dp,
    val targetStart: Dp,
    val targetWidth: Dp,
    val targetCornerRadius: Dp,
    val cardPlacement: CardPosition,
    val arrowOffsetDp: Dp,
    val arrowPointingUp: Boolean,
    val cardTopPadding: Dp,
    val cardBottomPadding: Dp,
)

/**
 * Clean vector pointer arrow connecting the coachmark card to the highlighted target element.
 */
@Composable
private fun PointerArrow(
    pointingUp: Boolean,
    color: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = 24.dp, height = 12.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            if (pointingUp) {
                moveTo(w / 2f, 0f)
                lineTo(w, h)
                lineTo(0f, h)
            } else {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w / 2f, h)
            }
            close()
        }
        drawPath(path, color = color)
        // Draw the matching outline on the two pointing sides
        val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        if (pointingUp) {
            drawLine(borderColor, Offset(0f, h), Offset(w / 2f, 0f), stroke.width, stroke.cap)
            drawLine(borderColor, Offset(w / 2f, 0f), Offset(w, h), stroke.width, stroke.cap)
        } else {
            drawLine(borderColor, Offset(0f, 0f), Offset(w / 2f, h), stroke.width, stroke.cap)
            drawLine(borderColor, Offset(w / 2f, h), Offset(w, 0f), stroke.width, stroke.cap)
        }
    }
}
