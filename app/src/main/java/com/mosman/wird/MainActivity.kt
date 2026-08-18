package com.mosman.wird

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mosman.wird.audio.AudioQuality
import com.mosman.wird.data.DayLogStore
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.data.Where
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.CompanionAction
import com.mosman.wird.domain.CompanionBrain
import com.mosman.wird.domain.replyFor
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.assignPortion
import com.mosman.wird.domain.progressOf
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
import com.mosman.wird.ui.WirdTopBar
import com.mosman.wird.ui.theme.LocalWirdColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier
import com.mosman.wird.audio.Recitation
import com.mosman.wird.data.ConversationStore
import com.mosman.wird.domain.Commitment
import com.mosman.wird.domain.Speaker
import com.mosman.wird.ui.ChatScreen
import com.mosman.wird.ui.HomeScreen
import com.mosman.wird.ui.RecitationsScreen
import com.mosman.wird.ui.SettingsScreen
import com.mosman.wird.ui.SurahsTab
import com.mosman.wird.ui.WirdTab
import com.mosman.wird.ui.SetupScreen
import com.mosman.wird.ui.TodayScreen
import com.mosman.wird.ui.positionLabelFor
import com.mosman.wird.ui.theme.WirdTheme
import java.time.LocalDate
import java.time.LocalDateTime

/** Where the app can be. There is no home screen; today's portion is the front door. */
private enum class Screen { SETUP, TODAY, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Nudge.createChannel(this)
        val store = WirdStore(this)
        val days = DayLogStore(this)
        val chat = ConversationStore(filesDir)

        setContent {
            var screen by remember {
                mutableStateOf(if (store.isSetUp) Screen.TODAY else Screen.SETUP)
            }
            var theme by remember { mutableStateOf(store.themeMode) }
            var plan by remember { mutableStateOf(store.plan) }
            var position by remember { mutableIntStateOf(store.positionUnit) }
            var startVerse by remember { mutableStateOf(store.startVerse) }
            var seenChrome by remember { mutableStateOf(store.hasSeenChrome) }
            var schedule by remember { mutableStateOf(store.nudgeSchedule) }
            var audioQuality by remember { mutableStateOf(store.audioQuality) }
            var readerName by remember { mutableStateOf(store.readerName) }
            var readingMode by remember { mutableStateOf(store.readingMode) }
            var turns by remember { mutableStateOf(chat.all()) }
            var commitment by remember { mutableStateOf(store.commitment) }
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

            LaunchedEffect(Unit) {
                // A no-op without permission, and a no-op while the stored fix is fresh.
                Where.refresh(this@MainActivity) { reArm() }
                reArm()
            }

            // Read here, in composable context. SYSTEM has no answer of its own, so both
            // overflow toggles have to ask what is actually being painted.
            val pageDark = isDark(theme)

            val today = LocalDate.now()
            var doneMethod by remember { mutableStateOf(days.methodFor(today)) }
            var hasRecording by remember { mutableStateOf(days.audioFor(today) != null) }
            var progress by remember { mutableStateOf(progressOf(days.all(), today)) }

            // **Today's portion does not change when you finish it.**
            //
            // The position advances on done, so computing from the live position would
            // rewrite what today *was* the instant you marked it — the page you had just
            // read went pale and the confirmation disappeared. A finished day therefore
            // remembers what it covered, and the screen shows that until tomorrow.
            val doneCover = days.coveredOn(today)
            val assignment = if (doneCover != null) {
                assignPortion(startUnit = doneCover.first, units = doneCover.second)
            } else {
                todaysAssignment(startUnit = position, plan = plan, date = today)
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

                val action = CompanionBrain.understand(t)
                when (action) {
                    // A commitment becomes a real alarm. This is the whole point: task 8's
                    // scheduler already turns "after Isha" into a time that moves with the
                    // sun, so the sentence lands on machinery rather than on a promise.
                    is CompanionAction.CommitTo -> {
                        store.nudgeSchedule = action.schedule
                        schedule = action.schedule
                        commitment = Commitment(spoken = action.spoken, madeAt = LocalDateTime.now())
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
                        progress = progressOf(days.all(), today)
                        store.positionUnit = assignment.nextStartUnit
                        position = store.positionUnit
                        store.startVerse = null
                        startVerse = null
                        // The promise is spent. Leaving it pinned would have the app still
                        // holding you to something you have already done.
                        commitment = null
                        store.commitment = null
                    }
                    is CompanionAction.OpenSurah -> {
                        openPage = action.surah.firstPage
                        onPage = true
                        onChat = false
                    }
                    is CompanionAction.Listen -> {
                        onPage = true
                        onChat = false
                    }
                    // Saying "not today" changes nothing on purpose. There is no row for a
                    // missed day and no penalty to apply - the reply already said it is
                    // fine. Sacred Rule 3.
                    else -> Unit
                }

                turns = chat.say(
                    Speaker.WIRD,
                    replyFor(action, progress, positionLabelFor(startVerse, Mushaf.pageOf(position))),
                )
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
                        shortcuts = listOf("After Isha", "In an hour", "Not today", "Already did it"),
                        onSend = { said(it) },
                        onBack = { onChat = false },
                    )
                } else if (screen == Screen.SETUP) {
                    SetupScreen(
                        onDone = { page, unitsPerDay, verse, name, mode ->
                            store.positionPage = page
                            store.plan = ReadingPlan(defaultUnits = unitsPerDay)
                            store.startVerse = verse
                            store.readerName = name
                            readerName = store.readerName
                            store.readingMode = mode
                            readingMode = mode
                            plan = store.plan
                            position = store.positionUnit
                            startVerse = verse
                            screen = Screen.TODAY
                            // They have just said what they want to read and how much.
                            // This is the moment the reminder is worth asking about.
                            askForWhatTheReminderNeeds()
                        },
                    )

                } else {
                  Column(
                      modifier = Modifier
                          .fillMaxSize()
                          .navigationBarsPadding(),
                  ) {
                    // The bar is hidden while the mushaf is open: the page is the one screen
                    // that should have nothing parked above it. Sacred Rule 5's other half.
                    if (!onPage) {
                        WirdTopBar(
                            current = tab,
                            onPick = { tab = it },
                            // Only Home has an overflow. On the other two it would open a
                            // menu about a screen you are not looking at.
                            onMenu = if (tab == WirdTab.HOME) ({ menuOpen = true }) else null,
                            menu = {
                                if (menuOpen) {
                                    // Read in composable context: the toggle needs what is
                                    // actually on screen, which SYSTEM only answers at draw
                                    // time.
                                    val darkNow = isDark(theme)
                                    HomeMenu(
                                        dark = darkNow,
                                        onNightMode = {
                                            store.themeMode =
                                                if (darkNow) ThemeMode.LIGHT else ThemeMode.DARK
                                            theme = store.themeMode
                                            menuOpen = false
                                        },
                                        onSettings = { menuOpen = false; screen = Screen.SETTINGS },
                                        onDismiss = { menuOpen = false },
                                    )
                                }
                            },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                      when (tab) {
                        WirdTab.HOME -> if (!onPage) HomeScreen(
                            assignment = assignment,
                            progress = progress,
                            doneMethod = doneMethod,
                            recent = days.all().sortedByDescending { it.date },
                            onOpenPage = { onPage = true },
                            positionLabel = positionLabelFor(startVerse, Mushaf.pageOf(position)),
                            readerName = readerName,
                            mode = readingMode,
                            // The week's page column. Days marked before task 6 began
                            // storing what they covered have no page, and show none.
                            pageFor = { date -> days.coveredOn(date)?.first?.let(Mushaf::pageOf) },
                            // The tap route, from Home. Same path the companion's "already
                            // did it" takes, and logged as a tap exactly the same way.
                            onMarkRead = {
                                days.markDone(
                                    date = today,
                                    method = Method.TAPPED,
                                    audio = null,
                                    startUnit = assignment.startUnit,
                                    units = assignment.units,
                                )
                                doneMethod = days.methodFor(today)
                                progress = progressOf(days.all(), today)
                                store.positionUnit = assignment.nextStartUnit
                                position = store.positionUnit
                                store.startVerse = null
                                startVerse = null
                            },
                            turns = turns,
                            onSaid = { said(it) },
                            onOpenChat = { onChat = true },
                        ) else TodayScreen(
                        assignment = assignment,
                        startVerse = startVerse,
                        onSettings = { screen = Screen.SETTINGS },
                            openPage = openPage,
                            onOpenPageHandled = { openPage = null },
                        hasSeenChrome = seenChrome,
                        onChromeSeen = { store.hasSeenChrome = true; seenChrome = true },
                        doneMethod = doneMethod,
                        progress = progress,
                        hasRecording = hasRecording,
                        audioFile = { days.audioFileFor(today) },
                        audioQuality = audioQuality,
                        readingMode = readingMode,
                        dark = pageDark,
                        onNightMode = {
                            store.themeMode = if (pageDark) ThemeMode.LIGHT else ThemeMode.DARK
                            theme = store.themeMode
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
                            hasRecording = days.audioFor(today) != null
                            progress = progressOf(days.all(), today)

                            // **The position moves here and nowhere else.** Opening the
                            // app, swiping, or browsing must never advance it — only
                            // finishing does. That is the whole reason the portion is
                            // stable within a day.
                            store.positionUnit = assignment.nextStartUnit
                            position = store.positionUnit
                            // The start ayah only ever applied to the first page.
                            store.startVerse = null
                            startVerse = null
                        },
                        onUndo = {
                            days.clear(today)
                            doneMethod = null
                            hasRecording = false
                            progress = progressOf(days.all(), today)
                            // Put the position back exactly as far as marking it moved it.
                            store.positionUnit = Math.floorMod(
                                store.positionUnit - assignment.units,
                                Mushaf.TOTAL_UNITS,
                            )
                            position = store.positionUnit
                        },
                    )

                        WirdTab.SURAHS -> SurahsTab(
                            onPick = { surah ->
                                // Picking a surah is a reading action, so it lands you on
                                // the page rather than leaving you in a list admiring it.
                                openPage = surah.firstPage
                                onPage = true
                                tab = WirdTab.HOME
                            },
                        )

                        WirdTab.HISTORY -> RecitationsScreen(
                            logs = days.all(),
                            audioFor = { d -> days.audioFor(d) },
                            onPlay = { f -> playback.play(f) },
                        )
                      }

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
                        audioQuality = audioQuality,
                        readingMode = readingMode,
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
                        onChangePosition = { screen = Screen.SETUP },
                        onBack = { screen = Screen.TODAY },
                    )
                }
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
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWirdColors.current
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceRaised,
    ) {
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
            text = { Text("Settings", color = colors.onSurfaceRaised) },
            onClick = onSettings,
        )
    }
}
