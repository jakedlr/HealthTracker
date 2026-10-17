package com.jakedlr.healthtracker.domain

import java.time.LocalDate

/** One graphed value for one local day. Days without data have no DailyValue at all. */
data class DailyValue(val date: LocalDate, val value: Double)

/**
 * The values inside [bounds], sorted by date. Missing days stay missing (gaps, never zeros),
 * so charts must plot by date rather than by index.
 */
fun pointsInRange(values: List<DailyValue>, bounds: ClosedRange<LocalDate>): List<DailyValue> =
    values.filter { it.date in bounds }.sortedBy { it.date }
