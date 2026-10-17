package com.jakedlr.healthtracker.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyValueTest {
    private fun day(d: Int) = LocalDate.of(2026, 10, d)

    @Test
    fun missingDays_areAbsent_notZero() {
        val values = listOf(DailyValue(day(1), 180.0), DailyValue(day(4), 179.0))
        val points = pointsInRange(values, day(1)..day(5))
        assertEquals(listOf(day(1), day(4)), points.map { it.date })
        assertTrue(points.none { it.value == 0.0 })
    }

    @Test
    fun valuesOutsideBounds_areDropped_inclusiveAtBothEnds() {
        val values = (1..10).map { DailyValue(day(it), it.toDouble()) }
        val points = pointsInRange(values, day(3)..day(6))
        assertEquals((3..6).map(::day), points.map { it.date })
    }

    @Test
    fun resultIsSortedByDate() {
        val values = listOf(DailyValue(day(5), 1.0), DailyValue(day(2), 2.0), DailyValue(day(3), 3.0))
        assertEquals(listOf(day(2), day(3), day(5)), pointsInRange(values, day(1)..day(9)).map { it.date })
    }
}
