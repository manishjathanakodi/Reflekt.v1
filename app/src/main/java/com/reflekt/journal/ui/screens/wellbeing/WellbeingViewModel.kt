package com.reflekt.journal.ui.screens.wellbeing

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.data.db.AppUsageLog
import com.reflekt.journal.data.db.AppUsageLogDao
import com.reflekt.journal.data.db.MoodLogDao
import com.reflekt.journal.data.preferences.settingsDataStore
import com.reflekt.journal.wellbeing.correlation.CorrelationEngine
import com.reflekt.journal.wellbeing.correlation.WELLBEING_INSIGHT_KEY
import com.reflekt.journal.wellbeing.usage.UsageStatsRepository
import com.reflekt.journal.wellbeing.usage.UsageStatsWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "WellbeingViewModel"

data class ScreenTimeSummary(
    val totalMs: Long,
    val pickupCount: Int,
    val vsGoalMs: Long,
    val moodScore: Float,
    val moodTrend: Float,
)

data class HeatmapCell(
    val category: String,
    val dayOfWeek: DayOfWeek,
    val impactScore: Float,
)

data class AppUsageWithImpact(
    val log: AppUsageLog,
    val appLabel: String,
)

@HiltViewModel
class WellbeingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsRepository: UsageStatsRepository,
    private val correlationEngine: CorrelationEngine,
    private val appUsageLogDao: AppUsageLogDao,
    private val moodLogDao: MoodLogDao,
) : ViewModel() {

    private val SCREEN_TIME_GOAL_MS = 3 * 60 * 60 * 1000L // 3h default goal

    // ── Permission state ──────────────────────────────────────────────────────
    private val _usagePermissionGranted = MutableStateFlow(usageStatsRepository.hasPermission())
    val usagePermissionGranted: StateFlow<Boolean> = _usagePermissionGranted

    // ── AI insight from DataStore ─────────────────────────────────────────────
    val aiInsight: StateFlow<String> = context.settingsDataStore.data
        .map { prefs -> prefs[WELLBEING_INSIGHT_KEY] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    // ── Today's summary ───────────────────────────────────────────────────────
    val screenTimeSummary: StateFlow<ScreenTimeSummary> =
        combine(
            appUsageLogDao.getByDate(LocalDate.now().toString()),
            moodLogDao.getLastNDays(2),
        ) { todayLogs, recentMoods ->
            val totalMs = todayLogs.sumOf { it.durationMs }
            val pickups = todayLogs.sumOf { it.launchCount }
            val moodToday = recentMoods.firstOrNull()?.moodScore ?: 0f
            val moodYesterday = recentMoods.getOrNull(1)?.moodScore ?: moodToday
            ScreenTimeSummary(
                totalMs      = totalMs,
                pickupCount  = pickups,
                vsGoalMs     = totalMs - SCREEN_TIME_GOAL_MS,
                moodScore    = moodToday,
                moodTrend    = moodToday - moodYesterday,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ScreenTimeSummary(0L, 0, 0L, 0f, 0f),
        )

    // ── Heatmap: category × day → impact ─────────────────────────────────────
    val heatmapData: StateFlow<List<HeatmapCell>> =
        appUsageLogDao.getAll().map { logs ->
            val sevenDaysAgo = LocalDate.now().minusDays(6)
            val thisWeek = logs.filter {
                try { LocalDate.parse(it.date) >= sevenDaysAgo } catch (_: Exception) { false }
            }
            val categories = listOf("SOCIAL", "WORK", "STREAMING")
            val days = DayOfWeek.values().toList()

            categories.flatMap { cat ->
                days.map { dow ->
                    val logsForCell = thisWeek.filter { log ->
                        log.category == cat &&
                                try { LocalDate.parse(log.date).dayOfWeek == dow } catch (_: Exception) { false }
                    }
                    val avgImpact = if (logsForCell.isEmpty()) 0f
                    else logsForCell.map { it.impactScore }.average().toFloat()
                    HeatmapCell(cat, dow, avgImpact)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Trigger apps ──────────────────────────────────────────────────────────
    val triggerApps: StateFlow<List<AppUsageWithImpact>> =
        appUsageLogDao.getAll().map { logs ->
            logs.filter { it.isTriggerApp }
                .sortedBy { it.impactScore }
                .map { AppUsageWithImpact(it, it.appLabel) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshPermissionAndStart()
    }

    fun refreshPermission() {
        _usagePermissionGranted.value = usageStatsRepository.hasPermission()
        if (_usagePermissionGranted.value) {
            refreshPermissionAndStart()
        }
    }

    fun requestUsagePermission(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun refreshPermissionAndStart() {
        if (!usageStatsRepository.hasPermission()) return
        _usagePermissionGranted.value = true
        UsageStatsWorker.schedule(context)
        viewModelScope.launch {
            try {
                correlationEngine.runCorrelation("local_user")
            } catch (e: Exception) {
                Log.e(TAG, "Correlation engine error", e)
            }
        }
    }
}
