package com.jakedlr.healthtracker.data

import androidx.room.withTransaction
import com.jakedlr.healthtracker.data.db.Exercise
import com.jakedlr.healthtracker.data.db.ExerciseCategory
import com.jakedlr.healthtracker.data.db.ExerciseLog
import com.jakedlr.healthtracker.data.db.ExerciseWithLastLogged
import com.jakedlr.healthtracker.data.db.HealthDatabase
import com.jakedlr.healthtracker.data.db.TrainingDao
import com.jakedlr.healthtracker.domain.LoadUnit
import com.jakedlr.healthtracker.domain.Sidedness
import com.jakedlr.healthtracker.domain.TopSets
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

const val RECENT_LOG_COUNT = 10

sealed interface TrainingSaveResult {
    data class Saved(val id: Long) : TrainingSaveResult

    /** Another category (or exercise) already uses this name, ignoring case. */
    data object NameTaken : TrainingSaveResult

    /** The exercise has logs, so its sidedness can no longer change. */
    data object SidednessLocked : TrainingSaveResult
}

/** Exercise categories, exercises, and exercise logs. Names are trimmed here; callers reject blank names. */
class TrainingRepository(
    private val database: HealthDatabase,
    private val dao: TrainingDao,
) {
    fun observeCategories(): Flow<List<ExerciseCategory>> = dao.observeCategories()

    fun observeCategory(id: Long): Flow<ExerciseCategory?> = dao.observeCategory(id)

    /** Adds the category at the bottom of the list. */
    suspend fun createCategory(name: String): TrainingSaveResult =
        database.withTransaction {
            val trimmed = availableName(name, selfId = null) { dao.getCategoryByName(it)?.id }
                ?: return@withTransaction TrainingSaveResult.NameTaken
            val id = dao.insertCategory(ExerciseCategory(name = trimmed, position = dao.nextCategoryPosition()))
            TrainingSaveResult.Saved(id)
        }

    suspend fun renameCategory(id: Long, name: String): TrainingSaveResult =
        database.withTransaction {
            val trimmed = availableName(name, selfId = id) { dao.getCategoryByName(it)?.id }
                ?: return@withTransaction TrainingSaveResult.NameTaken
            val category = dao.getCategory(id) ?: return@withTransaction TrainingSaveResult.Saved(id)
            dao.updateCategory(category.copy(name = trimmed))
            TrainingSaveResult.Saved(id)
        }

    /** Deletes an empty category; returns false (and changes nothing) if it still has exercises. */
    suspend fun deleteCategory(id: Long): Boolean =
        database.withTransaction {
            if (dao.countExercises(id) > 0) return@withTransaction false
            dao.deleteCategory(id)
            true
        }

    /** Saves the order after a drag: [idsInOrder] is the whole category list, top to bottom. */
    suspend fun reorderCategories(idsInOrder: List<Long>) = savePositions(idsInOrder, dao::setCategoryPosition)

    /** A category's exercises in the user's order, each with its last logged date. */
    fun observeExercises(categoryId: Long): Flow<List<ExerciseWithLastLogged>> = dao.observeExercises(categoryId)

    fun observeExercise(id: Long): Flow<Exercise?> = dao.observeExercise(id)

    suspend fun getExercise(id: Long): Exercise? = dao.getExercise(id)

    /** Adds the exercise at the bottom of its category. */
    suspend fun createExercise(
        categoryId: Long,
        name: String,
        sidedness: Sidedness,
        defaultUnit: LoadUnit,
    ): TrainingSaveResult =
        database.withTransaction {
            val trimmed = availableName(name, selfId = null) { dao.getExerciseByName(it)?.id }
                ?: return@withTransaction TrainingSaveResult.NameTaken
            val exercise = Exercise(
                categoryId = categoryId,
                name = trimmed,
                sidedness = sidedness,
                defaultUnit = defaultUnit,
                position = dao.nextExercisePosition(categoryId),
            )
            TrainingSaveResult.Saved(dao.insertExercise(exercise))
        }

    /**
     * Saves name, category, default unit, and sidedness. Moving to another category puts the exercise
     * at the bottom there. Sidedness can't change once the exercise has logs; past logs keep their unit.
     */
    suspend fun updateExercise(exercise: Exercise): TrainingSaveResult =
        database.withTransaction {
            val current = dao.getExercise(exercise.id) ?: return@withTransaction TrainingSaveResult.Saved(exercise.id)
            val trimmed = availableName(exercise.name, selfId = exercise.id) { dao.getExerciseByName(it)?.id }
                ?: return@withTransaction TrainingSaveResult.NameTaken
            if (exercise.sidedness != current.sidedness && dao.countLogs(exercise.id) > 0) {
                return@withTransaction TrainingSaveResult.SidednessLocked
            }
            val position = if (exercise.categoryId != current.categoryId) {
                dao.nextExercisePosition(exercise.categoryId)
            } else {
                current.position
            }
            dao.updateExercise(exercise.copy(name = trimmed, position = position))
            TrainingSaveResult.Saved(exercise.id)
        }

    /** Deletes the exercise and all of its logs. */
    suspend fun deleteExercise(id: Long) = dao.deleteExercise(id)

    /** Saves the order after a drag: [idsInOrder] is one category's whole exercise list, top to bottom. */
    suspend fun reorderExercises(idsInOrder: List<Long>) = savePositions(idsInOrder, dao::setExercisePosition)

    fun observeRecentLogs(exerciseId: Long): Flow<List<ExerciseLog>> =
        dao.observeRecentLogs(exerciseId, RECENT_LOG_COUNT)

    /** Every log for the exercise, newest date first. */
    fun observeAllLogs(exerciseId: Long): Flow<List<ExerciseLog>> = dao.observeAllLogs(exerciseId)

    suspend fun getLog(id: Long): ExerciseLog? = dao.getLog(id)

    suspend fun getLog(exerciseId: Long, date: LocalDate): ExerciseLog? = dao.getLogByDate(exerciseId, date)

    suspend fun countLogs(exerciseId: Long): Int = dao.countLogs(exerciseId)

    /**
     * One log per exercise and date. If [date] already has a log, it is replaced; otherwise the log being
     * edited ([editingId]) moves to [date], or a new log is added when there is none.
     */
    suspend fun saveLog(exerciseId: Long, editingId: Long?, date: LocalDate, unit: LoadUnit, sets: TopSets) =
        database.withTransaction {
            val existing = dao.getLogByDate(exerciseId, date)
            val id = existing?.id ?: editingId
            if (id != null) {
                dao.updateLog(ExerciseLog.of(exerciseId, date, unit, sets, id))
            } else {
                dao.insertLog(ExerciseLog.of(exerciseId, date, unit, sets))
            }
        }

    suspend fun deleteLog(id: Long) = dao.deleteLog(id)

    /**
     * [name] trimmed, or null if another row already uses it (ignoring case). [selfId] is the row being
     * renamed, which may keep its own name; [findOwner] returns the id of the row with a given name.
     */
    private suspend fun availableName(name: String, selfId: Long?, findOwner: suspend (String) -> Long?): String? {
        val trimmed = name.trim()
        val owner = findOwner(trimmed)
        return trimmed.takeIf { owner == null || owner == selfId }
    }

    /** Rewrites a whole list's positions in one transaction: [idsInOrder] top to bottom. */
    private suspend fun savePositions(idsInOrder: List<Long>, setPosition: suspend (id: Long, position: Int) -> Unit) =
        database.withTransaction {
            idsInOrder.forEachIndexed { position, id -> setPosition(id, position) }
        }
}
