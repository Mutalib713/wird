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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import com.mosman.wird.audio.AudioQuality
import com.mosman.wird.audio.AudioState
import com.mosman.wird.audio.PortionAudio
import com.mosman.wird.audio.Recitation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

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
    /** A page to open, set when a surah is picked from the Sūrahs tab. Consumed once. */
    openPage: Int? = null,
    onOpenPageHandled: () -> Unit = {},
    /** False until the reader has been shown, once, that the page is tappable. */
    hasSeenChrome: Boolean = true,
    onChromeSeen: () -> Unit = {},
    /** How today was marked, or null if it has not been. */
    doneMethod: com.mosman.wird.domain.Method? = null,
    progress: com.mosman.wird.domain.Progress? = null,
    hasRecording: Boolean = false,
    audioFile: () -> java.io.File = { java.io.File("") },
    /** Which Shatri recording to fetch. The reader's data, so the reader's choice. */
    audioQuality: AudioQuality = AudioQuality.LIGHT,
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
    var companionReply by remember { mutableStateOf<String?>(null) }

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

    // A pick from the Sūrahs tab arrives as a page number rather than a navigation event,
    // because the pager owns where it is. Consumed once, so returning to the tab later does
    // not silently jump you again.
    LaunchedEffect(openPage) {
        val p = openPage ?: return@LaunchedEffect
        jumpCount++
        jump = PageJump(p, jumpCount)
        onOpenPageHandled()
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

    // ---- Abu Bakr al-Shatri reciting today's portion (task 9) ----
    //
    // PROFILE.md § 4 calls this "the lazy-day escape hatch, so the ask can drop to *just
    // listen*", which is why it is reachable from the chrome bar rather than only from the
    // foot: on a day reading is not going to happen, the way out must not be at the bottom
    // of the thing you are avoiding.
    //
    // **Listening deliberately marks nothing.** Sacred Rule 6 says the app never lets you
    // believe you did more than you did, and there is no LISTENED method in the day log —
    // adding a third category is Mutalib's call, not a side effect of building playback.
    val portionAudio = remember { PortionAudio(context) }
    var audio by remember { mutableStateOf<AudioState>(AudioState.Idle) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) { onDispose { portionAudio.release() } }

    fun stopListening() {
        portionAudio.stop()
        audio = AudioState.Idle
    }

    fun listen() {
        if (audio !is AudioState.Idle) { stopListening(); return }
        scope.launch {
            audio = AudioState.Fetching(0, 0)
            val repo = MushafRepository(context)

            // Only the ayahs actually lit. A half-page portion must not fetch — or recite
            // — the half you were not asked to read.
            val verses = todaysPages.flatMap { p ->
                val layout = repo.layoutOnly(p) ?: return@flatMap emptyList()
                val lit = litFor(layout)
                layout.glyphs.filter { it.line in lit }.map { it.verseKey }
            }.distinct()

            if (verses.isEmpty()) {
                audio = AudioState.Failed("Couldn't work out which ayahs to play.")
                return@launch
            }

            val files = portionAudio.ensureCached(verses, audioQuality) { done, total ->
                audio = AudioState.Fetching(done, total)
            }
            if (files == null) {
                // Null also means "you pressed stop while this was downloading", and that
                // is not an error to report back at someone. stopListening() has already
                // set Idle, so only a fetch still believing it is running gets to fail.
                if (audio is AudioState.Fetching) {
                    audio = AudioState.Failed("Couldn't get the recitation. Check your connection.")
                }
                return@launch
            }
            portionAudio.play(
                files = files,
                verses = verses,
                onVerse = { i -> audio = AudioState.Playing(verses[i], i, verses.size) },
                onFinished = { audio = AudioState.Idle },
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.fillMaxSize()) {
        // The companion sits ABOVE the page, and only while the day is unfinished.
        //
        // Home is page-first (PROFILE § 5f) so this must not become a dashboard in front
        // of the reading. Two things keep it honest: it is a band rather than a screen,
        // and **it disappears the moment the day is marked** — once you have read, there
        // is nothing left to ask, and the page gets its full height back.
        if (doneMethod == null && !recording) {
            Companion(
                question = companionQuestion(),
                shortcuts = listOf("After Isha", "In an hour", "Not today"),
                lastReply = companionReply,
                onReply = { said ->
                    // Recorded, not yet acted on. Turning "after Isha" into a real alarm
                    // is PLAN task 21, and task 8's scheduler already does the hard half.
                    // Saying it back is not a stub: it is the difference between something
                    // that heard you and a box that swallowed your text.
                    companionReply = "You said: $said"
                },
                modifier = Modifier.safeDrawingPadding().padding(
                    horizontal = Scale.space4,
                    vertical = Scale.space2,
                ),
            )
        }
        Box(modifier = Modifier.weight(1f)) {
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
            // Only while it is actually playing. A download in progress marks nothing —
            // the page should not start rearranging itself before you hear anything.
            reciting = (audio as? AudioState.Playing)?.verseKey,
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
                        audio = audio,
                        onListen = { listen() },
                    )
                    // Under the done control, where you land having finished. Two numbers
                    // that never appear apart. Sacred Rules 4 and 6.
                    progress?.let { ProgressLine(it) }
                }
            },
        )

        }
      }

        // The listening glow, at the edges where it cannot cover the page.
        RecitationGlow(active = recording, level = level)

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
                progress = progress,
                offToday = current !in todaysPages,
                onBackToToday = {
                    jumpCount++
                    jump = PageJump(todaysPages.first(), jumpCount)
                    chromeShown = false
                },
                onJump = { showJump = true; chromeShown = false },
                onSettings = { chromeShown = false; onSettings() },
                audio = audio,
                // The bar stays up while it plays. It is the only stop control, and a
                // stop button that vanishes the moment you use it is how you end up
                // tapping the page trying to find it again.
                onListen = { listen() },
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
    progress: com.mosman.wird.domain.Progress?,
    offToday: Boolean,
    onBackToToday: () -> Unit,
    onJump: () -> Unit,
    onSettings: () -> Unit,
    audio: AudioState,
    onListen: () -> Unit,
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
            // How it is going, reachable from anywhere with one tap rather than only at
            // the foot of the page. Mutalib could not find it: it existed, but it lived
            // somewhere you only reach by finishing, which is the wrong place for the
            // number that is supposed to keep you going.
            progress?.takeIf { it.totalDaysRead > 0 }?.let { p ->
                Text(
                    text = streakLine(p),
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }
            // Downloading or playing replaces the streak line rather than adding a fourth,
            // because the bar is already three lines deep and this is temporary.
            audioLine(audio)?.let {
                Text(
                    text = it,
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }
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

        // Turns into a stop while it is running, in place, so the control that started it
        // is the control that ends it.
        val playing = audio !is AudioState.Idle && audio !is AudioState.Failed
        BarIcon(
            icon = if (playing) Icons.Filled.Close else Icons.Filled.PlayArrow,
            label = if (playing) "Stop the recitation" else "Listen to today's portion",
            onClick = onListen,
        )
        BarIcon(Icons.AutoMirrored.Filled.List, "Read something else", onJump)
        BarIcon(Icons.Filled.Settings, "Settings", onSettings)
    }
}

/** Same rules as the foot of the page: never the streak alone, never a nought. */
private fun streakLine(p: com.mosman.wird.domain.Progress): String {
    val total = if (p.totalDaysRead == 1) "1 day read" else "${p.totalDaysRead} days read"
    return if (p.currentStreak <= 1) total else "${p.currentStreak} in a row, $total"
}

/**
 * What the recitation is doing, or null when it is doing nothing.
 *
 * The download says how far along it is because on Ghanaian mobile data a page of audio
 * is a couple of megabytes and a silent wait reads as a hang. Playback names the ayah,
 * which is the one thing that makes a list of MP3s feel like a recitation.
 */
private fun audioLine(audio: AudioState): String? = when (audio) {
    is AudioState.Idle -> null
    is AudioState.Fetching ->
        if (audio.total == 0) "Getting the recitation…"
        else "Getting the recitation, ${audio.done} of ${audio.total}"
    is AudioState.Playing -> {
        // "Ya-Sin 28", not "36:28". The reader knows the surah by name — that is the
        // question setup asks them, and it is how the header names the portion.
        val surah = audio.verseKey.substringBefore(':').toIntOrNull()
            ?.let { com.mosman.wird.domain.SurahIndex.byNumber(it)?.name }
        val ayah = audio.verseKey.substringAfter(':')
        val where = if (surah != null) "$surah $ayah" else audio.verseKey
        "Playing $where · ${audio.index + 1} of ${audio.total}"
    }
    is AudioState.Failed -> audio.reason
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

/**
 * What the companion asks, phrased for the time of day.
 *
 * Sacred Rule 3 is the whole constraint here: it asks whether you are reading, never
 * whether you have failed to. "Still reading today?" in the evening is a question; "you
 * haven't read yet" is the same fact turned into an accusation.
 */
private fun companionQuestion(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "Reading this morning?"
        hour < 17 -> "Reading today?"
        else -> "Are you reading tonight?"
    }
}
