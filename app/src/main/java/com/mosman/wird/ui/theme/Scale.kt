package com.mosman.wird.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Type and spacing tokens. No component carries a raw value — random values are how the
 * generic look creeps back in.
 */
object Scale {
    // Type: 1.25 ratio off a 16sp body. Body is never below 16sp on mobile.
    val caption = 13.sp
    val body = 16.sp
    val title = 20.sp
    val display = 25.sp

    // The mushaf sits outside the scale on purpose. It is not chrome, and its size is
    // driven by fitting fifteen lines on a phone screen, not by a typographic ratio.
    val mushafLine = 28.sp

    // 4/8-pt grid.
    val space1 = 4.dp
    val space2 = 8.dp
    val space3 = 12.dp
    val space4 = 16.dp
    val space6 = 24.dp
    val space8 = 32.dp

    /** Fitts: nothing tappable is smaller than this. */
    val minTarget = 48.dp

    val radius = 6.dp

    /**
     * The radius a card is closed with. **§ 6e.**
     *
     * Bigger than [radius] because these are objects rather than controls: his comps hold
     * the portion, the check-in, the numbers and the week each inside its own rounded plate,
     * and a 6dp corner on a full-width plate reads as a mistake rather than as a choice.
     * Controls keep [radius]; only the plates get this.
     */
    val card = 16.dp
}
