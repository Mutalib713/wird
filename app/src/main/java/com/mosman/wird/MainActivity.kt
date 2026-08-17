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
import com.mosman.wird.data.Where
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.assignPortion
import com.mosman.wird.domain.progressOf
import com.mosman.wird.domain.todaysAssignment
import com.mosman.wird.nudge.Armed
import com.mosman.wird.nudge.Nudge
import com.mosman.wird.nudge.NudgeScheduler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.mosman.wird.audio.Recitation
import com.mosman.wird.ui.HomeScreen
import com.mosman.wird.ui.RecitationsScreen
import com.mosman.wird.ui.SettingsScreen
import com.mosman.wird.ui.SurahsTab
import com.mosman.wird.ui.WirdTab
import com.mosman.wird.ui.WirdTabBar
import com.mosman.wird.ui.SetupScreen
import com.mosman.wird.ui.TodayScreen
import com.mosman.wird.ui.positionLabelFor
import com.mosman.wird.ui.theme.WirdTheme
import java.time.LocalDate

/** Where the app can be. There is no home screen; today's portion is the front door. */
private enum class Screen { SETUP, TODAY, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Nudge.createChannel(this)
        val store = WirdStore(this)
        val days = DayLogStore(this)

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

            WirdTheme(mode = theme) {
                // Setup sits outside the tabs on purpose: there is nowhere else to be
                // until it is finished, and a tab bar during setup is four ways to
                // abandon the one thing being asked.
                if (screen == Screen.SETUP) {
                    SetupScreen(
                        onDone = { page, unitsPerDay, verse ->
                            store.positionPage = page
                            store.plan = ReadingPlan(defaultUnits = unitsPerDay)
                            store.startVerse = verse
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
                  Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                      when (tab) {
                        WirdTab.HOME -> if (!onPage) HomeScreen(
                            assignment = assignment,
                            progress = progress,
                            doneMethod = doneMethod,
                            recent = days.all().sortedByDescending { it.date },
                            onOpenPage = { onPage = true },
                        ) else TodayScreen(
                        assignment = assignment,
                        startVerse = startVerse,
                        onSettings = { tab = WirdTab.MORE },
                            openPage = openPage,
                            onOpenPageHandled = { openPage = null },
                        hasSeenChrome = seenChrome,
                        onChromeSeen = { store.hasSeenChrome = true; seenChrome = true },
                        doneMethod = doneMethod,
                        progress = progress,
                        hasRecording = hasRecording,
                        audioFile = { days.audioFileFor(today) },
                        audioQuality = audioQuality,
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

                        WirdTab.MORE -> SettingsScreen(
                        theme = theme,
                        plan = plan,
                        positionLabel = positionLabelFor(
                            startVerse = startVerse,
                            page = Mushaf.pageOf(position),
                        ),
                        schedule = schedule,
                        armed = armed,
                        audioQuality = audioQuality,
                        onTheme = { store.themeMode = it; theme = it },
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
                            onBack = { tab = WirdTab.HOME },
                        )
                      }
                    }
                    WirdTabBar(current = tab, onPick = { tab = it })
                  }
                }
            }
        }
    }
}
