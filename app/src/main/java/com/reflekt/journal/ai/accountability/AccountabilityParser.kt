package com.reflekt.journal.ai.accountability

import com.reflekt.journal.ai.engine.AccountabilityResult
import com.reflekt.journal.ai.engine.AiResponseParser
import com.reflekt.journal.ai.engine.LlmEngine
import com.reflekt.journal.ai.prompt.PromptBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountabilityParser @Inject constructor(
    private val llmEngine: LlmEngine,
    private val promptBuilder: PromptBuilder,
    private val aiResponseParser: AiResponseParser,
) {

    /**
     * Sends the journal [transcript] plus an ID map derived from [snapshot] to the LLM,
     * then parses and returns the [AccountabilityResult].
     */
    suspend fun parseTranscript(
        transcript: String,
        snapshot: AccountabilitySnapshot,
    ): AccountabilityResult = withContext(Dispatchers.IO) {
        val idMap = buildIdMap(snapshot)
        val prompt = promptBuilder.buildParserPrompt(transcript, idMap)
        val raw = llmEngine.generate(prompt)
        aiResponseParser.parseAccountabilityResult(raw)
    }

    private fun buildIdMap(snapshot: AccountabilitySnapshot): Map<String, String> {
        val map = mutableMapOf<String, String>()
        snapshot.habitsDueToday.forEach { map[it.habitId] = it.title }
        snapshot.overdueHabits.forEach { map[it.habitId] = it.title }
        snapshot.todayTodos.forEach { map[it.todoId] = it.title }
        return map
    }
}
