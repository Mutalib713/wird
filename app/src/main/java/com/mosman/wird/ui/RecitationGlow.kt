package com.mosman.wird.ui

import android.provider.Settings
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import com.mosman.wird.ui.theme.LocalWirdColors

/**
 * The glow that says the app is listening.
 *
 * **Purpose:** mark the moment recording begins, and keep saying so while it runs. Siri
 * and Gemini both do this at the screen edges, and the reason it works here is the same
 * reason it works there — the edges are the one place an ambient effect can live without
 * covering the thing you are looking at. Nothing is drawn over the Qur'an.
 *
 * **Trigger:** [active] going true. **Duration:** 550 ms to bloom in, 350 ms out.
 * **Easing:** ease-out, so it arrives quickly and settles rather than creeping.
 *
 * Once in, it breathes on a slow four-second cycle *and* swells with [level], so the glow
 * is loudest when you are. The breathing exists so the effect is alive in the pauses
 * between ayahs rather than dying every time you take a breath.
 *
 * **Fallback:** if the phone has animations turned off — an accessibility setting, and
 * the thing to honour rather than override — the glow appears at a steady strength with
 * no bloom and no breathing. Still visible, just still.
 */
@Composable
fun RecitationGlow(active: Boolean, level: Float, modifier: Modifier = Modifier) {
    val colors = LocalWirdColors.current
    val context = LocalContext.current

    val animationsOn = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        }.getOrDefault(true)
    }

    val bloom by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (!animationsOn) 0 else if (active) 550 else 350,
            easing = EaseOutCubic,
        ),
        label = "glow bloom",
    )

    val breath by if (animationsOn) {
        rememberInfiniteTransition(label = "breath").animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2_000, easing = EaseOutCubic),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "breath",
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.8f) }
    }

    val voice by animateFloatAsState(targetValue = level, label = "glow level")

    if (bloom <= 0.01f) return

    // Kept well under half: this is a hint at the edge of vision, not a vignette. At full
    // voice it reaches 0.42 alpha at the very edge and fades to nothing a fifth of the way
    // in, so the text never sits on top of it.
    val strength = bloom * (0.18f + breath * 0.10f + voice * 0.22f)
    val tint = colors.accent

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val depth = size.minDimension * 0.22f * (0.7f + bloom * 0.3f)
                fun edge(from: Offset, to: Offset, topLeft: Offset, boxSize: Size) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(tint.copy(alpha = strength), tint.copy(alpha = 0f)),
                            start = from,
                            end = to,
                        ),
                        topLeft = topLeft,
                        size = boxSize,
                    )
                }
                edge(Offset(0f, 0f), Offset(0f, depth), Offset(0f, 0f), Size(size.width, depth))
                edge(
                    Offset(0f, size.height), Offset(0f, size.height - depth),
                    Offset(0f, size.height - depth), Size(size.width, depth),
                )
                edge(Offset(0f, 0f), Offset(depth, 0f), Offset(0f, 0f), Size(depth, size.height))
                edge(
                    Offset(size.width, 0f), Offset(size.width - depth, 0f),
                    Offset(size.width - depth, 0f), Size(depth, size.height),
                )
            },
    )
}
