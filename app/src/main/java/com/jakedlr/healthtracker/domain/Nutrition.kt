package com.jakedlr.healthtracker.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Nutrition(
    val caloriesKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fiberG: Double,
    val fatG: Double,
) {
    operator fun plus(other: Nutrition) = Nutrition(
        caloriesKcal = caloriesKcal + other.caloriesKcal,
        proteinG = proteinG + other.proteinG,
        carbsG = carbsG + other.carbsG,
        fiberG = fiberG + other.fiberG,
        fatG = fatG + other.fatG,
    )

    operator fun times(factor: Double) = Nutrition(
        caloriesKcal = caloriesKcal * factor,
        proteinG = proteinG * factor,
        carbsG = carbsG * factor,
        fiberG = fiberG * factor,
        fatG = fatG * factor,
    )

    companion object {
        val ZERO = Nutrition(0.0, 0.0, 0.0, 0.0, 0.0)
    }
}

/**
 * One thing eaten. [perServing] is the snapshot copied from the library item at log time,
 * so later edits to the item never change [total].
 */
data class LoggedFood(
    val eatenAtMillis: Long,
    val servings: Double,
    val perServing: Nutrition,
) {
    val total: Nutrition get() = perServing * servings
}

/** Sums nutrition per local day in [zone]. Days without logs are absent (gaps, never zeros). */
fun dailyNutrition(logs: List<LoggedFood>, zone: ZoneId): Map<LocalDate, Nutrition> =
    logs.groupBy { Instant.ofEpochMilli(it.eatenAtMillis).atZone(zone).toLocalDate() }
        .mapValues { (_, dayLogs) -> dayLogs.fold(Nutrition.ZERO) { sum, log -> sum + log.total } }
        .toSortedMap()
