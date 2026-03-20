package com.reflekt.journal.ai.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiResponseParserTest {

    private lateinit var parser: AiResponseParser

    @Before
    fun setup() {
        parser = AiResponseParser()
    }

    // ── JournalAnalysis ───────────────────────────────────────────────────────

    @Test
    fun testValidJson_allMoods() {
        for (mood in MoodTag.entries) {
            val json = """
                {
                  "summary": "Test summary",
                  "mood": "${mood.name}",
                  "moodScore": 3.5,
                  "triggers": ["work", "sleep"],
                  "triageTier": 1,
                  "clinicalFlags": []
                }
            """.trimIndent()
            val result = parser.parseJournalAnalysis(json)
            assertEquals("Expected mood $mood", mood, result.mood)
            assertEquals(3.5f, result.moodScore)
            assertEquals(listOf("work", "sleep"), result.triggers)
            assertEquals(1, result.triageTier)
        }
    }

    @Test
    fun testMalformedJson_returnsDefault() {
        val result = parser.parseJournalAnalysis("this is not json at all !!!")
        assertEquals(MoodTag.NEUTRAL, result.mood)
        assertEquals(3.0f, result.moodScore)
        assertEquals(1, result.triageTier)
        assertTrue(result.triggers.isEmpty())
        assertTrue(result.clinicalFlags.isEmpty())
    }

    @Test
    fun testMissingFields_returnsDefault() {
        // JSON with only partial fields — missing fields should fall back to defaults
        val json = """{"summary": "Partial response only"}"""
        val result = parser.parseJournalAnalysis(json)
        assertEquals("Partial response only", result.summary)
        assertEquals(MoodTag.NEUTRAL, result.mood)
        assertEquals(3.0f, result.moodScore)
        assertEquals(1, result.triageTier)
    }

    @Test
    fun testStripsMarkdownFences() {
        val wrapped = """
            ```json
            {
              "summary": "Fenced response",
              "mood": "HAPPY",
              "moodScore": 4.0,
              "triggers": [],
              "triageTier": 1,
              "clinicalFlags": []
            }
            ```
        """.trimIndent()
        val result = parser.parseJournalAnalysis(wrapped)
        assertEquals("Fenced response", result.summary)
        assertEquals(MoodTag.HAPPY, result.mood)
        assertEquals(4.0f, result.moodScore)
    }

    @Test
    fun testStripsMarkdownFences_noJsonLabel() {
        val wrapped = """
            ```
            {
              "summary": "Plain fence",
              "mood": "ANXIOUS",
              "moodScore": 2.0,
              "triggers": ["stress"],
              "triageTier": 2,
              "clinicalFlags": []
            }
            ```
        """.trimIndent()
        val result = parser.parseJournalAnalysis(wrapped)
        assertEquals("Plain fence", result.summary)
        assertEquals(MoodTag.ANXIOUS, result.mood)
        assertEquals(2, result.triageTier)
    }

    // ── AccountabilityResult ──────────────────────────────────────────────────

    @Test
    fun testValidAccountabilityJson() {
        val json = """
            {
              "completedHabitIds": ["h1", "h2"],
              "skippedHabitIds": ["h3"],
              "completedTodoIds": ["t1"],
              "habitInsight": "You exercised consistently today.",
              "encouragement": "Keep it up!"
            }
        """.trimIndent()
        val result = parser.parseAccountabilityResult(json)
        assertEquals(listOf("h1", "h2"), result.completedHabitIds)
        assertEquals(listOf("h3"), result.skippedHabitIds)
        assertEquals(listOf("t1"), result.completedTodoIds)
        assertEquals("You exercised consistently today.", result.habitInsight)
    }

    @Test
    fun testMalformedAccountabilityJson_returnsDefault() {
        val result = parser.parseAccountabilityResult("not valid json")
        assertTrue(result.completedHabitIds.isEmpty())
        assertTrue(result.skippedHabitIds.isEmpty())
        assertTrue(result.completedTodoIds.isEmpty())
        assertEquals("", result.habitInsight)
        assertEquals("", result.encouragement)
    }

    // ── stripMarkdownFences ───────────────────────────────────────────────────

    @Test
    fun testStripMarkdownFences_noFences_unchanged() {
        val raw = """{"key": "value"}"""
        assertEquals(raw, AiResponseParser.stripMarkdownFences(raw))
    }

    @Test
    fun testStripMarkdownFences_withJsonFence() {
        val raw = "```json\n{\"key\": \"value\"}\n```"
        assertEquals("{\"key\": \"value\"}", AiResponseParser.stripMarkdownFences(raw))
    }
}
