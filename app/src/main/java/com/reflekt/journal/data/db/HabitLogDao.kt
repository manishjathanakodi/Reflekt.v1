package com.reflekt.journal.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: HabitLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: HabitLog)

    @Delete
    suspend fun delete(log: HabitLog)

    @Query("SELECT * FROM habit_log ORDER BY date DESC")
    fun getAll(): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_log WHERE logId = :logId LIMIT 1")
    suspend fun getById(logId: String): HabitLog?

    /** Returns the single log entry for a habit on a given date (YYYY-MM-DD). */
    @Query("SELECT * FROM habit_log WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getByHabitAndDate(habitId: String, date: String): HabitLog?

    /**
     * Returns the count of consecutive COMPLETED days ending today.
     * The repository layer calls this with recent logs and computes the true streak.
     * This query returns all completed logs for the habit ordered newest-first,
     * which the repository iterates until it finds a gap.
     */
    @Query(
        """
        SELECT COUNT(*) FROM habit_log
        WHERE habitId = :habitId
          AND status = 'COMPLETED'
          AND date >= date('now', '-365 days')
        """
    )
    suspend fun getStreakForHabit(habitId: String): Int
}
