package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mosman.wird.domain.LifeSpace
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.ReadingDirection
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.TrackType
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill
import java.time.DayOfWeek

/**
 * Dialog for managing life spaces (Home, School, Ramadan), freezing/unfreezing streaks, and creating new spaces.
 */
@Composable
fun LifeSpaceManagerDialog(
    spaces: List<LifeSpace>,
    activeSpaceId: String,
    onSetActiveSpace: (String) -> Unit,
    onToggleFreezeSpace: (String, Boolean) -> Unit,
    onAddSpace: (String) -> Unit,
    onDeleteSpace: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    var newSpaceName by remember { mutableStateOf("") }

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
                        shape = RoundedCornerShape(26.dp),
                        backgroundColor = if (isDark) Color(0xFF14221B) else Color.White,
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.95f),
                        shadowColor = Color.Black.copy(alpha = 0.35f),
                        elevation = 16.dp,
                    )
                    .clickable(enabled = false) {}
                    .padding(22.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
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
                                text = "Life Spaces & Routines",
                                color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
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

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Life spaces keep different routines separated. For instance, you can keep Home tracks separate from School tracks. When you go to school or travel, freeze Home so your streaks stay protected.",
                        color = if (isDark) Color(0xFF9CAFA4) else Color(0xFF6A7C73),
                        fontSize = 12.sp,
                        lineHeight = 16.5.sp,
                    )

                    Spacer(Modifier.height(16.dp))

                    // Existing Spaces List
                    spaces.forEach { space ->
                        val isActive = space.id == activeSpaceId
                        val spaceBg = if (isActive) {
                            if (isDark) Color(0xFF1E3A2E) else Color(0xFFDCEDE3)
                        } else {
                            if (isDark) Color(0xFF16251E) else Color(0xFFF3EFE6)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clayCard(
                                    shape = RoundedCornerShape(18.dp),
                                    backgroundColor = spaceBg,
                                    elevation = if (isActive) 3.dp else 1.dp,
                                )
                                .padding(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = space.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                    )
                                    if (space.isFrozen) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isDark) Color(0xFF1E3345) else Color(0xFFE1F0FA))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                FreezeGlyph(
                                                    tint = if (isDark) Color(0xFF90CAF9) else Color(0xFF1976D2),
                                                    modifier = Modifier.size(11.dp),
                                                )
                                                Text(
                                                    text = "Paused",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFF90CAF9) else Color(0xFF1976D2),
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (isDark) Color(0xFF8ED676) else Color(0xFF245847))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF0D2720) else Color.White,
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clayPill(
                                                backgroundColor = if (isDark) Color(0xFF1A3828) else Color(0xFFE2F0E6),
                                                elevation = 1.dp,
                                            )
                                            .clickable { onSetActiveSpace(space.id) }
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                    ) {
                                        Text(
                                            text = "Switch to this",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "${space.tracks.size} reading track${if (space.tracks.size != 1) "s" else ""}: " +
                                    space.tracks.joinToString(", ") { it.name },
                                fontSize = 11.5.sp,
                                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                            )

                            Spacer(Modifier.height(10.dp))

                            // Action buttons: Pause toggle & Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // Freeze/Pause Toggle
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clayPill(
                                            backgroundColor = if (space.isFrozen) {
                                                if (isDark) Color(0xFF2D4B3E) else Color(0xFFD3EADB)
                                            } else {
                                                if (isDark) Color(0xFF192821) else Color(0xFFEAE5D9)
                                            },
                                            elevation = 1.dp,
                                        )
                                        .clickable { onToggleFreezeSpace(space.id, !space.isFrozen) }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (space.isFrozen) "Resume Streaks" else "Pause & Freeze",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (space.isFrozen) {
                                            if (isDark) Color(0xFF8ED676) else Color(0xFF245847)
                                        } else {
                                            if (isDark) Color(0xFFB0C4B8) else Color(0xFF5A7266)
                                        },
                                    )
                                }

                                if (spaces.size > 1 && !isActive) {
                                    Box(
                                        modifier = Modifier
                                            .clayPill(
                                                backgroundColor = if (isDark) Color(0xFF381C1C) else Color(0xFFFCEBEB),
                                                elevation = 1.dp,
                                            )
                                            .clickable { onDeleteSpace(space.id) }
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Delete",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFFFF8B8B) else Color(0xFFB52A2A),
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Spacer(Modifier.height(10.dp))

                    // Create New Life Space Section
                    Text(
                        text = "+ CREATE NEW LIFE SPACE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF245847),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clayCard(
                                    shape = RoundedCornerShape(12.dp),
                                    backgroundColor = if (isDark) Color(0xFF13201A) else Color(0xFFF3EFE6),
                                    elevation = 1.dp,
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            if (newSpaceName.isEmpty()) {
                                Text(
                                    text = "e.g. Ramadan, Campus, Travel",
                                    color = if (isDark) Color(0xFF5D7569) else Color(0xFFA0B0A6),
                                    fontSize = 13.sp,
                                )
                            }
                            BasicTextField(
                                value = newSpaceName,
                                onValueChange = { newSpaceName = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                ),
                                cursorBrush = SolidColor(if (isDark) Color(0xFF8ED676) else Color(0xFF245847)),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clayPill(
                                    backgroundColor = Color(0xFF2D6B52),
                                    elevation = 2.dp,
                                )
                                .clickable {
                                    val trimmed = newSpaceName.trim()
                                    if (trimmed.isNotEmpty()) {
                                        onAddSpace(trimmed)
                                        newSpaceName = ""
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Add Space",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Done button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clayPill(
                                backgroundColor = Color(0xFF2D6B52),
                                elevation = 3.dp,
                            )
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "DONE",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog to configure or create a ReadingTrack (discipline, active days, start surah, daily target).
 */
@Composable
fun EditTrackDialog(
    track: ReadingTrack,
    canDelete: Boolean,
    onSaveTrack: (ReadingTrack) -> Unit,
    onDeleteTrack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    var name by remember { mutableStateOf(track.name) }
    var type by remember { mutableStateOf(track.type) }
    var activeDays by remember { mutableStateOf(track.activeDays) }
    var startPage by remember { mutableIntStateOf(track.pageNumber) }
    var dailyUnits by remember { mutableIntStateOf(track.dailyUnits) }
    var direction by remember { mutableStateOf(track.direction) }

    val monThu = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)
    val satSun = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    val daily = DayOfWeek.entries.toSet()

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
                        shape = RoundedCornerShape(26.dp),
                        backgroundColor = if (isDark) Color(0xFF14221B) else Color.White,
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.95f),
                        shadowColor = Color.Black.copy(alpha = 0.35f),
                        elevation = 16.dp,
                    )
                    .clickable(enabled = false) {}
                    .padding(22.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 580.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Reading Track Setup",
                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
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

                    // Field 1: Track Name
                    Column {
                        Text(
                            text = "TRACK NAME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF245847),
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clayCard(
                                    shape = RoundedCornerShape(12.dp),
                                    backgroundColor = if (isDark) Color(0xFF13201A) else Color(0xFFF3EFE6),
                                    elevation = 1.dp,
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                ),
                                cursorBrush = SolidColor(if (isDark) Color(0xFF8ED676) else Color(0xFF245847)),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // Field 2: Discipline (Ḥifẓ, Tilāwah, Murāja'ah)
                    Column {
                        Text(
                            text = "DISCIPLINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF245847),
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TrackType.entries.forEach { t ->
                                val isSelected = type == t
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clayPill(
                                            backgroundColor = if (isSelected) {
                                                if (isDark) Color(0xFF1E3A2E) else Color(0xFFDCEDE3)
                                            } else {
                                                if (isDark) Color(0xFF13201A) else Color(0xFFF3EFE6)
                                            },
                                            elevation = if (isSelected) 3.dp else 1.dp,
                                        )
                                        .clickable { type = t }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = t.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            if (isDark) Color(0xFF8ED676) else Color(0xFF245847)
                                        } else {
                                            if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378)
                                        },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = type.meaning,
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                        )
                    }

                    // Field 3: Active Days (Quick Presets + Day Toggles)
                    Column {
                        Text(
                            text = "ACTIVE DAYS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF245847),
                        )
                        Spacer(Modifier.height(6.dp))
                        // Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf("Mon–Thu" to monThu, "Sat–Sun" to satSun, "Daily" to daily).forEach { (label, days) ->
                                val isSelected = activeDays == days
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clayPill(
                                            backgroundColor = if (isSelected) {
                                                if (isDark) Color(0xFF1E3A2E) else Color(0xFFDCEDE3)
                                            } else {
                                                if (isDark) Color(0xFF13201A) else Color(0xFFF3EFE6)
                                            },
                                            elevation = if (isSelected) 2.dp else 1.dp,
                                        )
                                        .clickable { activeDays = days }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            if (isDark) Color(0xFF8ED676) else Color(0xFF245847)
                                        } else {
                                            if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378)
                                        },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // Individual day checkboxes/chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            listOf(
                                "M" to DayOfWeek.MONDAY,
                                "T" to DayOfWeek.TUESDAY,
                                "W" to DayOfWeek.WEDNESDAY,
                                "T" to DayOfWeek.THURSDAY,
                                "F" to DayOfWeek.FRIDAY,
                                "S" to DayOfWeek.SATURDAY,
                                "S" to DayOfWeek.SUNDAY,
                            ).forEach { (char, dow) ->
                                val isSelected = dow in activeDays
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clayPill(
                                            backgroundColor = if (isSelected) {
                                                if (isDark) Color(0xFF8ED676) else Color(0xFF245847)
                                            } else {
                                                if (isDark) Color(0xFF13201A) else Color(0xFFF3EFE6)
                                            },
                                            elevation = if (isSelected) 2.dp else 1.dp,
                                        )
                                        .clickable {
                                            activeDays = if (isSelected) activeDays - dow else activeDays + dow
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = char,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            if (isDark) Color(0xFF0D2720) else Color.White
                                        } else {
                                            if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Field 4: Reading Position (Page Number)
                    val surahAtPage = SurahIndex.on(startPage).firstOrNull()?.name ?: "Unknown"
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "READING POSITION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF8FA597) else Color(0xFF245847),
                            )
                            Text(
                                text = "Page $startPage · $surahAtPage",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clayPill(
                                        backgroundColor = if (isDark) Color(0xFF1A3828) else Color(0xFFE2F0E6),
                                        elevation = 1.dp,
                                    )
                                    .clickable { startPage = (startPage - 1).coerceAtLeast(1) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847))
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clayCard(
                                        shape = RoundedCornerShape(12.dp),
                                        backgroundColor = if (isDark) Color(0xFF13201A) else Color(0xFFF3EFE6),
                                        elevation = 1.dp,
                                    )
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Page $startPage of 604",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clayPill(
                                        backgroundColor = if (isDark) Color(0xFF1A3828) else Color(0xFFE2F0E6),
                                        elevation = 1.dp,
                                    )
                                    .clickable { startPage = (startPage + 1).coerceAtMost(Mushaf.PAGES) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847))
                            }
                        }
                    }

                    // Field 5: Daily Target
                    Column {
                        Text(
                            text = "DAILY TARGET",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF245847),
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(
                                "Half page" to 1,
                                "1 page" to 2,
                                "2 pages" to 4,
                                "4 pages" to 8,
                            ).forEach { (label, units) ->
                                val isSelected = dailyUnits == units
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clayPill(
                                            backgroundColor = if (isSelected) {
                                                if (isDark) Color(0xFF1E3A2E) else Color(0xFFDCEDE3)
                                            } else {
                                                if (isDark) Color(0xFF13201A) else Color(0xFFF3EFE6)
                                            },
                                            elevation = if (isSelected) 2.dp else 1.dp,
                                        )
                                        .clickable { dailyUnits = units }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            if (isDark) Color(0xFF8ED676) else Color(0xFF245847)
                                        } else {
                                            if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Save and Delete Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (canDelete) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clayPill(
                                        backgroundColor = if (isDark) Color(0xFF381C1C) else Color(0xFFFCEBEB),
                                        elevation = 2.dp,
                                    )
                                    .clickable {
                                        onDeleteTrack()
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Delete",
                                    color = if (isDark) Color(0xFFFF8B8B) else Color(0xFFB52A2A),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(if (canDelete) 1.5f else 1f)
                                .clayPill(
                                    backgroundColor = Color(0xFF2D6B52),
                                    elevation = 3.dp,
                                )
                                .clickable {
                                    val updated = track.copy(
                                        name = name.trim().ifEmpty { track.name },
                                        type = type,
                                        activeDays = activeDays,
                                        positionUnit = (startPage - 1) * Mushaf.UNITS_PER_PAGE,
                                        dailyUnits = dailyUnits,
                                        direction = direction,
                                    )
                                    onSaveTrack(updated)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "SAVE TRACK",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
