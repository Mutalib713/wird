package com.mosman.wird

import com.mosman.wird.domain.buildListenQueue
import org.junit.Assert.assertEquals
import org.junit.Test

class ListenQueueTest {

    @Test
    fun secondHalfPortionWithNullStartAtPlaysTodaysVersesFromIndexZero() {
        // B13: a second-half portion with startAt = null gives today's verses from index 0
        val todaysVerses = listOf("2:142", "2:143", "2:144", "2:145")
        val (queue, beginIndex) = buildListenQueue(todaysVerses, startAt = null)

        assertEquals(todaysVerses, queue)
        assertEquals(0, beginIndex)
    }

    @Test
    fun tappingAyahInsidePortionPlaysFromThatAyah() {
        val todaysVerses = listOf("2:142", "2:143", "2:144", "2:145")
        val (queue, beginIndex) = buildListenQueue(todaysVerses, startAt = "2:144")

        assertEquals(todaysVerses, queue)
        assertEquals(2, beginIndex)
    }

    @Test
    fun tappingAyahOutsidePortionPlaysOnlyThatTappedAyah() {
        // Tapping an ayah outside today's portion (e.g. from yesterday on the same page)
        val todaysVerses = listOf("2:142", "2:143", "2:144", "2:145")
        val (queue, beginIndex) = buildListenQueue(todaysVerses, startAt = "2:141")

        assertEquals(listOf("2:141"), queue)
        assertEquals(0, beginIndex)
    }

    @Test
    fun customVersesSelectionPreservesCustomList() {
        val todaysVerses = listOf("2:142", "2:143")
        val custom = listOf("1:1", "1:2", "1:3", "1:4")
        val (queue, beginIndex) = buildListenQueue(todaysVerses, startAt = "1:3", customVerses = custom)

        assertEquals(custom, queue)
        assertEquals(2, beginIndex)
    }
}
