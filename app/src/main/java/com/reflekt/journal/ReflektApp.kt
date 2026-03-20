package com.reflekt.journal

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.reflekt.journal.ui.notifications.ReflektNotificationManager
import com.reflekt.journal.wellbeing.usage.UsageStatsRepository
import com.reflekt.journal.wellbeing.usage.UsageStatsWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ReflektApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var usageStatsRepository: UsageStatsRepository
    @Inject lateinit var notificationManager: ReflektNotificationManager

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        notificationManager.createChannels()
        scheduleWellbeingWorkers()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun scheduleWellbeingWorkers() {
        if (usageStatsRepository.hasPermission()) {
            UsageStatsWorker.schedule(this)
        }
    }
}
