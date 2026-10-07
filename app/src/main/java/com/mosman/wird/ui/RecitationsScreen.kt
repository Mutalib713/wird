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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.mosman.wird.domain.ReadingTrack
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
import com.mosman.wird.ui.theme.SetStatusBarAppearance
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
    allTracks: List<ReadingTrack> = emptyList(),
    audioFor: (LocalDate, String?) -> File? = { d, _ -> null },
    coveredFor: (LocalDate, String?) -> Pair<Int, Int>? = { _, _ -> null },
    transcriptionFor: (LocalDate, String?) -> String? = { _, _ -> null },
    hasCorruptLogs: Boolean = false,
    onPlay: (File) -> Unit,
    onStop: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val groundColor = if (isDark) Color(0xFF08100D) else Color(0xFFF7F4EB)

    SetStatusBarAppearance(isLightBackground = !isDark)

    // Handle system back gesture to return to Home
    BackHandler(onBack = onBack)

    var selectedTrackIdFilter by remember { mutableStateOf<String?>(null) }
    var expandedKeys by remember { mutableStateOf(setOf<String>()) }

    // Newest first: the thing you did most recently is the thing you want to hear back.
    val ordered = remember(logs, selectedTrackIdFilter) {
        val base = if (selectedTrackIdFilter == null) logs else logs.filter { it.trackId == selectedTrackIdFilter }
        base.sortedByDescending { it.date }
    }
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
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
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
                    Text(
                        text = "🔥 ${progress.currentStreak}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = streakColor,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Day Streak",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
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
                    Text(
                        text = "📖 ${progress.totalDaysRead}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = daysColor,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Total Days",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
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
                    Text(
                        text = "🎙️ $aloudRatio%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = aloudColor,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Recited aloud",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
                    )
                }
            }
        }

        if (hasCorruptLogs) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clayCard(
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = if (isDark) Color(0xFF1F1A14) else Color(0xFFFFF9E6),
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                        shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                        elevation = 3.dp,
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "A previous reading log could not be read and was preserved as a backup.",
                    color = if (isDark) Color(0xFFE5C07B) else Color(0xFF8C5A28),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Horizontal Track Filter Pills
        val filterOptions = remember(logs, allTracks) {
            val list = mutableListOf<Pair<String?, String>>()
            list.add(null to "All (${logs.size})")
            val trackMap = allTracks.associateBy { it.id }
            val presentTrackIds = (logs.mapNotNull { it.trackId } + allTracks.map { it.id }).distinct()
            for (tid in presentTrackIds) {
                val name = trackMap[tid]?.name
                    ?: logs.firstOrNull { it.trackId == tid }?.trackName
                    ?: "Track"
                val count = logs.count { it.trackId == tid }
                list.add(tid to "$name ($count)")
            }
            list
        }

        if (filterOptions.size > 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                filterOptions.forEach { (tid, label) ->
                    val isSelected = selectedTrackIdFilter == tid
                    Box(
                        modifier = Modifier
                            .clayPill(
                                shape = RoundedCornerShape(999.dp),
                                backgroundColor = if (isSelected) {
                                    if (isDark) Color(0xFF1E3F32) else Color(0xFFDCEDE3)
                                } else {
                                    if (isDark) Color(0xFF14221B) else Color(0xFFEFECE1)
                                },
                                elevation = if (isSelected) 2.dp else 1.dp,
                            )
                            .clickable { selectedTrackIdFilter = tid }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                if (isDark) Color(0xFF92E2B6) else Color(0xFF1E3F32)
                            } else {
                                if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551)
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

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
                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
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
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
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
            items(ordered, key = { "${it.date}_${it.trackId.orEmpty()}" }) { log ->
                val itemKey = "${log.date}_${log.trackId.orEmpty()}"
                val isExpanded = itemKey in expandedKeys
                val audio = audioFor(log.date, log.trackId)
                val coverage = coverageLabel(coveredFor(log.date, log.trackId))
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
                                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
                                        fontSize = 12.sp,
                                    )
                                }
                                val displayTrackName = log.trackName
                                    ?: allTracks.firstOrNull { it.id == log.trackId }?.name
                                if (!displayTrackName.isNullOrBlank()) {
                                    Spacer(Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isDark) Color(0xFF162B21) else Color(0xFFE5EFE9))
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "📖 $displayTrackName",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFF8ED676) else Color(0xFF1F6B47),
                                        )
                                    }
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
                                Text(
                                    text = if (isRecited) "🎙️ Recited aloud" else "✓ Read (tapped)",
                                    color = if (isRecited) {
                                        if (isDark) Color(0xFF93DB7A) else Color(0xFF245847)
                                    } else {
                                        if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        // Collapsible Recitation Section
                        val hasAudio = audio != null && audio.exists() && audio.length() > 0
                        val transcription = transcriptionFor(log.date, log.trackId)
                        if (hasAudio) {
                            Spacer(Modifier.height(10.dp))
                            val durationSecs = (audio.length() / 8000L).toInt().coerceAtLeast(1)
                            val durationStr = String.format(
                                java.util.Locale.getDefault(),
                                "%d:%02d",
                                durationSecs / 60,
                                durationSecs % 60,
                            )

                            // Accordion Toggle Pill Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clayPill(
                                        shape = RoundedCornerShape(12.dp),
                                        backgroundColor = if (isExpanded) {
                                            if (isDark) Color(0xFF1D382B) else Color(0xFFDCEDE3)
                                        } else {
                                            if (isDark) Color(0xFF16251E) else Color(0xFFECE7DB)
                                        },
                                        elevation = 1.dp,
                                    )
                                    .clickable {
                                        expandedKeys = if (isExpanded) expandedKeys - itemKey else expandedKeys + itemKey
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (isExpanded) "🎙️ Recitation recording & text" else "🎙️ See what you recited ($durationStr)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF92E2B6) else Color(0xFF1E3F32),
                                )
                                Text(
                                    text = if (isExpanded) "▴" else "▾",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF92E2B6) else Color(0xFF1E3F32),
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .clayCard(
                                                shape = RoundedCornerShape(10.dp),
                                                backgroundColor = if (isDark) Color(0xFF182820) else Color(0xFFF7F5EE),
                                                highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.85f),
                                                shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF8C7D6B).copy(alpha = 0.15f),
                                                elevation = 1.dp,
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
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .background(
                                                        color = if (isDark) Color(0xFF2D694E) else Color(0xFF1E3F32),
                                                        shape = CircleShape,
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = if (isPlaying) "■" else "▶",
                                                    color = Color.White,
                                                    fontSize = if (isPlaying) 9.sp else 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }

                                            Spacer(Modifier.width(10.dp))

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
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }

                                    if (!transcription.isNullOrBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDark) Color(0xFF101C16) else Color(0xFFF3EFE7))
                                                .padding(10.dp),
                                        ) {
                                            Text(
                                                text = transcription,
                                                fontSize = 13.5.sp,
                                                lineHeight = 20.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!transcription.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = transcription,
                                fontSize = 13.5.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                            )
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

