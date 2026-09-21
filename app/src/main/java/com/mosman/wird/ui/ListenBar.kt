package com.mosman.wird.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.R
import com.mosman.wird.audio.AudioState
import com.mosman.wird.audio.PortionAudio
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * The controls that appear once recitation starts. **His ask, 2026-08-19**, holding up the
 * player in the app he actually reads in: *"when you start listening it should have options
 * like forward pause replay, it has like 1 2 3 and infinity."*
 *
 * **What was there before: a single button that started it and a second tap that stopped it.**
 * No way to hold it, no way to hear an ayah again without restarting the whole portion, and
 * no way to sit on one ayah — which is the thing a memoriser does most.
 *
 * ### Why the repeat counts ayahs rather than portions
 *
 * ⚠ Repeating a whole portion three times is *listening*. Repeating **one ayah** three times
 * is how memorisation is actually done, and § 5r already gives this app a memorising mode that
 * had nothing behind it but relabelled buttons. The infinity is the same control's honest end:
 * stay here until I say otherwise.
 *
 * ### Two details taken from how players behave, not from how they look
 *
 * - **Back restarts the current ayah first.** Once you are more than two seconds in, "back"
 *   means *"I missed that"*, and only pressing it again means the ayah before. Every music
 *   player does this and nobody notices, which is the point.
 * - **The ayah being heard is named.** The bar says *"Al-Kahf 18:10 · 3 of 12"*, because the
 *   whole reason this app plays one file per ayah rather than one per page is knowing which
 *   one you are on.
 *
 * **Play, pause and the skips are drawn here; the loop is Font Awesome's.** A triangle and two
 * bars are marks, and § 5ag's rule only sends a shape out of house when it is one anyone can
 * name and nobody can fake by hand. Two arrows chasing round a loop is that; a triangle is not.
 */
@Composable
fun ListenBar(
    audio: AudioState,
    repeatEach: Int,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    reciter: String = "",
    onChangeReciter: () -> Unit = {},
    repeatRange: Int = 1,
    onRepeatRange: (() -> Unit)? = null,
) {
    val colors = LocalWirdColors.current
    val playing = audio as? AudioState.Playing
    val paused = audio as? AudioState.Paused
    val verseKey = playing?.verseKey ?: paused?.verseKey ?: return
    val index = playing?.index ?: paused?.index ?: 0
    val total = playing?.total ?: paused?.total ?: 0
    val isBuffering = playing?.isBuffering ?: false
    val rangeCycle = playing?.rangeCycle ?: paused?.rangeCycle ?: 1
    val totalCycles = playing?.totalCycles ?: paused?.totalCycles ?: repeatRange

    // Floating card container: elevated with shadow and rounded corners rather than docked edge-to-edge
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.hairline, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            // ---- what you are hearing ----
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ayahLabel(verseKey),
                        color = colors.onSurfaceRaised,
                        style = TextStyle(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                    )
                    if (reciter.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onChangeReciter),
                        ) {
                            Text(
                                text = reciter,
                                color = colors.accent,
                                style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Change reciter",
                                tint = colors.accent,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isBuffering) {
                        Text(
                            text = "Buffering…",
                            color = colors.accent,
                            style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    val cycleInfo = if (totalCycles > 1 || totalCycles == PortionAudio.FOREVER) {
                        val cTotal = if (totalCycles == PortionAudio.FOREVER) "∞" else totalCycles.toString()
                        "Cycle $rangeCycle of $cTotal · "
                    } else ""
                    Text(
                        text = "$cycleInfo${index + 1} of $total",
                        color = colors.textSecondary,
                        style = TextStyle(fontSize = 12.sp),
                    )
                }
            }

            Spacer(Modifier.height(Scale.space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // If a range is playing with range repeat, repeat control toggles range loop; else ayah repeat
                if (onRepeatRange != null && totalCycles > 1) {
                    RangeRepeatControl(totalCycles, onRepeatRange)
                } else {
                    RepeatControl(repeatEach, onRepeat)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tap("Previous ayah", onPrevious) { SkipGlyph(it, forward = false) }
                    Spacer(Modifier.width(Scale.space2))

                    // The one filled control, because it is the one you reach for in the dark.
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                            .clickable(onClick = onPlayPause)
                            .semantics { contentDescription = if (paused != null) "Play" else "Pause" },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (paused != null) PlayGlyph(colors.surface) else PauseGlyph(colors.surface)
                    }

                    Spacer(Modifier.width(Scale.space2))
                    Tap("Next ayah", onNext) { SkipGlyph(it, forward = true) }
                }

                Tap("Stop", onStop) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = it,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RangeRepeatControl(repeatRange: Int, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    val on = repeatRange != 1
    val tint = if (on) colors.accent else colors.textSecondary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Scale.radius * 2))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Repeat range: ${repeatLabel(repeatRange)}" }
            .padding(horizontal = Scale.space3, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_repeat),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Range ${repeatLabel(repeatRange)}",
            color = tint,
            style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Medium),
        )
    }
}

/**
 * 1 → 2 → 3 → ∞ → 1. **The count is a number on screen, not a state you have to infer.**
 *
 * A loop icon that merely lights up tells you repeat is *on* and leaves you counting
 * repetitions to work out how many. His reference shows the figure, and the figure is the
 * whole feature.
 */
@Composable
private fun RepeatControl(repeatEach: Int, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    val on = repeatEach != 1
    val tint = if (on) colors.accent else colors.textSecondary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Scale.radius * 2))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Repeat each ayah: ${repeatLabel(repeatEach)}" }
            .padding(horizontal = Scale.space3, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_repeat),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = repeatLabel(repeatEach),
            color = tint,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
        )
    }
}

/** The next count in the cycle, so the caller never has to know the order. */
fun nextRepeat(current: Int): Int = when (current) {
    1 -> 2
    2 -> 3
    3 -> PortionAudio.FOREVER
    else -> 1
}

fun nextRangeRepeat(current: Int): Int = when (current) {
    1 -> 2
    2 -> 3
    3 -> 5
    5 -> 10
    10 -> PortionAudio.FOREVER
    else -> 1
}

private fun repeatLabel(repeatEach: Int) =
    if (repeatEach == PortionAudio.FOREVER) "∞" else "$repeatEach"

/** "Al-Kahf 18:10", or the bare key if the sūrah is somehow unknown. */
private fun ayahLabel(verseKey: String): String {
    val surah = verseKey.substringBefore(':').toIntOrNull() ?: return verseKey
    val name = com.mosman.wird.domain.SurahIndex.byNumber(surah)?.name ?: return verseKey
    return "$name $verseKey"
}

@Composable
private fun Tap(label: String, onClick: () -> Unit, glyph: @Composable (Color) -> Unit) {
    val colors = LocalWirdColors.current
    Box(
        modifier = Modifier
            .size(Scale.minTarget)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        glyph(colors.onSurfaceRaised)
    }
}

@Composable
private fun PlayGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        drawPath(
            Path().apply {
                moveTo(w * 0.24f, h * 0.14f)
                lineTo(w * 0.86f, h * 0.50f)
                lineTo(w * 0.24f, h * 0.86f)
                close()
            },
            color = tint,
        )
    }
}

@Composable
private fun PauseGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val bar = w * 0.26f
        drawRect(color = tint, topLeft = Offset(w * 0.16f, h * 0.10f), size = androidx.compose.ui.geometry.Size(bar, h * 0.80f))
        drawRect(color = tint, topLeft = Offset(w * 0.58f, h * 0.10f), size = androidx.compose.ui.geometry.Size(bar, h * 0.80f))
    }
}

/** A triangle with a bar against it. Mirrored for the other direction. */
@Composable
private fun SkipGlyph(tint: Color, forward: Boolean) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val bar = w * 0.13f
        val path = Path().apply {
            if (forward) {
                moveTo(w * 0.10f, h * 0.16f)
                lineTo(w * 0.66f, h * 0.50f)
                lineTo(w * 0.10f, h * 0.84f)
            } else {
                moveTo(w * 0.90f, h * 0.16f)
                lineTo(w * 0.34f, h * 0.50f)
                lineTo(w * 0.90f, h * 0.84f)
            }
            close()
        }
        drawPath(path, color = tint)
        drawRect(
            color = tint,
            topLeft = Offset(if (forward) w * 0.72f else w * 0.15f, h * 0.16f),
            size = androidx.compose.ui.geometry.Size(bar, h * 0.68f),
        )
    }
}
