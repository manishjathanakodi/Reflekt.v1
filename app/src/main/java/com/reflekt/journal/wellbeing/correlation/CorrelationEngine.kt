package com.reflekt.journal.wellbeing.correlation

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.reflekt.journal.ai.engine.LlmEngine
import com.reflekt.journal.data.db.AppUsageLog
import com.reflekt.journal.data.db.AppUsageLogDao
import com.reflekt.journal.data.db.MoodLogDao
import com.reflekt.journal.data.preferences.settingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CorrelationEngine"

val WELLBEING_INSIGHT_KEY       = stringPreferencesKey("wellbeing_insight")
val LAST_CORRELATION_DATE_KEY   = stringPreferencesKey("last_correlation_date")

@Singleton
class CorrelationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUsageLogDao: AppUsageLogDao,
    private val moodLogDao: MoodLogDao,
    private val llmEngine: LlmEngine,
) {

    suspend fun runCorrelation(userId: String) {
        val today = LocalDate.now().toString()

        // Only run once per day
        val lastRun = context.settingsDataStore.data
            .map { it[LAST_CORRELATION_DATE_KEY] }
            .firstOrNull()
        if (lastRun == today) {
            Log.d(TAG, "Correlation already run today — skipping")
            return
        }

        val usageLogs = appUsageLogDao.getAll().firstOrNull() ?: emptyList()
        val last7DayUsage = usageLogs.filter { log ->
            val logDate = LocalDate.parse(log.date)
            logDate >= LocalDate.now().minusDays(7)
        }

        val moodLogs = moodLogDao.getLastNDays(7).firstOrNull() ?: emptyList()

        if (last7DayUsage.isEmpty() || moodLogs.isEmpty()) {
            Log.d(TAG, "Insufficient data for correlation")
            return
        }

        // Build grouped usage summary per app
        val usageByApp = last7DayUsage.groupBy { it.packageName }
        val usageSummary = usageByApp.entries.take(10).joinToString("\n") { (pkg, logs) ->
            val totalMins = logs.sumOf { it.durationMs } / 60_000
            val label = logs.firstOrNull()?.appLabel ?: pkg
            "$label: ${totalMins}min total (${logs.size} days)"
        }

        val moodSummary = moodLogs.joinToString("\n") { log ->
            "${log.date}: score=${log.moodScore} mood=${log.dominantMood}"
        }

        val prompt = """
            Analyze the correlation between app usage and mood for a journaling user.

            App usage (last 7 days):
            $usageSummary

            Mood logs (last 7 days):
            $moodSummary

            Identify which apps negatively correlate with mood (impactScore -1.0 to 0, where -1 is worst).
            Respond ONLY in this JSON format with no extra text:
            {
              "triggerApps": [
                {"packageName": "com.instagram.android", "impactScore": -0.75},
                {"packageName": "com.twitter.android", "impactScore": -0.42}
              ],
              "insight": "Your mood drops 38% on days you use Instagram 60+ min."
            }
        """.trimIndent()

        val response = try {
            llmEngine.generate(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "LLM generation failed", e)
            buildFallbackInsight(last7DayUsage, moodLogs.map { it.moodScore }.average().toFloat())
            return
        }

        parseAndApply(response, usageByApp)

        context.settingsDataStore.edit { prefs ->
            prefs[LAST_CORRELATION_DATE_KEY] = today
        }
    }

    private suspend fun parseAndApply(
        response: String,
        usageByApp: Map<String, List<AppUsageLog>>,
    ) {
        try {
            val triggerAppsJson = Regex(""""triggerApps"\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
                .find(response)?.groupValues?.getOrNull(1) ?: ""

            val entries = Regex("""\{[^}]+\}""").findAll(triggerAppsJson)
            val triggerPackages = mutableMapOf<String, Float>()

            entries.forEach { match ->
                val pkg   = Regex(""""packageName"\s*:\s*"([^"]+)"""").find(match.value)?.groupValues?.getOrNull(1) ?: return@forEach
                val score = Regex(""""impactScore"\s*:\s*(-?[\d.]+)""").find(match.value)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: return@forEach
                triggerPackages[pkg] = score
            }

            // Update all usage logs with new impact scores
            triggerPackages.forEach { (pkg, score) ->
                val logs = usageByApp[pkg] ?: return@forEach
                logs.forEach { log ->
                    val updated = log.copy(
                        impactScore  = score,
                        isTriggerApp = score < -0.4f,
                    )
                    appUsageLogDao.upsert(updated)
                }
            }

            val insight = Regex(""""insight"\s*:\s*"([^"]+)"""").find(response)?.groupValues?.getOrNull(1)
                ?: "Your screen time patterns are being analysed."

            context.settingsDataStore.edit { prefs ->
                prefs[WELLBEING_INSIGHT_KEY] = insight
            }

            Log.d(TAG, "Correlation applied: ${triggerPackages.size} trigger apps updated")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse correlation response", e)
        }
    }

    private suspend fun buildFallbackInsight(usage: List<AppUsageLog>, avgMood: Float) {
        val topApp = usage.maxByOrNull { it.durationMs }
        val insight = if (topApp != null) {
            val mins = topApp.durationMs / 60_000
            "You spend most time on ${topApp.appLabel} (${mins}min/day). Monitor if this affects your mood score of ${"%.1f".format(avgMood)}/10."
        } else {
            "Tracking your screen time alongside mood entries helps uncover patterns."
        }
        context.settingsDataStore.edit { prefs ->
            prefs[WELLBEING_INSIGHT_KEY] = insight
        }
    }
}
