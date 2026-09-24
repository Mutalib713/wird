package com.mosman.wird.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mosman.wird.ui.theme.SetStatusBarAppearance
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import com.mosman.wird.audio.AudioQuality
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.data.ThemeMode
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.mosman.wird.domain.Assignment
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.HeardResult
import com.mosman.wird.domain.heardLabel
import com.mosman.wird.domain.judgeRecitation
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.linesOn
import com.mosman.wird.domain.versesOn
import com.mosman.wird.domain.pages
import com.mosman.wird.mushaf.MushafRepository
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import com.mosman.wird.ui.theme.WirdTheme
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
    /** Runs the recitation check on today's recording. Null when this phone cannot. */
    onCheckRecitation: (() -> Unit)? = null,
    /** What that check is doing or found. PLAN task 14. */
    checkState: CheckState = CheckState.Idle,
    /** Ayahs the recitation check could not follow, painted on the page. PLAN task 14. */
    reviewVerses: Set<String> = emptySet(),
    audioFile: () -> java.io.File = { java.io.File("") },
    /** Which Shatri recording to fetch. The reader's data, so the reader's choice. */
    audioQuality: AudioQuality = AudioQuality.LIGHT,
    readingMode: ReadingMode = ReadingMode.READING,
    /** Whether the app is painting dark right now. Drives the overflow's checkbox. */
    dark: Boolean = true,
    /** Flips light/dark from the page itself, which is where it is wanted. */
    onNightMode: () -> Unit = {},
    /** Leaves the mushaf. Also what the system back gesture does here. */
    onBack: () -> Unit = {},
    /** Whether a given ayah is saved. Asked per selection, not held as a list. */
    isBookmarked: (String) -> Boolean = { false },
    onToggleBookmark: (String) -> Unit = {},
    onPageVisited: (Int) -> Unit = {},
    onDone: (com.mosman.wird.domain.Method, java.io.File?) -> Unit = { _, _ -> },
    onUndo: () -> Unit = {},
    /** Whether this is the dedicated Today's Wird reader or the general Sūrah reader. */
    isWirdSession: Boolean = true,
    /** Navigates to the Sūrahs tab from the Wird hamburger menu. */
    onBrowseSurahs: (() -> Unit)? = null,
) {
    val colors = LocalWirdColors.current
    val todaysPages = remember(assignment) { assignment.pages }
    var showJump by remember { mutableStateOf(false) }
    val initialP = remember { openPage ?: if (isWirdSession) todaysPages.first() else 1 }
    var current by remember { mutableIntStateOf(initialP) }
    var jump by remember { mutableStateOf<PageJump?>(null) }
    var jumpCount by remember { mutableIntStateOf(0) }
    val allowedRange = remember(isWirdSession, todaysPages) {
        if (isWirdSession) (todaysPages.first() - 2).coerceAtLeast(1) .. (todaysPages.last() + 2).coerceAtMost(Mushaf.PAGES)
        else null
    }
    val offToday = isWirdSession && (current !in todaysPages)

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
    /** The ayah long-pressed, if any. Drives the wash and the floating toolbar. */
    var selectedVerse by remember { mutableStateOf<String?>(null) }
    /** Bumped on every bookmark toggle, so the icon re-reads the store. */
    var bookmarkTick by remember { mutableIntStateOf(0) }
    /**
     * Reading the translation instead of the mushaf. PROFILE.md section 5z.
     *
     * Not persisted on purpose: the mushaf is what Wird is for, so a session that ends in
     * translation mode should not open there tomorrow.
     */
    var translationMode by remember { mutableStateOf(false) }
    /** Which translation sources to display. Defaults to all bundled translations. */
    var selectedTranslationSources by remember {
        mutableStateOf(com.mosman.wird.data.TranslationSource.entries.toList())
    }
    val context0 = LocalContext.current
    val store = remember(context0) { com.mosman.wird.data.WirdStore(context0) }
    var showAyahBeforeTranslation by remember { mutableStateOf(store.ayahBeforeTranslation) }
    var showTranslationDialog by remember { mutableStateOf(false) }

    // Pressing back while in translation view smoothly returns to mushaf view
    BackHandler(enabled = translationMode) {
        translationMode = false
    }

    // Recording lives up here, not in the footer control. The record button is at the
    // foot of the page, so the moment you start you scroll up to read — and anything down
    // there goes out of sight. Mutalib started a recitation and could not tell it was on.
    val recitation = remember { Recitation(context0) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    // One loudness sample per second of recording. PLAN task 14: this is what decides whether
    // a recording sounds like a recitation at all, and it costs nothing to collect because
    // MediaRecorder is already counting it.
    var levels by remember { mutableStateOf<List<Int>>(emptyList()) }
    var heard by remember { mutableStateOf<HeardResult?>(null) }
    var level by remember { mutableFloatStateOf(0f) }
    var problem by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { recitation.release() } }

    LaunchedEffect(recording) {
        seconds = 0
        level = 0f
        levels = emptyList()
        var ticks = 0
        var peakThisSecond = 0f
        while (recording) {
            // Poll faster than the clock so the meter follows a voice rather than a
            // second hand. getMaxAmplitude reports the peak since the last call.
            delay(80)
            level = recitation.level()

            // ⚠ **The same reading feeds the meter and the check.** getMaxAmplitude resets
            // itself when read, so a second caller would see a fraction of the sound and both
            // would conclude the room was quiet. PLAN task 14's samples are therefore taken
            // from the value the meter already has, never from a second call.
            peakThisSecond = maxOf(peakThisSecond, level)
            if (++ticks % 12 == 0) {
                seconds++
                // Back to the raw 0..32767 the domain works in, undoing the meter's square
                // root. Judging in the API's own units keeps the thresholds meaningful to
                // anyone reading them against Android's documentation.
                levels = levels + (peakThisSecond * peakThisSecond * 32_767f).toInt()
                peakThisSecond = 0f
            }
        }
        level = 0f
    }

    // The verdict is a note beside the day, not a gate on it. Cleared when it is read.
    LaunchedEffect(heard) {
        val h = heard ?: return@LaunchedEffect
        problem = heardLabel(h)
        android.util.Log.i("WirdHeard", "recitation: ${h.verdict} ${h.spokenSeconds}/${h.totalSeconds}s share=${h.spokenShare}")
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
    val litFor: (com.mosman.wird.mushaf.MushafPage) -> Set<String> = remember(assignment, startVerse, todaysPages) {
        { page ->
            // dark means today, pale means not today — on every page, no exceptions.
            if (page.page !in todaysPages) {
                emptySet()
            } else {
                assignment.versesOn(page, startVerse)
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
    var selectedReciter by remember { mutableStateOf(store.selectedReciter) }
    var showReciterPicker by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { portionAudio.release() } }

    fun stopListening() {
        portionAudio.stop()
        audio = AudioState.Idle
    }

    // The repeat count lives here rather than in the player, because the bar has to draw it
    // and Compose only redraws what it can see change.
    var repeatEach by remember { mutableIntStateOf(1) }
    var rangeRepeat by remember { mutableIntStateOf(1) }
    var showRangePlayDialog by remember { mutableStateOf(false) }
    var rangePlayStartVerse by remember { mutableStateOf<String?>(null) }

    // What is loaded right now, so tapping a second ayah can jump rather than refetch.
    var loaded by remember { mutableStateOf<List<String>>(emptyList()) }

    /**
     * Start listening, optionally **at a particular ayah**.
     *
     * ⚠ **[startAt] exists because tapping Play on an ayah started the whole portion from the
     * top** — his report, 2026-08-19, and he is right that it is the opposite of what tapping
     * *that* ayah means.
     *
     * Three cases, in the order they are cheap:
     *  1. Already playing and the ayah is loaded — jump, no network, no delay.
     *  2. An ayah outside today's portion — play **just that one**, because he asked for that
     *     ayah and the surrounding ones were never today's reading.
     *  3. Otherwise — fetch the portion and begin at that ayah.
     */
    var pendingListenAyahs by remember { mutableStateOf<List<String>?>(null) }
    var pendingListenStartAt by remember { mutableStateOf<String?>(null) }
    var showMobileAudioPrompt by remember { mutableStateOf(false) }
    var uncachedAudioMb by remember { mutableFloatStateOf(0f) }

    fun startRecitation(list: List<String>, begin: Int, rangeRepeatCount: Int = 1) {
        scope.launch {
            if (store.streamingAudio) {
                audio = AudioState.Playing(
                    verseKey = list.getOrElse(begin) { list.first() },
                    index = begin,
                    total = list.size,
                    isBuffering = true,
                    rangeCycle = 1,
                    totalCycles = rangeRepeatCount,
                )
                portionAudio.repeatEach = repeatEach
                portionAudio.repeatRange = rangeRepeatCount
                portionAudio.playStream(
                    verses = list,
                    quality = audioQuality,
                    reciter = selectedReciter,
                    startIndex = begin,
                    repeatRange = rangeRepeatCount,
                    onVerse = { i ->
                        audio = AudioState.Playing(
                            verseKey = list[i],
                            index = i,
                            total = list.size,
                            isBuffering = false,
                            rangeCycle = portionAudio.currentCycle,
                            totalCycles = rangeRepeatCount,
                        )
                    },
                    onFinished = { audio = AudioState.Idle },
                    onBuffering = { buffering ->
                        val cur = audio as? AudioState.Playing
                        if (cur != null) {
                            audio = cur.copy(isBuffering = buffering)
                        }
                    },
                )
                return@launch
            }

            val uncachedEst = portionAudio.uncachedBytesEstimate(list, audioQuality, selectedReciter)
            audio = AudioState.Fetching(0, list.size, 0L, uncachedEst)
            val files = portionAudio.ensureCached(list, audioQuality, reciter = selectedReciter) { done, total, bytesDone, totalBytes ->
                audio = AudioState.Fetching(done, total, bytesDone, totalBytes)
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
            portionAudio.repeatEach = repeatEach
            portionAudio.repeatRange = rangeRepeatCount
            portionAudio.play(
                files = files,
                verses = list,
                onVerse = { i ->
                    audio = AudioState.Playing(
                        verseKey = list[i],
                        index = i,
                        total = list.size,
                        isBuffering = false,
                        rangeCycle = portionAudio.currentCycle,
                        totalCycles = rangeRepeatCount,
                    )
                },
                onFinished = { audio = AudioState.Idle },
                startIndex = begin,
                repeatRange = rangeRepeatCount,
            )
        }
    }

    /**
     * Start listening, optionally **at a particular ayah**.
     */
    fun listen(startAt: String? = null, customVerses: List<String>? = null, rangeRepeatCount: Int = 1) {
        // Already going: point it at the ayah instead of starting over.
        if (customVerses == null && (audio is AudioState.Playing || audio is AudioState.Paused)) {
            val at = loaded.indexOf(startAt)
            if (startAt != null && at >= 0) portionAudio.goTo(at)
            return
        }
        if (audio !is AudioState.Idle) { stopListening(); return }
        scope.launch {
            val repo = MushafRepository(context)

            // When viewing a page outside today's wird (e.g. browsing a Sūrah),
            // play the ayahs on the currently-viewed page instead of jumping to today's wird.
            val isToday = current in todaysPages
            val downloadAmt = store.downloadAmount
            val verses = if (customVerses != null && customVerses.isNotEmpty()) {
                customVerses
            } else when (downloadAmt) {
                "Page" -> {
                    val layout = repo.layoutOnly(current)
                    if (isToday && layout != null) {
                        litFor(layout).toList()
                    } else {
                        layout?.glyphs?.map { it.verseKey }?.distinct() ?: emptyList()
                    }
                }
                "Surah" -> {
                    val layout = repo.layoutOnly(current)
                    val sNum = layout?.glyphs?.firstOrNull()?.verseKey?.substringBefore(':')?.toIntOrNull()
                        ?: com.mosman.wird.domain.SurahIndex.on(current).firstOrNull()?.number ?: 1
                    val surah = com.mosman.wird.domain.SurahIndex.byNumber(sNum)
                    if (surah != null) {
                        (1..surah.verses).map { "$sNum:$it" }
                    } else {
                        layout?.glyphs?.map { it.verseKey }?.distinct() ?: emptyList()
                    }
                }
                "Juz" -> {
                    val jNum = repo.layoutOnly(current)?.juz ?: com.mosman.wird.domain.JuzIndex.of(current).number
                    val juz = com.mosman.wird.domain.JuzIndex.all.firstOrNull { it.number == jNum }
                    val nextPage = com.mosman.wird.domain.JuzIndex.all.getOrNull(jNum)?.firstPage ?: 605
                    val startP = juz?.firstPage ?: current
                    (startP until nextPage).flatMap { p ->
                        repo.layoutOnly(p)?.glyphs?.map { it.verseKey } ?: emptyList()
                    }.distinct()
                }
                else -> {
                    if (isToday) {
                        todaysPages.flatMap { p ->
                            val layout = repo.layoutOnly(p) ?: return@flatMap emptyList()
                            litFor(layout)
                        }.distinct()
                    } else {
                        val layout = repo.layoutOnly(current)
                        layout?.glyphs?.map { it.verseKey }?.distinct() ?: emptyList()
                    }
                }
            }

            if (verses.isEmpty()) {
                audio = AudioState.Failed("Couldn't work out which ayahs to play.")
                return@launch
            }

            // If startAt was not specified, start at the first verse on the currently viewed page
            val effectiveStartAt = startAt ?: run {
                val layout = repo.layoutOnly(current)
                layout?.glyphs?.firstOrNull { !it.isEndMarker }?.verseKey
            }
            val list = if (customVerses != null) customVerses else if (effectiveStartAt != null && effectiveStartAt !in verses) listOf(effectiveStartAt) else verses
            val begin = if (effectiveStartAt != null) list.indexOf(effectiveStartAt).coerceAtLeast(0) else 0
            loaded = list
            rangeRepeat = rangeRepeatCount

            if (!store.streamingAudio) {
                val cached = portionAudio.isCached(list, audioQuality, selectedReciter)
                if (!cached && !com.mosman.wird.audio.isWifi(context)) {
                    val bytes = portionAudio.uncachedBytesEstimate(list, audioQuality, selectedReciter)
                    uncachedAudioMb = (bytes.toFloat() / (1024 * 1024)).coerceAtLeast(0.5f)
                    pendingListenAyahs = list
                    pendingListenStartAt = effectiveStartAt
                    showMobileAudioPrompt = true
                    return@launch
                }
            }

            startRecitation(list, begin, rangeRepeatCount)
        }
    }

    // **The reading surface carries its own light and dark.** His instruction, 2026-08-19:
    // *"home and menu are in dark mode but page is light mode."* Everything below sits in the
    // page's theme rather than the app's, so the chrome can follow the phone while the mushaf
    // stays a printed object. The toolbar and the verse sheet come with it, because a dark bar
    // over a light page is two surfaces arguing.
    SetStatusBarAppearance(isLightBackground = !dark)

    WirdTheme(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
    Box(modifier = Modifier.fillMaxSize()) {
        // **The controls sit over the page while something is playing, and vanish when it
        // stops.** His ask, 2026-08-19. Drawn last so it lands above the mushaf, aligned to
        // the bottom because that is where a thumb already is and § 5e keeps the top of the
        // page clear of chrome.
        ListenBar(
            audio = audio,
            repeatEach = repeatEach,
            repeatRange = rangeRepeat,
            reciter = selectedReciter,
            onChangeReciter = { showReciterPicker = true },
            onPlayPause = {
                when (audio) {
                    is AudioState.Playing -> {
                        portionAudio.pause()
                        (audio as AudioState.Playing).let {
                            audio = AudioState.Paused(
                                verseKey = it.verseKey,
                                index = it.index,
                                total = it.total,
                                rangeCycle = it.rangeCycle,
                                totalCycles = it.totalCycles,
                            )
                        }
                    }
                    is AudioState.Paused -> {
                        portionAudio.resume()
                        (audio as AudioState.Paused).let {
                            audio = AudioState.Playing(
                                verseKey = it.verseKey,
                                index = it.index,
                                total = it.total,
                                isBuffering = false,
                                rangeCycle = it.rangeCycle,
                                totalCycles = it.totalCycles,
                            )
                        }
                    }
                    else -> Unit
                }
            },
            onPrevious = { portionAudio.previous() },
            onNext = { portionAudio.next() },
            onRepeat = {
                repeatEach = nextRepeat(repeatEach)
                portionAudio.repeatEach = repeatEach
            },
            onRepeatRange = {
                rangeRepeat = nextRangeRepeat(rangeRepeat)
                portionAudio.repeatRange = rangeRepeat
            },
            onStop = { stopListening() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                // You can still long-press an ayah while something is playing, and then both
                // want the same strip of screen. The bar steps up over the toolbar rather
                // than fighting it for the bottom edge.
                .padding(bottom = if (selectedVerse != null) 76.dp else 16.dp, start = 16.dp, end = 16.dp)
                .zIndex(2f),
        )

        // Floating audio player dock when chrome is shown and audio is idle
        val currentSurah = remember(current, pageInfo[current]) {
            pageInfo[current]?.first?.takeIf { it.isNotEmpty() }
                ?: com.mosman.wird.domain.SurahIndex.on(current).firstOrNull()?.name.orEmpty()
        }
        AnimatedVisibility(
            visible = chromeShown && !recording && (audio is AudioState.Idle || audio is AudioState.Failed),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = if (selectedVerse != null) 76.dp else 16.dp, start = 16.dp, end = 16.dp)
                .zIndex(2f),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            AudioDockIdle(
                surah = currentSurah,
                page = current,
                offToday = current !in todaysPages,
                reciter = selectedReciter,
                isMenuOpen = showReciterPicker,
                onPlay = { listen() },
                onChangeReciter = { showReciterPicker = !showReciterPicker },
            )
        }

        // Floating audio player dock when downloading recitation
        AnimatedVisibility(
            visible = !recording && audio is AudioState.Fetching,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = if (selectedVerse != null) 76.dp else 16.dp, start = 16.dp, end = 16.dp)
                .zIndex(3f),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            (audio as? AudioState.Fetching)?.let { fetching ->
                AudioDockDownloading(
                    audio = fetching,
                    onCancel = { stopListening() },
                )
            }
        }

        // Anchored Reciter Pop-Down Menu above audio dock
        AnimatedVisibility(
            visible = showReciterPicker,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = if (selectedVerse != null) 140.dp else 80.dp, start = 16.dp, end = 16.dp)
                .zIndex(10f),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            ReciterPopDownMenu(
                selected = selectedReciter,
                onSelect = { chosen ->
                    selectedReciter = chosen
                    store.selectedReciter = chosen
                    showReciterPicker = false
                    if (audio is AudioState.Playing || audio is AudioState.Paused) {
                        stopListening()
                    }
                },
                onDismiss = { showReciterPicker = false },
            )
        }

        if (showReciterPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(9f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showReciterPicker = false },
                    ),
            )
        }


        // **Two ways to read the same page, and the mushaf is the default every time.**
        // section 5z: the translation is a separate mode rather than English poured between
        // the mushaf's lines, because those lines are a per-page font's typesetting and an
        // inserted paragraph destroys them.
        if (translationMode) {
            TranslationScreen(
                pageNumber = current,
                modifier = Modifier.safeDrawingPadding(),
                showAyah = store.ayahBeforeTranslation,
                sources = selectedTranslationSources,
                onSelectSources = { selectedTranslationSources = it },
                onBackgroundTap = { chromeShown = !chromeShown },
            )
        } else {
        MushafPager(
            initialPage = initialP,
            jump = jump,
            highlightPortion = isWirdSession,
            allowedPageRange = allowedRange,
            modifier = Modifier.safeDrawingPadding(),
            onPageChanged = {
                current = it
                onPageVisited(it)
            },
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
            review = reviewVerses,
            selected = selectedVerse,
            selectedVerses = if (rangeRepeat > 1 && (audio is AudioState.Playing || audio is AudioState.Paused)) loaded.toSet() else emptySet(),
            // Long-press selects; a plain tap still belongs to the background handler that
            // shows the chrome, so selecting cannot be done by accident while reading.
            onWordLongPress = { key ->
                selectedVerse = if (selectedVerse == key) null else key
                chromeShown = false
            },
            footer = { page ->
                // Only under today's reading. On a page you are browsing there is nothing
                // to finish, and a "done" button there would be marking the wrong thing.
                if (isWirdSession && page.page == todaysPages.last()) {
                    DoneControl(
                        doneMethod = doneMethod,
                        mode = readingMode,
                        hasRecording = hasRecording,
                        onCheck = onCheckRecitation,
                        checkState = checkState,
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
                        page = page.page,
                        ayahCount = page.ayahCount(litFor(page)),
                        surahName = surahLabelFor(page, litFor(page)),
                        progress = progress,
                    )
                }
            },
        )

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
                        // ⚠ **The verdict never gates the day.** PLAN task 14's check is
                        // loudness, which cannot tell recitation from any other speech, so
                        // letting it veto a recording would be the app calling someone a liar
                        // on evidence it does not have. The day is marked either way and the
                        // reading is shown beside it — Sacred Rule 6 asks the app not to
                        // overstate, and that cuts both directions.
                        heard = judgeRecitation(levels)
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
            val pageVerseKey = remember(current) {
                val s = com.mosman.wird.domain.SurahIndex.on(current).firstOrNull()?.number ?: 1
                "$s:1"
            }
            ChromeBar(
                surah = pageInfo[current]?.first.orEmpty(),
                page = current,
                juz = pageInfo[current]?.second ?: 0,
                progress = progress,
                offToday = offToday,
                isWirdSession = isWirdSession,
                onBrowseSurahs = onBrowseSurahs,
                onSettings = { chromeShown = false; onSettings() },
                audio = audio,
                // The bar stays up while it plays. It is the only stop control, and a
                // stop button that vanishes the moment you use it is how you end up
                // tapping the page trying to find it again.
                onListen = { listen() },
                reciter = selectedReciter,
                onChangeReciter = { showReciterPicker = true },
                dark = dark,
                onNightMode = onNightMode,
                onBack = onBack,
                translation = translationMode,
                onToggleTranslation = { translationMode = !translationMode },
                onOpenTranslationDialog = {
                    if (translationMode) {
                        translationMode = false
                    } else {
                        showTranslationDialog = true
                    }
                },
                bookmarked = remember(pageVerseKey, bookmarkTick) { isBookmarked(pageVerseKey) },
                onToggleBookmark = {
                    onToggleBookmark(pageVerseKey)
                    bookmarkTick++
                },
                selectedSources = selectedTranslationSources,
                onSelectSources = { selectedTranslationSources = it },
            )
        }

        if (showTranslationDialog) {
            TranslationDialog(
                isTranslationActive = translationMode,
                selectedSources = selectedTranslationSources,
                showAyahBeforeTranslation = showAyahBeforeTranslation,
                onToggleActive = { active ->
                    translationMode = active
                },
                onSelectSources = { sources ->
                    selectedTranslationSources = sources
                    translationMode = true
                },
                onToggleShowAyah = { show ->
                    showAyahBeforeTranslation = show
                    store.ayahBeforeTranslation = show
                },
                onDismiss = { showTranslationDialog = false },
            )
        }

        // Option B: Top Ribbon Banner for Return to Today's Wird
        AnimatedVisibility(
            visible = isWirdSession && offToday,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .safeDrawingPadding()
                .padding(top = if (chromeShown) 70.dp else 12.dp)
                .zIndex(10f),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            ReturnToWirdTopBanner(
                currentPage = current,
                assignedPage = todaysPages.first(),
                onClick = {
                    jumpCount++
                    jump = PageJump(todaysPages.first(), jumpCount)
                },
            )
        }
    }

    // The toolbar for a selected ayah. Pinned near the foot rather than floated over the
    // exact word: the reference anchors it to the selection, which needs the word's screen
    // position, and the page is a scrolling pager of measured glyph rows. A fixed anchor is
    // honest and reachable; chasing the word would be guesswork.
    selectedVerse?.let { key ->
        val surahName = com.mosman.wird.domain.SurahIndex
            .byNumber(key.substringBefore(':').toIntOrNull() ?: 0)?.name.orEmpty()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(bottom = Scale.space8),
            contentAlignment = Alignment.BottomCenter,
        ) {
            VerseActions(
                verseKey = key,
                // Read through a counter so the icon flips the moment it is tapped: the store
                // is a file, and a plain call would be recomposed from stale state.
                bookmarked = remember(key, bookmarkTick) { isBookmarked(key) },
                onBookmark = {
                    onToggleBookmark(key)
                    bookmarkTick++
                },
                onPlay = {
                    listen(startAt = key)
                    // **The toolbar closes once it has been used.** Both it and the playback
                    // bar are pinned to the bottom, so leaving it up stacked one on the other
                    // — the ayah label was clipped and half the play button was behind a
                    // share icon. Acting on a selection also finishes with it.
                    selectedVerse = null
                },
                onPlayRange = {
                    rangePlayStartVerse = key
                    showRangePlayDialog = true
                    selectedVerse = null
                },
                onOpenElsewhere = OpenElsewhere.intentFor(context, key)?.let { i ->
                    {
                        context.startActivity(i)
                        selectedVerse = null
                    }
                },
                onShare = {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            shareTextFor(key, surahName),
                        )
                    }
                    context.startActivity(
                        android.content.Intent.createChooser(send, "Share this ayah")
                    )
                    selectedVerse = null
                },
                onDismiss = { selectedVerse = null },
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

    if (showMobileAudioPrompt) {
        ClayConfirmDialog(
            title = "Download Reciter Audio?",
            message = "You are on mobile data. Download recitation (~${String.format(java.util.Locale.US, "%.1f", uncachedAudioMb)} MB) by $selectedReciter?",
            confirmLabel = "Download with data",
            cancelLabel = "Wait for Wi-Fi",
            onConfirm = {
                showMobileAudioPrompt = false
                val toPlay = pendingListenAyahs
                val start = pendingListenStartAt
                pendingListenAyahs = null
                pendingListenStartAt = null
                if (toPlay != null) {
                    val begin = if (start != null) toPlay.indexOf(start).coerceAtLeast(0) else 0
                    startRecitation(toPlay, begin, rangeRepeat)
                }
            },
            onDismiss = {
                showMobileAudioPrompt = false
                pendingListenAyahs = null
                pendingListenStartAt = null
            },
        )
    }

    rangePlayStartVerse?.let { startV ->
        if (showRangePlayDialog) {
            RangePlayDialog(
                startVerse = startV,
                onDismiss = {
                    showRangePlayDialog = false
                    rangePlayStartVerse = null
                },
                onPlayRange = { verses, repeatRangeCount, repeatEachCount ->
                    showRangePlayDialog = false
                    rangePlayStartVerse = null
                    repeatEach = repeatEachCount
                    portionAudio.repeatEach = repeatEachCount
                    rangeRepeat = repeatRangeCount
                    listen(
                        startAt = verses.firstOrNull(),
                        customVerses = verses,
                        rangeRepeatCount = repeatRangeCount,
                    )
                },
            )
        }
    }
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
    isWirdSession: Boolean = true,
    onBrowseSurahs: (() -> Unit)? = null,
    onSettings: () -> Unit,
    audio: AudioState,
    onListen: () -> Unit,
    reciter: String = "",
    onChangeReciter: () -> Unit = {},
    dark: Boolean,
    onNightMode: () -> Unit,
    onBack: () -> Unit,
    translation: Boolean,
    onToggleTranslation: () -> Unit,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    selectedSources: List<com.mosman.wird.data.TranslationSource> = com.mosman.wird.data.TranslationSource.entries,
    onSelectSources: (List<com.mosman.wird.data.TranslationSource>) -> Unit = {},
    onOpenTranslationDialog: () -> Unit = {},
) {
    val colors = LocalWirdColors.current
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised)
            .safeDrawingPadding()
            .padding(horizontal = Scale.space4, vertical = Scale.space2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back arrow
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = colors.onSurfaceRaised,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .defaultMinSize(minWidth = Scale.minTarget, minHeight = Scale.minTarget)
                .padding(Scale.space3),
        )
        Spacer(Modifier.width(Scale.space2))

        // Center: Surah title, page & juz, progress streak
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (surah.isNotEmpty()) "Surah $surah" else "Surah",
                color = colors.onSurfaceRaised,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                text = if (isWirdSession) {
                    if (offToday) {
                        if (juz > 0) "Page $page, Juz' $juz" else "Page $page"
                    } else {
                        if (juz > 0) "Today's Wird · Page $page, Juz' $juz" else "Today's Wird · Page $page"
                    }
                } else {
                    if (juz > 0) "Page $page, Juz' $juz" else "Page $page"
                },
                color = colors.onSurfaceRaised.copy(alpha = 0.75f),
                style = TextStyle(fontSize = 12.sp),
            )
            progress?.takeIf { isWirdSession && it.totalDaysRead > 0 }?.let { p ->
                Text(
                    text = streakLine(p),
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }
            audioLine(audio)?.let {
                Text(
                    text = it,
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }
        }

        // Right side: 1) Bookmark glyph, 2) Translation globe (1 tap), 3) 3-lines menu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Bookmark Glyph (matches squircle vector style on home screen)
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier.size(40.dp),
            ) {
                BookmarkGlyph(
                    filled = bookmarked,
                    tint = if (bookmarked) Color(0xFFC9A24B) else colors.onSurfaceRaised,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Translation Globe Icon: 1-tap opens full translation picker dialog
            IconButton(
                onClick = onOpenTranslationDialog,
                modifier = Modifier.size(40.dp),
            ) {
                GlobeIcon(
                    tint = if (translation) Color(0xFFC9A24B) else colors.onSurfaceRaised,
                )
            }

            // 3-lines menu icon
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(40.dp),
                ) {
                    HamburgerIcon(tint = colors.onSurfaceRaised)
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = colors.surfaceRaised,
                ) {
                    DropdownMenuItem(
                        text = { Text("Night mode", color = colors.onSurfaceRaised) },
                        trailingIcon = {
                            Checkbox(
                                checked = dark,
                                onCheckedChange = { menuOpen = false; onNightMode() },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.accent,
                                    uncheckedColor = colors.textSecondary,
                                    checkmarkColor = colors.surface,
                                ),
                            )
                        },
                        onClick = { menuOpen = false; onNightMode() },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (translation) "Mushaf" else "Translation",
                                color = colors.onSurfaceRaised,
                            )
                        },
                        onClick = { menuOpen = false; onToggleTranslation() },
                    )
                    if (onBrowseSurahs != null) {
                        DropdownMenuItem(
                            text = { Text("Browse Sūrahs & Juz'", color = colors.onSurfaceRaised) },
                            onClick = { menuOpen = false; onBrowseSurahs() },
                        )
                    }
                    if (reciter.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Reciter: $reciter", color = colors.onSurfaceRaised) },
                            onClick = { menuOpen = false; onChangeReciter() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Settings", color = colors.onSurfaceRaised) },
                        onClick = { menuOpen = false; onSettings() },
                    )
                }
            }
        }
    }
}

/**
 * Option B: Elevated Clay Badge Pill shown when the user has flipped away from today's assigned Wird.
 * Features a tactile floating capsule, directional arrow badge pointing towards today's wird,
 * and smooth return jump on tap.
 */
@Composable
private fun ReturnToWirdTopBanner(
    currentPage: Int,
    assignedPage: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    val isBefore = currentPage < assignedPage

    Surface(
        modifier = modifier
            .wrapContentWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = colors.surfaceRaised,
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.35f)),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isBefore) {
                // When swiped backward (to page before today's wird), in RTL Mushaf today's wird is to the left
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to Today's Wird",
                        tint = colors.accent,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            Text(
                text = "Return to Today's Wird",
                color = colors.onSurfaceRaised,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "·",
                color = colors.onSurfaceRaised.copy(alpha = 0.45f),
                style = TextStyle(fontSize = 13.sp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Page $assignedPage",
                color = colors.accent,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
            )

            if (!isBefore) {
                // When swiped forward (to page after today's wird), in RTL Mushaf today's wird is to the right
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Return to Today's Wird",
                        tint = colors.accent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

/**
 * Floating Audio Dock shown when chrome is open and audio is idle.
 */
@Composable
private fun AudioDockIdle(
    surah: String,
    onPlay: () -> Unit,
    onChangeReciter: () -> Unit,
    modifier: Modifier = Modifier,
    page: Int = 1,
    offToday: Boolean = false,
    reciter: String = "Abu Bakr al-Shatri",
    isMenuOpen: Boolean = false,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    // Smoothly animate arrow rotation (0° = pointing down, 180° = pointing upwards)
    val arrowRotation by animateFloatAsState(
        targetValue = if (isMenuOpen) 180f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "reciterArrowRotation",
    )
    // Smoothly animate arrow moving upwards when menu opens
    val arrowOffsetY by animateDpAsState(
        targetValue = if (isMenuOpen) (-2.5).dp else 0.dp,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "reciterArrowOffsetY",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = if (isDark) Color(0xFF1F2026) else Color(0xFFF9F6EE),
                elevation = 4.dp,
                highlightColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.6f),
                shadowColor = Color.Black.copy(alpha = 0.25f),
                strokeWidth = 1.dp,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                // Play circular button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clayPill(
                            shape = CircleShape,
                            backgroundColor = colors.accent,
                            elevation = 2.dp,
                        )
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play recitation",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onChangeReciter),
                ) {
                    Text(
                        text = reciter,
                        color = colors.textPrimary,
                        style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (offToday) {
                            if (surah.isNotEmpty()) "Reciting Surah $surah · Page $page" else "Reciting Page $page"
                        } else {
                            if (surah.isNotEmpty()) "Reciting Surah $surah (Today's Wird)" else "Reciting Today's Wird"
                        },
                        color = colors.textSecondary,
                        style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Dropdown chevron arrow on the right with smooth flip and upward animation
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clayPill(
                        shape = CircleShape,
                        backgroundColor = if (isDark) Color(0xFF282932) else Color(0xFFEDE7DA),
                        elevation = 1.dp,
                    )
                    .clickable(onClick = onChangeReciter),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isMenuOpen) "Close reciter menu" else "Select reciter",
                    tint = colors.accent,
                    modifier = Modifier
                        .size(20.dp)
                        .offset { IntOffset(0, arrowOffsetY.roundToPx()) }
                        .rotate(arrowRotation),
                )
            }
        }
    }
}

/**
 * Modern downloading dock showing real-time megabytes progress and cancel button.
 */
@Composable
private fun AudioDockDownloading(
    audio: AudioState.Fetching,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    val progress = if (audio.totalBytes > 0) {
        (audio.bytesDownloaded.toFloat() / audio.totalBytes).coerceIn(0.04f, 1f)
    } else if (audio.total > 0) {
        (audio.done.toFloat() / audio.total).coerceIn(0.04f, 1f)
    } else {
        0.04f
    }

    val mbDone = String.format(java.util.Locale.US, "%.1f", audio.bytesDownloaded.toFloat() / (1024 * 1024))
    val mbTotal = String.format(java.util.Locale.US, "%.1f", audio.totalBytes.toFloat() / (1024 * 1024))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = if (isDark) Color(0xFF1F2026) else Color(0xFFF9F6EE),
                elevation = 4.dp,
                highlightColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.6f),
                shadowColor = Color.Black.copy(alpha = 0.25f),
                strokeWidth = 1.dp,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    // "X" Cancel Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clayPill(
                                shape = CircleShape,
                                backgroundColor = if (isDark) Color(0xFF282932) else Color(0xFFEDE7DA),
                                elevation = 1.dp,
                            )
                            .clickable(onClick = onCancel),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel download",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(17.dp),
                        )
                    }

                    Column {
                        Text(
                            text = "Downloading recitation…",
                            color = colors.textPrimary,
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = if (audio.totalBytes > 0) "$mbDone MB / $mbTotal MB" else "$mbDone MB",
                            color = colors.textSecondary,
                            style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Determinate Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) Color(0xFF282932) else Color(0xFFE2DDD2)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.accent),
                )
            }
        }
    }
}

/**
 * Anchored Pop-Down Reciter Menu card.
 */
@Composable
private fun ReciterPopDownMenu(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val reciters = listOf(
        DialogOption("Abu Bakr al-Shatri", "Abu Bakr al-Shatri", "Murattal · Hafs an Asim (Default)"),
        DialogOption("Mishary Rashid Alafasy", "Mishary Rashid Alafasy", "Murattal · Melodic & Clear"),
        DialogOption("Mahmoud Khalil Al-Husary", "Mahmoud Khalil Al-Husary", "Murattal · Master of Tajweed"),
        DialogOption("Abdul Basit Abdul Samad", "Abdul Basit Abdul Samad", "Murattal · Celebrated Egyptian Reciter"),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(20.dp),
                backgroundColor = if (isDark) Color(0xFF1F2026) else Color(0xFFFFFFFF),
                elevation = 8.dp,
                highlightColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.85f),
                shadowColor = Color.Black.copy(alpha = 0.35f),
                strokeWidth = 1.dp,
            )
            .padding(12.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SELECT RECITER",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            reciters.forEach { option ->
                val isSelected = option.value == selected
                val itemBg = if (isSelected) {
                    if (isDark) Color(0xFF1A3828) else Color(0xFFEDF5F0)
                } else {
                    Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(itemBg)
                        .clickable { onSelect(option.value) }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.title,
                            color = if (isSelected) colors.accent else colors.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                        option.description.let {
                            Text(
                                text = it,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                            )
                        }
                    }

                    if (isSelected) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tactile Clay Confirmation Dialog for mobile data audio download prompt.
 */
@Composable
private fun ClayConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "Download with data",
    cancelLabel: String = "Wait for Wi-Fi",
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
                .padding(24.dp),
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
                Column {
                    Text(
                        text = title,
                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    Text(
                        text = message,
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF556C60),
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(bottom = 20.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clayPill(
                                    shape = RoundedCornerShape(14.dp),
                                    backgroundColor = if (isDark) Color(0xFF1A2A20) else Color(0xFFEDE8DD),
                                    elevation = 2.dp,
                                )
                                .clickable(onClick = onDismiss)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = cancelLabel,
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1.3f).defaultMinSize(minHeight = 44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                text = confirmLabel,
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

@Composable
private fun BookmarkRibbonIcon(
    filled: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(22.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.12f)
            lineTo(size.width * 0.72f, size.height * 0.12f)
            lineTo(size.width * 0.72f, size.height * 0.88f)
            lineTo(size.width * 0.50f, size.height * 0.68f)
            lineTo(size.width * 0.28f, size.height * 0.88f)
            close()
        }
        if (filled) {
            drawPath(path, color = tint, style = Fill)
        } else {
            drawPath(
                path,
                color = tint,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
            )
        }
    }
}

@Composable
private fun GlobeIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(22.dp)) {
        val stroke = 1.8.dp.toPx()
        val r = size.minDimension * 0.40f
        val center = center
        drawCircle(color = tint, radius = r, center = center, style = Stroke(stroke))
        drawLine(color = tint, start = Offset(center.x - r, center.y), end = Offset(center.x + r, center.y), strokeWidth = stroke)
        val oval = Path().apply {
            addOval(Rect(center.x - r * 0.50f, center.y - r, center.x + r * 0.50f, center.y + r))
        }
        drawPath(oval, color = tint, style = Stroke(stroke))
    }
}

@Composable
private fun HamburgerIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height
        val startX = w * 0.20f
        val endX = w * 0.80f
        drawLine(tint, Offset(startX, h * 0.28f), Offset(endX, h * 0.28f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(startX, h * 0.50f), Offset(endX, h * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(startX, h * 0.72f), Offset(endX, h * 0.72f), strokeWidth = stroke, cap = StrokeCap.Round)
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
    // The bar names the ayah and says which of how many, so a second line saying the same
    // thing under the page would be the same fact twice in two shapes.
    is AudioState.Paused -> null
    is AudioState.Fetching -> null
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

@Composable
private fun TranslationDialog(
    isTranslationActive: Boolean,
    selectedSources: List<com.mosman.wird.data.TranslationSource>,
    showAyahBeforeTranslation: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onSelectSources: (List<com.mosman.wird.data.TranslationSource>) -> Unit,
    onToggleShowAyah: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val cardBg = if (isDark) Color(0xFF16241E) else Color(0xFFF7F4EB)

    // Multi-select state: default to all 3 if empty, otherwise whatever is currently chosen
    var tempSelected by remember(selectedSources) {
        mutableStateOf(
            if (selectedSources.isEmpty()) {
                com.mosman.wird.data.TranslationSource.entries.toSet()
            } else {
                selectedSources.toSet()
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clayCard(
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = cardBg,
                    elevation = 8.dp,
                    strokeWidth = 1.dp,
                )
                .padding(20.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GlobeIcon(
                            tint = colors.accent,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = "Translation View",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D),
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "SELECT TRANSLATIONS",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.accent,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = "Choose one or more to view side-by-side:",
                    fontSize = 11.5.sp,
                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )

                com.mosman.wird.data.TranslationSource.entries.forEach { source ->
                    val isChecked = tempSelected.contains(source)
                    val sub = when (source) {
                        com.mosman.wird.data.TranslationSource.SAHEEH -> "Saheeh International (English)"
                        com.mosman.wird.data.TranslationSource.HAUSA -> "Abubakar Mahmud Gumi (Hausa)"
                        com.mosman.wird.data.TranslationSource.TRANSLITERATION -> "Phonetic transliteration by Quran.com"
                    }
                    TranslationCheckboxRow(
                        title = source.label,
                        subtitle = sub,
                        checked = isChecked,
                        onToggle = {
                            tempSelected = if (isChecked) {
                                if (tempSelected.size > 1) tempSelected - source else tempSelected
                            } else {
                                tempSelected + source
                            }
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(8.dp))

                // Toggle: Show Arabic text before translation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleShowAyah(!showAyahBeforeTranslation) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Show Arabic Ayah with English",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFC7D3CB) else Color(0xFF2C4339),
                    )
                    Checkbox(
                        checked = showAyahBeforeTranslation,
                        onCheckedChange = { onToggleShowAyah(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.accent,
                            uncheckedColor = colors.textSecondary,
                            checkmarkColor = colors.surface,
                        ),
                    )
                }

                Spacer(Modifier.height(14.dp))

                // OK / Save Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clayCard(
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = colors.accent,
                            elevation = 3.dp,
                        )
                        .clickable {
                            val finalList = if (tempSelected.isEmpty()) {
                                com.mosman.wird.data.TranslationSource.entries.toList()
                            } else {
                                com.mosman.wird.data.TranslationSource.entries.filter { tempSelected.contains(it) }
                            }
                            onSelectSources(finalList)
                            onToggleActive(true)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "OK / Save",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                if (isTranslationActive) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Switch back to Mushaf View",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable {
                                onToggleActive(false)
                                onDismiss()
                            }
                            .padding(vertical = 4.dp, horizontal = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationCheckboxRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val bg = if (checked) {
        if (isDark) Color(0xFF1F382B) else Color(0xFFE5EFE7)
    } else {
        if (isDark) Color(0xFF111E18) else Color(0xFFEFECE2)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(12.dp),
                backgroundColor = bg,
                elevation = if (checked) 2.dp else 1.dp,
                strokeWidth = 1.dp,
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                color = if (checked) colors.accent else if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D),
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = colors.accent,
                uncheckedColor = colors.textSecondary,
                checkmarkColor = colors.surface,
            ),
        )
    }
}

