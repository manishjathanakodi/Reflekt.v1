package com.reflekt.journal.ui.screens.journal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.ai.accountability.AccountabilityEngine
import com.reflekt.journal.ai.accountability.AccountabilityParser
import com.reflekt.journal.ai.accountability.AccountabilitySnapshot
import com.reflekt.journal.ai.engine.AiResponseParser
import com.reflekt.journal.ai.engine.JournalSessionStore
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
import kotlinx.coroutines.delay
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

private const val CHAT_TAG = "JournalChatViewModel"

sealed interface ChatNavEvent {
    object NavigateToCrisis : ChatNavEvent
    object NavigateToSaved : ChatNavEvent
    object ShowClosingDialog : ChatNavEvent
}

// ── Domain types shared with JournalChatScreen ────────────────────────────────

sealed class ChatMessage {
    data class AiMessage(val text: String) : ChatMessage()
    data class UserMessage(val text: String) : ChatMessage()
}

data class ChatAnalysisResult(
    val mood: MoodTag,
    val trigger: String,
    val habitDetected: String?,
)

data class ChatPostSaveState(
    val encouragement: String,
    val autoMarkedHabits: List<Habit>,
    val autoMarkedTodos: List<Todo>,
    val moodTag: MoodTag,
    val habitsDoneCount: Int,
    val habitsTotalCount: Int,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class JournalChatViewModel @Inject constructor(
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
    private val sessionStore: JournalSessionStore,
) : ViewModel() {

    private val _conversation = MutableStateFlow<List<ChatMessage>>(emptyList())
    val conversation: StateFlow<List<ChatMessage>> = _conversation.asStateFlow()

    private val _liveAnalysis = MutableStateFlow<ChatAnalysisResult?>(null)
    val liveAnalysis: StateFlow<ChatAnalysisResult?> = _liveAnalysis.asStateFlow()

    @Volatile private var analysisComplete = false
    private val _parsedAnalysis = MutableStateFlow<com.reflekt.journal.ai.engine.JournalAnalysis?>(null)

    val isInitializing: StateFlow<Boolean> = llmEngine.isInitializing

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _accountabilitySnapshot = MutableStateFlow<AccountabilitySnapshot?>(null)
    val accountabilitySnapshot: StateFlow<AccountabilitySnapshot?> = _accountabilitySnapshot.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _chatPostSaveState = MutableStateFlow<ChatPostSaveState?>(null)
    val chatPostSaveState: StateFlow<ChatPostSaveState?> = _chatPostSaveState.asStateFlow()

    private val _navEvent = Channel<ChatNavEvent>(Channel.BUFFERED)
    val navEvent = _navEvent.receiveAsFlow()

    private val _moodSnackbar = Channel<String>(Channel.BUFFERED)
    val moodSnackbar = _moodSnackbar.receiveAsFlow()

    @Volatile private var initialMood: MoodTag? = null
    @Volatile private var closingMood: MoodTag? = null
    @Volatile private var pendingTier: Int = 1

    init {
        viewModelScope.launch(Dispatchers.IO) {
            llmEngine.initialize()

            initialMood = sessionStore.pendingInitialMood
            sessionStore.pendingInitialMood = null

            val profile = userProfileDao.getAll().first().firstOrNull()
            val snapshot = accountabilityEngine.buildSnapshot(profile?.uid ?: "")
            _accountabilitySnapshot.value = snapshot

            _conversation.value = listOf(ChatMessage.AiMessage(pickGreeting()))
        }
    }

    fun onInputChanged(text: String) {
        _inputText.value = text
    }

    fun onSendMessage(text: String) {
        if (text.isBlank() || _isGenerating.value) return
        _inputText.value = ""
        val userMsg = ChatMessage.UserMessage(text)
        _conversation.value = _conversation.value + userMsg

        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            try {
                val profile = userProfileDao.getAll().first().firstOrNull()
                val snapshot = _accountabilitySnapshot.value
                val systemPrompt = profile?.let { promptBuilder.buildJournalSystemPrompt(it, initialMood) } ?: ""
                val accountContext = snapshot?.let {
                    promptBuilder.buildAccountabilityContext(
                        it.habitsDueToday, it.overdueHabits, it.todayTodos, it.activeGoals,
                    )
                } ?: ""
                val historyMessages = _conversation.value.dropLast(1)
                val fullPrompt = buildFullPrompt(systemPrompt, accountContext, historyMessages, text)
                val response = llmEngine.generate(fullPrompt)

                if (isJsonResponse(response)) {
                    val parsed = aiResponseParser.parseJournalAnalysis(response)
                    _parsedAnalysis.value = parsed
                    analysisComplete = true
                    _liveAnalysis.value = ChatAnalysisResult(
                        mood = parsed.mood,
                        trigger = parsed.triggers.firstOrNull() ?: "",
                        habitDetected = null,
                    )
                } else {
                    _conversation.value = _conversation.value + ChatMessage.AiMessage(response)
                }
            } catch (e: Exception) {
                Log.e(CHAT_TAG, "generate() failed", e)
                _conversation.value = _conversation.value + ChatMessage.AiMessage("I'm here. Tell me more about what's on your mind.")
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

                val accountabilityResult = accountabilityParser.parseTranscript(transcript, snapshot)

                val journalAnalysis: com.reflekt.journal.ai.engine.JournalAnalysis
                if (analysisComplete && _parsedAnalysis.value != null) {
                    journalAnalysis = _parsedAnalysis.value!!
                } else {
                    val profile = userProfileDao.getAll().first().firstOrNull()
                    val systemPrompt = profile?.let { promptBuilder.buildJournalSystemPrompt(it, initialMood) } ?: ""
                    val accountContext = promptBuilder.buildAccountabilityContext(
                        snapshot.habitsDueToday, snapshot.overdueHabits, snapshot.todayTodos, snapshot.activeGoals,
                    )
                    val donePrompt = buildFullPrompt(
                        systemPrompt = systemPrompt,
                        accountabilityContext = accountContext,
                        conversation = _conversation.value,
                        newUserMessage = "The user has finished journaling. Now output ONLY the JSON analysis block.",
                    )
                    val jsonResponse = llmEngine.generate(donePrompt)
                    journalAnalysis = aiResponseParser.parseJournalAnalysis(jsonResponse)
                    _parsedAnalysis.value = journalAnalysis
                    analysisComplete = true
                }
                val tier = triageEngine.evaluate(journalAnalysis, rawText)

                val entryId = UUID.randomUUID().toString()
                val entry = JournalEntry(
                    entryId = entryId,
                    timestamp = System.currentTimeMillis(),
                    rawText = rawText,
                    conversationJson = Json.encodeToString(
                        _conversation.value.map { msg ->
                            when (msg) {
                                is ChatMessage.AiMessage -> mapOf("role" to "ai", "content" to msg.text)
                                is ChatMessage.UserMessage -> mapOf("role" to "user", "content" to msg.text)
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

                val todayStr = LocalDate.now().toString()
                val existingLog = moodLogDao.getByDate(todayStr)
                val dayStart = java.time.LocalDate.now()
                    .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val dayEnd = dayStart + 24L * 60 * 60 * 1000
                val todayEntries = journalEntryDao.getEntriesForDay(dayStart, dayEnd)
                val avgScore = if (todayEntries.isNotEmpty())
                    todayEntries.map { it.moodScore }.average().toFloat()
                else journalAnalysis.moodScore
                val dominantMoodStr = todayEntries.groupBy { it.moodTag }
                    .maxByOrNull { it.value.size }?.key ?: journalAnalysis.mood.name
                val primaryTrigger = todayEntries.flatMap { e ->
                    try { Json.decodeFromString<List<String>>(e.triggersJson) }
                    catch (_: Exception) { emptyList() }
                }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: ""
                moodLogDao.upsert(
                    MoodLog(
                        logId = existingLog?.logId ?: UUID.randomUUID().toString(),
                        date = todayStr,
                        moodScore = avgScore,
                        dominantMood = dominantMoodStr,
                        primaryTrigger = primaryTrigger,
                        screenTimeMs = 0L,
                        entryCount = todayEntries.size,
                    ),
                )

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

                val autoMarkedTodos = mutableListOf<Todo>()
                accountabilityResult.completedTodoIds.forEach { todoId ->
                    val todo = todoDao.getById(todoId)
                    if (todo != null) {
                        todoDao.upsert(
                            todo.copy(
                                isCompleted = true,
                                completedAt = System.currentTimeMillis(),
                                completedViaJournal = true,
                            ),
                        )
                        autoMarkedTodos.add(todo)
                    }
                }

                _chatPostSaveState.value = ChatPostSaveState(
                    encouragement = accountabilityResult.encouragement.ifBlank {
                        "Great reflection session! Every entry is a step toward self-understanding."
                    },
                    autoMarkedHabits = autoMarkedHabits,
                    autoMarkedTodos = autoMarkedTodos,
                    moodTag = journalAnalysis.mood,
                    habitsDoneCount = autoMarkedHabits.size,
                    habitsTotalCount = snapshot.habitsDueToday.size,
                )

                if (tier == 3) {
                    _navEvent.send(ChatNavEvent.NavigateToCrisis)
                } else {
                    pendingTier = tier
                    _navEvent.send(ChatNavEvent.ShowClosingDialog)
                }
            } catch (e: Exception) {
                Log.e(CHAT_TAG, "onDone() failed", e)
                _navEvent.send(ChatNavEvent.ShowClosingDialog)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun onClosingMoodSet(mood: MoodTag) {
        closingMood = mood
        viewModelScope.launch {
            val msg = buildMoodComparisonMessage(initialMood, mood)
            _moodSnackbar.send(msg)
            delay(1_600)
            _navEvent.send(ChatNavEvent.NavigateToSaved)
        }
    }

    fun onClosingDialogSkipped() {
        viewModelScope.launch {
            _navEvent.send(ChatNavEvent.NavigateToSaved)
        }
    }

    private fun buildMoodComparisonMessage(start: MoodTag?, end: MoodTag): String {
        if (start == null) return "Session saved. Great reflection! 💛"
        return if (start == end) {
            "You started and ended feeling ${end.name.lowercase()} — consistency matters. 💛"
        } else {
            "Mood shift: ${start.name.lowercase()} → ${end.name.lowercase()} 💛"
        }
    }

    private fun buildRawText(): String =
        _conversation.value
            .filterIsInstance<ChatMessage.UserMessage>()
            .joinToString("\n") { it.text }

    private fun buildTranscript(): String =
        _conversation.value.joinToString("\n") { msg ->
            when (msg) {
                is ChatMessage.AiMessage -> "Reflekt: ${msg.text}"
                is ChatMessage.UserMessage -> "User: ${msg.text}"
            }
        }

    private fun pickGreeting(): String {
        val hour = java.time.LocalTime.now().hour
        val options = when {
            hour in 6..11 -> listOf(
                "Good morning! How are you feeling as you start your day?",
                "Morning! What's on your mind today?",
                "Good morning. What would you like to reflect on today?",
            )
            hour in 12..17 -> listOf(
                "Good afternoon! How has your day been so far?",
                "Hi there. What's been on your mind today?",
                "Afternoon! What would you like to talk about?",
            )
            hour in 18..21 -> listOf(
                "Good evening! How did today treat you?",
                "Evening. What's been the highlight of your day?",
                "Hi! How are you feeling as the day winds down?",
            )
            else -> listOf(
                "Still up? What's on your mind tonight?",
                "Good evening. How was your day overall?",
            )
        }
        return options.random()
    }

    private fun buildFullPrompt(
        systemPrompt: String,
        accountabilityContext: String,
        conversation: List<ChatMessage>,
        newUserMessage: String,
    ): String {
        val sb = StringBuilder()
        sb.append(systemPrompt)
        sb.append("\n\n")
        sb.append(accountabilityContext)
        sb.append("\n\nConversation so far:\n")
        conversation.forEach { msg ->
            when (msg) {
                is ChatMessage.AiMessage -> sb.append("Assistant: ${msg.text}\n")
                is ChatMessage.UserMessage -> sb.append("User: ${msg.text}\n")
            }
        }
        sb.append("User: $newUserMessage\n")
        sb.append("Assistant:")
        return sb.toString()
    }

    private fun isJsonResponse(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("{") && trimmed.endsWith("}")
    }
}
