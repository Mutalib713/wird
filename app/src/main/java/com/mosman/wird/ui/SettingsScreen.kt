package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * **Rebuilt 2026-08-17.** The first version was five sections of chip rows stacked
 * identically, and Mutalib's verdict was that it "looks terrible and is not readable — it
 * doesn't have sections, it just looks someway." He was right, and the fault was
 * structural rather than cosmetic: every section rendered the same, so nothing told you
 * where one ended and the next began, and each section's explanation floated below the
 * whole block instead of attaching to the thing it explained.
 *
 * The shape now is the one the design used and the one every settings screen worth reading
 * uses: **a titled group, holding rows, each row carrying its own name, its own one-line
 * explanation of what it actually does, and its control.** The explanation is the part that
 * makes it readable — you should never have to change a setting to find out what it means.
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Scale.space4),
    ) {
        Spacer(Modifier.height(Scale.space6))
        Text("Settings", color = colors.textPrimary, style = TextStyle(fontSize = Scale.display))
        Spacer(Modifier.height(Scale.space4))

        // ---- reading ----
        Group("Reading") {
            SettingRow("How much a day", "The size of today's portion") {
                Chips(
                    listOf(1 to "Half a page", 2 to "One page", 4 to "Two pages"),
                    plan.defaultUnits,
                ) { push(default = it) }
            }
            Divider()
            SettingRow(
                title = "Go easier on some days",
                caption = if (overrideDays.isEmpty()) {
                    "Pick a day if some are heavier than others"
                } else {
                    "Those days ask for less; everything else stays as above"
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Scale.space1)) {
                    DayOfWeek.entries.chunked(4).forEach { days ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Scale.space1)) {
                            days.forEach { day ->
                                Choice(
                                    label = day.name.take(2).lowercase()
                                        .replaceFirstChar(Char::titlecase),
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
            }
            if (overrideDays.isNotEmpty()) {
                Divider()
                SettingRow("How much on those days", "The lighter amount") {
                    Chips(listOf(1 to "Half a page", 2 to "One page"), overrideUnits) {
                        overrideUnits = it; push()
                    }
                }
            }
            Divider()
            SettingRow("Where you are", positionLabel) {
                Action("Move to a different place", onChangePosition)
            }
        }

        // ---- the reminder ----
        Group("The reminder") {
            SettingRow("When it arrives", reminderCaption(schedule, armed)) {
                Chips3(
                    listOf(
                        "After a prayer" to (schedule is NudgeSchedule.AfterPrayer),
                        "At a set time" to (schedule is NudgeSchedule.AtClockTime),
                        "Off" to (schedule is NudgeSchedule.Off),
                    )
                ) { i ->
                    onSchedule(
                        when (i) {
                            0 -> NudgeSchedule.Default
                            1 -> NudgeSchedule.AtClockTime(NudgeScheduler.FALLBACK_TIME)
                            else -> NudgeSchedule.Off
                        }
                    )
                }
            }

            when (schedule) {
                is NudgeSchedule.AfterPrayer -> {
                    Divider()
                    SettingRow("Which prayer", "It moves with the sun through the year") {
                        Column(verticalArrangement = Arrangement.spacedBy(Scale.space1)) {
                            Prayer.entries.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(Scale.space1)) {
                                    row.forEach { p ->
                                        Choice(p.label, schedule.prayer == p) {
                                            onSchedule(schedule.copy(prayer = p))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Divider()
                    SettingRow("How long after", "Time to finish praying and settle") {
                        Chips(
                            listOf(0 to "At it", 15 to "15 min", 30 to "30 min", 60 to "An hour"),
                            schedule.offsetMinutes,
                        ) { onSchedule(schedule.copy(offsetMinutes = it)) }
                    }
                }

                is NudgeSchedule.AtClockTime -> {
                    Divider()
                    SettingRow("What time", "The same time every day, wherever you are") {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Scale.space1),
                        ) {
                            (4..23).forEach { hour ->
                                val at = LocalTime.of(hour, 0)
                                Choice(shortClock(at), schedule.time.hour == hour) {
                                    onSchedule(NudgeSchedule.AtClockTime(at))
                                }
                            }
                        }
                    }
                }

                is NudgeSchedule.Off -> Unit
            }

            if (schedule is NudgeSchedule.AfterPrayer && wantsLocation(armed)) {
                Divider()
                SettingRow(
                    "Where you are on Earth",
                    "Sunset needs a rough location. It never leaves the phone.",
                ) { Action("Let Wird check", onUseLocation) }
            }
        }

        // ---- listening ----
        Group("Listening") {
            SettingRow(
                title = "Audio quality",
                caption = "Abu Bakr al-Shatri, ${audioQuality.perPageMb}. " +
                    "Downloaded once, then it plays with no signal.",
            ) {
                Chips3(AudioQuality.entries.map { it.label to (it == audioQuality) }) { i ->
                    onAudioQuality(AudioQuality.entries[i])
                }
            }
        }

        // ---- appearance ----
        Group("How it looks") {
            SettingRow("Theme", "Ink is easier at night") {
                Chips3(
                    listOf(
                        "Paper" to (theme == ThemeMode.LIGHT),
                        "Ink" to (theme == ThemeMode.DARK),
                        "Match phone" to (theme == ThemeMode.SYSTEM),
                    )
                ) { i ->
                    onTheme(listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)[i])
                }
            }
        }

        Spacer(Modifier.height(Scale.space6))
        Action("Back to today's portion", onBack)
        Spacer(Modifier.height(Scale.space8))
    }
}

// ---- the pieces this screen is built from ----

/**
 * A titled group of rows.
 *
 * The title sits *outside* the group's surface, small and letterspaced, so the eye reads
 * it as a heading rather than as the first row. This is the piece the old screen was
 * missing entirely.
 */
@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    val colors = LocalWirdColors.current
    Spacer(Modifier.height(Scale.space6))
    Text(
        text = title.uppercase(),
        color = colors.textSecondary,
        style = TextStyle(fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium),
        modifier = Modifier.padding(start = Scale.space2, bottom = Scale.space2),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Scale.radius))
            .background(colors.surfaceRaised.copy(alpha = 0.35f)),
    ) {
        content()
    }
}

/**
 * One setting: what it is, what it does, and the control.
 *
 * The caption is the whole point of the rebuild. Previously the explanation floated under
 * a whole section, so "This changes today's portion too" sat below three unrelated
 * controls. Attached to its own row, it answers the question at the moment you ask it.
 */
@Composable
private fun SettingRow(title: String, caption: String, control: @Composable () -> Unit) {
    val colors = LocalWirdColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(Scale.space4)) {
        Text(
            text = title,
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.body, fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = caption,
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.caption),
        )
        Spacer(Modifier.height(Scale.space3))
        control()
    }
}

@Composable
private fun Divider() {
    val colors = LocalWirdColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Scale.space4)
            .height(1.dp)
            .background(colors.textOutsidePortion.copy(alpha = 0.22f))
    )
}

/** Chips whose value is an Int — amounts, offsets. */
@Composable
private fun Chips(options: List<Pair<Int, String>>, selected: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Scale.space1)) {
        options.forEach { (value, label) ->
            Choice(label, value == selected) { onPick(value) }
        }
    }
}

/** Chips already resolved to label + selected, picked by index. */
@Composable
private fun Chips3(options: List<Pair<String, Boolean>>, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Scale.space1)) {
        options.forEachIndexed { i, (label, on) -> Choice(label, on) { onPick(i) } }
    }
}

@Composable
private fun Action(label: String, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    Text(
        text = label,
        color = colors.accent,
        style = TextStyle(fontSize = Scale.body),
        modifier = Modifier
            .clip(RoundedCornerShape(Scale.radius))
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = Scale.minTarget)
            .padding(vertical = Scale.space3, horizontal = Scale.space2),
    )
}

/**
 * Selection carried by a filled ground rather than by colour alone — the palette is a
 * value ramp, so a "selected" colour would read as the same colour. Also the better
 * pattern regardless: colour is never the only signal.
 */
@Composable
private fun Choice(label: String, on: Boolean, onPick: () -> Unit) {
    val colors = LocalWirdColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Scale.radius))
            .background(if (on) colors.done else Color.Transparent)
            .clickable(onClick = onPick)
            .defaultMinSize(minHeight = Scale.minTarget)
            .padding(horizontal = Scale.space3, vertical = Scale.space2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (on) colors.onSurfaceRaised else colors.textSecondary,
            style = TextStyle(
                fontSize = Scale.body,
                fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
            ),
        )
    }
}

// ---- copy ----

private fun wantsLocation(armed: Armed?): Boolean = when (armed) {
    is Armed.AtFallback -> true
    is Armed.At -> armed.source == PlaceSource.TIMEZONE
    else -> false
}

/**
 * What the reminder is actually doing, in one line.
 *
 * Never prints a prayer time — PROFILE.md § 5. The only clock time here is the fixed-hour
 * fallback, which is a plain hour rather than a computed sunset, and it appears precisely
 * because the reader needs to know the app could not do what they asked.
 */
private fun reminderCaption(schedule: NudgeSchedule, armed: Armed?): String {
    val drift = if (armed.isInexact()) " Android may let it drift a few minutes." else ""
    return when (schedule) {
        is NudgeSchedule.Off -> "Nothing will arrive. Turn it back on whenever you want."
        is NudgeSchedule.AtClockTime -> "${schedule.label()}, every day.$drift"
        is NudgeSchedule.AfterPrayer -> when (armed) {
            is Armed.AtFallback ->
                "Wird can't work out sunset without knowing roughly where you are, so it " +
                    "will come at ${shortClockLong(NudgeScheduler.FALLBACK_TIME)} until it does.$drift"
            else -> "${schedule.label()}.$drift"
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
