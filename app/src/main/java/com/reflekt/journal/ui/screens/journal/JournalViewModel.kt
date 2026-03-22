package com.reflekt.journal.ui.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.ai.engine.JournalSessionStore
import com.reflekt.journal.ai.engine.MoodTag
import com.reflekt.journal.data.db.JournalEntry
import com.reflekt.journal.data.db.JournalEntryDao
import com.reflekt.journal.data.db.MoodLog
import com.reflekt.journal.data.db.MoodLogDao
import com.reflekt.journal.data.db.UserProfileDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

sealed interface JournalNavEvent {
    object NavigateToSaved : JournalNavEvent
    object NavigateToCrisis : JournalNavEvent
    object ShowClosingDialog : JournalNavEvent
}

data class JournalFormState(
    val initialMood: MoodTag? = null,
    val affirmation: String = "",
    val gratitude1: String = "",
    val gratitude2: String = "",
    val gratitude3: String = "",
    val bestPartOfDay: String = "",
    val challenge: String = "",
    val freeWrite: String = "",
    val tomorrowIntent: String = "",
    val closingMood: MoodTag? = null,
)

data class StructuredSaveState(
    val initialMood: MoodTag?,
    val closingMood: MoodTag?,
    val affirmation: String,
    val quote: String,
    val filledSections: Int,
)

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalEntryDao: JournalEntryDao,
    private val moodLogDao: MoodLogDao,
    private val userProfileDao: UserProfileDao,
    private val sessionStore: JournalSessionStore,
) : ViewModel() {

    private val _formState = MutableStateFlow(JournalFormState())
    val formState: StateFlow<JournalFormState> = _formState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _structuredSaveState = MutableStateFlow<StructuredSaveState?>(null)
    val structuredSaveState: StateFlow<StructuredSaveState?> = _structuredSaveState.asStateFlow()

    private val _navEvent = Channel<JournalNavEvent>(Channel.BUFFERED)
    val navEvent = _navEvent.receiveAsFlow()

    val selectedQuote: String

    init {
        val pendingMood = sessionStore.pendingInitialMood
        sessionStore.pendingInitialMood = null
        if (pendingMood != null) {
            _formState.value = _formState.value.copy(initialMood = pendingMood)
        }
        val dayOfYear = LocalDate.now().dayOfYear
        selectedQuote = QUOTES[dayOfYear % QUOTES.size]
    }

    fun onAffirmationChanged(text: String) {
        _formState.value = _formState.value.copy(affirmation = text)
    }

    fun onGratitude1Changed(text: String) {
        _formState.value = _formState.value.copy(gratitude1 = text)
    }

    fun onGratitude2Changed(text: String) {
        _formState.value = _formState.value.copy(gratitude2 = text)
    }

    fun onGratitude3Changed(text: String) {
        _formState.value = _formState.value.copy(gratitude3 = text)
    }

    fun onBestPartChanged(text: String) {
        _formState.value = _formState.value.copy(bestPartOfDay = text)
    }

    fun onChallengeChanged(text: String) {
        _formState.value = _formState.value.copy(challenge = text)
    }

    fun onFreeWriteChanged(text: String) {
        _formState.value = _formState.value.copy(freeWrite = text)
    }

    fun onTomorrowIntentChanged(text: String) {
        _formState.value = _formState.value.copy(tomorrowIntent = text)
    }

    fun onSave() {
        if (_isSaving.value) return
        viewModelScope.launch {
            _navEvent.send(JournalNavEvent.ShowClosingDialog)
        }
    }

    fun onClosingMoodSet(mood: MoodTag) {
        _formState.value = _formState.value.copy(closingMood = mood)
        saveEntry()
    }

    fun onClosingDialogSkipped() {
        saveEntry()
    }

    private fun saveEntry() {
        if (_isSaving.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            try {
                val form = _formState.value
                val rawText = buildRawText(form)
                val moodTag = form.closingMood ?: form.initialMood ?: MoodTag.NEUTRAL
                val score = moodScore(moodTag)
                val tier = computeTier(form, rawText)
                val aiSummary = buildSummary(form)
                val entryId = UUID.randomUUID().toString()

                val entry = JournalEntry(
                    entryId = entryId,
                    timestamp = System.currentTimeMillis(),
                    rawText = rawText,
                    conversationJson = "[]",
                    aiSummary = aiSummary,
                    moodTag = moodTag.name,
                    moodScore = score,
                    triggersJson = "[]",
                    triageTier = tier,
                    clinicalSummaryJson = null,
                    totalScreenTimeMs = 0L,
                    isDeleted = false,
                    initialMood = form.initialMood?.name,
                    closingMood = form.closingMood?.name,
                    affirmation = form.affirmation.ifBlank { null },
                    gratitude1 = form.gratitude1.ifBlank { null },
                    gratitude2 = form.gratitude2.ifBlank { null },
                    gratitude3 = form.gratitude3.ifBlank { null },
                    bestPartOfDay = form.bestPartOfDay.ifBlank { null },
                    challenge = form.challenge.ifBlank { null },
                    tomorrowIntent = form.tomorrowIntent.ifBlank { null },
                    freeWrite = form.freeWrite.ifBlank { null },
                    quote = selectedQuote,
                )
                journalEntryDao.insert(entry)

                // Update MoodLog for today
                val todayStr = LocalDate.now().toString()
                val existingLog = moodLogDao.getByDate(todayStr)
                val dayStart = java.time.LocalDate.now()
                    .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val dayEnd = dayStart + 24L * 60 * 60 * 1000
                val todayEntries = journalEntryDao.getEntriesForDay(dayStart, dayEnd)
                val avgScore = if (todayEntries.isNotEmpty())
                    todayEntries.map { it.moodScore }.average().toFloat()
                else score
                val dominantMoodStr = todayEntries.groupBy { it.moodTag }
                    .maxByOrNull { it.value.size }?.key ?: moodTag.name
                moodLogDao.upsert(
                    MoodLog(
                        logId = existingLog?.logId ?: UUID.randomUUID().toString(),
                        date = todayStr,
                        moodScore = avgScore,
                        dominantMood = dominantMoodStr,
                        primaryTrigger = "",
                        screenTimeMs = 0L,
                        entryCount = todayEntries.size,
                    ),
                )

                _structuredSaveState.value = StructuredSaveState(
                    initialMood = form.initialMood,
                    closingMood = form.closingMood,
                    affirmation = form.affirmation,
                    quote = selectedQuote,
                    filledSections = countFilledSections(form),
                )

                if (tier == 3) {
                    _navEvent.send(JournalNavEvent.NavigateToCrisis)
                } else {
                    _navEvent.send(JournalNavEvent.NavigateToSaved)
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun countFilledSections(form: JournalFormState): Int {
        var count = 0
        if (form.affirmation.isNotBlank()) count++
        if (form.gratitude1.isNotBlank() || form.gratitude2.isNotBlank() || form.gratitude3.isNotBlank()) count++
        if (form.bestPartOfDay.isNotBlank()) count++
        if (form.challenge.isNotBlank()) count++
        if (form.freeWrite.isNotBlank()) count++
        if (form.tomorrowIntent.isNotBlank()) count++
        return count
    }

    private fun buildRawText(form: JournalFormState): String {
        val parts = mutableListOf<String>()
        form.initialMood?.let { parts.add("Opening mood: ${it.name.lowercase()}") }
        if (form.affirmation.isNotBlank()) parts.add("Affirmation: ${form.affirmation}")
        if (form.gratitude1.isNotBlank()) parts.add("Grateful for: ${form.gratitude1}")
        if (form.gratitude2.isNotBlank()) parts.add("Grateful for: ${form.gratitude2}")
        if (form.gratitude3.isNotBlank()) parts.add("Grateful for: ${form.gratitude3}")
        if (form.bestPartOfDay.isNotBlank()) parts.add("Best part of today: ${form.bestPartOfDay}")
        if (form.challenge.isNotBlank()) parts.add("Challenge: ${form.challenge}")
        if (form.freeWrite.isNotBlank()) parts.add(form.freeWrite)
        if (form.tomorrowIntent.isNotBlank()) parts.add("Tomorrow I intend to: ${form.tomorrowIntent}")
        form.closingMood?.let { parts.add("Closing mood: ${it.name.lowercase()}") }
        return parts.joinToString("\n\n")
    }

    private fun buildSummary(form: JournalFormState): String =
        form.affirmation.ifBlank {
            form.gratitude1.ifBlank {
                form.bestPartOfDay.ifBlank {
                    form.freeWrite.take(120).ifBlank { "" }
                }
            }
        }

    private fun computeTier(form: JournalFormState, rawText: String): Int {
        val lower = rawText.lowercase()
        val crisisKeywords = listOf(
            "suicide", "kill myself", "end my life", "self-harm",
            "hurt myself", "don't want to live", "want to die",
        )
        if (crisisKeywords.any { lower.contains(it) }) return 3
        val distressed = setOf(MoodTag.SAD, MoodTag.FEAR, MoodTag.ANGRY, MoodTag.ANXIOUS)
        if (form.closingMood in distressed) return 2
        return 1
    }

    private fun moodScore(mood: MoodTag): Float = when (mood) {
        MoodTag.HAPPY   -> 8.0f
        MoodTag.NEUTRAL -> 5.0f
        MoodTag.SAD     -> 3.0f
        MoodTag.ANXIOUS -> 4.0f
        MoodTag.ANGRY   -> 3.5f
        MoodTag.FEAR    -> 2.5f
    }

    companion object {
        val QUOTES = listOf(
            "The present moment always will have been. — Simone Weil",
            "You are enough, just as you are.",
            "Small steps in the right direction are still steps forward.",
            "Feelings are visitors. Let them come and go.",
            "You don't have to be positive all the time.",
            "Growth and comfort cannot coexist. — Ginni Rometty",
            "Be kind to yourself — you are doing your best.",
            "What you are looking for is already in you.",
            "Healing is not linear.",
            "Every day is a fresh start.",
            "The bravest thing you can do is begin again.",
            "You are worthy of the love you give others.",
            "Rest is productive.",
            "Clarity comes with kindness to yourself.",
            "Your story isn't over yet.",
            "Progress, not perfection.",
            "You've survived 100% of your worst days so far.",
            "Self-awareness is the beginning of growth.",
            "It's okay to not have all the answers.",
            "Your emotions are valid.",
            "Difficult roads often lead to beautiful destinations.",
            "Take it one breath at a time.",
            "You are stronger than you think.",
            "The mind is powerful — be gentle with it.",
            "Gratitude turns what we have into enough.",
            "Reflect. Grow. Repeat.",
            "Every thought you write down loses a little of its power over you.",
            "Vulnerability is not weakness — it's courage.",
            "You matter. Your feelings matter.",
            "Today I choose to begin.",
        )
    }
}
