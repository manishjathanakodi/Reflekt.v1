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
class GoalDaoTest {

    private lateinit var db: ReflektDatabase
    private lateinit var dao: GoalDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReflektDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.goalDao()
    }

    @After
    fun tearDown() = db.close()

    private fun goal(id: String = "g1") = Goal(
        goalId = id, title = "Run a 5K", description = "Training goal",
        emoji = "🏃", targetDate = "2026-06-01", color = "#6FA880",
        milestonesJson = "[]", status = "ACTIVE",
        progressPercent = 0f, createdAt = System.currentTimeMillis(),
    )

    @Test
    fun insertAndGetById() = runTest {
        val g = goal()
        dao.insert(g)
        assertEquals(g, dao.getById(g.goalId))
    }

    @Test
    fun upsertUpdatesGoal() = runTest {
        dao.insert(goal())
        dao.upsert(goal().copy(progressPercent = 50f))
        assertEquals(50f, dao.getById("g1")?.progressPercent)
    }

    @Test
    fun deleteGoal() = runTest {
        val g = goal()
        dao.insert(g)
        dao.delete(g)
        assertNull(dao.getById(g.goalId))
    }

    @Test
    fun getAllReturnsOnlyActive() = runTest {
        dao.insert(goal("g1").copy(status = "ACTIVE"))
        dao.insert(goal("g2").copy(status = "ARCHIVED"))
        assertEquals(1, dao.getAll().first().size)
    }

    @Test
    fun getAllIncludingArchivedReturnsAll() = runTest {
        dao.insert(goal("g1").copy(status = "ACTIVE"))
        dao.insert(goal("g2").copy(status = "ARCHIVED"))
        assertEquals(2, dao.getAllIncludingArchived().first().size)
    }
}
