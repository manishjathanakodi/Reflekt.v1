package com.reflekt.journal.wellbeing.usage

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.reflekt.journal.data.db.AppUsageLog
import com.reflekt.journal.data.db.AppUsageLogDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG        = "UsageStatsWorker"
private const val WORK_NAME  = "reflekt_usage_stats_15m"

@HiltWorker
class UsageStatsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: UsageStatsRepository,
    private val appUsageLogDao: AppUsageLogDao,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!repository.hasPermission()) {
            Log.w(TAG, "PACKAGE_USAGE_STATS not granted — skipping work")
            return Result.success()
        }

        val today = LocalDate.now().toString()
        val stats = try {
            repository.getTodayUsage()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching usage stats", e)
            return Result.success()
        }

        stats.forEach { stat ->
            val logId    = "${stat.packageName}_$today"
            val existing = appUsageLogDao.getById(logId)
            val log = AppUsageLog(
                logId       = logId,
                date        = today,
                packageName = stat.packageName,
                appLabel    = stat.appLabel,
                category    = repository.getAppCategory(stat.packageName),
                durationMs  = stat.durationMs,
                launchCount = stat.launchCount,
                impactScore = existing?.impactScore ?: 0f,
                isTriggerApp = existing?.isTriggerApp ?: false,
            )
            appUsageLogDao.upsert(log)
        }

        Log.d(TAG, "Upserted ${stats.size} usage records for $today")
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UsageStatsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            Log.d(TAG, "UsageStatsWorker scheduled")
        }
    }
}
