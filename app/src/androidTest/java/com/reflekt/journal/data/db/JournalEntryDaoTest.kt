package com.reflekt.journal.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class JournalEntryDaoTest {

    private lateinit var db: ReflektDatabase
    private lateinit var dao: JournalEntryDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReflektDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.journalEntryDao()
    }

    @After
    fun tearDown() = db.close()

    private fun entry(id: String = "e1", rawText: String = "today was good") = JournalEntry(
        entryId = id, timestamp = System.currentTimeMillis(),
        rawText = rawText, conversationJson = "[]",
        aiSummary = "Positive day", moodTag = "HAPPY", moodScore = 8f,
        triggersJson = "[]", triageTier = 1, clinicalSummaryJson = null,
        totalScreenTimeMs = 3600_000L, isDeleted = false,
    )

    @Test
    fun insertAndGetById() = runTest {
        val e = entry()
        dao.insert(e)
        assertEquals(e, dao.getById(e.entryId))
    }

    @Test
    fun upsertUpdatesEntry() = runTest {
        dao.insert(entry())
        dao.upsert(entry().copy(aiSummary = "Updated summary"))
        assertEquals("Updated summary", dao.getById("e1")?.aiSummary)
    }

    @Test
    fun deleteRemovesEntry() = runTest {
        val e = entry()
        dao.insert(e)
        dao.delete(e)
        assertNull(dao.getById(e.entryId))
    }

    @Test
    fun getAllExcludesDeleted() = runTest {
        dao.insert(entry("e1"))
        dao.insert(entry("e2").copy(isDeleted = true))
        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("e1", all[0].entryId)
    }

    @Test
    fun searchEntriesUsingFts() = runTest {
        dao.insert(entry("e1", rawText = "feeling anxious about work"))
        dao.insert(entry("e2", rawText = "had a great walk today"))
        val results = dao.searchEntries("anxious").first()
        assertEquals(1, results.size)
        assertTrue(results[0].rawText.contains("anxious"))
    }
}
