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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.remember
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
    onMenu: (() -> Unit)? = null,
    menu: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
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
            .padding(top = 8.dp, bottom = 20.dp, start = 18.dp, end = 18.dp)
    ) {
        // Atmospheric artwork canvas: Crescent, birds, and mosque silhouette
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Glowing crescent moon (positioned in upper sky clear of top-right buttons)
            val moonCenter = Offset(w * 0.58f, h * 0.22f)
            val moonRadius = 14.dp.toPx()
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
                moveTo(w * 0.68f, h * 0.28f)
                quadraticTo(w * 0.70f, h * 0.25f, w * 0.72f, h * 0.28f)
                quadraticTo(w * 0.74f, h * 0.25f, w * 0.76f, h * 0.28f)
            }
            drawPath(bird1, color = birdColor, style = birdStroke)

            // Bird 2
            val bird2 = Path().apply {
                moveTo(w * 0.76f, h * 0.24f)
                quadraticTo(w * 0.775f, h * 0.21f, w * 0.79f, h * 0.24f)
                quadraticTo(w * 0.805f, h * 0.21f, w * 0.82f, h * 0.24f)
            }
            drawPath(bird2, color = birdColor, style = birdStroke)

            // Mosque silhouette (Minaret, Domes, Base)
            val mosqueColor = Color(0xFF132D24).copy(alpha = 0.95f)
            val minaretLeft = w * 0.86f
            
            // Minaret tower
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
                val domeCenterX = w * 0.72f
                val domeRadius = 38.dp.toPx()
                moveTo(domeCenterX - domeRadius, h)
                cubicTo(
                    domeCenterX - domeRadius, h - domeRadius * 1.3f,
                    domeCenterX + domeRadius, h - domeRadius * 1.3f,
                    domeCenterX + domeRadius, h
                )
                close()
            }
            drawPath(domePath, color = mosqueColor)

            // Dome finial
            drawLine(
                color = mosqueColor,
                start = Offset(w * 0.72f, h - 54.dp.toPx()),
                end = Offset(w * 0.72f, h - 42.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = Color(0xFFE8DCB8),
                radius = 2.dp.toPx(),
                center = Offset(w * 0.72f, h - 55.dp.toPx()),
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
                    // Bookmark icon button in tactile squircle pill
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clayPill(
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = Color(0xFF16382D).copy(alpha = 0.85f),
                                highlightColor = Color.White.copy(alpha = 0.35f),
                                shadowColor = Color.Black.copy(alpha = 0.4f),
                            )
                            .clickable(onClick = onOpenBookmarks),
                        contentAlignment = Alignment.Center,
                    ) {
                        BookmarkGlyph(
                            tint = Color(0xFFE4E9E5),
                            modifier = Modifier.size(19.dp),
                        )
                    }

                    // Optional overflow menu / settings button
                    onMenu?.let { openMenu ->
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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

            Spacer(Modifier.height(14.dp))

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

            // Greeting row (Sun/Moon icon + "$greeting, $readerName" + Calendar icon + Date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDaytime) {
                        SunriseGlyph(
                            tint = Color(0xFFF9C86A),
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        EveningMoonGlyph(
                            tint = Color(0xFFFFF6DC),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text(
                            text = "$greeting, $readerName",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFFFFF),
                        )
                        Text(
                            text = "May Allah make your Qur'an a light for your heart.",
                            fontSize = 10.5.sp,
                            color = Color(0xFFA5BAAF),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CalendarOutlineGlyph(
                        tint = Color(0xFFCBDCD3),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = hijriDate,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBDCD3),
                    )
                }
            }
        }
    }
}

/** Vector sunrise on horizon glyph matching approved reference design */
@Composable
private fun SunriseGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        // Horizon line
        drawLine(
            color = tint,
            start = Offset(w * 0.10f, h * 0.75f),
            end = Offset(w * 0.90f, h * 0.75f),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
        // Rising sun dome
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.30f, h * 0.45f),
            size = Size(w * 0.40f, h * 0.60f),
            style = stroke,
        )
        // Top ray
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.22f),
            end = Offset(w * 0.50f, h * 0.36f),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
        // Left diagonal ray
        drawLine(
            color = tint,
            start = Offset(w * 0.24f, h * 0.34f),
            end = Offset(w * 0.34f, h * 0.44f),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
        // Right diagonal ray
        drawLine(
            color = tint,
            start = Offset(w * 0.76f, h * 0.34f),
            end = Offset(w * 0.66f, h * 0.44f),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

/** Vector crescent moon glyph for evening hours */
@Composable
private fun EveningMoonGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.65f, h * 0.15f)
            cubicTo(w * 0.25f, h * 0.20f, w * 0.25f, h * 0.80f, w * 0.65f, h * 0.85f)
            cubicTo(w * 0.42f, h * 0.70f, w * 0.42f, h * 0.30f, w * 0.65f, h * 0.15f)
            close()
        }
        drawPath(path, color = tint)
        drawCircle(color = tint, radius = 1.5.dp.toPx(), center = Offset(w * 0.78f, h * 0.35f))
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

/** Crisp vector ribbon bookmark glyph */
@Composable
fun BookmarkGlyph(tint: Color, modifier: Modifier = Modifier) {
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
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
