package com.reflekt.journal.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Long-term goal. Progress is computed dynamically from linked habits/todos. */
@Entity(tableName = "goal")
data class Goal(
    @PrimaryKey val goalId: String,
    val title: String,
    val description: String?,
    val emoji: String,
    /** YYYY-MM-DD — null means open-ended */
    val targetDate: String?,
    val color: String,
    /** JSON array of milestone objects {id, title, isCompleted} */
    val milestonesJson: String,
    /** ACTIVE | COMPLETED | ARCHIVED */
    val status: String,
    /** 0.0 – 100.0 — recomputed after each habit/todo change */
    val progressPercent: Float,
    val createdAt: Long,
)
