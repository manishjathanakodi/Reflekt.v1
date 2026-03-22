package com.reflekt.journal.ai.engine

import kotlinx.coroutines.flow.StateFlow

interface LlmEngine {
    val isInitialized: StateFlow<Boolean>
    val isInitializing: StateFlow<Boolean>
    suspend fun initialize()
    suspend fun generate(prompt: String): String
}
