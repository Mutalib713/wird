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
import com.mosman.wird.audio.RecitationModel
import com.mosman.wird.data.PlaceSource
import com.mosman.wird.data.DataOnDevice
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.data.ThemeMode
import com.mosman.wird.domain.AwayPeriod
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.NudgeSchedule
import com.mosman.wird.domain.Prayer
import com.mosman.wird.domain.ReadingDirection
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.dayLabel
import com.mosman.wird.domain.label
import com.mosman.wird.nudge.Armed
import com.mosman.wird.nudge.NudgeScheduler
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Which setting is open. Null means the list. */
private enum class Detail { DIRECTION, AMOUNT, LIGHTER, MODE, REMINDER, AUDIO, THEME, DATA }

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
    /** A stretch of days with no reminders, or null. PLAN task 22. */
    away: AwayPeriod?,
    /** Pages on this phone out of 604, and what the whole cache weighs. */
    /** False when Android is silently swallowing every notification this app posts. */
    notificationsOn: Boolean = true,
    onFixNotifications: () -> Unit = {},
    /** The recitation model on this phone, or null. PLAN task 14. */
    model: RecitationModel? = null,
    /** Non-null while one is downloading: bytes done and total. */
    fetchingModel: Pair<Long, Long>? = null,
    /** Null when this phone has no native library at all - 32-bit, or an old build. */
    onGetModel: ((RecitationModel) -> Unit)? = null,
    /** Models already on the phone, so a second one can be tried against the first. */
    downloadedModels: List<RecitationModel> = emptyList(),
    onUseModel: (RecitationModel) -> Unit = {},
    cachedPages: Pair<Int, Long> = 0 to 0L,
    /** Non-null while the whole mushaf is being fetched: done out of total. */
    downloading: Pair<Int, Int>? = null,
    onDownloadAll: () -> Unit = {},
    audioQuality: AudioQuality,
    readingMode: ReadingMode,
    direction: ReadingDirection = ReadingDirection.TOWARDS_NAS,
    onDirection: (ReadingDirection) -> Unit = {},
    /** What is on the phone right now, for the sentence before you decide. */
    onDevice: DataOnDevice?,
    onExport: () -> Unit,
    onDeleteRecordings: () -> Unit,
    /** Set after an export runs, so the screen can say what happened. */
    exportNote: String?,
    onTheme: (ThemeMode) -> Unit,
    onPlan: (ReadingPlan) -> Unit,
    onSchedule: (NudgeSchedule) -> Unit,
    onResume: () -> Unit,
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
            direction = direction,
            overrideDays = overrideDays,
            overrideUnits = overrideUnits,
            positionLabel = positionLabel,
            schedule = schedule,
            away = away,
            notificationsOn = notificationsOn,
            onFixNotifications = onFixNotifications,
            model = model,
            fetchingModel = fetchingModel,
            onGetModel = onGetModel,
            downloadedModels = downloadedModels,
            onUseModel = onUseModel,
            cachedPages = cachedPages,
            downloading = downloading,
            onDownloadAll = onDownloadAll,
            onResume = onResume,
            audioQuality = audioQuality,
            theme = theme,
            onDevice = onDevice,
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

                Detail.DATA -> {
                    Explain(
                        "One file with your day log, your saved ayahs, your check-ins and " +
                            "every recording. Nothing is uploaded to make it."
                    )
                    Action("Export everything", onExport)
                    exportNote?.let { Explain(it) }

                    if ((onDevice?.recordings ?: 0) > 0) {
                        Spacer(Modifier.height(Scale.space4))
                        Explain(
                            "Recordings are the big thing here. Deleting them keeps your " +
                                "record of having recited - only the audio goes."
                        )
                        Action("Delete recordings", onDeleteRecordings)
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

                Detail.DIRECTION -> {
                    Explain(
                        "Most people reading front to back go towards An-Nas. Most people " +
                            "memorising work backwards, towards Al-Baqarah. This decides where " +
                            "tomorrow's portion comes from."
                    )
                    Chips3(
                        listOf(
                            "Upwards, towards Al-Fatihah" to (direction == ReadingDirection.TOWARDS_FATIHAH),
                            "Downwards, towards An-Nas" to (direction == ReadingDirection.TOWARDS_NAS),
                        )
                    ) { i ->
                        onDirection(
                            if (i == 0) ReadingDirection.TOWARDS_FATIHAH else ReadingDirection.TOWARDS_NAS
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
                    Explain("Dark is easier at night. The mushaf page stays readable either way.")
                    Chips3(
                        listOf(
                            "Light" to (theme == ThemeMode.LIGHT),
                            "Dark" to (theme == ThemeMode.DARK),
                            "Match phone" to (theme == ThemeMode.SYSTEM),
                        )
                    ) { i -> onTheme(listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)[i]) }
                }
            }
        }
    }
}

private fun Detail.title(): String = when (this) {
    Detail.DIRECTION -> "Which way you go"
    Detail.AMOUNT -> "How much a day"
    Detail.LIGHTER -> "Lighter days"
    Detail.MODE -> "How you read"
    Detail.REMINDER -> "The reminder"
    Detail.AUDIO -> "Listening"
    Detail.THEME -> "How it looks"
    Detail.DATA -> "Your data"
}

// ---- the list ----

@Composable
private fun SettingsList(
    plan: ReadingPlan,
    readingMode: ReadingMode,
    direction: ReadingDirection,
    overrideDays: Set<DayOfWeek>,
    overrideUnits: Int,
    positionLabel: String,
    schedule: NudgeSchedule,
    away: AwayPeriod?,
    notificationsOn: Boolean,
    onFixNotifications: () -> Unit,
    model: RecitationModel?,
    fetchingModel: Pair<Long, Long>?,
    onGetModel: ((RecitationModel) -> Unit)?,
    downloadedModels: List<RecitationModel>,
    onUseModel: (RecitationModel) -> Unit,
    cachedPages: Pair<Int, Long>,
    downloading: Pair<Int, Int>?,
    onDownloadAll: () -> Unit,
    onResume: () -> Unit,
    audioQuality: AudioQuality,
    theme: ThemeMode,
    onDevice: DataOnDevice?,
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
            // **His instruction, 2026-08-19: this belongs beside the memorise question**, and
            // he is right that they are one decision. Memorisers overwhelmingly work from the
            // back towards Al-Baqarah; readers overwhelmingly go front to back. Asking them
            // together is asking one thing twice from two angles.
            ValueRow("Which way you go", directionLabel(direction)) { onOpen(Detail.DIRECTION) }
            Divider()
            ValueRow("Lighter days", lighterLabel(overrideDays, overrideUnits)) {
                onOpen(Detail.LIGHTER)
            }
            Divider()
            ValueRow("Where you are", positionLabel, onClick = onChangePosition)
        }

        Group("The reminder") {
            // ⚠ **Found on his own phone, 2026-08-19: notifications were OFF for Wird.**
            // POST_NOTIFICATIONS denied and importance=NONE, which means every nudge this app
            // has armed since install fired into nothing. The alarms were real; Android threw
            // the notification away at the last step.
            //
            // **A reminder app that cannot post is broken, not merely quiet**, and it has no
            // way to discover that on its own — posting reports success either way. So the one
            // screen where someone goes to check their reminder says it out loud, first, before
            // the setting they came to adjust.
            if (!notificationsOn) {
                ValueRow(
                    title = "Notifications are off",
                    value = "Reminders can't arrive · fix",
                    onClick = onFixNotifications,
                )
                Divider()
            }
            ValueRow("When it arrives", schedule.label()) { onOpen(Detail.REMINDER) }

            // **A pause has to be visible somewhere you did not have to type.** PLAN task
            // 22: "I'm travelling till Sunday" stops the reminder for days, and a silence
            // with no explanation on screen is indistinguishable from the app being broken
            // - which is exactly what task 15's self-check is built to detect. It ends here
            // too, because a pause you can only undo with the right sentence is a trap.
            if (away != null) {
                Divider()
                ValueRow(
                    "Paused while you're away",
                    "Back ${dayLabel(away.returnsOn, LocalDate.now())} · tap to end",
                    onClick = onResume,
                )
            }
        }

        // ---- what is actually on this phone ----
        //
        // **His instruction, 2026-08-19**, after pointing at how Quran for Android works:
        // *"when you first open the app it downloads the pages for you... for the audio and
        // models the user has to download it themselves."*
        //
        // Wird already fetched pages and audio one at a time as they were needed, which is the
        // right default on Ghanaian data. What it had no way of saying was **"get it all now"**,
        // so anybody about to lose signal could not prepare, and nothing on screen said what was
        // already here. A cache you cannot see is one you cannot trust.
        Group("On this phone") {
            val (pages, bytes) = cachedPages
            val whole = com.mosman.wird.domain.Mushaf.PAGES
            ValueRow(
                title = "Mushaf pages",
                value = if (downloading != null) {
                    "Getting ${downloading.first} of ${downloading.second}…"
                } else {
                    "$pages of $whole · ${megabytes(bytes)}"
                },
                onClick = if (downloading == null && pages < whole) onDownloadAll else ({}),
            )
            if (downloading == null && pages < whole) {
                Explain(
                    "Pages arrive as you read them. Tap to fetch the whole mushaf now — about " +
                        "${(whole - pages) * 154 / 1024} MB, best on wifi, and then it reads offline."
                )
            }

            // ---- the recitation model ----
            //
            // **His terms, 2026-08-19:** *"for the audio and models the user has to download it
            // themselves"*, and asked which size, *"offer both, let each person choose"*. So
            // both sit here with their weight on the label, and neither is fetched for anyone.
            //
            // ⚠ **The row is absent entirely when the phone cannot run it**, rather than
            // present and disabled. A 32-bit handset gets no native library, and offering a
            // 42 MB download that could never work is worse than not mentioning it.
            if (onGetModel != null) {
                Divider()
                if (fetchingModel != null) {
                    val (done, total) = fetchingModel
                    ValueRow(
                        title = "Recitation checker",
                        value = if (total > 0) {
                            "Downloading, ${done * 100 / total}% · see the notification"
                        } else {
                            "Downloading ${done / 1024 / 1024} MB · see the notification"
                        },
                        onClick = {},
                    )
                } else {
                    // **Every model, downloaded or not, in one list.** Tapping one that is here
                    // switches to it; tapping one that is not fetches it. That is what makes
                    // the two comparable, which is the whole point of offering both.
                    RecitationModel.entries.forEach { option ->
                        val here = option in downloadedModels
                        ValueRow(
                            title = option.label,
                            value = when {
                                option == model -> "in use"
                                here -> "on this phone · tap to use"
                                else -> "${option.megabytes} MB · download"
                            },
                            onClick = { if (here) onUseModel(option) else onGetModel(option) },
                        )
                    }
                    Explain(
                        if (model == null) {
                            "Optional. Download one and Wird can listen to what you recited, " +
                                "on the phone, with nothing uploaded. Best on wifi."
                        } else {
                            "It listens on the phone and nothing is uploaded. It hears the " +
                                "words; it is not a judge of tajweed."
                        }
                    )
                }
            }
        }

        Group("Listening") {
            ValueRow("Audio quality", "${audioQuality.label} · ${audioQuality.perPageMb}") {
                onOpen(Detail.AUDIO)
            }
        }

        Group("How it looks") {
            ValueRow("Theme", themeLabel(theme)) { onOpen(Detail.THEME) }
        }

        // **PLAN task 19.** Sacred Rule 1 promises nothing leaves the phone; this is the other
        // half of that promise, because private and trapped are otherwise the same thing.
        Group("Your data") {
            ValueRow("Export everything", dataLabel(onDevice)) { onOpen(Detail.DATA) }
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
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
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

/** Bytes as something a person can weigh a download against. */
private fun megabytes(bytes: Long): String =
    if (bytes < 1024L * 1024L) "${bytes / 1024} KB"
    else String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / 1024.0 / 1024.0)

/** His words on screen; the code's names say where you end up. See [ReadingDirection]. */
private fun directionLabel(direction: ReadingDirection): String = when (direction) {
    ReadingDirection.TOWARDS_FATIHAH -> "Upwards, towards Al-Fatihah"
    ReadingDirection.TOWARDS_NAS -> "Downwards, towards An-Nas"
}

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

/** "3 recordings, 1.2 MB" - or the honest nothing, when there is nothing yet. */
private fun dataLabel(d: DataOnDevice?): String {
    if (d == null) return "Checking…"
    if (d.days == 0 && d.recordings == 0) return "Nothing recorded yet"
    val days = if (d.days == 1) "1 day" else "${d.days} days"
    if (d.recordings == 0) return days
    val mb = d.recordingBytes / 1024.0 / 1024.0
    val size = if (mb < 1) "${d.recordingBytes / 1024} KB" else String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
    val recs = if (d.recordings == 1) "1 recording" else "${d.recordings} recordings"
    return "$days · $recs · $size"
}
