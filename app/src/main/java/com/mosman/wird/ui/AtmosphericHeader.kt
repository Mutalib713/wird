package com.mosman.wird.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.mosman.wird.domain.ReadingTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.StrokeJoin
import com.mosman.wird.R
import com.mosman.wird.ui.theme.clayPill

/**
 * Atmospheric Dawn Header matching the user's approved reference design.
 * Features the dawn sky gradient, glowing crescent, flying birds, mosque silhouette,
 * serif Wird logo with leaf sprout dot, bookmark button, Al-Fatihah position pill,
 * and the morning greeting.
 */
@Composable
fun AtmosphericHeader(
    readerName: String,
    onOpenPosition: () -> Unit,
    onOpenBookmarks: () -> Unit,
    modifier: Modifier = Modifier,
    positionText: String = "Al-Fātihah 1, page 1",
    activeTrack: ReadingTrack? = null,
    totalDueTracksCount: Int = 1,
    activeTrackDueIndex: Int = 0,
    onOpenTrackPicker: () -> Unit = {},
    onMenu: (() -> Unit)? = null,
    menu: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0D2720),
                        Color(0xFF13332B),
                        Color(0xFF274136),
                        Color(0xFF56553F),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                )
            )
            .statusBarsPadding()
            .padding(top = 16.dp, bottom = 42.dp, start = 18.dp, end = 18.dp)
    ) {
        // Atmospheric artwork canvas: Crescent, birds, and mosque silhouette
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Glowing crescent moon (positioned in upper sky clear of top-right buttons)
            val moonCenter = Offset(w * 0.65f, h * 0.28f)
            val moonRadius = 13.dp.toPx()
            drawCircle(
                color = Color(0xFFFFF6DC).copy(alpha = 0.95f),
                radius = moonRadius,
                center = moonCenter,
            )
            // Cutout circle for crescent shape
            drawCircle(
                color = Color(0xFF17382D),
                radius = moonRadius * 0.88f,
                center = Offset(moonCenter.x - moonRadius * 0.45f, moonCenter.y - moonRadius * 0.25f),
            )

            // Flying birds
            val birdStroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            val birdColor = Color(0xFF18362B)
            
            // Bird 1
            val bird1 = Path().apply {
                moveTo(w * 0.68f, h * 0.38f)
                quadraticTo(w * 0.70f, h * 0.35f, w * 0.72f, h * 0.38f)
                quadraticTo(w * 0.74f, h * 0.35f, w * 0.76f, h * 0.38f)
            }
            drawPath(bird1, color = birdColor, style = birdStroke)

            // Bird 2
            val bird2 = Path().apply {
                moveTo(w * 0.76f, h * 0.34f)
                quadraticTo(w * 0.775f, h * 0.31f, w * 0.79f, h * 0.34f)
                quadraticTo(w * 0.805f, h * 0.31f, w * 0.82f, h * 0.34f)
            }
            drawPath(bird2, color = birdColor, style = birdStroke)

            // Mosque silhouette (Minaret, Domes, Base)
            val mosqueColor = Color(0xFF132D24).copy(alpha = 0.95f)
            
            // Slender secondary minaret in the background
            val secMinaretLeft = w * 0.71f
            drawRect(
                color = mosqueColor.copy(alpha = 0.8f),
                topLeft = Offset(secMinaretLeft - 3.dp.toPx(), h * 0.42f),
                size = Size(6.dp.toPx(), h * 0.58f),
            )
            val secMinaretTip = Path().apply {
                moveTo(secMinaretLeft, h * 0.36f)
                lineTo(secMinaretLeft - 3.dp.toPx(), h * 0.42f)
                lineTo(secMinaretLeft + 3.dp.toPx(), h * 0.42f)
                close()
            }
            drawPath(secMinaretTip, color = mosqueColor.copy(alpha = 0.8f))

            // Main minaret tower
            val minaretLeft = w * 0.85f
            drawRect(
                color = mosqueColor,
                topLeft = Offset(minaretLeft - 4.dp.toPx(), h * 0.35f),
                size = Size(8.dp.toPx(), h * 0.65f),
            )
            // Minaret balcony
            drawRoundRect(
                color = mosqueColor,
                topLeft = Offset(minaretLeft - 7.dp.toPx(), h * 0.48f),
                size = Size(14.dp.toPx(), 4.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            )
            // Minaret spire tip
            val minaretTip = Path().apply {
                moveTo(minaretLeft, h * 0.28f)
                lineTo(minaretLeft - 4.dp.toPx(), h * 0.35f)
                lineTo(minaretLeft + 4.dp.toPx(), h * 0.35f)
                close()
            }
            drawPath(minaretTip, color = mosqueColor)
            drawCircle(
                color = Color(0xFFE8DCB8),
                radius = 2.dp.toPx(),
                center = Offset(minaretLeft, h * 0.27f),
            )

            // Main central dome
            val domePath = Path().apply {
                val domeCenterX = w * 0.74f
                val domeRadius = 40.dp.toPx()
                moveTo(domeCenterX - domeRadius, h)
                cubicTo(
                    domeCenterX - domeRadius, h - domeRadius * 1.35f,
                    domeCenterX + domeRadius, h - domeRadius * 1.35f,
                    domeCenterX + domeRadius, h
                )
                close()
            }
            drawPath(domePath, color = mosqueColor)

            // Dome finial
            drawLine(
                color = mosqueColor,
                start = Offset(w * 0.74f, h - 58.dp.toPx()),
                end = Offset(w * 0.74f, h - 45.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = Color(0xFFE8DCB8),
                radius = 2.dp.toPx(),
                center = Offset(w * 0.74f, h - 59.dp.toPx()),
            )
        }

        // Foreground content
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top branding row: Serif Wird logo + Leaf sprout dot + Bookmark button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "W",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color(0xFFFFFFFF),
                        )
                        // Dotless i with green leaf sprout on top
                        Box(contentAlignment = Alignment.BottomCenter) {
                            Canvas(modifier = Modifier.size(12.dp, 24.dp)) {
                                // Leaf sprout
                                val leaf = Path().apply {
                                    moveTo(2.dp.toPx(), 10.dp.toPx())
                                    cubicTo(
                                        3.dp.toPx(), 4.dp.toPx(),
                                        6.dp.toPx(), 1.dp.toPx(),
                                        11.dp.toPx(), 0.5.dp.toPx()
                                    )
                                    cubicTo(
                                        11.dp.toPx(), 6.dp.toPx(),
                                        7.dp.toPx(), 9.5.dp.toPx(),
                                        2.dp.toPx(), 10.dp.toPx()
                                    )
                                    close()
                                }
                                drawPath(leaf, color = Color(0xFF7EBB6A))
                            }
                            Text(
                                text = "ı",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color(0xFFFFFFFF),
                            )
                        }
                        Text(
                            text = "rd",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color(0xFFFFFFFF),
                        )
                    }
                    Text(
                        text = "Qur'an Companion",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFC0D3C9),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Reading Track Pill with due count badge (e.g. 1 of 3)
                    if (activeTrack != null) {
                        val badgeText = if (totalDueTracksCount > 1 && activeTrackDueIndex >= 0) "${activeTrackDueIndex + 1} of $totalDueTracksCount" else null

                        Box(
                            modifier = Modifier
                                .clayPill(
                                    shape = RoundedCornerShape(999.dp),
                                    backgroundColor = Color(0xFF16382D).copy(alpha = 0.88f),
                                    highlightColor = Color.White.copy(alpha = 0.35f),
                                    shadowColor = Color.Black.copy(alpha = 0.4f),
                                )
                                .clickable(onClick = onOpenTrackPicker)
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                TrackTypeGlyph(
                                    type = activeTrack.type,
                                    tint = Color(0xFF8DE0A6),
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = activeTrack.name.take(14).trimEnd(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                if (badgeText != null) {
                                    Text(
                                        text = "·",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.4f),
                                    )
                                    Text(
                                        text = badgeText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFF9C86A),
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Switch Track",
                                    tint = Color(0xFF90A99C),
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }

                    // Overflow menu / settings button
                    onMenu?.let { openMenu ->
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clayPill(
                                        shape = RoundedCornerShape(12.dp),
                                        backgroundColor = Color(0xFF16382D).copy(alpha = 0.85f),
                                        highlightColor = Color.White.copy(alpha = 0.35f),
                                        shadowColor = Color.Black.copy(alpha = 0.4f),
                                    )
                                    .clickable(onClick = openMenu),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "More",
                                    tint = Color(0xFFE4E9E5),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            menu()
                        }
                    }
                }
            }

            Spacer(Modifier.height(34.dp))

            val currentHour = remember { LocalTime.now().hour }
            val greeting = remember(currentHour) {
                when (currentHour) {
                    in 5..11 -> "Good morning"
                    in 12..16 -> "Good afternoon"
                    else -> "Good evening"
                }
            }
            val isDaytime = currentHour in 6..17

            val hijriDate = remember {
                runCatching {
                    val today = LocalDate.now()
                    val h = HijrahDate.from(today)
                    val monthNames = listOf(
                        "Muḥarram", "Ṣafar", "Rabīʿ al-Awwal", "Rabīʿ al-Thānī",
                        "Jumādā al-Ūlā", "Jumādā al-Ākhirah", "Rajab", "Shaʿbān",
                        "Ramaḍān", "Shawwāl", "Dhū al-Qaʿdah", "Dhū al-Ḥijjah"
                    )
                    val mIndex = (h.get(ChronoField.MONTH_OF_YEAR) - 1).coerceIn(0, 11)
                    val d = h.get(ChronoField.DAY_OF_MONTH)
                    val y = h.get(ChronoField.YEAR)
                    "$d ${monthNames[mIndex]} $y"
                }.getOrDefault("5 Rabī‘ al-Awwal 1448")
            }

            // Greeting & Date: Hijri Date BEFORE Greeting
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                when (currentHour) {
                    in 5..11 -> {
                        IslamicDawnGlyph(
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier
                                .size(24.dp)
                                .padding(top = 2.dp),
                        )
                    }
                    in 12..16 -> {
                        IslamicShamsGlyph(
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier
                                .size(24.dp)
                                .padding(top = 2.dp),
                        )
                    }
                    else -> {
                        IslamicHilalGlyph(
                            tint = Color(0xFFFFF6DC),
                            modifier = Modifier
                                .size(24.dp)
                                .padding(top = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    // Date comes FIRST
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        CalendarOutlineGlyph(
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = hijriDate,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2EBE5),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    // Greeting comes SECOND
                    Text(
                        text = "$greeting, $readerName",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFFFFF),
                    )
                    Text(
                        text = "May Allah make your Qur'an a light for your heart.",
                        fontSize = 11.sp,
                        color = Color(0xFFA5BAAF),
                    )
                }
            }
        }
    }
}

/**
 * Option A: Islamic Geometric Craft - Morning (Subh Sadiq)
 * Horizon line, luminous rising solar dome, and 5 tapered radiating rays.
 */
@Composable
private fun IslamicDawnGlyph(modifier: Modifier = Modifier, tint: Color = Color(0xFFF59E0B)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.70f
        val strokeWidth = 1.6.dp.toPx()

        // 1. Horizon line
        drawLine(
            color = tint,
            start = Offset(w * 0.10f, cy),
            end = Offset(w * 0.90f, cy),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )

        // 2. Rising sun dome (semi-circle filled)
        val sunRadius = w * 0.28f
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - sunRadius, cy - sunRadius),
            size = Size(sunRadius * 2, sunRadius * 2),
        )

        // 3. Five geometric radiant rays
        val angles = listOf(-150.0, -120.0, -90.0, -60.0, -30.0)
        val rInner = sunRadius + 2.dp.toPx()
        val rOuter = w * 0.44f
        for (deg in angles) {
            val rad = Math.toRadians(deg)
            val x1 = cx + (rInner * kotlin.math.cos(rad)).toFloat()
            val y1 = cy + (rInner * kotlin.math.sin(rad)).toFloat()
            val x2 = cx + (rOuter * kotlin.math.cos(rad)).toFloat()
            val y2 = cy + (rOuter * kotlin.math.sin(rad)).toFloat()
            drawLine(
                color = tint,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Option A: Islamic Geometric Craft - Afternoon (8-point Islamic Shams)
 * Overlapping rotated geometric squares forming the sacred 8-point solar star.
 */
@Composable
private fun IslamicShamsGlyph(modifier: Modifier = Modifier, tint: Color = Color(0xFFFBBF24)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val rShams = w * 0.44f

        // Draw two overlapping squares rotated by 45 degrees
        for (rotDeg in listOf(0.0, 45.0)) {
            val rotRad = Math.toRadians(rotDeg)
            val path = Path()
            for (k in 0..3) {
                val angle = rotRad + k * (Math.PI / 2.0)
                val px = cx + (rShams * kotlin.math.cos(angle)).toFloat()
                val py = cy + (rShams * kotlin.math.sin(angle)).toFloat()
                if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path, color = tint)
        }

        // Inner luminous solar disk
        drawCircle(
            color = Color(0xFFFFE88A),
            radius = w * 0.18f,
            center = Offset(cx, cy),
        )
    }
}

/**
 * Option A: Islamic Geometric Craft - Evening (Ottoman Hilal & 8-point Najm)
 * Slender Ottoman crescent paired with a geometric 8-point star.
 */
@Composable
private fun IslamicHilalGlyph(modifier: Modifier = Modifier, tint: Color = Color(0xFFFFF6DC)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Crescent Moon (Hilal) using smooth cubic beziers
        val crescent = Path().apply {
            moveTo(w * 0.52f, h * 0.10f)
            cubicTo(
                w * 0.05f, h * 0.18f,
                w * 0.05f, h * 0.82f,
                w * 0.52f, h * 0.90f,
            )
            cubicTo(
                w * 0.24f, h * 0.72f,
                w * 0.24f, h * 0.28f,
                w * 0.52f, h * 0.10f,
            )
            close()
        }
        drawPath(crescent, color = tint)

        // 2. 8-Point Najm (Star) in the upper right
        val starCx = w * 0.72f
        val starCy = h * 0.38f
        val starR = w * 0.16f

        for (rotDeg in listOf(0.0, 45.0)) {
            val rotRad = Math.toRadians(rotDeg)
            val starPath = Path()
            for (k in 0..3) {
                val angle = rotRad + k * (Math.PI / 2.0)
                val px = starCx + (starR * kotlin.math.cos(angle)).toFloat()
                val py = starCy + (starR * kotlin.math.sin(angle)).toFloat()
                if (k == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
            }
            starPath.close()
            drawPath(starPath, color = tint)
        }
    }
}

/** Clean outline calendar glyph */
@Composable
private fun CalendarOutlineGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Calendar card body
        drawRoundRect(
            color = tint,
            topLeft = Offset(1.dp.toPx(), 2.5.dp.toPx()),
            size = Size(w - 2.dp.toPx(), h - 3.5.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = stroke,
        )
        // Top binder rings
        drawLine(
            color = tint,
            start = Offset(w * 0.3f, 0.5.dp.toPx()),
            end = Offset(w * 0.3f, 3.5.dp.toPx()),
            strokeWidth = 1.3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.7f, 0.5.dp.toPx()),
            end = Offset(w * 0.7f, 3.5.dp.toPx()),
            strokeWidth = 1.3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        // Binder line
        drawLine(
            color = tint,
            start = Offset(1.dp.toPx(), h * 0.44f),
            end = Offset(w - 1.dp.toPx(), h * 0.44f),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

/** Crisp vector ribbon bookmark glyph matching the home screen */
@Composable
fun BookmarkGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.12f)
            lineTo(w * 0.78f, h * 0.12f)
            lineTo(w * 0.78f, h * 0.88f)
            lineTo(w * 0.50f, h * 0.68f)
            lineTo(w * 0.22f, h * 0.88f)
            close()
        }
        if (filled) {
            drawPath(
                path = path,
                color = tint,
                style = Fill,
            )
        } else {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(
                    width = 1.8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}
