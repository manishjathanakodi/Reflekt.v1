package com.reflekt.journal.ui.screens.analytics

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.ai.engine.LlmEngine
import com.reflekt.journal.ai.engine.MoodTag
import com.reflekt.journal.data.db.JournalEntry
import com.reflekt.journal.data.db.JournalEntryDao
import com.reflekt.journal.data.db.MoodLog
import com.reflekt.journal.data.db.MoodLogDao
import com.reflekt.journal.data.preferences.weeklyReportFlow
import com.reflekt.journal.data.preferences.setWeeklyReport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "AnalyticsViewModel"

// ── Domain types ──────────────────────────────────────────────────────────────

enum class Period { SEVEN_DAYS, THIRTY_DAYS, ALL_TIME }

data class ChartEntry(
    val date: LocalDate,
    val moodScore: Float,
    val mood: MoodTag,
)

data class TriggerCount(
    val trigger: String,
    val count: Int,
    val pct: Float,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val journalEntryDao: JournalEntryDao,
    private val moodLogDao: MoodLogDao,
    private val llmEngine: LlmEngine,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val period = MutableStateFlow(Period.SEVEN_DAYS)

    // ── Base data ─────────────────────────────────────────────────────────────

    private val allEntries: StateFlow<List<JournalEntry>> = journalEntryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allMoodLogs: StateFlow<List<MoodLog>> = moodLogDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Filtered entries for current period ───────────────────────────────────

    private fun filteredEntries(entries: List<JournalEntry>, p: Period): List<JournalEntry> {
        val cutoff = when (p) {
            Period.SEVEN_DAYS   -> LocalDate.now().minusDays(7).toEpochMillis()
            Period.THIRTY_DAYS  -> LocalDate.now().minusDays(30).toEpochMillis()
            Period.ALL_TIME     -> 0L
        }
        return entries.filter { it.timestamp >= cutoff }
    }

    private fun filteredLogs(logs: List<MoodLog>, p: Period): List<MoodLog> {
        val cutoff = when (p) {
            Period.SEVEN_DAYS   -> LocalDate.now().minusDays(7).toString()
            Period.THIRTY_DAYS  -> LocalDate.now().minusDays(30).toString()
            Period.ALL_TIME     -> ""
        }
        return if (p == Period.ALL_TIME) logs else logs.filter { it.date >= cutoff }
    }

    private fun LocalDate.toEpochMillis(): Long =
        this.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    // ── Derived analytics flows ───────────────────────────────────────────────

    /** Percentage breakdown of each MoodTag for the selected period. */
    val moodDistribution: StateFlow<Map<MoodTag, Float>> =
        combine(period, allEntries) { p, entries ->
            val filtered = filteredEntries(entries, p)
            if (filtered.isEmpty()) return@combine emptyMap()
            val counts = filtered.groupBy { entry ->
                try { MoodTag.valueOf(entry.moodTag) } catch (_: Exception) { MoodTag.NEUTRAL }
            }.mapValues { it.value.size.toFloat() }
            val total = counts.values.sum()
            counts.mapValues { (it.value / total) * 100f }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Total entry count for the selected period (used by DonutChart centre). */
    val totalEntries: StateFlow<Int> =
        combine(period, allEntries) { p, entries ->
            filteredEntries(entries, p).size
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** One ChartEntry per MoodLog record — x = date, y = moodScore. */
    val trendData: StateFlow<List<ChartEntry>> =
        combine(period, allMoodLogs) { p, logs ->
            filteredLogs(logs, p)
                .sortedBy { it.date }
                .map { log ->
                    val mood = try { MoodTag.valueOf(log.dominantMood) }
                               catch (_: Exception) { MoodTag.NEUTRAL }
                    ChartEntry(
                        date      = LocalDate.parse(log.date),
                        moodScore = log.moodScore,
                        mood      = mood,
                    )
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Top triggers ranked by occurrence count. */
    val topTriggers: StateFlow<List<TriggerCount>> =
        combine(period, allEntries) { p, entries ->
            val filtered = filteredEntries(entries, p)
            val allTriggers = filtered.flatMap { entry ->
                try { Json { ignoreUnknownKeys = true; isLenient = true }
                    .decodeFromString<List<String>>(entry.triggersJson)
                } catch (_: Exception) { emptyList() }
            }
            if (allTriggers.isEmpty()) return@combine emptyList()
            val counts = allTriggers.groupingBy { it }.eachCount()
            val total  = counts.values.sum().toFloat()
            counts.entries
                .sortedByDescending { it.value }
                .take(8)
                .map { (trigger, count) ->
                    TriggerCount(
                        trigger = trigger,
                        count   = count,
                        pct     = count / total * 100f,
                    )
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Weekly report ─────────────────────────────────────────────────────────

    private val _weeklyReport = MutableStateFlow<String?>(null)
    val weeklyReport: StateFlow<String?> = _weeklyReport.asStateFlow()

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    private val _reportError = MutableStateFlow<String?>(null)
    val reportError: StateFlow<String?> = _reportError.asStateFlow()

    init {
        // Restore cached report from DataStore
        viewModelScope.launch {
            context.weeklyReportFlow().collect { cached ->
                if (_weeklyReport.value == null) _weeklyReport.value = cached
            }
        }
    }

    fun onPeriodChanged(p: Period) { period.value = p }

    /**
     * DEBUG ONLY — inserts 7 days of fake MoodLog records so the trend chart
     * has enough data points to render.  Uses upsert so it is safe to call
     * repeatedly; existing rows for those dates are simply overwritten.
     * Remove this function (and its call-site in AnalyticsScreen) before release.
     */
    fun generateWeeklyReport() {
        if (_isGeneratingReport.value) return
        _reportError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingReport.value = true
            try {
                val dist     = moodDistribution.value
                val triggers = topTriggers.value
                val prompt   = buildReportPrompt(dist, triggers)
                val report   = llmEngine.generate(prompt)
                _weeklyReport.value = report
                context.setWeeklyReport(report)
            } catch (e: Exception) {
                Log.e(TAG, "generateWeeklyReport failed", e)
                _reportError.value = e.message ?: "Report generation failed"
            } finally {
                _isGeneratingReport.value = false
            }
        }
    }

    private fun buildReportPrompt(
        dist: Map<MoodTag, Float>,
        triggers: List<TriggerCount>,
    ): String {
        val distStr = dist.entries.joinToString(", ") { (mood, pct) ->
            "${mood.name}: ${pct.toInt()}%"
        }
        val trigStr = triggers.take(5).joinToString(", ") { it.trigger }
        return """
            You are a compassionate mental wellness coach. Write a brief weekly insight report
            (3-4 sentences) based on this data:
            Mood distribution: $distStr
            Top triggers: $trigStr
            Start with an empathetic observation, then give one actionable suggestion.
        """.trimIndent()
    }
}
