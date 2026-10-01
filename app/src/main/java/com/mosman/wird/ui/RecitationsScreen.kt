package com.mosman.wird.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Canvas
import com.mosman.wird.R
import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.progressOf
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * What you have actually done — the History tab in tactile claymorphism.
 *
 * Displays your habit streak next to total days read (Sacred Rule 4),
 * the record of completed portions with surah and page coverage,
 * and audio playback for recitations you recited out loud.
 *
 * Missed days are simply absent (Sacred Rule 3).
 */
@Composable
fun RecitationsScreen(
    logs: List<DayLog>,
    audioFor: (LocalDate) -> File?,
    coveredFor: (LocalDate) -> Pair<Int, Int>? = { null },
    transcriptionFor: (LocalDate) -> String? = { null },
    onPlay: (File) -> Unit,
    onStop: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val groundColor = if (isDark) Color(0xFF08100D) else Color(0xFFF7F4EB)

    // Handle system back gesture to return to Home
    BackHandler(onBack = onBack)

    // Newest first: the thing you did most recently is the thing you want to hear back.
    val ordered = remember(logs) { logs.sortedByDescending { it.date } }
    val progress = remember(logs) { progressOf(logs, LocalDate.now()) }
    var playingFile by remember { mutableStateOf<File?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(groundColor)
            .statusBarsPadding(),
    ) {
        // App top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clayCard(
                            shape = CircleShape,
                            backgroundColor = if (isDark) Color(0xFF16251E) else Color(0xFFFFFFFF),
                            highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                            shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                            elevation = 3.dp,
                        )
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = if (isDark) Color(0xFF93DB7A) else Color(0xFF1E3F32),
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Your Wird",
                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                        )
                        Spacer(Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .offset(y = (-7).dp)
                                .background(Color(0xFF50A773), CircleShape),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "What you recited & recorded",
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Summary Stats Card in Tactile Clay (Streak + Totals, Sacred Rule 4)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clayCard(
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = if (isDark) Color(0xFF13201A) else Color(0xFFFFFFFF),
                    highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                    shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                    elevation = 4.dp,
                )
                .padding(vertical = 14.dp, horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Streak
                val streakColor = if (isDark) Color(0xFFE67E22) else Color(0xFFD35400)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FlameGlyph(tint = streakColor, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${progress.currentStreak}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = streakColor,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Day Streak",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                    )
                }

                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF8C7D6B).copy(alpha = 0.2f)),
                )

                // Total Days
                val daysColor = if (isDark) Color(0xFF92E2B6) else Color(0xFF1E3F32)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        BookGlyph(tint = daysColor)
                        Text(
                            text = "${progress.totalDaysRead}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = daysColor,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Total Days",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                    )
                }

                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF8C7D6B).copy(alpha = 0.2f)),
                )

                val aloudRatio = if (progress.totalDaysRead > 0) (progress.recitedDays * 100) / progress.totalDaysRead else 0
                // Aloud Ratio
                val aloudColor = if (isDark) Color(0xFF50A773) else Color(0xFF245847)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MicGlyph(tint = aloudColor)
                        Text(
                            text = "$aloudRatio%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = aloudColor,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Aloud Ratio",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (ordered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clayCard(
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = if (isDark) Color(0xFF13201A) else Color(0xFFFFFFFF),
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                        shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                        elevation = 3.dp,
                    )
                    .padding(20.dp),
            ) {
                Text(
                    text = "Nothing here yet. Once you finish a day it will be listed, and anything you recited you can hear back.",
                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "COMPLETED DAYS",
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Text(
                text = "Days you missed aren't listed",
                color = if (isDark) Color(0xFF62756C) else Color(0xFF8C7D6B),
                fontSize = 10.5.sp,
            )
        }

        Spacer(Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(ordered, key = { it.date.toString() }) { log ->
                val audio = audioFor(log.date)
                val coverage = coverageLabel(coveredFor(log.date))
                val isPlaying = playingFile == audio

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clayCard(
                            shape = RoundedCornerShape(18.dp),
                            backgroundColor = if (isDark) Color(0xFF13201A) else Color(0xFFFFFFFF),
                            highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                            shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                            elevation = 3.dp,
                        )
                        .padding(16.dp),
                ) {
                    val isRecited = log.method == Method.RECITED
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dayLabel(log.date),
                                    color = if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (coverage != null) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = coverage,
                                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                                        fontSize = 12.sp,
                                    )
                                }
                            }

                            // Badge
                            Box(
                                modifier = Modifier
                                    .clayPill(
                                        shape = RoundedCornerShape(999.dp),
                                        backgroundColor = if (isRecited) {
                                            if (isDark) Color(0xFF1B382A) else Color(0xFFEDF5F0)
                                        } else {
                                            if (isDark) Color(0xFF18241E) else Color(0xFFF3EFE7)
                                        },
                                        highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.8f),
                                        shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF8C7D6B).copy(alpha = 0.15f),
                                        elevation = 1.dp,
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (isRecited) {
                                        MicGlyph(
                                            tint = if (isDark) Color(0xFF93DB7A) else Color(0xFF245847),
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                    Text(
                                        text = if (isRecited) "Recited aloud" else "✓ Read (tapped)",
                                        color = if (isRecited) {
                                            if (isDark) Color(0xFF93DB7A) else Color(0xFF245847)
                                        } else {
                                            if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        // "See what you recited" review card (audio + text review)
                        val hasAudio = audio != null && audio.exists() && audio.length() > 0
                        val transcription = transcriptionFor(log.date)
                        if (isRecited || hasAudio) {
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clayCard(
                                        shape = RoundedCornerShape(14.dp),
                                        backgroundColor = if (isDark) Color(0xFF16251E) else Color(0xFFFAF7F0),
                                        highlightColor = Color.White.copy(alpha = if (isDark) 0.12f else 0.85f),
                                        shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF8C7D6B).copy(alpha = 0.18f),
                                        elevation = 2.dp,
                                    )
                                    .padding(14.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            MicGlyph(
                                                tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                                modifier = Modifier.size(13.dp),
                                            )
                                            Text(
                                                text = "See what you recited",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                            )
                                        }
                                    }

                                    // Audio player bar
                                    if (hasAudio && audio != null) {
                                        Spacer(Modifier.height(10.dp))
                                        val durationSecs = (audio.length() / 8000L).toInt().coerceAtLeast(1)
                                        val durationStr = String.format(
                                            java.util.Locale.getDefault(),
                                            "%d:%02d",
                                            durationSecs / 60,
                                            durationSecs % 60,
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .clayCard(
                                                    shape = RoundedCornerShape(12.dp),
                                                    backgroundColor = if (isDark) Color(0xFF1A2B23) else Color(0xFFFFFFFF),
                                                    highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.85f),
                                                    shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF8C7D6B).copy(alpha = 0.2f),
                                                    elevation = 2.dp,
                                                )
                                                .clickable {
                                                    if (isPlaying) {
                                                        onStop()
                                                        playingFile = null
                                                    } else {
                                                        playingFile = audio
                                                        onPlay(audio)
                                                    }
                                                }
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.CenterStart,
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                // Play / Stop button circle
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(
                                                            color = if (isDark) Color(0xFF2D694E) else Color(0xFF1E3F32),
                                                            shape = CircleShape,
                                                        ),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        text = if (isPlaying) "■" else "▶",
                                                        color = Color.White,
                                                        fontSize = if (isPlaying) 10.sp else 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }

                                                Spacer(Modifier.width(10.dp))

                                                // Tactile Audio Waveform lines
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    val waveHeights = listOf(6, 12, 16, 10, 18, 14, 8, 15, 12, 6, 14, 9, 16, 7)
                                                    waveHeights.forEach { h ->
                                                        Box(
                                                            modifier = Modifier
                                                                .width(3.dp)
                                                                .height(h.dp)
                                                                .background(
                                                                    color = if (isPlaying) Color(0xFF50A773) else Color(0xFF50A773).copy(alpha = 0.6f),
                                                                    shape = RoundedCornerShape(2.dp),
                                                                ),
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.width(8.dp))

                                                Text(
                                                    text = durationStr,
                                                    color = if (isDark) Color(0xFF92E2B6) else Color(0xFF1E3F32),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                    }

                                    // Recited text section
                                    Spacer(Modifier.height(10.dp))
                                    if (!transcription.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isDark) Color(0xFF0F1A15) else Color(0xFFF0ECE1))
                                                .padding(12.dp),
                                        ) {
                                            Column {
                                                Text(
                                                    text = "RECITED TEXT (WHISPER AI)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.8.sp,
                                                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = transcription,
                                                    fontSize = 15.sp,
                                                    lineHeight = 24.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isDark) Color(0xFF0F1A15) else Color(0xFFF0ECE1))
                                                .padding(10.dp),
                                        ) {
                                            Text(
                                                text = if (hasAudio) "Recitation audio recorded. Tap play to listen." else "Recitation portion logged for this day.",
                                                fontSize = 12.sp,
                                                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlameGlyph(tint: Color, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_flame),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(18.dp),
    )
}

@Composable
private fun MicGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.34f, h * 0.06f),
            size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.50f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.16f),
        )
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.20f, h * 0.34f),
            size = androidx.compose.ui.geometry.Size(w * 0.60f, h * 0.42f),
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round),
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.76f),
            end = Offset(w * 0.5f, h * 0.94f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun BookGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round)
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

private fun coverageLabel(covered: Pair<Int, Int>?): String? {
    if (covered == null) return null
    val startUnit = covered.first
    val units = covered.second
    val startPage = Mushaf.pageOf(startUnit)
    val endPage = Mushaf.pageOf((startUnit + units - 1).coerceAtMost(Mushaf.TOTAL_UNITS - 1))
    val surah = SurahIndex.on(startPage).firstOrNull()?.name
    val pageText = if (startPage == endPage) "Page $startPage" else "Pages $startPage–$endPage"
    return if (surah != null) "$surah · $pageText" else pageText
}

/** "Today", "Yesterday", then the date. Nobody counts back further than two days. */
private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
    }
}

