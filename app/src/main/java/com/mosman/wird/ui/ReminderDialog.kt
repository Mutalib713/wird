package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mosman.wird.data.decodeSchedule
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.NudgeSchedule
import com.mosman.wird.domain.PageVerses
import com.mosman.wird.domain.Prayer
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill
import java.time.LocalTime

private enum class ReminderMode {
    AFTER_PRAYERS,
    CLOCK_TIME,
    OFF
}

/**
 * Prompt dialog shown immediately upon creating a new reading track,
 * allowing the user to either customize its notification schedule right away
 * or keep the current default schedule.
 */
@Composable
fun TrackReminderPromptDialog(
    track: ReadingTrack,
    onCustomize: () -> Unit,
    onKeepCurrent: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        backgroundColor = if (isDark) Color(0xFF14241C) else Color(0xFFFAF7EE),
                        shape = RoundedCornerShape(24.dp),
                        elevation = 6.dp,
                    )
                    .clickable(enabled = false) {}
                    .padding(22.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1C3A29) else Color(0xFFD6EDE0)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF86E39D) else Color(0xFF245847),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Reminders for ${track.name}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Track created! Would you like to customize notification reminders for this track or keep your current schedule?",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp,
                    )
                    Spacer(Modifier.height(20.dp))

                    // Customize Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clayPill(
                                backgroundColor = if (isDark) Color(0xFF1E4531) else Color(0xFF245847),
                                elevation = 3.dp,
                            )
                            .clickable(onClick = onCustomize)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Customize Notification",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Keep Current Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF162A1F) else Color(0xFFE4EDE5))
                            .clickable(onClick = onKeepCurrent)
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Keep Current Schedule",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Advanced tactile modal dialog for configuring per-track reminders.
 *
 * Supports:
 * - Switching tracks directly at the top.
 * - Multi-prayer selection with 10, 15, 30 min presets and Custom minutes.
 * - Fixed clock time with Once, 1 hr, 2 hrs, 3 hrs presets and Custom hours repeat nag.
 * - Silence mode (Off).
 * - Real-time notification preview card showing the exact Surah and Ayahs to recite.
 */
@Composable
fun AdvancedReminderDialog(
    initialTrack: ReadingTrack,
    allTracks: List<ReadingTrack>,
    initialSchedule: NudgeSchedule,
    onSave: (ReadingTrack, NudgeSchedule) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    var currentTrack by remember { mutableStateOf(initialTrack) }

    // Resolve active schedule for currentTrack
    var trackSchedule by remember(currentTrack.id) {
        mutableStateOf(currentTrack.reminderScheduleRaw?.let(::decodeSchedule) ?: initialSchedule)
    }

    var mode by remember(trackSchedule) {
        mutableStateOf(
            when (trackSchedule) {
                is NudgeSchedule.AfterPrayers, is NudgeSchedule.AfterPrayer -> ReminderMode.AFTER_PRAYERS
                is NudgeSchedule.AtClockTime -> ReminderMode.CLOCK_TIME
                is NudgeSchedule.Off -> ReminderMode.OFF
            }
        )
    }

    // After Prayers state
    var selectedPrayers by remember(trackSchedule) {
        mutableStateOf(
            when (val s = trackSchedule) {
                is NudgeSchedule.AfterPrayers -> s.prayers
                is NudgeSchedule.AfterPrayer -> setOf(s.prayer)
                else -> setOf(Prayer.FAJR, Prayer.MAGHRIB)
            }
        )
    }
    var offsetMinutes by remember(trackSchedule) {
        mutableIntStateOf(
            when (val s = trackSchedule) {
                is NudgeSchedule.AfterPrayers -> s.offsetMinutes
                is NudgeSchedule.AfterPrayer -> s.offsetMinutes
                else -> 15
            }
        )
    }
    var isCustomOffset by remember(trackSchedule) {
        mutableStateOf(offsetMinutes !in listOf(10, 15, 30))
    }

    // Clock Time state
    var clockHour by remember(trackSchedule) {
        mutableIntStateOf(
            when (val s = trackSchedule) {
                is NudgeSchedule.AtClockTime -> s.time.hour
                else -> 14 // 2:00 PM
            }
        )
    }
    var clockMinute by remember(trackSchedule) {
        mutableIntStateOf(
            when (val s = trackSchedule) {
                is NudgeSchedule.AtClockTime -> s.time.minute
                else -> 0
            }
        )
    }
    var repeatInterval by remember(trackSchedule) {
        mutableIntStateOf(
            when (val s = trackSchedule) {
                is NudgeSchedule.AtClockTime -> s.repeatIntervalHours
                else -> 0
            }
        )
    }
    var isCustomRepeat by remember(trackSchedule) {
        mutableStateOf(repeatInterval !in listOf(0, 1, 2, 3))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        backgroundColor = if (isDark) Color(0xFF14241C) else Color(0xFFFAF7EE),
                        shape = RoundedCornerShape(24.dp),
                        elevation = 6.dp,
                    )
                    .clickable(enabled = false) {}
                    .padding(20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Header with Bell Icon and Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1C3A29) else Color(0xFFD6EDE0)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF86E39D) else Color(0xFF245847),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Track Reminders",
                                    color = colors.textPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Independent notification schedule",
                                    color = colors.textSecondary,
                                    fontSize = 11.5.sp,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E3326) else Color(0xFFE4EDE5))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    // Track selector chips if more than 1 track exists
                    if (allTracks.size > 1) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "CONFIGURE TRACK",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF7FA890) else Color(0xFF4A725D),
                            letterSpacing = 0.8.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            allTracks.forEach { track ->
                                val isSelected = track.id == currentTrack.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) {
                                                if (isDark) Color(0xFF1E432F) else Color(0xFF245847)
                                            } else {
                                                if (isDark) Color(0xFF1A2E23) else Color(0xFFE4ECE6)
                                            }
                                        )
                                        .clickable {
                                            currentTrack = track
                                            val sched = track.reminderScheduleRaw?.let(::decodeSchedule) ?: initialSchedule
                                            trackSchedule = sched
                                            mode = when (sched) {
                                                is NudgeSchedule.AfterPrayers, is NudgeSchedule.AfterPrayer -> ReminderMode.AFTER_PRAYERS
                                                is NudgeSchedule.AtClockTime -> ReminderMode.CLOCK_TIME
                                                is NudgeSchedule.Off -> ReminderMode.OFF
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = track.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else colors.textSecondary,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Mode segmented selector
                    Text(
                        text = "SCHEDULE TYPE",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF7FA890) else Color(0xFF4A725D),
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF0F1E16) else Color(0xFFDFECE3))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        listOf(
                            ReminderMode.AFTER_PRAYERS to "After Prayers",
                            ReminderMode.CLOCK_TIME to "Set Time",
                            ReminderMode.OFF to "Off",
                        ).forEach { (m, label) ->
                            val isChosen = mode == m
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isChosen) {
                                            if (isDark) Color(0xFF1F4330) else Color(0xFF245847)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .clickable { mode = m }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) Color.White else colors.textSecondary,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    when (mode) {
                        ReminderMode.AFTER_PRAYERS -> {
                            // Section: Select prayers
                            Text(
                                text = "SELECT PRAYERS",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF7FA890) else Color(0xFF4A725D),
                                letterSpacing = 0.8.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Wird calculates offline prayer times for your location and nudges you after the selected prayers.",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                lineHeight = 15.sp,
                            )
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Prayer.entries.forEach { prayer ->
                                    val isChecked = prayer in selectedPrayers
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isChecked) {
                                                    if (isDark) Color(0xFF1E432F) else Color(0xFF2D6B52)
                                                } else {
                                                    if (isDark) Color(0xFF1B2F23) else Color(0xFFE4EDE5)
                                                }
                                            )
                                            .clickable {
                                                selectedPrayers = if (isChecked) {
                                                    if (selectedPrayers.size > 1) selectedPrayers - prayer else selectedPrayers
                                                } else {
                                                    selectedPrayers + prayer
                                                }
                                            }
                                            .padding(horizontal = 11.dp, vertical = 8.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isChecked) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp),
                                                )
                                                Spacer(Modifier.width(5.dp))
                                            }
                                            Text(
                                                text = prayer.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isChecked) Color.White else colors.textSecondary,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // Delay offset selector with 10, 15, 30, and Custom
                            Text(
                                text = "DELAY AFTER PRAYER",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF7FA890) else Color(0xFF4A725D),
                                letterSpacing = 0.8.sp,
                            )
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                listOf(10, 15, 30).forEach { mins ->
                                    val isSelected = !isCustomOffset && offsetMinutes == mins
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) {
                                                    if (isDark) Color(0xFF86E39D) else Color(0xFF245847)
                                                } else {
                                                    if (isDark) Color(0xFF1B2F23) else Color(0xFFE4EDE5)
                                                }
                                            )
                                            .clickable {
                                                offsetMinutes = mins
                                                isCustomOffset = false
                                            }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "+$mins min",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                if (isDark) Color(0xFF0C2417) else Color.White
                                            } else {
                                                colors.textSecondary
                                            },
                                        )
                                    }
                                }

                                // Custom Chip
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isCustomOffset) {
                                                if (isDark) Color(0xFF86E39D) else Color(0xFF245847)
                                            } else {
                                                if (isDark) Color(0xFF1B2F23) else Color(0xFFE4EDE5)
                                            }
                                        )
                                        .clickable { isCustomOffset = true }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Custom",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isCustomOffset) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCustomOffset) {
                                            if (isDark) Color(0xFF0C2417) else Color.White
                                        } else {
                                            colors.textSecondary
                                        },
                                    )
                                }
                            }

                            // Custom delay stepper when Custom is chosen
                            if (isCustomOffset) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color(0xFF102017) else Color(0xFFE5EDE6))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Custom delay:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (isDark) Color(0xFF1D3B2B) else Color(0xFFD3E4D7))
                                            .clickable {
                                                offsetMinutes = (offsetMinutes - 5).coerceAtLeast(1)
                                            },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "$offsetMinutes min",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary,
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (isDark) Color(0xFF1D3B2B) else Color(0xFFD3E4D7))
                                                .clickable {
                                                    offsetMinutes = (offsetMinutes + 5).coerceAtMost(180)
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        ReminderMode.CLOCK_TIME -> {
                            // Section: Clock time picker
                            Text(
                                text = "SET CLOCK TIME",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF7FA890) else Color(0xFF4A725D),
                                letterSpacing = 0.8.sp,
                            )
                            Spacer(Modifier.height(8.dp))

                            // Time stepper row
                            val displayHour12 = when (val h = clockHour % 12) {
                                0 -> 12
                                else -> h
                            }
                            val isPm = clockHour >= 12

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color(0xFF102017) else Color(0xFFE5EDE6))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Hour steppers
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0xFF1D3B2B) else Color(0xFFD3E4D7))
                                            .clickable {
                                                clockHour = if (clockHour == 0) 23 else clockHour - 1
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "%02d".format(displayHour12),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0xFF1D3B2B) else Color(0xFFD3E4D7))
                                            .clickable {
                                                clockHour = (clockHour + 1) % 24
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    }
                                }

                                Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)

                                // Minute steppers
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0xFF1D3B2B) else Color(0xFFD3E4D7))
                                            .clickable {
                                                clockMinute = (clockMinute - 5 + 60) % 60
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "%02d".format(clockMinute),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0xFF1D3B2B) else Color(0xFFD3E4D7))
                                            .clickable {
                                                clockMinute = (clockMinute + 5) % 60
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    }
                                }

                                // AM/PM Toggle
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDark) Color(0xFF1E432F) else Color(0xFF245847))
                                        .clickable {
                                            clockHour = if (isPm) clockHour - 12 else clockHour + 12
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                ) {
                                    Text(
                                        text = if (isPm) "PM" else "AM",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // Repeat until recited chips with Custom
                            Text(
                                text = "REPEAT IF NOT RECITED TODAY",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF7FA890) else Color(0xFF4A725D),
                                letterSpacing = 0.8.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "If you have not yet completed today's portion for this track, Wird can gently re-remind you at intervals.",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                lineHeight = 15.sp,
                            )
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                listOf(
                                    0 to "Once",
                                    1 to "1 hr",
                                    2 to "2 hrs",
                                    3 to "3 hrs",
                                ).forEach { (rep, label) ->
                                    val isSelected = !isCustomRepeat && repeatInterval == rep
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) {
                                                    if (isDark) Color(0xFF86E39D) else Color(0xFF245847)
                                                } else {
                                                    if (isDark) Color(0xFF1B2F23) else Color(0xFFE4EDE5)
                                                }
                                            )
                                            .clickable {
                                                repeatInterval = rep
                                                isCustomRepeat = false
                                            }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                if (isDark) Color(0xFF0C2417) else Color.White
                                            } else {
                                                colors.textSecondary
                                            },
                                        )
                                    }
                                }

                                // Custom Repeat Chip
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isCustomRepeat) {
                                                if (isDark) Color(0xFF86E39D) else Color(0xFF245847)
                                            } else {
                                                if (isDark) Color(0xFF1B2F23) else Color(0xFFE4EDE5)
                                            }
                                        )
                                        .clickable { isCustomRepeat = true }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Custom",
                                        fontSize = 11.sp,
                                        fontWeight = if (isCustomRepeat) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCustomRepeat) {
                                            if (isDark) Color(0xFF0C2417) else Color.White
                                        } else {
                                            colors.textSecondary
                                        },
                                    )
                                }
                            }

                            // Custom repeat stepper when Custom is chosen
                            if (isCustomRepeat) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color(0xFF102017) else Color(0xFFE5EDE6))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Repeat interval:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (isDark) Color(0xFF1D3B2B) else Color(0xFFD3E4D7))
                                                .clickable {
                                                    repeatInterval = (repeatInterval - 1).coerceAtLeast(1)
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "Every $repeatInterval hrs",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary,
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (isDark) Color(0xFF1D3B2B) else Color(0xFFD3E4D7))
                                                .clickable {
                                                    repeatInterval = (repeatInterval + 1).coerceAtMost(12)
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        ReminderMode.OFF -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDark) Color(0xFF1B261F) else Color(0xFFE8EFEA))
                                    .padding(14.dp),
                            ) {
                                Text(
                                    text = "Reminders are paused for '${currentTrack.name}'. You will not receive any daily notifications for this track.",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 17.sp,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Notification Preview Card
                    if (mode != ReminderMode.OFF) {
                        Text(
                            text = "NOTIFICATION PREVIEW",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF7FA890) else Color(0xFF4A725D),
                            letterSpacing = 0.8.sp,
                        )
                        Spacer(Modifier.height(6.dp))

                        val trackPage = currentTrack.pageNumber
                        val surahs = remember(trackPage) { SurahIndex.on(trackPage) }
                        val primarySurah = surahs.firstOrNull() ?: SurahIndex.on(1).first()
                        val ayahRange = remember(trackPage, primarySurah) {
                            PageVerses.ayahRange(context, listOf(trackPage), primarySurah.number)
                        }
                        val recitationPortionText = if (ayahRange != null) {
                            "Time to recite ${primarySurah.name} ($ayahRange) · Page $trackPage"
                        } else {
                            "Time to recite ${primarySurah.name} · Page $trackPage"
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) Color(0xFF2C4A38) else Color(0xFFC8DEC9),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .background(if (isDark) Color(0xFF0F1E16) else Color(0xFFF1F6F2))
                                .padding(12.dp),
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0xFF86E39D) else Color(0xFF245847)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("W", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Your Daily Wırd · ${currentTrack.name}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = recitationPortionText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                )
                                Spacer(Modifier.height(3.dp))
                                val timingLabel = when (mode) {
                                    ReminderMode.AFTER_PRAYERS -> {
                                        val pList = selectedPrayers.sortedBy { it.ordinal }.joinToString(", ") { it.label }
                                        "Scheduled $offsetMinutes min after $pList"
                                    }
                                    ReminderMode.CLOCK_TIME -> {
                                        val repLabel = if (repeatInterval > 0) " · Repeats every ${repeatInterval}h if unread" else ""
                                        "Scheduled for %02d:%02d%s".format(
                                            clockHour,
                                            clockMinute,
                                            repLabel,
                                        )
                                    }
                                    ReminderMode.OFF -> ""
                                }
                                Text(
                                    text = timingLabel,
                                    fontSize = 10.5.sp,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }

                    // Save / Apply Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clayPill(
                                backgroundColor = if (isDark) Color(0xFF1E4531) else Color(0xFF245847),
                                elevation = 3.dp,
                            )
                            .clickable {
                                val computedSchedule: NudgeSchedule = when (mode) {
                                    ReminderMode.AFTER_PRAYERS -> {
                                        val finalPrayers = if (selectedPrayers.isEmpty()) setOf(Prayer.MAGHRIB) else selectedPrayers
                                        NudgeSchedule.AfterPrayers(finalPrayers, offsetMinutes)
                                    }
                                    ReminderMode.CLOCK_TIME -> {
                                        val validTime = LocalTime.of(clockHour.coerceIn(0, 23), clockMinute.coerceIn(0, 59))
                                        NudgeSchedule.AtClockTime(validTime, repeatInterval)
                                    }
                                    ReminderMode.OFF -> NudgeSchedule.Off
                                }
                                onSave(currentTrack, computedSchedule)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Save Reminder Schedule",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
