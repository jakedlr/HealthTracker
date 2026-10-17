package com.jakedlr.healthtracker.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    private val hour = 3_600_000L

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0): Long =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private fun date(mo: Int, d: Int) = LocalDate.of(2026, mo, d)

    // --- Attribution: the CLAUDE.md examples ---

    @Test
    fun bedAt11pm_wakeNextMorning_countsTowardBedDay() {
        assertEquals(date(10, 8), sleepDate(at(2026, 10, 8, 23), zone))
    }

    @Test
    fun bedAfterMidnight_countsTowardPreviousDay() {
        assertEquals(date(10, 8), sleepDate(at(2026, 10, 9, 1), zone))
    }

    @Test
    fun afternoonNap_countsTowardSameDay() {
        assertEquals(date(10, 9), sleepDate(at(2026, 10, 9, 14), zone))
    }

    // --- Attribution: the noon boundary ---

    @Test
    fun startingExactlyAtNoon_countsTowardSameDay() {
        assertEquals(date(10, 9), sleepDate(at(2026, 10, 9, 12, 0), zone))
    }

    @Test
    fun startingJustBeforeNoon_countsTowardPreviousDay() {
        assertEquals(date(10, 8), sleepDate(at(2026, 10, 9, 11, 59), zone))
    }

    @Test
    fun sleepSpanningNoon_countsByItsStart() {
        // 11 AM to 2 PM on 10/09: starts before noon, so it belongs to 10/08.
        val start = at(2026, 10, 9, 11)
        assertEquals(date(10, 8), sleepDate(start, zone))
    }

    @Test
    fun midnightExactly_countsTowardPreviousDay() {
        assertEquals(date(10, 8), sleepDate(at(2026, 10, 9, 0, 0), zone))
    }

    @Test
    fun daylightSavingDay_noonCutoffUsesLocalClock() {
        // US clocks spring forward at 2 AM on 2026-03-08. 12:30 PM local is after noon, so it
        // belongs to 03/08. (Subtracting 12 h from the instant instead would give 03/07.)
        assertEquals(date(3, 8), sleepDate(at(2026, 3, 8, 12, 30), zone))
        assertEquals(date(3, 7), sleepDate(at(2026, 3, 8, 11, 30), zone))
    }

    // --- Overlap ---

    @Test
    fun partialOverlap_eitherSide() {
        assertTrue(overlaps(0, 10 * hour, 9 * hour, 12 * hour))
        assertTrue(overlaps(9 * hour, 12 * hour, 0, 10 * hour))
    }

    @Test
    fun oneInsideTheOther_andIdentical() {
        assertTrue(overlaps(0, 10 * hour, 2 * hour, 3 * hour))
        assertTrue(overlaps(2 * hour, 3 * hour, 0, 10 * hour))
        assertTrue(overlaps(0, 8 * hour, 0, 8 * hour))
    }

    @Test
    fun touchingEdges_doNotOverlap() {
        assertFalse(overlaps(0, 7 * hour, 7 * hour, 8 * hour))
        assertFalse(overlaps(7 * hour, 8 * hour, 0, 7 * hour))
    }

    @Test
    fun separate_doNotOverlap() {
        assertFalse(overlaps(0, 7 * hour, 9 * hour, 10 * hour))
    }

    // --- Validation ---

    @Test
    fun validation() {
        assertEquals(SleepError.END_NOT_AFTER_START, validateSleep(5 * hour, 5 * hour))
        assertEquals(SleepError.END_NOT_AFTER_START, validateSleep(5 * hour, 4 * hour))
        assertNull(validateSleep(0, 24 * hour))
        assertEquals(SleepError.TOO_LONG, validateSleep(0, 24 * hour + 1))
        assertNull(validateSleep(0, 8 * hour))
    }

    // --- Daily totals ---

    @Test
    fun nightPlusNap_summedOnAssignedDate_missingDaysAbsent() {
        val entries = listOf(
            SleepSpan(at(2026, 10, 8, 23), at(2026, 10, 9, 7)), // 8 h -> 10/08
            SleepSpan(at(2026, 10, 8, 14), at(2026, 10, 8, 14, 30)), // 0.5 h nap -> 10/08
            SleepSpan(at(2026, 10, 11, 0, 30), at(2026, 10, 11, 6)), // 5.5 h -> 10/10
        )
        val daily = dailySleepHours(entries, zone)

        assertEquals(listOf(date(10, 8), date(10, 10)), daily.keys.toList())
        assertEquals(8.5, daily[date(10, 8)]!!, 1e-9)
        assertEquals(5.5, daily[date(10, 10)]!!, 1e-9)
        assertFalse(date(10, 9) in daily)
    }

    @Test
    fun noEntries_noDays() {
        assertTrue(dailySleepHours(emptyList(), zone).isEmpty())
    }
}
