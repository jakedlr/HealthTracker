package com.jakedlr.healthtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightValidationTest {
    @Test
    fun parsesDecimals() {
        assertEquals(182.4, parseWeightLbs("182.4")!!, 0.0)
        assertEquals(182.4, parseWeightLbs(" 182,4 ")!!, 0.0)
        assertEquals(180.0, parseWeightLbs("180")!!, 0.0)
    }

    @Test
    fun rejectsBlankGarbageAndOutOfRange() {
        assertNull(parseWeightLbs(""))
        assertNull(parseWeightLbs("abc"))
        assertNull(parseWeightLbs("0"))
        assertNull(parseWeightLbs("-150"))
        assertNull(parseWeightLbs("1000.1"))
    }
}
