package com.jakedlr.healthtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodValidationTest {
    @Test
    fun servings_acceptsPositiveFractions() {
        assertEquals(1.5, parseServings("1.5")!!, 0.0)
        assertEquals(0.25, parseServings("0,25")!!, 0.0)
    }

    @Test
    fun servings_rejectsZeroNegativeAndGarbage() {
        assertNull(parseServings("0"))
        assertNull(parseServings("-1"))
        assertNull(parseServings(""))
        assertNull(parseServings("NaN"))
    }

    @Test
    fun nutrient_acceptsZero_rejectsNegativeAndBlank() {
        assertEquals(0.0, parseNutrient("0")!!, 0.0)
        assertEquals(12.5, parseNutrient(" 12.5 ")!!, 0.0)
        assertNull(parseNutrient("-0.1"))
        assertNull(parseNutrient(""))
        assertNull(parseNutrient("Infinity"))
    }

    @Test
    fun foodName_isTrimmed_andBlankRejected() {
        assertEquals("Greek yogurt", normalizeFoodName("  Greek yogurt "))
        assertNull(normalizeFoodName("   "))
    }
}
