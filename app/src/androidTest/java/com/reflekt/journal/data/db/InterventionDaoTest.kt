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
class InterventionDaoTest {

    private lateinit var db: ReflektDatabase
    private lateinit var dao: InterventionDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReflektDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.interventionDao()
    }

    @After
    fun tearDown() = db.close()

    private fun intervention(id: String = "i1") = Intervention(
        id = id, timestamp = System.currentTimeMillis(),
        triggerType = "SCREEN_TIME_LIMIT", actionTaken = "BLOCKED",
        packageName = "com.example.app", microtaskType = "BREATHING",
        microtaskCompleted = false, overrideUsed = false,
        status = "PENDING", resolvedAt = null,
    )

    @Test
    fun insertAndGetById() = runTest {
        val i = intervention()
        dao.insert(i)
        assertEquals(i, dao.getById(i.id))
    }

    @Test
    fun upsertUpdatesRecord() = runTest {
        dao.insert(intervention())
        dao.upsert(intervention().copy(status = "RESOLVED", resolvedAt = 12345L))
        assertEquals("RESOLVED", dao.getById("i1")?.status)
    }

    @Test
    fun deleteRemovesRecord() = runTest {
        val i = intervention()
        dao.insert(i)
        dao.delete(i)
        assertNull(dao.getById(i.id))
    }

    @Test
    fun getAllReturnsInserted() = runTest {
        dao.insert(intervention("i1"))
        dao.insert(intervention("i2"))
        assertEquals(2, dao.getAll().first().size)
    }

    @Test
    fun getPendingFiltersCorrectly() = runTest {
        dao.insert(intervention("i1").copy(status = "PENDING"))
        dao.insert(intervention("i2").copy(status = "RESOLVED", resolvedAt = 1L))
        val pending = dao.getPending().first()
        assertEquals(1, pending.size)
        assertEquals("i1", pending[0].id)
    }
}
