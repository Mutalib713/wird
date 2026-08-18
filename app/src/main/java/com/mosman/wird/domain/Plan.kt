package com.mosman.wird.domain

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
fun todaysAssignment(startUnit: Int, plan: ReadingPlan, date: LocalDate): Assignment =
    assignPortion(startUnit, plan.unitsOn(date.dayOfWeek))

/** Pages this portion touches, in reading order, wrap included. */
val Assignment.pages: List<Int>
    get() = (startUnit until startUnit + units).map { Mushaf.pageOf(it) }.distinct()

/** Surahs this portion touches. More than one when it crosses a boundary. */
val Assignment.surahs: List<Surah>
    get() = SurahIndex.across(pages)

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
