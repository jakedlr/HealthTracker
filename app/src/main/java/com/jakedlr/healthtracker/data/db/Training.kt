package com.jakedlr.healthtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jakedlr.healthtracker.domain.LoadUnit
import com.jakedlr.healthtracker.domain.Sidedness
import com.jakedlr.healthtracker.domain.TopSet
import com.jakedlr.healthtracker.domain.TopSets
import java.time.LocalDate

/** A user-made group of exercises such as "Leg day". Names are unique ignoring case; [position] orders the list. */
@Entity(
    tableName = "exercise_category",
    indices = [Index(value = ["name"], unique = true)],
)
data class ExerciseCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val position: Int,
)

/**
 * One tracked movement. Names are unique across all categories, ignoring case.
 * A category with exercises can't be deleted (RESTRICT); [position] orders exercises within their category.
 */
@Entity(
    tableName = "exercise",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseCategory::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["category_id"]),
    ],
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val sidedness: Sidedness,
    @ColumnInfo(name = "default_unit") val defaultUnit: LoadUnit,
    val position: Int,
)

/**
 * One exercise's top set for one day; at most one per exercise and date.
 * A double-sided exercise fills [load]/[reps]; a single-sided one fills the left and right pairs,
 * with a skipped side left null. The unit is the log's own and is never converted.
 */
@Entity(
    tableName = "exercise_log",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["exercise_id", "date"], unique = true)],
)
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    val date: LocalDate,
    val unit: LoadUnit,
    val load: Double? = null,
    val reps: Int? = null,
    @ColumnInfo(name = "left_load") val leftLoad: Double? = null,
    @ColumnInfo(name = "left_reps") val leftReps: Int? = null,
    @ColumnInfo(name = "right_load") val rightLoad: Double? = null,
    @ColumnInfo(name = "right_reps") val rightReps: Int? = null,
) {
    val topSets: TopSets
        get() = if (load != null && reps != null) {
            TopSets.Both(TopSet(load, reps))
        } else {
            TopSets.PerSide(topSet(leftLoad, leftReps), topSet(rightLoad, rightReps))
        }

    companion object {
        fun of(exerciseId: Long, date: LocalDate, unit: LoadUnit, sets: TopSets, id: Long = 0) = when (sets) {
            is TopSets.Both -> ExerciseLog(id, exerciseId, date, unit, load = sets.set.load, reps = sets.set.reps)
            is TopSets.PerSide -> ExerciseLog(
                id, exerciseId, date, unit,
                leftLoad = sets.left?.load, leftReps = sets.left?.reps,
                rightLoad = sets.right?.load, rightReps = sets.right?.reps,
            )
        }

        private fun topSet(load: Double?, reps: Int?) = if (load != null && reps != null) TopSet(load, reps) else null
    }
}
