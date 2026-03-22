package com.reflekt.journal.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ManualAppLimitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(limit: ManualAppLimit)

    @Query("DELETE FROM manual_app_limit WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT * FROM manual_app_limit ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ManualAppLimit>>

    @Query("SELECT * FROM manual_app_limit WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): ManualAppLimit?
}
