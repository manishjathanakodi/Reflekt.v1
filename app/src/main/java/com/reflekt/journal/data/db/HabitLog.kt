package com.reflekt.journal.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One completion record per habit per day. */
@Entity(
    tableName = "habit_log",
    indices = [Index(value = ["habitId", "date"])],
)
data class HabitLog(
    @PrimaryKey val logId: String,
    val habitId: String,
    /** YYYY-MM-DD */
    val date: String,
    /** COMPLETED | SKIPPED | MISSED | PENDING */
    val status: String,
    val completedViaJournal: Boolean,
    val note: String?,
    /** HAPPY | SAD | etc. — null if mood not captured at completion */
    val moodAtCompletion: String?,
)
