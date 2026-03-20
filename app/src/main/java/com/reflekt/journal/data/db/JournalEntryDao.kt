package com.reflekt.journal.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: JournalEntry)

    @Delete
    suspend fun delete(entry: JournalEntry)

    @Query("SELECT * FROM journal_entry WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAll(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entry WHERE entryId = :entryId LIMIT 1")
    suspend fun getById(entryId: String): JournalEntry?

    // ── Paging sources for HistoryScreen ──────────────────────────────────────

    @Query("SELECT * FROM journal_entry WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllPaged(): PagingSource<Int, JournalEntry>

    @Query("SELECT * FROM journal_entry WHERE moodTag = :moodTag AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllByMoodPaged(moodTag: String): PagingSource<Int, JournalEntry>

    @Query("""
        SELECT je.* FROM journal_entry je
        WHERE je.rowid IN (
            SELECT rowid FROM journal_entry_fts
            WHERE journal_entry_fts MATCH :query
        )
        AND je.isDeleted = 0
        ORDER BY je.timestamp DESC
    """)
    fun searchEntriesPaged(query: String): PagingSource<Int, JournalEntry>

    @Query("""
        SELECT je.* FROM journal_entry je
        WHERE je.rowid IN (
            SELECT rowid FROM journal_entry_fts
            WHERE journal_entry_fts MATCH :query
        )
        AND je.moodTag = :moodTag
        AND je.isDeleted = 0
        ORDER BY je.timestamp DESC
    """)
    fun searchEntriesByMoodPaged(query: String, moodTag: String): PagingSource<Int, JournalEntry>

    /**
     * Full-text search over rawText using the FTS4 virtual table.
     * Results are ordered newest-first.
     */
    @Query("""
        SELECT je.* FROM journal_entry je
        WHERE je.rowid IN (
            SELECT rowid FROM journal_entry_fts
            WHERE journal_entry_fts MATCH :query
        )
        AND je.isDeleted = 0
        ORDER BY je.timestamp DESC
    """)
    fun searchEntries(query: String): Flow<List<JournalEntry>>

    /** Soft-delete: sets isDeleted = 1 without removing the row. */
    @Query("UPDATE journal_entry SET isDeleted = 1 WHERE entryId = :entryId")
    suspend fun softDelete(entryId: String)

    /** All non-deleted entries after [since] timestamp — for analytics period filtering. */
    @Query("SELECT * FROM journal_entry WHERE timestamp >= :since AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllAfter(since: Long): Flow<List<JournalEntry>>

    /** All entries within a calendar day — used to recalculate today's MoodLog aggregate. */
    @Query("SELECT * FROM journal_entry WHERE timestamp >= :startOfDay AND timestamp < :endOfDay AND isDeleted = 0")
    suspend fun getEntriesForDay(startOfDay: Long, endOfDay: Long): List<JournalEntry>
}
