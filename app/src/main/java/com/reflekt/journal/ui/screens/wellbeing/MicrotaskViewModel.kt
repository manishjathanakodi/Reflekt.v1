package com.reflekt.journal.ui.screens.wellbeing

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.ai.engine.LlmEngine
import com.reflekt.journal.data.db.InterventionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MicrotaskViewModel"

enum class MicrotaskType { BREATHING, GRATITUDE, BODY_SCAN }

enum class BreathPhase(val label: String) {
    INHALE("Inhale"),
    HOLD_IN("Hold"),
    EXHALE("Exhale"),
    HOLD_OUT("Hold"),
}

@HiltViewModel
class MicrotaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val interventionDao: InterventionDao,
    private val llmEngine: LlmEngine,
) : ViewModel() {

    private val taskTypeString: String = savedStateHandle["taskType"] ?: "BREATHING"
    val taskType: MicrotaskType = runCatching { MicrotaskType.valueOf(taskTypeString) }
        .getOrDefault(MicrotaskType.BREATHING)

    private val totalSeconds: Int = when (taskType) {
        MicrotaskType.BREATHING -> 120
        MicrotaskType.BODY_SCAN -> 300
        MicrotaskType.GRATITUDE -> 0
    }

    private val _timeRemainingSeconds = MutableStateFlow(totalSeconds)
    val timeRemainingSeconds: StateFlow<Int> = _timeRemainingSeconds

    private val _currentPhase = MutableStateFlow(BreathPhase.INHALE)
    val currentPhase: StateFlow<BreathPhase> = _currentPhase

    private val _cycleCount = MutableStateFlow(0)
    val cycleCount: StateFlow<Int> = _cycleCount

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete

    // Gratitude feedback
    private val _gratitudeFeedback = MutableStateFlow("")
    val gratitudeFeedback: StateFlow<String> = _gratitudeFeedback

    private val _gratitudeSubmitting = MutableStateFlow(false)
    val gratitudeSubmitting: StateFlow<Boolean> = _gratitudeSubmitting

    init {
        if (taskType != MicrotaskType.GRATITUDE) {
            startCountdown()
        }
        if (taskType == MicrotaskType.BREATHING) {
            startBreathingPhases()
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (_timeRemainingSeconds.value > 0) {
                delay(1_000)
                _timeRemainingSeconds.value = (_timeRemainingSeconds.value - 1).coerceAtLeast(0)
            }
            completeTask()
        }
    }

    private fun startBreathingPhases() {
        // 4-count each = 4s in, 4s hold, 4s out, 4s hold → 16s per cycle
        val phases = BreathPhase.values()
        viewModelScope.launch {
            var phaseIdx = 0
            while (!_isComplete.value) {
                _currentPhase.value = phases[phaseIdx % phases.size]
                delay(4_000)
                phaseIdx++
                if (phaseIdx % phases.size == 0) {
                    _cycleCount.value++
                }
            }
        }
    }

    private suspend fun completeTask() {
        _isComplete.value = true
        markInterventionComplete()
    }

    suspend fun submitGratitude(text: String) {
        if (text.length < 20) {
            _gratitudeFeedback.value = "Please write a bit more (at least 20 characters)."
            return
        }
        _gratitudeSubmitting.value = true
        val prompt = """
            The user wrote a gratitude note: "$text"
            Is the overall sentiment positive or neutral? Reply with exactly one word: POSITIVE, NEUTRAL, or NEGATIVE.
        """.trimIndent()
        val response = try {
            llmEngine.generate(prompt).trim().uppercase()
        } catch (e: Exception) {
            Log.e(TAG, "Gratitude analysis failed", e)
            "POSITIVE"
        }
        _gratitudeSubmitting.value = false

        if (response == "NEGATIVE") {
            _gratitudeFeedback.value = "That's a start — can you add something you genuinely appreciate today?"
        } else {
            _gratitudeFeedback.value = ""
            completeTask()
        }
    }

    private suspend fun markInterventionComplete() {
        try {
            val pending = interventionDao.getPending().firstOrNull() ?: return
            val match   = pending.firstOrNull { it.microtaskType == taskTypeString } ?: return
            interventionDao.upsert(
                match.copy(
                    microtaskCompleted = true,
                    status             = "RESOLVED",
                    resolvedAt         = System.currentTimeMillis(),
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark intervention complete", e)
        }
    }
}
