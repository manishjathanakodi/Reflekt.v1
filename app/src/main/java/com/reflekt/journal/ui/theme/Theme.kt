package com.reflekt.journal.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

// ── ThemePreference ───────────────────────────────────────────────────────────
enum class ThemePreference { SYSTEM, LIGHT, DARK }

// ── DataStore extension ───────────────────────────────────────────────────────
val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

val THEME_PREFERENCE_KEY = stringPreferencesKey("theme_preference")

fun Context.themePreferenceFlow() = themeDataStore.data.map { prefs ->
    val raw = prefs[THEME_PREFERENCE_KEY] ?: ThemePreference.SYSTEM.name
    ThemePreference.valueOf(raw)
}

// ── ReflektTheme ──────────────────────────────────────────────────────────────
@Composable
fun ReflektTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val themePreference by context.themePreferenceFlow()
        .collectAsState(initial = ThemePreference.SYSTEM)

    val systemDark = isSystemInDarkTheme()

    val useDark = when (themePreference) {
        ThemePreference.DARK   -> true
        ThemePreference.LIGHT  -> false
        ThemePreference.SYSTEM -> systemDark
    }

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = ReflektTypography,
        shapes      = ReflektShapes,
        content     = content,
    )
}
