package com.reflekt.journal.ui.screens.track

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.data.db.Goal
import com.reflekt.journal.data.db.GoalDao
import com.reflekt.journal.data.db.Habit
import com.reflekt.journal.data.db.HabitDao
import com.reflekt.journal.data.db.HabitLog
import com.reflekt.journal.data.db.HabitLogDao
import com.reflekt.journal.data.db.Todo
import com.reflekt.journal.data.db.TodoDao
import com.reflekt.journal.ui.receivers.HabitReminderReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

private const val TAG = "HabitsViewModel"

// ── Domain types ──────────────────────────────────────────────────────────────

data class HabitWithTodayStatus(val habit: Habit, val todayLog: HabitLog?) {
    val isDone: Boolean get() = todayLog?.status == "COMPLETED"
}

data class GoalWithProgress(val goal: Goal, val progressPercent: Float)

data class TodayProgress(val done: Int, val total: Int)

@Serializable
data class MilestoneItem(val id: String, val title: String, val isCompleted: Boolean)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val todoDao: TodoDao,
    private val goalDao: GoalDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val todayStr    = LocalDate.now().toString()
    private val todayDow    = LocalDate.now().dayOfWeek
    private val sevenAgo    = LocalDate.now().minusDays(7).toString()

    // Map habitId → list of logs for last 7 days (drives calendar strips)
    private val recentLogs: StateFlow<Map<String, List<HabitLog>>> =
        habitLogDao.getLogsForPeriod(sevenAgo)
            .map { logs -> logs.groupBy { it.habitId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val habits: StateFlow<List<HabitWithTodayStatus>> =
        combine(habitDao.getAll(), recentLogs) { allHabits, logsMap ->
            allHabits
                .filter { isHabitDueToday(it, todayDow) }
                .map { habit ->
                    val todayLog = logsMap[habit.habitId]?.find { it.date == todayStr }
                    HabitWithTodayStatus(habit, todayLog)
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayProgress: StateFlow<TodayProgress> = habits
        .map { list -> TodayProgress(list.count { it.isDone }, list.size) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayProgress(0, 0))

    val todos: StateFlow<List<Todo>> = todoDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeGoals: StateFlow<List<GoalWithProgress>> = goalDao.getAll()
        .map { goals -> goals.map { GoalWithProgress(it, it.progressPercent) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completedGoals: StateFlow<List<GoalWithProgress>> = goalDao.getAllIncludingArchived()
        .map { goals ->
            goals.filter { it.status == "COMPLETED" }
                .map { GoalWithProgress(it, it.progressPercent) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Returns last-7-days logs for a specific habit (read from cached state). */
    fun getLogsForHabit(habitId: String): List<HabitLog> =
        recentLogs.value[habitId] ?: emptyList()

    // ── Habit actions ─────────────────────────────────────────────────────────

    fun completeHabit(habitId: String, date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = habitLogDao.getByHabitAndDate(habitId, date)
            if (existing?.status == "COMPLETED") {
                // Toggle off → pending
                habitLogDao.upsert(existing.copy(status = "PENDING"))
            } else {
                habitLogDao.upsert(
                    HabitLog(
                        logId               = existing?.logId ?: UUID.randomUUID().toString(),
                        habitId             = habitId,
                        date                = date,
                        status              = "COMPLETED",
                        completedViaJournal = false,
                        note                = null,
                        moodAtCompletion    = null,
                    ),
                )
                val newStreak = habitLogDao.getStreakForHabit(habitId)
                val habit = habitDao.getById(habitId) ?: return@launch
                habitDao.upsert(
                    habit.copy(
                        streak        = newStreak,
                        longestStreak = maxOf(habit.longestStreak, newStreak),
                    ),
                )
            }
        }
    }

    // ── Todo actions ──────────────────────────────────────────────────────────

    fun completeTodo(todoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val todo = todoDao.getById(todoId) ?: return@launch
            todoDao.upsert(todo.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
        }
    }

    fun uncompleteTodo(todoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val todo = todoDao.getById(todoId) ?: return@launch
            todoDao.upsert(todo.copy(isCompleted = false, completedAt = null))
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val todo = todoDao.getById(todoId) ?: return@launch
            todoDao.upsert(todo.copy(isArchived = true))
        }
    }

    fun undoDeleteTodo(todoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val todo = todoDao.getById(todoId) ?: return@launch
            todoDao.upsert(todo.copy(isArchived = false))
        }
    }

    // ── Create actions ────────────────────────────────────────────────────────

    fun createHabit(
        title: String,
        emoji: String,
        frequency: String,
        customDays: List<Int>,
        reminderTime: String?,
        goalId: String?,
        color: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val habitId = UUID.randomUUID().toString()
            habitDao.insert(
                Habit(
                    habitId       = habitId,
                    title         = title,
                    emoji         = emoji,
                    frequency     = frequency,
                    customDaysJson = Json.encodeToString(customDays),
                    targetTime    = reminderTime,
                    goalId        = goalId,
                    color         = color,
                    streak        = 0,
                    longestStreak = 0,
                    isArchived    = false,
                    createdAt     = System.currentTimeMillis(),
                ),
            )
            reminderTime?.let { scheduleHabitReminder(habitId, title, emoji, it) }
        }
    }

    fun createTodo(
        title: String,
        description: String?,
        dueDate: String?,
        priority: String,
        goalId: String?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            todoDao.insert(
                Todo(
                    todoId             = UUID.randomUUID().toString(),
                    title              = title,
                    description        = description,
                    dueDate            = dueDate,
                    priority           = priority,
                    goalId             = goalId,
                    isCompleted        = false,
                    completedAt        = null,
                    completedViaJournal = false,
                    isArchived         = false,
                    createdAt          = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun createGoal(
        title: String,
        description: String?,
        emoji: String,
        targetDate: String?,
        color: String,
        milestones: List<MilestoneItem>,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            goalDao.insert(
                Goal(
                    goalId          = UUID.randomUUID().toString(),
                    title           = title,
                    description     = description,
                    emoji           = emoji,
                    targetDate      = targetDate,
                    color           = color,
                    milestonesJson  = Json.encodeToString(milestones),
                    status          = "ACTIVE",
                    progressPercent = 0f,
                    createdAt       = System.currentTimeMillis(),
                ),
            )
        }
    }

    // ── Alarm scheduling ──────────────────────────────────────────────────────

    private fun scheduleHabitReminder(habitId: String, title: String, emoji: String, timeStr: String) {
        try {
            val (hour, minute) = timeStr.split(":").map { it.toInt() }
            val triggerMs = LocalDate.now().atTime(hour, minute)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val finalMs = if (triggerMs <= System.currentTimeMillis())
                triggerMs + 24L * 60 * 60 * 1000 else triggerMs
            val intent = Intent(context, HabitReminderReceiver::class.java).apply {
                putExtra("habitId", habitId)
                putExtra("title", "$emoji $title")
            }
            val pi = PendingIntent.getBroadcast(
                context, habitId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalMs, pi)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule habit reminder", e)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isHabitDueToday(habit: Habit, dow: DayOfWeek): Boolean = when (habit.frequency) {
        "DAILY"    -> true
        "WEEKDAYS" -> dow in listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )
        "WEEKLY"   -> true
        "CUSTOM"   -> try {
            val days = Json.decodeFromString<List<Int>>(habit.customDaysJson)
            (dow.value - 1) in days
        } catch (_: Exception) { false }
        else -> true
    }

    // ── Pending undo state (for TodosTab snackbar) ────────────────────────────

    private val _pendingUndoTodoId = MutableStateFlow<String?>(null)
    val pendingUndoTodoId: StateFlow<String?> = _pendingUndoTodoId

    fun setPendingUndo(todoId: String?) { _pendingUndoTodoId.value = todoId }
}
