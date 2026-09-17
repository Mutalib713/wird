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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mosman.wird.audio.AudioState
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
) {
    val colors = LocalWirdColors.current
    val context = LocalContext.current

    val askMic = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) onStartRecording() else onMicRefused() }

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
                    Text(
                        text = "🎙",
                        style = TextStyle(fontSize = 16.sp),
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
                        Text(
                            text = "✓",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                            ),
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
                        Text(
                            text = "🎧",
                            style = TextStyle(fontSize = 13.sp),
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

            // Streak & reading info line
            progress?.takeIf { it.totalDaysRead > 0 }?.let { p ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (p.currentStreak > 1) {
                            "🔥 ${p.currentStreak} in a row · ${p.totalDaysRead} days read in total"
                        } else {
                            "📖 ${p.totalDaysRead} day read"
                        },
                        style = TextStyle(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary,
                        ),
                    )
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
                    Text(
                        text = "✓ Today's Wird Completed!",
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = emeraldText,
                        ),
                    )
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

                Text(
                    text = "🏆",
                    style = TextStyle(fontSize = 20.sp),
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
                        Text(
                            text = "▶ Hear back",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = emeraldText,
                            ),
                        )
                    }
                }

                if (hasRecording && onCheck != null && checkState !is CheckState.Working) {
                    Box(
                        modifier = Modifier
                            .clayPill(
                                shape = RoundedCornerShape(999.dp),
                                backgroundColor = if (isDark) Color(0xFF294E3E) else Color(0xFFD6EAE0),
                                elevation = 1.5.dp,
                            )
                            .clickable(onClick = onCheck)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = if (checkState is CheckState.Idle) "✦ AI Check" else "✦ Check again",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = emeraldText,
                            ),
                        )
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
                is CheckState.Idle -> Unit
                is CheckState.Working -> {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Listening back… ${checkState.seconds}s. It runs offline on this phone.",
                        color = emeraldText,
                        style = TextStyle(fontSize = Scale.caption),
                    )
                }
                is CheckState.Heard -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = checkState.summary.ifEmpty { checkState.text },
                        color = emeraldText,
                        style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                    )
                    if (checkState.marked > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (checkState.marked == 1) "One ayah is marked on the page above."
                            else "${checkState.marked} ayahs are marked on the page above.",
                            color = emeraldText,
                            style = TextStyle(fontSize = Scale.caption),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${checkState.seconds}s of audio, checked in ${checkState.took}s. It compares words, not tajweed.",
                        color = emeraldText.copy(alpha = 0.8f),
                        style = TextStyle(fontSize = Scale.caption),
                    )
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
