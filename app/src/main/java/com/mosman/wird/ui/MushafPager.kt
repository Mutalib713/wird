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
import kotlinx.coroutines.delay
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
 * **Only the page you stop on costs data.** Two things together do that:
 *
 * 1. `beyondViewportPageCount = 0`, so neighbours are not kept composed. On its own this
 *    was not enough — a measured swipe still composed 441 *and* 442 while settling.
 * 2. [SETTLE_BEFORE_FETCH_MS] of stillness before anything is fetched. A page you swipe
 *    past is disposed long before that, which cancels its load. So flicking through
 *    twenty pages downloads nothing at all.
 *
 * This matters because browsing is the one way this app could quietly cost someone real
 * money: at 154 KB a page, a careless browse used to be a month of reading. Everything is
 * cached on disk after the first fetch, so a page is only ever paid for once.
 */
/**
 * How long a page must stay on screen before its font is downloaded. Long enough that
 * flicking costs nothing, short enough that stopping does not feel broken.
 */
private const val SETTLE_BEFORE_FETCH_MS = 450L

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
                // The settle delay exists to protect data, so it only applies to pages
                // that would actually cost some. A page already on disk is drawn straight
                // away — waiting to decide whether to download something you already have
                // is waiting for nothing, and it is what made swiping back to a page you
                // had just read feel broken.
                if (!repo.isCached(pageNumber)) {
                    // Mutalib's idea: show the skeleton, and only fetch if the reader is
                    // still here a moment later. Pages you swipe past are disposed before
                    // this elapses, which cancels them. Five fast swipes fetched nothing.
                    delay(SETTLE_BEFORE_FETCH_MS)
                }
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
