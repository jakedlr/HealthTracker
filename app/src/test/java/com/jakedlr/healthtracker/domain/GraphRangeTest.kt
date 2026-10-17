package com.jakedlr.healthtracker.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GraphRangeTest {
    private val today = LocalDate.of(2026, 10, 16)

    @Test
    fun week_isSevenDaysCountingToday_whenDataIsOlder() {
        val bounds = graphBounds(GraphRange.WEEK, today, LocalDate.of(2026, 1, 1), today)
        assertEquals(LocalDate.of(2026, 10, 10)..today, bounds)
    }

    @Test
    fun month_clampsStartToOneMonthAgo() {
        val bounds = graphBounds(GraphRange.MONTH, today, LocalDate.of(2026, 1, 1), today)
        assertEquals(LocalDate.of(2026, 9, 16)..today, bounds)
    }

    @Test
    fun threeMonths_clampsStartToThreeMonthsAgo() {
        val bounds = graphBounds(GraphRange.THREE_MONTHS, today, LocalDate.of(2026, 1, 1), today)
        assertEquals(LocalDate.of(2026, 7, 16)..today, bounds)
    }

    @Test
    fun allTime_startsAtFirstMeasurement() {
        val first = LocalDate.of(2024, 3, 5)
        assertEquals(first..today, graphBounds(GraphRange.ALL_TIME, today, first, today))
    }

    @Test
    fun rangeLongerThanData_startsAtFirstMeasurement() {
        val first = LocalDate.of(2026, 10, 1)
        assertEquals(first..today, graphBounds(GraphRange.THREE_MONTHS, today, first, today))
    }

    @Test
    fun endsAtLastMeasurement_whenItIsBeforeToday() {
        val last = LocalDate.of(2026, 10, 12)
        val bounds = graphBounds(GraphRange.MONTH, today, LocalDate.of(2026, 1, 1), last)
        assertEquals(LocalDate.of(2026, 9, 16)..last, bounds)
    }

    @Test
    fun endsAtToday_whenDataExtendsIntoFuture() {
        val bounds = graphBounds(GraphRange.ALL_TIME, today, LocalDate.of(2026, 10, 1), today.plusDays(3))
        assertEquals(LocalDate.of(2026, 10, 1)..today, bounds)
    }

    @Test
    fun singleDayOfData() {
        assertEquals(today..today, graphBounds(GraphRange.WEEK, today, today, today))
    }

    @Test
    fun noData_returnsNull() {
        assertNull(graphBounds(GraphRange.ALL_TIME, today, null, null))
    }

    @Test
    fun allDataOlderThanRange_returnsNull() {
        val bounds = graphBounds(GraphRange.WEEK, today, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1))
        assertNull(bounds)
    }
}
