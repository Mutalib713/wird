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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Interactive Feature Spotlight Coachmark Tour with Curly Thought-Map Arrow Callouts.
 *
 * Implements the user's approved reference visual:
 * - Playful, hand-drawn style curly loop arrow pointing directly at each feature.
 * - Callout text sits directly adjacent to where it is pointing (close by, 40-70dp).
 * - Spotlight cutout in the dark scrim so the target feature shines through with full clarity.
 * - Floating text directly on the scrim (no heavy card container / box).
 * - Vibrant pill button ("Next" / "Okay, I got it") + skip option.
 * - Zero emoji clutter (Sacred Rule 9).
 */
@Composable
fun ToolkitSpotlightOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onStepChanged: (Int) -> Unit = {},
) {
    var stepIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(stepIndex) {
        onStepChanged(stepIndex)
    }

    val coral = Color(0xFFFB7185) // Reference warm coral/rose accent
    val textTint = Color(0xFFFECDD3) // Soft rose description text

    // Pulsing aura animation for the spotlight indicator
    val transition = rememberInfiniteTransition(label = "spotlight_pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    // Exact focal anchor points measured live on physical Pixel 6 Pro
    val steps = remember {
        listOf(
            // 1. Daily Portion Card (Full Section)
            SpotlightStep(
                title = "Your Daily Portion",
                description = "Committed recitation with streak protection. Follow your assigned pages, mark done, or recite out loud.",
                accentColor = coral,
                cutoutLeft = 16.dp,
                cutoutTop = 254.dp,
                cutoutRight = 395.dp,
                cutoutBottom = 633.dp,
                cornerRadius = 24.dp,
                targetX = 206.dp,
                targetY = 633.dp,
                arrowStartX = 206.dp,
                arrowStartY = 695.dp,
                pointingUp = true,
                textTop = 700.dp,
            ),
            // 2. Offline Voice Auditor (Recite Button)
            SpotlightStep(
                title = "Recite Out Loud",
                description = "Tap 'Recite' to recite by heart. Built-in Whisper AI checks your words 100% offline with zero data.",
                accentColor = coral,
                cutoutLeft = 28.dp,
                cutoutTop = 472.dp,
                cutoutRight = 114.dp,
                cutoutBottom = 572.dp,
                cornerRadius = 18.dp,
                targetX = 71.dp,
                targetY = 572.dp,
                arrowStartX = 120.dp,
                arrowStartY = 640.dp,
                pointingUp = true,
                textTop = 645.dp,
            ),
            // 3. Reading Tracks & Schedules (Track Pill in header)
            SpotlightStep(
                title = "Reading Tracks & Schedules",
                description = "Maintain parallel tracks for Tilāwah, Ḥifẓ, or Ramadan Khatmah with independent schedules. Tap anytime to switch tracks.",
                accentColor = coral,
                cutoutLeft = 180.dp,
                cutoutTop = 46.dp,
                cutoutRight = 350.dp,
                cutoutBottom = 90.dp,
                cornerRadius = 999.dp,
                targetX = 265.dp,
                targetY = 90.dp,
                arrowStartX = 265.dp,
                arrowStartY = 150.dp,
                pointingUp = true,
                textTop = 155.dp,
            ),
            // 4. Wird AI Companion & Tafsir (Today's Check-in Card - auto-scrolled into view)
            SpotlightStep(
                title = "Wird AI Companion & Tafsir",
                description = "Daily check-in and reflection. Tap 'Open Chat' to explore numbered Tafsir Ibn Kathir, translations, or pause without streak guilt.",
                accentColor = coral,
                cutoutLeft = 16.dp,
                cutoutTop = 140.dp,
                cutoutRight = 395.dp,
                cutoutBottom = 335.dp,
                cornerRadius = 24.dp,
                targetX = 206.dp,
                targetY = 335.dp,
                arrowStartX = 206.dp,
                arrowStartY = 415.dp,
                pointingUp = true,
                textTop = 430.dp,
            ),
            // 6. Floating Navigation Dock (Bottom Capsule)
            SpotlightStep(
                title = "Floating Navigation Dock",
                description = "Jump effortlessly between Today's Portion, Free Surah reading, and your recitations in Your Wird.",
                accentColor = coral,
                cutoutLeft = 24.dp,
                cutoutTop = 778.dp,
                cutoutRight = 388.dp,
                cutoutBottom = 852.dp,
                cornerRadius = 999.dp,
                targetX = 206.dp,
                targetY = 778.dp,
                arrowStartX = 206.dp,
                arrowStartY = 720.dp,
                pointingUp = false,
                textTop = 530.dp,
            ),
        )
    }

    // Full screen overlay consuming taps
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* consume background taps */ },
            ),
    ) {
        val step = steps[stepIndex]

        // 1. Full-screen Canvas with Offscreen Compositing for Scrim + Spotlight Cutout + Curly Arrow
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
        ) {
            // A. Dark translucent scrim (85% opacity dark emerald)
            drawRect(color = Color(0xD907140D))

            // B. Spotlight Cutout Hole - punched through scrim so feature shines bright
            val cutoutWidth = (step.cutoutRight - step.cutoutLeft).toPx()
            val cutoutHeight = (step.cutoutBottom - step.cutoutTop).toPx()
            val cRadius = step.cornerRadius.toPx()

            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(step.cutoutLeft.toPx(), step.cutoutTop.toPx()),
                size = Size(cutoutWidth, cutoutHeight),
                cornerRadius = CornerRadius(cRadius, cRadius),
                blendMode = BlendMode.Clear,
            )

            // C. Highlight Border around cutout hole
            drawRoundRect(
                color = step.accentColor.copy(alpha = 0.85f * pulseAlpha),
                topLeft = Offset(step.cutoutLeft.toPx(), step.cutoutTop.toPx()),
                size = Size(cutoutWidth, cutoutHeight),
                cornerRadius = CornerRadius(cRadius, cRadius),
                style = Stroke(width = 2.dp.toPx()),
            )

            // D. Curly Thought-Map Arrow with loop
            drawCurlyArrow(
                start = Offset(step.arrowStartX.toPx(), step.arrowStartY.toPx()),
                target = Offset(step.targetX.toPx(), step.targetY.toPx()),
                pointingUp = step.pointingUp,
                color = step.accentColor,
                strokeWidth = 2.8.dp.toPx(),
                pulseAlpha = pulseAlpha,
            )
        }

        // 2. Floating Callout Text & Action Buttons (Adjacent to feature, no box container)
        AnimatedContent(
            targetState = stepIndex,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            label = "tour_step",
            modifier = Modifier.fillMaxSize(),
        ) { targetIndex ->
            val currentStep = steps[targetIndex]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset(y = currentStep.textTop)
                        .fillMaxWidth(),
                ) {
                    // Title (Bold white)
                    Text(
                        text = currentStep.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(8.dp))

                    // Description (Soft rose/coral tint, close to feature)
                    Text(
                        text = currentStep.description,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Normal,
                        color = textTint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )

                    Spacer(Modifier.height(18.dp))

                    // Action Buttons Row: [Back] (if > 0) + [Next / Okay, I got it]
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (targetIndex > 0) {
                            OutlinedButton(
                                onClick = { stepIndex-- },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFCBD5E1),
                                ),
                                border = BorderStroke(1.dp, Color(0xFF475569)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(44.dp),
                            ) {
                                Text(
                                    text = "Back",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                        }

                        Button(
                            onClick = {
                                if (stepIndex < steps.lastIndex) {
                                    stepIndex++
                                } else {
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentStep.accentColor,
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(44.dp),
                        ) {
                            Text(
                                text = if (targetIndex == steps.lastIndex) "Okay, I got it" else "Next",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Dots progress & Skip
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (i in steps.indices) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (i == targetIndex) 7.dp else 4.5.dp)
                                    .background(
                                        color = if (i == targetIndex) Color.White else Color.White.copy(alpha = 0.35f),
                                        shape = CircleShape,
                                    ),
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Text(
                            text = "Skip tour",
                            color = Color(0xFFA1B8AB),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onDismiss() },
                        )
                    }
                }
            }
        }
    }
}

private data class SpotlightStep(
    val title: String,
    val description: String,
    val accentColor: Color,
    val cutoutLeft: Dp,
    val cutoutTop: Dp,
    val cutoutRight: Dp,
    val cutoutBottom: Dp,
    val cornerRadius: Dp,
    val targetX: Dp,
    val targetY: Dp,
    val arrowStartX: Dp,
    val arrowStartY: Dp,
    val pointingUp: Boolean,
    val textTop: Dp,
)

/**
 * Draws an organic, hand-drawn style curly arrow with a loop pointing directly at [target].
 */
private fun DrawScope.drawCurlyArrow(
    start: Offset,
    target: Offset,
    pointingUp: Boolean,
    color: Color,
    strokeWidth: Float,
    pulseAlpha: Float,
) {
    val sx = start.x
    val sy = start.y
    val tx = target.x
    val ty = target.y
    val dx = tx - sx
    val dy = ty - sy
    val dist = hypot(dx, dy).coerceAtLeast(1f)

    // Unit normal vector (perpendicular to direction)
    val nx = -dy / dist
    val ny = dx / dist

    // Loop center at ~45% distance along the curve
    val loopCx = sx + dx * 0.45f + nx * 8.dp.toPx()
    val loopCy = sy + dy * 0.45f + ny * 8.dp.toPx()
    val loopR = 11.dp.toPx()

    // 1. Entry curve from start to loop entry
    val entryX = loopCx + nx * 4.dp.toPx()
    val entryY = loopCy + ny * 4.dp.toPx()
    val c1X = sx + dx * 0.15f + nx * 16.dp.toPx()
    val c1Y = sy + dy * 0.15f + ny * 16.dp.toPx()

    val path = Path().apply {
        moveTo(sx, sy)
        quadraticTo(c1X, c1Y, entryX, entryY)

        // 2. Loop curl (full 360 deg sweep)
        val baseAngle = atan2(entryY - loopCy, entryX - loopCx)
        val steps = 24
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val angle = if (pointingUp) {
                baseAngle + (Math.PI.toFloat() * 2.15f) * t
            } else {
                baseAngle - (Math.PI.toFloat() * 2.15f) * t
            }
            val px = loopCx + loopR * cos(angle)
            val py = loopCy + loopR * sin(angle)
            lineTo(px, py)
        }

        // 3. Exit curve from loop to target
        val c2X = tx + nx * 10.dp.toPx()
        val c2Y = ty - (dy * 0.20f)
        quadraticTo(c2X, c2Y, tx, ty)
    }

    // Glowing under-stroke
    drawPath(
        path = path,
        color = color.copy(alpha = 0.25f * pulseAlpha),
        style = Stroke(width = strokeWidth * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    // Core stroke
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Arrowhead at target
    val c2X = tx + nx * 10.dp.toPx()
    val c2Y = ty - (dy * 0.20f)
    val tipDx = tx - c2X
    val tipDy = ty - c2Y
    val tipAngle = atan2(tipDy, tipDx)
    val wingLen = 11.dp.toPx()
    val wingAngle = 0.55f // ~31 degrees

    val w1X = tx - wingLen * cos(tipAngle - wingAngle)
    val w1Y = ty - wingLen * sin(tipAngle - wingAngle)
    val w2X = tx - wingLen * cos(tipAngle + wingAngle)
    val w2Y = ty - wingLen * sin(tipAngle + wingAngle)

    drawLine(
        color = color,
        start = target,
        end = Offset(w1X, w1Y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = target,
        end = Offset(w2X, w2Y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )

    // Focal beacon circle right at target point
    drawCircle(
        color = color.copy(alpha = 0.25f * pulseAlpha),
        radius = 8.dp.toPx(),
        center = target,
    )
    drawCircle(
        color = color,
        radius = 3.dp.toPx(),
        center = target,
    )
}
