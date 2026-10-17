package com.jakedlr.healthtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class StartAtRange(
    @ColumnInfo(name = "first_start") val first: Long?,
    @ColumnInfo(name = "last_start") val last: Long?,
)

@Dao
interface SleepDao {
    @Insert
    suspend fun insert(entry: SleepEntry): Long

    @Query("SELECT * FROM sleep_entry WHERE id = :id")
    suspend fun get(id: Long): SleepEntry?

    /** The earliest entry sharing time with [start, end), ignoring [excludeId] (0 to exclude nothing). */
    @Query(
        """
        SELECT * FROM sleep_entry
        WHERE start_at < :end AND end_at > :start AND id != :excludeId
        ORDER BY start_at LIMIT 1
        """,
    )
    suspend fun findOverlapping(start: Long, end: Long, excludeId: Long): SleepEntry?

    /** Entries with from <= start_at < to. */
    @Query("SELECT * FROM sleep_entry WHERE start_at >= :from AND start_at < :to ORDER BY start_at")
    fun observeStartingBetween(from: Long, to: Long): Flow<List<SleepEntry>>

    @Query("SELECT MIN(start_at) AS first_start, MAX(start_at) AS last_start FROM sleep_entry")
    fun observeStartAtRange(): Flow<StartAtRange>
}
