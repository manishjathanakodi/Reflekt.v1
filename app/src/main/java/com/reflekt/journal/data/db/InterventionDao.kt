package com.reflekt.journal.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(intervention: Intervention)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(intervention: Intervention)

    @Delete
    suspend fun delete(intervention: Intervention)

    @Query("SELECT * FROM intervention ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Intervention>>

    @Query("SELECT * FROM intervention WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Intervention?

    @Query("SELECT * FROM intervention WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPending(): Flow<List<Intervention>>

    @Query("SELECT * FROM intervention WHERE status = 'ACTIVE' ORDER BY timestamp DESC")
    fun getActive(): Flow<List<Intervention>>

    @Query("DELETE FROM intervention WHERE packageName = :packageName AND status = 'ACTIVE'")
    suspend fun deleteActiveByPackage(packageName: String)
}
