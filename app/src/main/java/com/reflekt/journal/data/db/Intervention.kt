package com.reflekt.journal.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Blocker service intervention record. */
@Entity(tableName = "intervention")
data class Intervention(
    @PrimaryKey val id: String,
    val timestamp: Long,
    /** e.g. "SCREEN_TIME_LIMIT" | "MOOD_TRIAGE" */
    val triggerType: String,
    val actionTaken: String,
    val packageName: String,
    /** BREATHING | JOURNAL_PROMPT | etc. — null if no microtask assigned */
    val microtaskType: String?,
    val microtaskCompleted: Boolean,
    val overrideUsed: Boolean,
    /** PENDING | RESOLVED | OVERRIDDEN */
    val status: String,
    /** Unix epoch ms — null while still PENDING */
    val resolvedAt: Long?,
)
