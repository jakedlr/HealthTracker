package com.jakedlr.healthtracker.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeightDaoTest {
    private lateinit var db: HealthDatabase
    private lateinit var dao: WeightDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HealthDatabase::class.java,
        ).build()
        dao = db.weightDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun upsert_sameDate_overwritesInsteadOfAddingRow() = runTest {
        val date = LocalDate.of(2026, 10, 16)
        dao.upsert(WeightEntry(date, 182.0))
        dao.upsert(WeightEntry(date, 181.4))

        val all = dao.observeAll().first()
        assertEquals(listOf(WeightEntry(date, 181.4)), all)
        assertEquals(181.4, dao.getByDate(date)!!.weightLbs, 0.0)
    }

    @Test
    fun observeAll_isSortedByDate() = runTest {
        val dates = listOf(LocalDate.of(2026, 10, 3), LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 15))
        dates.forEach { dao.upsert(WeightEntry(it, 180.0)) }

        assertEquals(dates.sorted(), dao.observeAll().first().map { it.date })
    }

    @Test
    fun delete_removesEntry() = runTest {
        val entry = WeightEntry(LocalDate.of(2026, 10, 16), 182.0)
        dao.upsert(entry)
        dao.delete(entry)

        assertNull(dao.getByDate(entry.date))
    }
}
