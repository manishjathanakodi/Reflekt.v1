package com.reflekt.journal.ai.triage

import com.reflekt.journal.ai.engine.JournalAnalysis
import com.reflekt.journal.data.db.MoodLogDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriageEngine @Inject constructor(
    private val moodLogDao: MoodLogDao,
) {

    /**
     * Returns triage tier 1, 2, or 3.
     *
     * Tier 3: crisis keyword matched in rawText (INDEPENDENT of LLM), OR LLM returned tier 3.
     * Tier 2: 3+ consecutive MoodLog days with moodScore ≤ 2.5.
     * Tier 1: default.
     */
    suspend fun evaluate(analysis: JournalAnalysis, rawText: String): Int =
        withContext(Dispatchers.IO) {
            // Tier 3 — crisis keyword check runs independently of LLM
            if (containsCrisisKeyword(rawText)) return@withContext 3

            // Tier 3 — LLM flagged it
            if (analysis.triageTier == 3) return@withContext 3

            // Tier 2 — consecutive negative days
            val recentLogs = moodLogDao.getLastNDays(30).first()
            if (countConsecutiveNegativeDays(recentLogs) >= 3) return@withContext 2

            1
        }

    private fun containsCrisisKeyword(text: String): Boolean {
        val lower = text.lowercase()
        return CRISIS_KEYWORDS.any { keyword -> lower.contains(keyword) }
    }

    private fun countConsecutiveNegativeDays(logs: List<com.reflekt.journal.data.db.MoodLog>): Int {
        if (logs.isEmpty()) return 0
        // Sort newest first, walk backwards counting consecutive negative days
        val sorted = logs.sortedByDescending { it.date }
        var count = 0
        var expectedDate: LocalDate? = null
        for (log in sorted) {
            val logDate = LocalDate.parse(log.date)
            if (expectedDate != null && logDate != expectedDate) break // gap → stop
            if (log.moodScore <= 2.5f) {
                count++
                expectedDate = logDate.minusDays(1)
            } else {
                break // positive day breaks the streak
            }
        }
        return count
    }

    companion object {
        // CRITICAL: do NOT log or send these strings anywhere outside this file.
        val CRISIS_KEYWORDS: Set<String> = setOf(
            "hopeless",
            "hopelessness",
            "no reason to live",
            "self-harm",
            "self harm",
            "hurt myself",
            "hurting myself",
            "suicidal",
            "suicide",
            "end my life",
            "don't want to be here",
        )
    }
}
