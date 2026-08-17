package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * Where the app can be, now that Sacred Rule 5's "no menu in between" is lifted
 * (PROFILE.md § 6, reversed 2026-08-17 — Mutalib: *"reopen rule 5, i like the tabs"*).
 *
 * **Four, not the design's five.** Its set was `TODAY · SŪRAHS · READ · RECITE · MORE`, and
 * READ is redundant here: in Wird, Today *is* the reading screen — tapping Today already
 * lands you on the mushaf page. A tab that goes where the previous tab already went is a
 * tab people learn to ignore, and the whole reason this bar exists is to make five places
 * findable rather than to have five of them. Mutalib gave explicit licence to rearrange.
 *
 * Words, not icons. The same argument as the chrome bar in task 5e: there is no glyph that
 * means "the bit you were supposed to read today", and a row of little pictures over a
 * Qur'an app needs explaining. Four short words fit across a phone comfortably — measured
 * in the weekday-chip fix, a 4-across row has room to spare.
 */
enum class WirdTab(val label: String) {
    TODAY("Today"),
    SURAHS("Sūrahs"),
    RECITE("Recite"),
    MORE("More"),
}

/**
 * The bottom bar.
 *
 * Deliberately not a Material `NavigationBar`: that component brings its own container
 * colour, its own indicator pill and its own 80dp height, and overriding all three to reach
 * the palette is more work than drawing a row. It is also the component most responsible
 * for the "boxy" feeling Mutalib objected to.
 *
 * The active tab is marked by **a gold rule above it and full-strength text**, not by a
 * filled pill. Gold as a rule rather than a ground is the same discipline the palette
 * forced everywhere else: gold is unreadable as text on light, and a filled gold pill with
 * a label on it would be exactly that mistake at the bottom of every screen.
 */
@Composable
fun WirdTabBar(current: WirdTab, onPick: (WirdTab) -> Unit) {
    val colors = LocalWirdColors.current
    Column(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
        // A single hairline across the whole bar, so the bar reads as one edge rather than
        // as four boxes sitting in a row.
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.textOutsidePortion.copy(alpha = 0.35f))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            WirdTab.entries.forEach { tab ->
                TabItem(tab = tab, active = tab == current, onPick = { onPick(tab) })
            }
        }
    }
}

@Composable
private fun TabItem(tab: WirdTab, active: Boolean, onPick: () -> Unit) {
    val colors = LocalWirdColors.current
    Column(
        modifier = Modifier
            .defaultMinSize(minHeight = Scale.minTarget)
            .clickable(onClick = onPick)
            .padding(horizontal = Scale.space2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The mark sits above the word, so the words themselves stay on one baseline.
        Box(
            Modifier
                .padding(top = Scale.space1)
                .width(20.dp)
                .height(2.dp)
                .background(if (active) colors.ornament else androidx.compose.ui.graphics.Color.Transparent)
        )
        Spacer(Modifier.height(Scale.space2))
        Text(
            text = tab.label,
            color = if (active) colors.textPrimary else colors.textSecondary,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                letterSpacing = 0.4.sp,
            ),
        )
        Spacer(Modifier.height(Scale.space3))
    }
}
