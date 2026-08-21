package com.mosman.wird.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Teal and white, after Quran for Android. Pinned by Mutalib on 2026-08-18.
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
 * 2. **⚠ Their `page_background` token is NOT the colour of their mushaf.** It is defined as
 *    `#FFF4CB`, a warm manila, and Wird used it for the reading page on that basis. Mutalib
 *    looked at it against his own screenshots and said it should be white. He is right, and
 *    the reason is that quran_android renders the mushaf as **page images** — bitmaps that
 *    carry their own white ground — so that token never paints the page you actually see.
 *
 *    **The lesson is narrower than "read the source".** Reading the source gave the correct
 *    *values*; it could not tell which value is used *where*. The screenshot was the ground
 *    truth for that and was sitting right there.
 *
 *    White also measures better on every pair: ink 15.43 against 14.00, accent 5.39 against
 *    4.89. And it separates from the chrome slightly *more* than the manila did, so the page
 *    still reads as a lit object rather than as more chrome.
 * 3. **The accent is a different colour in each theme** — deep teal on light, pale teal on
 *    dark. They lighten it rather than reuse it, which is the same problem the old gold hit
 *    and the honest way to solve it.
 */
object Q {
    // ---- light, from quran_android unless noted ----
    val surface = Color(0xFFFAF8F7)     // their `surface`
    val page = Color(0xFFFFFFFF)        // white. See the note on `page_background` above.
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

    /**
     * The ayah the recitation check could not follow. **PLAN task 14.**
     *
     * ⚠ **Amber, and deliberately not red.** Red means error, and this mark is not an
     * accusation - it is the app saying *"I could not match this one, look again"*. Sacred Rule
     * 3 forbids guilt in the wording and the same rule has to govern the colour, which is read
     * before any word is. Kept at the same 25% alpha as the other two so a page with several
     * marks still reads as a page rather than a warning screen.
     */
    val review = Color(0x40D9A441)

    // ---- ⚠ the warm ground was built and then reversed. § 6e ----
    //
    // Offered three ways on 2026-08-19 he chose "teal stays, take the warm cream ground and
    // the tinted tiles", so it was built and measured: ground #F9F6EE, cards #F3F0E7, every
    // pair AA. **He saw it on the phone and said he prefers the white.** So the ground is back
    // to their `surface` and the warm values are gone rather than left commented out.
    //
    // Kept, because it is the useful half and he did not object to it: **the four tinted
    // tiles below.** Their contrast was re-measured against the white card - ink is 12.84 to
    // 13.34 on all four, unchanged, because ink-on-tint never depended on what sits behind
    // the tint.
    //
    // The lesson is the same one § 6d already carries: a palette is looked at on the device,
    // not reasoned about in a table. Two rounds of measurement cost less than one round of
    // shipping the wrong ground.
    val rule = Color(0xFFDEE2E6)  // card hairline. 1.23:1 on the ground - an edge, not text

    // ---- the chat bubbles, after WhatsApp. His instruction, 2026-08-19 ----
    //
    // Yours tinted and theirs white, which is the arrangement every phone in Ghana already
    // has muscle memory for. The tinted one is the reason these are tokens rather than an
    // alpha on the accent: **the ordinary secondary grey FAILS on it** - #656E76 measures
    // 4.01:1 there - so the timestamp inside your own bubble needs its own darker tone. A
    // detail small enough to have shipped unnoticed, on the one element that repeats forever.
    val mine = Color(0xFFCFE7E7)        // ink 11.93:1
    val mineDetail = Color(0xFF5B646C)  // 4.66:1 - DARKENED from #656E76, which is 4.01 here
    val theirs = Color(0xFFFFFFFF)      // ink 15.43:1, secondary 5.19:1

    val nightRule = Color(0xFF3A3A3A)
    val nightMine = Color(0xFF2A3A3C)
    val nightTheirs = Color(0xFF303030)

    // ---- the four action tiles ----
    //
    // ⚠ **The tint alone is invisible and cannot be the whole idea.** Measured: every one of
    // these fills is 1.01-1.05:1 against the card behind it, which is nothing. That is § 5o's
    // lesson in a new costume - a fill is not an edge. So each tile carries a tint, a hairline
    // of its own hue, AND a coloured icon, and the icon is what actually says which is which.
    //
    // Every icon tone is measured on its own tint. The amber was darkened from #8A6A2B, which
    // came back at 4.4 - the same correction § 6d had to make twice on their palette.
    val teaTint = Color(0xFFE4EFEF); val teaEdge = Color(0xFFC6DCDC); val teaInk = Color(0xFF00767F) // 4.59
    val skyTint = Color(0xFFE7EAF4); val skyEdge = Color(0xFFC8D1E8); val skyInk = Color(0xFF33518F) // 6.44
    val sunTint = Color(0xFFF6EEDA); val sunEdge = Color(0xFFE4D4AC); val sunInk = Color(0xFF866727) // 4.56
    val clayTint = Color(0xFFF3E9DF); val clayEdge = Color(0xFFDFC9B4); val clayInk = Color(0xFF9C4B2E) // 5.07

    // Dark is designed, not inverted: the hue survives, the lightness flips, and the icon
    // becomes the light member of the pair. White clears AAA on all four grounds.
    val nightTea = Color(0xFF2A3A3C); val nightTeaInk = Color(0xFFB2DFDB)  // 8.18
    val nightSky = Color(0xFF2C3140); val nightSkyInk = Color(0xFFA9C0F0)  // 7.09
    val nightSun = Color(0xFF3A3327); val nightSunInk = Color(0xFFE3C88A)  // 7.66
    val nightClay = Color(0xFF3A2F2A); val nightClayInk = Color(0xFFE0A98C) // 6.31
}

/**
 * One action tile's three colours. **§ 6e.**
 *
 * Grouped rather than left as twelve loose tokens, because the three only ever mean anything
 * together: a fill this pale is decoration, and [ink] is the part carrying the contrast that
 * was measured.
 */
@Immutable
data class TileColors(val fill: Color, val edge: Color, val ink: Color)

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
    /**
     * The hairline that closes a card. **§ 6e.**
     *
     * Separate from [ornament], which is the accent and therefore shouts. § 5o found that a
     * screen reads as divided because of edges, and an edge drawn in the accent colour turns
     * every card into a highlighted one.
     */
    val cardEdge: Color,
    /** Your own chat bubble, its timestamp, and the companion's. **§ 6e.** */
    val bubbleMine: Color,
    val bubbleMineDetail: Color,
    val bubbleTheirs: Color,
    /** The four action tiles, in the order they appear. **§ 6e.** */
    val recite: TileColors,
    val read: TileColors,
    val listen: TileColors,
    val openPage: TileColors,
    /** The ayah being recited right now. */
    val highlightReciting: Color,
    /** The ayah you selected. */
    val highlightSelected: Color,

    /** The ayah to look at again after a recitation check. **§ 6e, PLAN task 14.** */
    val highlightReview: Color,
)

/**
 * Near-white chrome, white page, deep teal.
 *
 * Every ratio measured against both grounds, because text moves between them:
 * primary 14.57 / 15.43, secondary 4.90 / 5.19, accent 5.09 / 5.39 — chrome then page.
 */
val LightColors = WirdColors(
    surface = Q.surface,                 // § 6e - the warm ground was reversed, at his word
    page = Q.page,                       // white. The mushaf is a lit object, not chrome.
    textPrimary = Q.ink,                 // 14.57:1 AAA
    textSecondary = Q.detail,            // 4.90:1  AA
    textOutsidePortion = Q.recede,       // 3.45:1  on the page, and that is the point
    accent = Q.teal,                     // 5.09:1  AA
    done = Q.quiet,
    surfaceRaised = Q.raised,            // secondary 4.67, accent 4.84 - both AA
    onSurfaceRaised = Q.ink,             // 13.87:1 AAA
    band = Q.band,                       // ⚠ textPrimary only: secondary is 3.99 here
    ornament = Q.teal,
    cardEdge = Q.rule,
    bubbleMine = Q.mine,
    bubbleMineDetail = Q.mineDetail,
    bubbleTheirs = Q.theirs,
    recite = TileColors(Q.teaTint, Q.teaEdge, Q.teaInk),
    read = TileColors(Q.skyTint, Q.skyEdge, Q.skyInk),
    listen = TileColors(Q.sunTint, Q.sunEdge, Q.sunInk),
    openPage = TileColors(Q.clayTint, Q.clayEdge, Q.clayInk),
    highlightReciting = Q.reciting,
    highlightSelected = Q.selected,
    highlightReview = Q.review,
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
    cardEdge = Q.nightRule,
    bubbleMine = Q.nightMine,
    bubbleMineDetail = Q.nightDetail,
    bubbleTheirs = Q.nightTheirs,
    recite = TileColors(Q.nightTea, Q.nightTea, Q.nightTeaInk),
    read = TileColors(Q.nightSky, Q.nightSky, Q.nightSkyInk),
    listen = TileColors(Q.nightSun, Q.nightSun, Q.nightSunInk),
    openPage = TileColors(Q.nightClay, Q.nightClay, Q.nightClayInk),
    highlightReciting = Q.reciting,
    highlightSelected = Q.selected,
    highlightReview = Q.review,
)

val LocalWirdColors = staticCompositionLocalOf { LightColors }
