package com.jakedlr.healthtracker.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    @Test
    fun exportFileName_includesTheDate() {
        assertEquals("health-tracker-2026-10-16.db", exportFileName(LocalDate.of(2026, 10, 16)))
        assertEquals("health-tracker-2027-01-05.db", exportFileName(LocalDate.of(2027, 1, 5)))
    }

    @Test
    fun sqliteHeader_isRecognized() {
        val header = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII) + ByteArray(84)
        assertTrue(isSqliteHeader(header))
    }

    @Test
    fun otherFiles_areNot() {
        assertFalse(isSqliteHeader("SQLite format 2\u0000".toByteArray(Charsets.US_ASCII)))
        assertFalse(isSqliteHeader("hello world, not a db".toByteArray()))
        assertFalse(isSqliteHeader("SQLite form".toByteArray())) // too short
        assertFalse(isSqliteHeader(ByteArray(0)))
    }
}
