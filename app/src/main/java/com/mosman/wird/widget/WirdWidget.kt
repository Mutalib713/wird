package com.mosman.wird.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.action.actionStartActivity
import androidx.glance.unit.ColorProvider
import com.mosman.wird.MainActivity
import com.mosman.wird.data.DayLogStore
import com.mosman.wird.data.ThemeMode
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.assignPortion
import com.mosman.wird.domain.surahs
import com.mosman.wird.domain.todaysAssignment
import com.mosman.wird.ui.theme.Q
import java.time.LocalDate

/**
 * Today's portion, on the home screen. **PLAN task 10.**
 *
 * The plan calls this *"the nudge that cannot be swiped away or killed"*, and that is the
 * whole argument for it. Everything else aimed at the forgetting half of § 2's finding is
 * interruptible: a notification is swiped, an alarm is lost to a battery optimiser, and on
 * the Tecno and Infinix phones the testers actually carry both happen routinely. A widget is
 * not delivered to you — it is already on the screen you unlock to.
 *
 * **It reads the same two stores the app does**, directly, with no service and no cache of
 * its own. `WirdStore` is SharedPreferences and `DayLogStore` is a file; both are readable
 * from the widget's process, and both are the actual source of truth rather than a copy that
 * could disagree with the app.
 *
 * **It cannot mark a day.** Sacred Rule 6 turns on the app never blurring how a day was
 * finished, and a home-screen button is exactly the place where "done" would become a reflex
 * rather than a recitation. Tapping it opens Wird. That is all it does.
 */
class WirdWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read outside the composable: this runs once per update, and re-reading a file on
        // every recomposition is how a widget starts costing battery.
        val store = WirdStore(context)
        val days = DayLogStore(context)
        val today = LocalDate.now()

        val state = if (!store.isSetUp) {
            WidgetState.NotSetUp
        } else {
            // Same rule the app follows: a finished day shows what it covered, not what the
            // position now says. Otherwise marking a day rewrites what today *was*.
            val covered = days.coveredOn(today)
            val assignment = if (covered != null) {
                assignPortion(startUnit = covered.first, units = covered.second)
            } else {
                todaysAssignment(startUnit = store.positionUnit, plan = store.plan, date = today)
            }
            val surah = assignment.surahs.firstOrNull()
            WidgetState.Portion(
                title = surah?.name ?: "Page ${assignment.startPage}",
                detail = detailOf(assignment.units, assignment.startPage),
                method = days.methodFor(today),
            )
        }

        // **Glance 1.1.1 has no day/night ColorProvider**, so the theme is resolved here
        // rather than by the framework. That turns out to be the better answer anyway: the
        // widget follows *the app's* setting, so someone who forced Dark while their phone
        // is light does not get a light widget next to a dark app.
        val dark = when (store.themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> {
                val mode = context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
                mode == Configuration.UI_MODE_NIGHT_YES
            }
        }

        provideContent { WidgetBody(state, dark) }
    }
}

/** What the widget has to say. A closed set, so the layout has no undefined case. */
private sealed interface WidgetState {
    data object NotSetUp : WidgetState
    data class Portion(val title: String, val detail: String, val method: Method?) : WidgetState
}

/**
 * The face of it.
 *
 * **Deliberately three lines and no controls.** A widget competes with everything else on a
 * home screen, and the thing worth winning that competition is the portion's name — so it
 * gets the size, and everything else is small and quiet.
 *
 * Colours come from [Q], the same object the app's palette is built from, so the widget can
 * never drift into looking like a different app. They are given as day/night pairs because a
 * widget is not inside the app's theme and has to answer the launcher's own night setting.
 */
@androidx.compose.runtime.Composable
private fun WidgetBody(state: WidgetState, dark: Boolean) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(pick(dark, Q.surface, Q.nightSurface))
            .padding(16.dp)
            .clickable(openWird()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        when (state) {
            WidgetState.NotSetUp -> {
                Text(
                    text = "Wird",
                    style = TextStyle(
                        color = pick(dark, Q.teal, Q.paleTeal),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "Tap to set up your daily portion.",
                    style = TextStyle(color = pick(dark, Q.detail, Q.nightDetail), fontSize = 13.sp),
                )
            }

            is WidgetState.Portion -> {
                Text(
                    text = if (state.method == null) "TODAY'S PORTION" else "TODAY, DONE",
                    style = TextStyle(
                        color = pick(dark, Q.detail, Q.nightDetail),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    text = state.title,
                    style = TextStyle(color = pick(dark, Q.ink, Q.snow), fontSize = 22.sp),
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = state.detail,
                    style = TextStyle(color = pick(dark, Q.detail, Q.nightDetail), fontSize = 12.sp),
                )
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    // Says which of the two it was, on the home screen, for the same reason
                    // the app does everywhere else. Sacred Rule 6.
                    text = when (state.method) {
                        Method.RECITED -> "Recited aloud"
                        Method.TAPPED -> "Marked as read"
                        null -> "Not yet marked"
                    },
                    style = TextStyle(
                        color = if (state.method == null) {
                            pick(dark, Q.teal, Q.paleTeal)
                        } else {
                            pick(dark, Q.detail, Q.nightDetail)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

/** "One page · page 293". Same words the app uses, so the two never describe a day differently. */
private fun detailOf(units: Int, startPage: Int): String {
    val amount = when (units) {
        1 -> "Half a page"
        2 -> "One page"
        else -> "${units / 2} pages"
    }
    return "$amount · page $startPage"
}

/** One colour, chosen up front. See the note in [WirdWidget.provideGlance]. */
private fun pick(dark: Boolean, day: Color, night: Color) = ColorProvider(if (dark) night else day)

@androidx.compose.runtime.Composable
private fun openWird() = actionStartActivity<MainActivity>()

/**
 * What Android talks to.
 *
 * `updatePeriodMillis` in the XML is capped by the platform at **thirty minutes** and is not
 * honoured at all when the device is dozing, so it is not what keeps this correct. The app
 * pushing an update when something actually changes is — see [refreshWidget].
 */
class WirdWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WirdWidget()
}

/**
 * Push the widget a fresh copy of the truth.
 *
 * **This is what actually keeps it correct**, not the XML's update period. Called whenever
 * something the widget displays has changed: a day marked or unmarked, the position moved,
 * the plan edited. It is cheap and idempotent, so calling it when nothing changed costs a
 * repaint and nothing else.
 *
 * ⚠ **It is deliberately not called on a timer.** A widget that polls is a widget that
 * drains a battery to display a page number, and the phones the testers carry are exactly
 * the ones that would kill it for doing so.
 *
 * Fire-and-forget from the app's own scope: nothing the app does depends on the widget
 * having finished, and a launcher that is not hosting one makes this a no-op.
 */
suspend fun refreshWidget(context: Context) {
    runCatching { WirdWidget().updateAll(context) }
}
