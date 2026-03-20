package com.reflekt.journal.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationManager: ReflektNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DAILY_REMINDER -> notificationManager.showDailyReminderNotification()
            ACTION_HABIT_REMINDER -> {
                val habitId    = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
                val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: "your habit"
                notificationManager.showHabitReminderNotification(habitId, habitTitle)
            }
            ACTION_STREAK_NUDGE -> {
                val habitId    = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
                val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: "your habit"
                notificationManager.showStreakAtRiskNotification(habitId, habitTitle)
            }
        }
    }

    companion object {
        const val ACTION_DAILY_REMINDER = "com.reflekt.journal.ACTION_DAILY_REMINDER"
        const val ACTION_HABIT_REMINDER = "com.reflekt.journal.ACTION_HABIT_REMINDER"
        const val ACTION_STREAK_NUDGE   = "com.reflekt.journal.ACTION_STREAK_NUDGE"
        const val EXTRA_HABIT_ID        = "extra_habit_id"
        const val EXTRA_HABIT_TITLE     = "extra_habit_title"
    }
}
