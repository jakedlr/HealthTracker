package com.jakedlr.healthtracker.ui

import java.util.Locale
import kotlin.math.roundToInt

/** One decimal place, e.g. 182.4. */
fun formatLbs(lbs: Double): String = String.format(Locale.getDefault(), "%.1f", lbs)

/** Whole calories, e.g. 1840. */
fun formatKcal(kcal: Double): String = kcal.roundToInt().toString()

/** One decimal place, e.g. 12.5. */
fun formatGrams(grams: Double): String = String.format(Locale.getDefault(), "%.1f", grams)

/** Up to two decimals without trailing zeros, e.g. 1, 1.5, 0.25. Also used to pre-fill inputs. */
fun formatAmount(value: Double): String =
    String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
