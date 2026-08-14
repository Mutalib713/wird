package com.mosman.wird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mosman.wird.domain.Assignment
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.assignPortion

/**
 * PLAN task 2: prove the app builds, installs and opens on a real phone.
 *
 * Deliberately unstyled — no colours, no type scale, no spacing system. The palette is
 * Mutalib's to pin from the design-studio picker, and anything chosen here would
 * quietly become the default before he ever saw the options. Task 4 replaces this whole
 * screen with the real mushaf page.
 *
 * The position and target are hardcoded, but the portion itself is computed by the same
 * [assignPortion] the QA suite covers, so this screen also proves the domain layer runs
 * on device rather than only in a JVM test.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { TodayScaffolding(SAMPLE_ASSIGNMENT) } }
    }
}

/** Page 453 — Surah Sad — with a one-page target. Replaced by stored state in task 5. */
private val SAMPLE_ASSIGNMENT: Assignment =
    assignPortion(startUnit = (453 - 1) * Mushaf.UNITS_PER_PAGE, units = 2)

@Composable
private fun TodayScaffolding(assignment: Assignment) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Today")
            Text(
                if (assignment.startPage == assignment.endPage) {
                    "Page ${assignment.startPage}"
                } else {
                    "Pages ${assignment.startPage}–${assignment.endPage}"
                }
            )
            Text("Not done yet")
            Text("scaffolding — task 2")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScaffoldingPreview() {
    MaterialTheme { TodayScaffolding(SAMPLE_ASSIGNMENT) }
}
