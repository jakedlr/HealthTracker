package com.jakedlr.healthtracker.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarTest {
    // --- calendarGrid ---

    @Test
    fun monthStartingOnFirstDayOfWeek_hasNoLeadingBlanks() {
        // 2026-02-01 is a Sunday.
        val grid = calendarGrid(YearMonth.of(2026, 2), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 2, 1), grid.first())
        assertEquals(28, grid.size) // exactly four weeks
    }

    @Test
    fun sameMonth_mondayStart_shiftsBySixBlanks() {
        val grid = calendarGrid(YearMonth.of(2026, 2), DayOfWeek.MONDAY)
        assertEquals(List(6) { null }, grid.take(6))
        assertEquals(LocalDate.of(2026, 2, 1), grid[6])
    }

    @Test
    fun everyCellLinesUpWithItsWeekday() {
        for (start in listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)) {
            for (m in 1..12) {
                val grid = calendarGrid(YearMonth.of(2026, m), start)
                assertEquals(0, grid.size % 7)
                grid.forEachIndexed { i, date ->
                    if (date != null) assertEquals(start.plus((i % 7).toLong()), date.dayOfWeek)
                }
                assertEquals(YearMonth.of(2026, m).lengthOfMonth(), grid.count { it != null })
            }
        }
    }

    @Test
    fun leapFebruary_has29Days() {
        val grid = calendarGrid(YearMonth.of(2028, 2), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2028, 2, 29), grid.filterNotNull().last())
    }

    @Test
    fun monthNeedingSixRows() {
        // 2026-08-01 is a Saturday; 31 days starting in the last column needs six weeks.
        val grid = calendarGrid(YearMonth.of(2026, 8), DayOfWeek.SUNDAY)
        assertEquals(42, grid.size)
        assertEquals(LocalDate.of(2026, 8, 1), grid[6])
        assertNull(grid.last())
    }

    // --- daysWithData ---

    private val zone = ZoneId.of("America/Los_Angeles")
    private val october = YearMonth.of(2026, 10)
    private fun at(mo: Int, d: Int, h: Int, mi: Int = 0): Long =
        LocalDateTime.of(2026, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun marksEachKindOnItsDay_andDaysWithoutDataAreAbsent() {
        val marks = daysWithData(
            month = october,
            weightDates = listOf(LocalDate.of(2026, 10, 5)),
            foodTimes = listOf(at(10, 5, 12), at(10, 7, 8)),
            sleepStarts = listOf(at(10, 5, 23)),
            zone = zone,
        )
        assertEquals(DayMarks(weight = true, food = true, sleep = true), marks[LocalDate.of(2026, 10, 5)])
        assertEquals(DayMarks(food = true), marks[LocalDate.of(2026, 10, 7)])
        assertEquals(setOf(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7)), marks.keys)
    }

    @Test
    fun sleepAfterMidnight_marksThePreviousDay() {
        val marks = daysWithData(october, emptyList(), emptyList(), listOf(at(10, 9, 1)), zone)
        assertEquals(DayMarks(sleep = true), marks[LocalDate.of(2026, 10, 8)])
        assertNull(marks[LocalDate.of(2026, 10, 9)])
    }

    @Test
    fun sleepOnTheFirstAfterMidnight_belongsToThePreviousMonth() {
        // 1 AM on Oct 1 is assigned to Sep 30, so it marks nothing in October.
        val marks = daysWithData(october, emptyList(), emptyList(), listOf(at(10, 1, 1)), zone)
        assertTrue(marks.isEmpty())
        val september = daysWithData(YearMonth.of(2026, 9), emptyList(), emptyList(), listOf(at(10, 1, 1)), zone)
        assertEquals(DayMarks(sleep = true), september[LocalDate.of(2026, 9, 30)])
    }

    @Test
    fun foodNearMidnight_landsOnItsLocalDay() {
        val marks = daysWithData(october, emptyList(), listOf(at(10, 31, 23, 59), at(11, 1, 0, 1)), emptyList(), zone)
        assertEquals(setOf(LocalDate.of(2026, 10, 31)), marks.keys)
    }
}
