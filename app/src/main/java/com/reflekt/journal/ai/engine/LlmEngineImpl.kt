package com.reflekt.journal.ai.engine

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LlmEngineImpl"
private const val MODEL_FILE = "gemma-2b-it-gpu-int4.gguf"

@Singleton
class LlmEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : LlmEngine {

    @Volatile private var llm: LlmInference? = null
    @Volatile private var stubMode = false
    @Volatile private var initialized = false

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        val modelFile = File(context.filesDir, "models/$MODEL_FILE")
        if (!modelFile.exists()) {
            Log.w(TAG, "Model file not found at ${modelFile.absolutePath} — running in stub mode.")
            stubMode = true
            initialized = true
            return@withContext
        }
        try {
            val opts = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .setTopK(40)
                .setTemperature(0.7f)
                .build()
            llm = LlmInference.createFromOptions(context, opts)
            Log.i(TAG, "LlmInference initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialise LlmInference — falling back to stub.", e)
            stubMode = true
        }
        initialized = true
    }

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        if (!initialized) initialize()
        if (stubMode) return@withContext stubResponse(prompt)
        try {
            llm!!.generateResponse(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "generate() failed — returning stub.", e)
            stubResponse(prompt)
        }
    }

    private fun stubResponse(prompt: String): String {
        // Accountability parser prompt returns a different stub
        return if (prompt.contains("completedHabitIds")) {
            STUB_ACCOUNTABILITY
        } else {
            STUB_JOURNAL_ANALYSIS
        }
    }

    companion object {
        private val STUB_JOURNAL_ANALYSIS = """
            {
              "summary": "User had a reflective session exploring their thoughts and feelings.",
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
    }
}
