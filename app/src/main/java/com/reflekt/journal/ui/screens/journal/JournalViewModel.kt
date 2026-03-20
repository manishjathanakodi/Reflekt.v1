package com.reflekt.journal.ui.screens.journal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.ai.accountability.AccountabilityEngine
import com.reflekt.journal.ai.accountability.AccountabilityParser
import com.reflekt.journal.ai.accountability.AccountabilitySnapshot
import com.reflekt.journal.ai.engine.AiResponseParser
import com.reflekt.journal.ai.engine.LlmEngine
import com.reflekt.journal.ai.engine.MoodTag
import com.reflekt.journal.ai.prompt.PromptBuilder
import com.reflekt.journal.ai.triage.TriageEngine
import com.reflekt.journal.data.db.Goal
import com.reflekt.journal.data.db.Habit
import com.reflekt.journal.data.db.HabitLog
import com.reflekt.journal.data.db.HabitLogDao
import com.reflekt.journal.data.db.JournalEntry
import com.reflekt.journal.data.db.JournalEntryDao
import com.reflekt.journal.data.db.MoodLog
import com.reflekt.journal.data.db.MoodLogDao
import com.reflekt.journal.data.db.Todo
import com.reflekt.journal.data.db.TodoDao
import com.reflekt.journal.data.db.UserProfileDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

private const val TAG = "JournalViewModel"

// ── Domain types ──────────────────────────────────────────────────────────────

sealed class Message {
    data class AiMessage(val text: String) : Message()
    data class UserMessage(val text: String) : Message()
}

data class AnalysisResult(
    val mood: MoodTag,
    val trigger: String,
    val habitDetected: String?,
)

data class PostSaveState(
    val encouragement: String,
    val autoMarkedHabits: List<Habit>,
    val autoMarkedTodos: List<Todo>,
    val moodTag: MoodTag,
    val habitsDoneCount: Int,
    val habitsTotalCount: Int,
)

sealed interface JournalNavEvent {
    object NavigateToCrisis : JournalNavEvent
    object NavigateToSaved : JournalNavEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val llmEngine: LlmEngine,
    private val promptBuilder: PromptBuilder,
    private val aiResponseParser: AiResponseParser,
    private val triageEngine: TriageEngine,
    private val accountabilityEngine: AccountabilityEngine,
    private val accountabilityParser: AccountabilityParser,
    private val journalEntryDao: JournalEntryDao,
    private val moodLogDao: MoodLogDao,
    private val habitLogDao: HabitLogDao,
    private val todoDao: TodoDao,
    private val userProfileDao: UserProfileDao,
) : ViewModel() {

    private val _conversation = MutableStateFlow<List<Message>>(emptyList())
    val conversation: StateFlow<List<Message>> = _conversation.asStateFlow()

    private val _liveAnalysis = MutableStateFlow<AnalysisResult?>(null)
    val liveAnalysis: StateFlow<AnalysisResult?> = _liveAnalysis.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _accountabilitySnapshot = MutableStateFlow<AccountabilitySnapshot?>(null)
    val accountabilitySnapshot: StateFlow<AccountabilitySnapshot?> = _accountabilitySnapshot.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _postSaveState = MutableStateFlow<PostSaveState?>(null)
    val postSaveState: StateFlow<PostSaveState?> = _postSaveState.asStateFlow()

    private val _navEvent = Channel<JournalNavEvent>(Channel.BUFFERED)
    val navEvent = _navEvent.receiveAsFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = userProfileDao.getAll().first().firstOrNull()
            val snapshot = accountabilityEngine.buildSnapshot(profile?.uid ?: "")
            _accountabilitySnapshot.value = snapshot

            // Open with AI greeting
            val systemPrompt = profile?.let { promptBuilder.buildJournalSystemPrompt(it) } ?: ""
            val accountContext = promptBuilder.buildAccountabilityContext(
                snapshot.habitsDueToday, snapshot.overdueHabits, snapshot.todayTodos, snapshot.activeGoals,
            )
            _isGenerating.value = true
            val greeting = llmEngine.generate("$systemPrompt\n$accountContext\nReflekt:")
            _conversation.value = listOf(Message.AiMessage(greeting))
            _isGenerating.value = false
        }
    }

    fun onInputChanged(text: String) {
        _inputText.value = text
    }

    fun onSendMessage(text: String) {
        if (text.isBlank() || _isGenerating.value) return
        _inputText.value = ""
        val userMsg = Message.UserMessage(text)
        _conversation.value = _conversation.value + userMsg

        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            try {
                val profile = userProfileDao.getAll().first().firstOrNull()
                val snapshot = _accountabilitySnapshot.value
                val systemPrompt = profile?.let { promptBuilder.buildJournalSystemPrompt(it) } ?: ""
                val accountContext = snapshot?.let {
                    promptBuilder.buildAccountabilityContext(
                        it.habitsDueToday, it.overdueHabits, it.todayTodos, it.activeGoals,
                    )
                } ?: ""
                val history = _conversation.value.dropLast(1).joinToString("\n") { msg ->
                    when (msg) {
                        is Message.AiMessage -> "Reflekt: ${msg.text}"
                        is Message.UserMessage -> "User: ${msg.text}"
                    }
                }
                val fullPrompt = "$systemPrompt\n$accountContext\n$history\nUser: $text\nReflekt:"
                val response = llmEngine.generate(fullPrompt)
                _conversation.value = _conversation.value + Message.AiMessage(response)

                // Update live analysis from response
                val parsed = aiResponseParser.parseJournalAnalysis(response)
                if (parsed.summary.isNotBlank() || parsed.triageTier != 1) {
                    _liveAnalysis.value = AnalysisResult(
                        mood = parsed.mood,
                        trigger = parsed.triggers.firstOrNull() ?: "",
                        habitDetected = null,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "generate() failed", e)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun onDone() {
        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            try {
                val snapshot = _accountabilitySnapshot.value ?: AccountabilitySnapshot(
                    emptyList(), emptyList(), emptyList(), emptyList(),
                )
                val rawText = buildRawText()
                val transcript = buildTranscript()

                // 1. Parse accountability
                val accountabilityResult = accountabilityParser.parseTranscript(transcript, snapshot)

                // 2. Evaluate triage tier
                val lastAnalysis = _liveAnalysis.value
                val journalAnalysis = com.reflekt.journal.ai.engine.JournalAnalysis(
                    summary = "",
                    mood = lastAnalysis?.mood ?: MoodTag.NEUTRAL,
                    moodScore = 3.0f,
                    triggers = if (lastAnalysis?.trigger?.isNotBlank() == true) listOf(lastAnalysis.trigger) else emptyList(),
                    triageTier = 1,
                )
                val tier = triageEngine.evaluate(journalAnalysis, rawText)

                // 3. Save JournalEntry
                val entryId = UUID.randomUUID().toString()
                val entry = JournalEntry(
                    entryId = entryId,
                    timestamp = System.currentTimeMillis(),
                    rawText = rawText,
                    conversationJson = Json.encodeToString(
                        _conversation.value.map { msg ->
                            when (msg) {
                                is Message.AiMessage -> mapOf("role" to "ai", "content" to msg.text)
                                is Message.UserMessage -> mapOf("role" to "user", "content" to msg.text)
                            }
                        },
                    ),
                    aiSummary = journalAnalysis.summary,
                    moodTag = journalAnalysis.mood.name,
                    moodScore = journalAnalysis.moodScore,
                    triggersJson = Json.encodeToString(journalAnalysis.triggers),
                    triageTier = tier,
                    clinicalSummaryJson = null,
                    totalScreenTimeMs = 0L,
                    isDeleted = false,
                )
                journalEntryDao.insert(entry)

                // 4. Save/update MoodLog for today
                val todayStr = LocalDate.now().toString()
                val existingLog = moodLogDao.getAll().first().firstOrNull { it.date == todayStr }
                val moodLog = MoodLog(
                    logId = existingLog?.logId ?: UUID.randomUUID().toString(),
                    date = todayStr,
                    moodScore = journalAnalysis.moodScore,
                    dominantMood = journalAnalysis.mood.name,
                    primaryTrigger = journalAnalysis.triggers.firstOrNull() ?: "",
                    screenTimeMs = 0L,
                    entryCount = (existingLog?.entryCount ?: 0) + 1,
                )
                moodLogDao.upsert(moodLog)

                // 5. Auto-mark completed habits
                val autoMarkedHabits = mutableListOf<Habit>()
                accountabilityResult.completedHabitIds.forEach { habitId ->
                    val habit = snapshot.habitsDueToday.find { it.habitId == habitId }
                        ?: snapshot.overdueHabits.find { it.habitId == habitId }
                    val existingHabitLog = habitLogDao.getByHabitAndDate(habitId, todayStr)
                    if (existingHabitLog == null) {
                        habitLogDao.insert(
                            HabitLog(
                                logId = UUID.randomUUID().toString(),
                                habitId = habitId,
                                date = todayStr,
                                status = "COMPLETED",
                                completedViaJournal = true,
                                note = null,
                                moodAtCompletion = journalAnalysis.mood.name,
                            ),
                        )
                    } else {
                        habitLogDao.upsert(existingHabitLog.copy(status = "COMPLETED", completedViaJournal = true))
                    }
                    habit?.let { autoMarkedHabits.add(it) }
                }

                // 6. Auto-mark completed todos
                val autoMarkedTodos = mutableListOf<Todo>()
                accountabilityResult.completedTodoIds.forEach { todoId ->
                    val todo = todoDao.getById(todoId)
                    if (todo != null) {
                        todoDao.upsert(todo.copy(
                            isCompleted = true,
                            completedAt = System.currentTimeMillis(),
                            completedViaJournal = true,
                        ))
                        autoMarkedTodos.add(todo)
                    }
                }

                // Set post-save state
                _postSaveState.value = PostSaveState(
                    encouragement = accountabilityResult.encouragement.ifBlank {
                        "Great reflection session! Every entry is a step toward self-understanding."
                    },
                    autoMarkedHabits = autoMarkedHabits,
                    autoMarkedTodos = autoMarkedTodos,
                    moodTag = journalAnalysis.mood,
                    habitsDoneCount = autoMarkedHabits.size,
                    habitsTotalCount = snapshot.habitsDueToday.size,
                )

                // 7. Navigate
                if (tier == 3) {
                    _navEvent.send(JournalNavEvent.NavigateToCrisis)
                } else {
                    _navEvent.send(JournalNavEvent.NavigateToSaved)
                }
            } catch (e: Exception) {
                Log.e(TAG, "onDone() failed", e)
                _navEvent.send(JournalNavEvent.NavigateToSaved)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun buildRawText(): String =
        _conversation.value
            .filterIsInstance<Message.UserMessage>()
            .joinToString("\n") { it.text }

    private fun buildTranscript(): String =
        _conversation.value.joinToString("\n") { msg ->
            when (msg) {
                is Message.AiMessage -> "Reflekt: ${msg.text}"
                is Message.UserMessage -> "User: ${msg.text}"
            }
        }
}
