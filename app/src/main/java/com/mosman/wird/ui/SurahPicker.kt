package com.mosman.wird.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import com.mosman.wird.domain.Surah
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
) {
    val colors = LocalWirdColors.current
    var query by remember { mutableStateOf("") }
    val matches = remember(query) { searchSurahs(query) }

    Column(modifier = modifier) {
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
                items(matches, key = { it.number }) { surah -> SurahRow(surah, onPick) }
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
        Column(modifier = Modifier.padding(horizontal = Scale.space4, vertical = Scale.space2)) {
            Text(
                text = "Read something else",
                color = colors.textPrimary,
                style = TextStyle(fontSize = Scale.title),
            )
            Text(
                text = "Today's portion stays where it is. Nothing here changes it.",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
            )
            Spacer(Modifier.height(Scale.space3))
            SurahList(onPick = onPick)
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
