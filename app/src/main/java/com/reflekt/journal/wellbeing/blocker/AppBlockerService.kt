package com.reflekt.journal.wellbeing.blocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.reflekt.journal.data.db.AppUsageLogDao
import com.reflekt.journal.data.db.Intervention
import com.reflekt.journal.data.db.InterventionDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val TAG = "AppBlockerService"

@AndroidEntryPoint
class AppBlockerService : AccessibilityService() {

    @Inject lateinit var appUsageLogDao: AppUsageLogDao
    @Inject lateinit var interventionDao: InterventionDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes   = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags        = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        Log.d(TAG, "AppBlockerService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        scope.launch {
            val triggerLogs = appUsageLogDao.getAll().firstOrNull()
                ?.filter { it.isTriggerApp && it.packageName == packageName }
                ?: return@launch

            if (triggerLogs.isEmpty()) return@launch

            val activePending = interventionDao.getPending().firstOrNull()
                ?.firstOrNull { it.packageName == packageName && it.status == "PENDING" }

            if (activePending != null) {
                // Already has an active intervention — launch blocked screen
                launchBlockedActivity(packageName)
                return@launch
            }

            // No override active — check for recently resolved
            val allInterventions = interventionDao.getAll().firstOrNull() ?: emptyList()
            val recentOverride = allInterventions.firstOrNull {
                it.packageName == packageName &&
                        it.status == "RESOLVED" &&
                        it.resolvedAt != null &&
                        (System.currentTimeMillis() - it.resolvedAt!!) < 30 * 60 * 1_000L
            }
            if (recentOverride != null) return@launch // within access window

            // Create new intervention and show blocked screen
            val intervention = Intervention(
                id                 = UUID.randomUUID().toString(),
                timestamp          = System.currentTimeMillis(),
                triggerType        = "SCREEN_TIME_LIMIT",
                actionTaken        = "BLOCKED",
                packageName        = packageName,
                microtaskType      = null,
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

    override fun onInterrupt() {
        Log.d(TAG, "AppBlockerService interrupted")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
