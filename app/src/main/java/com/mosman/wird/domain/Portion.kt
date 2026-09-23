package com.mosman.wird.domain

/**
 * The mushaf, counted in half pages.
 *
 * Targets in Wird are expressed in pages and half pages ("one page today, half a page
 * on Friday"), so the arithmetic is done in half-page units rather than in floating
 * point pages. Integers cannot drift; 0.5 + 0.5 + 0.5 eventually can.
 */
object Mushaf {
    const val PAGES = 604
    const val UNITS_PER_PAGE = 2
    const val TOTAL_UNITS = PAGES * UNITS_PER_PAGE // 1208

    /** 1-based page number containing [unit]. Wraps, so page 604 is followed by page 1. */
    fun pageOf(unit: Int): Int = (Math.floorMod(unit, TOTAL_UNITS) / UNITS_PER_PAGE) + 1
}

/** One day's assignment: where to start, how much, and where that lands. */
data class Assignment(
    val startUnit: Int,
    val units: Int,
    val startPage: Int,
    val endPage: Int,
    /** True when the portion runs off the end of the mushaf and continues at page 1. */
    val wrapsPastEnd: Boolean,
    /** Which way tomorrow's portion lies. See [ReadingDirection]. */
    val direction: ReadingDirection = ReadingDirection.TOWARDS_NAS,
) {
    /** Where tomorrow starts. */
    /**
     * Where tomorrow starts.
     *
     * **The one line the whole direction feature turns on.** Forwards adds a portion, backwards
     * subtracts one, and `floorMod` wraps at whichever end you reach — 604 to 1 going one way,
     * 1 to 604 going the other.
     *
     * ⚠ **A portion is still read in its own natural order**, left page before right, whichever
     * way the reader is travelling. Only the *step between days* reverses. Going backwards two
     * pages a day therefore reads 440-441, then 438-439: each day in order, no page skipped and
     * none repeated, but the days themselves walk backwards. That is the honest consequence of
     * keeping [startUnit] the start rather than the end, and it is what someone reciting a page
     * at a time actually wants.
     */
    val nextStartUnit: Int
        get() = when (direction) {
            ReadingDirection.TOWARDS_NAS ->
                Math.floorMod(startUnit + units, Mushaf.TOTAL_UNITS)
            ReadingDirection.TOWARDS_FATIHAH -> {
                // In recitation, verses and pages within a surah always advance forwards (1 -> end).
                // TOWARDS_FATIHAH decides which surah comes next AFTER the current surah is finished:
                // it transitions to the preceding surah (e.g. from Ya-Sin 36 -> Fatir 35).
                val surahsOnPage = SurahIndex.on(startPage)
                val continuingSurah = surahsOnPage.firstOrNull { it.lastPage > endPage }
                if (continuingSurah != null) {
                    // Still advancing within the current surah
                    Math.floorMod(startUnit + units, Mushaf.TOTAL_UNITS)
                } else {
                    // Current surah(s) completed on this page: jump to the preceding surah
                    val earliestSurah = surahsOnPage.minByOrNull { it.number }
                    val prevSurahNum = if (earliestSurah != null && earliestSurah.number > 1) {
                        earliestSurah.number - 1
                    } else if (earliestSurah != null && earliestSurah.number == 1) {
                        114
                    } else null

                    if (prevSurahNum != null) {
                        val prevSurah = SurahIndex.byNumber(prevSurahNum)!!
                        (prevSurah.firstPage - 1) * Mushaf.UNITS_PER_PAGE
                    } else {
                        Math.floorMod(startUnit - units, Mushaf.TOTAL_UNITS)
                    }
                }
            }
        }
}

/**
 * Work out today's portion.
 *
 * [units] is in half pages: 1 = half a page, 2 = one page, 4 = two pages.
 *
 * Finishing the mushaf is not an ending — it wraps to page 1 and carries on, which is
 * what people actually do.
 */
/**
 * Which way through the mushaf a reader is travelling. **His instruction, 2026-08-19.**
 *
 * *"For me I memorise upwards, but some start from Baqarah downwards, and reading too is the
 * same, so the app must know."*
 *
 * **This is how ḥifẓ is actually done and the app did not know about it.** Memorisers very
 * commonly begin at the back — Juzʾ 30, the short sūrahs — and work *towards* Al-Baqarah, so
 * their position moves **down** the page numbers while a front-to-back reader's moves up. Wird
 * only ever advanced forwards, which made it silently wrong for a whole way of using the Qur'an
 * rather than merely unsupported.
 *
 * ⚠ **Named by destination, not by "up" and "down".** Those two words mean opposite things to
 * different people: he says *upwards* for travelling towards Al-Baqarah, which is *decreasing*
 * page numbers, while "going up" just as naturally means rising sūrah numbers. A name that has
 * to be explained is a name that will eventually be read the wrong way round in a code review.
 * The screens say it in his words; the code says where you end up.
 */
enum class ReadingDirection {
    /** Towards page 1. His "upwards": at Ya-Sin, tomorrow is Fatir. */
    TOWARDS_FATIHAH,

    /** Towards page 604. His "downwards": at Ya-Sin, tomorrow is As-Saffat. */
    TOWARDS_NAS,
}

fun assignPortion(
    startUnit: Int,
    units: Int,
    direction: ReadingDirection = ReadingDirection.TOWARDS_NAS,
): Assignment {
    require(units > 0) { "A portion must be at least one half page, got $units" }
    val start = Math.floorMod(startUnit, Mushaf.TOTAL_UNITS)
    val lastUnitRaw = start + units - 1
    return Assignment(
        startUnit = start,
        units = units,
        direction = direction,
        startPage = Mushaf.pageOf(start),
        endPage = Mushaf.pageOf(lastUnitRaw),
        wrapsPastEnd = lastUnitRaw >= Mushaf.TOTAL_UNITS,
    )
}
