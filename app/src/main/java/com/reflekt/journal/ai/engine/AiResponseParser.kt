package com.reflekt.journal.ai.engine

import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AiResponseParser"

enum class MoodTag { HAPPY, SAD, ANGRY, NEUTRAL, FEAR, ANXIOUS }

@Serializable
data class JournalAnalysis(
    val summary: String = "",
    val mood: MoodTag = MoodTag.NEUTRAL,
    val moodScore: Float = 3.0f,
    val triggers: List<String> = emptyList(),
    val triageTier: Int = 1,
    val clinicalFlags: List<String> = emptyList(),
)

@Serializable
data class AccountabilityResult(
    val completedHabitIds: List<String> = emptyList(),
    val skippedHabitIds: List<String> = emptyList(),
    val completedTodoIds: List<String> = emptyList(),
    val habitInsight: String = "",
    val encouragement: String = "",
)

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

@Singleton
class AiResponseParser @Inject constructor() {

    fun parseJournalAnalysis(raw: String): JournalAnalysis {
        return try {
            json.decodeFromString(stripMarkdownFences(raw))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse JournalAnalysis: ${e.message}")
            JournalAnalysis()
        }
    }

    fun parseAccountabilityResult(raw: String): AccountabilityResult {
        return try {
            json.decodeFromString(stripMarkdownFences(raw))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse AccountabilityResult: ${e.message}")
            AccountabilityResult()
        }
    }

    companion object {
        /** Removes ```json ... ``` or ``` ... ``` wrappers before parsing. */
        fun stripMarkdownFences(raw: String): String {
            val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(raw.trim())
            return fenced?.groupValues?.get(1)?.trim() ?: raw.trim()
        }
    }
}
