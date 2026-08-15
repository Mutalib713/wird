package com.mosman.wird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mosman.wird.nudge.Nudge
import com.mosman.wird.ui.TodayScreen
import com.mosman.wird.ui.theme.WirdTheme

/**
 * PLAN task 4: the real mushaf page.
 *
 * The task-3 scaffolding buttons are gone. Task 3 is verified and committed, and leaving
 * test controls on the reading surface contradicts the direction — the app is a thin
 * margin around the page, not a control panel. Real scheduling arrives with prayer times
 * in task 8.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Nudge.createChannel(this)
        setContent {
            WirdTheme {
                TodayScreen(
                    page = SAMPLE_PAGE,
                    portionLines = SAMPLE_PORTION,
                    surahLabel = SAMPLE_SURAH,
                )
            }
        }
    }
}

// Hardcoded until task 5 stores the real position. Half a page rather than a whole one,
// so the dimming is actually visible — a full-page portion would light every line and
// prove nothing about the mechanic.
private const val SAMPLE_PAGE = 453
private val SAMPLE_PORTION = 2..8
private const val SAMPLE_SURAH = "Ṣād"
