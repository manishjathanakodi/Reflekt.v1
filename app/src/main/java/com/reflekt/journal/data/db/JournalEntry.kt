package com.reflekt.journal.data.db

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/** Full journal entry record. */
@Entity(tableName = "journal_entry")
data class JournalEntry(
    @PrimaryKey val entryId: String,
    /** Unix epoch ms */
    val timestamp: Long,
    /** Plain-text transcript — mirrored by JournalEntryFts for full-text search */
    val rawText: String,
    /** JSON array of {role, content} chat turns */
    val conversationJson: String,
    val aiSummary: String,
    /** HAPPY | SAD | ANXIOUS | ANGRY | NEUTRAL | FEAR */
    val moodTag: String,
    /** 0.0 – 10.0 */
    val moodScore: Float,
    /** JSON array of trigger strings */
    val triggersJson: String,
    /** 1 | 2 | 3 */
    val triageTier: Int,
    /** JSON clinical summary — non-null only for tier 3 entries */
    val clinicalSummaryJson: String?,
    val totalScreenTimeMs: Long,
    val isDeleted: Boolean,
    // ── Structured journal fields (null = entry was created via chat mode) ────
    val initialMood: String? = null,
    val closingMood: String? = null,
    val affirmation: String? = null,
    val gratitude1: String? = null,
    val gratitude2: String? = null,
    val gratitude3: String? = null,
    val bestPartOfDay: String? = null,
    val challenge: String? = null,
    val tomorrowIntent: String? = null,
    val freeWrite: String? = null,
    val quote: String? = null,
)

/**
 * FTS4 virtual table — mirrors [JournalEntry] so that rawText is full-text searchable.
 * Room automatically creates INSERT / UPDATE / DELETE triggers to keep this in sync.
 */
@Fts4(contentEntity = JournalEntry::class)
@Entity(tableName = "journal_entry_fts")
data class JournalEntryFts(
    val rawText: String,
)
