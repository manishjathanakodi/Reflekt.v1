package com.reflekt.journal.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resource: Resource)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(resource: Resource)

    @Delete
    suspend fun delete(resource: Resource)

    @Query("SELECT * FROM resource ORDER BY tier ASC, title ASC")
    fun getAll(): Flow<List<Resource>>

    @Query("SELECT * FROM resource WHERE resourceId = :resourceId LIMIT 1")
    suspend fun getById(resourceId: String): Resource?

    @Query("SELECT * FROM resource WHERE tier = :tier ORDER BY title ASC")
    fun getByTier(tier: Int): Flow<List<Resource>>
}
