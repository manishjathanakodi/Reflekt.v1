package com.reflekt.journal.wellbeing.blocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.reflekt.journal.data.db.Intervention
import com.reflekt.journal.data.db.InterventionDao
import com.reflekt.journal.data.db.ManualAppLimitDao
import com.reflekt.journal.wellbeing.usage.UsageStatsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val TAG        = "AppBlockerService"
private const val CHANNEL_ID = "reflekt_wellbeing_limits"

@AndroidEntryPoint
class AppBlockerService : AccessibilityService() {

    @Inject lateinit var manualAppLimitDao: ManualAppLimitDao
    @Inject lateinit var interventionDao: InterventionDao
    @Inject lateinit var usageStatsRepository: UsageStatsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes          = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType        = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags               = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        ensureNotificationChannel()
        Log.d(TAG, "AppBlockerService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        scope.launch {
            // 1. Look up manual limit for this app
            val limit = manualAppLimitDao.getByPackage(packageName) ?: return@launch
            if (!limit.isActive) return@launch

            // 2. Get today's usage directly from UsageStatsRepository
            val usageMs = usageStatsRepository.getTodayUsage()
                .firstOrNull { it.packageName == packageName }
                ?.durationMs ?: 0L

            val limitMs = limit.limitMinutes * 60_000L

            if (!limit.autoBlock) {
                // Notify only — no blocking
                if (usageMs >= limitMs) {
                    sendLimitNotification(limit.appLabel, limit.limitMinutes, packageName)
                }
                return@launch
            }

            // 3. Auto-block path: check if usage exceeds limit
            if (usageMs < limitMs) return@launch

            // 4. Check for an existing PENDING intervention
            val activePending = interventionDao.getPending().firstOrNull()
                ?.firstOrNull { it.packageName == packageName }

            if (activePending != null) {
                launchBlockedActivity(packageName)
                return@launch
            }

            // 5. Check for a recently resolved access window (30 min)
            val allInterventions = interventionDao.getAll().firstOrNull() ?: emptyList()
            val recentOverride = allInterventions.firstOrNull {
                it.packageName == packageName &&
                        it.status == "RESOLVED" &&
                        it.resolvedAt != null &&
                        (System.currentTimeMillis() - it.resolvedAt!!) < 30 * 60_000L
            }
            if (recentOverride != null) return@launch

            // 6. Create intervention and block
            val intervention = Intervention(
                id                 = UUID.randomUUID().toString(),
                timestamp          = System.currentTimeMillis(),
                triggerType        = "USAGE_LIMIT",
                actionTaken        = "BLOCKED",
                packageName        = packageName,
                microtaskType      = if (limit.requireMicrotask) "BREATHING" else null,
                microtaskCompleted = false,
                overrideUsed       = false,
                status             = "PENDING",
                resolvedAt         = null,
            )
            interventionDao.insert(intervention)
            launchBlockedActivity(packageName)
        }
    }

    private fun launchBlockedActivity(packageName: String) {
        val intent = Intent(applicationContext, BlockedActivity::class.java).apply {
            putExtra("packageName", packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        applicationContext.startActivity(intent)
    }

    private fun sendLimitNotification(appLabel: String, limitMinutes: Int, packageName: String) {
        val notifManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Screen time limit reached")
            .setContentText("You've reached your ${limitMinutes}min limit for $appLabel today")
            .setAutoCancel(true)
            .build()
        notifManager.notify(packageName.hashCode(), notification)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Wellbeing Limits",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    )
                )
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "AppBlockerService interrupted")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
