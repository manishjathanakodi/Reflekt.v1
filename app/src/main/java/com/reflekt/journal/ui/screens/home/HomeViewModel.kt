package com.reflekt.journal.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.data.db.Habit
import com.reflekt.journal.data.db.HabitDao
import com.reflekt.journal.data.db.HabitLogDao
import com.reflekt.journal.data.db.JournalEntryDao
import com.reflekt.journal.data.db.GoalDao
import com.reflekt.journal.data.db.TodoDao
import com.reflekt.journal.data.db.UserProfileDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.serialization.json.Json

data class HabitWithTodayStatus(val habit: Habit, val isDone: Boolean)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val journalEntryDao: JournalEntryDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val todoDao: TodoDao,
    private val goalDao: GoalDao,
    private val userProfileDao: UserProfileDao,
) : ViewModel() {

    private val todayStr = LocalDate.now().toString()
    private val todayDow = LocalDate.now().dayOfWeek

    val userName: StateFlow<String> = userProfileDao.getAll()
        .map { it.firstOrNull()?.name ?: "there" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "there")

    val recentEntries = journalEntryDao.getAll()
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayHabits: StateFlow<List<HabitWithTodayStatus>> = habitDao.getAll()
        .map { habits ->
            habits
                .filter { isHabitDueToday(it, todayDow) }
                .map { habit ->
                    val log = habitLogDao.getByHabitAndDate(habit.habitId, todayStr)
                    HabitWithTodayStatus(habit, log?.status == "COMPLETED")
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val journalStreak: StateFlow<Int> = journalEntryDao.getAll()
        .map { entries -> calculateStreak(entries.map { it.timestamp }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val activeGoalsCount: StateFlow<Int> = goalDao.getAll()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val overdueTodosCount: StateFlow<Int> = todoDao.getOverdue()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val lastEntryTier: StateFlow<Int> = journalEntryDao.getAll()
        .map { it.firstOrNull()?.triageTier ?: 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    val todayPrompt: StateFlow<String> = kotlinx.coroutines.flow.flow {
        val dayOfYear = LocalDate.now().dayOfYear
        emit(PROMPTS[dayOfYear % PROMPTS.size])
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PROMPTS[0])

    private fun isHabitDueToday(habit: Habit, dow: DayOfWeek): Boolean = when (habit.frequency) {
        "DAILY" -> true
        "WEEKDAYS" -> dow in listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        "WEEKLY" -> true
        "CUSTOM" -> try {
            val days = Json.decodeFromString<List<Int>>(habit.customDaysJson)
            (dow.value - 1) in days
        } catch (e: Exception) { false }
        else -> true
    }

    private fun calculateStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val entryDates = timestamps
            .map { LocalDate.ofEpochDay(it / 86_400_000L) }
            .toSortedSet()
        var current = LocalDate.now()
        if (!entryDates.contains(current)) current = current.minusDays(1)
        var streak = 0
        while (entryDates.contains(current)) {
            streak++
            current = current.minusDays(1)
        }
        return streak
    }

    companion object {
        private val PROMPTS = listOf(
            "How did today make you feel, and why?",
            "What's been weighing on your mind lately?",
            "Describe one moment today that felt meaningful.",
            "What are you grateful for right now?",
            "Is there something you've been avoiding thinking about?",
            "How is your energy level today, and what's driving it?",
            "What would make tomorrow feel like a win?",
            "Describe a recent interaction that stuck with you.",
            "What habit felt easy today, and what felt hard?",
            "If you could change one thing about today, what would it be?",
        )
    }
}
