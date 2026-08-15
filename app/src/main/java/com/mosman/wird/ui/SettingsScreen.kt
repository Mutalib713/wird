package com.mosman.wird.ui

import androidx.compose.foundation.background
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
import com.mosman.wird.data.ThemeMode
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.ReadingPlan
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import java.time.DayOfWeek

/**
 * Settings.
 *
 * Three things, because three is what he asked for: how it looks, how much a day, and
 * where he is. No account, no sync, no notification preferences yet — those arrive with
 * the features that need them.
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
    onTheme: (ThemeMode) -> Unit,
    onPlan: (ReadingPlan) -> Unit,
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
        Section("Go easier on some days")
        Row(horizontalArrangement = Arrangement.spacedBy(Scale.space1)) {
            DayOfWeek.entries.forEach { day ->
                Choice(
                    label = day.name.take(2).lowercase().replaceFirstChar(Char::titlecase),
                    on = day in overrideDays,
                ) {
                    overrideDays = if (day in overrideDays) overrideDays - day else overrideDays + day
                    push()
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
