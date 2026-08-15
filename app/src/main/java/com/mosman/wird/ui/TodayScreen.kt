package com.mosman.wird.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.mosman.wird.domain.Assignment
import com.mosman.wird.domain.Surah
import com.mosman.wird.domain.linesOn
import com.mosman.wird.domain.pages
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * Today's portion, and the mushaf around it.
 *
 * The portion is the front door: the app opens here, on this page, marked. Swiping goes
 * to the pages either side, and "go to surah" jumps anywhere — but neither is a screen
 * you have to pass through first. Sacred Rule 5.
 */
@Composable
fun TodayScreen(
    assignment: Assignment,
    /** Where the reader said they were, when that is partway down the first page. */
    startVerse: Pair<Int, Int>? = null,
) {
    val colors = LocalWirdColors.current
    val todaysPages = remember(assignment) { assignment.pages }
    var showJump by remember { mutableStateOf(false) }
    var goTo by remember { mutableIntStateOf(todaysPages.first()) }
    var current by remember { mutableIntStateOf(todaysPages.first()) }

    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        MushafPager(
            initialPage = goTo,
            onPageChanged = { current = it },
            lit = { page ->
                // Only today's pages carry the marking. Wander off and the mushaf is just
                // the mushaf — nothing is dimmed, because none of it is today's.
                if (page.page !in todaysPages) {
                    page.lines.toSet()
                } else {
                    val byPage = assignment.linesOn(page.page, page.lines)
                    val startLine = startVerse?.let { (s, a) -> page.lineOf(s, a) }
                    if (startLine == null) byPage else byPage.filter { it >= startLine }.toSet()
                }
            },
            footer = { page ->
                PageFooter(
                    page = page.page,
                    todaysPages = todaysPages,
                    onBackToToday = { goTo = todaysPages.first() },
                    onJump = { showJump = true },
                )
            },
        )
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
 * The one bit of chrome on the reading surface, and it only says something when there is
 * something to say: that you have wandered off today's page, and how to get back.
 */
@Composable
private fun PageFooter(
    page: Int,
    todaysPages: List<Int>,
    onBackToToday: () -> Unit,
    onJump: () -> Unit,
) {
    val colors = LocalWirdColors.current
    if (page in todaysPages) {
        TextButton(onClick = onJump, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Read something else",
                color = colors.textSecondary,
                style = TextStyle(fontSize = Scale.caption, textAlign = TextAlign.Center),
            )
        }
    } else {
        TextButton(onClick = onBackToToday, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Back to today's portion",
                color = colors.accent,
                style = TextStyle(fontSize = Scale.caption, textAlign = TextAlign.Center),
            )
        }
    }
}
