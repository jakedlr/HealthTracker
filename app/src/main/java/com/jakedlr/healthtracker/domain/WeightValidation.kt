package com.jakedlr.healthtracker.domain

const val MAX_WEIGHT_LBS = 1000.0

/** Parses user input into pounds; null if blank, not a number, or outside (0, [MAX_WEIGHT_LBS]]. */
fun parseWeightLbs(text: String): Double? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return value.takeIf { it > 0.0 && it <= MAX_WEIGHT_LBS }
}
