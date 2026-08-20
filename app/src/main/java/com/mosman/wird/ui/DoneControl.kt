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
    /**
     * Runs the recitation check. **Null when this phone cannot** — no model downloaded, or no
     * native library — and then no button appears at all rather than one that does nothing.
     */
    onCheck: (() -> Unit)? = null,
    /** What the check is doing or found. PLAN task 14. */
    checkState: CheckState = CheckState.Idle,
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

            doneMethod != null ->
                AlreadyDone(doneMethod, hasRecording, onPlay, onUndo, onCheck, checkState)

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
                // ⚠ **It stopped saying "Stop" on 2026-08-19**, and the compiler is what
                // asked the question: adding a Paused state made this `when` inexhaustive and
                // forced a decision here. Stopping now belongs to the ListenBar, which has a
                // real control for it, so one button no longer means two things depending on
                // what it is already doing.
                text = when (audio) {
                    is AudioState.Idle, is AudioState.Failed -> "Listen to it instead"
                    is AudioState.Fetching -> "Getting the recitation…"
                    is AudioState.Playing -> "Playing"
                    is AudioState.Paused -> "Paused"
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
    onCheck: (() -> Unit)?,
    checkState: CheckState,
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
            // **His report, 2026-08-20: "I recorded but I didn't see anything."** He was right.
            // The transcription existed, ran in the background and wrote to the log, where a
            // reader has no way of ever seeing it. A feature nobody can observe is one nobody
            // can trust, and it also meant the measurement PLAN task 14 needs could only be
            // taken by plugging the phone into a laptop.
            if (hasRecording && onCheck != null && checkState !is CheckState.Working) {
                TextButton(
                    onClick = onCheck,
                    modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
                ) {
                    Text(
                        text = if (checkState is CheckState.Idle) "Check it" else "Check again",
                        color = colors.onSurfaceRaised,
                    )
                }
            }
            TextButton(
                onClick = onUndo,
                modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
            ) {
                Text("Undo", color = colors.onSurfaceRaised)
            }
        }

        // ---- what it heard ----
        //
        // ⚠ **Shown as "what the phone heard", never as a mark.** Sacred Rule 6: the recording
        // is what makes the day recited, and this is evidence about the recording. Nothing here
        // can change the day, and the wording must never imply it did.
        when (checkState) {
            is CheckState.Idle -> Unit

            is CheckState.Working -> {
                Spacer(Modifier.height(Scale.space2))
                Text(
                    text = "Listening back… this takes a few seconds.",
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }

            is CheckState.Heard -> {
                Spacer(Modifier.height(Scale.space3))
                Text(
                    text = "WHAT THE PHONE HEARD",
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = 10.sp, letterSpacing = 1.2.sp),
                )
                Spacer(Modifier.height(Scale.space1))
                Text(
                    text = checkState.text,
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = 17.sp, lineHeight = 28.sp),
                )
                Spacer(Modifier.height(Scale.space2))
                Text(
                    // The honest ceiling, in the same breath as the result. It is a
                    // transcription, not a judgement, and § 5h's rule about never sounding
                    // like a scholar applies to this screen too.
                    text = "${checkState.seconds}s of audio in ${checkState.took}s. " +
                        "This is what it made out, not a mark.",
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }

            is CheckState.Nothing -> {
                Spacer(Modifier.height(Scale.space2))
                Text(
                    text = checkState.why,
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = Scale.caption),
                )
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
    data object Working : CheckState
    data class Heard(val text: String, val seconds: Int, val took: String) : CheckState
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
