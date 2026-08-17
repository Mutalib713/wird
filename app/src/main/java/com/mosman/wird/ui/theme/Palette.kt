package com.mosman.wird.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Mutalib's palette, chosen 2026-08-17 from the Claude Design direction — gold on cream,
 * with midnight. PROFILE.md Sacred Rule 8, which was reversed that day to allow it; § 6b
 * keeps the old paper-and-ink palette as a record of what it was.
 *
 * **Every ratio below was computed, not eyeballed**, and the computation changed the design:
 * see [WirdColors] for the rule it forced.
 *
 * Deliberately *not* ported from the source: `--crimson`, `--curtain`, `--evergreen`,
 * `--snow`-as-green. Those are inherited from the Nutcracker poster design system the
 * design was built on top of, they appear on five elements in the whole rendered file, and
 * nothing in Wird is an error worth alarming someone about.
 */
object Ink {
    val midnight = Color(0xFF14101F)    // luminance 0.0062
    val midnight2 = Color(0xFF1F1830)   // 0.0116
    val ink = Color(0xFF1A1320)         // 0.0079
    val inkSoft = Color(0xFF463A44)     // 0.0475
    val goldDeep = Color(0xFFA07A2E)    // 0.2159
    val gold = Color(0xFFC9A24B)        // 0.3877
    val goldBright = Color(0xFFE7C97E)  // 0.6027
    val goldPale = Color(0xFFF0DCA8)    // 0.7254
    val parchment = Color(0xFFE8DCC4)   // 0.7233
    val cream = Color(0xFFF4EDE0)       // 0.8518
    val snow = Color(0xFFFBF9F3)        // 0.9473
}

/**
 * Roles, not colours. Components read these and never [Ink] directly.
 *
 * **The rule this palette forced, and it is the important one: gold is never text on the
 * light ground.** Measured 2026-08-17 — gold on cream is **2.06:1** and gold on snow is
 * **2.28:1**. Both fail AA body (4.5) *and* AA large (3.0), so this is not a close call to
 * be argued about; gold simply cannot be read on cream. On the light ground gold is a rule,
 * a border, an ornament, never a word.
 *
 * Gold is superb on midnight — **7.79:1, AAA** — which is what tells you this direction is
 * really a dark-ground design with a cream reading surface, rather than a light design with
 * gold accents.
 *
 * This mirrors the old palette exactly, which is a good sign the discipline transferred:
 * that one had "slate is never body text" at 3.72:1. This one has gold, at 2.06:1.
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
    /** Gold, for rules and ornament only. **Never text on [surface] in light mode.** */
    val ornament: Color,
)

/**
 * Cream and ink. The reading default.
 *
 * [accent] is ink-soft rather than gold on purpose — it is used for text buttons at body
 * size, and gold would be 2.06:1 there. Gold still appears on this surface, as [ornament].
 */
val LightColors = WirdColors(
    surface = Ink.cream,
    textPrimary = Ink.ink,              // 15.58:1 AAA
    textSecondary = Ink.inkSoft,        // 9.25:1  AAA
    textOutsidePortion = Ink.goldDeep,  // 3.39:1  — intended to recede, as slate did before
    accent = Ink.inkSoft,               // 9.25:1  AAA
    done = Ink.parchment,
    surfaceRaised = Ink.parchment,
    onSurfaceRaised = Ink.ink,          // 13.36:1 AAA
    ornament = Ink.gold,                // decoration only — 2.06:1, unreadable as text
)

/**
 * Midnight. Not an inversion — this is where the direction actually lives, and gold finally
 * gets to carry text.
 */
val DarkColors = WirdColors(
    surface = Ink.midnight,
    textPrimary = Ink.snow,             // 17.75:1 AAA
    textSecondary = Ink.goldBright,     // 11.62:1 AAA
    textOutsidePortion = Ink.goldDeep,  // 4.73:1  — recedes against snow's 17.75
    accent = Ink.gold,                  // 7.79:1  AAA
    done = Ink.midnight2,
    surfaceRaised = Ink.midnight2,
    onSurfaceRaised = Ink.snow,         // 17.19:1 AAA
    ornament = Ink.gold,                // 7.79:1 — here it may carry text too
)

val LocalWirdColors = staticCompositionLocalOf { LightColors }
