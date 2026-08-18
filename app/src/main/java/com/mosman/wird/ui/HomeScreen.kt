package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.Assignment
import com.mosman.wird.domain.CompanionAction
import com.mosman.wird.domain.CompanionBrain
import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.Progress
import com.mosman.wird.domain.surahs
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import java.time.LocalDate
import java.time.LocalTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

/**
 * Home — the dashboard.
 *
 * **PROFILE.md § 5g and § 5j.** Rebuilt against the design's *after* variant on 2026-08-18
 * after Mutalib looked at the first port and said it was *"not arranged well, it's not even
 * divided"*. He was right, and the fault was reading the design's **text order** off the
 * page instead of its **structure**. The order was correct; everything holding it apart was
 * missing.
 *
 * **What the design actually does, and what a plain scroll of Text and Spacer cannot:**
 * every part of this screen is a *bounded section*. The toolbar sits on its own raised
 * ground. The portion is a block closed by a hairline. The companion is a bordered card.
 * The week is a tinted header over ruled rows. Sections are separated by edges, not by
 * empty space — which is why the first attempt read as one long column of text.
 *
 * **The top strip is deliberately absent** — the after navigates by a `TODAY · SŪRAH · JUZʾ`
 * strip and has no bottom bar; Wird keeps the four-tab bottom bar. Asked; his answer was
 * *"i just want the bottom tabs"*.
 *
 * **The design's crimson is not ported.** Its recite control is filled `rgb(86,12,21)`,
 * inherited from the Nutcracker poster system the mockup was built on. § 6/8 already ruled
 * on that token: strip what is unused rather than carry the whole set across. Our accent
 * carries it instead.
 *
 * **The one number that governs this screen:** § 2 found 8 of 14 missed days were
 * procrastination. A dashboard puts a screen between him and the page, so `OPEN THE PAGE →`
 * stays full width and unmissable. If missed days rise after this, look here first.
 */
@Composable
fun HomeScreen(
    assignment: Assignment,
    progress: Progress?,
    doneMethod: Method?,
    recent: List<DayLog>,
    onOpenPage: () -> Unit,
    /** What the companion worked out you wanted. MainActivity is what can act on it. */
    onCompanionAction: (CompanionAction) -> Unit = {},
    positionLabel: String = "",
    /** What to call the reader, or null if they skipped the question. */
    readerName: String? = null,
    /** Which page a past day covered, for the week's page column. Null when unrecorded. */
    pageFor: (LocalDate) -> Int? = { null },
    /** Marks today read without leaving Home. The tap route, logged as a tap. */
    onMarkRead: () -> Unit = {},
) {
    val colors = LocalWirdColors.current
    var companionReply by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(rememberScrollState()),
    ) {
        // ---- the toolbar, on its own ground ----
        //
        // Full-bleed and raised, so the app's name and where you are read as chrome rather
        // than as the first paragraph of the page. This is the single biggest thing the
        // first port missed.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceRaised)
                .safeDrawingPadding()
                .padding(horizontal = Scale.space4, vertical = Scale.space3),
        ) {
            Text(
                text = "WIRD",
                color = colors.accent,
                style = TextStyle(fontSize = 13.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold),
            )
            if (positionLabel.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = positionLabel.uppercase(),
                    color = colors.textOutsidePortion,
                    style = TextStyle(fontSize = 10.sp, letterSpacing = 1.2.sp),
                )
            }
        }

        // ---- who you are ----
        Column(modifier = Modifier.padding(horizontal = Scale.space4)) {
            Spacer(Modifier.height(Scale.space6))
            Text(
                text = todayLine(),
                color = colors.textSecondary,
                style = TextStyle(fontSize = 11.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Medium),
            )
            Spacer(Modifier.height(Scale.space3))
            // The one thing taken from the *before* variant, at his instruction — the after
            // has no greeting at all. Falls back to the bare hour when no name was given.
            Text(
                text = greeting(),
                color = colors.textPrimary,
                style = TextStyle(fontSize = 32.sp),
            )
            readerName?.let {
                Text(text = it, color = colors.accent, style = TextStyle(fontSize = 32.sp))
            }
            Spacer(Modifier.height(Scale.space6))
        }

        // ---- what you are reading ----
        PortionSection(assignment, doneMethod, onOpenPage, onMarkRead)

        // ---- what is being asked ----
        if (doneMethod == null) {
            Spacer(Modifier.height(Scale.space4))
            Box(modifier = Modifier.padding(horizontal = Scale.space4)) {
                Companion(
                    question = companionQuestion(),
                    shortcuts = listOf("After Isha", "In an hour", "Not today"),
                    lastReply = companionReply,
                    onReply = { said ->
                        // Understood here, acted on upstairs. The brain is pure and
                        // testable; only MainActivity can move a schedule or mark a day.
                        val action = CompanionBrain.understand(said)
                        companionReply = replyTo(action, progress, positionLabel)
                        onCompanionAction(action)
                    },
                )
            }
        }

        // ---- how it has gone ----
        progress?.takeIf { it.totalDaysRead > 0 }?.let { p ->
            Spacer(Modifier.height(Scale.space6))
            Numbers(p)
        }

        // ---- the week behind you ----
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(Scale.space6))
            ThisWeek(recent, pageFor)
        }

        Spacer(Modifier.height(Scale.space8))
    }
}

/**
 * Today's portion — a bounded section, closed by a hairline.
 *
 * **Not a rounded card.** The first port made it one and it floated; the design closes the
 * block with a rule the full width of the screen, which is what makes the screen read as
 * divided rather than as a stack of boxes.
 *
 * Carries, in the design's own order: the sūrah numeral, the label, the portion, the detail
 * line, how far through the mushaf you are, the three ways to finish, and the way in.
 */
@Composable
private fun PortionSection(
    assignment: Assignment,
    doneMethod: Method?,
    onOpenPage: () -> Unit,
    onMarkRead: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val surahs = remember(assignment) { assignment.surahs }
    val surah = surahs.firstOrNull()
    val name = surah?.name ?: "Page ${assignment.startPage}"

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Scale.space4, vertical = 18.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            // The sūrah's own number, in the numerals the mushaf uses. Cheap, and it is the
            // one piece of Arabic on a screen that is otherwise all chrome.
            surah?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(44.dp)) {
                    Text(
                        text = arabicNumerals(it.number),
                        color = colors.accent,
                        style = TextStyle(fontSize = 22.sp),
                    )
                    Text(
                        text = "SŪRAH",
                        color = colors.textOutsidePortion,
                        style = TextStyle(fontSize = 8.sp, letterSpacing = 0.8.sp),
                    )
                }
                Spacer(Modifier.width(Scale.space3))
            }

            Column(modifier = Modifier.weight(1f)) {
                Label(if (doneMethod == null) "Today's portion" else "Today, done")
                Spacer(Modifier.height(Scale.space1))
                Text(text = name, color = colors.textPrimary, style = TextStyle(fontSize = 26.sp))
                Spacer(Modifier.height(2.dp))
                Text(
                    text = portionDetail(assignment, doneMethod),
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }

            // The page as a figure, not as words inside a sentence. It is glanced at.
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = assignment.startPage.toString(),
                    color = colors.accent,
                    style = TextStyle(fontSize = 26.sp),
                )
                Text(
                    text = "PAGE",
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = 9.sp, letterSpacing = 1.sp),
                )
            }
        }

        // ---- how far through ----
        Spacer(Modifier.height(Scale.space3))
        ThroughTheMushaf(assignment)

        // ---- the three ways to finish ----
        //
        // ⚠ Only "I read it" completes the day from here. Reciting and listening open the
        // page, because the recorder and the player both live there — see § 5j. That is
        // honest rather than ideal: hoisting them is its own change.
        Spacer(Modifier.height(Scale.space4))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Scale.space2),
        ) {
            Pill("Recite it aloud", filled = true, modifier = Modifier.weight(1f), onClick = onOpenPage)
            if (doneMethod == null) {
                Pill("I read it", filled = false, modifier = Modifier.weight(1f), onClick = onMarkRead)
            }
            Pill("Listen", filled = false, modifier = Modifier.weight(1f), onClick = onOpenPage)
        }

        // ---- the way in ----
        Spacer(Modifier.height(Scale.space3))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Scale.radius))
                .background(if (doneMethod == null) colors.accent else colors.done)
                .clickable(onClick = onOpenPage)
                .defaultMinSize(minHeight = Scale.minTarget)
                .padding(vertical = Scale.space3),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (doneMethod == null) "Open the page  →" else "Read it again  →",
                color = if (doneMethod == null) colors.surface else colors.onSurfaceRaised,
                style = TextStyle(fontSize = Scale.body, fontWeight = FontWeight.Medium),
            )
        }
    }

    // The rule that closes the section. Full width, edge to edge.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.ornament.copy(alpha = 0.28f))
    )
}

/**
 * How far through the mushaf you are, as a bar and a sentence.
 *
 * Real arithmetic, not decoration: position over 1,208 half-pages. The design shows
 * "JUZʾ 15 · 41% through" — the juzʾ is omitted because Home does not have it without
 * loading the page itself, and § 10 says a number is measured rather than guessed.
 */
@Composable
private fun ThroughTheMushaf(assignment: Assignment) {
    val colors = LocalWirdColors.current
    val fraction = (assignment.startUnit.toFloat() / Mushaf.TOTAL_UNITS).coerceIn(0f, 1f)
    val percent = (fraction * 100).toInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(colors.ornament.copy(alpha = 0.18f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.accent)
        )
    }
    Spacer(Modifier.height(Scale.space2))
    Text(
        text = "$percent% THROUGH THE MUSHAF",
        color = colors.textOutsidePortion,
        style = TextStyle(fontSize = 9.5.sp, letterSpacing = 1.sp),
    )
}

/** A control shaped the way the design shapes them — a full pill, not a rounded rectangle. */
@Composable
private fun Pill(label: String, filled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    Box(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (filled) {
                    Modifier.background(colors.accent)
                } else {
                    Modifier.border(1.dp, colors.ornament.copy(alpha = 0.45f), CircleShape)
                }
            )
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = Scale.minTarget)
            .padding(horizontal = Scale.space2, vertical = Scale.space3),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (filled) colors.surface else colors.textSecondary,
            style = TextStyle(fontSize = 13.sp, fontWeight = if (filled) FontWeight.Medium else FontWeight.Normal),
        )
    }
}

/**
 * The last few days, with the page each one covered.
 *
 * **Two things the first port left out**, and they are what make this read as a table
 * rather than as loose lines: the header sits in a tinted band, and every row carries a
 * left marker — **filled for a recitation, outlined for a tap.** Sacred Rule 6 says the two
 * are never blurred, and until now the only thing separating them was the words.
 */
@Composable
private fun ThisWeek(recent: List<DayLog>, pageFor: (LocalDate) -> Int?) {
    val colors = LocalWirdColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.ornament.copy(alpha = 0.08f))
            .padding(horizontal = Scale.space4, vertical = Scale.space3),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Label("This week")
        Label("Page")
    }

    recent.take(4).forEach { log ->
        val recited = log.method == Method.RECITED
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Scale.space4, vertical = Scale.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Filled bar for a recitation, hollow for a tap. Colour is never the only
            // signal here — the words underneath still say which it was.
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .then(
                        if (recited) {
                            Modifier.background(colors.accent)
                        } else {
                            Modifier.border(1.dp, colors.ornament.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                        }
                    )
            )
            Spacer(Modifier.width(Scale.space3))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dayName(log.date),
                    color = colors.textPrimary,
                    style = TextStyle(fontSize = Scale.body),
                )
                Text(
                    text = if (recited) "Recited aloud" else "Marked as read",
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }
            pageFor(log.date)?.let { page ->
                Text(
                    text = page.toString(),
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = Scale.body),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.ornament.copy(alpha = 0.1f))
        )
    }

    Spacer(Modifier.height(Scale.space3))
    Text(
        // Sacred Rule 3, said out loud rather than merely implemented.
        text = "Days you missed aren't listed. There is no row saying you failed.",
        color = colors.textOutsidePortion,
        style = TextStyle(fontSize = Scale.caption),
        modifier = Modifier.padding(horizontal = Scale.space4),
    )
}

/**
 * The two numbers, plus the split.
 *
 * Sacred Rules 4 and 6: the streak never appears without the total, a streak of nought is
 * not announced, and the split is written out rather than shown as a ratio.
 *
 * **The recited count is not a third figure.** The design shows two figures and then the
 * sentence, and it is right — the old row put "5 RECITED" directly above "5 recited aloud,
 * 18 marked as read", saying one number twice in two shapes.
 */
@Composable
private fun Numbers(p: Progress) {
    val colors = LocalWirdColors.current
    Column(modifier = Modifier.padding(horizontal = Scale.space4)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (p.currentStreak > 1) {
                Figure(p.currentStreak.toString(), "In a row")
                // The design's own separator between the figures — a gold dot, not a gap.
                Box(
                    modifier = Modifier
                        .padding(horizontal = Scale.space6)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
            }
            Figure(p.totalDaysRead.toString(), if (p.totalDaysRead == 1) "Day read" else "Days read")
        }
        Spacer(Modifier.height(Scale.space3))
        Text(
            text = "${p.recitedDays} recited aloud, ${p.tappedDays} marked as read.",
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.caption),
        )
    }
}

@Composable
private fun Figure(value: String, caption: String) {
    val colors = LocalWirdColors.current
    Column {
        Text(value, color = colors.accent, style = TextStyle(fontSize = 30.sp))
        Text(
            text = caption.uppercase(),
            color = colors.textSecondary,
            style = TextStyle(fontSize = 10.sp, letterSpacing = 1.sp),
        )
    }
}

@Composable
private fun Label(text: String) {
    val colors = LocalWirdColors.current
    Text(
        text = text.uppercase(),
        color = colors.textSecondary,
        style = TextStyle(fontSize = 10.5.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium),
    )
}

// ---- words ----

/** 18 → ١٨. The numerals the mushaf itself uses. */
private fun arabicNumerals(n: Int): String =
    n.toString().map { ARABIC_DIGITS[it - '0'] }.joinToString("")

private val ARABIC_DIGITS = listOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

private fun greeting(): String = when (LocalTime.now().hour) {
    in 0..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

private fun companionQuestion(): String = when (LocalTime.now().hour) {
    in 0..11 -> "Reading this morning?"
    in 12..16 -> "Reading today?"
    else -> "Are you reading tonight?"
}

/**
 * "Thursday · 3 Rabīʿ al-Awwal 1448".
 *
 * The Hijri date comes from `java.time.chrono.HijrahDate`, in the platform since API 26 —
 * this app's floor — so it costs nothing and needs no table of month names typed from
 * memory, which is exactly the kind of Islamic data this project refuses to guess at.
 */
private fun todayLine(): String {
    val today = LocalDate.now()
    val weekday = today.format(DateTimeFormatter.ofPattern("EEEE"))
    val hijri = runCatching {
        val h = HijrahDate.from(today)
        val month = HIJRI_MONTHS[h.get(ChronoField.MONTH_OF_YEAR) - 1]
        "$month ${h.get(ChronoField.YEAR)}"
    }.getOrNull()
    val day = runCatching { HijrahDate.from(today).get(ChronoField.DAY_OF_MONTH) }.getOrNull()
    return if (hijri != null && day != null) "$weekday · $day $hijri" else weekday
}

/** Transliterations, spelled the way they are written in English-language mushafs. */
private val HIJRI_MONTHS = listOf(
    "Muḥarram", "Ṣafar", "Rabīʿ al-Awwal", "Rabīʿ al-Thānī", "Jumādā al-Ūlā",
    "Jumādā al-Ākhirah", "Rajab", "Shaʿbān", "Ramaḍān", "Shawwāl",
    "Dhū al-Qaʿdah", "Dhū al-Ḥijjah",
)

/**
 * "One page · not yet marked", the design's own shape for this line.
 *
 * The page number has left this sentence and become a figure beside it, so what is left is
 * the amount and the state. A portion running across two pages still spells the span out
 * here, because the figure can only show where it starts.
 */
private fun portionDetail(a: Assignment, doneMethod: Method?): String {
    val amount = when (a.units) {
        1 -> "Half a page"
        2 -> "One page"
        else -> "${a.units / 2} pages"
    }
    val span = if (a.startPage == a.endPage) null else "to ${a.endPage}"
    val state = when (doneMethod) {
        Method.RECITED -> "recited aloud"
        Method.TAPPED -> "marked as read"
        null -> "not yet marked"
    }
    return listOfNotNull(amount, span, state).joinToString(" · ")
}

private fun dayName(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE"))
    }
}

/**
 * What the companion says back.
 *
 * **It repeats the commitment in its own words rather than saying "OK".** That is the whole
 * mechanic: "I'll hold you to after Isha" is a thing you can fail to do, and being told your
 * own promise back is what makes it one. "Saved" would make this a form.
 *
 * Sacred Rule 3 governs every line. A refusal gets no argument and no guilt — the app says
 * fine and gets out of the way, because the day someone is told off is the day they delete
 * a habit app.
 */
private fun replyTo(
    action: CompanionAction,
    progress: Progress?,
    positionLabel: String,
): String = when (action) {
    is CompanionAction.CommitTo -> "Alright — I'll ask again ${action.spoken}."
    is CompanionAction.NotToday -> "That's fine. It'll be here tomorrow."
    is CompanionAction.MarkDone -> "Good. Marked as read."
    is CompanionAction.Listen -> "Playing today's portion."
    is CompanionAction.OpenSurah -> "Opening ${action.surah.name}."
    is CompanionAction.WhereAmI ->
        if (positionLabel.isNotEmpty()) "You're at $positionLabel." else "Let me check the page."
    is CompanionAction.HowAmIDoing -> progress?.let { p ->
        val total = if (p.totalDaysRead == 1) "1 day" else "${p.totalDaysRead} days"
        val streak = if (p.currentStreak > 1) "${p.currentStreak} in a row, " else ""
        "$streak$total read — ${p.recitedDays} recited aloud, ${p.tappedDays} marked."
    } ?: "Nothing recorded yet."
    // Says so plainly, and shows what it does know rather than leaving you guessing at the
    // magic words.
    is CompanionAction.NotUnderstood ->
        "I didn't catch that. Try a time (\"after Isha\", \"at 9\"), \"not today\", " +
            "or ask how you're doing."
}
