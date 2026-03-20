package com.reflekt.journal.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: Todo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(todo: Todo)

    @Delete
    suspend fun delete(todo: Todo)

    @Query("SELECT * FROM todo WHERE isArchived = 0 ORDER BY dueDate ASC, priority ASC")
    fun getAll(): Flow<List<Todo>>

    @Query("SELECT * FROM todo WHERE todoId = :todoId LIMIT 1")
    suspend fun getById(todoId: String): Todo?

    @Query(
        """
        SELECT * FROM todo
        WHERE isArchived = 0
          AND isCompleted = 0
          AND (dueDate IS NULL OR dueDate <= date('now'))
        ORDER BY priority ASC
        """
    )
    fun getOverdue(): Flow<List<Todo>>

    @Query("SELECT * FROM todo WHERE goalId = :goalId AND isArchived = 0 ORDER BY dueDate ASC")
    fun getByGoal(goalId: String): Flow<List<Todo>>
}
