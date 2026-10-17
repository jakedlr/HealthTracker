package com.jakedlr.healthtracker.ui.food

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultEatenAtTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    private val now = LocalDateTime.of(2026, 10, 16, 13, 45, 30)
    private val clock = Clock.fixed(now.atZone(zone).toInstant(), zone)

    @Test
    fun noDay_isNowToTheMinute() {
        assertEquals(LocalDateTime.of(2026, 10, 16, 13, 45), defaultEatenAt(clock, null))
    }

    @Test
    fun pastDay_usesTheCurrentTimeOfDayOnThatDay() {
        assertEquals(LocalDateTime.of(2026, 10, 3, 13, 45), defaultEatenAt(clock, LocalDate.of(2026, 10, 3)))
    }

    @Test
    fun today_isNow() {
        assertEquals(LocalDateTime.of(2026, 10, 16, 13, 45), defaultEatenAt(clock, LocalDate.of(2026, 10, 16)))
    }
}
