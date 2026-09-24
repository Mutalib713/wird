package com.mosman.wird.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mosman.wird.domain.TrackType

/**
 * Pure vector home/space glyph — strictly avoiding informal emojis in compliance with Design Studio rules.
 */
@Composable
fun LifeSpaceGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // House roof
        val roof = Path().apply {
            moveTo(w * 0.15f, h * 0.45f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.85f, h * 0.45f)
        }
        drawPath(roof, color = tint, style = stroke)

        // House walls & floor
        val walls = Path().apply {
            moveTo(w * 0.25f, h * 0.45f)
            lineTo(w * 0.25f, h * 0.82f)
            lineTo(w * 0.75f, h * 0.82f)
            lineTo(w * 0.75f, h * 0.45f)
        }
        drawPath(walls, color = tint, style = stroke)

        // Door outline
        val door = Path().apply {
            moveTo(w * 0.42f, h * 0.82f)
            lineTo(w * 0.42f, h * 0.60f)
            lineTo(w * 0.58f, h * 0.60f)
            lineTo(w * 0.58f, h * 0.82f)
        }
        drawPath(door, color = tint, style = stroke)
    }
}

/**
 * Pure vector snowflake/dormant glyph indicating a frozen life space.
 */
@Composable
fun FreezeGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)

        // 3 intersecting lines
        drawLine(tint, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.85f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.2f, h * 0.32f), Offset(w * 0.8f, h * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.2f, h * 0.68f), Offset(w * 0.8f, h * 0.32f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

/**
 * Vector icon for TrackType (Ḥifẓ, Tilāwah, Murāja'ah).
 */
@Composable
fun TrackTypeGlyph(
    type: TrackType,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (type) {
            TrackType.HIFZ -> {
                // Heart / memory ribbon glyph
                val heart = Path().apply {
                    moveTo(w * 0.5f, h * 0.80f)
                    cubicTo(w * 0.2f, h * 0.55f, w * 0.15f, h * 0.30f, w * 0.32f, h * 0.22f)
                    cubicTo(w * 0.42f, h * 0.18f, w * 0.48f, h * 0.30f, w * 0.5f, h * 0.35f)
                    cubicTo(w * 0.52f, h * 0.30f, w * 0.58f, h * 0.18f, w * 0.68f, h * 0.22f)
                    cubicTo(w * 0.85f, h * 0.30f, w * 0.8f, h * 0.55f, w * 0.5f, h * 0.80f)
                }
                drawPath(heart, color = tint, style = stroke)
            }
            TrackType.TILAWAH -> {
                // Open book glyph
                val book = Path().apply {
                    moveTo(w * 0.5f, h * 0.35f)
                    cubicTo(w * 0.38f, h * 0.22f, w * 0.22f, h * 0.24f, w * 0.15f, h * 0.28f)
                    lineTo(w * 0.15f, h * 0.78f)
                    cubicTo(w * 0.22f, h * 0.74f, w * 0.38f, h * 0.72f, w * 0.5f, h * 0.85f)
                    cubicTo(w * 0.62f, h * 0.72f, w * 0.78f, h * 0.74f, w * 0.85f, h * 0.78f)
                    lineTo(w * 0.85f, h * 0.28f)
                    cubicTo(w * 0.78f, h * 0.24f, w * 0.62f, h * 0.22f, w * 0.5f, h * 0.35f)
                }
                drawPath(book, color = tint, style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.35f), Offset(w * 0.5f, h * 0.85f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            TrackType.REVISION -> {
                // Cyclic review arrows
                drawArc(
                    color = tint,
                    startAngle = 45f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.18f),
                    size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.64f),
                    style = stroke,
                )
                // Arrowhead
                val arrow = Path().apply {
                    moveTo(w * 0.68f, h * 0.10f)
                    lineTo(w * 0.82f, h * 0.24f)
                    lineTo(w * 0.65f, h * 0.32f)
                }
                drawPath(arrow, color = tint, style = stroke)
            }
        }
    }
}

/**
 * Vector indicator for automatic scheduling vs manual.
 */
@Composable
fun AutoScheduleGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(13.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)

        drawCircle(
            color = tint,
            radius = w * 0.38f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = stroke,
        )
        // Clock hands
        drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.26f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.68f, h * 0.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

/**
 * Vector indicator for manual track scheduling.
 */
@Composable
fun ManualScheduleGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(13.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)

        // Hand sliders / tuning glyph
        drawLine(tint, Offset(w * 0.18f, h * 0.35f), Offset(w * 0.82f, h * 0.35f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawCircle(tint, radius = 2.dp.toPx(), center = Offset(w * 0.38f, h * 0.35f))

        drawLine(tint, Offset(w * 0.18f, h * 0.68f), Offset(w * 0.82f, h * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawCircle(tint, radius = 2.dp.toPx(), center = Offset(w * 0.64f, h * 0.68f))
    }
}
