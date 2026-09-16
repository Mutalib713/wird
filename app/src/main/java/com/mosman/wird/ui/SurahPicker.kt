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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.Juz
import com.mosman.wird.domain.JuzIndex
import com.mosman.wird.domain.RevealedIn
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.listLabel
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.arabicNumerals

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
    searchQuery: String? = null,
    contentPadding: PaddingValues = PaddingValues(bottom = 100.dp),
) {
    val colors = LocalWirdColors.current
    var internalQuery by remember { mutableStateOf("") }
    val effectiveQuery = searchQuery ?: internalQuery
    val matches = remember(effectiveQuery) { searchSurahs(effectiveQuery) }

    // Null while searching. Computed once per query rather than per row: a surah belongs to
    // the juz' its FIRST page falls in, which is what puts Al-Mu'minun, An-Nur and Al-Furqan
    // together under Juz' 18 exactly as the reference does.
    //
    // **⚠ Every juz' gets a band, including the two that no surah starts in.** Grouping alone
    // produced a list that ran 1, 3, 4, 6 — arithmetically correct, because Al-Baqarah spans
    // juz 1-3 and An-Nisa spans 4-5, so nothing *begins* in juz 2 or 5. It read as a bug, and
    // worse, someone looking for Juz' 2 could not find it. So the list walks all thirty and a
    // juz' with no surah start says which surah you are still inside.
    val grouped: List<Pair<Juz, List<Surah>>>? = remember(effectiveQuery) {
        if (effectiveQuery.isNotBlank()) {
            null
        } else {
            val startsIn = matches.groupBy { JuzIndex.of(it.firstPage).number }
            JuzIndex.all.map { juz -> juz to startsIn[juz.number].orEmpty() }
        }
    }

    Column(modifier = modifier) {
        if (searchQuery == null) {
            OutlinedTextField(
                value = internalQuery,
                onValueChange = { internalQuery = it },
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
        }

        if (matches.isEmpty()) {
            Text(
                text = "Nothing matches \"$effectiveQuery\". Try part of the name, or its number.",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
                modifier = Modifier.padding(horizontal = Scale.space4, vertical = Scale.space3),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
 * A juz' section header in tactile claymorphism.
 * Shows Juz' number on the left and the bare starting page on the right.
 */
@Composable
private fun JuzBand(juz: Juz) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val bandBg = if (isDark) Color(0xFF0F1D17) else Color(0xFFEBE6D8)
    val bandText = if (isDark) Color(0xFF92AA9E) else Color(0xFF384F45)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(bandBg, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Juz' ${juz.number}",
            color = bandText,
            style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        )
        Text(
            text = juz.firstPage.toString(),
            color = bandText,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
        )
    }
}

/**
 * One surah rendered as an inflated 3D claymorphic card.
 *
 * Left: Mint squircle medallion with Arabic numerals (١, ٢, etc.)
 * Center: Sūrah name with meaning - "Al-Fatihah (The Opener)" - and revelation details underneath
 * Right: Bare start page number - "1", "2", "50"
 */
@Composable
private fun SurahRow(surah: Surah, onPick: (Surah) -> Unit) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val bg = if (isDark) Color(0xFF13201A) else Color(0xFFFFFFFF)
    val highlight = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f)
    val shadow = if (isDark) Color.Black.copy(alpha = 0.55f) else Color(0xFF8C7D6B).copy(alpha = 0.22f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clayCard(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = bg,
                highlightColor = highlight,
                shadowColor = shadow,
                elevation = 4.dp,
                strokeWidth = 1.dp,
            )
            .clickable { onPick(surah) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Squircle medallion with Arabic numerals
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clayCard(
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = if (isDark) Color(0xFF1A2B24) else Color(0xFFEDF5F0),
                        highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.85f),
                        shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFFA0B9AA).copy(alpha = 0.35f),
                        elevation = 2.dp,
                        strokeWidth = 1.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = arabicNumerals(surah.number),
                    color = if (isDark) Color(0xFF93DB7A) else Color(0xFF174233),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.listLabel(),
                    color = if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D),
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${revealedLabel(surah)} · ${surah.verses} verses",
                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                    style = TextStyle(fontSize = 12.sp),
                )
            }

            Spacer(Modifier.width(12.dp))

            // Bare page number
            Text(
                text = surah.firstPage.toString(),
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

/** How mushafs label it, rather than the API's "makkah" / "madinah". */
private fun revealedLabel(surah: Surah): String = when (surah.revealedIn) {
    RevealedIn.MAKKI -> "Meccan"
    RevealedIn.MADANI -> "Medinan"
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
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val ongoing = SurahIndex.on(juz.firstPage).lastOrNull()
    val bg = if (isDark) Color(0xFF0E1814) else Color(0xFFF8F5EE)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(
                if (onOpenPage == null) {
                    Modifier
                } else {
                    Modifier.clickable { onOpenPage(juz.firstPage) }
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ongoing?.let { "↳ Still inside ${it.name}" } ?: "↳ Continues",
            color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
            style = TextStyle(fontSize = 12.5.sp),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = juz.firstPage.toString(),
            color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
        )
    }
}
