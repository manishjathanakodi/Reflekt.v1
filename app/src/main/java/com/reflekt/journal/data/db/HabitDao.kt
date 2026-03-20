package com.reflekt.journal.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: Habit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)

    @Query("SELECT * FROM habit WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getAll(): Flow<List<Habit>>

    @Query("SELECT * FROM habit WHERE habitId = :habitId LIMIT 1")
    suspend fun getById(habitId: String): Habit?

    @Query("SELECT * FROM habit WHERE goalId = :goalId AND isArchived = 0")
    fun getByGoal(goalId: String): Flow<List<Habit>>
}
