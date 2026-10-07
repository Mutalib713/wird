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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import com.mosman.wird.R
import com.mosman.wird.data.ReadingMode
import com.mosman.wird.domain.Assignment
import com.mosman.wird.domain.ayahRangeIn
import com.mosman.wird.domain.DayLog
import com.mosman.wird.domain.Method
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.domain.LifeSpace
import com.mosman.wird.domain.ReadingDirection
import com.mosman.wird.domain.ReadingTrack
import com.mosman.wird.domain.TrackScheduleMode
import com.mosman.wird.domain.TrackType
import com.mosman.wird.domain.plan
import com.mosman.wird.domain.unitsLabel
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
    onUndoMarkRead: (() -> Unit)? = null,
    onNavigateTab: (WirdTab) -> Unit = {},
    onOpenSurahs: () -> Unit = {},
    mode: ReadingMode = ReadingMode.READING,
    onOpenInQuran: (() -> Unit)? = null,
    onOpenBookmarks: () -> Unit = {},
    onMenu: (() -> Unit)? = null,
    menu: @Composable () -> Unit = {},
    activeSpace: LifeSpace? = null,
    activeTrack: ReadingTrack? = null,
    allSpaces: List<LifeSpace> = emptyList(),
    allTracks: List<ReadingTrack> = emptyList(),
    scheduleMode: TrackScheduleMode = TrackScheduleMode.AUTOMATIC,
    onSelectTrack: (ReadingTrack) -> Unit = {},
    onSelectSpace: (LifeSpace) -> Unit = {},
    onToggleScheduleMode: () -> Unit = {},
    onOpenSettings: (SettingsDialog?) -> Unit = {},
    showToolkitTour: Boolean = false,
    onDismissToolkitTour: () -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState = androidx.compose.foundation.rememberScrollState(),
    today: LocalDate = LocalDate.now(),
) {
    val colors = LocalWirdColors.current
    val isDark = colors.isDark
    val currentHour = remember { LocalTime.now().hour }
    val photoRes = if (isDark || currentHour >= 18 || currentHour < 5) {
        R.drawable.header_sunset
    } else {
        R.drawable.header_day
    }

    SetStatusBarAppearance(isLightBackground = false)

    var showSwitchTrackSheet by remember { mutableStateOf(false) }

    val effectiveTracks = remember(allTracks, allSpaces) {
        if (allTracks.isNotEmpty()) allTracks else allSpaces.flatMap { it.tracks }
    }

    if (showSwitchTrackSheet) {
        SwitchTrackSheet(
            activeTrack = activeTrack,
            allTracks = effectiveTracks,
            scheduleMode = scheduleMode,
            onSelectTrack = {
                onSelectTrack(it)
                showSwitchTrackSheet = false
            },
            onToggleScheduleMode = onToggleScheduleMode,
            onCreateTrack = {
                showSwitchTrackSheet = false
                onOpenSettings(SettingsDialog.EDIT_TRACK)
            },
            onDismiss = { showSwitchTrackSheet = false },
        )
    }

    val dueTracks = remember(effectiveTracks) { effectiveTracks.filter { it.isDueToday() } }
    val completedDueTracks = remember(effectiveTracks) {
        effectiveTracks.filter { it.isDueToday() && it.isCompletedToday() }
    }
    val doneTracksCount = completedDueTracks.size
    val totalDueTracksCount = dueTracks.size.coerceAtLeast(1)
    val donePct = ((doneTracksCount.toFloat() / totalDueTracksCount) * 100).toInt().coerceIn(0, 100)

    val hijriText = remember(today) {
        try {
            val hijri = HijrahDate.from(today)
            val dayName = today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
            val day = hijri.get(ChronoField.DAY_OF_MONTH)
            val monthName = when (hijri.get(ChronoField.MONTH_OF_YEAR)) {
                1 -> "Muḥarram"
                2 -> "Ṣafar"
                3 -> "Rabīʿ al-Awwal"
                4 -> "Rabīʿ al-Thānī"
                5 -> "Jumādā al-Ūlā"
                6 -> "Jumādā al-Ākhirah"
                7 -> "Rajab"
                8 -> "Shaʿbān"
                9 -> "Ramaḍān"
                10 -> "Shawwāl"
                11 -> "Dhū al-Qaʿdah"
                12 -> "Dhū al-Ḥijjah"
                else -> "Hijri"
            }
            val year = hijri.get(ChronoField.YEAR)
            "$dayName · $day $monthName $year"
        } catch (_: Exception) {
            val formatter = DateTimeFormatter.ofPattern("EEEE · d MMMM yyyy")
            today.format(formatter)
        }
    }

    val isDone = doneMethod != null || activeTrack?.isCompletedToday() == true

    Box(modifier = modifier.fillMaxSize().background(colors.field)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            // Photographic Mosque Header (Ghana National Mosque)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp),
            ) {
                Image(
                    painter = painterResource(id = photoRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // Veil gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.0f to Color(0xDD182724),
                                0.55f to Color(0x8A182724),
                                0.85f to Color(0x18182724),
                                1.0f to Color.Transparent,
                            )
                        ),
                )

                // Fade to bottom field ground
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                1.0f to colors.field,
                            )
                        ),
                )

                // Greeting content
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 20.dp, top = 16.dp, end = 68.dp),
                ) {
                    Text(
                        text = "Assalamu Alaikum,",
                        fontSize = 15.sp,
                        color = colors.ink2,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = readerName?.takeIf { it.isNotBlank() } ?: "Reader",
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.ink,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = WirdIcons.Leaf,
                            contentDescription = null,
                            tint = colors.action,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = hijriText,
                        fontSize = 13.sp,
                        color = colors.ink2,
                    )
                }

                // Settings gear button at top right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 12.dp, end = 16.dp)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.card.copy(alpha = 0.85f))
                        .clickable { onOpenSettings(null) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = WirdIcons.Gear,
                        contentDescription = "Settings",
                        tint = colors.ink,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // Track chip button at bottom left
                Card(
                    onClick = { showSwitchTrackSheet = true },
                    shape = RoundedCornerShape(23.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 10.dp)
                        .height(46.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(colors.chip, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = WirdIcons.Quran,
                                contentDescription = null,
                                tint = colors.action,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = activeTrack?.name ?: "Daily Reading",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = WirdIcons.ChevronDown,
                            contentDescription = "Switch Track",
                            tint = colors.ink2,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // Cards stack
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // TODAY'S WIRD CARD
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.rule),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Top row: Title + streak/days pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Today's Wird",
                                style = TextStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.ink,
                                ),
                            )
                            val streak = progress?.currentStreak ?: 0
                            val pillText = if (streak > 0) "🔥 $streak day streak" else "📖 ${progress?.totalDaysRead ?: 0} days read"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.tile)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = pillText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.goldText,
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Portion text
                        val surahName = assignment.surahs.firstOrNull()?.name ?: "Al-Fātiḥah"
                        val portionText = if (assignment.verses != null && assignment.verses.isNotEmpty()) {
                            val firstAyah = assignment.verses.first().second
                            val lastAyah = assignment.verses.last().second
                            if (firstAyah == lastAyah) "$surahName $firstAyah" else "$surahName $firstAyah–$lastAyah"
                        } else {
                            surahName
                        }
                        Text(
                            text = portionText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.ink,
                        )

                        Spacer(Modifier.height(2.dp))

                        // Page text
                        val pStart = assignment.startPage
                        val pEnd = assignment.endPage
                        val pageLine = if (pStart == pEnd) "Page $pStart" else "Page $pStart → Page $pEnd"
                        Text(
                            text = pageLine,
                            fontSize = 13.sp,
                            color = colors.ink2,
                        )

                        Spacer(Modifier.height(10.dp))

                        // Chips row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.field)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = WirdIcons.Sheet,
                                        contentDescription = null,
                                        tint = colors.ink2,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = unitsLabel(activeTrack?.dailyUnits ?: 2),
                                        fontSize = 13.sp,
                                        color = colors.ink,
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.field)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    text = "Read · Every day",
                                    fontSize = 13.sp,
                                    color = colors.ink,
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Progress bar row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "$doneTracksCount of $totalDueTracksCount track done today",
                                fontSize = 12.sp,
                                color = colors.ink2,
                            )
                            Text(
                                text = "$donePct%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.ink2,
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        // Progress track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.field),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (totalDueTracksCount > 0) (doneTracksCount.toFloat() / totalDueTracksCount).coerceIn(0f, 1f) else 0f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(colors.action),
                            )
                        }

                        // Done line with Undo if completed
                        if (isDone) {
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(colors.action, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = WirdIcons.Check,
                                        contentDescription = null,
                                        tint = colors.onAction,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (doneMethod == Method.RECITED) "Recited today" else "Marked as read",
                                    fontSize = 15.sp,
                                    color = colors.ink,
                                )
                                Spacer(Modifier.weight(1f))
                                onUndoMarkRead?.let { undo ->
                                    TextButton(
                                        onClick = undo,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = "Undo",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.action,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Actions row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Primary: Recite & Review
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(colors.action)
                                    .clickable { onOpenPage() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = WirdIcons.Mic,
                                        contentDescription = null,
                                        tint = colors.onAction,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Recite & Review",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.onAction,
                                    )
                                }
                            }

                            // Quiet: Mark Done (shown if not done)
                            if (!isDone) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(colors.chip)
                                        .clickable { onMarkRead() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = WirdIcons.Check,
                                            contentDescription = null,
                                            tint = colors.ink,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Mark Done",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.ink,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // YOUR JOURNEY CARD
                val totalDaysRead = progress?.totalDaysRead ?: 0
                val recitedDays = recent.count { it.method == Method.RECITED }
                val reciteRate = if (totalDaysRead > 0) ((recitedDays.toFloat() / totalDaysRead) * 100).toInt().coerceIn(0, 100) else 0

                Card(
                    onClick = { onNavigateTab(WirdTab.WIRD) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.rule),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Your Journey",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.ink,
                            )
                            Icon(
                                imageVector = WirdIcons.ChevronRight,
                                contentDescription = null,
                                tint = colors.ink2,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$totalDaysRead",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ink,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Days read",
                                    fontSize = 12.sp,
                                    color = colors.ink2,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(colors.rule),
                            )

                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    text = "$recitedDays",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ink,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Recited aloud",
                                    fontSize = 12.sp,
                                    color = colors.ink2,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(colors.rule),
                            )

                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    text = "$reciteRate%",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ink,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Recite rate",
                                    fontSize = 12.sp,
                                    color = colors.ink2,
                                )
                            }
                        }
                    }
                }

                // QUICK ACCESS
                Text(
                    text = "Quick access",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.ink,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Qur'an
                    Card(
                        onClick = { onNavigateTab(WirdTab.QURAN) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.rule),
                        modifier = Modifier.weight(1f).height(84.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(colors.disc, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = WirdIcons.Quran,
                                    contentDescription = null,
                                    tint = colors.action,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = "Qur'an",
                                fontSize = 12.sp,
                                color = colors.ink2,
                            )
                        }
                    }

                    // Sūrahs
                    Card(
                        onClick = onOpenSurahs,
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.rule),
                        modifier = Modifier.weight(1f).height(84.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(colors.disc, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = WirdIcons.List,
                                    contentDescription = null,
                                    tint = colors.action,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = "Sūrahs",
                                fontSize = 12.sp,
                                color = colors.ink2,
                            )
                        }
                    }

                    // Bookmarks
                    Card(
                        onClick = onOpenBookmarks,
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.rule),
                        modifier = Modifier.weight(1f).height(84.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(colors.disc, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = WirdIcons.Bookmark,
                                    contentDescription = null,
                                    tint = colors.action,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = "Bookmarks",
                                fontSize = 12.sp,
                                color = colors.ink2,
                            )
                        }
                    }

                    // History
                    Card(
                        onClick = { onNavigateTab(WirdTab.WIRD) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.rule),
                        modifier = Modifier.weight(1f).height(84.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(colors.disc, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = WirdIcons.History,
                                    contentDescription = null,
                                    tint = colors.action,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = "History",
                                fontSize = 12.sp,
                                color = colors.ink2,
                            )
                        }
                    }
                }

                // Bottom spacer for navigation bar clearance
                Spacer(Modifier.height(88.dp))
            }
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
    onOpenTrackPicker: (() -> Unit)? = null,
    today: LocalDate = LocalDate.now(),
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val context = androidx.compose.ui.platform.LocalContext.current
    val surahs = remember(assignment) { assignment.surahs }
    val surah = surahs.firstOrNull() ?: com.mosman.wird.domain.SurahIndex.on(assignment.startPage).firstOrNull()
    val name = surah?.name ?: "Page ${assignment.startPage}"
    // Today's verses only (com.mosman.wird.domain.portionKeys): this used to count every ayah on
    // the portion's pages, so "10 verses" from Al-Baqarah 1 read as "Ayahs 1–16".
    val ayahRange = remember(assignment, surah, activeTrack) {
        val start = activeTrack?.startVerseSurah?.let { s -> activeTrack.startVerseAyah?.let { a -> s to a } }
        if (surah != null) assignment.ayahRangeIn(surah.number, start) else null
    }
    // Every sūrah the portion reads, each with its ayahs: a madrasa day on page 604 is "An-Nas 1–6,
    // Al-Falaq 1–5, Al-Ikhlas 1–4". Naming only the first sūrah hid the other two.
    val readsLabel = remember(assignment, surahs, activeTrack) {
        val start = activeTrack?.startVerseSurah?.let { s -> activeTrack.startVerseAyah?.let { a -> s to a } }
        surahs.mapNotNull { s -> assignment.ayahRangeIn(s.number, start)?.let { "${s.name} ${it.substringAfter(' ')}" } }
            .takeIf { it.size > 1 }
            ?.joinToString(", ")
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
                            .then(
                                if (onOpenTrackPicker != null) {
                                    Modifier.clickable(onClick = onOpenTrackPicker)
                                } else Modifier
                            )
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
                    text = if (readsLabel != null) {
                        "$readsLabel · Page ${assignment.startPage}"
                    } else if (surah != null && ayahRange != null) {
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

        // 3 Clay stat boxes (Daily target, start page, track streak)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val plannedUnits = activeTrack?.plan()?.unitsOn(today.dayOfWeek) ?: assignment.units
            val targetLabel = if (activeTrack?.customTargetVerses != null) {
                "${activeTrack.customTargetVerses} verses"
            } else {
                unitsLabel(plannedUnits).replaceFirstChar { it.uppercase() }
            }
            ClayStatPill(
                value = targetLabel,
                label = "Daily target",
                modifier = Modifier.weight(1f),
            )
            ClayStatPill(
                value = "Page ${assignment.startPage}",
                label = "Start page",
                modifier = Modifier.weight(1f),
            )
            ClayStatPill(
                value = "${activeTrack?.currentStreak ?: 0}d 🔥",
                label = "Track streak",
                valueColor = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        val done = doneMethod != null

        // Sūrah completion progress loader (measures progress through this sūrah rather than the whole mushaf)
        val surahSpan = surah?.let { (it.lastPage - it.firstPage + 1).coerceAtLeast(1) } ?: 1
        val baseInto = surah?.let { (assignment.startPage - it.firstPage).coerceAtLeast(0) } ?: 0
        val pagesDoneToday = if (done) (assignment.units / 2).coerceAtLeast(1) else 0
        val pagesCompleted = (baseInto + pagesDoneToday).coerceIn(0, surahSpan)
        val isSurahComplete = done && pagesCompleted >= surahSpan
        val surahFraction = if (isSurahComplete) 1f else (pagesCompleted.toFloat() / surahSpan).coerceIn(0f, 1f)
        val surahPercentLeft = if (isSurahComplete) 0 else ((1f - surahFraction) * 100).toInt()
        val surahPagesLeft = if (isSurahComplete) 0 else (surahSpan - pagesCompleted).coerceAtLeast(0)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isSurahComplete) "✓ Sūrah ${surah?.name ?: ""} Completed!" else "📖 Sūrah ${surah?.name ?: "Completion"}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF8ED676) else Color(0xFF245847),
            )
            Text(
                text = if (isSurahComplete) "0% left" else "$surahPercentLeft% left",
                fontSize = 11.sp,
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
                    .fillMaxWidth(surahFraction.coerceAtLeast(0.04f))
                    .height(7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) Color(0xFF68BD5B) else Color(0xFF245847)),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isSurahComplete) {
                    "All $surahSpan page${if (surahSpan != 1) "s" else ""} completed"
                } else if (surah != null) {
                    "$pagesCompleted of $surahSpan page${if (surahSpan != 1) "s" else ""} completed"
                } else {
                    "Page ${assignment.startPage}"
                },
                fontSize = 10.5.sp,
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
            )
            Text(
                text = if (isSurahComplete) {
                    "Sūrah complete"
                } else if (surahPagesLeft == 0) {
                    "Last page of sūrah"
                } else {
                    "$surahPagesLeft page${if (surahPagesLeft != 1) "s" else ""} remaining"
                },
                fontSize = 10.5.sp,
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
            )
        }

        Spacer(Modifier.height(16.dp))

        // ---- the three tactile action tiles on the exact same row (Candidate 1) ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionTile(
                label = "Recite & Review",
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
                label = "Open Page",
                glyph = { BookGlyph(it) },
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onClick = onOpenPage,
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Recite today's portion and we'll check your recitation.",
                fontSize = 9.5.sp,
                lineHeight = 12.5.sp,
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (!done) "Already recited it? Mark today complete." else "Portion completed for today.",
                fontSize = 9.5.sp,
                lineHeight = 12.5.sp,
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Read the mushaf page directly.",
                fontSize = 9.5.sp,
                lineHeight = 12.5.sp,
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF6A7C73),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
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
private fun NumbersCard(p: Progress, activeTrack: ReadingTrack? = null) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)
    val streakVal = activeTrack?.currentStreak ?: p.currentStreak
    val totalDaysVal = if (activeTrack != null && activeTrack.totalDaysRead > 0) activeTrack.totalDaysRead else p.totalDaysRead
    val aloudRatio = if (totalDaysVal > 0) {
        val recited = p.recitedDays
        (recited * 100) / totalDaysVal
    } else 0

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
                text = activeTrack?.name ?: "Overall progress",
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
            val streakColor = if (isDark) Color(0xFFE67E22) else Color(0xFFD35400)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "🔥 $streakVal",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = streakColor,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Day Streak",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
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
            val daysColor = if (isDark) Color(0xFF92E2B6) else Color(0xFF1E3F32)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "📖 $totalDaysVal",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = daysColor,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Total Days",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF8C7D6B).copy(alpha = 0.2f))
            )

            // Col 3: Recited aloud
            val aloudColor = if (isDark) Color(0xFF50A773) else Color(0xFF245847)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "🎙️ $aloudRatio%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = aloudColor,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Recited aloud",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
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
    val amount = unitsLabel(a.units).replaceFirstChar { it.uppercase() }
    val span = if (a.startPage == a.endPage) null else "to ${a.endPage}"
    val state = when (doneMethod) {
        Method.RECITED -> "recited aloud"
        Method.TAPPED -> "marked as read"
        null -> "not yet marked"
    }
    return listOfNotNull(amount, span, state).joinToString(" · ")
}

/**
 * Encouraging sequential banner shown when the active track is completed today
 * and another recitation track in the same mode is still due today.
 */
@Composable
private fun NextDueTrackBanner(
    nextTrack: com.mosman.wird.domain.ReadingTrack,
    onContinueTrack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = if (isDark) Color(0xFF183025) else Color(0xFFE4F1E9),
                elevation = 3.dp,
            )
            .clickable(onClick = onContinueTrack)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clayPill(
                            shape = CircleShape,
                            backgroundColor = if (isDark) Color(0xFF204234) else Color(0xFFD2E8DB),
                            elevation = 1.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    TrackTypeGlyph(
                        type = nextTrack.type,
                        tint = if (isDark) Color(0xFF8DE0A6) else Color(0xFF1A5A3C),
                        modifier = Modifier.size(17.dp),
                    )
                }
                Column {
                    Text(
                        text = "Next recitation due today",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF8DE0A6) else Color(0xFF245847),
                        letterSpacing = 0.5.sp,
                    )
                    Text(
                        text = nextTrack.name,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
                    )
                    Text(
                        text = "${nextTrack.surahName()} · ${nextTrack.scheduleLabel()}",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clayPill(
                        shape = RoundedCornerShape(999.dp),
                        backgroundColor = if (isDark) Color(0xFF204234) else Color(0xFF1B4E38),
                        elevation = 2.dp,
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

/**
 * Guide card shown when user has no recitation tracks yet.
 */
@Composable
private fun EmptyTracksGuideCard(
    onAddTrack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    val isDark = colors.surface == Color(0xFF212121) || colors.surface == Color(0xFF191A1E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clayCard(
                shape = RoundedCornerShape(20.dp),
                backgroundColor = if (isDark) Color(0xFF15261F) else Color(0xFFFBF8F1),
                elevation = 4.dp,
            )
            .padding(20.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TrackTypeGlyph(
                type = TrackType.TILAWAH,
                tint = if (isDark) Color(0xFF8DE0A6) else Color(0xFF245847),
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = "Set Up Your Reading Tracks",
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF7F5ED) else Color(0xFF17382D),
            )
            Text(
                text = "Wird adapts to your real life. Add parallel reading tracks (like Daily Tilāwah, Weekend Ḥifẓ, or Ramadan Khatmah) with independent schedules and daily targets.",
                fontSize = 12.5.sp,
                color = if (isDark) Color(0xFF8FA597) else Color(0xFF4B5551),
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clayPill(
                        shape = RoundedCornerShape(999.dp),
                        backgroundColor = if (isDark) Color(0xFF1F4837) else Color(0xFF245847),
                        elevation = 2.dp,
                    )
                    .clickable(onClick = onAddTrack)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "+ Add Your First Reading Track",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}
