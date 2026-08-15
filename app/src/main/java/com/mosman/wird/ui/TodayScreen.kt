package com.mosman.wird.ui

import android.graphics.Typeface
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.mosman.wird.domain.Assignment
import com.mosman.wird.domain.linesOn
import com.mosman.wird.domain.pages
import com.mosman.wird.mushaf.Glyph
import com.mosman.wird.mushaf.MushafPage
import com.mosman.wird.mushaf.MushafRepository
import com.mosman.wird.mushaf.PageState
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * The Today screen. The mushaf page is the surface; the app is a thin margin around it.
 *
 * Signature move — **the page is never painted on.** No wash, no coloured band. Today's
 * portion is marked twice over: everything outside it steps back to slate, and the ayah
 * numerals inside it take the accent. On a full page that is the entire accent budget,
 * spent on about eight small numerals.
 */
@Composable
fun TodayScreen(assignment: Assignment) {
    val context = LocalContext.current
    val colors = LocalWirdColors.current
    val repo = remember { MushafRepository(context) }
    var state by remember { mutableStateOf<PageState>(PageState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }

    // A one-page portion is one page. A bigger target spans several; this shows the
    // first and says so, rather than pretending the rest is not there.
    val page = assignment.pages.first()

    LaunchedEffect(page, attempt) {
        state = PageState.Loading
        state = repo.load(page)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            // The page must never sit under the status bar or the gesture handle.
            .safeDrawingPadding(),
    ) {
        when (val s = state) {
            is PageState.Loading -> PageSkeleton()
            is PageState.Failed -> PageProblem(s) { attempt++ }
            is PageState.Ready ->
                ReadyPage(s.page, s.typeface, s.bismillahTypeface, assignment)
        }
    }
}

@Composable
private fun ReadyPage(
    page: MushafPage,
    typeface: Typeface,
    bismillahTypeface: Typeface?,
    assignment: Assignment,
) {
    val colors = LocalWirdColors.current
    val family = remember(typeface) { FontFamily(typeface) }
    val lines = page.lines
    // Which of this page's lines are today's. A half-page target lights half of them.
    val portionLines = remember(assignment, lines) { assignment.linesOn(page.page, lines) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Scale.space4, vertical = Scale.space6),
    ) {
        // Chrome: a margin note, not a header bar. Nothing boxed, nothing tinted.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = page.surahName,
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption),
            )
            Text(
                text = if (page.juz > 0) "Juz' ${page.juz}" else "Page ${page.page}",
                color = colors.textOutsidePortion,
                style = TextStyle(fontSize = Scale.caption),
            )
        }

        Spacer(Modifier.height(Scale.space6))

        // Line 1 of a mushaf page that opens a surah. Drawn with its own font — the
        // page font maps the same codepoints to entirely different words.
        if (page.bismillahCodes != null && bismillahTypeface != null) {
            Bismillah(
                codes = page.bismillahCodes,
                typeface = bismillahTypeface,
                // The bismillah belongs to the first line, so it follows its fate.
                inPortion = lines.firstOrNull()?.let { it in portionLines } ?: true,
            )
            Spacer(Modifier.height(Scale.space4))
        }

        lines.forEach { line ->
            MushafLine(
                glyphs = page.glyphsOn(line),
                family = family,
                inPortion = line in portionLines,
            )
        }

        Spacer(Modifier.height(Scale.space6))

        Text(
            text = portionSummary(assignment, portionLines, lines),
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.caption),
            modifier = Modifier.fillMaxWidth(),
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Bismillah" },
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
            if (natural > target && natural > 0f) {
                Scale.mushafLine * (target / natural)
            } else {
                Scale.mushafLine
            }
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
 * words that must sit on one line: it cannot wrap, and it cannot be clipped, because
 * either one loses Qur'anic text. So the type size bends and the line always survives
 * whole. Lines vary in how tight they are, which is why this is per-line rather than one
 * size for the page.
 */
@Composable
private fun MushafLine(
    glyphs: List<Glyph>,
    family: FontFamily,
    inPortion: Boolean,
) {
    val colors = LocalWirdColors.current
    val bodyColor: Color = if (inPortion) colors.textPrimary else colors.textOutsidePortion
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Scale.space1)
            .semantics {
                contentDescription = if (inPortion) {
                    "Line of today's portion"
                } else {
                    "Line outside today's portion"
                }
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
            // Leave a little room: SpaceBetween still needs gaps between the words.
            val target = available * 0.94f
            if (natural > target && natural > 0f) {
                Scale.mushafLine * (target / natural)
            } else {
                Scale.mushafLine
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                glyphs.forEach { g ->
                    // No accent on the ayah numerals. It was tried and measured: deep
                    // teal against ink is 1.78:1 and sage against paper is 1.69:1, so in
                    // both modes the "marked" numeral was the same colour as the text
                    // around it. The dimming already tells you where today's portion
                    // ends, plainly, at a glance. A second marker that nobody can see is
                    // not restraint, it is decoration that failed.
                    Text(
                        text = g.code,
                        color = bodyColor,
                        maxLines = 1,
                        softWrap = false,
                        style = TextStyle(fontFamily = family, fontSize = fitted),
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
        modifier = Modifier
            .fillMaxSize()
            .padding(Scale.space6),
        verticalArrangement = Arrangement.spacedBy(Scale.space3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.reason,
            color = colors.textPrimary,
            style = TextStyle(fontSize = Scale.body),
        )
        // Sacred Rule 2, stated to the user rather than only in a comment.
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

private fun portionSummary(
    assignment: Assignment,
    portion: Set<Int>,
    lines: List<Int>,
): String {
    val rest = assignment.pages.drop(1)
    val here = when {
        portion.isEmpty() -> "Today's portion isn't on this page."
        portion.size == lines.size -> "All of this page is today's."
        else -> "${portion.size} of ${lines.size} lines are today's."
    }
    return when {
        rest.isEmpty() -> here
        rest.size == 1 -> "$here Page ${rest.first()} is today's as well."
        else -> "$here Pages ${rest.first()} to ${rest.last()} are today's as well."
    }
}
