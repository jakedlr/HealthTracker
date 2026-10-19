package com.jakedlr.healthtracker.ui

import com.jakedlr.healthtracker.domain.LoadUnit
import com.jakedlr.healthtracker.domain.Sidedness
import com.jakedlr.healthtracker.domain.TopSet
import com.jakedlr.healthtracker.domain.TopSets
import java.util.Locale
import kotlin.math.roundToInt

/** One decimal place, e.g. 182.4. */
fun formatLbs(lbs: Double): String = String.format(Locale.getDefault(), "%.1f", lbs)

/** Whole calories, e.g. 1840. */
fun formatKcal(kcal: Double): String = kcal.roundToInt().toString()

/** One decimal place, e.g. 12.5. */
fun formatGrams(grams: Double): String = String.format(Locale.getDefault(), "%.1f", grams)

/** Hours and minutes, e.g. "7 h 45 m", "45 m". */
fun formatDuration(millis: Long): String {
    val totalMinutes = (millis + 30_000) / 60_000
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h == 0L) "$m m" else "$h h $m m"
}

/** Up to two decimals without trailing zeros, e.g. 1, 1.5, 0.25. Also used to pre-fill inputs. */
fun formatAmount(value: Double): String =
    String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

/** An exercise log's top sets, e.g. "135 lbs × 8" or "L 20 × 10 · R 22.5 × 8 lbs" ("—" for a skipped side). */
fun formatTopSets(sets: TopSets, unit: LoadUnit): String = when (sets) {
    is TopSets.Both -> "${formatAmount(sets.set.load)} ${unit.label} × ${sets.set.reps}"
    is TopSets.PerSide -> "L ${side(sets.left)} · R ${side(sets.right)} ${unit.label}"
}

private fun side(set: TopSet?): String = set?.let { "${formatAmount(it.load)} × ${it.reps}" } ?: "—"

fun Sidedness.label(): String = when (this) {
    Sidedness.SINGLE -> "Single-sided"
    Sidedness.DOUBLE -> "Double-sided"
}
