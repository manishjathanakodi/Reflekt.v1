package com.reflekt.journal.ai.accountability

import com.reflekt.journal.data.db.Goal
import com.reflekt.journal.data.db.GoalDao
import com.reflekt.journal.data.db.Habit
import com.reflekt.journal.data.db.HabitDao
import com.reflekt.journal.data.db.HabitLogDao
import com.reflekt.journal.data.db.Todo
import com.reflekt.journal.data.db.TodoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class AccountabilitySnapshot(
    val habitsDueToday: List<Habit>,
    val overdueHabits: List<Habit>,
    val todayTodos: List<Todo>,
    val activeGoals: List<Goal>,
)

@Singleton
class AccountabilityEngine @Inject constructor(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val todoDao: TodoDao,
    private val goalDao: GoalDao,
) {

    suspend fun buildSnapshot(userId: String): AccountabilitySnapshot =
        withContext(Dispatchers.IO) {
            val today = LocalDate.now()
            val todayDow = today.dayOfWeek
            val allHabits = habitDao.getAll().first()

            val habitsDueToday = allHabits.filter { isHabitDueToday(it, todayDow) }

            val overdueHabits = allHabits.filter { habit ->
                val yesterdayLog = habitLogDao.getByHabitAndDate(
                    habit.habitId,
                    today.minusDays(1).toString(),
                )
                // Overdue if no log yesterday, or yesterday was MISSED/PENDING
                yesterdayLog == null || yesterdayLog.status in listOf("MISSED", "PENDING")
            }

            val todayTodos = todoDao.getOverdue().first()
            val activeGoals = goalDao.getAll().first()

            AccountabilitySnapshot(
                habitsDueToday = habitsDueToday,
                overdueHabits = overdueHabits,
                todayTodos = todayTodos,
                activeGoals = activeGoals,
            )
        }

    private fun isHabitDueToday(habit: Habit, dayOfWeek: DayOfWeek): Boolean {
        return when (habit.frequency) {
            "DAILY" -> true
            "WEEKDAYS" -> dayOfWeek in listOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
            )
            "WEEKLY" -> true // show weekly habits every day; caller filters by last log
            "CUSTOM" -> try {
                // customDaysJson: day indices 0=Monday … 6=Sunday
                val days = Json.decodeFromString<List<Int>>(habit.customDaysJson)
                (dayOfWeek.value - 1) in days // DayOfWeek.MONDAY.value == 1
            } catch (e: Exception) {
                false
            }
            else -> true
        }
    }
}
