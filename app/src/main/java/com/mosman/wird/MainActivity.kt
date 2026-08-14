package com.mosman.wird

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mosman.wird.domain.Assignment
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.assignPortion
import com.mosman.wird.nudge.Nudge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PLAN task 3: prove a scheduled notification arrives and that tapping it lands here.
 *
 * Still deliberately unstyled — the palette is Mutalib's to pin from the design-studio
 * picker, and task 4 replaces this screen with the real mushaf page.
 */
class MainActivity : ComponentActivity() {

    private var arrivedFromNudge by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Nudge.createChannel(this)
        arrivedFromNudge = intent?.getBooleanExtra(Nudge.EXTRA_FROM_NUDGE, false) == true
        setContent {
            MaterialTheme { TodayScaffolding(SAMPLE_ASSIGNMENT, arrivedFromNudge) }
        }
    }

    /** The activity is singleTop-ish via CLEAR_TOP, so a tap can arrive here too. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        arrivedFromNudge = intent.getBooleanExtra(Nudge.EXTRA_FROM_NUDGE, false)
    }
}

/** Page 453 with a one-page target. Replaced by stored state in task 5. */
private val SAMPLE_ASSIGNMENT: Assignment =
    assignPortion(startUnit = (453 - 1) * Mushaf.UNITS_PER_PAGE, units = 2)

private const val TEST_NUDGE_DELAY_MS = 15_000L

@Composable
private fun TodayScaffolding(assignment: Assignment, fromNudge: Boolean) {
    val context = LocalContext.current
    var exactAllowed by remember { mutableStateOf(Nudge.canScheduleExact(context)) }
    var status by remember { mutableStateOf<String?>(null) }

    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        status = if (granted) {
            "Notifications on."
        } else {
            "Notifications off. Nothing will arrive until you turn them on."
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
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

            if (fromNudge) {
                Text("Opened from the reminder.")
            }

            // Honest about which kind of alarm you actually got. A reminder that drifts
            // by half an hour without saying so is how people decide an app is broken.
            Text(
                if (exactAllowed) {
                    "Reminders arrive on time."
                } else {
                    "Reminders will drift. Android only lets clock and calendar apps " +
                        "set exact alarms, so yours fires when the phone gets round to it."
                }
            )

            Nudge.nextAt(context)?.let { at ->
                Text("Next: " + SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(at)))
            }

            status?.let { Text(it) }

            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    status = "Notifications already on."
                }
            }) {
                Text("Turn on notifications")
            }

            if (!exactAllowed) {
                Button(onClick = {
                    Nudge.exactAlarmSettingsIntent(context)?.let { context.startActivity(it) }
                }) {
                    Text("Let reminders arrive on time")
                }
            }

            Button(onClick = {
                val at = System.currentTimeMillis() + TEST_NUDGE_DELAY_MS
                val exact = Nudge.schedule(context, at)
                exactAllowed = exact
                status = if (exact) "Reminder set for 15 seconds." else "Reminder set, roughly 15 seconds."
            }) {
                Text("Remind me in 15 seconds")
            }

            Text("scaffolding — task 3")
        }
    }
}
