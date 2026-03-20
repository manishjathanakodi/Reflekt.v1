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
class TodoDaoTest {

    private lateinit var db: ReflektDatabase
    private lateinit var dao: TodoDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReflektDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.todoDao()
    }

    @After
    fun tearDown() = db.close()

    private fun todo(id: String = "t1") = Todo(
        todoId = id, title = "Buy groceries", description = null,
        dueDate = "2026-03-20", priority = "MEDIUM", goalId = null,
        isCompleted = false, completedAt = null,
        completedViaJournal = false, isArchived = false,
        createdAt = System.currentTimeMillis(),
    )

    @Test
    fun insertAndGetById() = runTest {
        val t = todo()
        dao.insert(t)
        assertEquals(t, dao.getById(t.todoId))
    }

    @Test
    fun upsertUpdatesTodo() = runTest {
        dao.insert(todo())
        dao.upsert(todo().copy(isCompleted = true, completedAt = 1L))
        assertEquals(true, dao.getById("t1")?.isCompleted)
    }

    @Test
    fun deleteTodo() = runTest {
        val t = todo()
        dao.insert(t)
        dao.delete(t)
        assertNull(dao.getById(t.todoId))
    }

    @Test
    fun getAllExcludesArchived() = runTest {
        dao.insert(todo("t1"))
        dao.insert(todo("t2").copy(isArchived = true))
        assertEquals(1, dao.getAll().first().size)
    }
}
