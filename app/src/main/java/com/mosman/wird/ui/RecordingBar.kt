package com.mosman.wird.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * The recording bar, pinned to the top of the screen while you recite.
 *
 * It does not scroll. That is the whole point: the record button lives at the foot of the
 * page, so the moment you start reciting you scroll back up to read — and the first
 * version put every sign that recording was happening off-screen. Mutalib started a
 * recitation and could not tell it was running.
 *
 * The level meter is driven by real microphone amplitude, not a timer. A pulse on a timer
 * looks the same whether the mic is working or muted; this one only moves when it hears
 * you, which makes it evidence rather than decoration.
 */
@Composable
fun RecordingBar(
    seconds: Int,
    level: Float,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    // Smoothed, because raw peaks jitter. Fast enough to feel connected to your voice,
    // slow enough not to strobe.
    val eased by animateFloatAsState(targetValue = level, label = "mic level")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised)
            .safeDrawingPadding()
            .padding(horizontal = Scale.space4, vertical = Scale.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Scale.space3),
    ) {
        // A dot that breathes with what the mic hears. Never fully still while sound is
        // arriving, never larger than its own row.
        Box(
            modifier = Modifier
                .size(14.dp)
                .scale(0.55f + eased * 0.65f)
                .clip(CircleShape)
                .background(colors.onSurfaceRaised),
        )

        Meter(level = eased, modifier = Modifier.weight(1f))

        Text(
            text = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
            color = colors.onSurfaceRaised,
            style = TextStyle(fontSize = Scale.body),
        )

        TextButton(
            onClick = onStop,
            modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
        ) {
            Text("Done", color = colors.onSurfaceRaised, style = TextStyle(fontSize = Scale.body))
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
        ) {
            Text("Discard", color = colors.onSurfaceRaised, style = TextStyle(fontSize = Scale.caption))
        }
    }
}

/** Five bars that rise and fall with your voice. Quiet at rest, never silent-looking. */
@Composable
private fun Meter(level: Float, modifier: Modifier = Modifier) {
    val colors = LocalWirdColors.current
    // Each bar reacts to a slightly different slice of the level, so the row ripples
    // instead of moving as one block — which reads as sound rather than as a progress bar.
    val weights = listOf(0.55f, 0.8f, 1f, 0.8f, 0.55f)
    Row(
        modifier = modifier.height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        weights.forEach { w ->
            val h = (3f + level * w * 17f).dp
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.onSurfaceRaised),
            )
        }
        Spacer(Modifier.width(Scale.space1))
    }
}
