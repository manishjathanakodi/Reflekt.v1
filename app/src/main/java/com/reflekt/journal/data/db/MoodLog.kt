package com.reflekt.journal.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Aggregated mood summary — one row per calendar day. */
@Entity(
    tableName = "mood_log",
    indices = [Index(value = ["date"], unique = true)],
)
data class MoodLog(
    @PrimaryKey val logId: String,
    /** YYYY-MM-DD — unique constraint enforced above */
    val date: String,
    /** 0.0 – 10.0 */
    val moodScore: Float,
    /** HAPPY | SAD | ANXIOUS | ANGRY | NEUTRAL | FEAR */
    val dominantMood: String,
    val primaryTrigger: String,
    val screenTimeMs: Long,
    /** Number of journal entries that contributed */
    val entryCount: Int,
)
