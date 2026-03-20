package com.reflekt.journal.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** User-defined habit. */
@Entity(tableName = "habit")
data class Habit(
    @PrimaryKey val habitId: String,
    val title: String,
    val emoji: String,
    /** DAILY | WEEKDAYS | WEEKLY | CUSTOM */
    val frequency: String,
    /** JSON array of day indices [0-6] — non-empty only when frequency == CUSTOM */
    val customDaysJson: String,
    /** "HH:mm" reminder time — null means no reminder */
    val targetTime: String?,
    /** Link to a Goal — null if standalone */
    val goalId: String?,
    /** Hex colour string, e.g. "#C9A96E" */
    val color: String,
    val streak: Int,
    val longestStreak: Int,
    val isArchived: Boolean,
    val createdAt: Long,
)
