package com.mosman.wird.domain

/**
 * Page-sized days in madrasa order: from An-Nās back towards Al-Fātiḥah.
 *
 * **Why this exists (2026-10-04).** Madrasa-order page targets used to walk the mushaf page by
 * page and guess which sūrah the reader was in from the sūrahs printed on the page. On 41 pages one
 * sūrah ends where the next begins (Fāṭir and Yā-Sīn share page 440, An-Naba' and An-Nāziʿāt share
 * page 583), and there the guess walked back into the sūrah already read. Replayed over the whole
 * mushaf, a reader starting at An-Nās looped between pages 599 and 600 from day 7, and the quick
 * start (An-Naba', one page a day) looped over pages 582 to 584 from day 4. A start at An-Nās also
 * lit only An-Nās on page 604 and then moved to page 603, so Al-Falaq and Al-Ikhlāṣ never came up.
 *
 * **A madrasa day is now a run of verses in madrasa order**: each sūrah from its first ayah, then
 * the sūrah before it. That makes it an ordinary verse portion, so the highlight, the recitation
 * check, the reminder, the day log and undo all carry it unchanged. This object answers one thing:
 * how many verses today's page-sized run holds.
 *
 * 1. Inside the sūrah being read, today covers the half-pages a page target always covers, with
 *    whole-page targets aligned to the top of the page.
 * 2. If the sūrah runs on past those half-pages, the day stops there, unless what is left of the
 *    sūrah weighs less than half a day. Then the sūrah is finished today, so no day is a stray
 *    verse or two.
 * 3. Once the sūrah is finished, the sūrahs before it come whole while each still fits in what is
 *    left of today's amount. The first that doesn't fit starts tomorrow.
 *
 * A verse weighs one page divided by the verses on its page, so a whole page weighs one page.
 *
 * In Juz' ʿAmma that gives one short sūrah a day at half a page (An-Nās, then Al-Falaq, then
 * Al-Ikhlāṣ) and page 604, then 603, then 602 at one page. Checked over the whole mushaf at half a
 * page, one page and two pages a day, from An-Nās, An-Naba', Yā-Sīn, Al-Baqarah 255 and
 * Al-Fātiḥah: every verse comes up exactly once before the cycle starts again. See MadrasaOrderTest.
 */
object MadrasaOrder {

    /** Weights are sums of fractions such as 1/15. This absorbs their rounding, nothing more. */
    private const val SLACK = 1e-9

    private val versesPerPage: IntArray by lazy {
        IntArray(Mushaf.PAGES + 1) { page -> if (page == 0) 0 else VerseIndex.versesOn(page).size }
    }

    private val surahWeight: DoubleArray by lazy {
        DoubleArray(115) { s -> if (s == 0) 0.0 else (1..VerseIndex.versesIn(s)).sumOf { weight(s to it) } }
    }

    private fun weight(verse: Pair<Int, Int>): Double = 1.0 / versesPerPage[VerseIndex.pageOf(verse)]

    private fun before(surah: Int): Int = if (surah > 1) surah - 1 else 114

    /**
     * How many verses today's run holds, starting at [start], for a target of [units] half-pages.
     * Walk that many verses from [start] towards Al-Fātiḥah to get the day.
     */
    fun versesFor(start: Pair<Int, Int>, units: Int): Int {
        require(units > 0) { "A page target must be at least half a page, got $units" }
        val target = units / 2.0
        val (surah, firstAyah) = start
        val lastAyah = VerseIndex.versesIn(surah)
        require(firstAyah in 1..lastAyah) { "No ayah $firstAyah in sūrah $surah" }

        // 1. Today's half-pages, the ones any page target would cover.
        var top = VerseIndex.unitOf(start)
        if (units % Mushaf.UNITS_PER_PAGE == 0) top -= Math.floorMod(top, Mushaf.UNITS_PER_PAGE)
        val today = (0 until units).map { Math.floorMod(top + it, Mushaf.TOTAL_UNITS) }.toSet()

        var through = firstAyah
        while (through < lastAyah && VerseIndex.unitOf(surah to through + 1) in today) through++

        // 2. A sūrah that runs on stops here, unless its tail is too small to be a day of its own.
        if (through < lastAyah) {
            val tail = (through + 1..lastAyah).sumOf { weight(surah to it) }
            if (tail >= target / 2 - SLACK) return through - firstAyah + 1
            through = lastAyah
        }

        // 3. The sūrah is finished, so the ones before it come whole while they fit.
        var count = through - firstAyah + 1
        var used = (firstAyah..through).sumOf { weight(surah to it) }
        var next = before(surah)
        while (next != surah && used + surahWeight[next] <= target + SLACK) {
            count += VerseIndex.versesIn(next)
            used += surahWeight[next]
            next = before(next)
        }
        return count
    }
}
