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
) {
    /** Where tomorrow starts. */
    val nextStartUnit: Int get() = Math.floorMod(startUnit + units, Mushaf.TOTAL_UNITS)
}

/**
 * Work out today's portion.
 *
 * [units] is in half pages: 1 = half a page, 2 = one page, 4 = two pages.
 *
 * Finishing the mushaf is not an ending — it wraps to page 1 and carries on, which is
 * what people actually do.
 */
fun assignPortion(startUnit: Int, units: Int): Assignment {
    require(units > 0) { "A portion must be at least one half page, got $units" }
    val start = Math.floorMod(startUnit, Mushaf.TOTAL_UNITS)
    val lastUnitRaw = start + units - 1
    return Assignment(
        startUnit = start,
        units = units,
        startPage = Mushaf.pageOf(start),
        endPage = Mushaf.pageOf(lastUnitRaw),
        wrapsPastEnd = lastUnitRaw >= Mushaf.TOTAL_UNITS,
    )
}
