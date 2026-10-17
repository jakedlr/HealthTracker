package com.jakedlr.healthtracker.data

import androidx.room.withTransaction
import com.jakedlr.healthtracker.data.db.HealthDatabase
import com.jakedlr.healthtracker.data.db.SleepDao
import com.jakedlr.healthtracker.data.db.SleepEntry
import com.jakedlr.healthtracker.data.db.StartAtRange
import com.jakedlr.healthtracker.domain.SleepError
import com.jakedlr.healthtracker.domain.sleepDate
import com.jakedlr.healthtracker.domain.validateSleep
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface SaveSleepResult {
    data class Saved(val id: Long) : SaveSleepResult
    data class Invalid(val error: SleepError) : SaveSleepResult

    /** Rejected because it shares time with [existing]. */
    data class Overlaps(val existing: SleepEntry) : SaveSleepResult
}

class SleepRepository(
    private val database: HealthDatabase,
    private val dao: SleepDao,
) {
    /** Validates, then inserts unless it overlaps an existing entry. */
    suspend fun addSleep(startAt: Long, endAt: Long): SaveSleepResult {
        validateSleep(startAt, endAt)?.let { return SaveSleepResult.Invalid(it) }
        return database.withTransaction {
            dao.findOverlapping(startAt, endAt, excludeId = 0)?.let { return@withTransaction SaveSleepResult.Overlaps(it) }
            SaveSleepResult.Saved(dao.insert(SleepEntry(startAt = startAt, endAt = endAt)))
        }
    }

    /** Entries whose assigned date (see sleepDate) is [date]. */
    fun observeSleepForDate(date: LocalDate, zone: ZoneId): Flow<List<SleepEntry>> {
        // Assigned to [date] means starting between noon on [date] and noon the next day;
        // query a wider window and filter with the domain rule so the two can't disagree.
        val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = date.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.observeStartingBetween(from, to).map { entries ->
            entries.filter { sleepDate(it.startAt, zone) == date }
        }
    }

    fun observeStartAtRange(): Flow<StartAtRange> = dao.observeStartAtRange()
}
