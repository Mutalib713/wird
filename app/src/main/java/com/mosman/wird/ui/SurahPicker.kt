package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mosman.wird.domain.Juz
import com.mosman.wird.domain.JuzIndex
import com.mosman.wird.domain.RevealedIn
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.listLabel
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * Search and pick a surah.
 *
 * Bundled data, so filtering 114 surahs costs nothing and works with the radio off. Used
 * by setup and by "read something else" — the same question in both places, so the same
 * list rather than two that drift apart.
 */
@Composable
fun SurahList(
    modifier: Modifier = Modifier,
    onPick: (Surah) -> Unit,
    /**
     * Jump straight to a page rather than to a surah's beginning.
     *
     * Needed by the "still inside" row: Juz' 2 starts on page 22, in the middle of
     * Al-Baqarah, and picking the surah would send you to page 2 instead. Null on screens
     * where a bare page means nothing, like setup, and the row is then not tappable.
     */
    onOpenPage: ((Int) -> Unit)? = null,
) {
    val colors = LocalWirdColors.current
    var query by remember { mutableStateOf("") }
    val matches = remember(query) { searchSurahs(query) }

    // Null while searching. Computed once per query rather than per row: a surah belongs to
    // the juz' its FIRST page falls in, which is what puts Al-Mu'minun, An-Nur and Al-Furqan
    // together under Juz' 18 exactly as the reference does.
    //
    // **⚠ Every juz' gets a band, including the two that no surah starts in.** Grouping alone
    // produced a list that ran 1, 3, 4, 6 — arithmetically correct, because Al-Baqarah spans
    // juz 1-3 and An-Nisa spans 4-5, so nothing *begins* in juz 2 or 5. It read as a bug, and
    // worse, someone looking for Juz' 2 could not find it. So the list walks all thirty and a
    // juz' with no surah start says which surah you are still inside.
    val grouped: List<Pair<Juz, List<Surah>>>? = remember(query) {
        if (query.isNotBlank()) {
            null
        } else {
            val startsIn = matches.groupBy { JuzIndex.of(it.firstPage).number }
            JuzIndex.all.map { juz -> juz to startsIn[juz.number].orEmpty() }
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search surah") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Scale.space4),
            // Without this the field draws in Material's default purple — a sixth colour
            // on a five-colour palette, on the first screen of a brand-new install.
            // Sacred Rule 8. Caught on the emulator 2026-08-17; invisible on the dev phone
            // because it was already past setup and never saw this screen again.
            colors = wirdFieldColors(),
        )
        Spacer(Modifier.height(Scale.space2))

        if (matches.isEmpty()) {
            Text(
                text = "Nothing matches \"$query\". Try part of the name, or its number.",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
                modifier = Modifier.padding(horizontal = Scale.space4, vertical = Scale.space3),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (grouped != null) {
                    // Browsing: Juz' bands, the way Quran for Android does it.
                    grouped.forEach { (juz, surahs) ->
                        item(key = "juz-" + juz.number) { JuzBand(juz) }
                        if (surahs.isEmpty()) {
                            item(key = "ongoing-" + juz.number) {
                                StillInside(juz, onOpenPage)
                            }
                        } else {
                            items(surahs, key = { it.number }) { surah ->
                                SurahRow(surah, onPick)
                            }
                        }
                    }
                } else {
                    // Searching: bands would be noise. Four results scattered under four
                    // headings is harder to read than four results.
                    items(matches, key = { it.number }) { surah -> SurahRow(surah, onPick) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahJumpSheet(onDismiss: () -> Unit, onPick: (Surah) -> Unit) {
    val colors = LocalWirdColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(modifier = Modifier.padding(vertical = Scale.space2)) {
            Text(
                text = "Read something else",
                modifier = Modifier.padding(horizontal = Scale.space4),
                color = colors.textPrimary,
                style = TextStyle(fontSize = Scale.title),
            )
            Text(
                text = "Today's portion stays where it is. Nothing here changes it.",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
                modifier = Modifier.padding(horizontal = Scale.space4),
            )
            Spacer(Modifier.height(Scale.space3))
            SurahList(onPick = onPick)
        }
    }
}

/**
 * A juz' section header.
 *
 * ⚠ **Both strings use [textPrimary], and that is measured, not stylistic.** On the light
 * band `#DEE2E6` the secondary grey is **3.99:1** and fails AA; primary is 11.85:1. See
 * PROFILE.md § 6d.
 */
@Composable
private fun JuzBand(juz: Juz) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.band)
            .padding(horizontal = Scale.space4, vertical = Scale.space3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Juz' ${juz.number}",
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.body),
        )
        Text(
            text = juz.firstPage.toString(),
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.body),
        )
    }
}

/**
 * One surah.
 *
 * Built to the reference Mutalib sent, and every part of it was something he asked for:
 * the number in its own column, **the meaning beside the name** — "An-Nisa (The Women)" —
 * where it was revealed and how many verses underneath, and **the start page as a bare
 * number**.
 *
 * *"Just write the number, don't bring the p there"*, and *"like 22 to 49, don't do it like
 * that, just write 22"*. So a range becomes its opening page: Al-Baqarah is 2, Ali 'Imran is
 * 50. The range was honest but it was answering a question nobody asked — you tap a surah to
 * go to its beginning, so where it ends is not the number you need.
 */
@Composable
private fun SurahRow(surah: Surah, onPick: (Surah) -> Unit) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick(surah) }
            .defaultMinSize(minHeight = Scale.minTarget)
            .padding(horizontal = Scale.space4, vertical = Scale.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = surah.number.toString(),
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.body),
            textAlign = TextAlign.End,
            modifier = Modifier.width(32.dp),
        )
        Spacer(Modifier.width(Scale.space3))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = surah.listLabel(),
                color = colors.textPrimary,
                style = TextStyle(fontSize = Scale.body),
            )
            Text(
                text = "${revealedLabel(surah)} · ${surah.verses} verses",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
            )
        }

        Spacer(Modifier.width(Scale.space3))
        Text(
            text = surah.firstPage.toString(),
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.body),
        )
    }
}

/** How mushafs label it, rather than the API's "makkah" / "madinah". */
private fun revealedLabel(surah: Surah): String = when (surah.revealedIn) {
    RevealedIn.MAKKI -> "Makki"
    RevealedIn.MADANI -> "Madani"
}

/**
 * Match on name or number, forgiving the punctuation people leave out: "anam" finds
 * Al-An'am, "baqara" finds Al-Baqarah.
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
 * Text fields in the app's own palette.
 *
 * Material's defaults are purple, which is a sixth colour on a five-colour palette
 * (Sacred Rule 8) and it landed on the very first screen of a new install. Defined once
 * here and used by both fields, so the next one added cannot quietly reintroduce it.
 */
@Composable
fun wirdFieldColors(): TextFieldColors {
    val colors = LocalWirdColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        cursorColor = colors.accent,
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.textOutsidePortion,
        focusedLabelColor = colors.accent,
        unfocusedLabelColor = colors.textSecondary,
        focusedContainerColor = colors.surface,
        unfocusedContainerColor = colors.surface,
    )
}

/**
 * "You are still inside Al-Baqarah."
 *
 * Shown under a juz' that no surah begins in. Two exist: **Juz' 2**, which is all Al-Baqarah,
 * and **Juz' 5**, which is all An-Nisa. Without this the list skipped straight from Juz' 1 to
 * Juz' 3, which is arithmetically honest and reads as a missing row.
 *
 * It jumps to the **juz' opening page**, not the surah's — Juz' 2 begins on page 22, and
 * sending someone to page 2 because that is where Al-Baqarah starts would be answering a
 * question they did not ask.
 */
@Composable
private fun StillInside(juz: Juz, onOpenPage: ((Int) -> Unit)?) {
    val colors = LocalWirdColors.current
    val ongoing = SurahIndex.on(juz.firstPage).lastOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onOpenPage == null) {
                    Modifier
                } else {
                    Modifier.clickable { onOpenPage(juz.firstPage) }
                }
            )
            .defaultMinSize(minHeight = Scale.minTarget)
            .padding(horizontal = Scale.space4, vertical = Scale.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ongoing?.let { "Still in ${it.name}" } ?: "Continues",
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.body),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = juz.firstPage.toString(),
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.body),
        )
    }
}
