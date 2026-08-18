package com.mosman.wird.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
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
 * **PROFILE.md § 5g, rebuilt to the design's *after* variant on 2026-08-18 — § 5j.**
 * Mutalib identified the two mockups himself: the *before* is the one with the bottom tab
 * bar and "Good evening, Amina"; the *after* is the one beside it with a toolbar and a top
 * strip. He asked for the after's layout, the before's greeting, and our own colours.
 *
 * **The top strip is deliberately absent.** The after navigates by a `TODAY · SŪRAH · JUZʾ`
 * strip and has no bottom bar at all. Wird keeps the four-tab bottom bar, and running both
 * would put "Sūrah" on the same screen twice. Asked directly; his answer was *"i just want
 * the bottom tabs"*. So this takes the after's order and hierarchy and none of its
 * navigation.
 *
 * The order is the after's own: **who you are, what you are reading, what is being asked,
 * how it has gone, and the week behind you.**
 *
 * **The one number that governs this screen:** § 2 found that 8 of his 14 missed days were
 * procrastination rather than forgetting. A dashboard puts a screen between him and the
 * page, so every element here has to earn that cost — and `OPEN THE PAGE →` is deliberately
 * the loudest thing on it, full width and unmissable. If missed days go up after this
 * lands, this screen is the first suspect.
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
) {
    val colors = LocalWirdColors.current
    var companionReply by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Scale.space4),
    ) {
        // ---- the toolbar ----
        //
        // The after's own header: the app's name, and under it where you currently are.
        // The juz' is not shown here as the design does — Home has the page number but not
        // the juz', which only arrives with the page data itself, and § 10 says a number
        // gets measured rather than guessed.
        Spacer(Modifier.height(Scale.space4))
        Text(
            text = "WIRD",
            color = colors.accent,
            style = TextStyle(fontSize = 13.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold),
        )
        if (positionLabel.isNotEmpty()) {
            Spacer(Modifier.height(Scale.space1))
            Text(
                text = positionLabel.uppercase(),
                color = colors.textOutsidePortion,
                style = TextStyle(fontSize = 10.sp, letterSpacing = 1.2.sp),
            )
        }

        Spacer(Modifier.height(Scale.space6))

        // ---- the date ----
        Text(
            text = todayLine(),
            color = colors.textSecondary,
            style = TextStyle(fontSize = 11.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.height(Scale.space3))

        // ---- the greeting ----
        //
        // The one thing taken from the *before* variant, at his instruction. The after has
        // no greeting at all, which is why it had to be carried across.
        //
        // The name is optional by design — setup offers a Skip — and this falls back to the
        // bare hour rather than inventing "friend", which reads worse than saying nothing.
        Text(
            text = greeting(),
            color = colors.textPrimary,
            style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Normal),
        )
        readerName?.let { name ->
            Text(
                text = name,
                color = colors.accent,
                style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Normal),
            )
        }

        Spacer(Modifier.height(Scale.space6))

        // ---- what you are reading ----
        PortionCard(assignment, doneMethod, onOpenPage)

        // ---- what is being asked ----
        //
        // UNDER the portion, which is where the after puts it too. Mutalib said "the daily
        // portion must come first" twice, and the before variant leads with the companion —
        // one more reason the after's order is the one he picked.
        if (doneMethod == null) {
            Spacer(Modifier.height(Scale.space4))
            Companion(
                question = companionQuestion(),
                shortcuts = listOf("After Isha", "In an hour", "Not today"),
                lastReply = companionReply,
                onReply = { said ->
                    // Understood here, acted on upstairs. The brain is pure and testable;
                    // only MainActivity can actually move a schedule or mark a day.
                    val action = CompanionBrain.understand(said)
                    companionReply = replyTo(action, progress, positionLabel)
                    onCompanionAction(action)
                },
            )
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
 * The last few days, with the page each one covered.
 *
 * **The page column is the after's addition and it is the useful one.** "Yesterday ·
 * recited aloud" tells you that you read; "293" tells you *where*, which is the thing you
 * actually want when you are wondering whether you have drifted.
 *
 * A day whose page was never recorded shows nothing rather than a zero or a dash-shaped
 * apology. Days marked before task 6 started storing `startUnit` are exactly that case.
 */
@Composable
private fun ThisWeek(recent: List<DayLog>, pageFor: (LocalDate) -> Int?) {
    val colors = LocalWirdColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Label("This week")
        Label("Page")
    }
    Spacer(Modifier.height(Scale.space2))

    recent.take(3).forEach { log ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Scale.space2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dayName(log.date),
                    color = colors.textPrimary,
                    style = TextStyle(fontSize = Scale.body),
                )
                Text(
                    text = if (log.method == Method.RECITED) "Recited aloud" else "Marked as read",
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
    }

    Spacer(Modifier.height(Scale.space1))
    Text(
        // Sacred Rule 3, said out loud rather than merely implemented. The second sentence
        // is the after's own wording and it is worth keeping — it names the absence, so an
        // empty row is read as mercy rather than as a bug.
        text = "Days you missed aren't listed. There is no row saying you failed.",
        color = colors.textOutsidePortion,
        style = TextStyle(fontSize = Scale.caption),
    )
}

/**
 * Today's portion, and the way in.
 *
 * `OPEN THE PAGE →` is a full-width filled control rather than a link, because it is the
 * only thing on this screen that leads to the actual reading and the whole app is judged on
 * whether that happens.
 */
@Composable
private fun PortionCard(assignment: Assignment, doneMethod: Method?, onOpenPage: () -> Unit) {
    val colors = LocalWirdColors.current
    val surahs = remember(assignment) { assignment.surahs }
    val name = surahs.firstOrNull()?.name ?: "Page ${assignment.startPage}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Scale.radius))
            .background(colors.surfaceRaised.copy(alpha = 0.3f))
            .padding(Scale.space4),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Label(if (doneMethod == null) "Today's portion" else "Today, done")
                Spacer(Modifier.height(Scale.space2))
                Text(
                    text = name,
                    color = colors.textPrimary,
                    style = TextStyle(fontSize = 26.sp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = portionDetail(assignment, doneMethod),
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }

            // The page as a figure rather than as words in the detail line. This is the
            // after's own move, and it is the better one: the page number is what you
            // glance for, and buried in a sentence it has to be read.
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

        Spacer(Modifier.height(Scale.space4))
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
}

/**
 * The two numbers, plus the split.
 *
 * Sacred Rules 4 and 6 both land here: the streak never appears without the total, a streak
 * of nought is not announced at all, and the split is written out rather than shown as a
 * ratio.
 *
 * **The recited count is no longer a third figure.** The after shows two figures and then
 * the sentence, and it is right — the old layout put "5 RECITED" directly above "5 recited
 * aloud, 18 marked as read", saying the same number twice in two shapes. The sentence is
 * the better of the two, because it is the one that also carries what it is being compared
 * against.
 */
@Composable
private fun Numbers(p: Progress) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Scale.space8),
    ) {
        if (p.currentStreak > 1) Figure(p.currentStreak.toString(), "In a row")
        Figure(p.totalDaysRead.toString(), if (p.totalDaysRead == 1) "Day read" else "Days read")
    }
    Spacer(Modifier.height(Scale.space3))
    Text(
        text = "${p.recitedDays} recited aloud, ${p.tappedDays} marked as read.",
        color = colors.textSecondary,
        style = TextStyle(fontSize = Scale.caption),
    )
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
 * The Hijri date comes from `java.time.chrono.HijrahDate`, which is in the platform from
 * API 26 — this app's floor — so it costs nothing and needs no table of month names typed
 * from memory, which is exactly the kind of Islamic data this project refuses to guess at.
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
 * "One page · not yet marked", the after's own shape for this line.
 *
 * The page number has left this sentence and become a figure beside it, so what is left is
 * the amount and the state. When a portion runs across two pages the span is still spelled
 * out here, because the figure can only show where it starts.
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
    progress: com.mosman.wird.domain.Progress?,
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
