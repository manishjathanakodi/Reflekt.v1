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
class AppUsageLogDaoTest {

    private lateinit var db: ReflektDatabase
    private lateinit var dao: AppUsageLogDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReflektDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.appUsageLogDao()
    }

    @After
    fun tearDown() = db.close()

    private fun log(id: String = "a1") = AppUsageLog(
        logId = id, date = "2026-03-19", packageName = "com.example.app",
        appLabel = "Example", category = "SOCIAL", durationMs = 7200_000L,
        launchCount = 10, impactScore = 0.7f, isTriggerApp = true,
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
        dao.upsert(log().copy(durationMs = 1000L))
        assertEquals(1000L, dao.getById("a1")?.durationMs)
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
        dao.insert(log("a1"))
        dao.insert(log("a2"))
        assertEquals(2, dao.getAll().first().size)
    }
}
