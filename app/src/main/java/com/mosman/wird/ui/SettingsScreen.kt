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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
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
import com.mosman.wird.data.ReadingMode
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

/** Which setting is open. Null means the list. */
private enum class Detail { AMOUNT, LIGHTER, MODE, REMINDER, AUDIO, THEME }

/**
 * Settings.
 *
 * **Second rebuild, 2026-08-17**, to the design's *after* pattern — Mutalib compared both
 * and said "that one is way better". He is right, and the difference is structural.
 *
 * The first rebuild fixed the real complaint (it was unreadable, because nothing separated
 * one section from the next) but left every control inline, so the screen was long: five
 * groups of chips is something you scroll rather than scan.
 *
 * **This version collapses each setting to one row showing its current value**, with a
 * chevron to drill in and change it:
 *
 * > **Lighter days**
 * > `FRIDAY AND SUNDAY · HALF A PAGE`  ›
 *
 * The value *is* the caption. You take in the whole of settings at a glance and see what
 * everything is currently set to; only the thing you came to change costs a tap. It is what
 * iOS and Android settings do, and it is why they stay legible at forty rows where an
 * all-inline screen stops being legible at eight.
 *
 * The controls themselves are unchanged from the first rebuild — they moved into the detail
 * screens rather than being rewritten.
 */
@Composable
fun SettingsScreen(
    theme: ThemeMode,
    plan: ReadingPlan,
    positionLabel: String,
    schedule: NudgeSchedule,
    armed: Armed?,
    audioQuality: AudioQuality,
    readingMode: ReadingMode,
    onTheme: (ThemeMode) -> Unit,
    onPlan: (ReadingPlan) -> Unit,
    onSchedule: (NudgeSchedule) -> Unit,
    onAudioQuality: (AudioQuality) -> Unit,
    onReadingMode: (ReadingMode) -> Unit,
    onUseLocation: () -> Unit,
    onChangePosition: () -> Unit,
    onBack: () -> Unit,
) {
    var detail by remember { mutableStateOf<Detail?>(null) }
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

    when (val open = detail) {
        null -> SettingsList(
            plan = plan,
            readingMode = readingMode,
            overrideDays = overrideDays,
            overrideUnits = overrideUnits,
            positionLabel = positionLabel,
            schedule = schedule,
            audioQuality = audioQuality,
            theme = theme,
            onOpen = { detail = it },
            onChangePosition = onChangePosition,
            onBack = onBack,
        )

        else -> DetailScreen(title = open.title(), onClose = { detail = null }) {
            when (open) {
                Detail.AMOUNT -> {
                    Explain("How much of the mushaf today's portion covers.")
                    Chips(
                        listOf(1 to "Half a page", 2 to "One page", 4 to "Two pages"),
                        plan.defaultUnits,
                    ) { push(default = it) }
                    Explain("This changes today's portion too, not just tomorrow's.")
                }

                Detail.LIGHTER -> {
                    Explain("Pick the days that are heavier for you, and give them less.")
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
                    if (overrideDays.isNotEmpty()) {
                        Spacer(Modifier.height(Scale.space6))
                        Explain("How much on those days")
                        Chips(listOf(1 to "Half a page", 2 to "One page"), overrideUnits) {
                            overrideUnits = it; push()
                        }
                    }
                }

                Detail.MODE -> {
                    Explain(
                        "Changes what the buttons are called. Reciting aloud is still how " +
                            "a day gets marked, either way."
                    )
                    Chips3(
                        listOf(
                            "From the mushaf" to (readingMode == ReadingMode.READING),
                            "From memory" to (readingMode == ReadingMode.MEMORISING),
                        ),
                    ) { i ->
                        onReadingMode(
                            if (i == 0) ReadingMode.READING else ReadingMode.MEMORISING
                        )
                    }
                }

                Detail.REMINDER -> {
                    Explain(reminderCaption(schedule, armed))
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

                    when (schedule) {
                        is NudgeSchedule.AfterPrayer -> {
                            Spacer(Modifier.height(Scale.space6))
                            Explain("Which prayer")
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
                            Spacer(Modifier.height(Scale.space6))
                            Explain("How long after")
                            Chips(
                                listOf(0 to "At it", 15 to "15 min", 30 to "30 min", 60 to "An hour"),
                                schedule.offsetMinutes,
                            ) { onSchedule(schedule.copy(offsetMinutes = it)) }

                            if (wantsLocation(armed)) {
                                Spacer(Modifier.height(Scale.space6))
                                Explain("Sunset needs a rough location. It never leaves the phone.")
                                Action("Let Wird check where I am", onUseLocation)
                            }
                        }

                        is NudgeSchedule.AtClockTime -> {
                            Spacer(Modifier.height(Scale.space6))
                            Explain("What time")
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

                        is NudgeSchedule.Off -> Unit
                    }
                }

                Detail.AUDIO -> {
                    Explain(
                        "Abu Bakr al-Shatri. Downloaded once, then it plays with no signal " +
                            "at all — so the only cost is the first listen."
                    )
                    Chips3(AudioQuality.entries.map { it.label to (it == audioQuality) }) { i ->
                        onAudioQuality(AudioQuality.entries[i])
                    }
                    Spacer(Modifier.height(Scale.space4))
                    Explain(AudioQuality.entries.joinToString("   ") { "${it.label}: ${it.perPageMb}" })
                }

                Detail.THEME -> {
                    Explain("Ink is easier at night. The mushaf page stays as it is either way.")
                    Chips3(
                        listOf(
                            "Paper" to (theme == ThemeMode.LIGHT),
                            "Ink" to (theme == ThemeMode.DARK),
                            "Match phone" to (theme == ThemeMode.SYSTEM),
                        )
                    ) { i -> onTheme(listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)[i]) }
                }
            }
        }
    }
}

private fun Detail.title(): String = when (this) {
    Detail.AMOUNT -> "How much a day"
    Detail.LIGHTER -> "Lighter days"
    Detail.MODE -> "How you read"
    Detail.REMINDER -> "The reminder"
    Detail.AUDIO -> "Listening"
    Detail.THEME -> "How it looks"
}

// ---- the list ----

@Composable
private fun SettingsList(
    plan: ReadingPlan,
    readingMode: ReadingMode,
    overrideDays: Set<DayOfWeek>,
    overrideUnits: Int,
    positionLabel: String,
    schedule: NudgeSchedule,
    audioQuality: AudioQuality,
    theme: ThemeMode,
    onOpen: (Detail) -> Unit,
    onChangePosition: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalWirdColors.current
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

        Group("Reading") {
            ValueRow("How much a day", amountLabel(plan.defaultUnits)) { onOpen(Detail.AMOUNT) }
            Divider()
            ValueRow("How you read", modeLabel(readingMode)) { onOpen(Detail.MODE) }
            Divider()
            ValueRow("Lighter days", lighterLabel(overrideDays, overrideUnits)) {
                onOpen(Detail.LIGHTER)
            }
            Divider()
            ValueRow("Where you are", positionLabel, onClick = onChangePosition)
        }

        Group("The reminder") {
            ValueRow("When it arrives", schedule.label()) { onOpen(Detail.REMINDER) }
        }

        Group("Listening") {
            ValueRow("Audio quality", "${audioQuality.label} · ${audioQuality.perPageMb}") {
                onOpen(Detail.AUDIO)
            }
        }

        Group("How it looks") {
            ValueRow("Theme", themeLabel(theme)) { onOpen(Detail.THEME) }
        }

        Spacer(Modifier.height(Scale.space6))
        Action("Back to today's portion", onBack)
        Spacer(Modifier.height(Scale.space8))
    }
}

/**
 * One row: what it is, what it is currently set to, and a way in.
 *
 * The value line is small-caps rather than sentence case so it reads as *state* rather than
 * as an instruction — you are being told the setting, not asked something.
 */
@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = Scale.minTarget)
            .padding(Scale.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = TextStyle(fontSize = Scale.body, fontWeight = FontWeight.Medium),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value.uppercase(),
                color = colors.textSecondary,
                style = TextStyle(fontSize = 11.5.sp, letterSpacing = 0.7.sp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            // The row is already labelled by its title and value; naming the chevron too
            // would make TalkBack read a third thing that means nothing on its own.
            contentDescription = null,
            tint = colors.textOutsidePortion,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun DetailScreen(title: String, onClose: () -> Unit, body: @Composable () -> Unit) {
    val colors = LocalWirdColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Scale.space4),
    ) {
        Spacer(Modifier.height(Scale.space4))
        Row(
            modifier = Modifier
                .clickable(onClick = onClose)
                .defaultMinSize(minHeight = Scale.minTarget)
                .padding(vertical = Scale.space2, horizontal = Scale.space1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to settings",
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(0.dp))
            Text(
                text = "  Settings",
                color = colors.accent,
                style = TextStyle(fontSize = Scale.body),
            )
        }
        Spacer(Modifier.height(Scale.space4))
        Text(title, color = colors.textPrimary, style = TextStyle(fontSize = Scale.display))
        Spacer(Modifier.height(Scale.space6))
        body()
        Spacer(Modifier.height(Scale.space8))
    }
}

// ---- value summaries ----

private fun amountLabel(units: Int): String = when (units) {
    1 -> "Half a page"
    2 -> "One page"
    4 -> "Two pages"
    else -> "$units half-pages"
}

/** "Friday and Sunday · half a page", or "None" — the shape the design used. */
private fun lighterLabel(days: Set<DayOfWeek>, units: Int): String {
    if (days.isEmpty()) return "None"
    val names = DayOfWeek.entries.filter { it in days }.map { d ->
        d.name.lowercase().replaceFirstChar(Char::titlecase)
    }
    val list = when (names.size) {
        1 -> names[0]
        2 -> "${names[0]} and ${names[1]}"
        else -> names.dropLast(1).joinToString(", ") + " and " + names.last()
    }
    return "$list · ${amountLabel(units).lowercase()}"
}

private fun themeLabel(theme: ThemeMode): String = when (theme) {
    ThemeMode.LIGHT -> "Paper"
    ThemeMode.DARK -> "Ink"
    ThemeMode.SYSTEM -> "Match phone"
}

// ---- pieces ----

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
    ) { content() }
}

@Composable
private fun Explain(text: String) {
    val colors = LocalWirdColors.current
    Text(
        text = text,
        color = colors.textSecondary,
        style = TextStyle(fontSize = Scale.caption),
        modifier = Modifier.padding(bottom = Scale.space3),
    )
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

@Composable
private fun Chips(options: List<Pair<Int, String>>, selected: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Scale.space1)) {
        options.forEach { (value, label) -> Choice(label, value == selected) { onPick(value) } }
    }
}

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
 * value ramp, so a "selected" colour would read as the same colour.
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

private fun reminderCaption(schedule: NudgeSchedule, armed: Armed?): String {
    val drift = if (armed.isInexact()) " Android may let it drift a few minutes." else ""
    return when (schedule) {
        is NudgeSchedule.Off -> "Nothing will arrive. Turn it back on whenever you want."
        is NudgeSchedule.AtClockTime -> "${schedule.label()}, every day.$drift"
        is NudgeSchedule.AfterPrayer -> when (armed) {
            is Armed.AtFallback ->
                "Wird can't work out sunset without knowing roughly where you are, so it " +
                    "will come at ${shortClockLong(NudgeScheduler.FALLBACK_TIME)} until it does.$drift"
            else -> "It follows the sun, so it stays right all year.$drift"
        }
    }
}

private fun Armed?.isInexact(): Boolean = when (this) {
    is Armed.At -> !exact
    is Armed.AtFallback -> !exact
    else -> false
}

private fun shortClock(time: LocalTime): String {
    val hour = if (time.hour % 12 == 0) 12 else time.hour % 12
    return "$hour${if (time.hour < 12) "am" else "pm"}"
}

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

/** "From the mushaf" / "From memory", for the settings value row. */
private fun modeLabel(mode: ReadingMode): String = when (mode) {
    ReadingMode.READING -> "From the mushaf"
    ReadingMode.MEMORISING -> "From memory"
}
