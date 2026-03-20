package com.reflekt.journal.ai.triage

import com.reflekt.journal.ai.engine.JournalAnalysis
import com.reflekt.journal.ai.engine.MoodTag
import com.reflekt.journal.data.db.MoodLog
import com.reflekt.journal.data.db.MoodLogDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class TriageEngineTest {

    private lateinit var fakeMoodLogDao: FakeMoodLogDao
    private lateinit var engine: TriageEngine

    @Before
    fun setup() {
        fakeMoodLogDao = FakeMoodLogDao()
        engine = TriageEngine(fakeMoodLogDao)
    }

    // ── Tier 1 ────────────────────────────────────────────────────────────────

    @Test
    fun testTier1_noKeywords_noStreak() = runTest {
        fakeMoodLogDao.setLogs(
            moodLog("2026-03-15", 4.0f),
            moodLog("2026-03-16", 4.5f),
        )
        val analysis = JournalAnalysis(triageTier = 1, mood = MoodTag.HAPPY, moodScore = 4.0f)
        assertEquals(1, engine.evaluate(analysis, "Feeling great today!"))
    }

    // ── Tier 2 ────────────────────────────────────────────────────────────────

    @Test
    fun testTier2_threeConsecutiveNegativeDays() = runTest {
        val today = LocalDate.now()
        fakeMoodLogDao.setLogs(
            moodLog(today.toString(), 2.0f),
            moodLog(today.minusDays(1).toString(), 2.0f),
            moodLog(today.minusDays(2).toString(), 2.0f),
        )
        val analysis = JournalAnalysis(triageTier = 1, mood = MoodTag.SAD, moodScore = 2.0f)
        assertEquals(2, engine.evaluate(analysis, "Feeling down again."))
    }

    @Test
    fun testTier1_twoNegativeDays_notEnough() = runTest {
        val today = LocalDate.now()
        fakeMoodLogDao.setLogs(
            moodLog(today.toString(), 2.0f),
            moodLog(today.minusDays(1).toString(), 2.0f),
        )
        val analysis = JournalAnalysis(triageTier = 1, mood = MoodTag.SAD, moodScore = 2.0f)
        assertEquals(1, engine.evaluate(analysis, "A bit low today."))
    }

    // ── Tier 3 ────────────────────────────────────────────────────────────────

    @Test
    fun testTier3_crisisKeywordInText() = runTest {
        fakeMoodLogDao.setLogs()
        val analysis = JournalAnalysis(triageTier = 1, mood = MoodTag.NEUTRAL, moodScore = 3.0f)
        assertEquals(3, engine.evaluate(analysis, "I've been feeling suicidal lately."))
    }

    @Test
    fun testTier3_llmReturnsTier3() = runTest {
        fakeMoodLogDao.setLogs()
        val analysis = JournalAnalysis(triageTier = 3, mood = MoodTag.SAD, moodScore = 1.5f)
        assertEquals(3, engine.evaluate(analysis, "Just feeling really bad."))
    }

    @Test
    fun testTier3_keywordOverridesLlmTier1() = runTest {
        fakeMoodLogDao.setLogs()
        // LLM says tier 1 — but keyword match must still return tier 3
        val analysis = JournalAnalysis(triageTier = 1, mood = MoodTag.NEUTRAL, moodScore = 3.0f)
        assertEquals(3, engine.evaluate(analysis, "I want to end my life."))
    }

    @Test
    fun testAllCrisisKeywords() = runTest {
        val analysis = JournalAnalysis(triageTier = 1, mood = MoodTag.NEUTRAL, moodScore = 3.0f)
        for (keyword in TriageEngine.CRISIS_KEYWORDS) {
            fakeMoodLogDao.setLogs()
            val result = engine.evaluate(analysis, "I feel $keyword right now.")
            assertEquals("Expected tier 3 for keyword: '$keyword'", 3, result)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun moodLog(date: String, score: Float) = MoodLog(
        logId = "log_$date",
        date = date,
        moodScore = score,
        dominantMood = if (score <= 2.5f) "SAD" else "HAPPY",
        primaryTrigger = "",
        screenTimeMs = 0L,
        entryCount = 1,
    )
}

// ── Fake DAO ──────────────────────────────────────────────────────────────────

class FakeMoodLogDao : MoodLogDao {
    private var logs: List<MoodLog> = emptyList()

    fun setLogs(vararg entries: MoodLog) {
        logs = entries.toList()
    }

    override suspend fun insert(log: MoodLog) {}
    override suspend fun upsert(log: MoodLog) {}
    override suspend fun delete(log: MoodLog) {}
    override fun getAll(): Flow<List<MoodLog>> = flowOf(logs)
    override suspend fun getById(logId: String): MoodLog? = logs.find { it.logId == logId }
    override fun getLastNDays(n: Int): Flow<List<MoodLog>> = flowOf(logs)
}
