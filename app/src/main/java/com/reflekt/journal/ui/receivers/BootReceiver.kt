package com.reflekt.journal.ui.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reflekt.journal.data.db.HabitDao
import com.reflekt.journal.data.preferences.dailyReminderTimeFlow
import com.reflekt.journal.data.preferences.notificationsEnabledFlow
import com.reflekt.journal.ui.notifications.ReflektNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationManager: ReflektNotificationManager
    @Inject lateinit var habitDao: HabitDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            val notificationsEnabled = context.notificationsEnabledFlow().firstOrNull() ?: true
            if (!notificationsEnabled) return@launch

            // Reschedule daily reminder
            val timeStr = context.dailyReminderTimeFlow().firstOrNull() ?: "21:00"
            val time    = runCatching {
                val parts = timeStr.split(":")
                LocalTime.of(parts[0].toInt(), parts[1].toInt())
            }.getOrDefault(LocalTime.of(21, 0))
            notificationManager.scheduleDailyReminder(time)

            // Reschedule habit reminders
            val habits = habitDao.getAll().firstOrNull() ?: emptyList()
            habits.filter { !it.isArchived }.forEach { habit ->
                if (habit.targetTime != null) {
                    notificationManager.scheduleHabitReminder(habit)
                }
            }
        }
    }
}
