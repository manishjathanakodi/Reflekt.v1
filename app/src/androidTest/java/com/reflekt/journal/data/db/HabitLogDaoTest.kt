package com.reflekt.journal.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitLogDaoTest {

    private lateinit var db: ReflektDatabase
    private lateinit var dao: HabitLogDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReflektDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.habitLogDao()
    }

    @After
    fun tearDown() = db.close()

    private fun log(id: String = "hl1", habitId: String = "h1", date: String = "2026-03-19") =
        HabitLog(
            logId = id, habitId = habitId, date = date,
            status = "COMPLETED", completedViaJournal = false,
            note = null, moodAtCompletion = "HAPPY",
        )

    @Test
    fun insertAndGetById() = runTest {
        val l = log()
        dao.insert(l)
        assertEquals(l, dao.getById(l.logId))
    }

    @Test
    fun upsertUpdatesLog() = runTest {
        dao.insert(log())
        dao.upsert(log().copy(status = "SKIPPED"))
        assertEquals("SKIPPED", dao.getById("hl1")?.status)
    }

    @Test
    fun deleteRemovesLog() = runTest {
        val l = log()
        dao.insert(l)
        dao.delete(l)
        assertNull(dao.getById(l.logId))
    }

    @Test
    fun getAllReturnsInserted() = runTest {
        dao.insert(log("hl1"))
        dao.insert(log("hl2", date = "2026-03-18"))
        assertEquals(2, dao.getAll().first().size)
    }

    @Test
    fun getByHabitAndDateReturnsCorrectLog() = runTest {
        dao.insert(log("hl1", "h1", "2026-03-19"))
        dao.insert(log("hl2", "h1", "2026-03-18"))
        val result = dao.getByHabitAndDate("h1", "2026-03-19")
        assertNotNull(result)
        assertEquals("hl1", result?.logId)
    }

    @Test
    fun getByHabitAndDateReturnsNullWhenMissing() = runTest {
        assertNull(dao.getByHabitAndDate("h1", "2026-01-01"))
    }

    @Test
    fun getStreakForHabitCountsCompletedLogs() = runTest {
        dao.insert(log("hl1", "h1", "2026-03-17").copy(status = "COMPLETED"))
        dao.insert(log("hl2", "h1", "2026-03-18").copy(status = "COMPLETED"))
        dao.insert(log("hl3", "h1", "2026-03-19").copy(status = "MISSED"))
        // 2 COMPLETED in the last 365 days
        assertEquals(2, dao.getStreakForHabit("h1"))
    }
}
