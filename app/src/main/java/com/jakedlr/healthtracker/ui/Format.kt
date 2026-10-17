package com.jakedlr.healthtracker.ui

import java.util.Locale

/** One decimal place, e.g. 182.4. */
fun formatLbs(lbs: Double): String = String.format(Locale.getDefault(), "%.1f", lbs)
