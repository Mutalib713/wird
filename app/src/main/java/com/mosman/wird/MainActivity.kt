package com.mosman.wird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mosman.wird.data.ThemeMode
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.ReadingPlan
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

/**
 * PLAN tasks 5–5c.
 *
 * A phone with no position goes to setup; everything else opens straight onto today's
 * portion. Settings is reachable from the foot of the page and returns there.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Nudge.createChannel(this)
        val store = WirdStore(this)

        setContent {
            var screen by remember {
                mutableStateOf(if (store.isSetUp) Screen.TODAY else Screen.SETUP)
            }
            // Mirrored into state so a change repaints immediately; the store stays the
            // thing that survives a restart.
            var theme by remember { mutableStateOf(store.themeMode) }
            var plan by remember { mutableStateOf(store.plan) }
            var position by remember { mutableStateOf(store.positionUnit) }
            var startVerse by remember { mutableStateOf(store.startVerse) }

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
                        assignment = todaysAssignment(
                            startUnit = position,
                            plan = plan,
                            date = LocalDate.now(),
                        ),
                        startVerse = startVerse,
                        onSettings = { screen = Screen.SETTINGS },
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        theme = theme,
                        plan = plan,
                        positionLabel = positionLabelFor(
                            startVerse = startVerse,
                            page = com.mosman.wird.domain.Mushaf.pageOf(position),
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
