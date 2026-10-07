package com.mosman.wird.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mosman.wird.domain.LifeSpace
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.TrackScheduleMode
import com.mosman.wird.domain.TrackType
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill

/**
 * Modal dialog for switching active Reading Tracks and managing them.
 */
@Composable
fun TrackPickerDialog(
    currentTrack: ReadingTrack?,
    allTracks: List<ReadingTrack>,
    onSelectTrack: (ReadingTrack) -> Unit,
    onAddNewTrack: () -> Unit = {},
    onManageTracks: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val cardBg = if (isDark) Color(0xFF16251E) else Color(0xFFFBF8F1)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clayCard(
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = cardBg,
                    highlightColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.95f),
                    shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8C7D6B).copy(alpha = 0.25f),
                    elevation = 8.dp,
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TrackTypeGlyph(
                        type = currentTrack?.type ?: TrackType.TILAWAH,
                        tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                        modifier = Modifier.size(20.dp),
                    )
                    Column {
                        Text(
                            text = "Reading Tracks",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                        )
                        Text(
                            text = "Switch active track or create new",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Track list
            if (allTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clayCard(
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = if (isDark) Color(0xFF122019) else Color(0xFFECE7D9),
                            elevation = 1.dp,
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No reading tracks yet. Add your first track below.",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    allTracks.forEach { track ->
                        val isTrackActive = currentTrack?.id == track.id
                        val isCompleted = track.isCompletedToday()
                        val isDue = track.isDueToday()
                        val trackBg = if (isTrackActive) {
                            if (isDark) Color(0xFF1D382B) else Color(0xFFDCEDE3)
                        } else {
                            if (isDark) Color(0xFF101C16) else Color(0xFFEFECE1)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clayPill(
                                    shape = RoundedCornerShape(14.dp),
                                    backgroundColor = trackBg,
                                    elevation = if (isTrackActive) 2.dp else 1.dp,
                                )
                                .clickable {
                                    onSelectTrack(track)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                TrackTypeGlyph(
                                    type = track.type,
                                    tint = if (isTrackActive) {
                                        if (isDark) Color(0xFF8ED676) else Color(0xFF245847)
                                    } else {
                                        if (isDark) Color(0xFF7E978B) else Color(0xFF678174)
                                    },
                                    modifier = Modifier.size(16.dp),
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = track.name,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isTrackActive) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                        )
                                    }
                                    val targetDesc = if (track.customTargetVerses != null) {
                                        "${track.customTargetVerses} v/day"
                                    } else if (track.dailyUnits == 1) {
                                        "½ p/day"
                                    } else {
                                        "${track.dailyUnits / 2} p/day"
                                    }
                                    val intentionDesc = if (!track.intention.isNullOrBlank()) " · ${track.intention}" else ""
                                    Text(
                                        text = "${track.type.englishLabel} (${track.type.arabicLabel}) · ${track.surahName()} · $targetDesc$intentionDesc",
                                        fontSize = 10.5.sp,
                                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (track.currentStreak > 0) {
                                    Text(
                                        text = "🔥 ${track.currentStreak}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF9800),
                                    )
                                }

                                if (isCompleted) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (isDark) Color(0xFF1B4D36) else Color(0xFFC7EBD5))
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                    ) {
                                        Text(
                                            text = "✓ Done",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF8ED676) else Color(0xFF1E5638),
                                        )
                                    }
                                } else if (isDue) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (isDark) Color(0xFF453612) else Color(0xFFF7E6B8))
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                    ) {
                                        Text(
                                            text = "Due Today",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFF9C86A) else Color(0xFF8F6300),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Actions: + Add Track and Manage in Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clayPill(
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = if (isDark) Color(0xFF1A382A) else Color(0xFFD4EBDC),
                            elevation = 1.dp,
                        )
                        .clickable {
                            onDismiss()
                            onAddNewTrack()
                        }
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+ Add Track",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF8ED676) else Color(0xFF1D5A3C),
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clayPill(
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = if (isDark) Color(0xFF13221B) else Color(0xFFE8E4D6),
                            elevation = 1.dp,
                        )
                        .clickable {
                            onDismiss()
                            onManageTracks()
                        }
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Manage Tracks",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}
