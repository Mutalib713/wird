package com.mosman.wird.ui

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.mushaf.Glyph
import com.mosman.wird.mushaf.MushafPage
import com.mosman.wird.mushaf.PageState
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * One mushaf page, drawn.
 *
 * Shared by the reading screen and by setup, because "show me the page and let me point
 * at where I am" is the same view as "here is today's portion" with different lines lit.
 *
 * [lit] decides which lines are at full strength; everything else steps back. Pass every
 * line to show a page plainly, as setup does.
 */
@Composable
fun MushafPageView(
    state: PageState,
    lit: (MushafPage) -> Set<Int>,
    modifier: Modifier = Modifier,
    /**
     * Whether a wait on this page should be dressed as a skeleton.
     *
     * False after the reader has already seen one. The skeleton's job is to say "this is
     * loading, not broken" — it only needs saying once. Someone who has swiped past the
     * prefetched pages has decided to browse, and repeating the placeholder at every page
     * turn is a flicker, not information.
     */
    showSkeleton: Boolean = true,
    onRetry: () -> Unit = {},
    onWordTap: ((verseKey: String) -> Unit)? = null,
    /** Tapping the page itself, used to show and hide the chrome. */
    onBackgroundTap: (() -> Unit)? = null,
    /** Fires when a page is drawn, so the chrome can name where you are. */
    /**
     * The ayah being recited right now, or null when nothing is playing.
     *
     * Sacred Rule 5 says the portion is marked by everything *else* stepping back rather
     * than by painting a wash over the Qur'an, so this follows the same mechanic one level
     * down: while audio plays, the ayah you are hearing keeps full ink and the rest of the
     * portion recedes to the same slate used for text outside today's reading. No new
     * colour is introduced, and both pairs are already contrast-checked (16.68:1 and
     * 3.72:1). A tint was not an option regardless — deep teal on ink is 1.78:1 and sage on
     * paper is 1.69:1, measured when the ayah numerals were tried and rejected.
     */
    reciting: String? = null,
    onPageShown: (MushafPage) -> Unit = {},
    footer: @Composable (MushafPage) -> Unit = {},
) {
    val colors = LocalWirdColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.page)
            .then(
                if (onBackgroundTap == null) {
                    Modifier
                } else {
                    // No ripple. A grey flash spreading across the Qur'an every time you
                    // tap the screen is exactly the kind of decoration this page refuses.
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBackgroundTap,
                    )
                }
            ),
    ) {
        when (state) {
            // Nothing at all the second time. Plain paper, then the page arrives.
            is PageState.Loading -> if (showSkeleton) PageSkeleton()
            is PageState.Failed -> PageProblem(state, onRetry)
            is PageState.Ready -> {
                LaunchedEffect(state.page.page) { onPageShown(state.page) }
                DrawnPage(
                    page = state.page,
                    typeface = state.typeface,
                    bismillahTypeface = state.bismillahTypeface,
                    lit = lit(state.page),
                    reciting = reciting,
                    onWordTap = onWordTap,
                    footer = footer,
                )
            }
        }
    }
}

@Composable
private fun DrawnPage(
    page: MushafPage,
    typeface: Typeface,
    bismillahTypeface: Typeface?,
    lit: Set<Int>,
    reciting: String?,
    onWordTap: ((String) -> Unit)?,
    footer: @Composable (MushafPage) -> Unit,
) {
    val colors = LocalWirdColors.current
    val family = remember(typeface) { FontFamily(typeface) }
    val lines = page.lines

    // Where a surah begins on this page, so the bismillah is drawn in front of it rather
    // than at the top of the sheet. Page 440's words jump from line 3 to line 6, and that
    // gap is the surah banner and the bismillah.
    val bismillahBeforeLine = remember(page) {
        page.surahStarts.keys
            .mapNotNull { key ->
                val s = key.substringBefore(':').toIntOrNull() ?: return@mapNotNull null
                val a = key.substringAfter(':').toIntOrNull() ?: return@mapNotNull null
                page.lineOf(s, a)
            }
            .minOrNull()
            ?: page.lines.firstOrNull()
    }

    val surahLabel = remember(lit, page) { surahLabelFor(page, lit) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Scale.space4, vertical = Scale.space6),
    ) {
        // What to read, sized like the most important thing on the screen — because it is.
        //
        // Three attempts to get here. First a titled block with an accent bar and a rule
        // beside every line, which Mutalib called furniture and was right about. Then the
        // range tucked into a corner at caption size next to the juz, which he could not
        // see at all — I had styled the whole point of the app like a footnote.
        //
        // This is the middle: no new structure, no decoration, just the right size and
        // weight, with a quiet line under it saying what it is. The juz moved to the
        // chrome bar, which frees the corner and stops navigation competing with the task.
        if (lit.isNotEmpty()) {
            Text(
                text = portionHeadline(page, lit, surahLabel),
                color = colors.textPrimary,
                style = TextStyle(fontSize = Scale.title),
            )
            Text(
                text = portionSubline(page, lit),
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
            )
        } else {
            Text(
                text = surahLabel,
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
            )
        }

        Spacer(Modifier.height(Scale.space6))

        lines.forEach { line ->
            if (line == bismillahBeforeLine && page.bismillahCodes != null && bismillahTypeface != null) {
                Spacer(Modifier.height(Scale.space4))
                Bismillah(page.bismillahCodes, bismillahTypeface, line in lit)
                Spacer(Modifier.height(Scale.space4))
            }
            MushafLine(
                glyphs = page.glyphsOn(line),
                reciting = reciting,
                family = family,
                inPortion = line in lit,
                onWordTap = onWordTap,
            )
        }

        Spacer(Modifier.height(Scale.space6))
        footer(page)
        Spacer(Modifier.height(Scale.space4))

        // Centred at the foot, where a printed mushaf puts it, and in the quietest colour
        // on the screen because it is the thing you need least often.
        Text(
            text = page.page.toString(),
            color = colors.textOutsidePortion,
            style = TextStyle(fontSize = Scale.caption, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Scale.space2))
    }
}

/**
 * Which surah to call this page, given what is lit on it.
 *
 * **Never `page.surahName`.** That field is the surah of the page's *first* verse, and a
 * page can open with the tail of the previous surah: page 440 begins with Fatir's last
 * ayah and only then starts Ya-Sin. Naming the page from it put "Fatir" in the chrome bar
 * while the page itself said "Ya-Sin 4–12" — the same bug Mutalib reported in setup,
 * reappearing somewhere new because the rule lived in one composable instead of a shared
 * function. It is a shared function now.
 */
fun surahLabelFor(page: MushafPage, lit: Set<Int>): String {
    val line = lit.minOrNull() ?: page.lines.firstOrNull()
    return line?.let { page.surahNumberOn(it) }
        ?.let { SurahIndex.byNumber(it)?.name }
        ?: page.surahName
}

/**
 * The one line of chrome: which surah you are looking at, and — when part of this page is
 * today's — exactly which ayahs to read.
 *
 * Naming the ayahs is what removes the guesswork. Colour says *where*, and now says it
 * the same way on every page; the words say *what*, and survive being screenshotted,
 * being colour-blind, or simply not having learned the convention yet.
 */
private fun portionHeadline(page: MushafPage, lit: Set<Int>, surahLabel: String): String {
    val (first, last) = page.ayahRange(lit) ?: return surahLabel
    val firstName = SurahIndex.byNumber(first.first)?.name ?: surahLabel
    val lastName = SurahIndex.byNumber(last.first)?.name ?: surahLabel
    return when {
        first == last -> "$firstName ${first.second}"
        first.first == last.first -> "$firstName ${first.second}–${last.second}"
        else -> "$firstName ${first.second} to $lastName ${last.second}"
    }
}

/**
 * The quiet line under it. Says *whose* reading this is, and how big — "9 ayahs" answers
 * the question a range does not: is this a lot, or is this nothing?
 */
private fun portionSubline(page: MushafPage, lit: Set<Int>): String {
    val n = page.ayahCount(lit)
    return when (n) {
        0 -> "Today's portion"
        1 -> "Today's portion, 1 ayah"
        else -> "Today's portion, $n ayahs"
    }
}

/**
 * The bismillah, centred on its own line, the way the mushaf opens a surah.
 *
 * Centred rather than justified: it is one short phrase, not a full measure, and
 * stretching it across the width would be a typographic lie about how the page is set.
 */
@Composable
private fun Bismillah(codes: String, typeface: Typeface, inPortion: Boolean) {
    val colors = LocalWirdColors.current
    val family = remember(typeface) { FontFamily(typeface) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Bismillah" },
        contentAlignment = Alignment.Center,
    ) {
        val available = with(density) { maxWidth.toPx() }
        val fitted = remember(codes, available) {
            val natural = measurer.measure(
                text = AnnotatedString(codes),
                style = TextStyle(fontFamily = family, fontSize = Scale.mushafLine),
                softWrap = false,
            ).size.width.toFloat()
            val target = available * 0.72f
            if (natural > target && natural > 0f) Scale.mushafLine * (target / natural)
            else Scale.mushafLine
        }
        Text(
            text = codes,
            color = if (inPortion) colors.textPrimary else colors.textOutsidePortion,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(fontFamily = family, fontSize = fitted),
        )
    }
}

/**
 * One line of the mushaf.
 *
 * Right-to-left, with the words pushed apart to fill the measure — that spacing *is* the
 * justification in a QCF page, which is why each line is a Row of words rather than one
 * Text with a justify flag.
 *
 * Each line is measured and shrunk to fit its own width. A mushaf line is a fixed set of
 * words that must sit on one line: it cannot wrap and it cannot be clipped, because
 * either one loses Qur'anic text. So the type size bends and the line survives whole.
 */
@Composable
private fun MushafLine(
    glyphs: List<Glyph>,
    family: FontFamily,
    inPortion: Boolean,
    reciting: String?,
    onWordTap: ((String) -> Unit)?,
) {
    val colors = LocalWirdColors.current
    // While a recitation is playing, the ayah being heard is the only thing at full ink
    // and the rest of the portion steps back — the same mechanic that separates today's
    // reading from the rest of the page, applied one level down. Nothing is painted over
    // the text and no new colour enters the palette. Sacred Rule 5.
    val recitingHere = reciting != null && glyphs.any { it.verseKey == reciting }
    val bodyColor: Color = when {
        !inPortion -> colors.textOutsidePortion
        reciting == null -> colors.textPrimary
        recitingHere -> colors.textPrimary
        else -> colors.textOutsidePortion
    }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Scale.space1)
            .semantics {
                contentDescription =
                    if (inPortion) "Line of today's portion" else "Line outside today's portion"
            },
    ) {
        val available = with(density) { maxWidth.toPx() }
        val joined = remember(glyphs) { glyphs.joinToString("") { it.code } }

        val fitted = remember(joined, available) {
            val natural = measurer.measure(
                text = AnnotatedString(joined),
                style = TextStyle(fontFamily = family, fontSize = Scale.mushafLine),
                softWrap = false,
            ).size.width.toFloat()
            // Leave room: SpaceBetween still needs gaps between the words.
            val target = available * 0.94f
            if (natural > target && natural > 0f) Scale.mushafLine * (target / natural)
            else Scale.mushafLine
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                glyphs.forEach { g ->
                    // No accent on the ayah numerals. It was tried and measured: deep
                    // teal against ink is 1.78:1 and sage against paper is 1.69:1, so the
                    // "marked" numeral was the same colour as the text around it. The
                    // dimming already says where the portion ends, plainly.
                    Text(
                        text = g.code,
                        // Per glyph, not per line: a mushaf line often carries the end of
                        // one ayah and the start of the next, and marking the whole line
                        // would light words nobody is reciting.
                        color = if (reciting != null && inPortion && g.verseKey != reciting) {
                            colors.textOutsidePortion
                        } else {
                            bodyColor
                        },
                        maxLines = 1,
                        softWrap = false,
                        style = TextStyle(fontFamily = family, fontSize = fitted),
                        modifier = if (onWordTap == null) {
                            Modifier
                        } else {
                            Modifier
                                .clickable { onWordTap(g.verseKey) }
                                // A word is smaller than a fingertip. The row is already
                                // tall enough; this widens the target sideways so tapping
                                // a one-letter word is not a game of accuracy.
                                .padding(horizontal = 2.dp)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PageSkeleton() {
    val colors = LocalWirdColors.current
    // Matches the final geometry: fifteen lines, same rhythm, so nothing jumps on arrival.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Scale.space4, vertical = Scale.space6),
    ) {
        Spacer(Modifier.height(Scale.space8))
        repeat(15) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (it % 3 == 0) 0.92f else 1f)
                    .height(Scale.mushafLine.value.dp)
                    .padding(vertical = Scale.space1)
                    .background(colors.textOutsidePortion.copy(alpha = 0.12f)),
            )
            Spacer(Modifier.height(Scale.space2))
        }
    }
}

@Composable
private fun PageProblem(state: PageState.Failed, onRetry: () -> Unit) {
    val colors = LocalWirdColors.current
    Column(
        modifier = Modifier.fillMaxSize().padding(Scale.space6),
        verticalArrangement = Arrangement.spacedBy(Scale.space3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(state.reason, color = colors.textPrimary, style = TextStyle(fontSize = Scale.body))
        // Sacred Rule 2, stated to the reader rather than only in a comment.
        Text(
            text = "The page needs its own font to be shown correctly, so Wird won't " +
                "guess at it with another one.",
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.caption),
        )
        if (state.retryable) {
            TextButton(
                onClick = onRetry,
                modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
            ) {
                Text("Fetch the page again", color = colors.accent)
            }
        }
    }
}
