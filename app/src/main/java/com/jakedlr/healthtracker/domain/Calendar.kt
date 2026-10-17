package com.jakedlr.healthtracker.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Cells for a month view: blanks (null) before the 1st so it lands under its weekday, then every
 * day, then blanks to complete the last week. Always a multiple of 7.
 */
fun calendarGrid(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = List<LocalDate?>(leading) { null } + days
    val trailing = (7 - cells.size % 7) % 7
    return cells + List(trailing) { null }
}

/** Which kinds of data a day has. */
data class DayMarks(val weight: Boolean = false, val food: Boolean = false, val sleep: Boolean = false)

/**
 * Marks for every day in [month] that has data. Food counts on the local day it was eaten;
 * sleep on its assigned date (see sleepDate). Days without data are absent.
 */
fun daysWithData(
    month: YearMonth,
    weightDates: List<LocalDate>,
    foodTimes: List<Long>,
    sleepStarts: List<Long>,
    zone: ZoneId,
): Map<LocalDate, DayMarks> {
    val marks = mutableMapOf<LocalDate, DayMarks>()
    fun mark(date: LocalDate, change: (DayMarks) -> DayMarks) {
        if (YearMonth.from(date) == month) marks[date] = change(marks[date] ?: DayMarks())
    }
    weightDates.forEach { mark(it) { m -> m.copy(weight = true) } }
    foodTimes.forEach { mark(Instant.ofEpochMilli(it).atZone(zone).toLocalDate()) { m -> m.copy(food = true) } }
    sleepStarts.forEach { mark(sleepDate(it, zone)) { m -> m.copy(sleep = true) } }
    return marks
}
