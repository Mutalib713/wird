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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
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
 * translation and play. Wird has **bookmark, play and share**; the tag is deliberately absent
 * (see [com.mosman.wird.data.BookmarkStore] — no folders, no tags) and translation joins when
 * § 5z is built. A toolbar with dead buttons would be worse than a short one.
 *
 * ⚠ **Share sends an ayah reference, never Qur'anic text.** Sacred Rule 2 governs what leaves
 * this app as much as what enters it: the app does not hold a verified copy of the words as
 * text — it holds glyph codes for a font — so anything it "shared" as text would be
 * reconstructed rather than quoted. It shares the reference and a link instead.
 */
@Composable
fun VerseActions(
    verseKey: String,
    /** Whether this ayah is already saved. Drives which bookmark icon shows. */
    bookmarked: Boolean,
    onBookmark: () -> Unit,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    /** Null when Quran for Android is not installed, and then no button appears. */
    onOpenElsewhere: (() -> Unit)?,
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
        // Filled when saved, hollow when not. The icon carries the state, so one control does
        // both jobs and nothing has to be read before it can be used.
        //
        // ⚠ **A star rather than the reference's ribbon**, and not by preference: there is no
        // bookmark glyph in `material-icons-core` — 147 icons, checked — and the build file
        // rules out `material-icons-extended` because it is several megabytes for a handful of
        // shapes. A filled-versus-hollow star is the same idea in an icon that is already here.
        ActionIcon(
            icon = if (bookmarked) Icons.Filled.Star else Icons.Outlined.Star,
            label = if (bookmarked) "Remove bookmark" else "Bookmark this ayah",
            onClick = onBookmark,
            tint = if (bookmarked) colors.accent else colors.onSurfaceRaised,
        )
        ActionIcon(Icons.Filled.PlayArrow, "Play this ayah", onPlay)
        ActionIcon(Icons.Filled.Share, "Share this ayah", onShare)
        // **PLAN task 12, and it only exists when the other app does.** A button that opened
        // the Play Store instead would be an advert wearing a feature's clothes.
        onOpenElsewhere?.let {
            ActionIcon(Icons.AutoMirrored.Filled.ExitToApp, "Open in Quran for Android", it)
        }
        ActionIcon(Icons.Filled.Close, "Close", onDismiss)
    }
}

@Composable
private fun ActionIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color? = null,
) {
    val colors = LocalWirdColors.current
    Icon(
        imageVector = icon,
        contentDescription = label,
        tint = tint ?: colors.onSurfaceRaised,
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
