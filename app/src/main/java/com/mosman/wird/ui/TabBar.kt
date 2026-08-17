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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * The four places the app has.
 *
 * Order and names are Mutalib's, 2026-08-17: *"the first tab should be the home tab, second
 * with the surah, third change the name since it has something to do with history, and
 * fourth will be more."*
 *
 * [HISTORY] was called Recite until he renamed it, and the rename is the more honest label
 * — reciting happens at the foot of today's page, not in a tab. What lives here is the
 * record of what you already did.
 */
enum class WirdTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    SURAHS("Sūrahs", Icons.AutoMirrored.Filled.List),
    HISTORY("History", Icons.Filled.DateRange),
    MORE("More", Icons.Filled.Settings),
}

/**
 * Icon above a label, the way every app does it.
 *
 * The first version was words alone, and Mutalib's note was direct: *"that's not how I like
 * it — do it like how apps do it, with a favicon and the name beneath it."* He is right, and
 * it is not only familiarity: four bare words in a row read as a sentence rather than as
 * four destinations, and nothing showed which was current except weight.
 *
 * **The rule the rest of the app follows does not apply down here.** On the page, meaning is
 * carried by ink and paleness and never by colour alone. A tab bar is the opposite case —
 * it is chrome, it is glanced at rather than read, and it earns colour. The active tab is
 * gold *and* full-weight *and* topped by a gold rule, so it survives being colour-blind or
 * looked at sideways in the dark.
 */
@Composable
fun WirdTabBar(current: WirdTab, onPick: (WirdTab) -> Unit) {
    val colors = LocalWirdColors.current
    Column(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
        // One hairline across the whole bar, not a box around each tab. Mutalib's word for
        // the design that did the latter was "boxy".
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.textOutsidePortion.copy(alpha = 0.3f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = Scale.space1),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            WirdTab.entries.forEach { t -> TabItem(t, t == current, onPick) }
        }
    }
}

@Composable
private fun TabItem(tab: WirdTab, active: Boolean, onPick: (WirdTab) -> Unit) {
    val colors = LocalWirdColors.current
    val tint = if (active) colors.accent else colors.textSecondary

    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = Scale.minTarget, minHeight = Scale.minTarget)
            .clickable { onPick(tab) }
            .padding(horizontal = Scale.space2, vertical = Scale.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The marker sits above the icon rather than under the label, so it reads as
        // "this one" without adding height below where the gesture bar already is.
        Box(
            Modifier
                .width(Scale.space6)
                .height(2.dp)
                .background(if (active) colors.accent else androidx.compose.ui.graphics.Color.Transparent)
        )
        Spacer(Modifier.height(Scale.space1))
        Icon(
            imageVector = tab.icon,
            // The label is right underneath, so repeating it here would make TalkBack say
            // everything twice.
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            color = tint,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = 0.3.sp,
            ),
        )
    }
}
