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
    /** Name of the surah this page opens in, for the margin. */
    val surahName: String,
    val juz: Int,
    /**
     * Glyph codes for the bismillah, or null when this page does not open a surah (or
     * opens At-Tawbah, which has none). Must be drawn with the **bismillah** font, never
     * the page font — see [Mushaf.BISMILLAH_FONT_URL].
     */
    val bismillahCodes: String?,
) {
    val lines: List<Int> get() = glyphs.map { it.line }.distinct().sorted()
    fun glyphsOn(line: Int): List<Glyph> = glyphs.filter { it.line == line }

    /**
     * The line a given ayah begins on, or null if it is not on this page.
     *
     * Needed because "start at Ya-Sin 1" does not mean "start at the top of page 440" —
     * that page opens with the last verse of Fatir.
     */
    fun lineOf(surah: Int, ayah: Int): Int? =
        glyphs.firstOrNull { it.verseKey == "$surah:$ayah" }?.line

    /**
     * Which surah the text on [line] belongs to.
     *
     * The page as a whole does not have one answer: page 440 carries the end of Fatir and
     * the start of Ya-Sin. Naming a page by its *first* verse is what made the app
     * announce Fatir to someone who had just chosen Ya-Sin.
     */
    fun surahNumberOn(line: Int): Int? =
        glyphsOn(line).firstOrNull()?.verseKey?.substringBefore(':')?.toIntOrNull()

    /**
     * The first and last ayah covered by [lines], as `surah:ayah` pairs.
     *
     * Exists so the app can say what to read *in words*. Mutalib looked at a marked page
     * and asked whether the light blue was his portion — it was the opposite. Colour
     * alone could not tell him, and nothing on screen said it in language.
     */
    /** How many distinct ayahs [lines] covers. Counted, not subtracted, so it survives a
     *  portion that crosses from one surah into the next. */
    fun ayahCount(lines: Set<Int>): Int =
        glyphs.filter { it.line in lines && !it.isEndMarker }
            .map { it.verseKey }
            .distinct()
            .size

    @JvmName("ayahCountVerses")
    fun ayahCount(verses: Set<String>): Int =
        glyphs.filter { it.verseKey in verses && !it.isEndMarker }
            .map { it.verseKey }
            .distinct()
            .size

    fun ayahRange(lines: Set<Int>): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
        val covered = glyphs.filter { it.line in lines && !it.isEndMarker }
        if (covered.isEmpty()) return null
        fun parse(key: String): Pair<Int, Int>? {
            val s = key.substringBefore(':').toIntOrNull() ?: return null
            val a = key.substringAfter(':').toIntOrNull() ?: return null
            return s to a
        }
        val keys = covered.mapNotNull { parse(it.verseKey) }
        if (keys.isEmpty()) return null
        val first = keys.minWith(compareBy({ it.first }, { it.second }))
        val last = keys.maxWith(compareBy({ it.first }, { it.second }))
        return first to last
    }

    @JvmName("ayahRangeVerses")
    fun ayahRange(verses: Set<String>): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
        val covered = glyphs.filter { it.verseKey in verses && !it.isEndMarker }
        if (covered.isEmpty()) return null
        fun parse(key: String): Pair<Int, Int>? {
            val s = key.substringBefore(':').toIntOrNull() ?: return null
            val a = key.substringAfter(':').toIntOrNull() ?: return null
            return s to a
        }
        val keys = covered.mapNotNull { parse(it.verseKey) }
        if (keys.isEmpty()) return null
        val first = keys.minWith(compareBy({ it.first }, { it.second }))
        val last = keys.maxWith(compareBy({ it.first }, { it.second }))
        return first to last
    }
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
     * The bismillah has its own font, and it must be used.
     *
     * QCF page fonts reuse the **same codepoints** on every page with different glyphs:
     * U+FB51 is "بِسْمِ" in this font and "ص" in page 453's. Drawing the bismillah codes
     * with a page font therefore produces real, well-formed, completely wrong Qur'anic
     * text — the exact failure Sacred Rule 2 exists to prevent. Neither font can render
     * the bismillah as plain Arabic either: both are missing U+0670, the superscript
     * alef in ٱلرَّحْمَٰنِ. Glyph codes with the right font is the only correct route.
     */
    const val BISMILLAH_FONT_URL =
        "https://raw.githubusercontent.com/nuqayah/qpc-fonts/master/mushaf/QCF_BSML.TTF"

    /**
     * The bismillah is **three glyphs in its own font**, not the word codes of verse 1:1.
     *
     * Measured from QCF_BSML.TTF's own tables: U+FB51→'A' adv 7203, U+FB52→'B' adv 7877,
     * U+FB53→'C' adv 2777, then U+FB54/55/56 repeat those three advances exactly. The
     * font stores the phrase several times over in triplets, so the first three are one
     * complete bismillah.
     *
     * Feeding it verse 1:1's five word codes — which are *page one's* codes, meant for
     * QCF_P001 — drew the phrase one and two-thirds times. Same lesson as the page fonts:
     * a codepoint means whatever the font in your hand says it means.
     */
    const val BISMILLAH_CODES = "ﭑﭒﭓ"

    /**
     * A wrong font path on raw.githubusercontent.com returns HTTP 404 with a 14-byte
     * body reading "404: Not Found". Anything under this is not a font, whatever the
     * status code said.
     */
    const val MIN_PLAUSIBLE_FONT_BYTES = 20_000
}
