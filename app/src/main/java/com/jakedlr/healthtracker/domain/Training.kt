package com.jakedlr.healthtracker.domain

import java.time.LocalDate

const val MAX_LOAD = 2000.0

/** Parses a load in the log's unit; null if blank, not a number, or outside [0, [MAX_LOAD]]. 0 means bodyweight. */
fun parseLoad(text: String): Double? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return value.takeIf { it >= 0.0 && it <= MAX_LOAD }
}

const val MAX_REPS = 999

/** Parses a rep count; null unless it is a whole number from 1 to [MAX_REPS]. */
fun parseReps(text: String): Int? =
    text.trim().toIntOrNull()?.takeIf { it in 1..MAX_REPS }

/** Exercise logs can be dated today or earlier, never in the future. */
fun isLoggableDate(date: LocalDate, today: LocalDate): Boolean = date <= today

/** Whether an exercise is done with both sides together or one side at a time. */
enum class Sidedness { SINGLE, DOUBLE }

enum class LoadUnit(val label: String) { LBS("lbs"), KG("kg") }

/** The day's main or best set for one side (or both, for a double-sided exercise). */
data class TopSet(val load: Double, val reps: Int)

/** What an exercise log records, shaped by the exercise's sidedness. */
sealed interface TopSets {
    data class Both(val set: TopSet) : TopSets

    /** A null side was skipped; never both. */
    data class PerSide(val left: TopSet?, val right: TopSet?) : TopSets {
        init {
            require(left != null || right != null) { "At most one side may be skipped" }
        }
    }
}

/** Left and right top sets for a single-sided exercise; null if both sides were skipped. */
fun perSide(left: TopSet?, right: TopSet?): TopSets.PerSide? =
    if (left == null && right == null) null else TopSets.PerSide(left, right)

/** The list after dragging the item at [from] to index [to]; the items in between shift by one. */
fun <T> List<T>.moved(from: Int, to: Int): List<T> =
    toMutableList().apply { add(to, removeAt(from)) }
