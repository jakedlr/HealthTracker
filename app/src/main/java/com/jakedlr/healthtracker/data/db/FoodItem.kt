package com.jakedlr.healthtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jakedlr.healthtracker.domain.Nutrition

/**
 * A reusable library entry. Nutrition values are per serving.
 * Names are unique ignoring case; barcodes are unique when present (SQLite allows many NULLs).
 */
@Entity(
    tableName = "food_item",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["barcode"], unique = true),
    ],
)
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val barcode: String? = null,
    @ColumnInfo(name = "calories_kcal") val caloriesKcal: Double,
    @ColumnInfo(name = "protein_g") val proteinG: Double,
    @ColumnInfo(name = "carbs_g") val carbsG: Double,
    @ColumnInfo(name = "fiber_g") val fiberG: Double,
    @ColumnInfo(name = "fat_g") val fatG: Double,
) {
    val nutrition: Nutrition get() = Nutrition(caloriesKcal, proteinG, carbsG, fiberG, fatG)

    fun withNutrition(n: Nutrition) = copy(
        caloriesKcal = n.caloriesKcal,
        proteinG = n.proteinG,
        carbsG = n.carbsG,
        fiberG = n.fiberG,
        fatG = n.fatG,
    )
}
