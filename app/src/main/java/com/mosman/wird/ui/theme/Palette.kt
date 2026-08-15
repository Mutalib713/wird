package com.mosman.wird.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Mutalib's pinned palette. PROFILE.md Sacred Rule 8 — canon, not to be extended,
 * re-derived or improved. Every contrast pair below was computed, not eyeballed;
 * the ratios live in PROFILE.md § 6b.
 */
object Ink {
    val ink = Color(0xFF01161E)      // luminance 0.0067
    val deepTeal = Color(0xFF124559) // 0.0511
    val slate = Color(0xFF598392)    // 0.2043
    val sage = Color(0xFFAEC3B0)     // 0.5116
    val paper = Color(0xFFEFF6E0)    // 0.8964
}

/**
 * Roles, not colours. Components read these and never [Ink] directly.
 *
 * The one rule that is easy to break: **slate is never body text.** It fails AA body on
 * both grounds (3.72:1 on paper, 4.48:1 on ink). Its single job is [textOutsidePortion],
 * where receding is the whole point.
 */
@Immutable
data class WirdColors(
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    /** Ayahs outside today's portion. Deliberately low contrast. */
    val textOutsidePortion: Color,
    /** Spent almost nowhere: the ayah numerals inside today's portion. */
    val accent: Color,
    val done: Color,
    val surfaceRaised: Color,
    val onSurfaceRaised: Color,
)

/** Paper and ink. The reading default. */
val LightColors = WirdColors(
    surface = Ink.paper,
    textPrimary = Ink.ink,          // 16.68:1
    textSecondary = Ink.deepTeal,   // 9.37:1
    textOutsidePortion = Ink.slate, // 3.72:1 — intended
    accent = Ink.deepTeal,          // 9.37:1
    done = Ink.sage,
    surfaceRaised = Ink.sage,
    onSurfaceRaised = Ink.ink,      // 9.90:1
)

/**
 * Not an inversion. Deep teal disappears on ink (1.78:1) so it cannot carry text here —
 * it becomes the raised surface instead, and sage takes over as the accent.
 */
val DarkColors = WirdColors(
    surface = Ink.ink,
    textPrimary = Ink.paper,        // 16.68:1
    textSecondary = Ink.sage,       // 9.90:1
    textOutsidePortion = Ink.slate, // 4.48:1
    accent = Ink.sage,              // 9.90:1
    done = Ink.sage,
    surfaceRaised = Ink.deepTeal,
    onSurfaceRaised = Ink.paper,    // 9.37:1
)

val LocalWirdColors = staticCompositionLocalOf { LightColors }
