package com.mosman.wird

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mosman.wird.audio.AudioQuality
import com.mosman.wird.audio.ModelDownload
import com.mosman.wird.audio.Recogniser
import com.mosman.wird.audio.WhisperNative
import com.mosman.wird.data.DayLogStore
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.data.Where
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.CompanionAction
import com.mosman.wird.domain.CompanionBrain
import com.mosman.wird.domain.replyForAll
import com.mosman.wird.domain.surahs
import com.mosman.wird.mushaf.MushafDownloadService
import com.mosman.wird.mushaf.MushafRepository
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.assignPortion
import com.mosman.wird.domain.checkRecitation
import com.mosman.wird.domain.pages
import com.mosman.wird.domain.progressOf
import com.mosman.wird.domain.LifeSpace
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.TrackType
import com.mosman.wird.domain.TrackScheduleMode
import com.mosman.wird.domain.todaysAssignment
import com.mosman.wird.nudge.Armed
import com.mosman.wird.nudge.Nudge
import com.mosman.wird.nudge.NudgeScheduler
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.mosman.wird.data.ThemeMode
import com.mosman.wird.data.ArabicText
import com.mosman.wird.data.TranslationSource
import com.mosman.wird.data.Translations
import com.mosman.wird.ui.WirdTopBar
import com.mosman.wird.ui.theme.LocalWirdColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import com.mosman.wird.ui.BookmarkGlyph
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.mosman.wird.audio.Recitation
import com.mosman.wird.data.BookmarkStore
import com.mosman.wird.data.ConversationStore
import com.mosman.wird.data.DataOnDevice
import com.mosman.wird.data.Export
import com.mosman.wird.domain.Commitment
import com.mosman.wird.domain.ReadingDirection
import com.mosman.wird.domain.Speaker
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.ui.BookmarksScreen
import com.mosman.wird.ui.ChatScreen
import com.mosman.wird.ui.HomeScreen
import com.mosman.wird.ui.CheckState
import com.mosman.wird.ui.OpenElsewhere
import com.mosman.wird.ui.RecitationsScreen
import com.mosman.wird.ui.SettingsScreen
import com.mosman.wird.ui.SurahsTab
import com.mosman.wird.ui.WirdTab
import com.mosman.wird.ui.FloatingIslandDock
import com.mosman.wird.ui.SetupScreen
import com.mosman.wird.ui.TodayScreen
import com.mosman.wird.ui.positionLabelFor
import com.mosman.wird.ui.theme.WirdTheme
import com.mosman.wird.widget.refreshWidget
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

/** Where the app can be. There is no home screen; today's portion is the front door. */
private enum class Screen { SETUP, TODAY, SETTINGS, BOOKMARKS }

/** Tracks where the user opened a reading page from, so back returns correctly. */
private enum class PageSource { HOME, SURAHS, BOOKMARKS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Nudge.createChannel(this)
        val store = WirdStore(this)
        val days = DayLogStore(this)
        val chat = ConversationStore(filesDir)
        val bookmarks = BookmarkStore(filesDir)

        setContent {
            var screen by remember {
                mutableStateOf(if (store.isSetUp) Screen.TODAY else Screen.SETUP)
            }
            var theme by remember { mutableStateOf(store.themeMode) }
            var plan by remember { mutableStateOf(store.plan) }
            var position by remember { mutableIntStateOf(store.positionUnit) }
            var startVerse by remember { mutableStateOf(store.startVerse) }
            var seenChrome by remember { mutableStateOf(store.hasSeenChrome) }
            var showToolkitTour by remember { mutableStateOf(!store.hasSeenToolkitTour && store.isSetUp) }
            var schedule by remember { mutableStateOf(store.nudgeSchedule) }
            var audioQuality by remember { mutableStateOf(store.audioQuality) }
            var readerName by remember { mutableStateOf(store.readerName) }
            var readingMode by remember { mutableStateOf(store.readingMode) }
            var direction by remember { mutableStateOf(store.readingDirection) }
            val today = LocalDate.now()
            var lifeSpaces by remember { mutableStateOf(store.getLifeSpaces()) }
            var activeSpace by remember { mutableStateOf(store.activeSpace()) }
            var activeTrack by remember { mutableStateOf(store.activeTrack(today)) }
            var trackScheduleMode by remember { mutableStateOf(store.trackScheduleMode) }
            var turns by remember { mutableStateOf(chat.all()) }
            var saved by remember { mutableStateOf(bookmarks.all()) }
            /** What is on the phone, for the "Your data" row. Refreshed after either action. */
            var onDevice by remember { mutableStateOf<DataOnDevice?>(null) }
            var exportNote by remember { mutableStateOf<String?>(null) }
            var commitment by remember { mutableStateOf(store.commitment) }
            var away by remember { mutableStateOf(store.away) }
            var pageNight by remember { mutableStateOf(store.pageNight) }
            val translations = remember { Translations(this@MainActivity) }
            val mushaf = remember { MushafRepository(this@MainActivity) }
            var cachedPages by remember { mutableStateOf(0 to 0L) }
            val recogniser = remember { Recogniser(this@MainActivity) }
            var model by remember { mutableStateOf(recogniser.installed()) }
            var fetchingModel by remember { mutableStateOf<Pair<Long, Long>?>(null) }
            var downloadedModels by remember { mutableStateOf(recogniser.downloaded()) }
            var checkState by remember { mutableStateOf<CheckState>(CheckState.Idle) }
            val arabic = remember { ArabicText(this@MainActivity) }
            var reviewVerses by remember { mutableStateOf<Set<String>>(emptySet()) }

            // Re-read whenever Settings opens rather than once at launch: the whole point is
            // to notice a change the user made outside the app, in Android's own settings.
            var notificationsOn by remember { mutableStateOf(true) }
            LaunchedEffect(screen) {
                notificationsOn = androidx.core.app.NotificationManagerCompat
                    .from(this@MainActivity).areNotificationsEnabled()
            }

            // Orientation and screen wake settings
            val lockOrientation = store.lockOrientation
            val landscapeOrientation = store.landscapeOrientation
            LaunchedEffect(lockOrientation, landscapeOrientation, screen) {
                requestedOrientation = when {
                    !store.lockOrientation -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    store.landscapeOrientation -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
            val keepScreenAwake = store.keepScreenAwake
            LaunchedEffect(keepScreenAwake, screen) {
                if (store.keepScreenAwake) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            val liveMushafProgress by MushafDownloadService.mushafProgress.collectAsState()
            val downloading = liveMushafProgress?.let { it.done to it.total }

            // Counted when Settings is opened or when background download completes
            LaunchedEffect(screen, liveMushafProgress) {
                if (screen == Screen.SETTINGS && liveMushafProgress == null) {
                    cachedPages = withContext(Dispatchers.IO) { mushaf.cached() }
                }
            }

            // ⚠ **Re-read every time Settings opens, because the download now finishes
            // somewhere else.** The service owns it, so this screen has no callback to wait
            // for - it just looks at what is on disk. That also makes the row self-healing:
            // whatever went wrong, reopening Settings shows the truth.
            LaunchedEffect(screen) {
                if (screen == Screen.SETTINGS) {
                    withContext(Dispatchers.IO) {
                        downloadedModels = recogniser.downloaded()
                        model = recogniser.installed()
                    }
                }
            }
            /** The chat, opened from Home's companion card. */
            var onChat by remember { mutableStateOf(false) }
            /** Home's overflow. Settings used to be a quarter of the tab bar; now it lives here. */
            var menuOpen by remember { mutableStateOf(false) }
            var armed by remember { mutableStateOf<Armed?>(null) }
            var tab by remember { mutableStateOf(WirdTab.HOME) }
            /** Set when a surah is picked from the Sūrahs tab; consumed by TodayScreen. */
            var openPage by remember { mutableStateOf<Int?>(null) }
            /** Home is a dashboard (PROFILE 5g); the page is one tap behind it. */
            var onPage by remember { mutableStateOf(false) }
            /** Whether current reader session is Today's Wird or general Sūrah reading. */
            var isWirdSession by remember { mutableStateOf(true) }
            /** Where this reading page was opened from, for correct back navigation. */
            var pageSource by remember { mutableStateOf(PageSource.HOME) }
            val playback = remember { Recitation(this@MainActivity) }

            /**
             * Re-arm and remember what happened.
             *
             * Called on every launch, not only when something changes. Prayer times move
             * a minute a day, an alarm can be lost to a force-stop or a battery
             * optimiser, and opening the app is the one moment we are certain to get.
             */
            fun reArm() {
                armed = NudgeScheduler.arm(this@MainActivity)
            }

            val askLocation = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                // Refused is a supported answer, not an error. The fallback already
                // works and the settings screen already explains it.
                if (granted) Where.refresh(this@MainActivity) { reArm() }
                reArm()
            }

            val askNotifications = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { reArm() }

            /**
             * Ask for the two things the reminder needs, at the moment it starts to mean
             * something.
             *
             * Never on a cold first launch. A permission sheet that appears before the
             * app has shown what it is gets refused on reflex, and these testers are on
             * phones where a refusal is difficult to walk back. By the time someone has
             * set a position and a daily amount they have said what they want; asking
             * then is asking about something they just chose.
             */
            fun askForWhatTheReminderNeeds() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (!Where.hasPermission(this@MainActivity)) {
                    askLocation.launch(Where.PERMISSION)
                }
            }

            LaunchedEffect(Unit) { onDevice = Export.whatIsHere(this@MainActivity) }

            // PLAN task 14. Logged once at launch because "is the recitation checker usable on
            // this phone" has three separate answers - no native library, no model, or ready -
            // and on a tester's device the log is the only way anyone finds out which.
            LaunchedEffect(Unit) {
                val recogniser = Recogniser(this@MainActivity)
                android.util.Log.i(
                    "WirdWhisper",
                    "native=${WhisperNative.available} model=${recogniser.installed()} " +
                        "ready=${recogniser.ready()}" +
                        if (WhisperNative.available) " | ${WhisperNative.getSystemInfo()}" else "",
                )
            }

            LaunchedEffect(Unit) {
                // A no-op without permission, and a no-op while the stored fix is fresh.
                Where.refresh(this@MainActivity) { reArm() }
                reArm()
            }

            // Read here, in composable context. SYSTEM has no answer of its own, so both
            // overflow toggles have to ask what is actually being painted.
            // ⚠ **No longer derived from the app's theme.** It is the page's own
            // setting now, defaulting to light, so a dark phone gives dark chrome around a
            // light mushaf. See WirdStore.pageNight.
            val pageDark = pageNight

            // The widget shows today's portion and whether it is done, so it has to be told
            // whenever either moves. Fire-and-forget: nothing in the app waits on it, and it
            // is a no-op when no widget is on a home screen.
            val widgetScope = rememberCoroutineScope()
            fun nudgeWidget() {
                widgetScope.launch { refreshWidget(this@MainActivity) }
            }

            // **The app had no back handling at all until 2026-08-18.** Pressing back on the
            // page, in the chat or in Settings quit Wird outright. That was survivable while
            // a bottom tab bar was always on screen; § 5t moved the bar to the top and hides
            // it over the mushaf, which turned a rough edge into a dead end.
            //
            // Order matters: the innermost thing closes first, and Home falls through to the
            // system so back still leaves the app from where leaving makes sense.
            BackHandler(enabled = onChat) { onChat = false }
            BackHandler(enabled = !onChat && screen == Screen.SETTINGS) { screen = Screen.TODAY }
            BackHandler(enabled = !onChat && screen == Screen.BOOKMARKS) {
                tab = WirdTab.HOME
                screen = Screen.TODAY
            }
            BackHandler(enabled = !onChat && screen == Screen.TODAY && onPage) {
                onPage = false
                when (pageSource) {
                    PageSource.SURAHS -> tab = WirdTab.SURAHS
                    PageSource.BOOKMARKS -> screen = Screen.BOOKMARKS
                    PageSource.HOME -> { /* stay on HOME tab, which is the default */ }
                }
            }
            BackHandler(enabled = !onChat && screen == Screen.TODAY && !onPage && tab != WirdTab.HOME) {
                tab = WirdTab.HOME
            }

            var doneMethod by remember { mutableStateOf(days.methodFor(today)) }
            var hasRecording by remember { mutableStateOf(days.audioFor(today) != null) }
            var progress by remember { mutableStateOf(progressOf(days.all(), today)) }

            val isTrackDoneToday = activeTrack.lastCompletedDate == today.toString()
            val trackDoneMethod = if (isTrackDoneToday) doneMethod ?: Method.TAPPED else null
            val doneCover = days.coveredOn(today)
            val assignment = if (doneCover != null && isTrackDoneToday) {
                assignPortion(doneCover.first, doneCover.second, activeTrack.direction)
            } else {
                todaysAssignment(
                    startUnit = activeTrack.positionUnit,
                    plan = ReadingPlan(defaultUnits = activeTrack.dailyUnits),
                    date = today,
                    direction = activeTrack.direction,
                )
            }

            /**
             * One sentence in, everything that follows out.
             *
             * **The whole companion loop lives here and nowhere else.** Both surfaces — the
             * card on Home and [ChatScreen] — only forward raw text; they do not understand
             * it, act on it, or word the reply. That matters because the conversation is now
             * persisted: if a screen kept its own copy of what was said, two screens would
             * disagree about it the moment you moved between them.
             *
             * The order is deliberate. Your line is logged **before** the action runs, so a
             * crash mid-action still leaves what you said on disk. PLAN task 21 needs that:
             * it cannot measure commitments made against commitments kept if the making was
             * never recorded.
             */
            fun said(text: String) {
                val t = text.trim()
                if (t.isEmpty()) return
                turns = chat.say(Speaker.YOU, t)

                // **One sentence can hold three instructions.** PLAN task 22 - "one page a
                // day, half on Fridays, I'm travelling next week" is a schedule, a weekday
                // exception and a pause, and applying only the first would leave the other
                // two silently unheard. The plan goes in because "make Fridays lighter" is
                // relative to what Fridays currently are.
                val actions = CompanionBrain.understandAll(t, plan = plan)

                actions.forEach { action ->
                    when (action) {
                        // A commitment becomes a real alarm. This is the whole point: task
                        // 8's scheduler already turns "after Isha" into a time that moves
                        // with the sun, so the sentence lands on machinery rather than on a
                        // promise.
                        //
                        // ⚠ It rides inside the commitment rather than being written into
                        // the daily reminder, so it expires with the day. PLAN task 22.
                        is CompanionAction.CommitTo -> {
                            commitment = Commitment(
                                spoken = action.spoken,
                                madeAt = LocalDateTime.now(),
                                schedule = action.schedule,
                            )
                            store.commitment = commitment
                            reArm()
                        }
                        is CompanionAction.MarkDone -> {
                            days.markDone(
                                date = today,
                                method = Method.TAPPED,
                                audio = null,
                                startUnit = assignment.startUnit,
                                units = assignment.units,
                            )
                            doneMethod = days.methodFor(today)
                            nudgeWidget()
                            progress = progressOf(days.all(), today)
                            store.recordTrackDone(activeTrack.id, assignment.nextStartUnit, today)
                            lifeSpaces = store.getLifeSpaces()
                            activeSpace = store.activeSpace()
                            activeTrack = store.activeTrack(today)
                            position = activeTrack.positionUnit
                            store.startVerse = null
                            startVerse = null
                            // The promise is spent. Leaving it pinned would have the app
                            // still holding you to something you have already done.
                            commitment = null
                            store.commitment = null
                        }
                        is CompanionAction.OpenSurah -> {
                            openPage = action.surah.firstPage
                            onPage = true
                            pageSource = PageSource.HOME
                            onChat = false
                        }
                        is CompanionAction.Listen -> {
                            onPage = true
                            pageSource = PageSource.HOME
                            onChat = false
                        }

                        // The plan changes take effect today, not tomorrow. Today's portion
                        // is computed from the plan every time it is drawn, so the widget
                        // has to be told or it keeps showing yesterday's arithmetic.
                        is CompanionAction.ChangePlan -> {
                            plan = plan.copy(defaultUnits = action.units)
                            store.plan = plan
                            nudgeWidget()
                        }
                        is CompanionAction.ChangeDayPlan -> {
                            val byDay = plan.weekdayUnits.toMutableMap()
                            if (action.units == null) {
                                byDay.remove(action.day)
                            } else {
                                byDay[action.day] = action.units
                            }
                            plan = plan.copy(weekdayUnits = byDay)
                            store.plan = plan
                            nudgeWidget()
                        }

                        // **A routine change clears tonight's promise.** Otherwise "move my
                        // reminder to 9 from now on" would be answered by an alarm still
                        // following whatever was promised an hour earlier, and the change
                        // would look broken on the one evening it was asked for.
                        is CompanionAction.MoveReminder -> {
                            store.nudgeSchedule = action.schedule
                            schedule = action.schedule
                            commitment = null
                            store.commitment = null
                            reArm()
                        }
                        is CompanionAction.PauseUntil -> {
                            away = action.away
                            store.away = action.away
                            commitment = null
                            store.commitment = null
                            reArm()
                        }
                        is CompanionAction.Resume -> {
                            away = null
                            store.away = null
                            reArm()
                        }

                        // Saying "not today" changes nothing on purpose. There is no row for
                        // a missed day and no penalty to apply - the reply already said it is
                        // fine. Sacred Rule 3.
                        else -> Unit
                    }
                }

                // **The translation is answered on its own, off the main thread.** The words
                // live in a bundled asset and reading one is IO, so this action is lifted out
                // of the synchronous reply rather than blocking the conversation on a file
                // read. Everything else answers immediately, as before.
                val spoken = actions.filter { it !is CompanionAction.ExplainVerse }
                if (spoken.isNotEmpty()) {
                    turns = chat.say(
                        Speaker.WIRD,
                        replyForAll(
                            spoken,
                            progress,
                            positionLabelFor(startVerse, Mushaf.pageOf(position)),
                        ),
                    )
                }

                actions.filterIsInstance<CompanionAction.ExplainVerse>().forEach { ask ->
                    widgetScope.launch {
                        turns = chat.say(Speaker.WIRD, translationFor(translations, ask))
                    }
                }
            }

            WirdTheme(mode = theme) {
                // Setup sits outside the tabs on purpose: there is nowhere else to be
                // until it is finished, and a tab bar during setup is four ways to
                // abandon the one thing being asked.
                if (onChat) {
                    // A full screen rather than a sheet: PROFILE.md § 5m. The whole point
                    // is that it reads unmistakably as a chat, and a half-height sheet with
                    // a tab bar under it does not.
                    ChatScreen(
                        turns = turns,
                        commitment = if (doneMethod == null) commitment else null,
                        checkingBackAt = (armed as? Armed.At)?.time?.let(::clockLabel)
                            ?: (armed as? Armed.AtFallback)?.time?.let(::clockLabel),
                        shortcuts = listOf("Tafsir of verse", "How am I doing?", "Where am I?", "Already recited today", "Remind in 1 hour", "Not today"),
                        surahName = assignment.surahs.firstOrNull()?.name,
                        pageNumber = assignment.pages.firstOrNull(),
                        doneMethod = doneMethod,
                        streak = progress.currentStreak,
                        pagesLeft = (Mushaf.PAGES - Mushaf.pageOf(position)).coerceAtLeast(0),
                        onSend = { said(it) },
                        onBack = { onChat = false },
                        onNewChat = {
                            chat.clear()
                            val surah = assignment.surahs.firstOrNull()?.name ?: "your daily portion"
                            val page = assignment.pages.firstOrNull() ?: Mushaf.pageOf(position)
                            val greeting = "Assalamu Alaikum! Fresh reflection started for Page $page ($surah). How did your recitation go today, and what would you like to reflect on?"
                            turns = chat.say(Speaker.WIRD, greeting)
                        },
                    )
                } else if (screen == Screen.SETUP) {
                    SetupScreen(
                        onDone = { page, unitsPerDay, verse, name, mode, way ->
                            store.positionPage = page
                            store.plan = ReadingPlan(defaultUnits = unitsPerDay)
                            store.startVerse = verse
                            store.readerName = name
                            readerName = store.readerName
                            store.readingMode = mode
                            readingMode = mode
                            store.readingDirection = way
                            direction = way
                            plan = store.plan
                            position = store.positionUnit
                            startVerse = verse
                            val currentTrack = store.activeTrack()
                            store.updateTrack(currentTrack.copy(
                                positionUnit = store.positionUnit,
                                direction = way,
                                dailyUnits = unitsPerDay,
                                startVerseSurah = verse?.first,
                                startVerseAyah = verse?.second,
                            ))
                            lifeSpaces = store.getLifeSpaces()
                            activeSpace = store.activeSpace()
                            activeTrack = store.activeTrack(today)
                            store.hasSeenToolkitTour = false
                            showToolkitTour = true
                            screen = Screen.TODAY
                            // They have just said what they want to read and how much.
                            // This is the moment the reminder is worth asking about.
                            askForWhatTheReminderNeeds()
                        },
                    )

                } else {
                  Box(
                      modifier = Modifier
                          .fillMaxSize()
                          .navigationBarsPadding(),
                  ) {
                    if (onPage) {
                        TodayScreen(
                            assignment = assignment,
                            startVerse = startVerse,
                            onSettings = { screen = Screen.SETTINGS },
                            openPage = openPage,
                            onOpenPageHandled = { openPage = null },
                            hasSeenChrome = seenChrome,
                            onChromeSeen = { store.hasSeenChrome = true; seenChrome = true },
                            doneMethod = if (isWirdSession) trackDoneMethod else null,
                            progress = progress,
                            hasRecording = hasRecording,
                            checkState = checkState,
                            reviewVerses = reviewVerses,
                            // Null when the phone cannot do it, so no button appears rather than
                            // one that quietly does nothing.
                            onCheckRecitation = if (recogniser.ready()) ({
                                widgetScope.launch {
                                    val file = days.audioFileFor(today)
                                    val started = System.currentTimeMillis()

                                    // A ticking clock in its own coroutine. ⚠ **This is the fix for
                                    // "it never worked":** the transcription was running fine and
                                    // the screen simply never changed, which is indistinguishable
                                    // from a hang. A moving number is the whole difference.
                                    val ticker = launch {
                                        var n = 0
                                        while (true) {
                                            checkState = CheckState.Working(n)
                                            kotlinx.coroutines.delay(1_000)
                                            n++
                                        }
                                    }

                                    val heard = try {
                                        // ⚠ Bounded, because native code that never returns would
                                        // otherwise leave the screen waiting forever. Generous: a
                                        // long portion on a slow phone is genuinely minutes.
                                        kotlinx.coroutines.withTimeoutOrNull(6 * 60 * 1000L) {
                                            recogniser.transcribe(file)
                                        }
                                    } finally {
                                        ticker.cancel()
                                    }

                                    val took = String.format(
                                        java.util.Locale.getDefault(),
                                        "%.1f",
                                        (System.currentTimeMillis() - started) / 1000.0,
                                    )
                                    if (heard.isNullOrBlank()) {
                                        reviewVerses = emptySet()
                                        checkState = CheckState.Nothing(
                                            "It couldn't make out any words after ${took}s. The " +
                                                "recording may be too quiet, or this model may not " +
                                                "be good enough."
                                        )
                                    } else {
                                        // ⚠ Compared against the page rather than shown raw. The
                                        // expected words come from the ayahs today's portion
                                        // actually covers, so a reader who stopped early is judged
                                        // against what they set out to read and nothing more.
                                        val expected = withContext(Dispatchers.IO) {
                                            arabic.wordsAcross(assignment.pages)
                                        }
                                        val verdict = checkRecitation(expected, heard)
                                        reviewVerses = verdict.versesToReview
                                        android.util.Log.i(
                                            "WirdWhisper",
                                            "verdict: ${verdict.coverage} covered, " +
                                                "confident=${verdict.confident}, " +
                                                "marked=${verdict.versesToReview} | heard: $heard",
                                        )
                                        checkState = CheckState.Heard(
                                            text = heard,
                                            seconds = (file.length() / 8000).toInt(),
                                            took = took,
                                            summary = verdict.summary,
                                            marked = verdict.versesToReview.size,
                                        )
                                    }
                                }
                            }) else null,
                            audioFile = { days.audioFileFor(today) },
                            audioQuality = audioQuality,
                            readingMode = readingMode,
                            dark = pageDark,
                            onBack = {
                                onPage = false
                                when (pageSource) {
                                    PageSource.SURAHS -> tab = WirdTab.SURAHS
                                    PageSource.BOOKMARKS -> screen = Screen.BOOKMARKS
                                    PageSource.HOME -> { /* stay on HOME tab */ }
                                }
                            },
                            isBookmarked = { key -> bookmarks.has(key) },
                            onToggleBookmark = { key ->
                                bookmarks.toggle(key)
                                saved = bookmarks.all()
                            },
                            onPageVisited = { p -> store.recordRecentPage(p) },
                            onNightMode = {
                                // Flips the PAGE, not the app. Before 2026-08-19 this line set
                                // store.themeMode and took Home and the menus with it.
                                pageNight = !pageNight
                                store.pageNight = pageNight
                            },
                            onDone = { method, file ->
                                days.markDone(
                                    date = today,
                                    method = method,
                                    audio = file,
                                    startUnit = assignment.startUnit,
                                    units = assignment.units,
                                )
                                doneMethod = days.methodFor(today)
                                nudgeWidget()
                                hasRecording = days.audioFor(today) != null
                                progress = progressOf(days.all(), today)

                                // ---- listen back, if this phone can. PLAN task 14 ----
                                //
                                // ⚠ **The day is already marked before this runs, and that order
                                // is deliberate.** Sacred Rule 6 says a recording is what makes a
                                // day recited; the transcription is *evidence about* that
                                // recording and never a verdict on it. Nothing here can unmark a
                                // day, and a phone with no model simply produces no evidence.
                                //
                                // It runs after the fact rather than blocking the screen because
                                // a minute of audio takes real seconds, and the reader has
                                // finished reciting — they should not be watching a spinner.
                                if (method == Method.RECITED && file != null && recogniser.ready()) {
                                    widgetScope.launch {
                                        val heard = recogniser.transcribe(file)
                                        // The measurement PLAN task 14 asks for. Logged rather
                                        // than shown, because until the numbers exist nobody knows
                                        // whether this is worth putting in front of a reader.
                                        android.util.Log.i(
                                            "WirdWhisper",
                                            if (heard != null) "HEARD: $heard" else "HEARD: nothing usable",
                                        )
                                    }
                                }

                                // **The position moves here and nowhere else.** Opening the
                                // app, swiping, or browsing must never advance it — only
                                // finishing does. That is the whole reason the portion is
                                // stable within a day.
                                store.recordTrackDone(activeTrack.id, assignment.nextStartUnit, today)
                                lifeSpaces = store.getLifeSpaces()
                                activeSpace = store.activeSpace()
                                activeTrack = store.activeTrack(today)
                                position = activeTrack.positionUnit
                                // The start ayah only ever applied to the first page.
                                store.startVerse = null
                                startVerse = null
                            },
                            onUndo = {
                                days.clear(today)
                                doneMethod = null
                                // Undo sets doneMethod directly rather than re-reading it, so it
                                // misses the refresh the other three paths get. Left alone, the
                                // widget would go on saying a day was done after you undid it.
                                nudgeWidget()
                                hasRecording = false
                                progress = progressOf(days.all(), today)
                                val revertedTrack = activeTrack.copy(
                                    positionUnit = assignment.startUnit,
                                    lastCompletedDate = null,
                                    currentStreak = (activeTrack.currentStreak - 1).coerceAtLeast(0),
                                )
                                store.updateTrack(revertedTrack)
                                lifeSpaces = store.getLifeSpaces()
                                activeSpace = store.activeSpace()
                                activeTrack = store.activeTrack(today)
                                position = activeTrack.positionUnit
                            },
                            isWirdSession = isWirdSession,
                            onBrowseSurahs = if (isWirdSession) ({
                                onPage = false
                                tab = WirdTab.SURAHS
                            }) else null,
                        )
                    } else {
                        when (tab) {
                            WirdTab.HOME -> HomeScreen(
                                assignment = assignment,
                                progress = progress,
                                doneMethod = trackDoneMethod,
                                recent = days.all().sortedByDescending { it.date },
                                onOpenPage = {
                                    onPage = true
                                    isWirdSession = true
                                    pageSource = PageSource.HOME
                                    openPage = null
                                },
                                positionLabel = positionLabelFor(
                                    startVerse = activeTrack.startVerseSurah?.let { s ->
                                        activeTrack.startVerseAyah?.let { a -> s to a }
                                    } ?: startVerse,
                                    page = Mushaf.pageOf(activeTrack.positionUnit),
                                ),
                                readerName = readerName,
                                mode = readingMode,
                                onOpenBookmarks = { screen = Screen.BOOKMARKS },
                                onMenu = { menuOpen = true },
                                menu = {
                                    if (menuOpen) {
                                        val darkNow = isDark(theme)
                                        HomeMenu(
                                            dark = darkNow,
                                            onNightMode = {
                                                store.themeMode =
                                                    if (darkNow) ThemeMode.LIGHT else ThemeMode.DARK
                                                theme = store.themeMode
                                                menuOpen = false
                                            },
                                            onBookmarks = {
                                                menuOpen = false
                                                screen = Screen.BOOKMARKS
                                            },
                                            onSettings = { menuOpen = false; screen = Screen.SETTINGS },
                                            onToolkitTour = {
                                                menuOpen = false
                                                showToolkitTour = true
                                            },
                                            onDismiss = { menuOpen = false },
                                        )
                                    }
                                },
                                onOpenInQuran = run {
                                    val key = startVerse?.let { "${it.first}:${it.second}" }
                                        ?: assignment.surahs.firstOrNull()?.let { "${it.number}:1" }
                                    val intent = key?.let {
                                        OpenElsewhere.intentFor(this@MainActivity, it)
                                    }
                                    intent?.let { { startActivity(it) } }
                                },
                                pageFor = { date -> days.coveredOn(date)?.first?.let(Mushaf::pageOf) },
                                onMarkRead = {
                                    days.markDone(
                                        date = today,
                                        method = Method.TAPPED,
                                        audio = null,
                                        startUnit = assignment.startUnit,
                                        units = assignment.units,
                                    )
                                    doneMethod = days.methodFor(today)
                                    nudgeWidget()
                                    progress = progressOf(days.all(), today)
                                    store.recordTrackDone(activeTrack.id, assignment.nextStartUnit, today)
                                    lifeSpaces = store.getLifeSpaces()
                                    activeSpace = store.activeSpace()
                                    activeTrack = store.activeTrack(today)
                                    position = activeTrack.positionUnit
                                    store.startVerse = null
                                    startVerse = null
                                },
                                turns = turns,
                                onSaid = { said(it) },
                                onOpenChat = { onChat = true },
                                activeSpace = activeSpace,
                                activeTrack = activeTrack,
                                allSpaces = lifeSpaces,
                                scheduleMode = trackScheduleMode,
                                onSelectTrack = { track ->
                                    store.setActiveTrack(track.id)
                                    trackScheduleMode = store.trackScheduleMode
                                    activeTrack = store.activeTrack(today)
                                    position = activeTrack.positionUnit
                                    direction = activeTrack.direction
                                },
                                onSelectSpace = { space ->
                                    store.setActiveSpace(space.id)
                                    activeSpace = store.activeSpace()
                                    activeTrack = store.activeTrack(today)
                                    position = activeTrack.positionUnit
                                    direction = activeTrack.direction
                                },
                                onToggleScheduleMode = {
                                    val newMode = if (trackScheduleMode == TrackScheduleMode.AUTOMATIC) {
                                        TrackScheduleMode.MANUAL
                                    } else {
                                        TrackScheduleMode.AUTOMATIC
                                    }
                                    store.trackScheduleMode = newMode
                                    trackScheduleMode = newMode
                                    activeTrack = store.activeTrack(today)
                                    position = activeTrack.positionUnit
                                    direction = activeTrack.direction
                                },
                                onOpenSettings = {
                                    screen = Screen.SETTINGS
                                },
                                showToolkitTour = showToolkitTour,
                                onDismissToolkitTour = {
                                    store.hasSeenToolkitTour = true
                                    showToolkitTour = false
                                },
                            )

                            WirdTab.SURAHS -> SurahsTab(
                                bookmarks = saved,
                                onOpenBookmarks = { screen = Screen.BOOKMARKS },
                                onOpenPage = { p ->
                                    openPage = p
                                    onPage = true
                                    isWirdSession = false
                                    pageSource = PageSource.SURAHS
                                },
                                onOpenBookmark = { b ->
                                    openPage = com.mosman.wird.domain.SurahIndex
                                        .byNumber(b.verseKey.substringBefore(':').toIntOrNull() ?: 0)
                                        ?.firstPage
                                    onPage = true
                                    isWirdSession = false
                                    pageSource = PageSource.SURAHS
                                },
                                onPick = { surah ->
                                    openPage = surah.firstPage
                                    onPage = true
                                    isWirdSession = false
                                    pageSource = PageSource.SURAHS
                                },
                                onBack = { tab = WirdTab.HOME },
                            )

                            WirdTab.HISTORY -> RecitationsScreen(
                                logs = days.all(),
                                audioFor = { d -> days.audioFor(d) },
                                coveredFor = { d -> days.coveredOn(d) },
                                onPlay = { f -> playback.play(f) },
                                onStop = { playback.stopPlaying() },
                                onBack = { tab = WirdTab.HOME },
                            )
                        }

                        // Option C Floating Island Capsule Dock at bottom
                        FloatingIslandDock(
                            current = tab,
                            onPick = { tab = it },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                  }
                }

                // Settings, reached from Home's overflow rather than from a tab of its own.
                if (screen == Screen.SETTINGS) {
                    SettingsScreen(
                        theme = theme,
                        plan = plan,
                        positionLabel = positionLabelFor(
                            startVerse = startVerse,
                            page = Mushaf.pageOf(position),
                        ),
                        schedule = schedule,
                        armed = armed,
                        // A pause that has already run its course is history, not a state
                        // the screen should still be reporting.
                        away = away?.takeIf { !it.isPast(today) },
                        notificationsOn = notificationsOn,
                        onFixNotifications = {
                            // Android's own screen for this app. Asking again in-app is not an
                            // option once the permission has been denied: the system stops
                            // showing the dialog, so the only honest route is the settings page.
                            startActivity(
                                android.content.Intent(
                                    android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                ).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                            )
                        },
                        model = model,
                        fetchingModel = fetchingModel,
                        // Null when there is no native library, which removes the row rather
                        // than showing a download that could never be used.
                        onGetModel = if (WhisperNative.available) ({ option ->
                            // Handed to the service rather than run here. A composition-scoped
                            // coroutine dies when the screen does, and 78 MB is far too much to
                            // lose because someone pressed back.
                            MushafDownloadService.startModel(this@MainActivity, option)
                        }) else null,
                        downloadedModels = downloadedModels,
                        onUseModel = { option ->
                            recogniser.choose(option)
                            model = option
                        },
                        cachedPages = cachedPages,
                        downloading = downloading,
                        onDownloadAll = {
                            MushafDownloadService.start(this@MainActivity)
                        },
                        onResume = {
                            away = null
                            store.away = null
                            reArm()
                        },
                        audioQuality = audioQuality,
                        readingMode = readingMode,
                        direction = direction,
                        onDirection = {
                            store.readingDirection = it
                            direction = it
                        },
                        onDevice = onDevice,
                        exportNote = exportNote,
                        onExport = {
                            widgetScope.launch {
                                exportNote = "Preparing…"
                                val r = Export.run(this@MainActivity)
                                if (r == null) {
                                    exportNote = "Couldn't build the file. Nothing was changed."
                                    return@launch
                                }
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    this@MainActivity,
                                    "$packageName.files",
                                    r.file,
                                )
                                val send = android.content.Intent(
                                    android.content.Intent.ACTION_SEND
                                ).apply {
                                    type = "application/zip"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                startActivity(
                                    android.content.Intent.createChooser(send, "Your Wird data")
                                )
                                val mb = r.bytes / 1024.0 / 1024.0
                                exportNote = "Built ${r.file.name}, ${r.recordings} recordings, " +
                                    String.format(java.util.Locale.getDefault(), "%.1f MB", mb) + "."
                            }
                        },
                        onDeleteRecordings = {
                            widgetScope.launch {
                                val n = Export.deleteRecordings(this@MainActivity)
                                onDevice = Export.whatIsHere(this@MainActivity)
                                hasRecording = days.audioFor(today) != null
                                exportNote = if (n == 1) {
                                    "1 recording deleted. Your record of reciting is untouched."
                                } else {
                                    "$n recordings deleted. Your record of reciting is untouched."
                                }
                            }
                        },
                        onTheme = { store.themeMode = it; theme = it },
                        onReadingMode = { store.readingMode = it; readingMode = it },
                        onPlan = { store.plan = it; plan = it },
                        onSchedule = {
                            store.nudgeSchedule = it
                            schedule = it
                            // Straight away, so the line underneath describes the alarm
                            // that now exists rather than the one that used to.
                            reArm()
                        },
                        onAudioQuality = { store.audioQuality = it; audioQuality = it },
                        onUseLocation = { askLocation.launch(Where.PERMISSION) },
                        onPositionChanged = { newVerse, newPage ->
                            store.positionPage = newPage
                            store.startVerse = newVerse
                            position = store.positionUnit
                            startVerse = newVerse
                            val currentTrack = store.activeTrack()
                            store.updateTrack(
                                currentTrack.copy(
                                    positionUnit = store.positionUnit,
                                    startVerseSurah = newVerse?.first,
                                    startVerseAyah = newVerse?.second,
                                )
                            )
                            lifeSpaces = store.getLifeSpaces()
                            activeSpace = store.activeSpace()
                            activeTrack = store.activeTrack(today)
                        },
                        onLifeSpacesChanged = {
                            lifeSpaces = store.getLifeSpaces()
                            activeSpace = store.activeSpace()
                            activeTrack = store.activeTrack(today)
                            trackScheduleMode = store.trackScheduleMode
                            position = activeTrack.positionUnit
                            direction = activeTrack.direction
                        },
                        onBack = { screen = Screen.TODAY },
                    )
                }

                // Dedicated Bookmarks & Recents screen
                if (screen == Screen.BOOKMARKS) {
                    val allRecents = remember(store.recentPages, position) {
                        val list = store.recentPages
                        if (list.isEmpty()) listOf(Mushaf.pageOf(position)) else list
                    }
                    BookmarksScreen(
                        bookmarks = saved,
                        recentPages = allRecents,
                        onOpenPage = { p ->
                            store.recordRecentPage(p)
                            openPage = p
                            onPage = true
                            pageSource = PageSource.BOOKMARKS
                            screen = Screen.TODAY
                        },
                        onOpenBookmark = { b ->
                            val surahNum = b.verseKey.substringBefore(':').toIntOrNull() ?: 1
                            val targetPage = SurahIndex.byNumber(surahNum)?.firstPage ?: 1
                            store.recordRecentPage(targetPage)
                            openPage = targetPage
                            onPage = true
                            pageSource = PageSource.BOOKMARKS
                            screen = Screen.TODAY
                        },
                        onRemoveBookmark = { key ->
                            bookmarks.toggle(key)
                            saved = bookmarks.all()
                        },
                        onBack = {
                            tab = WirdTab.HOME
                            screen = Screen.TODAY
                        },
                    )
                }

                // (Removed: the duplicate BackHandler that overrode Settings sub-screen
                // and Bookmarks back navigation, jumping straight to TODAY.)
            }
        }
    }
}

/**
 * "8:10 pm" from an armed alarm time.
 *
 * The chat shows the real scheduled moment rather than re-deriving one, so what it promises
 * and what `dumpsys alarm` will show can never drift apart.
 */
private fun clockLabel(at: java.time.ZonedDateTime): String =
    at.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")).lowercase()

/**
 * Whether the app is currently painting dark, whatever the setting says.
 *
 * [ThemeMode.SYSTEM] has no answer of its own — it defers to the phone — so a night-mode
 * toggle has to ask what is actually on screen rather than what was chosen.
 */
@Composable
private fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
}

/**
 * Home's overflow menu.
 *
 * **This is where the More tab went.** PROFILE.md § 5t — Settings was never a destination
 * you visit alongside your wird; it is a drawer you open, change one thing in, and leave.
 * It was spending a quarter of the navigation bar.
 *
 * Two items, both real and both wired to state that already existed. Modelled on the
 * reference Mutalib sent: Quran for Android's overflow carries a night-mode *checkbox*,
 * toggled where you are rather than buried three taps into settings.
 */
@Composable
private fun HomeMenu(
    dark: Boolean,
    onNightMode: () -> Unit,
    onBookmarks: () -> Unit,
    onSettings: () -> Unit,
    onToolkitTour: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceRaised,
    ) {
        DropdownMenuItem(
            text = { Text("Bookmarks & Recents", color = colors.onSurfaceRaised) },
            leadingIcon = {
                BookmarkGlyph(
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp),
                )
            },
            onClick = onBookmarks,
        )
        DropdownMenuItem(
            text = { Text("Night mode", color = colors.onSurfaceRaised) },
            trailingIcon = {
                Checkbox(
                    checked = dark,
                    onCheckedChange = { onNightMode() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.accent,
                        uncheckedColor = colors.textSecondary,
                        checkmarkColor = colors.surface,
                    ),
                )
            },
            onClick = onNightMode,
        )
        DropdownMenuItem(
            text = { Text("App Toolkit Tour", color = colors.onSurfaceRaised) },
            onClick = onToolkitTour,
        )
        DropdownMenuItem(
            text = { Text("Settings", color = colors.onSurfaceRaised) },
            onClick = onSettings,
        )
    }
}

/**
 * What the companion says when asked what an ayah means. **His ask, 2026-08-19**, after
 * typing *"so explain verse 1 of fatiha"* and being told "I didn't catch that."
 *
 * ⚠ **It answers with a TRANSLATION and says so in the same breath.** PROFILE.md § 5h approved
 * explaining the Qur'an and Sacred Rule 2 was not softened by that approval: the words come
 * from the bundled Saheeh International text, the translator is named every time, and nothing
 * in this function writes, summarises or interprets anything. The line saying it is not a
 * tafsir is part of the answer rather than a disclaimer bolted on, because a reader who thinks
 * they have been given a scholar's explanation has been misled by the shape of the reply.
 *
 * **Offline, no key, no model, no cost** — his decision the same day: *"for now offline but we
 * will add gemini."* When Gemini lands it may rephrase a *fetched* tafsir; it may not author
 * one, and this function is the shape that rule takes in code.
 */
private suspend fun translationFor(
    translations: Translations,
    ask: CompanionAction.ExplainVerse,
): String {
    val surah = SurahIndex.byNumber(ask.surah)
        ?: return "I don't know that surah."

    // A surah named with no ayah is a question about the title, and is answered as one.
    val ayah = ask.ayah
        ?: return "${surah.name} means ${surah.meaning}. Name an ayah, like " +
            "${surah.number}:1, and I'll show you its translation."

    if (ayah > surah.verses) {
        return "${surah.name} has ${surah.verses} ayahs, so there is no ${surah.number}:$ayah."
    }

    val found = translations.verse(
        surah = surah.number,
        ayah = ayah,
        source = TranslationSource.SAHEEH,
        firstPage = surah.firstPage,
        lastPage = surah.lastPage,
    ) ?: return "I couldn't find ${surah.number}:$ayah in the bundled translation."

    return "${surah.name} ${surah.number}:$ayah\n\n" +
        "${found.text}\n\n" +
        "Translated by ${TranslationSource.SAHEEH.by}. That is a translation, not a tafsir."
}
