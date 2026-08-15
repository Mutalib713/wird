package com.mosman.wird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.todaysAssignment
import com.mosman.wird.nudge.Nudge
import com.mosman.wird.ui.SetupScreen
import com.mosman.wird.ui.TodayScreen
import com.mosman.wird.ui.theme.WirdTheme
import java.time.LocalDate

/**
 * PLAN task 5: the page comes from where you actually are, not from a constant.
 *
 * Two destinations. A phone with no position goes to setup; everything else goes to
 * today's portion. There is no home screen in between, on purpose — the app opens on the
 * thing you came to do.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Nudge.createChannel(this)
        val store = WirdStore(this)

        setContent {
            WirdTheme {
                var setUp by remember { mutableStateOf(store.isSetUp) }

                if (!setUp) {
                    SetupScreen(
                        onDone = { page, unitsPerDay, startVerse ->
                            store.positionPage = page
                            store.plan = ReadingPlan(defaultUnits = unitsPerDay)
                            // Remembered so day one begins at the reader's own ayah
                            // rather than at the top of a page that may open with the
                            // tail of the previous surah.
                            store.startVerse = startVerse
                            setUp = true
                        },
                    )
                } else {
                    // Read once per composition rather than held in state: the position
                    // only moves when a day is marked done, which is task 6's job.
                    TodayScreen(
                        assignment = todaysAssignment(
                            startUnit = store.positionUnit,
                            plan = store.plan,
                            date = LocalDate.now(),
                        ),
                        startVerse = store.startVerse,
                    )
                }
            }
        }
    }
}
