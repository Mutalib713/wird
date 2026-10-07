package com.mosman.wird.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.TrackScheduleMode
import com.mosman.wird.domain.unitsLabel
import com.mosman.wird.ui.theme.LocalWirdColors

/**
 * Switch Track sheet in Wird's new look (PROFILE § 5bj, PLAN task 28).
 * Displays configured tracks with checkmark selection,
 * preserved automatic track mode toggle, and "+ Create new track" action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchTrackSheet(
    activeTrack: ReadingTrack?,
    allTracks: List<ReadingTrack>,
    scheduleMode: TrackScheduleMode,
    onSelectTrack: (ReadingTrack) -> Unit,
    onToggleScheduleMode: () -> Unit,
    onCreateTrack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.ink2.copy(alpha = 0.4f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Switch track",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    ),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = WirdIcons.Close,
                        contentDescription = "Close",
                        tint = colors.ink2,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                allTracks.forEach { track ->
                    val isSelected = track.id == activeTrack?.id
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) colors.action else colors.rule,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectTrack(track)
                                onDismiss()
                            },
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
                                    text = "Read · Every day\n${unitsLabel(track.dailyUnits)} / day",
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp,
                                    color = colors.ink2,
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(colors.action, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = WirdIcons.Check,
                                        contentDescription = "Selected",
                                        tint = colors.onAction,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Preserved "Pick the track by day" switch row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pick the track by day",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.ink,
                    )
                    Text(
                        text = "Home shows whichever track is due today",
                        fontSize = 13.sp,
                        color = colors.ink2,
                    )
                }
                Switch(
                    checked = scheduleMode == TrackScheduleMode.AUTOMATIC,
                    onCheckedChange = { onToggleScheduleMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onAction,
                        checkedTrackColor = colors.action,
                        uncheckedThumbColor = colors.ink2,
                        uncheckedTrackColor = colors.chip,
                    ),
                )
            }

            Spacer(Modifier.height(10.dp))

            // + Create new track button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.chip)
                    .clickable {
                        onDismiss()
                        onCreateTrack()
                    },
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
        }
    }
}
