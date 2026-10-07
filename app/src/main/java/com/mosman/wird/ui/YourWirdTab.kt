package com.mosman.wird.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
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
import com.mosman.wird.domain.Progress
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.unitsLabel
import com.mosman.wird.ui.theme.LocalWirdColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class WirdSegment { TRACKS, PROGRESS }

/**
 * Your Wird tab in the new look (PROFILE § 5bj, PLAN task 28).
 * Segmented control:
 * - Tracks: Manage configured reading tracks, add new tracks, and track overall mushaf progress.
 * - Progress: Day log history, recited vs tapped ratio, and recordings.
 */
@Composable
fun YourWirdTab(
    activeTrack: ReadingTrack,
    allTracks: List<ReadingTrack>,
    progress: Progress?,
    logs: List<DayLog>,
    onSelectTrack: (ReadingTrack) -> Unit,
    onCreateTrack: () -> Unit,
    onOpenRecordings: () -> Unit,
    modifier: Modifier = Modifier,
    initialSegment: WirdSegment = WirdSegment.TRACKS,
    today: LocalDate = LocalDate.now(),
) {
    val colors = LocalWirdColors.current
    var segment by remember(initialSegment) { mutableStateOf(initialSegment) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.field)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Your Wird",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
            ),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Your tracks, and how they're going.",
            fontSize = 15.sp,
            color = colors.ink2,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(14.dp))

        // Segmented Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(colors.chip)
                .padding(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(21.dp))
                    .background(if (segment == WirdSegment.TRACKS) colors.action else Color.Transparent)
                    .clickable { segment = WirdSegment.TRACKS },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Tracks",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (segment == WirdSegment.TRACKS) colors.onAction else colors.ink2,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(21.dp))
                    .background(if (segment == WirdSegment.PROGRESS) colors.action else Color.Transparent)
                    .clickable { segment = WirdSegment.PROGRESS },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Progress",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (segment == WirdSegment.PROGRESS) colors.onAction else colors.ink2,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        when (segment) {
            WirdSegment.TRACKS -> {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Reading tracks",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.ink,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        allTracks.forEach { track ->
                            val isSelected = track.id == activeTrack.id
                            val trackLogs = logs.filter { it.trackId == track.id }
                            val daysCount = trackLogs.map { it.date }.distinct().size

                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.card),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectTrack(track) },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(colors.disc, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = WirdIcons.Quran,
                                            contentDescription = track.name,
                                            tint = colors.action,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.ink,
                                        )
                                        Text(
                                            text = "${unitsLabel(track.dailyUnits)} · Every day",
                                            fontSize = 13.sp,
                                            color = colors.ink2,
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = daysCount.toString(),
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.ink,
                                        )
                                        Text(
                                            text = "days read",
                                            fontSize = 12.sp,
                                            color = colors.ink2,
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        imageVector = WirdIcons.ChevronRight,
                                        contentDescription = "Open",
                                        tint = colors.ink2,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Create new track button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.chip)
                            .clickable { onCreateTrack() },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = WirdIcons.Plus,
                            contentDescription = "Add",
                            tint = colors.ink,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Create new track",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Where you are card
                    val currentPage = Mushaf.pageOf(activeTrack.positionUnit)
                    val totalPages = Mushaf.PAGES
                    val pct = (currentPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)
                    val pctFormatted = String.format(Locale.getDefault(), "%.1f%%", pct * 100f)

                    val activeTrackLogs = logs.filter { it.trackId == activeTrack.id }
                    val totalDays = activeTrackLogs.map { it.date }.distinct().size
                    val recitedDays = activeTrackLogs.filter { it.method == Method.RECITED }.map { it.date }.distinct().size
                    val rate = if (totalDays > 0) ((recitedDays.toFloat() / totalDays.toFloat()) * 100).toInt() else 0

                    Text(
                        text = "Track overview",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.ink,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    )

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.rule),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = activeTrack.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.ink,
                                    )
                                    Text(
                                        text = "Page $currentPage of $totalPages",
                                        fontSize = 13.sp,
                                        color = colors.ink2,
                                    )
                                }
                                Text(
                                    text = pctFormatted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ink,
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Track progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(colors.field),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(pct)
                                        .height(6.dp)
                                        .background(colors.action, RoundedCornerShape(3.dp)),
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // 3 stats columns
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = totalDays.toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.ink,
                                    )
                                    Text(text = "Days read", fontSize = 12.sp, color = colors.ink2)
                                }
                                Box(Modifier.width(1.dp).height(32.dp).background(colors.rule))
                                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(
                                        text = recitedDays.toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.ink,
                                    )
                                    Text(text = "Recited aloud", fontSize = 12.sp, color = colors.ink2)
                                }
                                Box(Modifier.width(1.dp).height(32.dp).background(colors.rule))
                                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(
                                        text = "$rate%",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.ink,
                                    )
                                    Text(text = "Recite rate", fontSize = 12.sp, color = colors.ink2)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }

            WirdSegment.PROGRESS -> {
                val activeTrackLogs = logs.filter { it.trackId == activeTrack.id }.sortedByDescending { it.date }
                val dtf = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = "Days read · ${activeTrack.name}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                    }

                    if (activeTrackLogs.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.card),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) {
                                Text(
                                    text = "Days you read will show here.",
                                    fontSize = 14.sp,
                                    color = colors.ink2,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    } else {
                        items(activeTrackLogs) { row ->
                            val isToday = row.date == today
                            val label = if (isToday) "Today, ${row.date.format(dtf)}" else row.date.format(dtf)
                            val isRecited = row.method == Method.RECITED

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.card),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(if (isRecited) colors.tile else colors.field, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (isRecited) WirdIcons.Mic else WirdIcons.Check,
                                            contentDescription = if (isRecited) "Recited" else "Read",
                                            tint = if (isRecited) colors.goldText else colors.action,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.ink,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isRecited) colors.tile else colors.field)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = if (isRecited) "Recited aloud" else "Marked as read",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isRecited) colors.goldText else colors.ink2,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Recordings",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                        val recitedCount = activeTrackLogs.count { it.method == Method.RECITED }
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.card),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenRecordings() },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(colors.disc, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = WirdIcons.Mic,
                                        contentDescription = "Recordings",
                                        tint = colors.action,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$recitedCount recording${if (recitedCount == 1) "" else "s"}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.ink,
                                    )
                                    Text(
                                        text = "Saved and checked on this phone",
                                        fontSize = 13.sp,
                                        color = colors.ink2,
                                    )
                                }
                                Icon(
                                    imageVector = WirdIcons.ChevronRight,
                                    contentDescription = "Open",
                                    tint = colors.ink2,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}
