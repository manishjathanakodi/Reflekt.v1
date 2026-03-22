package com.reflekt.journal.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** User-defined per-app screen time limit. */
@Entity(tableName = "manual_app_limit")
data class ManualAppLimit(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val limitMinutes: Int,
    val autoBlock: Boolean,
    val requireMicrotask: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
)
