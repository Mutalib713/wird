package com.mosman.wird.ui

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.mushaf.MushafPage
import com.mosman.wird.mushaf.MushafRepository
import com.mosman.wird.mushaf.PageState

/**
 * The mushaf, swipeable.
 *
 * Right-to-left, like the book: swiping right goes forward, because that is the direction
 * you turn a page in a mushaf.
 *
 * **Fonts are fetched a page at a time, never ahead.** `beyondViewportPageCount = 0` stops
 * the pager keeping neighbours composed. Measured caveat: during a drag it still composes
 * the page you are swiping towards *and* the one after, so one flick pulled 441 and 442.
 * That is the pager settling, not a prefetch policy, and it costs at most one extra page
 * per swipe — but it means "nothing is loaded until you land on it" would be a false
 * claim.
 *
 * This matters because browsing is the one way this app could quietly cost someone real
 * data: at 154 KB a page, flicking through thirty pages is a month of reading. Everything
 * is cached on disk forever after, so a page is only ever paid for once.
 */
@Composable
fun MushafPager(
    initialPage: Int,
    lit: (MushafPage) -> Set<Int>,
    modifier: Modifier = Modifier,
    onPageChanged: (Int) -> Unit = {},
    onWordTap: ((verseKey: String) -> Unit)? = null,
    footer: @Composable (MushafPage) -> Unit = {},
) {
    val context = LocalContext.current
    val repo = remember { MushafRepository(context) }
    val states = remember { mutableStateMapOf<Int, PageState>() }
    var retryTick by remember { mutableIntStateOf(0) }

    val pagerState = rememberPagerState(
        initialPage = (initialPage - 1).coerceIn(0, Mushaf.PAGES - 1),
        pageCount = { Mushaf.PAGES },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onPageChanged(it + 1) }
    }

    // Swiping right turns towards page 2, the way a mushaf opens.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            beyondViewportPageCount = 0,
        ) { index ->
            val pageNumber = index + 1
            val state = states[pageNumber] ?: PageState.Loading

            LaunchedEffect(pageNumber, retryTick) {
                if (states[pageNumber] is PageState.Ready) return@LaunchedEffect
                states[pageNumber] = PageState.Loading
                states[pageNumber] = repo.load(pageNumber)
            }

            // The page itself reads left-to-right for its chrome; only the pager and the
            // ayah rows are right-to-left.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                MushafPageView(
                    state = state,
                    lit = lit,
                    onRetry = { states.remove(pageNumber); retryTick++ },
                    onWordTap = onWordTap,
                    footer = footer,
                )
            }
        }
    }
}
