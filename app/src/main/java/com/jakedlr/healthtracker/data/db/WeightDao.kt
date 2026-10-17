package com.jakedlr.healthtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class WeightDateRange(
    @ColumnInfo(name = "first_date") val first: LocalDate?,
    @ColumnInfo(name = "last_date") val last: LocalDate?,
)

@Dao
interface WeightDao {
    @Upsert
    suspend fun upsert(entry: WeightEntry)

    @Delete
    suspend fun delete(entry: WeightEntry)

    @Query("SELECT * FROM weight_entry WHERE date = :date")
    suspend fun getByDate(date: LocalDate): WeightEntry?

    @Query("SELECT * FROM weight_entry WHERE date = :date")
    fun observeByDate(date: LocalDate): Flow<WeightEntry?>

    @Query("SELECT * FROM weight_entry ORDER BY date ASC")
    fun observeAll(): Flow<List<WeightEntry>>

    /** Entries with start <= date <= end, oldest first. */
    @Query("SELECT * FROM weight_entry WHERE date >= :start AND date <= :end ORDER BY date ASC")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<WeightEntry>>

    @Query("SELECT MIN(date) AS first_date, MAX(date) AS last_date FROM weight_entry")
    fun observeDateRange(): Flow<WeightDateRange>
}
