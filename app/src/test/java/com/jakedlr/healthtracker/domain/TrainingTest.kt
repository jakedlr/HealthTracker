package com.jakedlr.healthtracker.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingTest {
    @Test
    fun loadAcceptsZeroDecimalsAndCommas() {
        assertEquals(0.0, parseLoad("0")!!, 0.0)
        assertEquals(22.5, parseLoad("22.5")!!, 0.0)
        assertEquals(22.5, parseLoad(" 22,5 ")!!, 0.0)
        assertEquals(2000.0, parseLoad("2000")!!, 0.0)
    }

    @Test
    fun loadRejectsBlankGarbageAndOutOfRange() {
        assertNull(parseLoad(""))
        assertNull(parseLoad("abc"))
        assertNull(parseLoad("-1"))
        assertNull(parseLoad("2000.01"))
    }

    @Test
    fun repsAcceptWholeNumbersFromOneTo999() {
        assertEquals(1, parseReps("1"))
        assertEquals(12, parseReps(" 12 "))
        assertEquals(999, parseReps("999"))
    }

    @Test
    fun repsRejectBlankFractionsAndOutOfRange() {
        assertNull(parseReps(""))
        assertNull(parseReps("0"))
        assertNull(parseReps("1000"))
        assertNull(parseReps("8.5"))
        assertNull(parseReps("-3"))
    }

    @Test
    fun singleSidedLogMaySkipOneSide() {
        val left = TopSet(20.0, 10)
        assertEquals(TopSets.PerSide(left, null), perSide(left, null))
        assertEquals(TopSets.PerSide(null, left), perSide(null, left))
    }

    @Test
    fun singleSidedLogCannotSkipBothSides() {
        assertNull(perSide(null, null))
    }

    @Test
    fun movingDownShiftsTheItemsBetweenUp() {
        assertEquals(listOf("b", "c", "a", "d"), listOf("a", "b", "c", "d").moved(from = 0, to = 2))
    }

    @Test
    fun movingUpShiftsTheItemsBetweenDown() {
        assertEquals(listOf("d", "a", "b", "c"), listOf("a", "b", "c", "d").moved(from = 3, to = 0))
    }

    @Test
    fun movingInPlaceChangesNothing() {
        assertEquals(listOf("a", "b", "c"), listOf("a", "b", "c").moved(from = 1, to = 1))
    }

    @Test
    fun logDatesCanBeTodayOrEarlier_neverInTheFuture() {
        val today = LocalDate.of(2026, 10, 19)
        assertTrue(isLoggableDate(today, today))
        assertTrue(isLoggableDate(today.minusYears(2), today))
        assertFalse(isLoggableDate(today.plusDays(1), today))
    }
}
