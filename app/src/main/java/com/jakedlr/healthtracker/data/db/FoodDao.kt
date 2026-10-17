package com.jakedlr.healthtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class EatenAtRange(
    @ColumnInfo(name = "first_eaten") val first: Long?,
    @ColumnInfo(name = "last_eaten") val last: Long?,
)

@Dao
interface FoodDao {
    @Insert
    suspend fun insertItem(item: FoodItem): Long

    @Update
    suspend fun updateItem(item: FoodItem)

    @Delete
    suspend fun deleteItem(item: FoodItem)

    @Query("SELECT * FROM food_item WHERE id = :id")
    suspend fun getItem(id: Long): FoodItem?

    @Query("SELECT * FROM food_item WHERE id = :id")
    fun observeItem(id: Long): Flow<FoodItem?>

    /** Case-insensitive because the name column is COLLATE NOCASE. */
    @Query("SELECT * FROM food_item WHERE name = :name")
    suspend fun getItemByName(name: String): FoodItem?

    @Query("SELECT * FROM food_item WHERE barcode = :barcode")
    suspend fun getItemByBarcode(barcode: String): FoodItem?

    /** Sets the barcode only if the item has none; returns the number of rows changed. */
    @Query("UPDATE food_item SET barcode = :barcode WHERE id = :id AND barcode IS NULL")
    suspend fun setBarcodeIfMissing(id: Long, barcode: String): Int

    /**
     * Items whose name contains [pattern] (a LIKE pattern escaped with '\'), most recently
     * logged first, then never-logged items alphabetically.
     */
    @Query(
        """
        SELECT food_item.* FROM food_item
        LEFT JOIN (
            SELECT food_item_id, MAX(eaten_at) AS last_eaten FROM food_log GROUP BY food_item_id
        ) AS recent ON recent.food_item_id = food_item.id
        WHERE food_item.name LIKE :pattern ESCAPE '\'
        ORDER BY recent.last_eaten IS NULL, recent.last_eaten DESC, food_item.name
        """,
    )
    fun searchItems(pattern: String): Flow<List<FoodItem>>

    @Insert
    suspend fun insertLog(log: FoodLog): Long

    @Query("SELECT * FROM food_log WHERE id = :id")
    suspend fun getLog(id: Long): FoodLog?

    /** Logs with start <= eaten_at < end. */
    @Query("SELECT * FROM food_log WHERE eaten_at >= :start AND eaten_at < :end ORDER BY eaten_at")
    fun observeLogsBetween(start: Long, end: Long): Flow<List<FoodLog>>

    @Query("SELECT MIN(eaten_at) AS first_eaten, MAX(eaten_at) AS last_eaten FROM food_log")
    fun observeEatenAtRange(): Flow<EatenAtRange>
}
