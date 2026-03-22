package com.reflekt.journal.ai.prompt

import com.reflekt.journal.ai.engine.MoodTag
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
    fun buildJournalSystemPrompt(profile: UserProfile, initialMood: MoodTag? = null): String {
        val relationMapStr = parseRelationMap(profile.relationMapJson)
        val moodContext = if (initialMood != null)
            "\nThe user started this session feeling ${initialMood.name.lowercase()}. Keep that in mind as you guide the conversation."
        else ""
        return """
You are Reflekt, a warm and empathetic journaling companion. Your job is to help the user reflect on their day through natural conversation.

User context: ${profile.name}, ${profile.age}, ${profile.occupation}.
Close relationships: $relationMapStr.$moodContext

CONVERSATION STRUCTURE (guide through ~5 turns):
1. Open: Ask about their current feeling or what brought them here today.
2. Deepen: Follow up on one specific detail they shared.
3. Explore: Gently probe the root cause or trigger behind the feeling.
4. Reflect: Acknowledge what you've heard; ask what support would help.
5. Wrap: Summarise warmly and let them know they can tap Done when ready to save.

CONVERSATION RULES:
- Respond naturally and conversationally
- Ask ONE focused follow-up question per response
- Keep responses under 3 sentences
- Be warm, not clinical
- NEVER output JSON during the conversation

ANALYSIS RULES:
- When the user signals they are done (says "done", "finish", "that's all", "save", or taps the Done button), output ONLY this JSON block with no other text before or after it:

{
  "summary": "2 sentence summary of the session",
  "mood": "HAPPY|SAD|ANGRY|NEUTRAL|FEAR|ANXIOUS",
  "moodScore": 1.0-5.0,
  "triggers": ["trigger1", "trigger2"],
  "triageTier": 1,
  "clinicalFlags": []
}

Until the Done signal, ONLY have a natural conversation. Never output JSON mid-conversation.
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

    /**
     * Wraps a system prompt + conversation history in Gemma 3 instruction format.
     * Each turn is a (role, content) pair where role is "user" or "ai".
     */
    fun formatForGemma3(
        systemPrompt: String,
        turns: List<Pair<String, String>>,
    ): String {
        val sb = StringBuilder()
        sb.append("<start_of_turn>system\n")
        sb.append(systemPrompt)
        sb.append("<end_of_turn>\n")
        turns.forEach { (role, content) ->
            when (role) {
                "user" -> {
                    sb.append("<start_of_turn>user\n")
                    sb.append(content)
                    sb.append("<end_of_turn>\n")
                }
                "ai" -> {
                    sb.append("<start_of_turn>model\n")
                    sb.append(content)
                    sb.append("<end_of_turn>\n")
                }
            }
        }
        // Signal model to respond
        sb.append("<start_of_turn>model\n")
        return sb.toString()
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
