package com.mosman.wird.mushaf

/** One word (or ayah-end marker) as the mushaf lays it out. */
data class Glyph(
    /** Codepoints from the QCF font. NOT readable Arabic — meaningless without the font. */
    val code: String,
    val line: Int,
    val verseKey: String,
    /** True for the ayah-number marker that closes a verse. */
    val isEndMarker: Boolean,
)

data class MushafPage(
    val page: Int,
    val glyphs: List<Glyph>,
    /** Surah names starting on this page, in order, keyed by the verse they start at. */
    val surahStarts: Map<String, String>,
) {
    val lines: List<Int> get() = glyphs.map { it.line }.distinct().sorted()
    fun glyphsOn(line: Int): List<Glyph> = glyphs.filter { it.line == line }
}

/**
 * Which QCF rendering to use.
 *
 * **v1 is the default and it is a data decision, not a taste one.** Measured across all
 * 604 pages: v1 TTF averages 154 KB/page (~4.5 MB/month for a page-a-day reader), v2
 * averages 336 KB/page (~9.9 MB/month). There is no v2 woff2 build in the font repo, so
 * the small-and-native option only exists at v1.
 *
 * Flip [FONT_VERSION] to [V2] and both the font URL and the API's glyph field follow.
 */
enum class MushafVersion(val apiField: String, val fontUrl: (Int) -> String) {
    V1(
        apiField = "code_v1",
        fontUrl = { p -> "https://raw.githubusercontent.com/nuqayah/qpc-fonts/master/mushaf/QCF_P%03d.TTF".format(p) },
    ),
    V2(
        apiField = "code_v2",
        fontUrl = { p -> "https://raw.githubusercontent.com/nuqayah/qpc-fonts/master/mushaf-v2/QCF2%03d.ttf".format(p) },
    ),
}

object Mushaf {
    val FONT_VERSION = MushafVersion.V1

    /**
     * A wrong font path on raw.githubusercontent.com returns HTTP 404 with a 14-byte
     * body reading "404: Not Found". Anything under this is not a font, whatever the
     * status code said.
     */
    const val MIN_PLAUSIBLE_FONT_BYTES = 20_000
}
