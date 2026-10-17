package com.jakedlr.healthtracker.domain

import java.time.LocalDate
import java.time.ZoneId

/** Everything the four graphs plot. Each list holds only days with data, sorted, inside the window. */
data class GraphSeries(
    val weight: List<DailyValue> = emptyList(),
    val calories: List<DailyValue> = emptyList(),
    val protein: List<DailyValue> = emptyList(),
    val carbs: List<DailyValue> = emptyList(),
    val fat: List<DailyValue> = emptyList(),
    val fiber: List<DailyValue> = emptyList(),
    val sleepHours: List<DailyValue> = emptyList(),
)

/**
 * Builds the graph series for [window]. Food is summed per local day, sleep per assigned day
 * (see sleepDate); days without data are left out, never zero.
 */
fun buildGraphSeries(
    window: ClosedRange<LocalDate>,
    weights: List<DailyValue>,
    food: List<LoggedFood>,
    sleep: List<SleepSpan>,
    zone: ZoneId,
): GraphSeries {
    val nutrition = dailyNutrition(food, zone)
    fun nutrient(select: (Nutrition) -> Double) =
        pointsInRange(nutrition.map { (date, n) -> DailyValue(date, select(n)) }, window)

    return GraphSeries(
        weight = pointsInRange(weights, window),
        calories = nutrient { it.caloriesKcal },
        protein = nutrient { it.proteinG },
        carbs = nutrient { it.carbsG },
        fat = nutrient { it.fatG },
        fiber = nutrient { it.fiberG },
        sleepHours = pointsInRange(
            dailySleepHours(sleep, zone).map { (date, hours) -> DailyValue(date, hours) },
            window,
        ),
    )
}
