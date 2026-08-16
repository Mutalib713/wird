package com.mosman.wird.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import com.mosman.wird.audio.Recitation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.mosman.wird.domain.Assignment
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.linesOn
import com.mosman.wird.domain.pages
import com.mosman.wird.mushaf.MushafRepository
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import kotlinx.coroutines.delay

/** Long enough to notice and read, short enough not to feel like a splash screen. */
private const val FIRST_RUN_CHROME_MS = 3_500L

/**
 * Today's portion, and the mushaf around it.
 *
 * The portion is the front door: the app opens here, on this page, marked. Sacred Rule 5.
 *
 * **The page carries no permanent chrome.** Tap it and a slim bar appears with the ways
 * out — settings, another surah, back to today; tap again and it goes. That is what every
 * serious reading app does, and it is the only arrangement that survives more controls
 * being added: the links used to sit at the foot of the page, which meant scrolling to the
 * end of the Qur'an to reach settings. Fine for one link, wrong for four, and task 6 adds
 * the most important one of all.
 */
@Composable
fun TodayScreen(
    assignment: Assignment,
    /** Where the reader said they were, when that is partway down the first page. */
    startVerse: Pair<Int, Int>? = null,
    onSettings: () -> Unit = {},
    /** False until the reader has been shown, once, that the page is tappable. */
    hasSeenChrome: Boolean = true,
    onChromeSeen: () -> Unit = {},
    /** How today was marked, or null if it has not been. */
    doneMethod: com.mosman.wird.domain.Method? = null,
    hasRecording: Boolean = false,
    audioFile: () -> java.io.File = { java.io.File("") },
    onDone: (com.mosman.wird.domain.Method, java.io.File?) -> Unit = { _, _ -> },
    onUndo: () -> Unit = {},
) {
    val colors = LocalWirdColors.current
    val todaysPages = remember(assignment) { assignment.pages }
    var showJump by remember { mutableStateOf(false) }
    var current by remember { mutableIntStateOf(todaysPages.first()) }
    var jump by remember { mutableStateOf<PageJump?>(null) }
    var jumpCount by remember { mutableIntStateOf(0) }

    // Keyed by page, not stored as "the current one".
    //
    // The first version wrote the surah only when the drawn page matched `current`, and
    // during a swipe those two are briefly out of step, so the update was dropped and the
    // name never changed. Remembering every page it has drawn removes the race entirely.
    val pageInfo = remember { mutableStateMapOf<Int, Pair<String, Int>>() }

    // Open with the bar showing the very first time, then let it go.
    //
    // Nothing on a clean page announces that it is tappable. Mutalib asked whether this
    // should be a hamburger icon; it would announce itself, but at the price of a mark
    // parked on the Qur'an forever. Showing the bar once and withdrawing it teaches the
    // same gesture and leaves the page alone afterwards.
    var chromeShown by remember { mutableStateOf(!hasSeenChrome) }

    // Recording lives up here, not in the footer control. The record button is at the
    // foot of the page, so the moment you start you scroll up to read — and anything down
    // there goes out of sight. Mutalib started a recitation and could not tell it was on.
    val context0 = LocalContext.current
    val recitation = remember { Recitation(context0) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var level by remember { mutableFloatStateOf(0f) }
    var problem by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { recitation.release() } }

    LaunchedEffect(recording) {
        seconds = 0
        level = 0f
        var ticks = 0
        while (recording) {
            // Poll faster than the clock so the meter follows a voice rather than a
            // second hand. getMaxAmplitude reports the peak since the last call.
            delay(80)
            level = recitation.level()
            if (++ticks % 12 == 0) seconds++
        }
        level = 0f
    }

    LaunchedEffect(hasSeenChrome) {
        if (hasSeenChrome) return@LaunchedEffect
        delay(FIRST_RUN_CHROME_MS)
        chromeShown = false
        onChromeSeen()
    }

    // The one rule about what is lit, defined once so the bar and the page agree.
    val litFor: (com.mosman.wird.mushaf.MushafPage) -> Set<Int> = remember(assignment, startVerse, todaysPages) {
        { page ->
            // dark means today, pale means not today — on every page, no exceptions.
            if (page.page !in todaysPages) {
                emptySet()
            } else {
                val byPage = assignment.linesOn(page.page, page.lines)
                val startLine = startVerse?.let { (s, a) -> page.lineOf(s, a) }
                if (startLine == null) byPage else byPage.filter { it >= startLine }.toSet()
            }
        }
    }

    // Warm one page either side of today's, once, in the background.
    val context = LocalContext.current
    LaunchedEffect(todaysPages) {
        MushafRepository(context).prefetchAround(todaysPages.first())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MushafPager(
            initialPage = todaysPages.first(),
            jump = jump,
            modifier = Modifier.safeDrawingPadding(),
            onPageChanged = { current = it },
            onBackgroundTap = { chromeShown = !chromeShown },
            // Name the page the same way the page names itself. Using page.surahName
            // here is what put "Fatir" in the bar while the page said "Ya-Sin 4-12".
            onPageShown = { page ->
                pageInfo[page.page] = surahLabelFor(page, litFor(page)) to page.juz
            },
            lit = litFor,
            footer = { page ->
                // Only under today's reading. On a page you are browsing there is nothing
                // to finish, and a "done" button there would be marking the wrong thing.
                if (page.page == todaysPages.last()) {
                    DoneControl(
                        doneMethod = doneMethod,
                        hasRecording = hasRecording,
                        recording = recording,
                        problem = problem,
                        onStartRecording = {
                            problem = null
                            if (recitation.start(audioFile())) {
                                recording = true
                            } else {
                                problem = "The microphone didn't start. Try again."
                            }
                        },
                        onMicRefused = {
                            problem = "Wird needs the microphone to hear you recite. " +
                                "You can still mark it read."
                        },
                        onTap = { onDone(com.mosman.wird.domain.Method.TAPPED, null) },
                        onPlay = { recitation.play(audioFile()) },
                        onUndo = onUndo,
                    )
                }
            },
        )

        AnimatedVisibility(
            visible = recording,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            RecordingBar(
                seconds = seconds,
                level = level,
                onStop = {
                    recording = false
                    val file = recitation.stop()
                    if (file == null) {
                        problem = "That was too short to keep. Nothing was saved."
                    } else {
                        onDone(com.mosman.wird.domain.Method.RECITED, file)
                    }
                },
                onCancel = {
                    recording = false
                    recitation.cancel()
                },
            )
        }

        AnimatedVisibility(
            visible = chromeShown && !recording,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            ChromeBar(
                surah = pageInfo[current]?.first.orEmpty(),
                page = current,
                juz = pageInfo[current]?.second ?: 0,
                offToday = current !in todaysPages,
                onBackToToday = {
                    jumpCount++
                    jump = PageJump(todaysPages.first(), jumpCount)
                    chromeShown = false
                },
                onJump = { showJump = true; chromeShown = false },
                onSettings = { chromeShown = false; onSettings() },
            )
        }
    }

    if (showJump) {
        SurahJumpSheet(
            onDismiss = { showJump = false },
            onPick = { surah: Surah ->
                jumpCount++
                jump = PageJump(surah.firstPage, jumpCount)
                showJump = false
            },
        )
    }
}

/**
 * Where you are, and the ways out. Hidden until asked for; closes once you have chosen.
 *
 * Shaped after the app Mutalib actually reads in: the place you are on the left as a name
 * and a page, the actions as icons on the right. Icons rather than words *here* because
 * the bar now has a title to carry — two lines of prose plus two more of buttons would be
 * a paragraph sitting on a Qur'an page.
 *
 * "Today's portion" keeps its words. It is the one action that needs explaining, it only
 * appears when you have wandered off, and no glyph means "back to the bit you were
 * supposed to read".
 */
@Composable
private fun ChromeBar(
    surah: String,
    page: Int,
    juz: Int,
    offToday: Boolean,
    onBackToToday: () -> Unit,
    onJump: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised)
            .safeDrawingPadding()
            .padding(horizontal = Scale.space4, vertical = Scale.space2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = surah,
                color = colors.onSurfaceRaised,
                style = TextStyle(fontSize = Scale.body),
            )
            Text(
                text = if (juz > 0) "Page $page, Juz' $juz" else "Page $page",
                color = colors.onSurfaceRaised,
                style = TextStyle(fontSize = Scale.caption),
            )
        }

        if (offToday) {
            TextButton(
                onClick = onBackToToday,
                modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
            ) {
                Text(
                    text = "Today's portion",
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }
        }

        BarIcon(Icons.AutoMirrored.Filled.List, "Read something else", onJump)
        BarIcon(Icons.Filled.Settings, "Settings", onSettings)
    }
}

@Composable
private fun BarIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    IconButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minWidth = Scale.minTarget, minHeight = Scale.minTarget),
    ) {
        // The label is the screen-reader name. An icon with no name is a button nobody
        // using TalkBack can identify.
        Icon(imageVector = icon, contentDescription = label, tint = colors.onSurfaceRaised)
    }
}

// No hand-written juz table lives here. The first draft of this file had one — thirty page
// numbers typed from memory — which is precisely the kind of Qur'anic metadata this
// project refuses to guess at. The API reports `juz_number` for every page and
// MushafPage already carries it, so the bar reads the page it is actually showing.
