package com.mosman.wird.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay
import com.mosman.wird.domain.JuzIndex
import com.mosman.wird.domain.SurahIndex
import com.mosman.wird.data.WirdStore
import com.mosman.wird.domain.Mushaf
import com.mosman.wird.mushaf.MushafPage
import com.mosman.wird.mushaf.MushafRepository
import com.mosman.wird.mushaf.PageState

private fun isWifi(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

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

/**
 * A request to jump somewhere.
 *
 * Carries a [nonce] because the page alone is not enough: swipe away from today's page and
 * back is a jump to the same number, and a plain Int would compare equal and do nothing.
 * That is exactly the bug Mutalib hit — "Today's portion" did nothing once he had wandered
 * off and returned.
 */
data class PageJump(val page: Int, val nonce: Int)

@Composable
fun MushafPager(
    initialPage: Int,
    lit: (MushafPage) -> Set<String>,
    modifier: Modifier = Modifier,
    highlightPortion: Boolean = true,
    allowedPageRange: IntRange? = null,
    /** Set to move the pager after it has been composed. */
    jump: PageJump? = null,
    onPageChanged: (Int) -> Unit = {},
    onWordTap: ((verseKey: String) -> Unit)? = null,
    onBackgroundTap: (() -> Unit)? = null,
    /**
     * The ayah being recited right now, or null when nothing is playing.
     *
     * Sacred Rule 5 says the portion is marked by everything *else* stepping back rather
     * than by painting a wash over the Qur'an, so this follows the same mechanic one level
     * down: while audio plays, the ayah you are hearing keeps full ink and the rest of the
     * portion recedes to the same slate used for text outside today's reading. No new
     * colour is introduced, and both pairs are already contrast-checked (16.68:1 and
     * 3.72:1). A tint was not an option regardless — deep teal on ink is 1.78:1 and sage on
     * paper is 1.69:1, measured when the ayah numerals were tried and rejected.
     */
    reciting: String? = null,
    /** Ayahs the recitation check could not follow. PLAN task 14. */
    review: Set<String> = emptySet(),
    selected: String? = null,
    selectedVerses: Set<String> = emptySet(),
    onWordLongPress: ((String) -> Unit)? = null,
    onPageShown: (MushafPage) -> Unit = {},
    footer: @Composable (MushafPage) -> Unit = {},
) {
    val context = LocalContext.current
    val repo = remember { MushafRepository(context) }
    val states = remember { mutableStateMapOf<Int, PageState>() }
    var retryTick by remember { mutableIntStateOf(0) }

    // The skeleton is shown once per visit to the app, then never again.
    //
    // It exists to say "this is loading, not broken". That only needs saying once: by the
    // time someone has swiped past the prefetched pages they have decided to browse, and
    // a placeholder flashing at every page turn is a flicker rather than information.
    // Held in `remember`, so leaving the app and coming back starts the count again —
    // which is what Mutalib asked for.
    var skeletonSpent by remember { mutableStateOf(false) }

    val minPage = allowedPageRange?.first?.coerceIn(1, Mushaf.PAGES) ?: 1
    val maxPage = allowedPageRange?.last?.coerceIn(minPage, Mushaf.PAGES) ?: Mushaf.PAGES
    val totalPages = maxPage - minPage + 1

    val pagerState = rememberPagerState(
        initialPage = (initialPage - minPage).coerceIn(0, totalPages - 1),
        pageCount = { totalPages },
    )

    LaunchedEffect(pagerState, minPage) {
        snapshotFlow { pagerState.currentPage }.collect { onPageChanged(minPage + it) }
    }

    // `rememberPagerState` reads initialPage exactly once, so changing it later moves
    // nothing. Jumping has to be an explicit instruction.
    LaunchedEffect(jump) {
        val target = jump ?: return@LaunchedEffect
        pagerState.scrollToPage((target.page - minPage).coerceIn(0, totalPages - 1))
    }

    // Swiping right turns towards page 2, the way a mushaf opens.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            beyondViewportPageCount = 0,
        ) { index ->
            val pageNumber = minPage + index
            val state = states[pageNumber] ?: PageState.Loading

            // Captured when this page first composes, before the effect below spends it.
            val dressAsSkeleton = remember(pageNumber) { !skeletonSpent }

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
                    skeletonSpent = true
                    delay(SETTLE_BEFORE_FETCH_MS)
                }
                states[pageNumber] = repo.load(pageNumber)
            }

            // The page itself reads left-to-right for its chrome; only the pager and the
            // ayah rows are right-to-left.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                MushafPageView(
                    state = state,
                    pageNumber = pageNumber,
                    lit = lit,
                    highlightPortion = highlightPortion,
                    reciting = reciting,
                    review = review,
                    selected = selected,
                    selectedVerses = selectedVerses,
                    onWordLongPress = onWordLongPress,
                    showSkeleton = dressAsSkeleton,
                    onBackgroundTap = onBackgroundTap,
                    onPageShown = onPageShown,
                    onRetry = { states.remove(pageNumber); retryTick++ },
                    onWordTap = onWordTap,
                    footer = footer,
                )
            }
        }
    }
}
