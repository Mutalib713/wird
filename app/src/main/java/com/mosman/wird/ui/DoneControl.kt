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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.core.content.ContextCompat
import com.mosman.wird.audio.AudioState
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.domain.Method
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

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
    /**
     * Changes what the two controls are called, and nothing else.
     *
     * PROFILE.md § 5r. A memoriser pressing "Recite it out loud" while deliberately not
     * looking at the page is being described wrongly by their own app. The mechanic is
     * untouched: a recording is still a recitation, a tap is still a tap, and Sacred Rule 6
     * still keeps them apart.
     */
    mode: ReadingMode = ReadingMode.READING,
    hasRecording: Boolean,
    recording: Boolean,
    problem: String?,
    onStartRecording: () -> Unit,
    onMicRefused: () -> Unit,
    onTap: () -> Unit,
    onPlay: () -> Unit,
    onUndo: () -> Unit,
    audio: AudioState = AudioState.Idle,
    onListen: () -> Unit = {},
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

            doneMethod != null -> AlreadyDone(doneMethod, hasRecording, onPlay, onUndo)

            else -> NotYet(
                mode = mode,
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
    onRecite: () -> Unit,
    onTap: () -> Unit,
    audio: AudioState,
    onListen: () -> Unit,
) {
    val colors = LocalWirdColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onRecite,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Scale.minTarget),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.surface,
            ),
        ) {
            Text(reciteLabel(mode), style = TextStyle(fontSize = Scale.body))
        }
        TextButton(
            onClick = onTap,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Scale.minTarget),
        ) {
            // Quieter, and honest about what it is. Not "done" — read.
            Text(tapLabel(mode), color = colors.textSecondary, style = TextStyle(fontSize = Scale.body))
        }
        // Quietest of the three, and deliberately not a way of finishing.
        //
        // PROFILE.md § 4 wants the ask to be able to drop to "just listen" on a bad day,
        // which is why it is here at all. But hearing someone else recite is not the same
        // as reading, and Sacred Rule 6 turns on the app never blurring that — so this
        // plays, and you still choose one of the two above afterwards.
        TextButton(
            onClick = onListen,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Scale.minTarget),
        ) {
            Text(
                text = when (audio) {
                    is AudioState.Idle, is AudioState.Failed -> "Listen to it instead"
                    is AudioState.Fetching -> "Getting the recitation…"
                    is AudioState.Playing -> "Stop"
                },
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
            )
        }
        if (audio is AudioState.Failed) {
            Text(
                text = audio.reason,
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
            )
        }
    }
}

@Composable
private fun AlreadyDone(
    method: Method,
    hasRecording: Boolean,
    onPlay: () -> Unit,
    onUndo: () -> Unit,
) {
    val colors = LocalWirdColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Scale.radius))
            .background(colors.done)
            .padding(Scale.space4),
    ) {
        // Says which one. Sacred Rule 6 — the app never lets you believe you recited when
        // you tapped.
        Text(
            text = if (method == Method.RECITED) "Recited today" else "Marked as read today",
            color = colors.onSurfaceRaised,
            style = TextStyle(fontSize = Scale.body),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Scale.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasRecording) {
                TextButton(
                    onClick = onPlay,
                    modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
                ) {
                    Text("Hear it back", color = colors.onSurfaceRaised)
                }
            }
            TextButton(
                onClick = onUndo,
                modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
            ) {
                Text("Undo", color = colors.onSurfaceRaised)
            }
        }
    }
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
