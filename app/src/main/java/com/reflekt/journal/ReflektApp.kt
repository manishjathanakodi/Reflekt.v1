package com.reflekt.journal

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.reflekt.journal.ai.engine.LlmEngine
import com.reflekt.journal.ui.notifications.ReflektNotificationManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ReflektApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationManager: ReflektNotificationManager
    @Inject lateinit var llmEngine: LlmEngine

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        notificationManager.createChannels()
        // Initialize LLM in background on startup so it's ready when user opens journal
        CoroutineScope(Dispatchers.IO).launch {
            try {
                llmEngine.initialize()
            } catch (e: Exception) {
                Log.e("ReflektApp", "LLM init failed", e)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
