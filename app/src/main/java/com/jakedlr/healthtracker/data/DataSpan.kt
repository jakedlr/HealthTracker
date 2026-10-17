package com.jakedlr.healthtracker.data

import com.jakedlr.healthtracker.domain.sleepDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Earliest and latest dates with any data (CLAUDE.md's firstAny/lastAny); nulls when there is none. */
data class DataSpan(val first: LocalDate?, val last: LocalDate?)

/**
 * firstAny/lastAny across weight (its date), food (local date eaten), and sleep (assigned date).
 * sleepDate() never decreases as start time grows, so the earliest and latest starts give the
 * earliest and latest assigned dates.
 */
fun observeDataSpan(
    weight: WeightRepository,
    food: FoodRepository,
    sleep: SleepRepository,
    zone: ZoneId,
): Flow<DataSpan> = combine(
    weight.observeDateRange(),
    food.observeEatenAtRange(),
    sleep.observeStartAtRange(),
) { w, eatenAt, sleepStarts ->
    fun Long.localDate() = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
    DataSpan(
        first = listOfNotNull(w.first, eatenAt.first?.localDate(), sleepStarts.first?.let { sleepDate(it, zone) }).minOrNull(),
        last = listOfNotNull(w.last, eatenAt.last?.localDate(), sleepStarts.last?.let { sleepDate(it, zone) }).maxOrNull(),
    )
}
