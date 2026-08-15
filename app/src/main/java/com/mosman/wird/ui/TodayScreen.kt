package com.mosman.wird.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.mosman.wird.domain.Assignment
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.linesOn
import com.mosman.wird.domain.pages
import com.mosman.wird.mushaf.MushafRepository
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale
import kotlinx.coroutines.delay

/** Long enough to notice and read, short enough not to feel like a splash screen. */
private const val FIRST_RUN_CHROME_MS = 3_500L

/**
 * Today's portion, and the mushaf around it.
 *
 * The portion is the front door: the app opens here, on this page, marked. Sacred Rule 5.
 *
 * **The page carries no permanent chrome.** Tap it and a slim bar appears with the ways
 * out — settings, another surah, back to today; tap again and it goes. That is what every
 * serious reading app does, and it is the only arrangement that survives more controls
 * being added: the links used to sit at the foot of the page, which meant scrolling to the
 * end of the Qur'an to reach settings. Fine for one link, wrong for four, and task 6 adds
 * the most important one of all.
 */
@Composable
fun TodayScreen(
    assignment: Assignment,
    /** Where the reader said they were, when that is partway down the first page. */
    startVerse: Pair<Int, Int>? = null,
    onSettings: () -> Unit = {},
    /** False until the reader has been shown, once, that the page is tappable. */
    hasSeenChrome: Boolean = true,
    onChromeSeen: () -> Unit = {},
) {
    val colors = LocalWirdColors.current
    val todaysPages = remember(assignment) { assignment.pages }
    var showJump by remember { mutableStateOf(false) }
    var goTo by remember { mutableIntStateOf(todaysPages.first()) }
    var current by remember { mutableIntStateOf(todaysPages.first()) }
    // Read off the page actually on screen, so the bar never states anything the API did
    // not say. Both are blank until the first page finishes drawing.
    var currentSurah by remember { mutableStateOf("") }
    var currentJuz by remember { mutableIntStateOf(0) }

    // Open with the bar showing the very first time, then let it go.
    //
    // Nothing on a clean page announces that it is tappable. Mutalib asked whether this
    // should be a hamburger icon; it would announce itself, but at the price of a mark
    // parked on the Qur'an forever. Showing the bar once and withdrawing it teaches the
    // same gesture and leaves the page alone afterwards.
    var chromeShown by remember { mutableStateOf(!hasSeenChrome) }

    LaunchedEffect(hasSeenChrome) {
        if (hasSeenChrome) return@LaunchedEffect
        delay(FIRST_RUN_CHROME_MS)
        chromeShown = false
        onChromeSeen()
    }

    // Warm one page either side of today's, once, in the background.
    val context = LocalContext.current
    LaunchedEffect(todaysPages) {
        MushafRepository(context).prefetchAround(todaysPages.first())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MushafPager(
            initialPage = goTo,
            modifier = Modifier.safeDrawingPadding(),
            onPageChanged = { current = it },
            onBackgroundTap = { chromeShown = !chromeShown },
            onPageShown = { page ->
                if (page.page == current) {
                    currentSurah = page.surahName
                    currentJuz = page.juz
                }
            },
            lit = { page ->
                // One rule, no exceptions: dark means today, pale means not today — on
                // every page, including the ones you swipe to. A signal that holds only
                // sometimes is not a signal.
                if (page.page !in todaysPages) {
                    emptySet()
                } else {
                    val byPage = assignment.linesOn(page.page, page.lines)
                    val startLine = startVerse?.let { (s, a) -> page.lineOf(s, a) }
                    if (startLine == null) byPage else byPage.filter { it >= startLine }.toSet()
                }
            },
        )

        AnimatedVisibility(
            visible = chromeShown,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            ChromeBar(
                surah = currentSurah,
                page = current,
                juz = currentJuz,
                offToday = current !in todaysPages,
                onBackToToday = { goTo = todaysPages.first(); chromeShown = false },
                onJump = { showJump = true; chromeShown = false },
                onSettings = { chromeShown = false; onSettings() },
            )
        }
    }

    if (showJump) {
        SurahJumpSheet(
            onDismiss = { showJump = false },
            onPick = { surah: Surah ->
                goTo = surah.firstPage
                showJump = false
            },
        )
    }
}

/**
 * Where you are, and the ways out. Hidden until asked for; closes once you have chosen.
 *
 * Shaped after the app Mutalib actually reads in: the place you are on the left as a name
 * and a page, the actions as icons on the right. Icons rather than words *here* because
 * the bar now has a title to carry — two lines of prose plus two more of buttons would be
 * a paragraph sitting on a Qur'an page.
 *
 * "Today's portion" keeps its words. It is the one action that needs explaining, it only
 * appears when you have wandered off, and no glyph means "back to the bit you were
 * supposed to read".
 */
@Composable
private fun ChromeBar(
    surah: String,
    page: Int,
    juz: Int,
    offToday: Boolean,
    onBackToToday: () -> Unit,
    onJump: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = LocalWirdColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised)
            .safeDrawingPadding()
            .padding(horizontal = Scale.space4, vertical = Scale.space2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = surah,
                color = colors.onSurfaceRaised,
                style = TextStyle(fontSize = Scale.body),
            )
            Text(
                text = if (juz > 0) "Page $page, Juz' $juz" else "Page $page",
                color = colors.onSurfaceRaised,
                style = TextStyle(fontSize = Scale.caption),
            )
        }

        if (offToday) {
            TextButton(
                onClick = onBackToToday,
                modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
            ) {
                Text(
                    text = "Today's portion",
                    color = colors.onSurfaceRaised,
                    style = TextStyle(fontSize = Scale.caption),
                )
            }
        }

        BarIcon(Icons.AutoMirrored.Filled.List, "Read something else", onJump)
        BarIcon(Icons.Filled.Settings, "Settings", onSettings)
    }
}

@Composable
private fun BarIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    IconButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minWidth = Scale.minTarget, minHeight = Scale.minTarget),
    ) {
        // The label is the screen-reader name. An icon with no name is a button nobody
        // using TalkBack can identify.
        Icon(imageVector = icon, contentDescription = label, tint = colors.onSurfaceRaised)
    }
}

// No hand-written juz table lives here. The first draft of this file had one — thirty page
// numbers typed from memory — which is precisely the kind of Qur'anic metadata this
// project refuses to guess at. The API reports `juz_number` for every page and
// MushafPage already carries it, so the bar reads the page it is actually showing.
