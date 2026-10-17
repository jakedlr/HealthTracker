package com.jakedlr.healthtracker.data

import androidx.room.withTransaction
import com.jakedlr.healthtracker.data.db.EatenAtRange
import com.jakedlr.healthtracker.data.db.FoodDao
import com.jakedlr.healthtracker.data.db.FoodItem
import com.jakedlr.healthtracker.data.db.FoodLog
import com.jakedlr.healthtracker.data.db.HealthDatabase
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

sealed interface SaveItemResult {
    data class Saved(val itemId: Long) : SaveItemResult

    /** Another item already uses this name (ignoring case). */
    data class NameTaken(val existing: FoodItem) : SaveItemResult
}

class FoodRepository(
    private val database: HealthDatabase,
    private val dao: FoodDao,
) {
    fun searchItems(query: String): Flow<List<FoodItem>> = dao.searchItems(likePattern(query))

    fun observeItem(id: Long): Flow<FoodItem?> = dao.observeItem(id)

    suspend fun getItem(id: Long): FoodItem? = dao.getItem(id)

    /** [barcode] must already be normalized (see normalizeBarcode). */
    suspend fun findByBarcode(barcode: String): FoodItem? = dao.getItemByBarcode(barcode)

    /**
     * Attaches [barcode] to the item if it has none and no other item uses it.
     * Returns true if the barcode was linked.
     */
    suspend fun linkBarcode(itemId: Long, barcode: String): Boolean =
        database.withTransaction {
            if (dao.getItemByBarcode(barcode) != null) return@withTransaction false
            dao.setBarcodeIfMissing(itemId, barcode) == 1
        }

    suspend fun getLog(id: Long): FoodLog? = dao.getLog(id)

    /** Edits a past log. Totals recalculate from the log's own snapshot, never the library item. */
    suspend fun updateLog(id: Long, servings: Double, eatenAt: Long) =
        dao.updateLogServingsAndTime(id, servings, eatenAt)

    suspend fun deleteLog(id: Long) = dao.deleteLog(id)

    /** Logs [servings] of [item], snapshotting its current nutrition. */
    suspend fun logFood(item: FoodItem, servings: Double, eatenAt: Long) {
        dao.insertLog(FoodLog.snapshotOf(item, servings, eatenAt))
    }

    /** Manual entry: saves a new library item and logs it, or neither if the name is taken. */
    suspend fun createItemAndLog(item: FoodItem, servings: Double, eatenAt: Long): SaveItemResult =
        database.withTransaction {
            dao.getItemByName(item.name)?.let { return@withTransaction SaveItemResult.NameTaken(it) }
            val id = dao.insertItem(item.copy(id = 0))
            dao.insertLog(FoodLog.snapshotOf(item.copy(id = id), servings, eatenAt))
            SaveItemResult.Saved(id)
        }

    /** Changes the library item only; existing logs keep their snapshots. */
    suspend fun updateItem(item: FoodItem): SaveItemResult =
        database.withTransaction {
            val sameName = dao.getItemByName(item.name)
            if (sameName != null && sameName.id != item.id) {
                return@withTransaction SaveItemResult.NameTaken(sameName)
            }
            dao.updateItem(item)
            SaveItemResult.Saved(item.id)
        }

    /** Removes the item from the library; past logs keep their snapshot and lose only the link. */
    suspend fun deleteItem(item: FoodItem) = dao.deleteItem(item)

    fun observeLogsForDay(date: LocalDate, zone: ZoneId): Flow<List<FoodLog>> =
        observeLogsForDates(date..date, zone)

    /** Logs eaten on any local day in [dates]. */
    fun observeLogsForDates(dates: ClosedRange<LocalDate>, zone: ZoneId): Flow<List<FoodLog>> =
        dao.observeLogsBetween(
            start = dates.start.atStartOfDay(zone).toInstant().toEpochMilli(),
            end = dates.endInclusive.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        )

    fun observeEatenAtRange(): Flow<EatenAtRange> = dao.observeEatenAtRange()

    private fun likePattern(query: String): String {
        val escaped = query.trim()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return "%$escaped%"
    }
}
