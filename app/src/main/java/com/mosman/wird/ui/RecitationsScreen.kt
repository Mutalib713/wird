package com.mosman.wird.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.SetStatusBarAppearance
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Recordings screen in the New Look design system.
 * Shows saved recitations with audio playback, track attribution, and transcription.
 */
@Composable
fun RecitationsScreen(
    logs: List<DayLog>,
    allTracks: List<ReadingTrack> = emptyList(),
    activeTrack: ReadingTrack? = null,
    today: LocalDate = LocalDate.now(),
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

    SetStatusBarAppearance(isLightBackground = !isDark)
    BackHandler(onBack = onBack)

    var selectedTrackIdFilter by remember(activeTrack) { mutableStateOf(activeTrack?.id) }
    var expandedKeys by remember { mutableStateOf(setOf<String>()) }
    var playingFile by remember { mutableStateOf<File?>(null) }

    // Only logs with an actual audio recording saved on this phone belong in Recordings.
    // Days that were simply read/tapped are not recordings and belong in the reading log.
    val recordedLogs = remember(logs) {
        logs.filter { log ->
            val file = audioFor(log.date, log.trackId)
            file != null && file.exists() && file.length() > 0
        }
    }

    // Only filter among active tracks that actually have recordings
    val trackMap = remember(allTracks) { allTracks.associateBy { it.id } }
    val validTrackIds = remember(recordedLogs, trackMap) {
        recordedLogs.mapNotNull { it.trackId }.filter { it in trackMap }.distinct()
    }

    val ordered = remember(recordedLogs, selectedTrackIdFilter, validTrackIds) {
        val base = if (selectedTrackIdFilter == null || selectedTrackIdFilter !in validTrackIds) {
            recordedLogs
        } else {
            recordedLogs.filter { it.trackId == selectedTrackIdFilter }
        }
        base.sortedByDescending { it.date }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.field)
            .statusBarsPadding(),
    ) {
        // App top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.card)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = WirdIcons.Back,
                    contentDescription = "Back",
                    tint = colors.ink,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    text = "Recordings",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                val count = ordered.size
                Text(
                    text = if (count > 0) "$count saved recitation${if (count == 1) "" else "s"}" else "What you recited & recorded",
                    fontSize = 13.sp,
                    color = colors.ink2,
                )
            }
        }

        if (hasCorruptLogs) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.tile),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "A previous reading log could not be read and was preserved as a backup.",
                    color = colors.goldText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }

        // Horizontal Track Filter Chips (only if multiple active tracks have recordings)
        val filterOptions = remember(recordedLogs, validTrackIds, trackMap) {
            val list = mutableListOf<Pair<String?, String>>()
            if (validTrackIds.size > 1) {
                list.add(null to "All (${recordedLogs.size})")
                for (tid in validTrackIds) {
                    val name = trackMap[tid]?.name ?: "Track"
                    val c = recordedLogs.count { it.trackId == tid }
                    list.add(tid to "$name ($c)")
                }
            }
            list
        }

        if (filterOptions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                filterOptions.forEach { (tid, label) ->
                    val isSelected = selectedTrackIdFilter == tid
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) colors.action else colors.chip)
                            .clickable { selectedTrackIdFilter = tid }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) colors.onAction else colors.ink2,
                        )
                    }
                }
            }
        }

        if (ordered.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = BorderStroke(1.dp, colors.rule),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(colors.disc, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = WirdIcons.Mic,
                            contentDescription = null,
                            tint = colors.action,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "No recordings yet",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Recite your daily portion with Recite & Verify to save and hear back your recitation on this phone.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = colors.ink2,
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(ordered, key = { "${it.date}_${it.trackId.orEmpty()}" }) { log ->
                val itemKey = "${log.date}_${log.trackId.orEmpty()}"
                val isExpanded = itemKey in expandedKeys
                val audio = audioFor(log.date, log.trackId)
                val coverage = coverageLabel(coveredFor(log.date, log.trackId))
                val isPlaying = playingFile == audio
                val hasAudio = audio != null && audio.exists() && audio.length() > 0
                val transcription = transcriptionFor(log.date, log.trackId)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    border = BorderStroke(1.dp, colors.rule),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dayLabel(log.date),
                                    color = colors.ink,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (coverage != null) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = coverage,
                                        color = colors.ink2,
                                        fontSize = 13.sp,
                                    )
                                }
                                val displayTrackName = log.trackName
                                    ?: allTracks.firstOrNull { it.id == log.trackId }?.name
                                if (!displayTrackName.isNullOrBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.tile)
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = WirdIcons.Quran,
                                                contentDescription = null,
                                                tint = colors.goldText,
                                                modifier = Modifier.size(12.dp),
                                            )
                                            Text(
                                                text = displayTrackName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.goldText,
                                            )
                                        }
                                    }
                                }
                            }

                            // Status Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.tile)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(
                                        imageVector = WirdIcons.Mic,
                                        contentDescription = null,
                                        tint = colors.goldText,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = "Recited aloud",
                                        color = colors.goldText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        // Audio Player & Recitation Section
                        if (hasAudio) {
                            Spacer(Modifier.height(12.dp))
                            val durationSecs = (audio.length() / 8000L).toInt().coerceAtLeast(1)
                            val durationStr = String.format(
                                Locale.getDefault(),
                                "%d:%02d",
                                durationSecs / 60,
                                durationSecs % 60,
                            )

                            // Audio player bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.field)
                                    .clickable {
                                        if (isPlaying) {
                                            onStop()
                                            playingFile = null
                                        } else {
                                            playingFile = audio
                                            onPlay(audio)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(colors.action, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) WirdIcons.Pause else WirdIcons.Play,
                                            contentDescription = if (isPlaying) "Stop" else "Play",
                                            tint = colors.onAction,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    // Simulated waveform bars
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
                                                        color = if (isPlaying) colors.action else colors.action.copy(alpha = 0.45f),
                                                        shape = RoundedCornerShape(2.dp),
                                                    ),
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Text(
                                        text = durationStr,
                                        color = colors.ink,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }

                            // Details / Transcription toggle if transcription exists
                            if (!transcription.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            expandedKeys = if (isExpanded) expandedKeys - itemKey else expandedKeys + itemKey
                                        }
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = if (isExpanded) "Hide recitation text" else "Show recitation text",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.action,
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) WirdIcons.ChevronDown else WirdIcons.ChevronRight,
                                        contentDescription = null,
                                        tint = colors.action,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically(),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.tile)
                                            .padding(12.dp),
                                    ) {
                                        Text(
                                            text = transcription,
                                            fontSize = 13.5.sp,
                                            lineHeight = 20.sp,
                                            color = colors.ink,
                                        )
                                    }
                                }
                            }
                        } else if (!transcription.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = transcription,
                                fontSize = 13.5.sp,
                                lineHeight = 20.sp,
                                color = colors.ink,
                            )
                        }
                    }
                }
            }
        }
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

private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
    }
}
