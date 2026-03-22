package com.reflekt.journal.ai.engine

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LlmEngineImpl"
private const val MODEL_ASSET = "models/gemma-3-1b-it-q4_0.gguf"

@Singleton
class LlmEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : LlmEngine {

    private var llm: LlmInference? = null
    private var useStub = false

    override val isInitialized = MutableStateFlow(false)
    override val isInitializing = MutableStateFlow(false)

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isInitialized.value || isInitializing.value) return@withContext
        isInitializing.value = true
        try {
            val modelFile = File(context.filesDir, MODEL_ASSET)

            // Copy from assets to internal storage on first launch
            // (MediaPipe needs a file path, not an asset stream)
            if (!modelFile.exists()) {
                modelFile.parentFile?.mkdirs()
                context.assets.open(MODEL_ASSET).use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(512)
                .setTopK(40)
                .setTemperature(0.8f)
                .setRandomSeed(42)
                .build()

            llm = LlmInference.createFromOptions(context, options)
            useStub = false
            isInitialized.value = true
            Log.i(TAG, "Gemma 3 1B loaded ✓")
        } catch (e: Exception) {
            Log.e(TAG, "Model load failed, using stub", e)
            useStub = true
            isInitialized.value = true // mark done even in stub mode so callers unblock
        } finally {
            isInitializing.value = false
        }
    }

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        if (!isInitialized.value) initialize()
        if (useStub) return@withContext stubResponse(prompt)
        try {
            llm?.generateResponse(prompt) ?: stubResponse(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Generation error", e)
            stubResponse(prompt)
        }
    }

    private var stubTurnCount = 0

    private fun stubResponse(prompt: String): String = when {
        prompt.contains("completedHabitIds") -> STUB_ACCOUNTABILITY
        prompt.contains("finished journaling") || prompt.contains("DONE_SIGNAL") ||
            prompt.contains("output ONLY the JSON") -> {
            stubTurnCount = 0
            STUB_JOURNAL_ANALYSIS
        }
        else -> generateStubResponse(prompt)
    }

    private fun generateStubResponse(prompt: String): String {
        // Reset on new conversation (greeting present means session start)
        if (prompt.contains("What would you like to reflect on") ||
            prompt.contains("Good morning") || prompt.contains("Good afternoon") ||
            prompt.contains("Good evening") || prompt.contains("Still up") ||
            stubTurnCount > 10
        ) {
            stubTurnCount = 0
        }
        val response = when (stubTurnCount) {
            0    -> "That's a start. What's been on your mind most today?"
            1    -> "Tell me more about that. How did it make you feel?"
            2    -> "How long has this been weighing on you?"
            3    -> "Did anything or anyone make it better or worse?"
            4    -> "What do you think is the root cause of this feeling?"
            5    -> "Is there something specific you wish had gone differently today?"
            6    -> "What's one thing within your control that you could do about this?"
            7    -> "How are you feeling right now compared to when you started writing?"
            8    -> "Is there anything else you want to get off your chest before we wrap up?"
            else -> "Take your time. What else is on your mind?"
        }
        stubTurnCount++
        return response
    }

    companion object {
        private val STUB_JOURNAL_ANALYSIS = """
            {
              "summary": "User shared their thoughts and feelings in today's session.",
              "mood": "NEUTRAL",
              "moodScore": 3.0,
              "triggers": [],
              "triageTier": 1,
              "clinicalFlags": []
            }
        """.trimIndent()

        private val STUB_ACCOUNTABILITY = """
            {
              "completedHabitIds": [],
              "skippedHabitIds": [],
              "completedTodoIds": [],
              "habitInsight": "No specific habit data could be extracted from this session.",
              "encouragement": "Every reflection is a step forward. Keep going!"
            }
        """.trimIndent()

        private const val STUB_WEEKLY_REPORT =
            "You've been showing up for yourself consistently this week — that takes courage. " +
            "Your entries suggest a reflective, thoughtful mindset. " +
            "Keep going, one day at a time."
    }
}
