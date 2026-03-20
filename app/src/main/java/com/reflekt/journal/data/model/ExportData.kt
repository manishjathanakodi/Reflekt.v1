package com.reflekt.journal.data.model

import kotlinx.serialization.Serializable

/**
 * Root container for the full encrypted backup.
 * All fields use basic Kotlin types so kotlinx.serialization works without
 * modifying the Room entity classes.
 */
@Serializable
data class ExportData(
    val exportedAt: Long,
    val version: Int = 1,
    val userProfile: ExportProfile? = null,
    val journalEntries: List<ExportJournalEntry> = emptyList(),
    val moodLogs: List<ExportMoodLog> = emptyList(),
    val habits: List<ExportHabit> = emptyList(),
    val habitLogs: List<ExportHabitLog> = emptyList(),
    val todos: List<ExportTodo> = emptyList(),
    val goals: List<ExportGoal> = emptyList(),
    val appUsageLogs: List<ExportAppUsageLog> = emptyList(),
    val interventions: List<ExportIntervention> = emptyList(),
)

@Serializable
data class ExportProfile(
    val uid: String,
    val name: String,
    val age: Int,
    val gender: String,
    val occupation: String,
    val industry: String,
    val maritalStatus: String,
    val relationMapJson: String,
    val struggleAreasJson: String,
    val screenTimeGoalMinutes: Int,
)

@Serializable
data class ExportJournalEntry(
    val entryId: String,
    val timestamp: Long,
    val rawText: String,
    val conversationJson: String,
    val aiSummary: String,
    val moodTag: String,
    val moodScore: Float,
    val triggersJson: String,
    val triageTier: Int,
    val clinicalSummaryJson: String?,
    val totalScreenTimeMs: Long,
)

@Serializable
data class ExportMoodLog(
    val logId: String,
    val date: String,
    val moodScore: Float,
    val dominantMood: String,
    val primaryTrigger: String,
    val screenTimeMs: Long,
    val entryCount: Int,
)

@Serializable
data class ExportHabit(
    val habitId: String,
    val title: String,
    val emoji: String,
    val frequency: String,
    val customDaysJson: String,
    val targetTime: String?,
    val goalId: String?,
    val color: String,
    val streak: Int,
    val longestStreak: Int,
    val isArchived: Boolean,
    val createdAt: Long,
)

@Serializable
data class ExportHabitLog(
    val logId: String,
    val habitId: String,
    val date: String,
    val status: String,
    val completedViaJournal: Boolean,
    val note: String?,
    val moodAtCompletion: String?,
)

@Serializable
data class ExportTodo(
    val todoId: String,
    val title: String,
    val description: String?,
    val dueDate: String?,
    val priority: String,
    val goalId: String?,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val completedViaJournal: Boolean,
    val isArchived: Boolean,
    val createdAt: Long,
)

@Serializable
data class ExportGoal(
    val goalId: String,
    val title: String,
    val description: String?,
    val emoji: String,
    val targetDate: String?,
    val color: String,
    val milestonesJson: String,
    val status: String,
    val progressPercent: Float,
    val createdAt: Long,
)

@Serializable
data class ExportAppUsageLog(
    val logId: String,
    val date: String,
    val packageName: String,
    val appLabel: String,
    val category: String,
    val durationMs: Long,
    val launchCount: Int,
    val impactScore: Float,
    val isTriggerApp: Boolean,
)

@Serializable
data class ExportIntervention(
    val id: String,
    val timestamp: Long,
    val triggerType: String,
    val actionTaken: String,
    val packageName: String,
    val microtaskType: String?,
    val microtaskCompleted: Boolean,
    val overrideUsed: Boolean,
    val status: String,
    val resolvedAt: Long?,
)
