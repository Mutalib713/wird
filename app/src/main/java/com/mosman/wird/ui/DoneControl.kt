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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.core.content.ContextCompat
import com.mosman.wird.audio.Recitation
import com.mosman.wird.domain.Method
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import kotlinx.coroutines.delay
import java.io.File

/**
 * Marking the day done, at the foot of the page.
 *
 * Placed here on purpose: this is where you arrive when you have actually finished
 * reading, so the control is *earned* rather than parked somewhere convenient.
 *
 * Two ways, and the app never pretends they are the same. Reciting is the one that
 * cannot be done without doing it — you have to open your mouth, which is the whole
 * mechanism this app is a stand-in for. Tapping exists because he asked for it, and
 * because there are lecture halls, but every tap is recorded as a tap.
 */
@Composable
fun DoneControl(
    doneMethod: Method?,
    hasRecording: Boolean,
    audioFile: () -> File,
    onDone: (Method, File?) -> Unit,
    onUndo: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val context = LocalContext.current
    val recitation = remember { Recitation(context) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var problem by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { recitation.release() } }

    LaunchedEffect(recording) {
        seconds = 0
        while (recording) {
            delay(1_000)
            seconds++
        }
    }

    fun beginRecording() {
        problem = null
        if (recitation.start(audioFile())) {
            recording = true
        } else {
            problem = "The microphone didn't start. Try again."
        }
    }

    val askMic = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginRecording()
        } else {
            problem = "Wird needs the microphone to hear you recite. You can still mark it read."
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = Scale.space6)) {
        when {
            recording -> Recording(
                seconds = seconds,
                onStop = {
                    recording = false
                    val file = recitation.stop()
                    if (file == null) {
                        problem = "That was too short to keep. Nothing was saved."
                    } else {
                        onDone(Method.RECITED, file)
                    }
                },
                onCancel = {
                    recording = false
                    recitation.cancel()
                },
            )

            doneMethod != null -> AlreadyDone(
                method = doneMethod,
                hasRecording = hasRecording,
                onPlay = { recitation.play(audioFile()) },
                onUndo = onUndo,
            )

            else -> NotYet(
                onRecite = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) beginRecording() else askMic.launch(Manifest.permission.RECORD_AUDIO)
                },
                onTap = { onDone(Method.TAPPED, null) },
            )
        }

        problem?.let {
            Spacer(Modifier.height(Scale.space2))
            Text(it, color = colors.textSecondary, style = TextStyle(fontSize = Scale.caption))
        }
    }
}

@Composable
private fun NotYet(onRecite: () -> Unit, onTap: () -> Unit) {
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
            Text("Recite it out loud", style = TextStyle(fontSize = Scale.body))
        }
        TextButton(
            onClick = onTap,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Scale.minTarget),
        ) {
            // Quieter, and honest about what it is. Not "done" — read.
            Text(
                "I read it",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.body),
            )
        }
    }
}

@Composable
private fun Recording(seconds: Int, onStop: () -> Unit, onCancel: () -> Unit) {
    val colors = LocalWirdColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Listening. ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.body),
        )
        Spacer(Modifier.height(Scale.space2))
        Row(horizontalArrangement = Arrangement.spacedBy(Scale.space2)) {
            Button(
                onClick = onStop,
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.surface,
                ),
            ) {
                Text("Done reciting", style = TextStyle(fontSize = Scale.body))
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
            ) {
                Text("Throw it away", color = colors.textSecondary)
            }
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
