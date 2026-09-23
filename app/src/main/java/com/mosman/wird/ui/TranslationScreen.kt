package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.data.TranslationSource
import com.mosman.wird.data.Translations
import com.mosman.wird.mushaf.MushafRepository
import com.mosman.wird.mushaf.PageState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import com.mosman.wird.ui.theme.clayPill

/**
 * The translation reading mode. **PROFILE.md § 5z.**
 *
 * **Why this is a separate screen rather than English under the mushaf's lines.** The mushaf
 * page is drawn as fixed glyph rows from a per-page QCF font — that is what makes it a real
 * mushaf rather than reflowed text — and inserting a paragraph between two of those rows
 * destroys it. His own reference agrees: Quran for Android's translation view is its own
 * screen too.
 *
 * **The Arabic here is the page's own glyphs, filtered to one ayah.** Wird holds glyph codes
 * for a font, not readable Arabic, so this is the only way to show Qur'anic text without
 * reconstructing it — the same fact that makes `share` send a link rather than words. The
 * font is already on disk for the mushaf view, so this costs no extra data.
 *
 * ⚠ **The lines will not match the printed mushaf here, and that is correct.** A mushaf line
 * is a typesetting fact about a page; an ayah is a unit of meaning. This screen is organised
 * by ayah, so its glyphs wrap wherever the phone's width says. Anyone who wants the printed
 * layout has the mushaf view one tap away.
 */
@Composable
fun TranslationScreen(
    /** The page to read. Loads its own layout and font, the same way the mushaf view does. */
    pageNumber: Int,
    modifier: Modifier = Modifier,
    /** Which sources to show, in order. All three are bundled; § 5z. */
    sources: List<TranslationSource> = TranslationSource.entries,
    /** When false, omit the Arabic ayah glyphs and show only translation text. */
    showAyah: Boolean = true,
    onSelectSources: ((List<TranslationSource>) -> Unit)? = null,
    onBackgroundTap: (() -> Unit)? = null,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val context = LocalContext.current
    val store = remember { Translations(context) }
    val repo = remember { MushafRepository(context) }

    var state by remember(pageNumber) { mutableStateOf<PageState>(PageState.Loading) }
    LaunchedEffect(pageNumber) { state = repo.load(pageNumber) }

    val ready = state as? PageState.Ready
    if (ready == null) {
        Box(
            modifier = modifier.fillMaxSize().background(colors.page),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when (val s = state) {
                    is PageState.Failed -> s.reason
                    else -> "Loading page $pageNumber…"
                },
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.body),
            )
        }
        return
    }

    val page = ready.page
    val family = remember(ready.typeface) { FontFamily(ready.typeface) }

    // source -> (verseKey -> text). Read once per page rather than per row.
    var bySource by remember(page.page) {
        mutableStateOf<Map<TranslationSource, Map<String, String>>>(emptyMap())
    }
    LaunchedEffect(page.page, sources) {
        bySource = sources.associateWith { s ->
            store.page(page.page, s).associate { it.verseKey to it.text }
        }
    }

    // Ayahs in the order they appear on the page, which is reading order.
    val verseKeys = remember(page) { page.glyphs.map { it.verseKey }.distinct() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.page)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = { onBackgroundTap?.invoke() },
            ),
    ) {
        // 1-Tap Translation Source Chips Bar
        if (onSelectSources != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val allSelected = sources.size == TranslationSource.entries.size
                Box(
                    modifier = Modifier
                        .clayPill(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                            backgroundColor = if (allSelected) colors.accent else if (isDark) Color(0xFF1E2822) else Color(0xFFEBE6DC),
                            elevation = if (allSelected) 2.dp else 1.dp,
                        )
                        .clickable { onSelectSources(TranslationSource.entries.toList()) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "All",
                        fontSize = 11.5.sp,
                        fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (allSelected) Color.White else colors.textPrimary,
                    )
                }

                TranslationSource.entries.forEach { source ->
                    val isSelected = sources.size == 1 && sources.first() == source
                    Box(
                        modifier = Modifier
                            .clayPill(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                                backgroundColor = if (isSelected) colors.accent else if (isDark) Color(0xFF1E2822) else Color(0xFFEBE6DC),
                                elevation = if (isSelected) 2.dp else 1.dp,
                            )
                            .clickable { onSelectSources(listOf(source)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = source.label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else colors.textPrimary,
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = Scale.space4,
                vertical = Scale.space4,
            ),
        ) {
            items(verseKeys, key = { it }) { key ->
                VerseBlock(
                    verseKey = key,
                    glyphs = page.glyphs.filter { it.verseKey == key }.joinToString("") { it.code },
                    family = family,
                    lines = sources.mapNotNull { s ->
                        bySource[s]?.get(key)?.let { s to it }
                    },
                    showAyah = showAyah,
                    onTap = { onBackgroundTap?.invoke() },
                )
            }
        }
        Attribution(sources)
    }
}


/**
 * One ayah: its number, its words, then each translation with whose it is.
 *
 * The verse number leads rather than trails, unlike the mushaf where the marker closes the
 * ayah. On a scrolling list you need to know what you are looking at before you read it.
 */
@Composable
private fun VerseBlock(
    verseKey: String,
    glyphs: String,
    family: FontFamily,
    lines: List<Pair<TranslationSource, String>>,
    showAyah: Boolean = true,
    onTap: (() -> Unit)? = null,
) {
    val colors = LocalWirdColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = { onTap?.invoke() },
            )
            .padding(bottom = Scale.space6),
    ) {
        Text(
            text = verseKey,
            color = colors.accent,
            style = TextStyle(fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.height(Scale.space3))

        // The ayah, in the page's own font. Only shown when the user has
        // "Show ayah before translation" enabled in Settings.
        if (showAyah) {
            Text(
                text = glyphs,
                color = colors.textPrimary,
                textAlign = TextAlign.End,
                style = TextStyle(fontFamily = family, fontSize = Scale.mushafLine, lineHeight = 52.sp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        lines.forEach { (source, text) ->
            Spacer(Modifier.height(Scale.space3))
            Text(
                text = source.label.uppercase(),
                color = colors.textOutsidePortion,
                style = TextStyle(fontSize = 9.5.sp, letterSpacing = 1.sp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = text,
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.body, lineHeight = 24.sp),
            )
        }

        Spacer(Modifier.height(Scale.space4))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.ornament.copy(alpha = 0.15f))
        )
    }
}

/**
 * Who translated what, pinned at the foot.
 *
 * **Sacred Rule 2 in its plainest form.** Translations differ, and the differences are
 * theological rather than stylistic. A reader is entitled to know whose reading they have,
 * and this is the one thing on the screen that never scrolls away.
 */
@Composable
private fun Attribution(sources: List<TranslationSource>) {
    val colors = LocalWirdColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised)
            .padding(horizontal = Scale.space4, vertical = Scale.space3),
    ) {
        sources.forEach { s ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = s.label,
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = Scale.caption),
                )
                Text(
                    text = s.by,
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }
        }
    }
}
