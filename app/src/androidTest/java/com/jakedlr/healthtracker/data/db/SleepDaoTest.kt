package com.jakedlr.healthtracker.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jakedlr.healthtracker.data.SaveSleepResult
import com.jakedlr.healthtracker.data.SleepRepository
import com.jakedlr.healthtracker.domain.SleepError
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SleepDaoTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    private lateinit var db: HealthDatabase
    private lateinit var dao: SleepDao
    private lateinit var repository: SleepRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HealthDatabase::class.java,
        ).build()
        dao = db.sleepDao()
        repository = SleepRepository(db, dao)
    }

    @After
    fun tearDown() = db.close()

    private fun at(d: Int, h: Int, mi: Int = 0): Long =
        LocalDateTime.of(2026, 10, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun overlappingEntry_isRejected_andNamesTheConflict() = runTest {
        val first = repository.addSleep(at(8, 23), at(9, 7)) as SaveSleepResult.Saved

        val result = repository.addSleep(at(9, 6), at(9, 8))

        assertTrue(result is SaveSleepResult.Overlaps)
        assertEquals(first.id, (result as SaveSleepResult.Overlaps).existing.id)
        assertEquals(1, dao.observeStartingBetween(0, Long.MAX_VALUE).first().size)
    }

    @Test
    fun touchingEntries_areBothSaved() = runTest {
        assertTrue(repository.addSleep(at(8, 23), at(9, 7)) is SaveSleepResult.Saved)
        assertTrue(repository.addSleep(at(9, 7), at(9, 8)) is SaveSleepResult.Saved)
    }

    @Test
    fun invalidEntries_areRejected_withoutTouchingTheTable() = runTest {
        assertEquals(SaveSleepResult.Invalid(SleepError.END_NOT_AFTER_START), repository.addSleep(at(9, 7), at(9, 7)))
        assertEquals(SaveSleepResult.Invalid(SleepError.TOO_LONG), repository.addSleep(at(8, 7), at(9, 8)))
        assertTrue(dao.observeStartingBetween(0, Long.MAX_VALUE).first().isEmpty())
    }

    @Test
    fun findOverlapping_canExcludeAnEntry() = runTest {
        val id = (repository.addSleep(at(8, 23), at(9, 7)) as SaveSleepResult.Saved).id
        // Editing the same entry later must not conflict with itself.
        assertNull(dao.findOverlapping(at(8, 22), at(9, 6), excludeId = id))
        assertEquals(id, dao.findOverlapping(at(8, 22), at(9, 6), excludeId = 0)?.id)
    }

    @Test
    fun sleepForDate_followsAttributionRule() = runTest {
        repository.addSleep(at(8, 23), at(9, 7)) // -> 10/08
        repository.addSleep(at(9, 14), at(9, 15)) // nap -> 10/09
        repository.addSleep(at(9, 11, 59), at(9, 12, 30)) // just before noon -> 10/08
        repository.addSleep(at(9, 23, 30), at(10, 6)) // -> 10/09

        val oct8 = repository.observeSleepForDate(LocalDate.of(2026, 10, 8), zone).first()
        val oct9 = repository.observeSleepForDate(LocalDate.of(2026, 10, 9), zone).first()

        assertEquals(listOf(at(8, 23), at(9, 11, 59)), oct8.map { it.startAt })
        assertEquals(listOf(at(9, 14), at(9, 23, 30)), oct9.map { it.startAt })
    }

    @Test
    fun startAtRange_reportsEarliestAndLatestStart() = runTest {
        assertNull(dao.observeStartAtRange().first().first)
        repository.addSleep(at(9, 23), at(10, 7))
        repository.addSleep(at(5, 23), at(6, 7))
        val range = dao.observeStartAtRange().first()
        assertEquals(at(5, 23), range.first)
        assertEquals(at(9, 23), range.last)
    }

    @Test
    fun editingAnEntry_doesNotConflictWithItself() = runTest {
        val id = (repository.addSleep(at(8, 23), at(9, 7)) as SaveSleepResult.Saved).id

        assertEquals(SaveSleepResult.Saved(id), repository.updateSleep(id, at(8, 22, 30), at(9, 6, 30)))
        assertEquals(SleepEntry(id, at(8, 22, 30), at(9, 6, 30)), dao.get(id))
    }

    @Test
    fun editingIntoAnotherEntry_isRejected_andLeavesBothUnchanged() = runTest {
        val night = (repository.addSleep(at(8, 23), at(9, 7)) as SaveSleepResult.Saved).id
        val nap = (repository.addSleep(at(9, 14), at(9, 15)) as SaveSleepResult.Saved).id

        val result = repository.updateSleep(nap, at(9, 6), at(9, 9))

        assertEquals(night, (result as SaveSleepResult.Overlaps).existing.id)
        assertEquals(SleepEntry(nap, at(9, 14), at(9, 15)), dao.get(nap))
    }

    @Test
    fun editingWithInvalidTimes_isRejected() = runTest {
        val id = (repository.addSleep(at(8, 23), at(9, 7)) as SaveSleepResult.Saved).id
        assertEquals(SaveSleepResult.Invalid(SleepError.END_NOT_AFTER_START), repository.updateSleep(id, at(9, 7), at(8, 23)))
        assertEquals(SleepEntry(id, at(8, 23), at(9, 7)), dao.get(id))
    }

    @Test
    fun deleteSleep_removesTheEntry() = runTest {
        val id = (repository.addSleep(at(8, 23), at(9, 7)) as SaveSleepResult.Saved).id
        repository.deleteSleep(id)
        assertNull(dao.get(id))
    }
}
