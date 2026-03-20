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
class HabitDaoTest {

    private lateinit var db: ReflektDatabase
    private lateinit var dao: HabitDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReflektDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.habitDao()
    }

    @After
    fun tearDown() = db.close()

    private fun habit(id: String = "h1") = Habit(
        habitId = id, title = "Morning Run", emoji = "🏃", frequency = "DAILY",
        customDaysJson = "[]", targetTime = "07:00", goalId = null,
        color = "#6FA880", streak = 5, longestStreak = 10,
        isArchived = false, createdAt = System.currentTimeMillis(),
    )

    @Test
    fun insertAndGetById() = runTest {
        val h = habit()
        dao.insert(h)
        assertEquals(h, dao.getById(h.habitId))
    }

    @Test
    fun upsertUpdatesHabit() = runTest {
        dao.insert(habit())
        dao.upsert(habit().copy(streak = 6))
        assertEquals(6, dao.getById("h1")?.streak)
    }

    @Test
    fun deleteRemovesHabit() = runTest {
        val h = habit()
        dao.insert(h)
        dao.delete(h)
        assertNull(dao.getById(h.habitId))
    }

    @Test
    fun getAllExcludesArchived() = runTest {
        dao.insert(habit("h1"))
        dao.insert(habit("h2").copy(isArchived = true))
        assertEquals(1, dao.getAll().first().size)
    }
}
