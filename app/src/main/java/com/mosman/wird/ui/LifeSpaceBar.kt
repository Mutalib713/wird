package com.mosman.wird.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill

/**
 * Top bar above Today's Wird card displaying active Life Space and its parallel reading tracks.
 */
@Composable
fun LifeSpaceBar(
    activeSpace: LifeSpace,
    activeTrack: ReadingTrack,
    allSpaces: List<LifeSpace>,
    scheduleMode: TrackScheduleMode,
    onSelectTrack: (ReadingTrack) -> Unit,
    onSelectSpace: (LifeSpace) -> Unit,
    onToggleScheduleMode: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    var showSpaceDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Upper line: Space selector pill & Schedule mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Life Space selector chip
            Box(
                modifier = Modifier
                    .clayPill(
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = if (isDark) Color(0xFF13231D) else Color(0xFFEBE7DC),
                        elevation = 2.dp,
                    )
                    .clickable { showSpaceDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    LifeSpaceGlyph(
                        tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = activeSpace.name,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                    )
                    if (activeSpace.isFrozen) {
                        FreezeGlyph(
                            tint = if (isDark) Color(0xFF90CAF9) else Color(0xFF1976D2),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "Paused",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF90CAF9) else Color(0xFF1976D2),
                        )
                    }
                    Text(
                        text = "▾",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF7E978B) else Color(0xFF678174),
                    )
                }
            }

            // Mode toggle chip: Automatic vs Manual
            Box(
                modifier = Modifier
                    .clayPill(
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = if (isDark) Color(0xFF13231D) else Color(0xFFEBE7DC),
                        elevation = 1.dp,
                    )
                    .clickable(onClick = onToggleScheduleMode)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    AutoScheduleGlyph(
                        tint = if (scheduleMode == TrackScheduleMode.AUTOMATIC) {
                            if (isDark) Color(0xFFF9C86A) else Color(0xFF8F6300)
                        } else {
                            if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378)
                        },
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = if (scheduleMode == TrackScheduleMode.AUTOMATIC) "Auto" else "Manual",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (scheduleMode == TrackScheduleMode.AUTOMATIC) {
                            if (isDark) Color(0xFFF9C86A) else Color(0xFF8F6300)
                        } else {
                            if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378)
                        },
                    )
                }
            }
        }

        // Lower line: Parallel tracks tabs (e.g. Mon–Thu Ya-Sin vs Weekend Al-Anbiya)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            activeSpace.tracks.forEach { track ->
                val isSelected = track.id == activeTrack.id
                val tabBg by animateColorAsState(
                    targetValue = if (isSelected) {
                        if (isDark) Color(0xFF1E3D31) else Color(0xFFDCEFE5)
                    } else {
                        if (isDark) Color(0xFF111E18) else Color(0xFFEFECE1)
                    },
                    label = "trackTabBg",
                )
                val tabBorder = if (isSelected) {
                    if (isDark) Color(0xFF8ED676).copy(alpha = 0.6f) else Color(0xFF245847).copy(alpha = 0.5f)
                } else {
                    Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .clayPill(
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = tabBg,
                            elevation = if (isSelected) 3.dp else 1.dp,
                        )
                        .border(1.dp, tabBorder, RoundedCornerShape(14.dp))
                        .clickable { onSelectTrack(track) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        // Vector discipline icon
                        TrackTypeGlyph(
                            type = track.type,
                            tint = if (isSelected) {
                                if (isDark) Color(0xFF8ED676) else Color(0xFF245847)
                            } else {
                                if (isDark) Color(0xFF7E978B) else Color(0xFF678174)
                            },
                            modifier = Modifier.size(13.dp),
                        )

                        // Track Name & Schedule
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Text(
                                    text = track.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D)
                                    } else {
                                        if (isDark) Color(0xFFB0C4BA) else Color(0xFF5A7266)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "· ${track.type.label}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        if (isDark) Color(0xFFF9C86A) else Color(0xFF8F6300)
                                    } else {
                                        if (isDark) Color(0xFF7E978B) else Color(0xFF7A9386)
                                    },
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "${track.scheduleLabel()} · Page ${track.pageNumber}",
                                    fontSize = 10.sp,
                                    color = if (isDark) Color(0xFF7E978B) else Color(0xFF678174),
                                )
                                if (track.currentStreak > 0) {
                                    Text(
                                        text = "${track.currentStreak}d streak",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSpaceDialog) {
        LifeSpacePickerDialog(
            currentSpace = activeSpace,
            allSpaces = allSpaces,
            onSelectSpace = {
                onSelectSpace(it)
                showSpaceDialog = false
            },
            onOpenSettings = {
                showSpaceDialog = false
                onOpenSettings()
            },
            onDismiss = { showSpaceDialog = false },
        )
    }
}

/**
 * Modal dialog for switching active Reading Modes and parallel tracks or accessing settings.
 */
@Composable
fun LifeSpacePickerDialog(
    currentSpace: LifeSpace,
    activeTrack: ReadingTrack? = null,
    allSpaces: List<LifeSpace>,
    onSelectSpace: (LifeSpace) -> Unit,
    onSelectTrack: (ReadingTrack) -> Unit = {},
    onOpenSettings: () -> Unit,
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
                    LifeSpaceGlyph(
                        tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Reading Modes & Tracks",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                    )
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
                        tint = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Text(
                text = "Switch between reading modes (Home, School, Ramadan) and your active tracks.",
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                lineHeight = 16.5.sp,
            )

            // Section 1: Reading Modes
            Text(
                text = "READING MODES",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
            )

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                allSpaces.forEach { space ->
                    val isSelected = space.id == currentSpace.id
                    val itemBg = if (isSelected) {
                        if (isDark) Color(0xFF1D382B) else Color(0xFFDCEDE3)
                    } else {
                        if (isDark) Color(0xFF101C16) else Color(0xFFEFECE1)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clayPill(
                                shape = RoundedCornerShape(14.dp),
                                backgroundColor = itemBg,
                                elevation = if (isSelected) 3.dp else 1.dp,
                            )
                            .clickable { onSelectSpace(space) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(
                                text = space.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                            )
                            if (space.isFrozen) {
                                FreezeGlyph(
                                    tint = if (isDark) Color(0xFF90CAF9) else Color(0xFF1976D2),
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = "Paused",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF90CAF9) else Color(0xFF1976D2),
                                )
                            }
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF8ED676) else Color(0xFF245847)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Active",
                                    tint = if (isDark) Color(0xFF0D2720) else Color.White,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Tracks in current space
            if (currentSpace.tracks.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "TRACKS IN ${currentSpace.name.uppercase()}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    currentSpace.tracks.forEach { track ->
                        val isTrackActive = activeTrack?.id == track.id
                        val trackBg = if (isTrackActive) {
                            if (isDark) Color(0xFF1D382B) else Color(0xFFDCEDE3)
                        } else {
                            if (isDark) Color(0xFF101C16) else Color(0xFFEFECE1)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clayPill(
                                    shape = RoundedCornerShape(12.dp),
                                    backgroundColor = trackBg,
                                    elevation = if (isTrackActive) 2.dp else 1.dp,
                                )
                                .clickable {
                                    onSelectTrack(track)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TrackTypeGlyph(
                                    type = track.type,
                                    tint = if (isTrackActive) {
                                        if (isDark) Color(0xFF8ED676) else Color(0xFF245847)
                                    } else {
                                        if (isDark) Color(0xFF7E978B) else Color(0xFF678174)
                                    },
                                    modifier = Modifier.size(13.dp),
                                )
                                Column {
                                    Text(
                                        text = track.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isTrackActive) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                    )
                                    Text(
                                        text = "${track.type.label} · ${track.scheduleLabel()}",
                                        fontSize = 10.5.sp,
                                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                                    )
                                }
                            }

                            if (isTrackActive) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF8ED676) else Color(0xFF245847)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Active Track",
                                        tint = if (isDark) Color(0xFF0D2720) else Color.White,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Manage in Settings Link
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayPill(
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = if (isDark) Color(0xFF13221B) else Color(0xFFE8E4D6),
                        elevation = 1.dp,
                    )
                    .clickable(onClick = onOpenSettings)
                    .padding(vertical = 11.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Manage Modes & Tracks in Settings",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}
