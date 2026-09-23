package com.mosman.wird.ui

import android.graphics.Typeface
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mosman.wird.domain.JuzIndex
import com.mosman.wird.domain.RevealedIn
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.domain.arabicName
import com.mosman.wird.domain.toArabicDigits
import com.mosman.wird.mushaf.Glyph
import com.mosman.wird.mushaf.MushafPage
import com.mosman.wird.mushaf.PageState
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill

val LocalAyahTextSize = androidx.compose.runtime.compositionLocalOf { 24f }
val LocalCustomAyahSizeEnabled = androidx.compose.runtime.compositionLocalOf { false }

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
    lit: (MushafPage) -> Set<String>,
    modifier: Modifier = Modifier,
    pageNumber: Int = 1,
    showSkeleton: Boolean = true,
    highlightPortion: Boolean = true,
    onRetry: () -> Unit = {},
    onWordTap: ((verseKey: String) -> Unit)? = null,
    onBackgroundTap: (() -> Unit)? = null,
    reciting: String? = null,
    selected: String? = null,
    selectedVerses: Set<String> = emptySet(),
    review: Set<String> = emptySet(),
    onWordLongPress: ((String) -> Unit)? = null,
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
            is PageState.Failed -> PageProblemCapsule(pageNumber, state, onRetry)
            is PageState.Ready -> {
                LaunchedEffect(state.page.page) { onPageShown(state.page) }
                DrawnPage(
                    page = state.page,
                    typeface = state.typeface,
                    bismillahTypeface = state.bismillahTypeface,
                    lit = lit(state.page),
                    highlightPortion = highlightPortion,
                    reciting = reciting,
                    review = review,
                    selected = selected,
                    selectedVerses = selectedVerses,
                    onWordTap = onWordTap,
                    onWordLongPress = onWordLongPress,
                    onBackgroundTap = onBackgroundTap,
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
    lit: Set<String>,
    highlightPortion: Boolean,
    reciting: String?,
    selected: String?,
    review: Set<String>,
    onWordTap: ((String) -> Unit)?,
    onWordLongPress: ((String) -> Unit)?,
    onBackgroundTap: (() -> Unit)?,
    footer: @Composable (MushafPage) -> Unit,
    selectedVerses: Set<String> = emptySet(),
) {
    val colors = LocalWirdColors.current
    val family = remember(typeface) { FontFamily(typeface) }
    val lines = page.lines
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { com.mosman.wird.data.WirdStore(context) }
    val customEnabled = LocalCustomAyahSizeEnabled.current || store.customAyahTextSizeEnabled
    val currentAyahSize = if (LocalCustomAyahSizeEnabled.current) LocalAyahTextSize.current else store.ayahTextSize.toFloat()
    val verticalPagePad = if (customEnabled && currentAyahSize > 24f) Scale.space4 else 6.dp

    val surahsStartingBeforeLine = remember(page) {
        page.surahStarts.keys.mapNotNull { key ->
            val s = key.substringBefore(':').toIntOrNull() ?: return@mapNotNull null
            val a = key.substringAfter(':').toIntOrNull() ?: 1
            val line = page.lineOf(s, a) ?: page.lines.firstOrNull() ?: 1
            val surah = SurahIndex.byNumber(s) ?: return@mapNotNull null
            line to surah
        }.groupBy({ it.first }, { it.second })
    }

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
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onBackgroundTap?.invoke() },
            )
            .padding(horizontal = Scale.space4, vertical = verticalPagePad),
    ) {
        // Quiet corner headers (Surah left, Juz' right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Scale.space2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Surah $surahLabel",
                color = colors.textSecondary,
                style = TextStyle(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                ),
            )
            if (page.juz > 0) {
                Text(
                    text = "Juz' ${page.juz}",
                    color = colors.textSecondary,
                    style = TextStyle(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    ),
                )
            }
        }

        // Today's Wird portion indicator (only when this page contains today's reading)
        if (lit.isNotEmpty()) {
            val headline = portionHeadline(page, lit, surahLabel)
            val subline = portionSubline(page, lit)
            Row(
                modifier = Modifier
                    .padding(bottom = Scale.space3)
                    .clayPill(
                        shape = RoundedCornerShape(999.dp),
                        backgroundColor = if (isDark) Color(0xFF162620) else Color(0xFFF1EDE1),
                        elevation = 1.dp,
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF8ED676) else Color(0xFF245847))
                )
                Text(
                    text = "$subline · $headline",
                    color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                    style = TextStyle(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                )
            }
        }

        Spacer(Modifier.height(Scale.space2))

        Column(
            modifier = Modifier
                .then(
                    if (page.page <= 2) {
                        Modifier.widthIn(max = 330.dp).align(Alignment.CenterHorizontally)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            lines.forEach { line ->
                val surahsHere = surahsStartingBeforeLine[line]
                if (!surahsHere.isNullOrEmpty()) {
                    surahsHere.forEach { surah ->
                        Spacer(Modifier.height(Scale.space2))
                        SurahBannerClay(surah = surah)
                        Spacer(Modifier.height(Scale.space2))
                        if (surah.number != 9 && page.bismillahCodes != null && bismillahTypeface != null) {
                            val bismillahInPortion = if (highlightPortion) ("${surah.number}:1" in lit) else true
                            Bismillah(page.bismillahCodes, bismillahTypeface, bismillahInPortion)
                            Spacer(Modifier.height(Scale.space3))
                        }
                    }
                } else if (line == bismillahBeforeLine && surahsStartingBeforeLine.isEmpty() && page.bismillahCodes != null && bismillahTypeface != null) {
                    Spacer(Modifier.height(Scale.space4))
                    val surahNum = page.glyphs.firstOrNull()?.verseKey?.substringBefore(':')?.toIntOrNull() ?: 1
                    val bismillahInPortion = if (highlightPortion) ("$surahNum:1" in lit) else true
                    Bismillah(page.bismillahCodes, bismillahTypeface, bismillahInPortion)
                    Spacer(Modifier.height(Scale.space4))
                }
                MushafLine(
                    glyphs = page.glyphsOn(line),
                    reciting = reciting,
                    review = review,
                    selected = selected,
                    selectedVerses = selectedVerses,
                    family = family,
                    litVerses = lit,
                    highlightPortion = highlightPortion,
                    onWordTap = onWordTap,
                    onWordLongPress = onWordLongPress,
                    onBackgroundTap = onBackgroundTap,
                )
            }
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
@JvmName("surahLabelForVerses")
fun surahLabelFor(page: MushafPage, lit: Set<String>): String {
    val surahNum = lit.firstOrNull()?.substringBefore(':')?.toIntOrNull()
        ?: page.glyphs.firstOrNull()?.verseKey?.substringBefore(':')?.toIntOrNull()
    return surahNum?.let { SurahIndex.byNumber(it)?.name } ?: page.surahName
}

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
private fun portionHeadline(page: MushafPage, lit: Set<String>, surahLabel: String): String {
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
private fun portionSubline(page: MushafPage, lit: Set<String>): String {
    val n = page.ayahCount(lit)
    return when (n) {
        0 -> "Today's Wird"
        1 -> "Today's Wird, 1 ayah"
        else -> "Today's Wird, $n ayahs"
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

    val context = LocalContext.current
    val store = remember(context) { com.mosman.wird.data.WirdStore(context) }
    val customEnabled = LocalCustomAyahSizeEnabled.current || store.customAyahTextSizeEnabled
    val currentAyahSize = if (LocalCustomAyahSizeEnabled.current) LocalAyahTextSize.current else store.ayahTextSize.toFloat()
    val scaleMultiplier = if (customEnabled) (currentAyahSize / 24f).coerceIn(0.7f, 1.5f) else 1.0f
    val baseLineSize = 23.sp

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Bismillah" },
        contentAlignment = Alignment.Center,
    ) {
        val available = with(density) { maxWidth.toPx() }
        val fitted = remember(codes, available, baseLineSize) {
            val natural = measurer.measure(
                text = AnnotatedString(codes),
                style = TextStyle(fontFamily = family, fontSize = baseLineSize),
                softWrap = false,
            ).size.width.toFloat()
            val target = available * 0.72f
            if (natural > target && natural > 0f) baseLineSize * (target / natural)
            else baseLineSize
        }
        val finalFontSize = remember(fitted, scaleMultiplier) { fitted * scaleMultiplier }
        Text(
            text = codes,
            color = if (inPortion) colors.textPrimary else colors.textOutsidePortion,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(fontFamily = family, fontSize = finalFontSize),
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MushafLine(
    glyphs: List<Glyph>,
    family: FontFamily,
    litVerses: Set<String>,
    highlightPortion: Boolean,
    reciting: String?,
    selected: String?,
    review: Set<String>,
    onWordTap: ((String) -> Unit)?,
    onWordLongPress: ((String) -> Unit)?,
    onBackgroundTap: (() -> Unit)?,
    selectedVerses: Set<String> = emptySet(),
) {
    val colors = LocalWirdColors.current
    val lineInPortion = if (highlightPortion) glyphs.any { it.verseKey in litVerses } else true
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val context = LocalContext.current
    val store = remember(context) { com.mosman.wird.data.WirdStore(context) }
    val customEnabled = LocalCustomAyahSizeEnabled.current || store.customAyahTextSizeEnabled
    val currentAyahSize = if (LocalCustomAyahSizeEnabled.current) LocalAyahTextSize.current else store.ayahTextSize.toFloat()
    val scaleMultiplier = if (customEnabled) (currentAyahSize / 24f).coerceIn(0.7f, 1.5f) else 1.0f
    val lineVerticalPad = if (customEnabled && currentAyahSize > 24f) Scale.space1 else 0.5.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = lineVerticalPad)
            .semantics {
                contentDescription =
                    if (lineInPortion) "Line of today's portion" else "Line outside today's portion"
            },
    ) {
        val baseLineSize = 23.sp
        val available = with(density) { maxWidth.toPx() }
        // Measure each glyph individually so natural width reflects exactly what the
        // separate Text composables in the SpaceBetween Row measure, with no kerning
        // or ligature discrepancies from a joined run.
        val natural = remember(glyphs, family, baseLineSize) {
            glyphs.sumOf { g ->
                measurer.measure(
                    text = AnnotatedString(g.code),
                    style = TextStyle(fontFamily = family, fontSize = baseLineSize),
                    softWrap = false,
                ).size.width
            }.toFloat()
        }

        val baseFitted = remember(natural, available, density, glyphs.size, baseLineSize) {
            val minGap = with(density) { 3.dp.toPx() }
            val count = glyphs.size
            val target = if (count > 1) {
                minOf(available * 0.92f, available - (count - 1) * minGap)
            } else {
                available * 0.85f
            }
            if (natural > target && natural > 0f) baseLineSize * (target / natural)
            else baseLineSize
        }
        val finalFontSize = remember(baseFitted, scaleMultiplier) { baseFitted * scaleMultiplier }

        // **One band behind the run, not a patch per word.**
        var band by remember(glyphs, reciting, selected, review, selectedVerses) {
            mutableStateOf<Pair<Float, Float>?>(null)
        }
        val washColor = when {
            glyphs.any { it.verseKey == reciting } -> colors.highlightReciting
            glyphs.any { it.verseKey == selected || it.verseKey in selectedVerses } -> colors.highlightSelected
            glyphs.any { it.verseKey in review } -> colors.highlightReview
            else -> Color.Transparent
        }

        val maxGapPx = with(density) { 18.dp.toPx() }
        val count = glyphs.size
        val spreadGap = if (count > 1) (available - natural) / (count - 1) else 0f
        val isWideSpacing = count > 1 && spreadGap > maxGapPx

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier
                    .then(
                        if (isWideSpacing || count == 1) {
                            Modifier.wrapContentWidth().align(Alignment.Center)
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    )
                    .drawBehind {
                        val b = band ?: return@drawBehind
                        val pad = 4.dp.toPx()
                        drawRoundRect(
                            color = washColor,
                            topLeft = Offset(b.first - pad, -pad),
                            size = Size(
                                width = (b.second - b.first) + pad * 2,
                                height = size.height + pad * 2,
                            ),
                            cornerRadius = CornerRadius(3.dp.toPx()),
                        )
                    },
                horizontalArrangement = when {
                    isWideSpacing -> Arrangement.spacedBy(10.dp)
                    count == 1 -> Arrangement.Center
                    else -> Arrangement.SpaceBetween
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                glyphs.forEach { g ->
                    val inPortion = if (highlightPortion) (g.verseKey in litVerses) else true
                    val bodyColor: Color = if (inPortion) colors.textPrimary else colors.textOutsidePortion
                    val lit = g.verseKey == reciting || g.verseKey == selected ||
                        g.verseKey in selectedVerses || g.verseKey in review
                    Text(
                        text = g.code,
                        color = bodyColor,
                        maxLines = 1,
                        softWrap = false,
                        style = TextStyle(fontFamily = family, fontSize = finalFontSize),
                        modifier = Modifier
                            .then(
                                if (lit) {
                                    // Report where this word landed so the row can paint one
                                    // band behind the whole run. See `band` above.
                                    Modifier.onGloballyPositioned { c ->
                                        val left = c.positionInParent().x
                                        val right = left + c.size.width
                                        val cur = band
                                        band = if (cur == null) {
                                            left to right
                                        } else {
                                            minOf(cur.first, left) to maxOf(cur.second, right)
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .then(
                                if (onWordTap == null && onWordLongPress == null) {
                                    Modifier
                                } else {
                                    Modifier
                                        .combinedClickable(
                                            // ⚠ **A glyph consumes the tap it is given**, so
                                            // an empty onClick does not fall through to the
                                            // page behind it - it swallows it. That is
                                            // exactly what happened when long-press was
                                            // added: on the reading page onWordTap is null,
                                            // every word ate the tap, and the chrome bar
                                            // stopped appearing. Found on the emulator, not
                                            // by reading this.
                                            //
                                            // So a word with no tap action of its own hands
                                            // the tap to the background handler by name.
                                            onClick = {
                                                if (onWordTap != null) {
                                                    onWordTap(g.verseKey)
                                                } else {
                                                    onBackgroundTap?.invoke()
                                                }
                                            },
                                            onLongClick = onWordLongPress?.let {
                                                { it(g.verseKey) }
                                            },
                                        )
                                }
                            ),
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
private fun PageProblemCapsule(
    pageNumber: Int,
    state: PageState.Failed,
    onRetry: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)

    Box(modifier = Modifier.fillMaxSize()) {
        // Ghost skeleton page in background
        PageSkeleton()

        // Floating Capsule at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .clayCard(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = if (isDark) Color(0xFF13231B) else Color(0xFFFFFFFF),
                    highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.95f),
                    shadowColor = if (isDark) Color.Black.copy(alpha = 0.55f) else Color(0xFF8C7D6B).copy(alpha = 0.22f),
                    elevation = 4.dp,
                )
                .padding(16.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        DataSignalVectorIcon(tint = gold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Connection timed out",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = state.reason.ifEmpty { "Couldn't load page $pageNumber. Check connection." },
                    color = colors.textSecondary,
                    fontSize = 12.5.sp,
                )

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF245847) else Color(0xFF2D6B52),
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    DownloadVectorIcon(tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Retry",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** Vector cellular data icon matching Sacred Rule 6. */
@Composable
private fun DataSignalVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val barWidth = w * 0.16f
        val gap = w * 0.08f
        val heights = listOf(0.35f, 0.55f, 0.75f, 0.95f)
        heights.forEachIndexed { i, frac ->
            val left = i * (barWidth + gap)
            val barH = h * frac
            drawRoundRect(
                color = tint,
                topLeft = Offset(left, h - barH),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(1.5.dp.toPx()),
            )
        }
    }
}

/** Vector download icon matching Sacred Rule 6. */
@Composable
private fun DownloadVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        // Arrow line
        drawLine(tint, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.65f), stroke.width, stroke.cap)
        // Arrow head
        drawLine(tint, Offset(w * 0.28f, h * 0.45f), Offset(w * 0.5f, h * 0.65f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.72f, h * 0.45f), Offset(w * 0.5f, h * 0.65f), stroke.width, stroke.cap)
        // Tray
        val tray = Path().apply {
            moveTo(w * 0.20f, h * 0.75f)
            lineTo(w * 0.20f, h * 0.88f)
            lineTo(w * 0.80f, h * 0.88f)
            lineTo(w * 0.80f, h * 0.75f)
        }
        drawPath(tray, color = tint, style = stroke)
    }
}

/**
 * Tactile Surah Header Banner.
 *
 * Drawn at the start of a Surah (e.g. Page 1 for Al-Fatihah, Page 2 for Al-Baqarah).
 * Displays chapter calligraphy, chapter number seal, and revelation info in a tactile clay card.
 */
@Composable
fun SurahBannerClay(
    surah: Surah,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val gold = Color(0xFFC9A24B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Scale.space2)
            .clayCard(
                shape = RoundedCornerShape(14.dp),
                backgroundColor = if (isDark) Color(0xFF1F2026) else Color(0xFFF9F6EE),
                elevation = 2.dp,
                highlightColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.6f),
                shadowColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.12f),
                strokeWidth = 1.dp,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left column: English revelation & verse count
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = if (surah.revealedIn == RevealedIn.MADANI) "MADANIYYAH" else "MAKKIYYAH",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = gold,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${surah.verses} Verses · Surah ${surah.number}",
                    style = TextStyle(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary,
                    ),
                )
            }

            // Right row: Arabic Calligraphic Name + Number Seal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = "سُورَةُ ${surah.arabicName}",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        ),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clayPill(
                            shape = CircleShape,
                            backgroundColor = if (isDark) Color(0xFF2A2824) else Color(0xFFF4EDE0),
                            elevation = 1.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = toArabicDigits(surah.number),
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = gold,
                        ),
                    )
                }
            }
        }
    }
}

