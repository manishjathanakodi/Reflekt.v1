package com.reflekt.journal.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Crisis / wellbeing resource. Seeded at install time — never user-created. */
@Entity(tableName = "resource")
data class Resource(
    @PrimaryKey val resourceId: String,
    val title: String,
    val description: String,
    /** e.g. "HELPLINE" | "ARTICLE" | "EXERCISE" */
    val category: String,
    /** Condition tag that triggers display, e.g. "CRISIS_TIER_3" | "ANXIOUS" */
    val conditionTag: String,
    val durationMinutes: Int,
    /** 1 | 2 | 3 — mirrors triage tier */
    val tier: Int,
)
