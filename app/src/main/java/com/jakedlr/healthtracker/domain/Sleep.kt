package com.jakedlr.healthtracker.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val MAX_SLEEP: Duration = Duration.ofHours(24)

/** One sleep period, as instants. */
data class SleepSpan(val startAtMillis: Long, val endAtMillis: Long) {
    val durationMillis: Long get() = endAtMillis - startAtMillis
}

/**
 * The day a sleep counts toward: local date of (start − 12 hours). The 12 hours come off the
 * local clock time, so the noon cutoff stays at exactly noon on daylight-saving days.
 */
fun sleepDate(startAtMillis: Long, zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(startAtMillis).atZone(zone).toLocalDateTime().minusHours(12).toLocalDate()

/** True if the two periods share any time. Periods that only touch (one ends as the other starts) don't overlap. */
fun overlaps(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Boolean = aStart < bEnd && aEnd > bStart

enum class SleepError { END_NOT_AFTER_START, TOO_LONG }

/** Null if valid. Exactly 24 hours is allowed; wake times in the future are allowed. */
fun validateSleep(startAtMillis: Long, endAtMillis: Long): SleepError? = when {
    endAtMillis <= startAtMillis -> SleepError.END_NOT_AFTER_START
    endAtMillis - startAtMillis > MAX_SLEEP.toMillis() -> SleepError.TOO_LONG
    else -> null
}

/** Total hours of sleep per assigned date. Dates without sleep are absent (gaps, never zeros). */
fun dailySleepHours(entries: List<SleepSpan>, zone: ZoneId): Map<LocalDate, Double> =
    entries.groupBy { sleepDate(it.startAtMillis, zone) }
        .mapValues { (_, spans) -> spans.sumOf { it.durationMillis } / 3_600_000.0 }
        .toSortedMap()
