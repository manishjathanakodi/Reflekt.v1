package com.reflekt.journal.ui.screens.wellbeing

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.data.db.AppUsageLog
import com.reflekt.journal.data.db.AppUsageLogDao
import com.reflekt.journal.data.db.ManualAppLimit
import com.reflekt.journal.data.db.ManualAppLimitDao
import com.reflekt.journal.data.db.UserProfileDao
import com.reflekt.journal.wellbeing.usage.AppUsageStat
import com.reflekt.journal.wellbeing.usage.UsageStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ScreenTimeSummary(
    val totalMs: Long,
    val pickupCount: Int,
    val vsGoalMs: Long,
    val hasGoal: Boolean,
)

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

@HiltViewModel
class WellbeingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsRepository: UsageStatsRepository,
    private val userProfileDao: UserProfileDao,
    private val appUsageLogDao: AppUsageLogDao,
    private val manualAppLimitDao: ManualAppLimitDao,
) : ViewModel() {

    // ── Permission ────────────────────────────────────────────────────────────
    private val _usagePermissionGranted = MutableStateFlow(usageStatsRepository.hasPermission())
    val usagePermissionGranted: StateFlow<Boolean> = _usagePermissionGranted

    // ── Today's raw usage stats ───────────────────────────────────────────────
    private val _todayStats = MutableStateFlow<List<AppUsageStat>>(emptyList())

    /** packageName → ms used today */
    val todayUsageMap: StateFlow<Map<String, Long>> = _todayStats
        .map { stats -> stats.associate { it.packageName to it.durationMs } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ── Screen time goal (from UserProfile) ───────────────────────────────────
    val screenTimeGoalMinutes: StateFlow<Int> = userProfileDao.getAll()
        .map { profiles -> profiles.firstOrNull()?.screenTimeGoalMinutes ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ── Screen time summary ───────────────────────────────────────────────────
    val screenTimeSummary: StateFlow<ScreenTimeSummary> = combine(
        _todayStats,
        userProfileDao.getAll(),
    ) { todayStats, profiles ->
        val totalMs     = todayStats.sumOf { it.durationMs }
        val pickups     = todayStats.sumOf { it.launchCount }
        val goalMinutes = profiles.firstOrNull()?.screenTimeGoalMinutes ?: 0
        ScreenTimeSummary(
            totalMs     = totalMs,
            pickupCount = pickups,
            vsGoalMs    = totalMs - goalMinutes * 60_000L,
            hasGoal     = goalMinutes > 0,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ScreenTimeSummary(0L, 0, 0L, false),
    )

    // ── Manual app limits ─────────────────────────────────────────────────────
    val appLimits: StateFlow<List<ManualAppLimit>> = manualAppLimitDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Count of active limits */
    val activeLimitsCount: StateFlow<Int> = appLimits
        .map { limits -> limits.count { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Count of active limits whose today usage has hit the threshold */
    val limitsHitToday: StateFlow<Int> = combine(
        appLimits,
        todayUsageMap,
    ) { limits, usageMap ->
        limits.count { limit ->
            limit.isActive &&
                    (usageMap[limit.packageName] ?: 0L) >= limit.limitMinutes * 60_000L
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ── Weekly usage heatmap data (date → total ms) ───────────────────────────
    val weeklyUsageByDay: StateFlow<Map<String, Long>> = appUsageLogDao.getAll()
        .map { logs ->
            val sevenDaysAgo = LocalDate.now().minusDays(6)
            logs.filter { log ->
                try { LocalDate.parse(log.date) >= sevenDaysAgo } catch (_: Exception) { false }
            }
                .groupBy { it.date }
                .mapValues { (_, dayLogs) -> dayLogs.sumOf { it.durationMs } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ── Installed apps ────────────────────────────────────────────────────────
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) { _installedApps.value = loadInstalledApps() }
        refreshPermission()
    }

    fun refreshPermission() {
        _usagePermissionGranted.value = usageStatsRepository.hasPermission()
        if (_usagePermissionGranted.value) {
            viewModelScope.launch(Dispatchers.IO) {
                val stats = usageStatsRepository.getTodayUsage()
                _todayStats.value = stats
                // Persist today's usage so the weekly heatmap builds up over time
                val today = LocalDate.now().toString()
                stats.forEach { stat ->
                    val logId   = "${stat.packageName}_$today"
                    val existing = appUsageLogDao.getById(logId)
                    appUsageLogDao.upsert(
                        AppUsageLog(
                            logId        = logId,
                            date         = today,
                            packageName  = stat.packageName,
                            appLabel     = stat.appLabel,
                            category     = usageStatsRepository.getAppCategory(stat.packageName),
                            durationMs   = stat.durationMs,
                            launchCount  = stat.launchCount,
                            impactScore  = existing?.impactScore ?: 0f,
                            isTriggerApp = existing?.isTriggerApp ?: false,
                        )
                    )
                }
            }
        }
    }

    fun requestUsagePermission(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    fun addLimit(limit: ManualAppLimit) {
        viewModelScope.launch { manualAppLimitDao.upsert(limit) }
    }

    fun removeLimit(packageName: String) {
        viewModelScope.launch { manualAppLimitDao.delete(packageName) }
    }

    fun setScreenTimeGoal(minutes: Int) {
        viewModelScope.launch {
            val profile = userProfileDao.getAll().first().firstOrNull() ?: return@launch
            userProfileDao.upsert(profile.copy(screenTimeGoalMinutes = minutes))
        }
    }

    private fun loadInstalledApps(): List<InstalledApp> {
        val intent = Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val pm = context.packageManager
        return pm.queryIntentActivities(intent, 0)
            .sortedBy { it.loadLabel(pm).toString() }
            .map { ri ->
                InstalledApp(
                    packageName = ri.activityInfo.packageName,
                    label       = ri.loadLabel(pm).toString(),
                    icon        = try { ri.loadIcon(pm) } catch (_: Exception) { null },
                )
            }
    }
}
