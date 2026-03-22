package com.reflekt.journal.ui.screens.wellbeing

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.data.db.ManualAppLimit
import com.reflekt.journal.data.db.ManualAppLimitDao
import com.reflekt.journal.data.db.MoodLogDao
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScreenTimeSummary(
    val totalMs: Long,
    val pickupCount: Int,
    val vsGoalMs: Long,
    val moodScore: Float,
    val moodTrend: Float,
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
    private val moodLogDao: MoodLogDao,
    private val manualAppLimitDao: ManualAppLimitDao,
) : ViewModel() {

    private val SCREEN_TIME_GOAL_MS = 3 * 60 * 60 * 1000L

    // ── Permission ────────────────────────────────────────────────────────────
    private val _usagePermissionGranted = MutableStateFlow(usageStatsRepository.hasPermission())
    val usagePermissionGranted: StateFlow<Boolean> = _usagePermissionGranted

    // ── Today's raw usage (loaded on demand from UsageStatsRepository) ────────
    private val _todayStats = MutableStateFlow<List<AppUsageStat>>(emptyList())

    /** packageName → milliseconds used today */
    val todayUsageMap: StateFlow<Map<String, Long>> = _todayStats
        .map { stats -> stats.associate { it.packageName to it.durationMs } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ── Screen time summary ───────────────────────────────────────────────────
    val screenTimeSummary: StateFlow<ScreenTimeSummary> = combine(
        _todayStats,
        moodLogDao.getLastNDays(2),
    ) { todayStats, recentMoods ->
        val totalMs = todayStats.sumOf { it.durationMs }
        val pickups  = todayStats.sumOf { it.launchCount }
        val moodToday     = recentMoods.firstOrNull()?.moodScore ?: 0f
        val moodYesterday = recentMoods.getOrNull(1)?.moodScore ?: moodToday
        ScreenTimeSummary(
            totalMs     = totalMs,
            pickupCount = pickups,
            vsGoalMs    = totalMs - SCREEN_TIME_GOAL_MS,
            moodScore   = moodToday,
            moodTrend   = moodToday - moodYesterday,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ScreenTimeSummary(0L, 0, 0L, 0f, 0f),
    )

    // ── Manual app limits ─────────────────────────────────────────────────────
    val appLimits: StateFlow<List<ManualAppLimit>> = manualAppLimitDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Installed apps (loaded once from PackageManager on init) ──────────────
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
                _todayStats.value = usageStatsRepository.getTodayUsage()
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

    private fun loadInstalledApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
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
