package com.reflekt.journal.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MoodLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: MoodLog)

    @Delete
    suspend fun delete(log: MoodLog)

    @Query("SELECT * FROM mood_log ORDER BY date DESC")
    fun getAll(): Flow<List<MoodLog>>

    @Query("SELECT * FROM mood_log WHERE logId = :logId LIMIT 1")
    suspend fun getById(logId: String): MoodLog?

    /** Returns mood logs for the last [n] calendar days, newest first. */
    @Query("SELECT * FROM mood_log WHERE date >= date('now', '-' || :n || ' days') ORDER BY date DESC")
    fun getLastNDays(n: Int): Flow<List<MoodLog>>
}
