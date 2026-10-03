package com.mosman.wird.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mosman.wird.audio.AudioState
import com.mosman.wird.audio.RecitationModel
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Progress
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill


/**
 * Marking the day done, at the foot of the page.
 *
 * Placed here on purpose: this is where you arrive when you have actually finished
 * reading, so the control is *earned* rather than parked somewhere convenient.
 *
 * While a recitation is running this shows nothing — the recording bar pinned to the top
 * of the screen owns that state, because the moment you start reciting you scroll up to
 * read and anything down here goes out of sight.
 *
 * Two ways, and the app never pretends they are the same. Reciting cannot be done without
 * doing it. Tapping exists because there are lecture halls, and every tap is logged as a
 * tap. Sacred Rule 6.
 */
@Composable
fun DoneControl(
    doneMethod: Method?,
    mode: ReadingMode = ReadingMode.READING,
    hasRecording: Boolean,
    recording: Boolean,
    problem: String?,
    onStartRecording: () -> Unit,
    onMicRefused: () -> Unit,
    onTap: () -> Unit,
    onPlay: () -> Unit,
    onUndo: () -> Unit,
    onCheck: (() -> Unit)? = null,
    checkState: CheckState = CheckState.Idle,
    audio: AudioState = AudioState.Idle,
    onListen: () -> Unit = {},
    page: Int = 0,
    ayahCount: Int = 0,
    surahName: String = "",
    progress: Progress? = null,
    onDownloadModel: ((RecitationModel) -> Unit)? = null,
    isModelReady: Boolean = false,
) {
    val colors = LocalWirdColors.current
    val context = LocalContext.current
    var showModelDialog by remember { mutableStateOf(false) }

    val askMic = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) onStartRecording() else onMicRefused() }

    if (showModelDialog) {
        ClayOptionDialog(
            title = "Offline Recitation Checker",
            options = listOf(
                DialogOption(
                    RecitationModel.TINY,
                    "Whisper Compact (42 MB)",
                    "Faster on-device model, uses less RAM. Recommended.",
                ),
                DialogOption(
                    RecitationModel.BASE,
                    "Whisper Accurate (78 MB)",
                    "Higher precision Arabic phoneme recognition.",
                ),
            ),
            selected = RecitationModel.TINY,
            onSelect = { chosen ->
                showModelDialog = false
                onDownloadModel?.invoke(chosen)
            },
            onDismiss = { showModelDialog = false },
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = Scale.space6)) {
        when {
            recording -> Text(
                text = "Reciting. The controls are at the top of the screen.",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
            )

            doneMethod != null ->
                AlreadyDone(
                    method = doneMethod,
                    hasRecording = hasRecording,
                    surahName = surahName,
                    onPlay = onPlay,
                    onUndo = onUndo,
                    onCheck = onCheck,
                    checkState = checkState,
                    isModelReady = isModelReady,
                    onRequestModelDialog = { showModelDialog = true },
                )

            else -> NotYet(
                mode = mode,
                page = page,
                ayahCount = ayahCount,
                progress = progress,
                onRecite = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) onStartRecording() else askMic.launch(Manifest.permission.RECORD_AUDIO)
                },
                onTap = onTap,
                audio = audio,
                onListen = onListen,
                isModelReady = isModelReady,
                onDownloadPrompt = { showModelDialog = true },
            )
        }

        problem?.let {
            Spacer(Modifier.height(Scale.space2))
            Text(it, color = colors.textSecondary, style = TextStyle(fontSize = Scale.caption))
        }
    }
}

@Composable
private fun NotYet(
    mode: ReadingMode,
    page: Int,
    ayahCount: Int,
    progress: Progress?,
    onRecite: () -> Unit,
    onTap: () -> Unit,
    audio: AudioState,
    onListen: () -> Unit,
    isModelReady: Boolean = false,
    onDownloadPrompt: () -> Unit = {},
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = if (isDark) Color(0xFF1C1D22) else Color(0xFFFAF7F0),
                elevation = 3.dp,
                highlightColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.6f),
                shadowColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.12f),
                strokeWidth = 1.dp,
            )
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header kicker row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (ayahCount > 0) "TODAY'S WIRD · $ayahCount VERSES" else "TODAY'S WIRD",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                    ),
                )
                if (page > 0) {
                    Box(
                        modifier = Modifier
                            .clayPill(
                                shape = RoundedCornerShape(999.dp),
                                backgroundColor = if (isDark) Color(0xFF26272E) else Color(0xFFEDE8DC),
                                elevation = 1.dp,
                            )
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "Page $page of 604",
                            style = TextStyle(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                            ),
                        )
                    }
                }
            }

            // Primary CTA: Tactile deep forest green button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clayCard(
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = if (isDark) Color(0xFF1D3B30) else Color(0xFF245847),
                        elevation = 3.dp,
                        highlightColor = Color(0xFF48826D).copy(alpha = 0.5f),
                        shadowColor = Color.Black.copy(alpha = 0.35f),
                    )
                    .clickable(onClick = onRecite)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MicVectorIcon(
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = reciteLabel(mode),
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                }
            }

            // Recitation review status helper badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isModelReady) {
                    SparkleVectorIcon(
                        tint = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "100% offline Whisper AI voice check ready",
                        color = colors.textSecondary,
                        style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDownloadPrompt)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        DownloadVectorIcon(
                            tint = colors.textSecondary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Offline voice review available · Tap to get model (42 MB)",
                            color = colors.textSecondary,
                            style = TextStyle(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline,
                            ),
                        )
                    }
                }
            }

            // Secondary Row: Two tactile clay pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Mark Read
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .clayCard(
                            shape = RoundedCornerShape(10.dp),
                            backgroundColor = if (isDark) Color(0xFF24262E) else Color(0xFFEFE9DC),
                            elevation = 1.5.dp,
                            highlightColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.5f),
                            shadowColor = Color.Black.copy(alpha = 0.15f),
                        )
                        .clickable(onClick = onTap)
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = tapLabel(mode),
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                            ),
                        )
                    }
                }

                // Listen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .clayCard(
                            shape = RoundedCornerShape(10.dp),
                            backgroundColor = if (isDark) Color(0xFF24262E) else Color(0xFFEFE9DC),
                            elevation = 1.5.dp,
                            highlightColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.5f),
                            shadowColor = Color.Black.copy(alpha = 0.15f),
                        )
                        .clickable(onClick = onListen)
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        HeadphonesVectorIcon(
                            tint = colors.textPrimary,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = when (audio) {
                                is AudioState.Idle, is AudioState.Failed -> "Listen"
                                is AudioState.Fetching -> "Loading…"
                                is AudioState.Playing -> "Playing"
                                is AudioState.Paused -> "Paused"
                            },
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                            ),
                        )
                    }
                }
            }

            // Streak & reading info line - synchronized with home screen journey stats
            progress?.takeIf { it.totalDaysRead > 0 }?.let { p ->
                val aloudRatio = if (p.totalDaysRead > 0) (p.recitedDays * 100) / p.totalDaysRead else 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FlameVectorIcon(tint = Color(0xFFD35400), modifier = Modifier.size(13.dp))
                        Text(
                            text = if (p.currentStreak == 1) "1 Day Streak" else "${p.currentStreak} Days Streak",
                            style = TextStyle(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                            ),
                        )
                    }
                    Text(
                        text = " · ",
                        style = TextStyle(fontSize = 11.sp, color = colors.textSecondary.copy(alpha = 0.5f)),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CompanionBookVectorIcon(tint = colors.accent, modifier = Modifier.size(13.dp))
                        val totalStr = if (p.totalDaysRead == 1) "1 Total Day" else "${p.totalDaysRead} Total Days"
                        Text(
                            text = totalStr,
                            style = TextStyle(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                            ),
                        )
                    }
                    Text(
                        text = " · ",
                        style = TextStyle(fontSize = 11.sp, color = colors.textSecondary.copy(alpha = 0.5f)),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MicVectorIcon(tint = colors.accent, modifier = Modifier.size(13.dp))
                        Text(
                            text = "$aloudRatio% Aloud",
                            style = TextStyle(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlreadyDone(
    method: Method,
    hasRecording: Boolean,
    surahName: String,
    onPlay: () -> Unit,
    onUndo: () -> Unit,
    onCheck: (() -> Unit)?,
    checkState: CheckState,
    isModelReady: Boolean = false,
    onRequestModelDialog: () -> Unit = {},
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val emerald = if (isDark) Color(0xFF1E382D) else Color(0xFFE8F3EE)
    val emeraldText = if (isDark) Color(0xFF8ED676) else Color(0xFF245847)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = emerald,
                elevation = 3.dp,
                highlightColor = if (isDark) Color(0xFF335E4E).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f),
                shadowColor = Color.Black.copy(alpha = 0.2f),
                strokeWidth = 1.dp,
            )
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = emeraldText,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Today's Wird Completed!",
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = emeraldText,
                            ),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (method == Method.RECITED) {
                            if (surahName.isNotEmpty()) "Recited aloud · $surahName" else "Recited aloud today"
                        } else {
                            if (surahName.isNotEmpty()) "Marked as read · $surahName" else "Marked as read today"
                        },
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = emeraldText.copy(alpha = 0.85f),
                        ),
                    )
                }

                SparkleVectorIcon(
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(22.dp),
                )
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasRecording) {
                    Box(
                        modifier = Modifier
                            .clayPill(
                                shape = RoundedCornerShape(999.dp),
                                backgroundColor = if (isDark) Color(0xFF294E3E) else Color(0xFFD6EAE0),
                                elevation = 1.5.dp,
                            )
                            .clickable(onClick = onPlay)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Hear back",
                                tint = emeraldText,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Hear back",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = emeraldText,
                                ),
                            )
                        }
                    }
                }

                if (hasRecording) {
                    if (isModelReady && checkState !is CheckState.Working) {
                        Box(
                            modifier = Modifier
                                .clayPill(
                                    shape = RoundedCornerShape(999.dp),
                                    backgroundColor = if (isDark) Color(0xFF294E3E) else Color(0xFFD6EAE0),
                                    elevation = 1.5.dp,
                                )
                                .clickable(onClick = { onCheck?.invoke() })
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                SparkleVectorIcon(
                                    tint = emeraldText,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = if (checkState is CheckState.Idle) "AI Review" else "Review again",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = emeraldText,
                                    ),
                                )
                            }
                        }
                    } else if (!isModelReady && checkState !is CheckState.Working && checkState !is CheckState.DownloadingModel) {
                        Box(
                            modifier = Modifier
                                .clayPill(
                                    shape = RoundedCornerShape(999.dp),
                                    backgroundColor = if (isDark) Color(0xFF2D5C46) else Color(0xFFC7E5D4),
                                    elevation = 2.dp,
                                )
                                .clickable(onClick = onRequestModelDialog)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                DownloadVectorIcon(
                                    tint = emeraldText,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "Get Voice Reviewer (42 MB)",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = emeraldText,
                                    ),
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clayPill(
                            shape = RoundedCornerShape(999.dp),
                            backgroundColor = if (isDark) Color(0xFF26272E) else Color(0xFFEDE8DC),
                            elevation = 1.dp,
                        )
                        .clickable(onClick = onUndo)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = "Undo",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                        ),
                    )
                }
            }

            // Recitation check state display
            when (checkState) {
                is CheckState.Idle -> {
                    if (hasRecording && !isModelReady) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clayCard(
                                    shape = RoundedCornerShape(12.dp),
                                    backgroundColor = if (isDark) Color(0xFF162B21) else Color(0xFFDCEDE3),
                                    elevation = 1.dp,
                                )
                                .clickable(onClick = onRequestModelDialog)
                                .padding(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "✦ Review recitation with Whisper AI",
                                        color = emeraldText,
                                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Download on-device voice checker (42 MB) to check your words offline.",
                                        color = emeraldText.copy(alpha = 0.85f),
                                        style = TextStyle(fontSize = 11.5.sp),
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clayPill(
                                            backgroundColor = Color(0xFF2D6B52),
                                            elevation = 2.dp,
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = "DOWNLOAD",
                                        color = Color.White,
                                        style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                        }
                    }
                }
                is CheckState.NeedsModel -> {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clayCard(
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = if (isDark) Color(0xFF162B21) else Color(0xFFDCEDE3),
                                elevation = 1.dp,
                            )
                            .clickable(onClick = onRequestModelDialog)
                            .padding(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "✦ Voice checker model required",
                                    color = emeraldText,
                                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Download on-device Whisper AI (42 MB) to verify today's recording. Zero data used.",
                                    color = emeraldText.copy(alpha = 0.85f),
                                    style = TextStyle(fontSize = 11.5.sp),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clayPill(
                                        backgroundColor = Color(0xFF2D6B52),
                                        elevation = 2.dp,
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = "DOWNLOAD",
                                    color = Color.White,
                                    style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }
                is CheckState.DownloadingModel -> {
                    val done = checkState.doneBytes
                    val total = checkState.totalBytes
                    val pct = if (total > 0) (done * 100 / total).toInt() else 0
                    val mbDone = String.format(java.util.Locale.US, "%.1f", done.toFloat() / (1024 * 1024))
                    val mbTotal = String.format(java.util.Locale.US, "%.1f", total.toFloat() / (1024 * 1024))
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text(
                            text = "Downloading offline Whisper AI model… $pct% ($mbDone / $mbTotal MB)",
                            color = emeraldText,
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = emeraldText,
                            trackColor = emeraldText.copy(alpha = 0.2f),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Your recitation will be reviewed automatically once download finishes.",
                            color = emeraldText.copy(alpha = 0.75f),
                            style = TextStyle(fontSize = 11.sp),
                        )
                    }
                }
                is CheckState.Working -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "✦ Auditing recitation… ${checkState.seconds}s. Running offline on your phone.",
                        color = emeraldText,
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
                is CheckState.Heard -> {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clayCard(
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = if (isDark) Color(0xFF13281E) else Color(0xFFE2F0E7),
                                elevation = 2.dp,
                                strokeWidth = 1.dp,
                            )
                            .padding(14.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Header: "See what you recited" with audio playback button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    SparkleVectorIcon(
                                        tint = Color(0xFFD4AF37),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "See what you recited",
                                        style = TextStyle(
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = emeraldText,
                                        ),
                                    )
                                }

                                if (hasRecording) {
                                    Box(
                                        modifier = Modifier
                                            .clayPill(
                                                shape = RoundedCornerShape(999.dp),
                                                backgroundColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                                                elevation = 1.dp,
                                            )
                                            .clickable(onClick = onPlay)
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Hear your voice",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Text(
                                                text = "Hear audio",
                                                style = TextStyle(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }

                            // Transcribed Text Container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0xFF0C1B14) else Color.White)
                                    .padding(12.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "TRANSCRIPTION",
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
                                        ),
                                    )
                                    Text(
                                        text = checkState.text.ifEmpty { checkState.summary },
                                        style = TextStyle(
                                            fontSize = 14.5.sp,
                                            lineHeight = 22.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDark) Color(0xFFE4F2E7) else Color(0xFF143326),
                                        ),
                                    )
                                }
                            }

                            // Verdict / Audit details
                            if (checkState.summary.isNotEmpty() && checkState.summary != checkState.text) {
                                Text(
                                    text = checkState.summary,
                                    color = emeraldText,
                                    style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                                )
                            }

                            if (checkState.marked > 0) {
                                Text(
                                    text = if (checkState.marked == 1) "One ayah is marked on the page above for review."
                                    else "${checkState.marked} ayahs are marked on the page above for review.",
                                    color = Color(0xFFD97706),
                                    style = TextStyle(fontSize = Scale.caption, fontWeight = FontWeight.Bold),
                                )
                            }

                            Text(
                                text = "${checkState.seconds}s audio · verified in ${checkState.took}s with offline Whisper AI.",
                                color = emeraldText.copy(alpha = 0.75f),
                                style = TextStyle(fontSize = 10.5.sp),
                            )
                        }
                    }
                }
                is CheckState.Nothing -> {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = checkState.why,
                        color = emeraldText,
                        style = TextStyle(fontSize = Scale.caption),
                    )
                }
            }
        }
    }
}

/**
 * Where the recitation check has got to. **PLAN task 14.**
 *
 * A sealed set rather than a nullable string, because "not started", "running" and "found
 * nothing" are three different things to show and collapsing them is how a screen ends up
 * saying nothing at the moment it most needs to say something.
 */
sealed interface CheckState {
    data object Idle : CheckState
    /** [seconds] ticks while it runs, because a still screen and a hung screen look alike. */
    data class Working(val seconds: Int) : CheckState
    /** Model is currently downloading in the background. */
    data class DownloadingModel(val doneBytes: Long, val totalBytes: Long) : CheckState
    /** A recitation was recorded, but no offline model is installed to review it. */
    data object NeedsModel : CheckState
    /**
     * The comparison came back. **This is what he asked for**, and [text] is kept only for the
     * log and for the case where there was nothing to compare against.
     */
    data class Heard(
        val text: String,
        val seconds: Int,
        val took: String,
        /** What to tell him, already written by the domain. */
        val summary: String = "",
        /** Ayahs marked on the page. Empty when the check would not commit. */
        val marked: Int = 0,
    ) : CheckState
    data class Nothing(val why: String) : CheckState
}

/**
 * What the recording control is called.
 *
 * The design's own wording for the memorising case, kept verbatim: *"the same loop, pointed
 * at revision."* Reciting aloud is still how a day gets marked either way.
 */
internal fun reciteLabel(mode: ReadingMode): String = when (mode) {
    ReadingMode.READING -> "Recite it out loud"
    ReadingMode.MEMORISING -> "Recite from memory"
}

/**
 * What the tap route is called.
 *
 * Still plainly the quieter of the two, and still logged as a tap. Sacred Rule 6 turns on
 * the app never blurring these, so the memorising wording has to stay just as clearly *not*
 * a recitation.
 */
internal fun tapLabel(mode: ReadingMode): String = when (mode) {
    ReadingMode.READING -> "I read it"
    ReadingMode.MEMORISING -> "I revised it"
}

/** Crisp vector sparkle / 4-pointed star icon */
@Composable
private fun SparkleVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val star = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, h * 0.05f)
            quadraticTo(cx, cy, w * 0.95f, cy)
            quadraticTo(cx, cy, cx, h * 0.95f)
            quadraticTo(cx, cy, w * 0.05f, cy)
            quadraticTo(cx, cy, cx, h * 0.05f)
            close()
        }
        drawPath(star, color = tint, style = androidx.compose.ui.graphics.drawscope.Fill)
    }
}

/** Crisp vector download icon */
@Composable
private fun DownloadVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        // Down arrow stem
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.12f),
            end = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.60f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Arrowhead
        val arrow = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.28f, h * 0.40f)
            lineTo(w * 0.50f, h * 0.62f)
            lineTo(w * 0.72f, h * 0.40f)
        }
        drawPath(
            arrow,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
        // Bottom tray
        val tray = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.20f, h * 0.72f)
            lineTo(w * 0.20f, h * 0.88f)
            lineTo(w * 0.80f, h * 0.88f)
            lineTo(w * 0.80f, h * 0.72f)
        }
        drawPath(
            tray,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}

/** Crisp vector headphones icon */
@Composable
private fun HeadphonesVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        // Headband arc
        val arcPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.18f, h * 0.60f)
            cubicTo(
                w * 0.18f, h * 0.15f,
                w * 0.82f, h * 0.15f,
                w * 0.82f, h * 0.60f,
            )
        }
        drawPath(
            arcPath,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            ),
        )
        // Left and right earcups
        val leftCup = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(w * 0.12f, h * 0.52f, w * 0.26f, h * 0.85f),
                    radiusX = w * 0.07f,
                    radiusY = w * 0.07f,
                )
            )
        }
        val rightCup = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(w * 0.74f, h * 0.52f, w * 0.88f, h * 0.85f),
                    radiusX = w * 0.07f,
                    radiusY = w * 0.07f,
                )
            )
        }
        drawPath(leftCup, color = tint, style = androidx.compose.ui.graphics.drawscope.Fill)
        drawPath(rightCup, color = tint, style = androidx.compose.ui.graphics.drawscope.Fill)
    }
}

