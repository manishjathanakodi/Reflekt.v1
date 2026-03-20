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
class ResourceDaoTest {

    private lateinit var db: ReflektDatabase
    private lateinit var dao: ResourceDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReflektDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.resourceDao()
    }

    @After
    fun tearDown() = db.close()

    private fun resource(id: String = "r1", tier: Int = 1) = Resource(
        resourceId = id, title = "Crisis Helpline",
        description = "Call 988 for immediate support",
        category = "HELPLINE", conditionTag = "CRISIS_TIER_3",
        durationMinutes = 0, tier = tier,
    )

    @Test
    fun insertAndGetById() = runTest {
        val r = resource()
        dao.insert(r)
        assertEquals(r, dao.getById(r.resourceId))
    }

    @Test
    fun upsertUpdatesResource() = runTest {
        dao.insert(resource())
        dao.upsert(resource().copy(title = "Updated Title"))
        assertEquals("Updated Title", dao.getById("r1")?.title)
    }

    @Test
    fun deleteResource() = runTest {
        val r = resource()
        dao.insert(r)
        dao.delete(r)
        assertNull(dao.getById(r.resourceId))
    }

    @Test
    fun getAllReturnsAll() = runTest {
        dao.insert(resource("r1", tier = 1))
        dao.insert(resource("r2", tier = 3))
        assertEquals(2, dao.getAll().first().size)
    }

    @Test
    fun getByTierFiltersCorrectly() = runTest {
        dao.insert(resource("r1", tier = 1))
        dao.insert(resource("r2", tier = 3))
        val tier3 = dao.getByTier(3).first()
        assertEquals(1, tier3.size)
        assertEquals("r2", tier3[0].resourceId)
    }
}
