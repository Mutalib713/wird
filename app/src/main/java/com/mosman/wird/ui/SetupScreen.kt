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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * First run. Two questions, because two is what the app needs to start being useful.
 *
 * Everything else — Friday's lighter portion, the reminder time, reciting — can be
 * learned later. Asking it all now would be a form standing between someone and the
 * thing they came to do.
 */
@Composable
fun SetupScreen(
    initialPage: Int,
    onDone: (page: Int, unitsPerDay: Int) -> Unit,
) {
    val colors = LocalWirdColors.current
    var pageText by remember { mutableStateOf(initialPage.toString()) }
    var units by remember { mutableIntStateOf(Mushaf.UNITS_PER_PAGE) }

    val page = pageText.toIntOrNull()
    val valid = page != null && page in 1..Mushaf.PAGES
    val surah = if (valid) SurahIndex.on(page).firstOrNull() else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .safeDrawingPadding()
            .padding(Scale.space6),
        verticalArrangement = Arrangement.spacedBy(Scale.space4, Alignment.CenterVertically),
    ) {
        Text(
            text = "Where are you?",
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.display),
        )
        Text(
            text = "The page you're on now. You can change it whenever you like.",
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.body),
        )

        OutlinedTextField(
            value = pageText,
            onValueChange = { new -> pageText = new.filter(Char::isDigit).take(3) },
            label = { Text("Page") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = pageText.isNotEmpty() && !valid,
            modifier = Modifier.fillMaxWidth(),
        )

        // Reflect the number back as a place, not a number. 453 means nothing; "Sad"
        // is how you know you typed the right thing.
        Text(
            text = when {
                pageText.isEmpty() -> "Pages run 1 to ${Mushaf.PAGES}."
                !valid -> "There's no page $pageText. The mushaf runs 1 to ${Mushaf.PAGES}."
                surah != null -> "That's ${surah.name}."
                else -> " "
            },
            color = if (pageText.isNotEmpty() && !valid) colors.textPrimary else colors.textSecondary,
            style = TextStyle(fontSize = Scale.caption),
        )

        Spacer(Modifier.height(Scale.space2))

        Text(
            text = "How much a day?",
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.title),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Scale.space2)) {
            AmountChoice("Half a page", 1, units) { units = it }
            AmountChoice("One page", 2, units) { units = it }
            AmountChoice("Two pages", 4, units) { units = it }
        }

        Spacer(Modifier.height(Scale.space4))

        Button(
            onClick = { if (valid) onDone(page, units) },
            enabled = valid,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Scale.minTarget),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.surface,
            ),
        ) {
            Text("Start on page ${page ?: initialPage}")
        }
    }
}

@Composable
private fun AmountChoice(label: String, value: Int, selected: Int, onPick: (Int) -> Unit) {
    val colors = LocalWirdColors.current
    val isOn = value == selected
    TextButton(
        onClick = { onPick(value) },
        modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
    ) {
        Text(
            text = label,
            color = if (isOn) colors.textPrimary else colors.textOutsidePortion,
            style = TextStyle(fontSize = Scale.body),
        )
    }
}
