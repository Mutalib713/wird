package com.mosman.wird.domain

import com.mosman.wird.mushaf.MushafPage
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * How much to read, and on which days.
 *
 * Stored in half pages so "half a page on Friday" is an ordinary number rather than a
 * fraction — see [Mushaf]. A weekday with no entry falls back to [defaultUnits], so a
 * plan is "one page a day, except Fridays" rather than seven separate settings.
 */
data class ReadingPlan(
    val defaultUnits: Int = Mushaf.UNITS_PER_PAGE,
    val weekdayUnits: Map<DayOfWeek, Int> = emptyMap(),
) {
    fun unitsOn(day: DayOfWeek): Int = weekdayUnits[day] ?: defaultUnits

    init {
        require(defaultUnits > 0) { "A plan must ask for at least half a page" }
        require(weekdayUnits.values.all { it > 0 }) { "A weekday override must be positive" }
    }
}

/**
 * An amount of reading, said the way a person says it.
 *
 * The app counts in half pages because that is what divides evenly; nobody says "three
 * units". This is the translation back, and it is used wherever the companion has to repeat
 * an instruction — PLAN task 22 — because being told *"a page and a half a day"* is how you
 * notice it heard you wrong.
 */
fun unitsLabel(units: Int): String = when {
    units <= 1 -> "half a page"
    units == 2 -> "one page"
    units == 3 -> "a page and a half"
    units % 2 == 0 -> "${units / 2} pages"
    else -> "${units / 2} and a half pages"
}

/**
 * Today's portion: where you are, plus what today asks of you.
 *
 * The position only moves when a day is marked done. Opening the app twice on the same
 * day must show the same portion both times — a target that crept forward on every
 * launch would be unusable.
 */
fun todaysAssignment(
    startUnit: Int,
    plan: ReadingPlan,
    date: LocalDate,
    direction: ReadingDirection = ReadingDirection.TOWARDS_NAS,
): Assignment = assignPortion(startUnit, plan.unitsOn(date.dayOfWeek), direction)

/** Pages this portion touches, in reading order, wrap included. */
val Assignment.pages: List<Int>
    get() = verses?.map { VerseIndex.pageOf(it) }?.distinct()
        ?: (startUnit until startUnit + units).map { Mushaf.pageOf(it) }.distinct()

/**
 * A portion of exactly [count] verses from [start]. Used when a track's daily target is a
 * number of verses ("10 verses a day") rather than pages.
 */
fun verseAssignment(start: Pair<Int, Int>, count: Int, direction: ReadingDirection): Assignment {
    val verses = VerseIndex.walk(start, count, direction)
    val pages = verses.map { VerseIndex.pageOf(it) }.distinct()
    val startUnit = VerseIndex.unitOf(verses.first())
    return Assignment(
        startUnit = startUnit,
        units = pages.size * Mushaf.UNITS_PER_PAGE,
        startPage = pages.first(),
        endPage = pages.last(),
        wrapsPastEnd = false,
        direction = direction,
        verses = verses,
        nextVerse = VerseIndex.next(verses.last(), direction),
    )
}

/**
 * Surahs this portion touches, in reading order. More than one when it crosses a boundary.
 *
 * A verse portion names only the sūrahs it reads, in the order it reads them: a madrasa day on
 * page 604 is An-Nās, Al-Falaq, Al-Ikhlāṣ. A page portion names every sūrah on its pages.
 */
val Assignment.surahs: List<Surah>
    get() = verses?.map { it.first }?.distinct()?.mapNotNull { SurahIndex.byNumber(it) }
        ?: SurahIndex.across(pages)

/**
 * Which lines of [page] belong to this portion.
 *
 * A page is two half-page units. When only one of them is today's, the portion is half
 * the lines: the top half for the first unit, the bottom half for the second. The mushaf
 * does not record where its own half-way point is, so this splits the lines evenly and
 * rounds the top half up — an odd fifteenth line reads better attached to the first half
 * than orphaned at the bottom.
 *
 * [linesOnPage] is what the page actually rendered, which is not always 1..15: a page
 * that opens a surah gives its first line to the bismillah.
 */
fun Assignment.linesOn(page: Int, linesOnPage: List<Int>): Set<Int> {
    if (linesOnPage.isEmpty()) return emptySet()

    val firstHalf = (page - 1) * Mushaf.UNITS_PER_PAGE
    val secondHalf = firstHalf + 1
    val covered = (startUnit until startUnit + units)
        .map { Math.floorMod(it, Mushaf.TOTAL_UNITS) }
        .toSet()

    val hasFirst = firstHalf in covered
    val hasSecond = secondHalf in covered

    val sorted = linesOnPage.sorted()
    return when {
        hasFirst && hasSecond -> sorted.toSet()
        hasFirst -> sorted.take((sorted.size + 1) / 2).toSet()
        hasSecond -> sorted.drop((sorted.size + 1) / 2).toSet()
        else -> emptySet()
    }
}

/**
 * Which verses of [page] belong to this portion.
 *
 * A page is two half-page units. When only one of them is today's, the portion is half
 * the verses: the top half for the first unit, the bottom half for the second.
 * Splitting by verse rather than by line ensures that verses starting or ending mid-line
 * are not cut in half or dimmed prematurely.
 */
fun Assignment.versesOn(page: MushafPage, startVerse: Pair<Int, Int>? = null): Set<String> =
    keysOn(
        page = page.page,
        pageVerses = page.glyphs.filter { !it.isEndMarker }.map { it.verseKey }.distinct(),
        startVerse = startVerse,
    )

/**
 * Every verse in this portion, in reading order, as "sūrah:ayah" keys.
 *
 * **The one rule for "which verses are today's".** The page highlight, the recitation check
 * and the notification text all use it. Before 2026-10-03 the check compared what you recited
 * with *every* word on the page, so a correct half-page recitation covered about half and the
 * check called itself unreliable every time.
 */
fun Assignment.portionKeys(startVerse: Pair<Int, Int>? = null): List<String> =
    verses?.map { "${it.first}:${it.second}" }
        ?: pages.flatMap { p ->
            val onPage = VerseIndex.versesOn(p).map { "${it.first}:${it.second}" }
            val mine = keysOn(p, onPage, startVerse)
            onPage.filter { it in mine }
        }

/** "Ayahs 1–20" for [surah] within this portion, or null if the portion doesn't touch it. */
fun Assignment.ayahRangeIn(surah: Int, startVerse: Pair<Int, Int>? = null): String? {
    val ayahs = portionKeys(startVerse)
        .filter { it.substringBefore(':').toIntOrNull() == surah }
        .mapNotNull { it.substringAfter(':').toIntOrNull() }
    if (ayahs.isEmpty()) return null
    val lo = ayahs.min()
    val hi = ayahs.max()
    return if (lo == hi) "Ayah $lo" else "Ayahs $lo–$hi"
}

private fun Assignment.keysOn(
    page: Int,
    pageVerses: List<String>,
    startVerse: Pair<Int, Int>?,
): Set<String> {
    if (page !in pages) return emptySet()
    if (pageVerses.isEmpty()) return emptySet()

    // A verse portion is exactly its verses, whatever half of the page they are in.
    verses?.let { mine ->
        val keys = mine.map { "${it.first}:${it.second}" }.toSet()
        return pageVerses.filter { it in keys }.toSet()
    }

    val firstHalf = (page - 1) * Mushaf.UNITS_PER_PAGE
    val secondHalf = firstHalf + 1
    val covered = (startUnit until startUnit + units)
        .map { Math.floorMod(it, Mushaf.TOTAL_UNITS) }
        .toSet()

    val hasFirst = firstHalf in covered
    val hasSecond = secondHalf in covered

    if (pageVerses.size <= 1) {
        return if (hasFirst || hasSecond) pageVerses.toSet() else emptySet()
    }

    val split = (pageVerses.size + 1) / 2
    val assigned = when {
        hasFirst && hasSecond -> pageVerses
        hasFirst -> pageVerses.take(split)
        hasSecond -> pageVerses.drop(split)
        else -> emptyList()
    }

    if (startVerse == null || page != pages.first()) {
        return assigned.toSet()
    }

    val (startSurah, startAyah) = startVerse
    val targetKey = "$startSurah:$startAyah"
    if (targetKey in pageVerses) {
        val filtered = assigned.filter { vk ->
            val s = vk.substringBefore(':').toIntOrNull() ?: 0
            val a = vk.substringAfter(':').toIntOrNull() ?: 0
            s > startSurah || (s == startSurah && a >= startAyah)
        }
        if (filtered.isNotEmpty()) {
            return filtered.toSet()
        }
    }

    return assigned.toSet()
}

