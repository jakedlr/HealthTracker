package com.jakedlr.healthtracker.domain

import java.time.LocalDate

/** Every SQLite database file starts with these 16 bytes. */
private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

/** True if [firstBytes] (the start of a file) is a SQLite database header. */
fun isSqliteHeader(firstBytes: ByteArray): Boolean =
    firstBytes.size >= SQLITE_HEADER.size && SQLITE_HEADER.indices.all { firstBytes[it] == SQLITE_HEADER[it] }

/** Suggested export file name, e.g. "health-tracker-2026-10-16.db". */
fun exportFileName(date: LocalDate): String = "health-tracker-$date.db"
