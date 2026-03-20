package com.reflekt.journal.ui.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.reflekt.journal.MainActivity
import com.reflekt.journal.data.db.Habit
import java.time.LocalTime
import java.util.Calendar
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

const val CHANNEL_REMINDERS = "reflekt_reminders"
const val CHANNEL_NUDGES    = "reflekt_nudges"

private const val REQUEST_DAILY_REMINDER    = 1000
private const val REQUEST_HABIT_BASE        = 2000
private const val REQUEST_STREAK_RISK_BASE  = 3000

@Singleton
class ReflektNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // ── Channel creation (called from Application.onCreate) ───────────────────

    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            listOf(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Daily Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Daily journal prompt and habit reminders" },
                NotificationChannel(
                    CHANNEL_NUDGES,
                    "Streak Nudges",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "Streak at-risk nudges" },
            ).forEach { notifManager.createNotificationChannel(it) }
        }
    }

    // ── Daily journal reminder ─────────────────────────────────────────────────

    fun scheduleDailyReminder(time: LocalTime) {
        val triggerMs = nextOccurrenceMs(time.hour, time.minute)
        val intent    = buildLaunchIntent(REQUEST_DAILY_REMINDER)
        scheduleExact(intent, triggerMs, REQUEST_DAILY_REMINDER)
    }

    fun cancelDailyReminder() {
        alarmManager.cancel(buildLaunchIntent(REQUEST_DAILY_REMINDER))
    }

    // ── Habit reminder ────────────────────────────────────────────────────────

    fun scheduleHabitReminder(habit: Habit) {
        val targetTime = habit.targetTime ?: return
        val (hour, minute) = parseTime(targetTime)
        val requestCode = REQUEST_HABIT_BASE + habit.habitId.hashCode().and(0xFFFF)
        val triggerMs   = nextOccurrenceMs(hour, minute)
        val intent      = buildHabitIntent(habit.habitId, requestCode)
        scheduleExact(intent, triggerMs, requestCode)
    }

    fun cancelHabitReminder(habitId: String) {
        val requestCode = REQUEST_HABIT_BASE + habitId.hashCode().and(0xFFFF)
        alarmManager.cancel(buildHabitIntent(habitId, requestCode))
    }

    // ── Streak at-risk nudge ──────────────────────────────────────────────────

    fun scheduleStreakAtRiskNudge(habit: Habit) {
        // Fires 2 hours before midnight if the habit hasn't been logged today
        val requestCode = REQUEST_STREAK_RISK_BASE + habit.habitId.hashCode().and(0xFFFF)
        val triggerMs   = nextOccurrenceMs(22, 0)
        val intent      = buildStreakNudgeIntent(habit.habitId, habit.title, requestCode)
        scheduleExact(intent, triggerMs, requestCode)
    }

    fun cancelStreakAtRiskNudge(habitId: String) {
        val requestCode = REQUEST_STREAK_RISK_BASE + habitId.hashCode().and(0xFFFF)
        alarmManager.cancel(buildStreakNudgeIntent(habitId, "", requestCode))
    }

    // ── Immediate notification (used by AlarmReceiver) ────────────────────────

    fun showDailyReminderNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time to reflect ✍️")
            .setContentText("How are you feeling today? Take a moment to journal.")
            .setAutoCancel(true)
            .setContentIntent(mainActivityPendingIntent())
            .build()
        notifManager.notify(REQUEST_DAILY_REMINDER, notification)
    }

    fun showHabitReminderNotification(habitId: String, habitTitle: String) {
        val requestCode = REQUEST_HABIT_BASE + habitId.hashCode().and(0xFFFF)
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Habit reminder ✅")
            .setContentText("Don't forget: $habitTitle")
            .setAutoCancel(true)
            .setContentIntent(mainActivityPendingIntent())
            .build()
        notifManager.notify(requestCode, notification)
    }

    fun showStreakAtRiskNotification(habitId: String, habitTitle: String) {
        val requestCode = REQUEST_STREAK_RISK_BASE + habitId.hashCode().and(0xFFFF)
        val notification = NotificationCompat.Builder(context, CHANNEL_NUDGES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔥 Streak at risk!")
            .setContentText("Log $habitTitle before midnight to keep your streak.")
            .setAutoCancel(true)
            .setContentIntent(mainActivityPendingIntent())
            .build()
        notifManager.notify(requestCode, notification)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun scheduleExact(pendingIntent: PendingIntent, triggerAtMs: Long, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Fallback to inexact if exact alarm permission not granted
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
    }

    private fun nextOccurrenceMs(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun parseTime(timeStr: String): Pair<Int, Int> {
        return runCatching {
            val parts = timeStr.split(":")
            parts[0].toInt() to parts[1].toInt()
        }.getOrDefault(21 to 0) // default 9 PM
    }

    private fun buildLaunchIntent(requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DAILY_REMINDER
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildHabitIntent(habitId: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_HABIT_REMINDER
            putExtra(AlarmReceiver.EXTRA_HABIT_ID, habitId)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildStreakNudgeIntent(habitId: String, habitTitle: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_STREAK_NUDGE
            putExtra(AlarmReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(AlarmReceiver.EXTRA_HABIT_TITLE, habitTitle)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun mainActivityPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
