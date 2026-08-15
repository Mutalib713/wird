package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.mushaf.MushafRepository
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * First run.
 *
 * Asks the question a person can answer. Nobody knows they are on page 453; they know
 * they are in Surah Sad, somewhere around ayah 25. So setup takes a surah and an ayah and
 * works the page out itself — the page number is the app's business, not the reader's.
 *
 * The surah list is searchable and comes from the bundled [SurahIndex], so scrolling and
 * filtering 114 surahs costs nothing and works with the radio off. Only the final
 * ayah → page lookup needs the network, and only when the ayah is not 1.
 */
@Composable
fun SetupScreen(
    onDone: (page: Int, unitsPerDay: Int) -> Unit,
) {
    val colors = LocalWirdColors.current
    val context = LocalContext.current
    val repo = remember { MushafRepository(context) }

    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<Surah?>(null) }
    var ayahText by remember { mutableStateOf("") }
    var units by remember { mutableIntStateOf(Mushaf.UNITS_PER_PAGE) }
    var resolvedPage by remember { mutableStateOf<Int?>(null) }
    var ayahCount by remember { mutableStateOf<Int?>(null) }
    var looking by remember { mutableStateOf(false) }

    val matches = remember(query) { searchSurahs(query) }
    val ayah = ayahText.toIntOrNull()
    val ayahOutOfRange = ayah != null && ayahCount != null && ayah !in 1..ayahCount!!

    // Ayah 1 is free — the surah's first page is already known offline.
    LaunchedEffect(chosen, ayahText) {
        val surah = chosen ?: return@LaunchedEffect
        ayahCount = repo.ayahCount(surah.number)
        resolvedPage = when {
            ayahText.isBlank() || ayah == 1 -> surah.firstPage
            ayah == null || ayahOutOfRange -> null
            else -> {
                looking = true
                repo.pageOfVerse(surah.number, ayah).also { looking = false }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .safeDrawingPadding()
            .padding(horizontal = Scale.space6, vertical = Scale.space4),
    ) {
        Text(
            text = "Where are you?",
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.display),
        )
        Spacer(Modifier.height(Scale.space2))

        if (chosen == null) {
            Text(
                text = "The surah you're reading now.",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.body),
            )
            Spacer(Modifier.height(Scale.space3))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search surah") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Scale.space2))

            if (matches.isEmpty()) {
                Text(
                    text = "Nothing matches \"$query\". Try part of the name, or its number.",
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = Scale.caption),
                    modifier = Modifier.padding(vertical = Scale.space3),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(matches, key = { it.number }) { surah ->
                        SurahRow(surah) { chosen = it }
                    }
                }
            }
        } else {
            val surah = chosen!!
            Text(
                text = surah.name,
                color = colors.textPrimary,
                style = TextStyle(fontSize = Scale.title),
            )
            Text(
                text = "Surah ${surah.number} of 114",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
            )
            TextButton(
                onClick = { chosen = null; ayahText = ""; resolvedPage = null },
                modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
            ) {
                Text("Pick a different surah", color = colors.textSecondary)
            }

            Spacer(Modifier.height(Scale.space3))
            Text(
                text = "Which ayah? Leave it blank to start at the beginning.",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.body),
            )
            Spacer(Modifier.height(Scale.space2))
            OutlinedTextField(
                value = ayahText,
                onValueChange = { ayahText = it.filter(Char::isDigit).take(3) },
                label = { Text("Ayah") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = ayahOutOfRange,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Scale.space2))
            Text(
                text = when {
                    ayahOutOfRange -> "${surah.name} has ${ayahCount} ayahs."
                    looking -> "Finding the page"
                    resolvedPage != null -> "That's page $resolvedPage."
                    ayahText.isNotBlank() -> "Couldn't look that up. Check your connection."
                    else -> " "
                },
                color = if (ayahOutOfRange) colors.textPrimary else colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
            )

            Spacer(Modifier.height(Scale.space4))
            Text(
                text = "How much a day?",
                color = colors.textPrimary,
                style = TextStyle(fontSize = Scale.title),
            )
            Spacer(Modifier.height(Scale.space2))
            Row(horizontalArrangement = Arrangement.spacedBy(Scale.space2)) {
                AmountChoice("Half a page", 1, units) { units = it }
                AmountChoice("One page", 2, units) { units = it }
                AmountChoice("Two pages", 4, units) { units = it }
            }

            Spacer(Modifier.height(Scale.space6))
            val page = resolvedPage
            Button(
                onClick = { page?.let { onDone(it, units) } },
                enabled = page != null && !looking,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Scale.minTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.surface,
                ),
            ) {
                Text(
                    if (ayah != null && ayah > 1) {
                        "Start at ${surah.name} ${ayah}"
                    } else {
                        "Start at the beginning of ${surah.name}"
                    }
                )
            }
        }
    }
}

@Composable
private fun SurahRow(surah: Surah, onPick: (Surah) -> Unit) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick(surah) }
            .defaultMinSize(minHeight = Scale.minTarget)
            .padding(vertical = Scale.space3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${surah.number}. ${surah.name}",
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.body),
        )
        Text(
            text = if (surah.firstPage == surah.lastPage) {
                "p. ${surah.firstPage}"
            } else {
                "pp. ${surah.firstPage}–${surah.lastPage}"
            },
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.caption),
        )
    }
}

/**
 * Match on name or number, and forgive the accents and hyphens people leave out —
 * "anam" should find Al-An'am, and "baqara" should find Al-Baqarah.
 */
internal fun searchSurahs(query: String): List<Surah> {
    val q = query.trim().lowercase().filter(Char::isLetterOrDigit)
    if (q.isEmpty()) return SurahIndex.all
    return SurahIndex.all.filter { surah ->
        val name = surah.name.lowercase().filter(Char::isLetterOrDigit)
        name.contains(q) || surah.number.toString() == q
    }
}

/**
 * Selection is shown by a filled ground, not by colour alone.
 *
 * The first version dimmed the unselected labels to slate — 3.72:1 on paper, under the
 * 4.5:1 floor for text this size, so the options you had not picked were the hard ones to
 * read. Colouring the selected one differently instead does not work either: this palette
 * is a value ramp, so ink and deep teal sit 1.78:1 apart and read as the same colour.
 *
 * So both labels stay legible — deep teal at 9.37:1 unselected, ink on sage at 9.90:1
 * selected — and the *ground* carries the state. Which is the better pattern regardless:
 * colour should never be the only signal.
 */
@Composable
private fun AmountChoice(label: String, value: Int, current: Int, onPick: (Int) -> Unit) {
    val colors = LocalWirdColors.current
    val isOn = value == current
    TextButton(
        onClick = { onPick(value) },
        modifier = Modifier
            .defaultMinSize(minHeight = Scale.minTarget)
            .clip(RoundedCornerShape(Scale.radius))
            .background(if (isOn) colors.done else Color.Transparent)
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = label,
            color = if (isOn) colors.onSurfaceRaised else colors.textSecondary,
            style = TextStyle(fontSize = Scale.body),
        )
    }
}
