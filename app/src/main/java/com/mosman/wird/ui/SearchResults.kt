package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.data.SearchTexts
import com.mosman.wird.domain.QuranSearch
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.VerseIndex
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.clayCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Results shown at a time; "Show more" adds this many again. */
private const val BATCH = 50

/**
 * What the Sūrahs tab's search box finds: sūrahs by name or number first, then every verse that
 * holds the words, in Arabic, English, Hausa or transliteration (PLAN task 26).
 *
 * The verse search waits a moment after each key so it runs once per pause, not once per letter.
 * The index it searches starts building when the search box opens (`SurahsTab`).
 */
@Composable
fun SearchResults(
    query: String,
    onPick: (Surah) -> Unit,
    onOpenPage: (Int) -> Unit,
    showTranslatedName: Boolean,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val surahs = remember(query) { searchSurahs(query).takeIf { it.size < SurahIndex.all.size }.orEmpty() }
    val searchable = remember(query) { QuranSearch.searchable(query) }
    var found by remember { mutableStateOf<Found?>(null) }
    val scope = rememberCoroutineScope()
    var more by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(query) {
        found = null
        if (!searchable) return@LaunchedEffect
        delay(250)
        val matches = SearchTexts.search(context, query)
        found = Found(matches, SearchTexts.hits(context, matches.take(BATCH)))
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (surahs.isNotEmpty()) {
            item(key = "surahs-label") { SectionLabel(if (surahs.size == 1) "1 sūrah" else "${surahs.size} sūrahs") }
            items(surahs, key = { "s" + it.number }) { surah -> SurahRow(surah, onPick, showTranslatedName) }
        }
        val now = found
        item(key = "verses-label") {
            SectionLabel(
                when {
                    !searchable -> "In the verses"
                    now == null -> "Looking through the verses…"
                    now.matches.size == 1 -> "1 verse"
                    else -> "${now.matches.size} verses"
                },
            )
        }
        when {
            !searchable -> item(key = "short") { Note("Type a little more to search inside the verses.") }
            now == null -> Unit
            now.matches.isEmpty() -> item(key = "none") {
                Note("No verse has \"${query.trim()}\". Try fewer letters, or another spelling.")
            }
            else -> {
                items(now.hits, key = { "v${it.key.first}:${it.key.second}" }) { hit ->
                    HitRow(hit, onOpen = { onOpenPage(VerseIndex.pageOf(hit.key)) })
                }
                if (now.matches.size > now.hits.size) {
                    item(key = "more") {
                        ShowMore(remaining = now.matches.size - now.hits.size, onClick = {
                            if (more?.isActive != true) more = scope.launch {
                                val next = now.matches.subList(now.hits.size, minOf(now.hits.size + BATCH, now.matches.size))
                                val read = SearchTexts.hits(context, next)
                                // Unless a new search replaced it while the words were being read.
                                if (found === now) found = Found(now.matches, now.hits + read)
                            }
                        })
                    }
                }
            }
        }
    }
}

/** What a search found: every matching verse, and those shown so far with their words. */
private class Found(val matches: List<QuranSearch.Match>, val hits: List<QuranSearch.Hit>)

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    Text(
        text = text.uppercase(),
        color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
    )
}

@Composable
private fun Note(text: String) {
    val colors = LocalWirdColors.current
    Text(
        text = text,
        color = colors.textSecondary,
        style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

/** One verse that matched: where it is, and its words with the match marked. */
@Composable
private fun HitRow(hit: QuranSearch.Hit, onOpen: () -> Unit) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val surah = SurahIndex.byNumber(hit.key.first)
    val page = VerseIndex.pageOf(hit.key)
    val where = "${surah?.name ?: "Sūrah ${hit.key.first}"} ${hit.key.first}:${hit.key.second} · page $page"
    val arabic = hit.field == QuranSearch.Field.ARABIC
    // Gold from the pinned palette, as a wash behind the matched letters; the ink stays readable.
    val mark = SpanStyle(
        background = Color(0xFFC9A24B).copy(alpha = if (isDark) 0.40f else 0.30f),
        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
        fontWeight = FontWeight.Bold,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clayCard(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = if (isDark) Color(0xFF13201A) else Color(0xFFFFFFFF),
                highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                shadowColor = if (isDark) Color.Black.copy(alpha = 0.55f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                elevation = 4.dp,
                strokeWidth = 1.dp,
            )
            .clickable(onClick = onOpen)
            .semantics { contentDescription = "$where. Opens the page." }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = if (arabic) where else "$where · ${hit.field.label}",
            color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = snippet(hit, mark),
            color = if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D),
            style = if (arabic) {
                TextStyle(fontSize = 20.sp, lineHeight = 36.sp, textAlign = TextAlign.Right, textDirection = TextDirection.Rtl)
            } else {
                TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The verse with its matches marked. A long verse is cut to a window around the first match, so
 * the match is always on screen; 2:282 alone is over a page.
 */
private fun snippet(hit: QuranSearch.Hit, mark: SpanStyle): AnnotatedString {
    val text = hit.text
    val first = hit.highlight.firstOrNull()
    val reach = 90
    var from = 0
    var to = text.length
    if (first != null && text.length > reach * 2 + 20) {
        from = (first.first - reach).coerceAtLeast(0).let { s -> if (s == 0) 0 else text.indexOf(' ', s).let { if (it in s..first.first) it + 1 else s } }
        to = (first.last + 1 + reach).coerceAtMost(text.length).let { e -> if (e == text.length) e else text.lastIndexOf(' ', e).let { if (it > first.last) it else e } }
    }
    return buildAnnotatedString {
        if (from > 0) append("… ")
        val base = length
        append(text.substring(from, to))
        for (r in hit.highlight) {
            val s = r.first - from
            val e = r.last + 1 - from
            if (s >= 0 && e <= to - from) addStyle(mark, base + s, base + e)
        }
        if (to < text.length) append(" …")
    }
}

@Composable
private fun ShowMore(remaining: Int, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .defaultMinSize(minHeight = 48.dp)
            .background(if (isDark) Color(0xFF16251E) else Color(0xFFE4EDE7), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Show ${minOf(remaining, BATCH)} more · $remaining left",
            color = if (isDark) Color(0xFF93DB7A) else Color(0xFF245847),
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
        )
    }
}
