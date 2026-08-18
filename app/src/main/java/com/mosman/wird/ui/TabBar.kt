package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * The three places the app has.
 *
 * **Reduced from four and moved to the top on 2026-08-18**, at Mutalib's word: *"the tab, I
 * think it should come top rather… but in time it will be home, surahs and history. For the
 * home there will be that three vertical dot menu with the settings and other things there."*
 *
 * **More stopped being a place and became a menu.** That is the honest shape: Settings was
 * never a destination you visit alongside your wird, it was a drawer you open, do one thing
 * in, and leave. A quarter of the navigation bar was spent on it.
 *
 * The order is his and matches what the app is for: where you are today, the whole Qur'an,
 * what you have already done.
 */
enum class WirdTab(val label: String) {
    HOME("Home"),
    SURAHS("Sūrahs"),
    HISTORY("History"),
}

/**
 * The toolbar and the tabs, as one block at the top of the screen.
 *
 * Modelled on Quran for Android, which Mutalib sent as the reference: a title row carrying
 * the app's name and an overflow menu, and under it a row of tabs marked by an underline
 * rather than by a filled pill.
 *
 * **Why the underline and not the old icon-over-label bar:** at the bottom, tabs are thumb
 * targets and want mass. At the top they are a *heading* — you read them once and then read
 * past them — so weight there competes with the screen's own title. The underline says which
 * one without adding a second bold thing to the page.
 *
 * Colour is not the only signal, same as before: the active tab is accented **and**
 * full-weight **and** underlined.
 */
@Composable
fun WirdTopBar(
    current: WirdTab,
    onPick: (WirdTab) -> Unit,
    /** Opens the overflow. Null hides it — screens that are not Home have nothing in it. */
    onMenu: (() -> Unit)? = null,
    /**
     * The overflow's contents, rendered *inside* the button.
     *
     * A `DropdownMenu` anchors to wherever it sits in the layout, so it has to be a child of
     * the icon. Parented to the content area instead, it opened at the bottom-left of the
     * screen — found on the emulator, not by reading this.
     */
    menu: @Composable () -> Unit = {},
) {
    val colors = LocalWirdColors.current

    Column(modifier = Modifier.fillMaxWidth().background(colors.surfaceRaised)) {
        // ---- title row ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = Scale.space4, end = Scale.space2, top = Scale.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "WIRD",
                color = colors.accent,
                style = TextStyle(
                    fontSize = 15.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.weight(1f),
            )
            onMenu?.let { open ->
                Box {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = colors.textPrimary,
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable(onClick = open)
                            .defaultMinSize(minWidth = Scale.minTarget, minHeight = Scale.minTarget)
                            .padding(Scale.space3),
                    )
                    menu()
                }
            }
        }

        // ---- tabs ----
        // **Each tab gets an equal share, explicitly.** The underline is a full-width Box
        // inside each tab, and without a weight the first tab measures itself at the whole
        // row and squeezes the other two to nothing — they vanished from the layout entirely,
        // which is how this was caught: an element with no width does not appear in
        // `uiautomator dump` at all.
        Row(modifier = Modifier.fillMaxWidth()) {
            WirdTab.entries.forEach { t ->
                TabItem(t, t == current, onPick, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: WirdTab,
    active: Boolean,
    onPick: (WirdTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    val tint = if (active) colors.accent else colors.textSecondary

    Column(
        modifier = modifier
            .clickable { onPick(tab) }
            .defaultMinSize(minHeight = Scale.minTarget),
        horizontalAlignment = Alignment.CenterHorizontally,
        // Bottom, so the underline sits on the bar's own edge. The 48dp minimum is a touch
        // target and is usually taller than the label needs; anchoring to the top left that
        // slack *under* the mark and read as an empty band below the tabs.
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = tab.label.uppercase(),
            color = tint,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            ),
            modifier = Modifier.padding(vertical = Scale.space3),
        )
        // The mark sits under the label, on the edge the content starts from.
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (active) colors.accent else Color.Transparent)
        )
    }
}
