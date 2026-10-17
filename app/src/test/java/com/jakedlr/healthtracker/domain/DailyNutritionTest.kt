package com.jakedlr.healthtracker.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyNutritionTest {
    // A non-UTC zone, so a UTC-based implementation would put logs on the wrong day.
    private val zone = ZoneId.of("America/Los_Angeles")
    private val per100 = Nutrition(100.0, 1.0, 2.0, 3.0, 4.0)

    private fun log(dateTime: LocalDateTime, servings: Double = 1.0) =
        LoggedFood(dateTime.atZone(zone).toInstant().toEpochMilli(), servings, per100)

    @Test
    fun sumsPerLocalDay_andMissingDaysAreAbsent() {
        val logs = listOf(
            log(LocalDateTime.of(2026, 10, 1, 8, 0)),
            log(LocalDateTime.of(2026, 10, 1, 19, 0), servings = 2.0),
            log(LocalDateTime.of(2026, 10, 3, 12, 0), servings = 0.5),
        )
        val daily = dailyNutrition(logs, zone)

        assertEquals(setOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)), daily.keys)
        assertEquals(per100 * 3.0, daily[LocalDate.of(2026, 10, 1)])
        assertEquals(per100 * 0.5, daily[LocalDate.of(2026, 10, 3)])
        assertFalse(LocalDate.of(2026, 10, 2) in daily)
    }

    @Test
    fun logsAroundMidnight_landOnTheirLocalDay() {
        val logs = listOf(
            log(LocalDateTime.of(2026, 10, 1, 23, 59)),
            log(LocalDateTime.of(2026, 10, 2, 0, 0)),
        )
        val daily = dailyNutrition(logs, zone)

        // 23:59 Pacific is already Oct 2 in UTC; it must still count toward Oct 1.
        assertEquals(per100, daily[LocalDate.of(2026, 10, 1)])
        assertEquals(per100, daily[LocalDate.of(2026, 10, 2)])
    }

    @Test
    fun noLogs_noDays() {
        assertTrue(dailyNutrition(emptyList(), zone).isEmpty())
    }
}
