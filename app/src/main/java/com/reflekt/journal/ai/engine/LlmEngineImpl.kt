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

    private fun stubResponse(prompt: String): String = when {
        prompt.contains("completedHabitIds") -> STUB_ACCOUNTABILITY
        prompt.contains("finished journaling") || prompt.contains("DONE_SIGNAL") -> STUB_JOURNAL_ANALYSIS
        prompt.contains("weekly emotional wellness") -> STUB_WEEKLY_REPORT
        else -> generateStubResponse(prompt)
    }

    private fun generateStubResponse(prompt: String): String {
        val responses = listOf(
            "That's worth exploring. What do you think triggered that feeling?",
            "I hear you. How long have you been feeling this way?",
            "That makes sense. What part of your day affected you the most?",
            "Thank you for sharing that. Is there anything specific you'd like to focus on today?",
            "It sounds like you have a lot on your mind. What feels most important to talk about?",
            "How did that make you feel in the moment?",
            "What would make tomorrow feel better than today?",
            "Is there anyone in your life you'd like to talk to about this?",
        )
        val index = prompt.length % responses.size
        return responses[index]
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
