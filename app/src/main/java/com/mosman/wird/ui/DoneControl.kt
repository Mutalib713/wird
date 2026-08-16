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
    hasRecording: Boolean,
    recording: Boolean,
    problem: String?,
    onStartRecording: () -> Unit,
    onMicRefused: () -> Unit,
    onTap: () -> Unit,
    onPlay: () -> Unit,
    onUndo: () -> Unit,
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
                onRecite = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) onStartRecording() else askMic.launch(Manifest.permission.RECORD_AUDIO)
                },
                onTap = onTap,
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
            Text("I read it", color = colors.textSecondary, style = TextStyle(fontSize = Scale.body))
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
