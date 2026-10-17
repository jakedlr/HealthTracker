package com.jakedlr.healthtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionTest {
    private val perServing = Nutrition(caloriesKcal = 200.0, proteinG = 10.0, carbsG = 30.0, fiberG = 4.0, fatG = 5.0)

    @Test
    fun totalIsPerServingTimesServings_withFractionalServings() {
        val log = LoggedFood(eatenAtMillis = 0, servings = 1.5, perServing = perServing)
        assertEquals(Nutrition(300.0, 15.0, 45.0, 6.0, 7.5), log.total)
    }

    @Test
    fun plusAddsEveryField() {
        assertEquals(Nutrition(400.0, 20.0, 60.0, 8.0, 10.0), perServing + perServing)
    }

    @Test
    fun zeroIsIdentity() {
        assertEquals(perServing, Nutrition.ZERO + perServing)
    }
}
