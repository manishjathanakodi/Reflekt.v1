package com.reflekt.journal.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AppUsageLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: AppUsageLog)

    @Delete
    suspend fun delete(log: AppUsageLog)

    @Query("SELECT * FROM app_usage_log ORDER BY date DESC, durationMs DESC")
    fun getAll(): Flow<List<AppUsageLog>>

    @Query("SELECT * FROM app_usage_log WHERE logId = :logId LIMIT 1")
    suspend fun getById(logId: String): AppUsageLog?

    @Query("SELECT * FROM app_usage_log WHERE date = :date ORDER BY durationMs DESC")
    fun getByDate(date: String): Flow<List<AppUsageLog>>
}
