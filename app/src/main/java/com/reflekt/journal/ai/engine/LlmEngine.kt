package com.reflekt.journal.ai.engine

interface LlmEngine {
    suspend fun initialize()
    suspend fun generate(prompt: String): String
}
