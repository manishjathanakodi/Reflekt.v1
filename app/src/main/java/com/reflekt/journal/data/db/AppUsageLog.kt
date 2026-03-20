package com.reflekt.journal.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Per-app daily usage record. Populated by UsageStatsWorker. */
@Entity(
    tableName = "app_usage_log",
    indices = [Index(value = ["date", "packageName"])],
)
data class AppUsageLog(
    @PrimaryKey val logId: String,
    /** YYYY-MM-DD */
    val date: String,
    val packageName: String,
    val appLabel: String,
    val category: String,
    /** Total foreground duration in milliseconds */
    val durationMs: Long,
    val launchCount: Int,
    /** 0.0 – 1.0 — correlation with negative mood outcomes */
    val impactScore: Float,
    val isTriggerApp: Boolean,
)
