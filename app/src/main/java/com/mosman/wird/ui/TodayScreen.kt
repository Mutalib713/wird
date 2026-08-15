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
) {
    val colors = LocalWirdColors.current
    val todaysPages = remember(assignment) { assignment.pages }
    var showJump by remember { mutableStateOf(false) }
    var goTo by remember { mutableIntStateOf(todaysPages.first()) }
    var current by remember { mutableIntStateOf(todaysPages.first()) }
    var chromeShown by remember { mutableStateOf(false) }

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
 * The ways out. Hidden until asked for, and it closes itself once you have chosen one.
 *
 * Deliberately words rather than icons: there are three of them, they are all one-off
 * actions rather than a place you live in, and a row of little glyphs over a Qur'an page
 * would need explaining. Naming things costs nothing here.
 */
@Composable
private fun ChromeBar(
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
            .padding(horizontal = Scale.space2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (offToday) {
            BarAction("Today's portion", onBackToToday)
        } else {
            BarAction("Read something else", onJump)
        }
        BarAction("Settings", onSettings)
    }
}

@Composable
private fun BarAction(label: String, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    TextButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = Scale.minTarget),
    ) {
        Text(
            text = label,
            color = colors.onSurfaceRaised,
            style = TextStyle(fontSize = Scale.caption),
        )
    }
}
