package com.reflekt.journal.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")

fun Context.biometricEnabledFlow() =
    settingsDataStore.data.map { prefs -> prefs[BIOMETRIC_ENABLED_KEY] ?: false }

suspend fun Context.setBiometricEnabled(enabled: Boolean) {
    settingsDataStore.edit { prefs -> prefs[BIOMETRIC_ENABLED_KEY] = enabled }
}
