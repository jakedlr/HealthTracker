package com.jakedlr.healthtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jakedlr.healthtracker.domain.LoggedFood
import com.jakedlr.healthtracker.domain.Nutrition

/**
 * One thing eaten. Name and per-serving nutrition are a snapshot of the library item at log
 * time; editing or deleting the item never changes this row (deleting only clears the link).
 */
@Entity(
    tableName = "food_log",
    foreignKeys = [
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["food_item_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("food_item_id"), Index("eaten_at")],
)
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "food_item_id") val foodItemId: Long?,
    @ColumnInfo(name = "eaten_at") val eatenAt: Long,
    val servings: Double,
    val name: String,
    @ColumnInfo(name = "calories_kcal") val caloriesKcal: Double,
    @ColumnInfo(name = "protein_g") val proteinG: Double,
    @ColumnInfo(name = "carbs_g") val carbsG: Double,
    @ColumnInfo(name = "fiber_g") val fiberG: Double,
    @ColumnInfo(name = "fat_g") val fatG: Double,
) {
    val perServing: Nutrition get() = Nutrition(caloriesKcal, proteinG, carbsG, fiberG, fatG)

    fun toLoggedFood() = LoggedFood(eatenAt, servings, perServing)

    companion object {
        /** The only way to build a log: copies the item's current values as the snapshot. */
        fun snapshotOf(item: FoodItem, servings: Double, eatenAt: Long) = FoodLog(
            foodItemId = item.id,
            eatenAt = eatenAt,
            servings = servings,
            name = item.name,
            caloriesKcal = item.caloriesKcal,
            proteinG = item.proteinG,
            carbsG = item.carbsG,
            fiberG = item.fiberG,
            fatG = item.fatG,
        )
    }
}
