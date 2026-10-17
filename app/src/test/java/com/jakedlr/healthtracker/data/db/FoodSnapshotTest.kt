package com.jakedlr.healthtracker.data.db

import com.jakedlr.healthtracker.domain.Nutrition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Snapshot math: a log's totals come from its own snapshot, never from the current library item. */
class FoodSnapshotTest {
    private val original = FoodItem(
        id = 7, name = "Oatmeal",
        caloriesKcal = 150.0, proteinG = 5.0, carbsG = 27.0, fiberG = 4.0, fatG = 3.0,
    )

    @Test
    fun snapshotCopiesNameAndNutrition() {
        val log = FoodLog.snapshotOf(original, servings = 2.0, eatenAt = 1_000)
        assertEquals(7L, log.foodItemId)
        assertEquals("Oatmeal", log.name)
        assertEquals(original.nutrition, log.perServing)
        assertEquals(Nutrition(300.0, 10.0, 54.0, 8.0, 6.0), log.toLoggedFood().total)
    }

    @Test
    fun editingItem_doesNotChangePastTotals() {
        val log = FoodLog.snapshotOf(original, servings = 2.0, eatenAt = 1_000)
        val before = log.toLoggedFood().total

        val edited = original.copy(name = "Oatmeal (new recipe)").withNutrition(Nutrition(400.0, 20.0, 50.0, 10.0, 12.0))

        assertEquals(before, log.toLoggedFood().total)
        assertNotEquals(edited.nutrition, log.perServing)
        // Only future logs pick up the edit.
        assertEquals(Nutrition(800.0, 40.0, 100.0, 20.0, 24.0), FoodLog.snapshotOf(edited, 2.0, 2_000).toLoggedFood().total)
    }

    @Test
    fun changingPastServings_recalculatesFromOwnSnapshot() {
        val log = FoodLog.snapshotOf(original, servings = 1.0, eatenAt = 1_000)
        original.withNutrition(Nutrition(999.0, 99.0, 99.0, 99.0, 99.0)) // library edited meanwhile

        val updated = log.copy(servings = 0.5)
        assertEquals(Nutrition(75.0, 2.5, 13.5, 2.0, 1.5), updated.toLoggedFood().total)
    }
}
