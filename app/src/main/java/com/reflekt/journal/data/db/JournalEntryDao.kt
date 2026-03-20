package com.reflekt.journal.data.db

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

    /**
     * Full-text search over rawText using the FTS4 virtual table.
     * Results are ordered newest-first.
     */
    @Query(
        """
        SELECT je.* FROM journal_entry je
        WHERE je.rowid IN (
            SELECT rowid FROM journal_entry_fts
            WHERE journal_entry_fts MATCH :query
        )
        AND je.isDeleted = 0
        ORDER BY je.timestamp DESC
        """
    )
    fun searchEntries(query: String): Flow<List<JournalEntry>>
}
