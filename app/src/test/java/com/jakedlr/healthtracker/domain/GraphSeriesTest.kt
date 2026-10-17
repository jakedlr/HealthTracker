package com.jakedlr.healthtracker.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphSeriesTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    private val window = date(10, 10)..date(10, 16)

    private fun date(mo: Int, d: Int) = LocalDate.of(2026, mo, d)
    private fun at(d: Int, h: Int, mi: Int = 0): Long =
        LocalDateTime.of(2026, 10, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private val meal = Nutrition(caloriesKcal = 500.0, proteinG = 30.0, carbsG = 60.0, fiberG = 8.0, fatG = 15.0)

    @Test
    fun onlyDaysWithData_insideTheWindow_inDateOrder() {
        val series = buildGraphSeries(
            window = window,
            weights = listOf(
                DailyValue(date(10, 15), 181.0),
                DailyValue(date(10, 9), 183.0), // before the window
                DailyValue(date(10, 11), 182.0),
            ),
            food = emptyList(),
            sleep = emptyList(),
            zone = zone,
        )
        assertEquals(listOf(DailyValue(date(10, 11), 182.0), DailyValue(date(10, 15), 181.0)), series.weight)
        assertTrue(series.calories.isEmpty())
        assertTrue(series.sleepHours.isEmpty())
    }

    @Test
    fun caloriesAndMacros_comeFromTheSameDailyTotals() {
        val series = buildGraphSeries(
            window = window,
            weights = emptyList(),
            food = listOf(
                LoggedFood(at(12, 8), 1.0, meal),
                LoggedFood(at(12, 19), 2.0, meal),
                LoggedFood(at(14, 12), 0.5, meal),
            ),
            sleep = emptyList(),
            zone = zone,
        )
        val days = listOf(date(10, 12), date(10, 14))
        assertEquals(listOf(1500.0, 250.0), series.calories.map { it.value })
        assertEquals(listOf(90.0, 15.0), series.protein.map { it.value })
        assertEquals(listOf(180.0, 30.0), series.carbs.map { it.value })
        assertEquals(listOf(45.0, 7.5), series.fat.map { it.value })
        assertEquals(listOf(24.0, 4.0), series.fiber.map { it.value })
        for (s in listOf(series.calories, series.protein, series.carbs, series.fat, series.fiber)) {
            assertEquals(days, s.map { it.date })
        }
    }

    @Test
    fun sleepUsesAssignedDate_soA1amSleepLandsOnThePreviousDay() {
        val series = buildGraphSeries(
            window = window,
            weights = emptyList(),
            food = emptyList(),
            sleep = listOf(
                SleepSpan(at(13, 1), at(13, 8)), // 7 h, assigned to 10/12
                SleepSpan(at(13, 15), at(13, 16)), // 1 h nap, assigned to 10/13
            ),
            zone = zone,
        )
        assertEquals(
            listOf(DailyValue(date(10, 12), 7.0), DailyValue(date(10, 13), 1.0)),
            series.sleepHours,
        )
    }

    @Test
    fun sleepAssignedBeforeTheWindow_isExcluded() {
        // Starts 1 AM on the window's first day, so it belongs to the day before the window.
        val series = buildGraphSeries(
            window = window,
            weights = emptyList(),
            food = emptyList(),
            sleep = listOf(SleepSpan(at(10, 1), at(10, 7))),
            zone = zone,
        )
        assertTrue(series.sleepHours.isEmpty())
    }

    @Test
    fun foodJustAfterMidnight_countsOnItsOwnLocalDay() {
        val series = buildGraphSeries(
            window = window,
            weights = emptyList(),
            food = listOf(LoggedFood(at(16, 0, 5), 1.0, meal), LoggedFood(at(15, 23, 55), 1.0, meal)),
            sleep = emptyList(),
            zone = zone,
        )
        assertEquals(listOf(date(10, 15), date(10, 16)), series.calories.map { it.date })
    }
}
