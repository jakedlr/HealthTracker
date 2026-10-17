package com.jakedlr.healthtracker.data.db

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun localDate_roundTripsThroughEpochDay() {
        for (date in listOf(LocalDate.of(1970, 1, 1), LocalDate.of(2024, 2, 29), LocalDate.of(2026, 10, 16))) {
            assertEquals(date, converters.epochDayToLocalDate(converters.localDateToEpochDay(date)))
        }
        assertEquals(0L, converters.localDateToEpochDay(LocalDate.of(1970, 1, 1)))
    }

    @Test
    fun nullsPassThrough() {
        assertNull(converters.localDateToEpochDay(null))
        assertNull(converters.epochDayToLocalDate(null))
    }
}
