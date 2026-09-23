package com.mosman.wird.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.R
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.domain.Assignment
import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.LifeSpace
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.TrackScheduleMode
import com.mosman.wird.domain.Progress
import com.mosman.wird.domain.Turn
import com.mosman.wird.domain.surahs
import com.mosman.wird.domain.pages
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import com.mosman.wird.ui.theme.SetStatusBarAppearance
import com.mosman.wird.ui.theme.clayCard
import com.mosman.wird.ui.theme.clayPill
import com.mosman.wird.ui.theme.arabicNumerals
import kotlinx.coroutines.delay
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
    modifier: Modifier = Modifier,
    turns: List<Turn> = emptyList(),
    onSaid: (String) -> Unit = {},
    onOpenChat: () -> Unit = {},
    positionLabel: String = "",
    readerName: String? = null,
    pageFor: (LocalDate) -> Int? = { null },
    onMarkRead: () -> Unit = {},
    mode: ReadingMode = ReadingMode.READING,
    onOpenInQuran: (() -> Unit)? = null,
    onOpenBookmarks: () -> Unit = {},
    onMenu: (() -> Unit)? = null,
    menu: @Composable () -> Unit = {},
    activeSpace: LifeSpace? = null,
    activeTrack: ReadingTrack? = null,
    allSpaces: List<LifeSpace> = emptyList(),
    scheduleMode: TrackScheduleMode = TrackScheduleMode.AUTOMATIC,
    onSelectTrack: (ReadingTrack) -> Unit = {},
    onSelectSpace: (LifeSpace) -> Unit = {},
    onToggleScheduleMode: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val groundColor = if (isDark) Color(0xFF08100D) else Color(0xFFF7F4EB)

    SetStatusBarAppearance(isLightBackground = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(groundColor)
            .verticalScroll(rememberScrollState()),
    ) {
        // Atmospheric Dawn Mosque Header (reference design)
        AtmosphericHeader(
            readerName = readerName ?: "Mutalib",
            positionText = if (positionLabel.isNotEmpty()) positionLabel else "Al-Fātihah 1, page 1",
            onOpenPosition = onOpenPage,
            onOpenBookmarks = onOpenBookmarks,
            onMenu = onMenu,
            menu = menu,
        )

        // Cards body with soft rounded overlap
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-14).dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(groundColor)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            var cardIndex = 0

            if (activeSpace != null && activeTrack != null) {
                StaggeredEnter(index = cardIndex++) {
                    LifeSpaceBar(
                        activeSpace = activeSpace,
                        activeTrack = activeTrack,
                        allSpaces = allSpaces,
                        scheduleMode = scheduleMode,
                        onSelectTrack = onSelectTrack,
                        onSelectSpace = onSelectSpace,
                        onToggleScheduleMode = onToggleScheduleMode,
                        onOpenSettings = onOpenSettings,
                    )
                }
            }

            StaggeredEnter(index = cardIndex++) {
                PortionCard(
                    assignment = assignment,
                    doneMethod = doneMethod,
                    onOpenPage = onOpenPage,
                    onMarkRead = onMarkRead,
                    mode = mode,
                    onOpenInQuran = onOpenInQuran,
                    activeTrack = activeTrack,
                )
            }

            // Today's Habit Clarity Card + Reflection Capsule (Option A)
            Spacer(Modifier.height(Scale.space4))
            StaggeredEnter(index = cardIndex++) {
                HomeHabitClarityCard(
                    assignment = assignment,
                    doneMethod = doneMethod,
                    progress = progress,
                    turns = turns,
                    onYesRecited = {
                        onSaid("Already did it")
                    },
                    onRemindInHour = {
                        onSaid("In an hour")
                    },
                    onNotToday = {
                        onSaid("Not today")
                    },
                    onOpenChat = onOpenChat,
                )
            }

            progress?.let { p ->
                Spacer(Modifier.height(Scale.space4))
                StaggeredEnter(index = cardIndex++) {
                    NumbersCard(p)
                }
            }

            Spacer(Modifier.height(Scale.space4))
            StaggeredEnter(index = cardIndex++) {
                ThisWeekCard(recent, pageFor)
            }

            // Bottom clearance for floating island dock
            Spacer(Modifier.height(84.dp))
        }
    }
}

/**
 * Entrance animation for cards: subtle slide up + fade in with staggered index delay.
 */
@Composable
private fun StaggeredEnter(
    index: Int,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 85L)
        visible = true
    }
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 36f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing,
        ),
        label = "translateY_$index",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing,
        ),
        label = "alpha_$index",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = translateY
                this.alpha = alpha
            }
    ) {
        content()
    }
}

/**
 * The claymorphic plate every section sits on.
 * Soft inflated 3D tactile card with dual-light gradient bevels and elevation drop shadows.
 */
@Composable
private fun Plate(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val cardBg = if (isDark) Color(0xFF111E18) else Color(0xFFFFFFFF)
    val highlight = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.95f)
    val shadow = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF8C7D6B).copy(alpha = 0.28f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(24.dp),
                backgroundColor = cardBg,
                highlightColor = highlight,
                shadowColor = shadow,
                elevation = 7.dp,
                strokeWidth = 1.5.dp,
            )
            .padding(18.dp),
    ) { content() }
}

/**
 * Today's portion, matching the user's approved reference design.
 * Features the squircle Arabic medallion, title & verses, 3 clay stat boxes,
 * dual-counter progress bar, and 4 tactile action tiles (Record in dark forest green).
 */
@Composable
private fun PortionCard(
    assignment: Assignment,
    doneMethod: Method?,
    onOpenPage: () -> Unit,
    onMarkRead: () -> Unit,
    mode: ReadingMode,
    onOpenInQuran: (() -> Unit)?,
    activeTrack: ReadingTrack? = null,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val context = androidx.compose.ui.platform.LocalContext.current
    val surahs = remember(assignment) { assignment.surahs }
    val surah = surahs.firstOrNull() ?: com.mosman.wird.domain.SurahIndex.on(assignment.startPage).firstOrNull()
    val name = surah?.name ?: "Page ${assignment.startPage}"
    val ayahRange = remember(assignment, surah) {
        if (surah != null) com.mosman.wird.domain.PageVerses.ayahRange(context, assignment.pages, surah.number) else null
    }
    val page = Mushaf.pageOf(assignment.startUnit)
    val span = surah?.let { (it.lastPage - it.firstPage + 1).coerceAtLeast(1) } ?: 1
    val into = surah?.let { (page - it.firstPage).coerceAtLeast(0) } ?: 0
    val fraction = (into.toFloat() / span).coerceIn(0f, 1f)
    val percent = (fraction * 100).toInt()

    Plate {
        // Card header row with status pill & active track tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (doneMethod == null) "TODAY'S WIRD" else "TODAY, DONE",
                    color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp),
                )
                if (activeTrack != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF1E382B) else Color(0xFFD6EDE0))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${activeTrack.type.label} · ${activeTrack.name}",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF8ED676) else Color(0xFF1E5B42),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .clayPill(
                        shape = RoundedCornerShape(999.dp),
                        backgroundColor = if (isDark) Color(0xFF162620) else Color(0xFFF1EDE1),
                        elevation = 1.dp,
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF8ED676) else Color(0xFF245847))
                    )
                    Text(
                        text = when (doneMethod) {
                            Method.RECITED -> "Recited aloud"
                            Method.TAPPED -> "Marked as read"
                            null -> "Not yet marked"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Sūrah row: Medallion (Arabic number) + Name + Details (NO book illustration)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Medallion(surah?.number ?: 1)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D),
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (surah != null && ayahRange != null) {
                        "${surah.name} · $ayahRange · Page ${assignment.startPage}"
                    } else if (surah != null) {
                        "${surah.name} · Page ${assignment.startPage}"
                    } else {
                        portionDetail(assignment, doneMethod)
                    },
                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                    style = TextStyle(fontSize = 11.5.sp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 3 Clay stat boxes (1 page, start page, % through mushaf or track streak)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ClayStatPill(
                value = if (assignment.units <= 2) "1 page" else "${assignment.units / 2} pages",
                label = "Daily amount",
                modifier = Modifier.weight(1f),
            )
            ClayStatPill(
                value = "Page ${assignment.startPage}",
                label = "Start page",
                modifier = Modifier.weight(1f),
            )
            ClayStatPill(
                value = if (activeTrack != null && activeTrack.currentStreak > 0) "${activeTrack.currentStreak}d" else "$percent%",
                label = if (activeTrack != null && activeTrack.currentStreak > 0) "Track streak" else "Of mushaf",
                valueColor = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        // Progress bar with dual page markers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Page ${assignment.startPage} of 604",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
            )
            Text(
                text = "${(604 - assignment.startPage).coerceAtLeast(0)} pages left",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (isDark) Color(0xFF1B2722) else Color(0xFFE6DFCF)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(0.04f))
                    .height(7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) Color(0xFF68BD5B) else Color(0xFF245847)),
            )
        }

        Spacer(Modifier.height(16.dp))

        // ---- the four tactile action tiles ----
        val done = doneMethod != null
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionTile(
                label = reciteLabel(mode),
                glyph = { MicGlyph(it) },
                isPrimary = true,
                modifier = Modifier.weight(1f),
                onClick = onOpenPage,
            )
            ActionTile(
                label = if (done) doneLabel(doneMethod) else "Mark Done",
                glyph = { tint ->
                    Icon(Icons.Filled.Check, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
                },
                isPrimary = false,
                enabled = !done,
                modifier = Modifier.weight(1f),
                onClick = onMarkRead,
            )
            ActionTile(
                label = "Listen",
                glyph = { tint ->
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                },
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onClick = onOpenPage,
            )
            ActionTile(
                label = "Mushaf",
                glyph = { BookGlyph(it) },
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onClick = onOpenPage,
            )
        }

        // Quran for Android button if available
        onOpenInQuran?.let { open ->
            Spacer(Modifier.height(Scale.space3))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clayPill(
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = if (isDark) Color(0xFF132019) else Color(0xFFF1F5F2),
                        elevation = 2.dp,
                    )
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

@Composable
private fun ClayStatPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val textColor = valueColor ?: (if (isDark) Color(0xFFE4E9E5) else Color(0xFF17382D))
    Column(
        modifier = modifier
            .clayCard(
                shape = RoundedCornerShape(14.dp),
                backgroundColor = if (isDark) Color(0xFF16241E) else Color(0xFFF7F4EB),
                highlightColor = Color.White.copy(alpha = if (isDark) 0.08f else 0.8f),
                shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF8C7D6B).copy(alpha = 0.16f),
                elevation = 2.dp,
                strokeWidth = 1.dp,
            )
            .padding(horizontal = 6.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
            maxLines = 1,
        )
    }
}

/**
 * One action tile.
 * Matches the reference: Record is primary forest green (#1E3F32) with white ink,
 * other tiles are warm cream (#F7F3E8) with dark green (#245847) ink.
 */
@Composable
private fun ActionTile(
    label: String,
    glyph: @Composable (Color) -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val bg = when {
        isPrimary -> if (isDark) Color(0xFF1B4537) else Color(0xFF1E3F32)
        else -> if (isDark) Color(0xFF16241E) else Color(0xFFF7F3E8)
    }
    val ink = when {
        isPrimary -> Color.White
        else -> if (isDark) Color(0xFFBAD3C5) else Color(0xFF245847)
    }
    val highlight = if (isPrimary) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = if (isDark) 0.12f else 0.85f)
    val shadow = if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF8C7D6B).copy(alpha = 0.16f)

    Column(
        modifier = modifier
            .clayCard(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = if (enabled) bg else bg.copy(alpha = 0.5f),
                highlightColor = highlight,
                shadowColor = shadow,
                elevation = 4.dp,
                strokeWidth = 1.dp,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = 84.dp)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        glyph(if (enabled) ink else ink.copy(alpha = 0.4f))
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = if (enabled) ink else ink.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, lineHeight = 13.sp),
        )
    }
}

/**
 * The sūrah's number in Arabic numerals inside a mint squircle medallion.
 */
@Composable
private fun Medallion(number: Int) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clayCard(
                shape = RoundedCornerShape(14.dp),
                backgroundColor = if (isDark) Color(0xFF1A2B24) else Color(0xFFEDF5F0),
                highlightColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.85f),
                shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFFA0B9AA).copy(alpha = 0.35f),
                elevation = 3.dp,
                strokeWidth = 1.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = arabicNumerals(number),
            color = if (isDark) Color(0xFF93DB7A) else Color(0xFF174233),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * The mushaf mark on the portion card. **A real icon, not a drawing.**
 *
 * ⚠ **Three hand-built attempts at this shape were rejected**, the last one twice — *"the
 * quran image there, it looks terrible"* and then *"still on the quran image it looks
 * terrible, why not use an actual image like they did for the reference image."* He was right
 * both times, and the lesson is worth more than the icon:
 *
 * **A Canvas path is the right tool for a mark and the wrong tool for an illustration.** The
 * crescent on a chip, the flame, the tail on a bubble, the medallion — those are marks: a few
 * strokes where the meaning survives being crude. A book on a stand is an illustration, with
 * perspective, weight and a dozen curves that all have to agree, and hand-writing bezier
 * control points is not how anyone draws one.
 *
 * So this is **Font Awesome Free 6's `book-quran`**, drawn by people who draw icons for a
 * living, taken as a vector at about 2KB. See `res/drawable/ic_book_quran.xml` for the licence
 * — CC BY 4.0, attributed in README.md and PROFILE.md § 9.
 *
 * **It carries no lettering, and that is not incidental.** Sacred Rule 2 forbids generated
 * Qur'anic text; an illustration with plausible Arabic-looking squiggles across its pages
 * would be the same failure in a different coat. This is why the generated-image route was
 * flagged before it was offered.
 *
 * ⬜ **The cover carries a star and crescent**, which is the icon set's choice rather than
 * ours. It is a common motif and not a universally loved one as a symbol of Islam. Flagged
 * for him rather than shipped quietly: `book-open` from the same set is one line away.
 */
@Composable
private fun MushafMark() {
    val colors = LocalWirdColors.current
    Icon(
        painter = painterResource(R.drawable.ic_book_quran),
        contentDescription = null,
        tint = colors.accent,
        modifier = Modifier.size(40.dp),
    )
}

/**
 * The sun or the moon beside the greeting.
 *
 * ⚠ **Two bugs, one cause: this and the greeting were reading different clocks.** The words
 * called anything before noon "morning" while the mark called anything before six "night", so
 * at five in the morning the screen said **"Good morning" under a crescent moon**. Both now
 * take [greeting] as the single source of truth — if the sentence says morning, the sky does.
 *
 * The sun is Font Awesome Free's, after his note that the comp does it differently and the
 * drawn one did not match. The crescent stays hand-drawn: a disc with a second disc knocked
 * out of it is a mark rather than an illustration, it is two lines of code, and it looked
 * right the first time. § 5ag's rule cuts both ways.
 */
@Composable
private fun DayMark() {
    val colors = LocalWirdColors.current
    val night = greeting() == "Good evening"

    if (!night) {
        Icon(
            painter = painterResource(R.drawable.ic_sun),
            contentDescription = null,
            tint = colors.listen.ink,
            modifier = Modifier.size(34.dp),
        )
        return
    }

    Canvas(modifier = Modifier.size(34.dp)) {
        val r = size.minDimension * 0.34f
        drawCircle(color = colors.accent.copy(alpha = 0.9f), radius = r, center = center)
        // A crescent, cut by knocking a second disc out of the first with the plate's own
        // colour. Cheaper than a path, and it lands on the same pixel grid.
        drawCircle(
            color = colors.surface,
            radius = r * 0.92f,
            center = Offset(center.x + r * 0.55f, center.y - r * 0.30f),
        )
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
 * How far through **this sūrah** you are, as a bar and a figure.
 *
 * ⚠ **Changed 2026-08-19 at his word — it used to measure the whole mushaf, and that was the
 * wrong number in a way worth recording.** Position over 1,208 half-pages is a *bookmark*, not
 * an achievement: he set his position to page 442 during setup, so the app read **73%** on day
 * one, before a single page had been read in Wird. A figure that large, that early, next to a
 * progress bar, invites exactly the belief Sacred Rule 6 exists to prevent.
 *
 * A sūrah is also the unit a reader actually feels. "Seventy-three per cent of the Qur'an" is
 * an abstraction; "two pages left of Al-Kahf" is a thing you can finish tonight — and finishing
 * is what his idea 1 wants to congratulate.
 *
 * **It is still a position rather than a tally.** Where you are in this sūrah is honest about
 * being a place; the old number implied a distance travelled. When a real "how much have I read"
 * figure is wanted, `Progress.totalDaysRead` is the one that never resets and never lies.
 */
@Composable
private fun ThroughTheMushaf(assignment: Assignment) {
    val colors = LocalWirdColors.current
    val surah = remember(assignment) { assignment.surahs.firstOrNull() }
    val page = Mushaf.pageOf(assignment.startUnit)

    // A sūrah spanning one page still has to divide by something.
    val span = surah?.let { (it.lastPage - it.firstPage + 1).coerceAtLeast(1) } ?: 1
    val into = surah?.let { (page - it.firstPage).coerceAtLeast(0) } ?: 0
    val fraction = (into.toFloat() / span).coerceIn(0f, 1f)
    val percent = (fraction * 100).toInt()
    val left = (span - into - 1).coerceAtLeast(0)

    Label(surah?.let { "Through ${it.name}" } ?: "Through this surah")
    Spacer(Modifier.height(Scale.space2))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.cardEdge),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.accent),
            )
        }
        Spacer(Modifier.width(Scale.space3))
        Text(
            text = "$percent%",
            color = colors.textSecondary,
            style = TextStyle(fontSize = Scale.caption),
        )
    }
    Spacer(Modifier.height(Scale.space2))
    Text(
        // The figure people can act on: not how far in, but how much is left.
        text = when {
            surah == null -> "Page $page"
            left == 0 -> "Last page of ${surah.name}"
            left == 1 -> "1 page left of ${surah.name}"
            else -> "$left pages left of ${surah.name}"
        },
        color = colors.textSecondary,
        style = TextStyle(fontSize = 11.sp),
    )
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
/**
 * The two numbers, plus the split, in the approved clay layout.
 */
@Composable
private fun NumbersCard(p: Progress) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val aloudRatio = if (p.totalDaysRead > 0) (p.recitedDays * 100) / p.totalDaysRead else 0

    Plate {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "YOUR JOURNEY",
                color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp),
            )
            Text(
                text = "Overall progress",
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                style = TextStyle(fontSize = 11.sp),
            )
        }
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            // Col 1: Streak
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "🔥 ${p.currentStreak}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFE67E22) else Color(0xFFD35400),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Day Streak",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF8C7D6B).copy(alpha = 0.2f))
            )

            // Col 2: Total days read
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "📖 ${p.totalDaysRead}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFF92E2B6) else Color(0xFF1E3F32),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Total Days",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF8C7D6B).copy(alpha = 0.2f))
            )

            // Col 3: Aloud Ratio
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "🎙️ $aloudRatio%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFF50A773) else Color(0xFF245847),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Aloud Ratio",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF6F8378),
                )
            }
        }
    }
}

@Composable
private fun FlameGlyph(tint: Color, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_flame),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(18.dp),
    )
}

/**
 * This week card featuring exclusively the 7 circular day tracker beads.
 * Past day rows have been cleanly removed as requested.
 */
@Composable
private fun ThisWeekCard(recent: List<DayLog>, pageFor: (LocalDate) -> Int?) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val today = LocalDate.now()
    val last7Days = remember { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val completedDates = remember(recent) { recent.map { it.date }.toSet() }
    val completedInWeek = remember(completedDates, last7Days) {
        last7Days.count { it in completedDates }
    }

    Plate {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "THIS WEEK",
                color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp),
            )
            Text(
                text = "$completedInWeek of 7 days completed",
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                style = TextStyle(fontSize = 11.sp),
            )
        }
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            last7Days.forEach { date ->
                val isDone = date in completedDates
                val isToday = date == today
                val dayLabel = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayLabel,
                        fontSize = 10.5.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                        color = when {
                            isToday -> if (isDark) Color(0xFF8ED676) else Color(0xFF245847)
                            else -> if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73)
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .then(
                                when {
                                    isDone -> Modifier
                                        .background(Color(0xFF245847))
                                        .clayPill(
                                            shape = CircleShape,
                                            backgroundColor = Color(0xFF245847),
                                            elevation = 2.dp,
                                        )
                                    isToday -> Modifier
                                        .border(1.5.dp, if (isDark) Color(0xFF8ED676) else Color(0xFF245847), CircleShape)
                                        .background((if (isDark) Color(0xFF8ED676) else Color(0xFF245847)).copy(alpha = 0.12f))
                                    else -> Modifier
                                        .background(if (isDark) Color(0xFF17241F) else Color(0xFFEAE4D5))
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        } else if (isToday) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF8ED676) else Color(0xFF245847))
                            )
                        } else {
                            Text(
                                text = "·",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF556C60) else Color(0xFF8A9990),
                            )
                        }
                    }
                }
            }
        }
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


private fun greeting(): String = when (LocalTime.now().hour) {
    in 0..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

private fun companionQuestion(isDone: Boolean = false): String {
    if (isDone) {
        return "Alhamdulillah on finishing today! How did your portion feel?"
    }
    return when (LocalTime.now().hour) {
        in 0..11 -> "Will you read today's portion before Dhuhr prayer?"
        in 12..16 -> "Reading your portion this afternoon?"
        else -> "Are you reading tonight?"
    }
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
