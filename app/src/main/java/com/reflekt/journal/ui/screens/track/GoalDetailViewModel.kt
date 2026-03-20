package com.reflekt.journal.ui.screens.track

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.ai.engine.LlmEngine
import com.reflekt.journal.data.db.Goal
import com.reflekt.journal.data.db.GoalDao
import com.reflekt.journal.data.db.Habit
import com.reflekt.journal.data.db.HabitDao
import com.reflekt.journal.data.db.Todo
import com.reflekt.journal.data.db.TodoDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val TAG = "GoalDetailViewModel"

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val goalDao: GoalDao,
    private val habitDao: HabitDao,
    private val todoDao: TodoDao,
    private val llmEngine: LlmEngine,
) : ViewModel() {

    private val goalId: String = checkNotNull(savedStateHandle["goalId"])

    val goal: StateFlow<GoalWithProgress?> = goalDao.getAllIncludingArchived()
        .map { goals ->
            goals.find { it.goalId == goalId }
                ?.let { GoalWithProgress(it, it.progressPercent) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val linkedHabits: StateFlow<List<Habit>> = habitDao.getByGoal(goalId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val linkedTodos: StateFlow<List<Todo>> = todoDao.getByGoal(goalId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _progressNarrative   = MutableStateFlow<String?>(null)
    val progressNarrative: StateFlow<String?> = _progressNarrative

    private val _isGeneratingNarrative = MutableStateFlow(false)
    val isGeneratingNarrative: StateFlow<Boolean> = _isGeneratingNarrative

    fun generateNarrative() {
        if (_isGeneratingNarrative.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingNarrative.value = true
            try {
                val g        = goal.value?.goal ?: return@launch
                val habits   = linkedHabits.value
                val todos    = linkedTodos.value
                val done     = todos.count { it.isCompleted }
                val habBest  = habits.maxOfOrNull { it.streak } ?: 0
                val prompt   = """
                    You are a supportive coach. Write a 2-3 sentence motivational progress narrative for this goal:
                    Goal: "${g.title}"
                    Progress: ${g.progressPercent.toInt()}%
                    Linked habits: ${habits.size} (best streak: $habBest days)
                    Todos: $done/${todos.size} completed
                    Be concise, warm, and action-oriented.
                """.trimIndent()
                _progressNarrative.value = llmEngine.generate(prompt)
            } catch (e: Exception) {
                Log.e(TAG, "generateNarrative failed", e)
            } finally {
                _isGeneratingNarrative.value = false
            }
        }
    }

    fun toggleMilestone(milestoneId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val g = goal.value?.goal ?: return@launch
            val milestones = try {
                Json { ignoreUnknownKeys = true }.decodeFromString<List<MilestoneItem>>(g.milestonesJson)
            } catch (_: Exception) { emptyList() }
            val updated = milestones.map { m ->
                if (m.id == milestoneId) m.copy(isCompleted = !m.isCompleted) else m
            }
            val completedCount = updated.count { it.isCompleted }
            val progress = if (updated.isEmpty()) g.progressPercent
                           else (completedCount.toFloat() / updated.size) * 100f
            goalDao.upsert(
                g.copy(
                    milestonesJson  = Json.encodeToString(updated),
                    progressPercent = progress,
                ),
            )
        }
    }
}
