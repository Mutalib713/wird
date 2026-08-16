package com.mosman.wird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mosman.wird.data.DayLogStore
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.assignPortion
import com.mosman.wird.domain.todaysAssignment
import com.mosman.wird.nudge.Nudge
import com.mosman.wird.ui.SettingsScreen
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

            val today = LocalDate.now()
            var doneMethod by remember { mutableStateOf(days.methodFor(today)) }
            var hasRecording by remember { mutableStateOf(days.audioFor(today) != null) }

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
                when (screen) {
                    Screen.SETUP -> SetupScreen(
                        onDone = { page, unitsPerDay, verse ->
                            store.positionPage = page
                            store.plan = ReadingPlan(defaultUnits = unitsPerDay)
                            store.startVerse = verse
                            plan = store.plan
                            position = store.positionUnit
                            startVerse = verse
                            screen = Screen.TODAY
                        },
                    )

                    Screen.TODAY -> TodayScreen(
                        assignment = assignment,
                        startVerse = startVerse,
                        onSettings = { screen = Screen.SETTINGS },
                        hasSeenChrome = seenChrome,
                        onChromeSeen = { store.hasSeenChrome = true; seenChrome = true },
                        doneMethod = doneMethod,
                        hasRecording = hasRecording,
                        audioFile = { days.audioFileFor(today) },
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
                            // Put the position back exactly as far as marking it moved it.
                            store.positionUnit = Math.floorMod(
                                store.positionUnit - assignment.units,
                                Mushaf.TOTAL_UNITS,
                            )
                            position = store.positionUnit
                        },
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        theme = theme,
                        plan = plan,
                        positionLabel = positionLabelFor(
                            startVerse = startVerse,
                            page = Mushaf.pageOf(position),
                        ),
                        onTheme = { store.themeMode = it; theme = it },
                        onPlan = { store.plan = it; plan = it },
                        onChangePosition = { screen = Screen.SETUP },
                        onBack = { screen = Screen.TODAY },
                    )
                }
            }
        }
    }
}
