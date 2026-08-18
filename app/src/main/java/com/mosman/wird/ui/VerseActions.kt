package com.mosman.wird.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mosman.wird.ui.theme.LocalWirdColors
import com.mosman.wird.ui.theme.Scale

/**
 * What you can do with an ayah you long-pressed.
 *
 * Ported from the reference Mutalib sent: in Quran for Android, holding an ayah tints it and
 * floats a small dark toolbar above it. The tint is [WirdColors.highlightSelected], their
 * `selection_highlight`, and it is now on the page.
 *
 * **Only what the app can actually do is here.** Their bar carries bookmark, tag, share,
 * translation and play; Wird has share and play today, and bookmarks and translations do not
 * exist yet. A toolbar with three dead buttons would be worse than a toolbar with two live
 * ones, so the row grows when those features do.
 *
 * ⚠ **Share sends an ayah reference, never Qur'anic text.** Sacred Rule 2 governs what leaves
 * this app as much as what enters it: the app does not hold a verified copy of the words as
 * text — it holds glyph codes for a font — so anything it "shared" as text would be
 * reconstructed rather than quoted. It shares the reference and a link instead.
 */
@Composable
fun VerseActions(
    verseKey: String,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWirdColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Scale.radius))
            .background(colors.surfaceRaised)
            .padding(horizontal = Scale.space2, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Scale.space1),
    ) {
        ActionIcon(Icons.Filled.PlayArrow, "Play this ayah", onPlay)
        ActionIcon(Icons.Filled.Share, "Share this ayah", onShare)
        ActionIcon(Icons.Filled.Close, "Close", onDismiss)
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = LocalWirdColors.current
    Icon(
        imageVector = icon,
        contentDescription = label,
        tint = colors.onSurfaceRaised,
        modifier = Modifier
            .clip(RoundedCornerShape(Scale.radius))
            .clickable(onClick = onClick)
            .defaultMinSize(minWidth = Scale.minTarget, minHeight = Scale.minTarget)
            .padding(Scale.space3)
            .size(20.dp),
    )
}

/**
 * What gets shared: a reference, not the Qur'an.
 *
 * "Al-Kahf 18:10 — read it at quran.com/18/10". The link is to a published source rather than
 * to text this app assembled, which is the only honest thing it can offer: Wird stores glyph
 * codes for a per-page font, so it has no verified plain-text copy of the words to quote.
 */
fun shareTextFor(verseKey: String, surahName: String): String {
    val ayah = verseKey.substringAfter(':')
    val path = verseKey.replace(':', '/')
    return "$surahName $verseKey (ayah $ayah) — https://quran.com/$path"
}
