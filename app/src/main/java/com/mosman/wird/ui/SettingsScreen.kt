package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mosman.wird.audio.AudioQuality
import com.mosman.wird.data.PlaceSource
import com.mosman.wird.data.ThemeMode
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.NudgeSchedule
import com.mosman.wird.domain.Prayer
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.label
import com.mosman.wird.nudge.Armed
import com.mosman.wird.nudge.NudgeScheduler
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Settings.
 *
 * Five things now: how it looks, how much a day, when to be reminded, how the recitation
 * is fetched, and where he is. The reminder section arrived with task 8; the recitation
 * one with task 9, where a page of audio turned out to cost fifteen times the page itself.
 *
 * Notably absent: choosing a highlight colour. Raised and declined — see Sacred Rule 5.
 * There is no highlight to colour; the portion is marked by everything else stepping
 * back, and arbitrary colours would break the contrast pairs in PROFILE.md § 6b.
 */
@Composable
fun SettingsScreen(
    theme: ThemeMode,
    plan: ReadingPlan,
    positionLabel: String,
    schedule: NudgeSchedule,
    armed: Armed?,
    audioQuality: AudioQuality,
    onTheme: (ThemeMode) -> Unit,
    onPlan: (ReadingPlan) -> Unit,
    onSchedule: (NudgeSchedule) -> Unit,
    onAudioQuality: (AudioQuality) -> Unit,
    onUseLocation: () -> Unit,
    onChangePosition: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalWirdColors.current
    var overrideDays by remember { mutableStateOf(plan.weekdayUnits.keys) }
    var overrideUnits by remember {
        mutableIntStateOf(plan.weekdayUnits.values.firstOrNull() ?: 1)
    }

    fun push(default: Int = plan.defaultUnits) {
        onPlan(
            ReadingPlan(
                defaultUnits = default,
                weekdayUnits = overrideDays.associateWith { overrideUnits },
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Scale.space6, vertical = Scale.space4),
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
        ) {
            Text("Back to today's portion", color = colors.accent)
        }

        Spacer(Modifier.height(Scale.space4))
        Text("Settings", color = colors.textPrimary, style = TextStyle(fontSize = Scale.display))

        // ---- how it looks ----
        Section("How it looks")
        Row(horizontalArrangement = Arrangement.spacedBy(Scale.space2)) {
            Choice("Paper", theme == ThemeMode.LIGHT) { onTheme(ThemeMode.LIGHT) }
            Choice("Ink", theme == ThemeMode.DARK) { onTheme(ThemeMode.DARK) }
            Choice("Match phone", theme == ThemeMode.SYSTEM) { onTheme(ThemeMode.SYSTEM) }
        }
        Hint("Ink is easier at night.")

        // ---- how much ----
        Section("How much a day")
        Row(horizontalArrangement = Arrangement.spacedBy(Scale.space2)) {
            Choice("Half a page", plan.defaultUnits == 1) { push(default = 1) }
            Choice("One page", plan.defaultUnits == 2) { push(default = 2) }
            Choice("Two pages", plan.defaultUnits == 4) { push(default = 4) }
        }
        Hint("This changes today's portion too, not just tomorrow's.")

        // ---- lighter days ----
        //
        // Four then three, because seven do not fit. Measured on the Pixel: each chip
        // sits on a 66dp pitch (48dp minimum target, widened to Material's 58dp button
        // floor, plus the 4dp gap), so a week costs 458dp across — and 411dp of screen
        // minus the 24dp margins leaves 363dp. One row was 95dp short, which is not a
        // squeeze that degrades gracefully: Saturday broke onto two lines and Sunday was
        // given zero width, so it was absent from the accessibility tree entirely. Two
        // days that could not be picked at all, by touch or by TalkBack.
        //
        // Scrolling sideways was the alternative and it is the same bug wearing a hat —
        // the day on the end stays hidden. Same call as the prayer rows below. The hour
        // chips further down do scroll, and that is not inconsistent: twenty hours can
        // never be shown at once, whereas a week is a small complete set you should be
        // able to take in at a glance.
        //
        // The gap between the rows earns its place. Several days can be lit at once, so
        // two selected chips stacked flush would merge into one sage block and read as a
        // single thing. The prayer rows never need it — only one prayer can be chosen,
        // so their grounds can never touch.
        Section("Go easier on some days")
        Column(verticalArrangement = Arrangement.spacedBy(Scale.space1)) {
            DayOfWeek.entries.chunked(4).forEach { days ->
                Row(horizontalArrangement = Arrangement.spacedBy(Scale.space1)) {
                    days.forEach { day ->
                        Choice(
                            label = day.name.take(2).lowercase().replaceFirstChar(Char::titlecase),
                            on = day in overrideDays,
                        ) {
                            overrideDays =
                                if (day in overrideDays) overrideDays - day
                                else overrideDays + day
                            push()
                        }
                    }
                }
            }
        }
        if (overrideDays.isNotEmpty()) {
            Spacer(Modifier.height(Scale.space2))
            Row(horizontalArrangement = Arrangement.spacedBy(Scale.space2)) {
                Choice("Half a page", overrideUnits == 1) { overrideUnits = 1; push() }
                Choice("One page", overrideUnits == 2) { overrideUnits = 2; push() }
            }
        }
        Hint(
            if (overrideDays.isEmpty()) {
                "Pick a day if some are heavier than others."
            } else {
                "Those days ask for less. Everything else stays as above."
            }
        )

        // ---- when to remind you ----
        //
        // PROFILE.md § 5 keeps prayer times out of v1 as a feature, and this respects
        // that: you pick a landmark you already know, and no time is ever printed. The
        // one exception is the fallback below, where the hour shown is a plain fixed
        // hour rather than a computed prayer.
        Section("When to remind you")
        Row(horizontalArrangement = Arrangement.spacedBy(Scale.space2)) {
            Choice("After a prayer", schedule is NudgeSchedule.AfterPrayer) {
                onSchedule(NudgeSchedule.Default)
            }
            Choice("At a set time", schedule is NudgeSchedule.AtClockTime) {
                onSchedule(NudgeSchedule.AtClockTime(NudgeScheduler.FALLBACK_TIME))
            }
            Choice("Off", schedule is NudgeSchedule.Off) { onSchedule(NudgeSchedule.Off) }
        }

        when (schedule) {
            is NudgeSchedule.AfterPrayer -> {
                Spacer(Modifier.height(Scale.space3))
                // Two rows rather than one: five prayer names do not fit across a phone,
                // and a row that scrolls sideways hides the option on the end.
                Row(horizontalArrangement = Arrangement.spacedBy(Scale.space1)) {
                    listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR).forEach { p ->
                        Choice(p.label, schedule.prayer == p) {
                            onSchedule(schedule.copy(prayer = p))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Scale.space1)) {
                    listOf(Prayer.MAGHRIB, Prayer.ISHA).forEach { p ->
                        Choice(p.label, schedule.prayer == p) {
                            onSchedule(schedule.copy(prayer = p))
                        }
                    }
                }
                Spacer(Modifier.height(Scale.space3))
                Row(horizontalArrangement = Arrangement.spacedBy(Scale.space1)) {
                    listOf(0 to "Right at it", 15 to "15 min", 30 to "30 min", 60 to "An hour")
                        .forEach { (minutes, label) ->
                            Choice(label, schedule.offsetMinutes == minutes) {
                                onSchedule(schedule.copy(offsetMinutes = minutes))
                            }
                        }
                }
            }

            is NudgeSchedule.AtClockTime -> {
                Spacer(Modifier.height(Scale.space3))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Scale.space1),
                ) {
                    // Waking hours only. A reminder at three in the morning is a bug
                    // someone tapped by accident, not a choice.
                    (4..23).forEach { hour ->
                        val at = LocalTime.of(hour, 0)
                        Choice(shortClock(at), schedule.time.hour == hour) {
                            onSchedule(NudgeSchedule.AtClockTime(at))
                        }
                    }
                }
            }

            is NudgeSchedule.Off -> Unit
        }

        Hint(reminderHint(schedule, armed))

        if (schedule is NudgeSchedule.AfterPrayer && wantsLocation(armed)) {
            Spacer(Modifier.height(Scale.space2))
            TextButton(
                onClick = onUseLocation,
                modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
            ) {
                Text("Let Wird check where I am", color = colors.accent)
            }
        }

        // ---- the recitation ----
        //
        // A quality setting is normally a lazy way of avoiding a decision. This one is
        // not: it is a data setting wearing a quality label, and the numbers are real.
        // Mutalib asked for the choice on 2026-08-16 rather than take either default.
        Section("Listening to it")
        Row(horizontalArrangement = Arrangement.spacedBy(Scale.space2)) {
            AudioQuality.entries.forEach { q ->
                Choice(q.label, audioQuality == q) { onAudioQuality(q) }
            }
        }
        Hint(
            "Abu Bakr al-Shatri, ${audioQuality.perPageMb}. Downloaded once, then it plays " +
                "with no signal at all."
        )

        // ---- where you are ----
        Section("Where you are")
        Text(positionLabel, color = colors.textPrimary, style = TextStyle(fontSize = Scale.body))
        Spacer(Modifier.height(Scale.space2))
        TextButton(
            onClick = onChangePosition,
            modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
        ) {
            Text("Move to a different place", color = colors.accent)
        }
        Hint("For when you read ahead on paper, or fall behind.")

        Spacer(Modifier.height(Scale.space8))
    }
}

@Composable
private fun Section(title: String) {
    val colors = LocalWirdColors.current
    Spacer(Modifier.height(Scale.space6))
    Text(title, color = colors.textPrimary, style = TextStyle(fontSize = Scale.title))
    Spacer(Modifier.height(Scale.space2))
}

@Composable
private fun Hint(text: String) {
    val colors = LocalWirdColors.current
    Spacer(Modifier.height(Scale.space2))
    Text(text, color = colors.textSecondary, style = TextStyle(fontSize = Scale.caption))
}

/**
 * Selection carried by a filled ground rather than by colour alone — the palette is a
 * value ramp, so a "selected" colour would read as the same colour. Also the better
 * pattern regardless: colour is never the only signal.
 */
@Composable
private fun Choice(label: String, on: Boolean, onPick: () -> Unit) {
    val colors = LocalWirdColors.current
    TextButton(
        onClick = onPick,
        modifier = Modifier
            .defaultMinSize(minHeight = Scale.minTarget)
            .clip(RoundedCornerShape(Scale.radius))
            .background(if (on) colors.done else Color.Transparent)
            .padding(horizontal = 2.dp),
    ) {
        Text(
            text = label,
            color = if (on) colors.onSurfaceRaised else colors.textSecondary,
            style = TextStyle(fontSize = Scale.body),
        )
    }
}

/**
 * True when the reminder is prayer-based but we are not working from a real fix.
 *
 * Covers both the timezone guess and the fixed-hour fallback, because in both cases the
 * reader can improve things by letting the app look, and in neither case should it just
 * ask again on its own.
 */
private fun wantsLocation(armed: Armed?): Boolean = when (armed) {
    is Armed.AtFallback -> true
    is Armed.At -> armed.source == PlaceSource.TIMEZONE
    else -> false
}

/**
 * What the reminder is actually doing, in one line.
 *
 * Never prints a prayer time — see PROFILE.md § 5. The only clock time that appears here
 * is the fixed-hour fallback, which is a plain hour rather than a computed sunset, and it
 * appears precisely because the reader needs to know the app could not do what they
 * asked.
 */
private fun reminderHint(schedule: NudgeSchedule, armed: Armed?): String {
    val drift = if (armed.isInexact()) " Android may let it drift by a few minutes." else ""
    return when (schedule) {
        is NudgeSchedule.Off ->
            "Nothing will arrive. Turn it back on whenever you want."

        is NudgeSchedule.AtClockTime ->
            "${schedule.label()}, every day.$drift"

        is NudgeSchedule.AfterPrayer -> when (armed) {
            is Armed.AtFallback ->
                "Wird can't work out sunset without knowing roughly where you are, " +
                    "so it will come at ${NudgeScheduler.FALLBACK_TIME.let(::shortClockLong)} " +
                    "until it does.$drift"

            else ->
                "${schedule.label()}. It follows the sun, so it stays right all year.$drift"
        }
    }
}

private fun Armed?.isInexact(): Boolean = when (this) {
    is Armed.At -> !exact
    is Armed.AtFallback -> !exact
    else -> false
}

/** "4pm", for a chip that has to stay narrow. */
private fun shortClock(time: LocalTime): String {
    val hour = if (time.hour % 12 == 0) 12 else time.hour % 12
    return "$hour${if (time.hour < 12) "am" else "pm"}"
}

/** "8:00 pm", for a sentence. */
private fun shortClockLong(time: LocalTime): String {
    val hour = if (time.hour % 12 == 0) 12 else time.hour % 12
    return "$hour:%02d %s".format(time.minute, if (time.hour < 12) "am" else "pm")
}

/** "Ya-Sin 5, page 440" — where the app thinks you are, in words. */
fun positionLabelFor(startVerse: Pair<Int, Int>?, page: Int): String {
    val surah = startVerse?.let { SurahIndex.byNumber(it.first) }
        ?: SurahIndex.on(page).firstOrNull()
    val name = surah?.name ?: "Page $page"
    return when {
        startVerse != null && surah != null -> "$name ${startVerse.second}, page $page"
        else -> "$name, page $page"
    }
}

/** Guards against a stored page outside the mushaf, however it got there. */
fun clampPage(page: Int): Int = page.coerceIn(1, Mushaf.PAGES)
