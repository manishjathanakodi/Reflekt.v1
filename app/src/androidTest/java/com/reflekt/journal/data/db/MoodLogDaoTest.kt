package com.reflekt.journal.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoodLogDaoTest {

    private lateinit var db: ReflektDatabase
    private lateinit var dao: MoodLogDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReflektDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.moodLogDao()
    }

    @After
    fun tearDown() = db.close()

    private fun log(id: String = "l1", date: String = "2026-03-19") = MoodLog(
        logId = id, date = date, moodScore = 7f,
        dominantMood = "HAPPY", primaryTrigger = "Exercise",
        screenTimeMs = 1800_000L, entryCount = 2,
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
        dao.upsert(log().copy(moodScore = 9f))
        assertEquals(9f, dao.getById("l1")?.moodScore)
    }

    @Test
    fun deleteRemovesLog() = runTest {
        val l = log()
        dao.insert(l)
        dao.delete(l)
        assertNull(dao.getById(l.logId))
    }

    @Test
    fun getAllReturnsAll() = runTest {
        dao.insert(log("l1", "2026-03-18"))
        dao.insert(log("l2", "2026-03-19"))
        assertEquals(2, dao.getAll().first().size)
    }

    @Test
    fun getLastNDaysReturnsRecent() = runTest {
        // Insert one log for today and one for 400 days ago (outside range)
        dao.insert(log("l1", "2026-03-19"))
        dao.insert(log("l2", "2024-01-01"))
        // Only the recent one should be in the last 30 days
        val recent = dao.getLastNDays(30).first()
        assertEquals(1, recent.size)
        assertEquals("l1", recent[0].logId)
    }
}
