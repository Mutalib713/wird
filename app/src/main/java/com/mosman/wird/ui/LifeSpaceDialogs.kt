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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mosman.wird.domain.LifeSpace
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.ReadingDirection
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.TrackType
import com.mosman.wird.mushaf.MushafRepository
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill
import java.time.DayOfWeek
import kotlinx.coroutines.launch

/**
 * Dialog for managing life spaces (Home, School, Ramadan), freezing/unfreezing streaks, and creating new spaces.
 */
@Composable
fun LifeSpaceManagerDialog(
    spaces: List<LifeSpace>,
    activeSpaceId: String,
    onSetActiveSpace: (String) -> Unit,
    onToggleFreezeSpace: (String, Boolean) -> Unit,
    onAddSpace: (String, String?) -> Unit,
    onDeleteSpace: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    var showCreateModeDialog by remember { mutableStateOf(false) }
    var spacePendingDelete by remember { mutableStateOf<LifeSpace?>(null) }

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
                                text = "Reading Modes & Tracks",
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
                        text = "Reading modes keep different reading routines separated. For instance, you can keep Home tracks separate from School tracks. When you go to school or travel, freeze Home so your streaks stay protected.",
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

                            if (!space.goal.isNullOrBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "Goal: ${space.goal}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFC9A24B) else Color(0xFF9E782F),
                                )
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
                                            .clickable { spacePendingDelete = space }
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

                    // Create New Reading Mode Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clayPill(
                                backgroundColor = if (isDark) Color(0xFF1E3A2E) else Color(0xFFDCEDE3),
                                elevation = 2.dp,
                            )
                            .clickable { showCreateModeDialog = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+ ADD NEW READING MODE",
                            color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        )
                    }

                    Spacer(Modifier.height(14.dp))

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

    if (showCreateModeDialog) {
        CreateModeDialog(
            onConfirm = { name, goal ->
                onAddSpace(name, goal)
                showCreateModeDialog = false
            },
            onDismiss = { showCreateModeDialog = false },
        )
    }

    spacePendingDelete?.let { space ->
        DeleteConfirmDialog(
            title = "Delete Reading Mode",
            message = "Are you sure you want to delete \"${space.name}\"? All tracks inside this mode will be deleted.",
            onConfirm = {
                onDeleteSpace(space.id)
                spacePendingDelete = null
            },
            onDismiss = { spacePendingDelete = null },
        )
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
    var startSurahNumber by remember {
        mutableStateOf(track.startVerseSurah ?: SurahIndex.on(track.pageNumber).firstOrNull()?.number ?: 1)
    }
    var startAyahNumber by remember { mutableIntStateOf(track.startVerseAyah ?: 1) }
    var dailyUnits by remember { mutableIntStateOf(track.dailyUnits) }
    var isCustomTarget by remember { mutableStateOf(track.dailyUnits !in listOf(1, 2, 4)) }
    var customVersesText by remember {
        mutableStateOf(if (isCustomTarget) "${((track.dailyUnits / 2) * 10).coerceAtLeast(5)}" else "10")
    }
    var direction by remember { mutableStateOf(track.direction) }

    var showPositionPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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

                    // Field 3: Active Days
                    Column {
                        Text(
                            text = "ACTIVE DAYS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF245847),
                        )
                        Spacer(Modifier.height(6.dp))
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

                    // Field 4: Reading Position (Sūrah, Ayah, Page)
                    val surahObj = SurahIndex.byNumber(startSurahNumber)
                    val surahDisplay = surahObj?.name ?: (SurahIndex.on(startPage).firstOrNull()?.name ?: "Unknown")
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
                                text = "Page $startPage of 604",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                            )
                        }
                        Spacer(Modifier.height(6.dp))

                        // Tappable card to open rich Sūrah & Ayah position picker
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clayCard(
                                    shape = RoundedCornerShape(14.dp),
                                    backgroundColor = if (isDark) Color(0xFF13201A) else Color(0xFFF3EFE6),
                                    elevation = 2.dp,
                                )
                                .clickable { showPositionPicker = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = "$surahDisplay · Verse $startAyahNumber",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Tap to choose Sūrah & starting verse",
                                    fontSize = 11.5.sp,
                                    color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                )
                            }
                            Text(
                                text = "Choose ›",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Page nudge - / +
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
                                    .clickable {
                                        startPage = (startPage - 1).coerceAtLeast(1)
                                        SurahIndex.on(startPage).firstOrNull()?.let { s ->
                                            startSurahNumber = s.number
                                            startAyahNumber = 1
                                        }
                                    },
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
                                    text = "Page $startPage",
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
                                    .clickable {
                                        startPage = (startPage + 1).coerceAtMost(Mushaf.PAGES)
                                        SurahIndex.on(startPage).firstOrNull()?.let { s ->
                                            startSurahNumber = s.number
                                            startAyahNumber = 1
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847))
                            }
                        }
                    }

                    // Field 5: Daily Target with Presets and Custom Verses
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
                            ).forEach { (label, units) ->
                                val isSelected = !isCustomTarget && dailyUnits == units
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
                                        .clickable {
                                            dailyUnits = units
                                            isCustomTarget = false
                                        }
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

                            // Custom Target Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clayPill(
                                        backgroundColor = if (isCustomTarget) {
                                            if (isDark) Color(0xFF1E3A2E) else Color(0xFFDCEDE3)
                                        } else {
                                            if (isDark) Color(0xFF13201A) else Color(0xFFF3EFE6)
                                        },
                                        elevation = if (isCustomTarget) 2.dp else 1.dp,
                                    )
                                    .clickable { isCustomTarget = true }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Custom",
                                    fontSize = 11.sp,
                                    fontWeight = if (isCustomTarget) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCustomTarget) {
                                        if (isDark) Color(0xFF8ED676) else Color(0xFF245847)
                                    } else {
                                        if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378)
                                    },
                                )
                            }
                        }

                        // Custom Verses input row
                        if (isCustomTarget) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clayCard(
                                        shape = RoundedCornerShape(12.dp),
                                        backgroundColor = if (isDark) Color(0xFF14241B) else Color(0xFFF5EFE3),
                                        elevation = 1.dp,
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Verses per day:",
                                    fontSize = 12.5.sp,
                                    color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                    fontWeight = FontWeight.Medium,
                                )
                                Box(
                                    modifier = Modifier
                                        .width(64.dp)
                                        .clayCard(
                                            shape = RoundedCornerShape(8.dp),
                                            backgroundColor = if (isDark) Color(0xFF0F1A14) else Color.White,
                                            elevation = 1.dp,
                                        )
                                        .padding(horizontal = 6.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    BasicTextField(
                                        value = customVersesText,
                                        onValueChange = { text ->
                                            customVersesText = text.filter(Char::isDigit).take(3)
                                            val count = customVersesText.toIntOrNull() ?: 10
                                            dailyUnits = when {
                                                count <= 5 -> 1
                                                count <= 10 -> 2
                                                count <= 20 -> 4
                                                else -> ((count / 10) * 2).coerceIn(1, 40)
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(
                                            color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                        ),
                                        singleLine = true,
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
                                    .clickable { showDeleteConfirm = true }
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
                                        startVerseSurah = startSurahNumber,
                                        startVerseAyah = startAyahNumber,
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

    if (showPositionPicker) {
        TrackPositionPickerDialog(
            initialSurahNumber = startSurahNumber,
            initialAyahNumber = startAyahNumber,
            onPositionSelected = { surah, ayah, page ->
                startSurahNumber = surah.number
                startAyahNumber = ayah
                startPage = page
                showPositionPicker = false
            },
            onDismiss = { showPositionPicker = false },
        )
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            title = "Delete Reading Track",
            message = "Are you sure you want to delete \"${track.name}\"? This action cannot be undone.",
            onConfirm = {
                onDeleteTrack()
                showDeleteConfirm = false
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

/**
 * Dialog popup for adding a new Reading Mode with name and optional goal.
 */
@Composable
fun CreateModeDialog(
    onConfirm: (name: String, goal: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = if (isDark) Color(0xFF14221B) else Color.White,
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.95f),
                        shadowColor = Color.Black.copy(alpha = 0.35f),
                        elevation = 16.dp,
                    )
                    .clickable(enabled = false) {}
                    .padding(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "New Reading Mode",
                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = "Reading modes keep different reading routines separated. Streaks are tracked independently.",
                        color = if (isDark) Color(0xFF9CAFA4) else Color(0xFF6A7C73),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )

                    // Mode Name
                    Column {
                        Text(
                            text = "MODE NAME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
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
                            if (name.isEmpty()) {
                                Text(
                                    text = "e.g. Ramadan 2026, Campus, Travel",
                                    color = if (isDark) Color(0xFF5D7569) else Color(0xFFA0B0A6),
                                    fontSize = 13.sp,
                                )
                            }
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

                    // Optional Goal Box
                    Column {
                        Text(
                            text = "GOAL (OPTIONAL)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFC9A24B) else Color(0xFF9E782F),
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
                            if (goal.isEmpty()) {
                                Text(
                                    text = "e.g. Complete 1 Khatmah in 30 days",
                                    color = if (isDark) Color(0xFF5D7569) else Color(0xFFA0B0A6),
                                    fontSize = 13.sp,
                                )
                            }
                            BasicTextField(
                                value = goal,
                                onValueChange = { goal = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                ),
                                cursorBrush = SolidColor(Color(0xFFC9A24B)),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clayPill(
                                    backgroundColor = if (isDark) Color(0xFF1E2E25) else Color(0xFFEDE8DD),
                                    elevation = 2.dp,
                                )
                                .clickable(onClick = onDismiss)
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Cancel",
                                color = if (isDark) Color(0xFFB0C4B8) else Color(0xFF556C60),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        val canCreate = name.trim().isNotEmpty()
                        Box(
                            modifier = Modifier
                                .weight(1.5f)
                                .clayPill(
                                    backgroundColor = if (canCreate) Color(0xFF2D6B52) else Color(0xFF8FA597).copy(alpha = 0.4f),
                                    elevation = if (canCreate) 3.dp else 0.dp,
                                )
                                .clickable(enabled = canCreate) {
                                    val trimmedName = name.trim()
                                    val trimmedGoal = goal.trim().ifEmpty { null }
                                    onConfirm(trimmedName, trimmedGoal)
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Create Mode",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Confirmation dialog before deleting a mode or track.
 */
@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Yes, Delete",
    onConfirm: () -> Unit,
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
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayCard(
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = if (isDark) Color(0xFF1E1414) else Color.White,
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.95f),
                        shadowColor = Color.Black.copy(alpha = 0.35f),
                        elevation = 16.dp,
                    )
                    .clickable(enabled = false) {}
                    .padding(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = title,
                        color = if (isDark) Color(0xFFFF8B8B) else Color(0xFFB52A2A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = message,
                        color = if (isDark) Color(0xFFE4D5D5) else Color(0xFF4A3838),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clayPill(
                                    backgroundColor = if (isDark) Color(0xFF2A2222) else Color(0xFFEDE8DD),
                                    elevation = 2.dp,
                                )
                                .clickable(onClick = onDismiss)
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Keep It",
                                color = if (isDark) Color(0xFFE0D0D0) else Color(0xFF556C60),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clayPill(
                                    backgroundColor = Color(0xFFB52A2A),
                                    elevation = 3.dp,
                                )
                                .clickable(onClick = onConfirm)
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = confirmLabel,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Searchable modal picker for choosing Sūrah and starting Ayah for a ReadingTrack.
 */
@Composable
fun TrackPositionPickerDialog(
    initialSurahNumber: Int,
    initialAyahNumber: Int,
    onPositionSelected: (surah: Surah, ayah: Int, page: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { MushafRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    var currentStep by remember { mutableIntStateOf(1) } // 1: Choose Surah, 2: Choose Ayah
    var selectedSurah by remember { mutableStateOf(SurahIndex.byNumber(initialSurahNumber) ?: SurahIndex.all.first()) }
    var selectedAyah by remember { mutableIntStateOf(initialAyahNumber) }
    var ayahText by remember { mutableStateOf(initialAyahNumber.toString()) }
    var targetPage by remember { mutableIntStateOf(selectedSurah.firstPage) }
    var searchQuery by remember { mutableStateOf("") }

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
                        shape = RoundedCornerShape(26.dp),
                        backgroundColor = if (isDark) Color(0xFF14221B) else Color.White,
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.1f else 0.95f),
                        shadowColor = Color.Black.copy(alpha = 0.35f),
                        elevation = 16.dp,
                    )
                    .clickable(enabled = false) {}
                    .padding(20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 580.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentStep == 2) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable { currentStep = 1 },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to Surahs",
                                        tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = if (currentStep == 1) "Choose Starting Sūrah" else "Choose Starting Verse",
                                color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                fontSize = 17.sp,
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

                    Spacer(Modifier.height(12.dp))

                    if (currentStep == 1) {
                        // Search bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clayCard(
                                    shape = RoundedCornerShape(12.dp),
                                    backgroundColor = if (isDark) Color(0xFF13201A) else Color(0xFFF3EFE6),
                                    elevation = 1.dp,
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search by name or number...",
                                        color = if (isDark) Color(0xFF5D7569) else Color(0xFFA0B0A6),
                                        fontSize = 13.sp,
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                    ),
                                    cursorBrush = SolidColor(if (isDark) Color(0xFF8ED676) else Color(0xFF245847)),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { searchQuery = "" },
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Surah list
                        Box(modifier = Modifier.weight(1f)) {
                            SurahList(
                                searchQuery = searchQuery,
                                onPick = { surah ->
                                    selectedSurah = surah
                                    selectedAyah = 1
                                    ayahText = "1"
                                    targetPage = surah.firstPage
                                    currentStep = 2
                                },
                            )
                        }
                    } else {
                        // Step 2: Pick Ayah in selected Sūrah
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // Selected Surah info banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clayCard(
                                        shape = RoundedCornerShape(16.dp),
                                        backgroundColor = if (isDark) Color(0xFF1A3828) else Color(0xFFE2F0E6),
                                        elevation = 2.dp,
                                    )
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "${selectedSurah.number}. ${selectedSurah.name}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${selectedSurah.verses} verses · Page ${selectedSurah.firstPage}",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                    )
                                }
                                Text(
                                    text = selectedSurah.meaning,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFC0D3C9) else Color(0xFF436556),
                                )
                            }

                            // Ayah selector
                            Column {
                                Text(
                                    text = "STARTING AYAH NUMBER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
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
                                            .size(42.dp)
                                            .clayPill(
                                                backgroundColor = if (isDark) Color(0xFF1A3828) else Color(0xFFE2F0E6),
                                                elevation = 1.dp,
                                            )
                                            .clickable {
                                                val prev = (selectedAyah - 1).coerceAtLeast(1)
                                                selectedAyah = prev
                                                ayahText = prev.toString()
                                                coroutineScope.launch {
                                                    repo.pageOfVerse(selectedSurah.number, prev)?.let { p ->
                                                        targetPage = p
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847))
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
                                        BasicTextField(
                                            value = ayahText,
                                            onValueChange = { input ->
                                                ayahText = input.filter(Char::isDigit).take(3)
                                                val a = ayahText.toIntOrNull()
                                                if (a != null && a in 1..selectedSurah.verses) {
                                                    selectedAyah = a
                                                    coroutineScope.launch {
                                                        repo.pageOfVerse(selectedSurah.number, a)?.let { p ->
                                                            targetPage = p
                                                        }
                                                    }
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            textStyle = TextStyle(
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                                textAlign = TextAlign.Center,
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clayPill(
                                                backgroundColor = if (isDark) Color(0xFF1A3828) else Color(0xFFE2F0E6),
                                                elevation = 1.dp,
                                            )
                                            .clickable {
                                                val next = (selectedAyah + 1).coerceAtMost(selectedSurah.verses)
                                                selectedAyah = next
                                                ayahText = next.toString()
                                                coroutineScope.launch {
                                                    repo.pageOfVerse(selectedSurah.number, next)?.let { p ->
                                                        targetPage = p
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847))
                                    }
                                }

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Verse $selectedAyah of ${selectedSurah.verses} · Calculated Mushaf Page $targetPage",
                                    fontSize = 11.5.sp,
                                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // Confirm Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clayPill(
                                        backgroundColor = Color(0xFF2D6B52),
                                        elevation = 3.dp,
                                    )
                                    .clickable {
                                        onPositionSelected(selectedSurah, selectedAyah, targetPage)
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "USE THIS POSITION",
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
}

/**
 * Dialog for managing recitation tracks within a reading mode (Option C).
 */
@Composable
fun ManageTracksDialog(
    space: LifeSpace,
    onEditTrack: (ReadingTrack) -> Unit,
    onAddNewTrack: () -> Unit,
    onDeleteTrack: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    var trackPendingDelete by remember { mutableStateOf<ReadingTrack?>(null) }

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
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Recitation Tracks",
                                color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Mode: ${space.name}",
                                color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
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
                        text = "Tracks let you maintain parallel reading disciplines (e.g. daily Tilāwah, memorization, and revision).",
                        color = if (isDark) Color(0xFF9CAFA4) else Color(0xFF6A7C73),
                        fontSize = 12.sp,
                        lineHeight = 16.5.sp,
                    )

                    // Track Cards
                    if (space.tracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clayCard(
                                    shape = RoundedCornerShape(16.dp),
                                    backgroundColor = if (isDark) Color(0xFF13201A) else Color(0xFFF3EFE6),
                                    elevation = 1.dp,
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No tracks configured yet. Add your first track below.",
                                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        space.tracks.forEach { track ->
                            val trackBg = if (isDark) Color(0xFF16251E) else Color(0xFFF7F4EB)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clayCard(
                                        shape = RoundedCornerShape(16.dp),
                                        backgroundColor = trackBg,
                                        elevation = 2.dp,
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
                                        TrackTypeGlyph(
                                            type = track.type,
                                            tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = track.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clayPill(
                                                backgroundColor = if (isDark) Color(0xFF1A3828) else Color(0xFFE2F0E6),
                                                elevation = 1.dp,
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        Text(
                                            text = "${track.currentStreak}d streak",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))
                                val surah = track.surahName()
                                val posDesc = if (surah.isNotEmpty()) "$surah · Page ${track.pageNumber}" else "Page ${track.pageNumber}"
                                Text(
                                    text = "${track.type.label} · ${track.scheduleLabel()} · $posDesc",
                                    fontSize = 11.5.sp,
                                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                                )

                                Spacer(Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clayPill(
                                                backgroundColor = if (isDark) Color(0xFF192821) else Color(0xFFEAE5D9),
                                                elevation = 1.dp,
                                            )
                                            .clickable { onEditTrack(track) }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Edit Track",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                                        )
                                    }

                                    if (space.tracks.size > 1) {
                                        Box(
                                            modifier = Modifier
                                                .clayPill(
                                                    backgroundColor = if (isDark) Color(0xFF381C1C) else Color(0xFFFCEBEB),
                                                    elevation = 1.dp,
                                                )
                                                .clickable { trackPendingDelete = track }
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
                        }
                    }

                    // Add Track Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clayPill(
                                backgroundColor = if (isDark) Color(0xFF1E3A2E) else Color(0xFFDCEDE3),
                                elevation = 2.dp,
                            )
                            .clickable(onClick = onAddNewTrack)
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+ ADD NEW TRACK",
                            color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }

                    // Done Button
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

    trackPendingDelete?.let { track ->
        DeleteConfirmDialog(
            title = "Delete Reading Track",
            message = "Are you sure you want to delete \"${track.name}\"? This action cannot be undone.",
            onConfirm = {
                onDeleteTrack(track.id)
                trackPendingDelete = null
            },
            onDismiss = { trackPendingDelete = null },
        )
    }
}

