package com.reflekt.journal.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** User task / to-do item. */
@Entity(tableName = "todo")
data class Todo(
    @PrimaryKey val todoId: String,
    val title: String,
    val description: String?,
    /** YYYY-MM-DD — null means no due date */
    val dueDate: String?,
    /** HIGH | MEDIUM | LOW */
    val priority: String,
    /** Linked goal — null if standalone */
    val goalId: String?,
    val isCompleted: Boolean,
    /** Unix epoch ms */
    val completedAt: Long?,
    val completedViaJournal: Boolean,
    val isArchived: Boolean,
    val createdAt: Long,
)
