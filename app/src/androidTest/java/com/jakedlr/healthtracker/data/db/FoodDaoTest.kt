package com.jakedlr.healthtracker.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jakedlr.healthtracker.data.FoodRepository
import com.jakedlr.healthtracker.data.SaveItemResult
import com.jakedlr.healthtracker.domain.Nutrition
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodDaoTest {
    private lateinit var db: HealthDatabase
    private lateinit var dao: FoodDao
    private lateinit var repository: FoodRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HealthDatabase::class.java,
        ).build()
        dao = db.foodDao()
        repository = FoodRepository(db, dao)
    }

    @After
    fun tearDown() = db.close()

    private fun item(name: String, barcode: String? = null, kcal: Double = 100.0) = FoodItem(
        name = name, barcode = barcode,
        caloriesKcal = kcal, proteinG = 1.0, carbsG = 2.0, fiberG = 3.0, fatG = 4.0,
    )

    @Test
    fun editingItem_leavesExistingLogsUnchanged() = runTest {
        val id = dao.insertItem(item("Oatmeal", kcal = 150.0))
        repository.logFood(dao.getItem(id)!!, servings = 2.0, eatenAt = 1_000)
        val logBefore = dao.observeLogsBetween(0, Long.MAX_VALUE).first().single()

        val result = repository.updateItem(
            dao.getItem(id)!!.copy(name = "Oatmeal v2").withNutrition(Nutrition(400.0, 9.0, 9.0, 9.0, 9.0)),
        )

        assertEquals(SaveItemResult.Saved(id), result)
        assertEquals("Oatmeal v2", dao.getItem(id)!!.name)
        val logAfter = dao.observeLogsBetween(0, Long.MAX_VALUE).first().single()
        assertEquals(logBefore, logAfter)
        assertEquals("Oatmeal", logAfter.name)
        assertEquals(300.0, logAfter.toLoggedFood().total.caloriesKcal, 0.0)
    }

    @Test
    fun duplicateNameDifferentCase_isRejected() = runTest {
        dao.insertItem(item("Banana"))
        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { dao.insertItem(item("banana")) }
        }
        val result = repository.createItemAndLog(item("BANANA"), servings = 1.0, eatenAt = 0)
        assertTrue(result is SaveItemResult.NameTaken)
        assertEquals("Banana", (result as SaveItemResult.NameTaken).existing.name)
        // Neither the item nor the log was written.
        assertTrue(dao.observeLogsBetween(0, Long.MAX_VALUE).first().isEmpty())
    }

    @Test
    fun renamingToAnotherItemsName_isRejected_butKeepingOwnNameIsFine() = runTest {
        val appleId = dao.insertItem(item("Apple"))
        val pearId = dao.insertItem(item("Pear"))

        val clash = repository.updateItem(dao.getItem(pearId)!!.copy(name = "APPLE"))
        assertEquals(SaveItemResult.NameTaken(dao.getItem(appleId)!!), clash)

        val recase = repository.updateItem(dao.getItem(pearId)!!.copy(name = "pear"))
        assertEquals(SaveItemResult.Saved(pearId), recase)
    }

    @Test
    fun barcode_isUniqueWhenPresent_nullsAllowed() = runTest {
        dao.insertItem(item("A"))
        dao.insertItem(item("B"))
        dao.insertItem(item("C", barcode = "0123456789012"))
        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { dao.insertItem(item("D", barcode = "0123456789012")) }
        }
    }

    @Test
    fun deletingItem_keepsLogs_andClearsTheLink() = runTest {
        val id = dao.insertItem(item("Toast"))
        repository.logFood(dao.getItem(id)!!, servings = 1.0, eatenAt = 1_000)

        repository.deleteItem(dao.getItem(id)!!)

        assertNull(dao.getItem(id))
        val log = dao.observeLogsBetween(0, Long.MAX_VALUE).first().single()
        assertNull(log.foodItemId)
        assertEquals("Toast", log.name)
        assertEquals(100.0, log.caloriesKcal, 0.0)
    }

    @Test
    fun search_isCaseInsensitive_escapesWildcards_andPutsRecentFirst() = runTest {
        val apple = dao.insertItem(item("Apple pie"))
        dao.insertItem(item("Pineapple"))
        dao.insertItem(item("Banana"))
        val pineapple = dao.getItemByName("pineapple")!!
        repository.logFood(pineapple, 1.0, eatenAt = 5_000)
        repository.logFood(dao.getItem(apple)!!, 1.0, eatenAt = 1_000)

        assertEquals(listOf("Pineapple", "Apple pie"), repository.searchItems("APPLE").first().map { it.name })
        assertTrue(repository.searchItems("%").first().isEmpty())
        // Logged items first (most recent first), then the rest alphabetically.
        assertEquals(listOf("Pineapple", "Apple pie", "Banana"), repository.searchItems("").first().map { it.name })
    }

    @Test
    fun logsForDay_useLocalDayBoundaries() = runTest {
        val zone = ZoneId.of("America/Los_Angeles")
        val day = LocalDate.of(2026, 10, 16)
        val id = dao.insertItem(item("Snack"))
        val snack = dao.getItem(id)!!
        val startOfDay = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfNext = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        repository.logFood(snack, 1.0, eatenAt = startOfDay - 1)
        repository.logFood(snack, 1.0, eatenAt = startOfDay)
        repository.logFood(snack, 1.0, eatenAt = startOfNext - 1)
        repository.logFood(snack, 1.0, eatenAt = startOfNext)

        val logs = repository.observeLogsForDay(day, zone).first()
        assertEquals(listOf(startOfDay, startOfNext - 1), logs.map { it.eatenAt })
    }
}
