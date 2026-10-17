package com.jakedlr.healthtracker.domain

import java.time.LocalDate

enum class GraphRange(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    THREE_MONTHS("3 months"),
    ALL_TIME("All time"),
}

/**
 * Date window shared by every graph.
 *
 * Start = max(firstAny, today − range) (just firstAny for all time); End = min(today, lastAny).
 * "Last week" is 7 calendar days counting today; month ranges start on the same date that many months ago.
 * [firstAny]/[lastAny] are the earliest/latest measurement dates across all data types.
 * Returns null when there is no data or no data falls inside the window.
 */
fun graphBounds(
    range: GraphRange,
    today: LocalDate,
    firstAny: LocalDate?,
    lastAny: LocalDate?,
): ClosedRange<LocalDate>? {
    if (firstAny == null || lastAny == null) return null
    val rangeStart = when (range) {
        GraphRange.WEEK -> today.minusDays(6)
        GraphRange.MONTH -> today.minusMonths(1)
        GraphRange.THREE_MONTHS -> today.minusMonths(3)
        GraphRange.ALL_TIME -> null
    }
    val start = if (rangeStart == null) firstAny else maxOf(firstAny, rangeStart)
    val end = minOf(today, lastAny)
    return if (start <= end) start..end else null
}
