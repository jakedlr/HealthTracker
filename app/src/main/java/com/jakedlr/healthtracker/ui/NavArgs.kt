package com.jakedlr.healthtracker.ui

import androidx.lifecycle.SavedStateHandle
import java.time.LocalDate

/** Optional navigation argument: a day as epoch day, or [NO_DATE]. Used by the History "Add" shortcuts. */
const val DATE_ARG = "date"
const val NO_DATE = -1L

/** The day passed in [DATE_ARG], or null when the screen was opened without one. */
fun SavedStateHandle.optionalDate(): LocalDate? =
    get<Long>(DATE_ARG)?.takeIf { it != NO_DATE }?.let(LocalDate::ofEpochDay)
