package com.reflekt.journal.ai.prompt

import com.reflekt.journal.data.db.Goal
import com.reflekt.journal.data.db.Habit
import com.reflekt.journal.data.db.Todo
import com.reflekt.journal.data.db.UserProfile
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptBuilder @Inject constructor() {

    /**
     * Section 7.2 — System prompt with injected user profile.
     */
    fun buildJournalSystemPrompt(profile: UserProfile): String {
        val relationMapStr = parseRelationMap(profile.relationMapJson)
        return """
            You are Reflekt, a private and empathetic journaling companion.
            User: ${profile.name}, ${profile.age}, ${profile.occupation}, ${profile.industry}.
            Close relationships: $relationMapStr.
            Role: Ask ONE focused follow-up question per turn. After 2-3 turns,
            output ONLY a JSON block (no prose) in this exact format:
            {
              "summary": "2-sentence summary",
              "mood": "HAPPY|SAD|ANGRY|NEUTRAL|FEAR|ANXIOUS",
              "moodScore": 1.0-5.0,
              "triggers": ["trigger1", "trigger2"],
              "triageTier": 1|2|3,
              "clinicalFlags": []
            }
            Never reference external servers or storage.
        """.trimIndent()
    }

    /**
     * Section 7.3 — Accountability context injected after the system prompt.
     */
    fun buildAccountabilityContext(
        habitsDueToday: List<Habit>,
        overdueHabits: List<Habit>,
        todayTodos: List<Todo>,
        activeGoals: List<Goal>,
    ): String {
        val habitsToday = habitsDueToday.joinToString(", ") { "${it.emoji} ${it.title}" }
            .ifEmpty { "none" }
        val overdue = overdueHabits.joinToString(", ") { "${it.emoji} ${it.title}" }
            .ifEmpty { "none" }
        val todos = todayTodos.joinToString(", ") { it.title }
            .ifEmpty { "none" }
        val goals = activeGoals.joinToString(", ") { "${it.emoji} ${it.title}" }
            .ifEmpty { "none" }

        return """
            --- ACCOUNTABILITY CONTEXT (inject after system prompt) ---
            DO NOT list these as a checklist. Weave into conversation naturally.
            If user mentions completing something, acknowledge warmly.
            Habits due today: $habitsToday
            Overdue habits: $overdue
            Todos due today or overdue: $todos
            Active goals: $goals
            --- END ACCOUNTABILITY CONTEXT ---
        """.trimIndent()
    }

    /**
     * Section 7.4 — Post-session accountability parsing prompt.
     */
    fun buildParserPrompt(transcript: String, idMap: Map<String, String>): String {
        val idMapJson = idMap.entries.joinToString(", ", "{", "}") {
            "\"${it.key}\": \"${it.value}\""
        }
        return """
            From this journal transcript, identify habits/todos completed or skipped.
            Output ONLY JSON:
            {
              "completedHabitIds": ["uuid1"],
              "skippedHabitIds": [],
              "completedTodoIds": ["uuid2"],
              "habitInsight": "One sentence linking habit to today mood",
              "encouragement": "One warm sentence"
            }
            Active habit/todo IDs: $idMapJson

            Transcript:
            $transcript
        """.trimIndent()
    }

    private fun parseRelationMap(jsonStr: String): String {
        return try {
            Json.decodeFromString<List<Map<String, String>>>(jsonStr)
                .joinToString(", ") { "${it["emoji"] ?: ""} ${it["name"]} (${it["role"]})" }
                .ifEmpty { "none listed" }
        } catch (e: Exception) {
            jsonStr.ifEmpty { "none listed" }
        }
    }
}
