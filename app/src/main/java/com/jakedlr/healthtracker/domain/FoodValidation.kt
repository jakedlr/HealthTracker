package com.jakedlr.healthtracker.domain

/** Parses a servings count; null unless it is a number greater than zero. Fractions are fine. */
fun parseServings(text: String): Double? =
    parseDecimal(text)?.takeIf { it > 0.0 }

/** Parses a per-serving nutrient amount; null unless it is a number of zero or more. */
fun parseNutrient(text: String): Double? =
    parseDecimal(text)?.takeIf { it >= 0.0 }

/** Trims a food name; null if blank. */
fun normalizeFoodName(text: String): String? =
    text.trim().takeIf { it.isNotEmpty() }

private fun parseDecimal(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
