package com.jakedlr.healthtracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

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
}
