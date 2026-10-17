package com.jakedlr.healthtracker.data

import com.jakedlr.healthtracker.data.db.WeightDao
import com.jakedlr.healthtracker.data.db.WeightEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class WeightRepository(private val dao: WeightDao) {
    fun observeWeight(date: LocalDate): Flow<WeightEntry?> = dao.observeByDate(date)

    fun observeAll(): Flow<List<WeightEntry>> = dao.observeAll()

    suspend fun getWeight(date: LocalDate): WeightEntry? = dao.getByDate(date)

    suspend fun saveWeight(date: LocalDate, weightLbs: Double) =
        dao.upsert(WeightEntry(date = date, weightLbs = weightLbs))

    suspend fun delete(entry: WeightEntry) = dao.delete(entry)
}
