package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.domain.Assignment
import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.Progress
import com.mosman.wird.domain.Turn
import com.mosman.wird.domain.surahs
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import com.mosman.wird.ui.theme.TileColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

/**
 * Home — the shape of a day.
 *
 * **Rebuilt 2026-08-19 from Mutalib's own comps**, four images with an explicit instruction
 * per image: image 1 is the base, image 2 gives the greeting and the check-in, image 3 gives
 * the numbers, and anything he did not name comes from image 1. His two rulings before a line
 * was written: **the pinned teal stays**, and the comps' warm cream ground and tinted tiles
 * come across. See PROFILE.md § 6e.
 *
 * **§ 5o's rule survives the restyle, in a different costume.** That port failed because it
 * read the design's *strings* and reproduced their order while losing everything that held
 * them apart. The rule it produced — *sections are separated by edges, not by empty space* —
 * still governs here. What changed is which edge: the old screen closed a section with a
 * full-bleed hairline, and the comps close each one inside its own bordered plate. Both are
 * edges. Neither is a gap.
 *
 * ⚠ **The tint on a plate is not the edge.** Measured before building: every tile fill in
 * § 6e is 1.01-1.05:1 against the card behind it, which is nothing at all. A tinted plate
 * with no border is an invisible plate. So every plate here carries a hairline, and every
 * tile carries a hairline plus a coloured icon — the icon is what actually distinguishes them.
 *
 * **Not taken from the comps, deliberately:** their bottom tab bar and their top-right gear.
 * His instruction, and it matches where those already live — § 5t put three tabs at the top
 * and Settings behind the overflow, so porting the comps' chrome would have shipped both twice.
 */
@Composable
fun HomeScreen(
    assignment: Assignment,
    progress: Progress?,
    doneMethod: Method?,
    recent: List<DayLog>,
    onOpenPage: () -> Unit,
    /** The conversation so far. Owned by MainActivity, which also persists it. */
    turns: List<Turn> = emptyList(),
    /** Raw text the reader typed or tapped. Understood and answered upstairs. */
    onSaid: (String) -> Unit = {},
    /** Opens the full conversation. */
    onOpenChat: () -> Unit = {},
    positionLabel: String = "",
    /** What to call the reader, or null if they skipped the question. */
    readerName: String? = null,
    /** Which page a past day covered, for the week's page column. Null when unrecorded. */
    pageFor: (LocalDate) -> Int? = { null },
    /** Marks today read without leaving Home. The tap route, logged as a tap. */
    onMarkRead: () -> Unit = {},
    /** Reading from the mushaf or reciting from memory. Changes labels only - § 5r. */
    mode: ReadingMode = ReadingMode.READING,
    /**
     * Hands today's portion to Quran for Android. **Null when that app is not installed**,
     * and then no button appears at all. PLAN task 12: an action that opens the Play Store
     * instead is an advert wearing a feature's clothes.
     */
    onOpenInQuran: (() -> Unit)? = null,
) {
    val colors = LocalWirdColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Scale.space4),
    ) {
        Spacer(Modifier.height(Scale.space4))

        // ---- who you are, and when ----
        //
        // The app's name and the overflow live in the shared bar since § 5t. What is left
        // here describes today rather than the app.
        if (positionLabel.isNotEmpty()) {
            Text(
                text = positionLabel.uppercase(),
                color = colors.textSecondary,
                style = TextStyle(fontSize = 10.sp, letterSpacing = 1.2.sp),
            )
            Spacer(Modifier.height(Scale.space2))
        }
        Text(
            text = todayLine(),
            color = colors.textSecondary,
            style = TextStyle(fontSize = 12.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.Medium),
        )

        Spacer(Modifier.height(Scale.space3))

        // **The greeting, taken whole from his image 2**, which is the one thing he named
        // twice: two lines, the name underneath in the accent, and a sun beside it. The
        // earlier Home had the same words at the same size with nothing to the right of
        // them, and the difference between those two screens is entirely the sun.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // ⚠ **The comma and the second line both belong to the name.** Built without
            // that condition it rendered "Good morning," with a dangling comma over an empty
            // line of 36sp, which is the gap that showed up on the emulator. A name is
            // optional by § 5k - setup offers a Skip - so the no-name case is a real state,
            // not an edge case, and it has to look deliberate.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (readerName != null) "${greeting()}," else greeting(),
                    color = colors.textPrimary,
                    style = TextStyle(fontSize = 30.sp, lineHeight = 36.sp),
                )
                readerName?.let {
                    Text(
                        text = it,
                        color = colors.accent,
                        style = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium),
                    )
                }
            }
            DayMark()
        }

        Spacer(Modifier.height(Scale.space6))

        PortionCard(assignment, doneMethod, onOpenPage, onMarkRead, mode, onOpenInQuran)

        // ---- what is being asked ----
        //
        // His image 2 captions this "only visible until you finish today", which is what the
        // screen already did. Sacred Rule 3: someone who has read today does not need asking
        // whether they are going to.
        if (doneMethod == null) {
            Spacer(Modifier.height(Scale.space4))
            Companion(
                question = companionQuestion(),
                turns = turns,
                shortcuts = listOf("After Isha", "In an hour", "Not today"),
                onReply = onSaid,
                onOpenChat = onOpenChat,
            )
        }

        // ⚠ **Both of these used to disappear entirely until you had read a day**, which is
        // how a brand-new Home ended at the check-in and looked unfinished. Found by dumping
        // the view tree on a clean install: neither section was in it at all. His comps show
        // them full, because a comp is always drawn with data in it — the empty state is the
        // one screen a mockup never shows you and every new reader starts on.
        progress?.let { p ->
            Spacer(Modifier.height(Scale.space4))
            NumbersCard(p)
        }

        run {
            Spacer(Modifier.height(Scale.space4))
            ThisWeekCard(recent, pageFor)
        }

        Spacer(Modifier.height(Scale.space8))
    }
}

/**
 * The plate every section sits on. **§ 6e.**
 *
 * One composable rather than four copies of the same three modifiers, because the day one of
 * them drifts is the day the screen stops looking made by one person.
 */
@Composable
private fun Plate(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalWirdColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Scale.card))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.cardEdge, RoundedCornerShape(Scale.card))
            .padding(Scale.space4),
    ) { content() }
}

/**
 * Today's portion, as his comps arrange it.
 *
 * The order is theirs: the label, the medallion with the sūrah's own numeral, the name, the
 * state, the start page and how far through, then the four ways to act.
 */
@Composable
private fun PortionCard(
    assignment: Assignment,
    doneMethod: Method?,
    onOpenPage: () -> Unit,
    onMarkRead: () -> Unit,
    mode: ReadingMode,
    onOpenInQuran: (() -> Unit)?,
) {
    val colors = LocalWirdColors.current
    val surahs = remember(assignment) { assignment.surahs }
    val surah = surahs.firstOrNull()
    val name = surah?.name ?: "Page ${assignment.startPage}"

    Plate {
        Label(if (doneMethod == null) "Today's portion" else "Today, done")
        Spacer(Modifier.height(Scale.space3))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            surah?.let { Medallion(it.number) }
            Spacer(Modifier.width(Scale.space4))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = colors.textPrimary,
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = portionDetail(assignment, doneMethod),
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }
            // His comps put a mushaf on a stand at this corner, and he asked for it by name.
            // Drawn rather than shipped as a raster: it is the same reasoning as every other
            // glyph here, and an illustration that costs no bytes is one the Ghana floor
            // never has to argue about.
            MushafMark()
        }

        Spacer(Modifier.height(Scale.space4))
        Hairline()
        Spacer(Modifier.height(Scale.space3))

        // ---- where it starts, and how far through ----
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Label("Start page")
                Spacer(Modifier.height(Scale.space1))
                Text(
                    text = assignment.startPage.toString(),
                    color = colors.textPrimary,
                    style = TextStyle(fontSize = Scale.title, fontWeight = FontWeight.Medium),
                )
            }
            Spacer(Modifier.width(Scale.space6))
            Column(modifier = Modifier.weight(1f)) {
                ThroughTheMushaf(assignment)
            }
        }

        Spacer(Modifier.height(Scale.space4))

        // ---- the four ways to act ----
        //
        // ⚠ Only "I read it" completes the day from here. Reciting and listening open the
        // page, because the recorder and the player both live there — § 5j. Honest rather
        // than ideal, and unchanged by this restyle.
        val done = doneMethod != null
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Scale.space2),
        ) {
            ActionTile(
                tile = colors.recite,
                label = reciteLabel(mode),
                glyph = { MicGlyph(it) },
                modifier = Modifier.weight(1f),
                onClick = onOpenPage,
            )
            ActionTile(
                tile = colors.read,
                label = if (done) doneLabel(doneMethod) else tapLabel(mode),
                glyph = { tint ->
                    Icon(Icons.Filled.Check, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                },
                enabled = !done,
                modifier = Modifier.weight(1f),
                onClick = onMarkRead,
            )
            ActionTile(
                tile = colors.listen,
                label = "Listen",
                glyph = { tint ->
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                },
                modifier = Modifier.weight(1f),
                onClick = onOpenPage,
            )
            ActionTile(
                tile = colors.openPage,
                label = "Open the page",
                glyph = { BookGlyph(it) },
                modifier = Modifier.weight(1f),
                onClick = onOpenPage,
            )
        }

        // ---- hand it to the other app ----
        //
        // **His ask, 2026-08-19**, and it lands on machinery PLAN task 12 already built and
        // proved: Quran for Android exports a `quran://sura/ayah` forwarder, read out of their
        // own GPL source rather than guessed.
        //
        // ⚠ **It is absent unless that app is installed**, which is also why nobody has seen
        // it yet — no device here has Quran for Android, so task 12 stayed "verified only in
        // the negative". The first phone that has it settles the last open half of that task.
        onOpenInQuran?.let { open ->
            Spacer(Modifier.height(Scale.space3))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Scale.radius * 2))
                    .border(1.dp, colors.cardEdge, RoundedCornerShape(Scale.radius * 2))
                    .clickable(onClick = open)
                    .defaultMinSize(minHeight = Scale.minTarget)
                    .padding(horizontal = Scale.space3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                BookGlyph(colors.accent)
                Spacer(Modifier.width(Scale.space2))
                Text(
                    text = "Open in Quran for Android",
                    color = colors.accent,
                    style = TextStyle(fontSize = Scale.caption, fontWeight = FontWeight.Medium),
                )
            }
        }
    }
}

/**
 * One action, as a tinted tile.
 *
 * ⚠ **The tint cannot carry this on its own** — § 6e measured every fill at 1.01-1.05:1
 * against the plate behind it. The hairline gives it an edge and the coloured glyph gives it
 * an identity; the tint is atmosphere. Colour is never the only signal: the label says what
 * the tile does in words.
 */
@Composable
private fun ActionTile(
    tile: TileColors,
    label: String,
    glyph: @Composable (Color) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = LocalWirdColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Scale.radius * 2))
            .background(tile.fill)
            .border(1.dp, tile.edge, RoundedCornerShape(Scale.radius * 2))
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = 84.dp)
            .padding(horizontal = Scale.space1, vertical = Scale.space3),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        glyph(if (enabled) tile.ink else colors.textSecondary)
        Spacer(Modifier.height(Scale.space2))
        Text(
            text = label,
            color = colors.onSurfaceRaised,
            textAlign = TextAlign.Center,
            style = TextStyle(fontSize = 11.sp, lineHeight = 14.sp),
        )
    }
}

/**
 * The sūrah's number, in the numerals the mushaf itself uses.
 *
 * His comps ring it in a scalloped medallion. The scallop is drawn rather than shipped as an
 * asset — twelve arcs on a circle costs nothing to download, which the Ghana floor cares
 * about more than it cares about the difference between a drawn ring and a traced one.
 */
@Composable
private fun Medallion(number: Int) {
    val colors = LocalWirdColors.current
    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f

            // ⚠ **Beads, not ticks — and the first attempt got this wrong.** Twelve short
            // strokes pointing inward from a ring is a clock face, which is what it read as
            // on the emulator: a clock sitting where a sūrah number should be, on a screen
            // that is otherwise about time. Beads sit ON the ring instead of pointing across
            // it, and nothing about them suggests an hour.
            drawCircle(
                color = colors.accent.copy(alpha = 0.22f),
                radius = r - 2.dp.toPx(),
                style = Stroke(width = 1.dp.toPx()),
            )
            repeat(8) { i ->
                rotate(degrees = i * 45f) {
                    drawCircle(
                        color = colors.accent.copy(alpha = 0.38f),
                        radius = 1.6.dp.toPx(),
                        center = Offset(center.x, center.y - r + 2.dp.toPx()),
                    )
                }
            }
        }
        Text(
            text = arabicNumerals(number),
            color = colors.accent,
            style = TextStyle(fontSize = 26.sp),
        )
    }
}

/**
 * A mushaf open on a rihāl.
 *
 * ⚠ **Redrawn 2026-08-19 — his verdict on the first one was "it looks terrible".** He was
 * right, and the reason is worth keeping because it applies to every drawn illustration:
 * **the first version was made of straight lines, and a book has no straight lines.** Two flat
 * quadrilaterals meeting at a point over a bare X read as a paper aeroplane on sticks. What
 * makes a shape say *book* is the curve — leaves sag away from the spine under their own
 * weight, and the outer edge is where you see it.
 *
 * So this is built from the things that actually signal a bound mushaf:
 *
 *  1. **Curved leaves.** Each page is a quadratic bezier falling from the gutter to its outer
 *     edge, which is the single change that stopped it looking like folded paper.
 *  2. **A gutter, not a point.** The two leaves meet in a narrow V, the way an open book does.
 *  3. **A visible cover** under the leaves — a darker band standing slightly proud of the
 *     paper on both sides. Without it the pages float and nothing says the thing is bound.
 *  4. **Lines of text**, three per leaf, following the curve rather than sitting level. Level
 *     lines on a curved page is the tell that gives away a cheap drawing.
 *  5. **A rihāl with thickness** — two tapered slats, wider where they cross.
 *
 * Drawn rather than shipped as a raster: § 10 measures every byte for readers on Ghanaian
 * mobile data, and a picture is the easiest place to spend a hundred kilobytes unnoticed. It
 * takes its colours from tokens already measured in § 6e, so it adds a picture without adding
 * a colour.
 */
@Composable
private fun MushafMark() {
    val colors = LocalWirdColors.current
    Canvas(modifier = Modifier.size(56.dp)) {
        val w = size.width
        val h = size.height
        val wood = colors.openPage.ink
        val leaf = colors.accent
        val paper = colors.surface

        // ---- the stand: two slats crossing under the book ----
        drawLine(wood, Offset(w * 0.20f, h * 0.95f), Offset(w * 0.63f, h * 0.55f), w * 0.07f, StrokeCap.Round)
        drawLine(wood, Offset(w * 0.80f, h * 0.95f), Offset(w * 0.37f, h * 0.55f), w * 0.07f, StrokeCap.Round)

        // ---- the cover, sitting a little proud of the paper on each side ----
        val cover = Path().apply {
            moveTo(w * 0.50f, h * 0.70f)
            quadraticTo(w * 0.26f, h * 0.72f, w * 0.06f, h * 0.60f)
            lineTo(w * 0.06f, h * 0.66f)
            quadraticTo(w * 0.26f, h * 0.78f, w * 0.50f, h * 0.76f)
            quadraticTo(w * 0.74f, h * 0.78f, w * 0.94f, h * 0.66f)
            lineTo(w * 0.94f, h * 0.60f)
            quadraticTo(w * 0.74f, h * 0.72f, w * 0.50f, h * 0.70f)
            close()
        }
        drawPath(cover, color = leaf)

        // ---- the two leaves, each sagging away from the gutter ----
        listOf(-1f, 1f).forEach { side ->
            val outer = w * (0.5f + side * 0.44f)
            val mid = w * (0.5f + side * 0.24f)
            val page = Path().apply {
                moveTo(w * 0.50f, h * 0.32f)
                // the top edge lifts, then falls to the outer corner
                quadraticTo(mid, h * 0.20f, outer, h * 0.30f)
                lineTo(outer, h * 0.60f)
                // the bottom edge sags back to the gutter
                quadraticTo(mid, h * 0.72f, w * 0.50f, h * 0.70f)
                close()
            }
            drawPath(page, color = paper)
            drawPath(page, color = leaf, style = Stroke(width = w * 0.035f))

            // three lines of text, following the sag rather than sitting level
            repeat(3) { row ->
                val t = 0.40f + row * 0.11f
                val yIn = h * (t + 0.02f)
                val yOut = h * t
                drawLine(
                    color = leaf.copy(alpha = 0.45f),
                    start = Offset(w * (0.5f + side * 0.10f), yIn),
                    end = Offset(w * (0.5f + side * 0.36f), yOut),
                    strokeWidth = w * 0.022f,
                    cap = StrokeCap.Round,
                )
            }
        }

        // ---- the gutter: a narrow V, which is what says "two leaves" and not "one sheet" ----
        drawLine(leaf, Offset(w * 0.50f, h * 0.32f), Offset(w * 0.50f, h * 0.70f), w * 0.035f, StrokeCap.Round)
    }
}

/**
 * A sun, for the greeting. His image 2's one piece of illustration.
 *
 * Drawn, not shipped: eight rays and a disc in the amber already measured for the listen
 * tile, so it costs no bytes and no new colour. It also turns with the day — the disc sits
 * low and the rays shorten after dark, because a blazing sun above "Good evening" is a
 * picture disagreeing with its own caption.
 */
@Composable
private fun DayMark() {
    val colors = LocalWirdColors.current
    val night = LocalTime.now().hour !in 6..18
    val ink = if (night) colors.accent else colors.listen.ink

    Canvas(modifier = Modifier.size(56.dp)) {
        val r = size.minDimension * 0.20f
        drawCircle(color = ink.copy(alpha = 0.85f), radius = r, center = center)
        if (!night) {
            repeat(8) { i ->
                rotate(degrees = i * 45f) {
                    drawLine(
                        color = ink.copy(alpha = 0.7f),
                        start = Offset(center.x, center.y - r - 5.dp.toPx()),
                        end = Offset(center.x, center.y - r - 11.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        } else {
            // A crescent, cut by knocking a second disc out of the first with the plate's
            // own colour. Cheaper than a path, and it lands on the same pixel grid.
            drawCircle(
                color = colors.surfaceRaised,
                radius = r * 0.92f,
                center = Offset(center.x + r * 0.55f, center.y - r * 0.30f),
            )
        }
    }
}

/** A microphone. Two rounded rectangles and an arc; the core icon set has no mic. */
@Composable
private fun MicGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.34f, h * 0.06f),
            size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.50f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.16f),
        )
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.20f, h * 0.34f),
            size = androidx.compose.ui.geometry.Size(w * 0.60f, h * 0.42f),
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round),
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.76f),
            end = Offset(w * 0.5f, h * 0.94f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round,
        )
    }
}

/** An open book. Two leaves meeting at a spine; the core icon set has no book either. */
@Composable
private fun BookGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round)
        listOf(-1f, 1f).forEach { side ->
            val outer = Offset(w * (0.5f + side * 0.40f), h * 0.24f)
            val inner = Offset(w * 0.5f, h * 0.32f)
            drawLine(tint, outer, inner, stroke.width, stroke.cap)
            drawLine(tint, outer, Offset(outer.x, h * 0.78f), stroke.width, stroke.cap)
            drawLine(tint, Offset(outer.x, h * 0.78f), Offset(w * 0.5f, h * 0.84f), stroke.width, stroke.cap)
        }
        drawLine(tint, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.5f, h * 0.84f), stroke.width, stroke.cap)
    }
}

/**
 * How far through the mushaf you are, as a bar and a figure.
 *
 * Real arithmetic, not decoration: position over 1,208 half-pages. His comps put the percent
 * at the end of the bar rather than under it, which is the better place for it — the bar is
 * the approximate answer and the figure is the exact one, and they should be read together.
 */
@Composable
private fun ThroughTheMushaf(assignment: Assignment) {
    val colors = LocalWirdColors.current
    val fraction = (assignment.startUnit.toFloat() / Mushaf.TOTAL_UNITS).coerceIn(0f, 1f)
    val percent = (fraction * 100).toInt()

    Label("Progress in the mushaf")
    Spacer(Modifier.height(Scale.space2))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.16f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
            )
        }
        Spacer(Modifier.width(Scale.space2))
        Text(
            text = "$percent%",
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.caption, fontWeight = FontWeight.Medium),
        )
    }
}

/**
 * The two numbers, plus the split. **His image 3.**
 *
 * Sacred Rules 4 and 6: the streak never appears without the total, a streak of nought is
 * not announced, and the split is written out in words rather than shown as a ratio.
 *
 * **The recited count is not a third figure.** The comps show two figures and then the
 * sentence, and they are right — an older row put "5 RECITED" directly above "5 recited
 * aloud, 18 marked as read", which is one number said twice in two shapes.
 */
@Composable
private fun NumbersCard(p: Progress) {
    val colors = LocalWirdColors.current
    Plate {
        Label("Your numbers")
        Spacer(Modifier.height(Scale.space3))

        // **Day one is a real state and it gets a real sentence.** Sacred Rule 3: it says
        // what will fill the card, not what is missing from it. "No data" is a fact about
        // the app; "today would be your first" is a fact about the reader.
        if (p.totalDaysRead == 0) {
            Text(
                text = "Nothing recorded yet. Finish today and this becomes day one.",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption, lineHeight = 20.sp),
            )
            return@Plate
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // **The streak always shows, even at zero.** It used to appear only above 1,
            // so the card silently changed shape on the second day and a reader on day one
            // never saw the thing the app is asking them to build. Sacred Rule 4 is why it
            // is safe to show a nought: it never appears without total days read beside it.
            Figure(
                value = p.currentStreak.toString(),
                caption = if (p.currentStreak == 1) "Day streak" else "Day streak",
                modifier = Modifier.weight(1f),
                glyph = { FlameGlyph(it) },
            )
            VerticalHair()
            Figure(
                value = p.totalDaysRead.toString(),
                caption = if (p.totalDaysRead == 1) "Day read" else "Total days read",
                modifier = Modifier.weight(1f),
                glyph = { BookGlyph(it) },
            )
            VerticalHair()
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = "${p.recitedDays} recited aloud,",
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = Scale.caption),
                )
                Text(
                    text = "${p.tappedDays} marked as read.",
                    color = colors.textSecondary,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }
        }
    }
}

@Composable
private fun Figure(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    glyph: (@Composable (Color) -> Unit)? = null,
) {
    val colors = LocalWirdColors.current
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            glyph?.let {
                it(colors.accent)
                Spacer(Modifier.width(Scale.space2))
            }
            Text(
                text = value,
                color = colors.accent,
                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium),
            )
        }
        Text(
            text = caption,
            color = colors.textSecondary,
            style = TextStyle(fontSize = 11.sp),
        )
    }
}

/**
 * A flame, for the streak. His comp uses an emoji here and this does not.
 *
 * Emoji in UI chrome renders differently on every phone and reads as a placeholder; the gate
 * blocks it for that reason, and the rest of this screen already draws its own marks. A flame
 * is two curves meeting at a point, which is cheaper than it looks.
 */
@Composable
private fun FlameGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        // ⚠ **A symmetrical teardrop is a water droplet, not a flame** — which is exactly
        // what the first attempt drew, on a card about a reading streak. A flame is
        // asymmetric: the tip leans, one side bulges, and the other carries an S-curve back
        // in towards the base. That curl is the whole difference between fire and water.
        val body = Path().apply {
            moveTo(w * 0.56f, h * 0.04f)
            quadraticTo(w * 0.98f, h * 0.44f, w * 0.76f, h * 0.76f)
            quadraticTo(w * 0.58f, h * 1.00f, w * 0.36f, h * 0.90f)
            quadraticTo(w * 0.10f, h * 0.74f, w * 0.30f, h * 0.46f)
            quadraticTo(w * 0.44f, h * 0.28f, w * 0.38f, h * 0.14f)
            quadraticTo(w * 0.48f, h * 0.24f, w * 0.56f, h * 0.04f)
            close()
        }
        drawPath(body, color = tint)
    }
}

/**
 * The last few days, with the page each one covered.
 *
 * **Two things that make this a table rather than loose lines**, both kept from the build
 * § 5o corrected: ruled rows, and a left marker per row — **filled for a recitation, hollow
 * for a tap.** Sacred Rule 6 says the two are never blurred, and colour is not the only thing
 * saying which: the words are right there.
 */
@Composable
private fun ThisWeekCard(recent: List<DayLog>, pageFor: (LocalDate) -> Int?) {
    val colors = LocalWirdColors.current
    val shown = recent.take(4)

    Plate {
        if (shown.isEmpty()) {
            Label("This week")
            Spacer(Modifier.height(Scale.space3))
            Text(
                text = "The days you finish will be listed here, newest first.",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption, lineHeight = 20.sp),
            )
            return@Plate
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Label("This week")
            Text(
                text = if (shown.size == 1) "Last day" else "Last ${shown.size} days",
                color = colors.textSecondary,
                style = TextStyle(fontSize = 11.sp),
            )
        }
        Spacer(Modifier.height(Scale.space2))

        shown.forEachIndexed { i, log ->
            if (i > 0) Hairline()
            val recited = log.method == Method.RECITED
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Scale.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .then(
                            if (recited) {
                                Modifier.background(colors.accent)
                            } else {
                                Modifier.border(1.dp, colors.accent.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                            }
                        )
                )
                Spacer(Modifier.width(Scale.space3))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dayName(log.date),
                        color = colors.textPrimary,
                        style = TextStyle(fontSize = 15.sp),
                    )
                    Text(
                        text = if (recited) "Recited aloud" else "Marked as read",
                        color = if (recited) colors.accent else colors.textSecondary,
                        style = TextStyle(fontSize = 12.sp),
                    )
                }
                pageFor(log.date)?.let { page ->
                    Text(
                        text = "Page $page",
                        color = colors.textSecondary,
                        style = TextStyle(fontSize = 12.sp),
                    )
                }
                Spacer(Modifier.width(Scale.space1))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(Scale.space2))
        Text(
            // Sacred Rule 3, said out loud rather than merely implemented.
            text = "Days you missed aren't listed. There is no row saying you failed.",
            color = colors.textSecondary,
            style = TextStyle(fontSize = 11.sp),
        )
    }
}

@Composable
private fun Hairline() {
    val colors = LocalWirdColors.current
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.cardEdge))
}

@Composable
private fun VerticalHair() {
    val colors = LocalWirdColors.current
    Box(
        modifier = Modifier
            .padding(horizontal = Scale.space3)
            .width(1.dp)
            .height(34.dp)
            .background(colors.cardEdge)
    )
}

@Composable
private fun Label(text: String) {
    val colors = LocalWirdColors.current
    Text(
        text = text.uppercase(),
        color = colors.textSecondary,
        style = TextStyle(fontSize = 10.5.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium),
    )
}

// ---- words ----

/** 18 → ١٨. The numerals the mushaf itself uses. */
private fun arabicNumerals(n: Int): String =
    n.toString().map { ARABIC_DIGITS[it - '0'] }.joinToString("")

private val ARABIC_DIGITS = listOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

private fun greeting(): String = when (LocalTime.now().hour) {
    in 0..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

private fun companionQuestion(): String = when (LocalTime.now().hour) {
    in 0..11 -> "Reading this morning?"
    in 12..16 -> "Reading today?"
    else -> "Are you reading tonight?"
}

/** What the read tile says once the day is already finished. */
private fun doneLabel(method: Method?): String =
    if (method == Method.RECITED) "Recited aloud" else "Marked as read"

/**
 * "Thursday · 3 Rabīʿ al-Awwal 1448".
 *
 * The Hijri date comes from `java.time.chrono.HijrahDate`, in the platform since API 26 —
 * this app's floor — so it costs nothing and needs no table of month names typed from
 * memory, which is exactly the kind of Islamic data this project refuses to guess at.
 */
private fun todayLine(): String {
    val today = LocalDate.now()
    val weekday = today.format(DateTimeFormatter.ofPattern("EEEE"))
    val hijri = runCatching {
        val h = HijrahDate.from(today)
        val month = HIJRI_MONTHS[h.get(ChronoField.MONTH_OF_YEAR) - 1]
        "$month ${h.get(ChronoField.YEAR)}"
    }.getOrNull()
    val day = runCatching { HijrahDate.from(today).get(ChronoField.DAY_OF_MONTH) }.getOrNull()
    return if (hijri != null && day != null) "$weekday · $day $hijri" else weekday
}

/** Transliterations, spelled the way they are written in English-language mushafs. */
private val HIJRI_MONTHS = listOf(
    "Muḥarram", "Ṣafar", "Rabīʿ al-Awwal", "Rabīʿ al-Thānī", "Jumādā al-Ūlā",
    "Jumādā al-Ākhirah", "Rajab", "Shaʿbān", "Ramaḍān", "Shawwāl",
    "Dhū al-Qaʿdah", "Dhū al-Ḥijjah",
)

/**
 * "One page · not yet marked", the design's own shape for this line.
 *
 * The page number has left this sentence and become a figure of its own, so what is left is
 * the amount and the state. A portion running across two pages still spells the span out
 * here, because the figure can only show where it starts.
 */
private fun portionDetail(a: Assignment, doneMethod: Method?): String {
    val amount = when (a.units) {
        1 -> "Half a page"
        2 -> "One page"
        else -> "${a.units / 2} pages"
    }
    val span = if (a.startPage == a.endPage) null else "to ${a.endPage}"
    val state = when (doneMethod) {
        Method.RECITED -> "recited aloud"
        Method.TAPPED -> "marked as read"
        null -> "not yet marked"
    }
    return listOfNotNull(amount, span, state).joinToString(" · ")
}

private fun dayName(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE"))
    }
}
