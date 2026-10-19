package com.jakedlr.healthtracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** An exercise with the date of its latest log (null if never logged). */
data class ExerciseWithLastLogged(
    @Embedded val exercise: Exercise,
    @ColumnInfo(name = "last_logged") val lastLogged: LocalDate?,
)

@Dao
interface TrainingDao {
    @Query("SELECT * FROM exercise_category ORDER BY position, id")
    fun observeCategories(): Flow<List<ExerciseCategory>>

    @Query("SELECT * FROM exercise_category WHERE id = :id")
    fun observeCategory(id: Long): Flow<ExerciseCategory?>

    @Query("SELECT * FROM exercise_category WHERE id = :id")
    suspend fun getCategory(id: Long): ExerciseCategory?

    @Query("SELECT * FROM exercise_category WHERE name = :name")
    suspend fun getCategoryByName(name: String): ExerciseCategory?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM exercise_category")
    suspend fun nextCategoryPosition(): Int

    @Insert
    suspend fun insertCategory(category: ExerciseCategory): Long

    @Update
    suspend fun updateCategory(category: ExerciseCategory)

    @Query("UPDATE exercise_category SET position = :position WHERE id = :id")
    suspend fun setCategoryPosition(id: Long, position: Int)

    @Query("DELETE FROM exercise_category WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query(
        """
        SELECT exercise.*, (SELECT MAX(date) FROM exercise_log WHERE exercise_id = exercise.id) AS last_logged
        FROM exercise WHERE category_id = :categoryId ORDER BY position, id
        """,
    )
    fun observeExercises(categoryId: Long): Flow<List<ExerciseWithLastLogged>>

    @Query("SELECT COUNT(*) FROM exercise WHERE category_id = :categoryId")
    suspend fun countExercises(categoryId: Long): Int

    @Query("SELECT * FROM exercise WHERE id = :id")
    fun observeExercise(id: Long): Flow<Exercise?>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getExercise(id: Long): Exercise?

    @Query("SELECT * FROM exercise WHERE name = :name")
    suspend fun getExerciseByName(name: String): Exercise?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM exercise WHERE category_id = :categoryId")
    suspend fun nextExercisePosition(categoryId: Long): Int

    @Insert
    suspend fun insertExercise(exercise: Exercise): Long

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Query("UPDATE exercise SET position = :position WHERE id = :id")
    suspend fun setExercisePosition(id: Long, position: Int)

    @Query("DELETE FROM exercise WHERE id = :id")
    suspend fun deleteExercise(id: Long)

    @Query("SELECT * FROM exercise_log WHERE exercise_id = :exerciseId ORDER BY date DESC LIMIT :limit")
    fun observeRecentLogs(exerciseId: Long, limit: Int): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM exercise_log WHERE exercise_id = :exerciseId ORDER BY date DESC")
    fun observeAllLogs(exerciseId: Long): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM exercise_log WHERE id = :id")
    suspend fun getLog(id: Long): ExerciseLog?

    @Query("SELECT * FROM exercise_log WHERE exercise_id = :exerciseId AND date = :date")
    suspend fun getLogByDate(exerciseId: Long, date: LocalDate): ExerciseLog?

    @Query("SELECT COUNT(*) FROM exercise_log WHERE exercise_id = :exerciseId")
    suspend fun countLogs(exerciseId: Long): Int

    @Insert
    suspend fun insertLog(log: ExerciseLog): Long

    @Update
    suspend fun updateLog(log: ExerciseLog)

    @Query("DELETE FROM exercise_log WHERE id = :id")
    suspend fun deleteLog(id: Long)
}
