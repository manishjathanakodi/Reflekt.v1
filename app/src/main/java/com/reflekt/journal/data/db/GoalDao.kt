package com.reflekt.journal.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: Goal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: Goal)

    @Delete
    suspend fun delete(goal: Goal)

    @Query("SELECT * FROM goal WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Goal>>

    @Query("SELECT * FROM goal WHERE goalId = :goalId LIMIT 1")
    suspend fun getById(goalId: String): Goal?

    @Query("SELECT * FROM goal ORDER BY createdAt DESC")
    fun getAllIncludingArchived(): Flow<List<Goal>>
}
