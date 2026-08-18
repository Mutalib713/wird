package com.mosman.wird.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Teal and manila, after Quran for Android. Pinned by Mutalib on 2026-08-18.
 *
 * **Sacred Rule 8 reversed a second time, on his explicit word**, having been told plainly
 * that this was the second reversal in two days and that it costs Wird its own look. He
 * chose it anyway, so it is his call and it is canon now. The gold direction of § 6c is kept
 * as a record of what it was, not as something to drift back to.
 *
 * **Source, not screenshots.** Read from
 * [`values/colors.xml`](https://github.com/quran/quran_android/blob/main/app/src/main/res/values/colors.xml)
 * and `values-night/colors.xml` in quran/quran_android. Colour *values* are data rather than
 * code, so nothing here is a derivative of their GPL source — but the debt is real and named.
 *
 * ### The measurement that changed two of their colours
 *
 * **Their light palette does not meet AA for body text on two roles**, computed rather than
 * assumed:
 * - their detail grey `#6C757D` on their surface `#FAF8F7` is **4.43:1** — under the 4.5 floor
 * - their teal `#00838F` on the same surface is **4.27:1** — also under it
 *
 * Both are fine in their app because those roles carry short labels at large-ish sizes. Wird
 * puts real sentences in them. So exactly two values are darkened, and nothing else is
 * touched: `#6C757D` → [Q.detail] at 4.90:1, and `#00838F` → [Q.teal] at 5.09:1. Their dark
 * palette needed no adjustment at all — white is 16.10:1 and their pale teal is 11.10:1.
 *
 * ### Three things worth knowing about the palette we adopted
 *
 * 1. **The greys are Bootstrap's, not Material's** — `#212529`, `#6C757D`, `#DEE2E6` are
 *    `gray-900 / gray-600 / gray-300` exactly. That is why their app reads as a document.
 * 2. **The page is warm manila `#FFF4CB`, not white**, sitting inside near-white chrome.
 *    Wird already claimed to do this and did not: the mushaf was painted with `surface` like
 *    everything else. [WirdColors.page] makes it true.
 * 3. **The accent is a different colour in each theme** — deep teal on light, pale teal on
 *    dark. They lighten it rather than reuse it, which is the same problem the old gold hit
 *    and the honest way to solve it.
 */
object Q {
    // ---- light, from quran_android unless noted ----
    val surface = Color(0xFFFAF8F7)     // their `surface`
    val page = Color(0xFFFFF4CB)        // their `page_background` - the mushaf ground
    val ink = Color(0xFF212529)         // their `title_color` / Bootstrap gray-900
    val detail = Color(0xFF656E76)      // DARKENED from their #6C757D (4.43 -> 4.90)
    val teal = Color(0xFF00767F)        // DARKENED from their #00838F (4.27 -> 5.09)
    val recede = Color(0xFF848B93)      // Wird's own - the "not your portion" grey
    val raised = Color(0xFFF1F3F5)      // Bootstrap gray-100; their #DEE2E6 fails under text
    val band = Color(0xFFDEE2E6)        // their `header_background` - Juz' section rows
    val quiet = Color(0xFFE9ECEF)       // gray-200, for a filled done state

    // ---- dark, from quran_android values-night ----
    val nightSurface = Color(0xFF212121)   // their `surface`
    val nightPage = Color(0xFF1A1A1A)      // Wird's own - the page, just off the chrome
    val snow = Color(0xFFFFFFFF)           // their `title_color`
    val nightDetail = Color(0xFFB5B5B5)    // their sura_details white@70, flattened
    val nightRecede = Color(0xFF6E6E6E)    // Wird's own
    val paleTeal = Color(0xFFB2DFDB)       // their `accent_color`
    val nightRaised = Color(0xFF303030)    // their `secondary_dark_background`
    val nightBand = Color(0xFF424242)      // their `header_background`
    val nightQuiet = Color(0xFF2A2A2A)

    // ---- the highlights, theirs verbatim, all 25% alpha over a solid ----
    /** Their `audio_highlight` - the ayah currently being recited. */
    val reciting = Color(0x4046A646)

    /** Their `selection_highlight` - the ayah you long-pressed. */
    val selected = Color(0x404694A6)
}

/**
 * Roles, not colours. Components read these and never [Q] directly.
 *
 * ⚠ **One rule this palette carries, and it is measured:** on the light [band] `#DEE2E6`,
 * [textSecondary] is only **3.99:1** and fails. Section labels sitting on a band must use
 * [textPrimary]. On the dark band it is 4.90:1 and either works.
 */
@Immutable
data class WirdColors(
    val surface: Color,
    /** The mushaf's own ground. Distinct from [surface] - the page is a lit object. */
    val page: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    /** Ayahs outside today's portion. Deliberately low contrast; it is meant to recede. */
    val textOutsidePortion: Color,
    val accent: Color,
    val done: Color,
    val surfaceRaised: Color,
    val onSurfaceRaised: Color,
    /** Section headers - the Juz' rows in a surah list. ⚠ [textPrimary] only, on light. */
    val band: Color,
    /** Rules and borders. Same value as [accent] in both themes now. */
    val ornament: Color,
    /** The ayah being recited right now. */
    val highlightReciting: Color,
    /** The ayah you selected. */
    val highlightSelected: Color,
)

/**
 * Near-white chrome, manila page, deep teal.
 *
 * Every ratio measured against both grounds, because text moves between them:
 * primary 14.57 / 14.00, secondary 4.90 / 4.71, accent 5.09 / 4.89 — chrome then page.
 */
val LightColors = WirdColors(
    surface = Q.surface,
    page = Q.page,
    textPrimary = Q.ink,                 // 14.57:1 AAA
    textSecondary = Q.detail,            // 4.90:1  AA
    textOutsidePortion = Q.recede,       // 3.13:1  on the page, and that is the point
    accent = Q.teal,                     // 5.09:1  AA
    done = Q.quiet,
    surfaceRaised = Q.raised,            // secondary 4.67, accent 4.84 - both AA
    onSurfaceRaised = Q.ink,             // 13.87:1 AAA
    band = Q.band,                       // ⚠ textPrimary only: secondary is 3.99 here
    ornament = Q.teal,
    highlightReciting = Q.reciting,
    highlightSelected = Q.selected,
)

/**
 * Their night palette, adopted whole — it needed no correction.
 *
 * primary 16.10 / 17.40, secondary 7.85 / 8.49, accent 11.10 / 11.99 — chrome then page.
 */
val DarkColors = WirdColors(
    surface = Q.nightSurface,
    page = Q.nightPage,
    textPrimary = Q.snow,                // 16.10:1 AAA
    textSecondary = Q.nightDetail,       // 7.85:1  AAA
    textOutsidePortion = Q.nightRecede,  // 3.41:1  on the page, deliberately
    accent = Q.paleTeal,                 // 11.10:1 AAA
    done = Q.nightQuiet,
    surfaceRaised = Q.nightRaised,       // secondary 6.44, accent 9.09
    onSurfaceRaised = Q.snow,            // 13.20:1 AAA
    band = Q.nightBand,                  // secondary 4.90 - AA, so either works here
    ornament = Q.paleTeal,
    highlightReciting = Q.reciting,
    highlightSelected = Q.selected,
)

val LocalWirdColors = staticCompositionLocalOf { LightColors }
