package com.reflekt.journal.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

private val BIOMETRIC_ENABLED_KEY      = booleanPreferencesKey("biometric_enabled")
private val WEEKLY_REPORT_KEY          = stringPreferencesKey("weekly_report")
private val NOTIFICATIONS_ENABLED_KEY  = booleanPreferencesKey("notifications_enabled")
private val DAILY_REMINDER_TIME_KEY    = stringPreferencesKey("daily_reminder_time")

fun Context.biometricEnabledFlow() =
    settingsDataStore.data.map { prefs -> prefs[BIOMETRIC_ENABLED_KEY] ?: false }

suspend fun Context.setBiometricEnabled(enabled: Boolean) {
    settingsDataStore.edit { prefs -> prefs[BIOMETRIC_ENABLED_KEY] = enabled }
}

fun Context.weeklyReportFlow() =
    settingsDataStore.data.map { prefs -> prefs[WEEKLY_REPORT_KEY] }

suspend fun Context.setWeeklyReport(report: String) {
    settingsDataStore.edit { prefs -> prefs[WEEKLY_REPORT_KEY] = report }
}

fun Context.notificationsEnabledFlow() =
    settingsDataStore.data.map { prefs -> prefs[NOTIFICATIONS_ENABLED_KEY] ?: true }

suspend fun Context.setNotificationsEnabled(enabled: Boolean) {
    settingsDataStore.edit { prefs -> prefs[NOTIFICATIONS_ENABLED_KEY] = enabled }
}

fun Context.dailyReminderTimeFlow() =
    settingsDataStore.data.map { prefs -> prefs[DAILY_REMINDER_TIME_KEY] ?: "21:00" }

suspend fun Context.setDailyReminderTime(time: String) {
    settingsDataStore.edit { prefs -> prefs[DAILY_REMINDER_TIME_KEY] = time }
}
