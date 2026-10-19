package com.jakedlr.healthtracker.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jakedlr.healthtracker.data.db.FoodItem
import com.jakedlr.healthtracker.data.db.HealthDatabase
import com.jakedlr.healthtracker.data.db.WeightEntry
import com.jakedlr.healthtracker.domain.LoadUnit
import com.jakedlr.healthtracker.domain.Sidedness
import com.jakedlr.healthtracker.domain.TopSet
import com.jakedlr.healthtracker.domain.TopSets
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone = ZoneId.of("America/Los_Angeles")
    private val names = listOf("backup-src.db", "backup-dst.db")
    private val exportFile = File(context.cacheDir, "backup-test-export.db")

    /** Stands in for the Application: owns one named database and can close/reopen it. */
    private class TestHolder(private val context: Context, private val name: String) : DatabaseHolder {
        override var database: HealthDatabase = HealthDatabase.build(context, name)
            private set

        override fun closeDatabase() = database.close()

        override fun reopenDatabase() {
            database = HealthDatabase.build(context, name)
        }
    }

    private lateinit var src: TestHolder
    private lateinit var dst: TestHolder

    private fun repo(holder: TestHolder, name: String) = BackupRepository(context, name, holder, zone)

    @Before
    fun setUp() {
        names.forEach { context.deleteDatabase(it) }
        exportFile.delete()
        src = TestHolder(context, names[0])
        dst = TestHolder(context, names[1])
    }

    @After
    fun tearDown() {
        src.closeDatabase()
        dst.closeDatabase()
        names.forEach { context.deleteDatabase(it) }
        exportFile.delete()
    }

    private fun at(d: Int, h: Int): Long = LocalDateTime.of(2026, 10, d, h, 0).atZone(zone).toInstant().toEpochMilli()

    private suspend fun fill(holder: TestHolder) {
        val db = holder.database
        db.weightDao().upsert(WeightEntry(LocalDate.of(2026, 10, 1), 182.4))
        db.weightDao().upsert(WeightEntry(LocalDate.of(2026, 10, 2), 182.0))
        val foodRepo = FoodRepository(db, db.foodDao())
        val id = db.foodDao().insertItem(
            FoodItem(name = "Oats", barcode = "4006381333931", caloriesKcal = 150.0, proteinG = 5.0, carbsG = 27.0, fiberG = 4.0, fatG = 3.0),
        )
        foodRepo.logFood(db.foodDao().getItem(id)!!, 1.5, at(1, 8))
        SleepRepository(db, db.sleepDao()).addSleep(at(1, 23), at(2, 7))
        val training = TrainingRepository(db, db.trainingDao())
        val back = (training.createCategory("Back day") as TrainingSaveResult.Saved).id
        val legs = (training.createCategory("Leg day") as TrainingSaveResult.Saved).id
        training.reorderCategories(listOf(legs, back))
        val lunge = (training.createExercise(legs, "Lunge", Sidedness.SINGLE, LoadUnit.LBS) as TrainingSaveResult.Saved).id
        training.saveLog(lunge, null, LocalDate.of(2026, 10, 1), LoadUnit.LBS, TopSets.PerSide(TopSet(20.0, 10), TopSet(22.5, 8)))
    }

    private fun fileWithSql(name: String, version: Int, vararg sql: String): Uri {
        val file = File(context.cacheDir, name).apply { delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            sql.forEach(db::execSQL)
            db.version = version
        }
        return Uri.fromFile(file)
    }

    @Test
    fun exportThenImport_reproducesEveryRow() = runBlocking {
        fill(src)
        val exported = repo(src, names[0]).export(Uri.fromFile(exportFile))
        assertEquals(2L, exported.weighIns)
        assertEquals(1L, exported.foodLogs)
        assertEquals(1L, exported.sleepEntries)
        assertEquals(1L, exported.exerciseLogs)
        assertEquals(LocalDate.of(2026, 10, 1), exported.firstDay)

        val target = repo(dst, names[1])
        val inspected = target.inspect(Uri.fromFile(exportFile)) as InspectResult.Valid
        assertEquals(exported, inspected.summary)
        assertEquals(ImportResult.Imported, target.replaceWith(inspected))

        val a = src.database
        val b = dst.database
        assertEquals(a.weightDao().observeAll().first(), b.weightDao().observeAll().first())
        assertEquals(a.foodDao().searchItems("%").first(), b.foodDao().searchItems("%").first())
        assertEquals(a.foodDao().observeLogsBetween(0, Long.MAX_VALUE).first(), b.foodDao().observeLogsBetween(0, Long.MAX_VALUE).first())
        assertEquals(a.sleepDao().observeStartingBetween(0, Long.MAX_VALUE).first(), b.sleepDao().observeStartingBetween(0, Long.MAX_VALUE).first())
        val categories = b.trainingDao().observeCategories().first()
        assertEquals(listOf("Leg day", "Back day"), categories.map { it.name })
        assertEquals(a.trainingDao().observeCategories().first(), categories)
        val exercises = a.trainingDao().observeExercises(categories[0].id).first()
        assertEquals(exercises, b.trainingDao().observeExercises(categories[0].id).first())
        val lunge = exercises.single().exercise.id
        assertEquals(a.trainingDao().observeAllLogs(lunge).first(), b.trainingDao().observeAllLogs(lunge).first())
    }

    @Test
    fun exportDuringWrites_producesAValidCompleteSnapshot() = runBlocking(Dispatchers.IO) {
        fill(src)
        val writer = async {
            (1..300).forEach { src.database.weightDao().upsert(WeightEntry(LocalDate.of(2020, 1, 1).plusDays(it.toLong()), 170.0)) }
        }
        val exported = repo(src, names[0]).export(Uri.fromFile(exportFile))
        writer.await()

        val inspected = repo(dst, names[1]).inspect(Uri.fromFile(exportFile)) as InspectResult.Valid
        assertEquals(exported, inspected.summary)
        assertTrue(exported.weighIns in 2L..302L)
    }

    @Test
    fun nonSqliteFile_isRejected() = runBlocking {
        val file = File(context.cacheDir, "not-a-db.txt").apply { writeText("hello, this is not a database") }
        assertEquals(InspectResult.Invalid(InvalidBackup.NOT_SQLITE), repo(dst, names[1]).inspect(Uri.fromFile(file)))
    }

    @Test
    fun newerSchemaVersion_isRejected() = runBlocking {
        val uri = fileWithSql("newer.db", 99, "CREATE TABLE weight_entry (date INTEGER PRIMARY KEY, weight_lbs REAL)")
        assertEquals(InspectResult.Invalid(InvalidBackup.NEWER_VERSION), repo(dst, names[1]).inspect(uri))
    }

    @Test
    fun missingTables_isRejected() = runBlocking {
        val uri = fileWithSql("missing.db", 3, "CREATE TABLE weight_entry (date INTEGER PRIMARY KEY, weight_lbs REAL)")
        assertEquals(InspectResult.Invalid(InvalidBackup.MISSING_TABLES), repo(dst, names[1]).inspect(uri))
    }

    @Test
    fun version4FileWithoutTrainingTables_isRejected() = runBlocking {
        val uri = fileWithSql(
            "missing-training.db", 4,
            "CREATE TABLE weight_entry (date INTEGER PRIMARY KEY, weight_lbs REAL)",
            "CREATE TABLE food_item (id INTEGER PRIMARY KEY)",
            "CREATE TABLE food_log (id INTEGER PRIMARY KEY)",
            "CREATE TABLE sleep_entry (id INTEGER PRIMARY KEY)",
        )
        assertEquals(InspectResult.Invalid(InvalidBackup.MISSING_TABLES), repo(dst, names[1]).inspect(uri))
    }

    @Test
    fun version1File_importsAndMigratesToCurrent() = runBlocking {
        val uri = fileWithSql(
            "v1.db",
            1,
            "CREATE TABLE IF NOT EXISTS `weight_entry` (`date` INTEGER NOT NULL, `weight_lbs` REAL NOT NULL, PRIMARY KEY(`date`))",
            "INSERT INTO weight_entry (date, weight_lbs) VALUES (20742, 182.4)",
        )
        val target = repo(dst, names[1])
        val inspected = target.inspect(uri) as InspectResult.Valid
        assertEquals(1, inspected.schemaVersion)

        assertEquals(ImportResult.Imported, target.replaceWith(inspected))
        assertEquals(listOf(WeightEntry(LocalDate.ofEpochDay(20742), 182.4)), dst.database.weightDao().observeAll().first())
        // Migrated: the newer tables exist and work.
        assertTrue(dst.database.foodDao().observeLogsBetween(0, Long.MAX_VALUE).first().isEmpty())
        assertTrue(dst.database.sleepDao().observeStartingBetween(0, Long.MAX_VALUE).first().isEmpty())
    }

    @Test
    fun fileThatFailsToOpen_restoresThePreviousData() = runBlocking {
        fill(dst)
        val before = dst.database.weightDao().observeAll().first()
        // Passes the file checks (version 3, tables present) but doesn't match Room's schema.
        val uri = fileWithSql(
            "bad-schema.db",
            3,
            "CREATE TABLE weight_entry (date INTEGER)",
            "CREATE TABLE food_item (id INTEGER)",
            "CREATE TABLE food_log (eaten_at INTEGER)",
            "CREATE TABLE sleep_entry (start_at INTEGER)",
        )
        val target = repo(dst, names[1])
        val inspected = target.inspect(uri) as InspectResult.Valid

        assertEquals(ImportResult.FailedAndRestored, target.replaceWith(inspected))
        assertEquals(before, dst.database.weightDao().observeAll().first())
        assertEquals(1, dst.database.sleepDao().observeStartingBetween(0, Long.MAX_VALUE).first().size)
    }
}
