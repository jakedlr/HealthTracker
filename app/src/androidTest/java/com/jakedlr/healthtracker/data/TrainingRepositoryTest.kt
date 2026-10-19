package com.jakedlr.healthtracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jakedlr.healthtracker.data.db.HealthDatabase
import com.jakedlr.healthtracker.domain.LoadUnit
import com.jakedlr.healthtracker.domain.Sidedness
import com.jakedlr.healthtracker.domain.TopSet
import com.jakedlr.healthtracker.domain.TopSets
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainingRepositoryTest {
    private lateinit var db: HealthDatabase
    private lateinit var repository: TrainingRepository

    private val oct15 = LocalDate.of(2026, 10, 15)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HealthDatabase::class.java,
        ).build()
        repository = TrainingRepository(db, db.trainingDao())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun category(name: String): Long =
        (repository.createCategory(name) as TrainingSaveResult.Saved).id

    private suspend fun exercise(
        categoryId: Long,
        name: String,
        sidedness: Sidedness = Sidedness.DOUBLE,
    ): Long = (repository.createExercise(categoryId, name, sidedness, LoadUnit.LBS) as TrainingSaveResult.Saved).id

    private fun double(load: Double, reps: Int) = TopSets.Double(TopSet(load, reps))

    private suspend fun categoryNames() = repository.observeCategories().first().map { it.name }

    private suspend fun exerciseNames(categoryId: Long) =
        repository.observeExercises(categoryId).first().map { it.exercise.name }

    // Categories

    @Test
    fun categoryNames_areTrimmed_andUniqueIgnoringCase() = runTest {
        category("  Leg day ")

        assertEquals(TrainingSaveResult.NameTaken, repository.createCategory("LEG DAY"))
        assertEquals(listOf("Leg day"), categoryNames())
    }

    @Test
    fun newCategories_goToTheBottom() = runTest {
        category("Leg day")
        category("Back day")
        category("Arms")

        assertEquals(listOf("Leg day", "Back day", "Arms"), categoryNames())
    }

    @Test
    fun reorderingCategories_persistsTheNewOrder() = runTest {
        val legs = category("Leg day")
        val back = category("Back day")
        val arms = category("Arms")

        repository.reorderCategories(listOf(arms, legs, back))

        assertEquals(listOf("Arms", "Leg day", "Back day"), categoryNames())
    }

    @Test
    fun renamingCategory_toAnotherCategorysName_isRefused() = runTest {
        val legs = category("Leg day")
        category("Back day")

        assertEquals(TrainingSaveResult.NameTaken, repository.renameCategory(legs, "back DAY"))
        assertEquals(TrainingSaveResult.Saved(legs), repository.renameCategory(legs, "Legs"))
        assertEquals(listOf("Legs", "Back day"), categoryNames())
    }

    @Test
    fun deletingCategory_keepsTheRestInOrder() = runTest {
        category("Leg day")
        val back = category("Back day")
        category("Arms")

        assertTrue(repository.deleteCategory(back))
        category("Core")

        assertEquals(listOf("Leg day", "Arms", "Core"), categoryNames())
    }

    @Test
    fun deletingNonEmptyCategory_isRefused() = runTest {
        val legs = category("Leg day")
        exercise(legs, "Squat")

        assertFalse(repository.deleteCategory(legs))
        assertEquals(listOf("Leg day"), categoryNames())
    }

    // Exercises

    @Test
    fun exerciseNames_areUniqueAcrossCategories_ignoringCase() = runTest {
        val legs = category("Leg day")
        val lower = category("Lower body")
        exercise(legs, " Squat ")

        assertEquals(
            TrainingSaveResult.NameTaken,
            repository.createExercise(lower, "squat", Sidedness.DOUBLE, LoadUnit.KG),
        )
        assertEquals(listOf("Squat"), exerciseNames(legs))
        assertEquals(emptyList<String>(), exerciseNames(lower))
    }

    @Test
    fun newExercises_goToTheBottomOfTheirCategory_andReorderPersists() = runTest {
        val legs = category("Leg day")
        val squat = exercise(legs, "Squat")
        val lunge = exercise(legs, "Lunge")
        val press = exercise(legs, "Leg press")
        assertEquals(listOf("Squat", "Lunge", "Leg press"), exerciseNames(legs))

        repository.reorderExercises(listOf(press, squat, lunge))

        assertEquals(listOf("Leg press", "Squat", "Lunge"), exerciseNames(legs))
    }

    @Test
    fun movingExercise_putsItAtTheBottomOfTheNewCategory() = runTest {
        val legs = category("Leg day")
        val back = category("Back day")
        val deadlift = exercise(legs, "Deadlift")
        exercise(back, "Row")
        exercise(back, "Pull-up")

        val moved = repository.getExercise(deadlift)!!.copy(categoryId = back)
        assertEquals(TrainingSaveResult.Saved(deadlift), repository.updateExercise(moved))

        assertEquals(emptyList<String>(), exerciseNames(legs))
        assertEquals(listOf("Row", "Pull-up", "Deadlift"), exerciseNames(back))
    }

    @Test
    fun sidedness_canChangeUntilTheExerciseHasLogs() = runTest {
        val legs = category("Leg day")
        val lunge = exercise(legs, "Lunge", Sidedness.DOUBLE)

        val single = repository.getExercise(lunge)!!.copy(sidedness = Sidedness.SINGLE)
        assertEquals(TrainingSaveResult.Saved(lunge), repository.updateExercise(single))

        repository.saveLog(lunge, null, oct15, LoadUnit.LBS, TopSets.PerSide(TopSet(20.0, 10), null))
        val backToDouble = repository.getExercise(lunge)!!.copy(sidedness = Sidedness.DOUBLE)

        assertEquals(TrainingSaveResult.SidednessLocked, repository.updateExercise(backToDouble))
        assertEquals(Sidedness.SINGLE, repository.getExercise(lunge)!!.sidedness)
    }

    @Test
    fun changingDefaultUnit_leavesPastLogsInTheirOwnUnit() = runTest {
        val legs = category("Leg day")
        val press = exercise(legs, "Leg press")
        repository.saveLog(press, null, oct15, LoadUnit.LBS, double(200.0, 10))

        repository.updateExercise(repository.getExercise(press)!!.copy(defaultUnit = LoadUnit.KG))

        val log = repository.observeAllLogs(press).first().single()
        assertEquals(LoadUnit.LBS, log.unit)
        assertEquals(double(200.0, 10), log.topSets)
    }

    @Test
    fun deletingExercise_removesItsLogs() = runTest {
        val legs = category("Leg day")
        val squat = exercise(legs, "Squat")
        repository.saveLog(squat, null, oct15, LoadUnit.LBS, double(135.0, 8))
        repository.saveLog(squat, null, oct15.plusDays(2), LoadUnit.LBS, double(140.0, 8))
        assertEquals(2, repository.countLogs(squat))

        repository.deleteExercise(squat)

        assertNull(repository.getExercise(squat))
        assertEquals(0, repository.countLogs(squat))
        assertTrue(repository.deleteCategory(legs))
    }

    @Test
    fun exercises_showTheirLastLoggedDate() = runTest {
        val legs = category("Leg day")
        val squat = exercise(legs, "Squat")
        exercise(legs, "Lunge")
        repository.saveLog(squat, null, oct15.minusDays(3), LoadUnit.LBS, double(130.0, 8))
        repository.saveLog(squat, null, oct15, LoadUnit.LBS, double(135.0, 8))

        val lastLogged = repository.observeExercises(legs).first().associate { it.exercise.name to it.lastLogged }

        assertEquals(mapOf("Squat" to oct15, "Lunge" to null), lastLogged)
    }

    // Exercise logs

    @Test
    fun savingForADateThatHasALog_replacesIt() = runTest {
        val legs = category("Leg day")
        val squat = exercise(legs, "Squat")
        repository.saveLog(squat, null, oct15, LoadUnit.LBS, double(135.0, 8))

        repository.saveLog(squat, null, oct15, LoadUnit.KG, double(60.0, 6))

        val logs = repository.observeAllLogs(squat).first()
        assertEquals(1, logs.size)
        assertEquals(LoadUnit.KG, logs.single().unit)
        assertEquals(double(60.0, 6), logs.single().topSets)
    }

    @Test
    fun editingALogsDate_movesIt() = runTest {
        val legs = category("Leg day")
        val squat = exercise(legs, "Squat")
        repository.saveLog(squat, null, oct15, LoadUnit.LBS, double(135.0, 8))
        val original = repository.getLog(squat, oct15)!!

        repository.saveLog(squat, original.id, oct15.minusDays(1), LoadUnit.LBS, double(135.0, 8))

        assertNull(repository.getLog(squat, oct15))
        assertEquals(original.id, repository.getLog(squat, oct15.minusDays(1))!!.id)
    }

    @Test
    fun singleSidedLogs_keepEachSide_includingASkippedOne() = runTest {
        val legs = category("Leg day")
        val lunge = exercise(legs, "Lunge", Sidedness.SINGLE)
        val sides = TopSets.PerSide(TopSet(20.0, 10), TopSet(22.5, 8))
        repository.saveLog(lunge, null, oct15, LoadUnit.LBS, sides)
        repository.saveLog(lunge, null, oct15.plusDays(1), LoadUnit.LBS, TopSets.PerSide(null, TopSet(22.5, 8)))

        assertEquals(sides, repository.getLog(lunge, oct15)!!.topSets)
        assertEquals(TopSets.PerSide(null, TopSet(22.5, 8)), repository.getLog(lunge, oct15.plusDays(1))!!.topSets)
    }

    @Test
    fun recentLogs_areTheLatestTen_newestFirst() = runTest {
        val legs = category("Leg day")
        val squat = exercise(legs, "Squat")
        // Saved out of order to make sure ordering comes from the date.
        for (day in (0L until 12L).shuffled()) {
            repository.saveLog(squat, null, oct15.minusDays(day), LoadUnit.LBS, double(100.0 + day, 5))
        }

        val recent = repository.observeRecentLogs(squat).first().map { it.date }

        assertEquals((0L until 10L).map { oct15.minusDays(it) }, recent)
        assertEquals(12, repository.observeAllLogs(squat).first().size)
    }

    @Test
    fun deletingALog_removesOnlyThatLog() = runTest {
        val legs = category("Leg day")
        val squat = exercise(legs, "Squat")
        repository.saveLog(squat, null, oct15, LoadUnit.LBS, double(135.0, 8))
        repository.saveLog(squat, null, oct15.minusDays(2), LoadUnit.LBS, double(130.0, 8))

        repository.deleteLog(repository.getLog(squat, oct15)!!.id)

        assertEquals(listOf(oct15.minusDays(2)), repository.observeAllLogs(squat).first().map { it.date })
    }
}
