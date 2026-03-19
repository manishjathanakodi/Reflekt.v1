package com.reflekt.journal.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// ── Dark Palette ──────────────────────────────────────────────────────────────
val DarkBackground       = Color(0xFF0E1117)
val DarkSurface          = Color(0xFF161B27)
val DarkSurfaceVariant   = Color(0xFF1E2538)
val DarkSurfaceContainer = Color(0xFF252D44)
val DarkPrimary          = Color(0xFFC9A96E)
val DarkOnPrimary        = Color(0xFF1A1208)
val DarkPrimaryContainer = Color(0xFF2D3A1E)
val DarkSecondary        = Color(0xFF6FA880)
val DarkTertiary         = Color(0xFF9B85C8)
val DarkError            = Color(0xFFD4756A)
val DarkOutline          = Color(0xFF252D44)
val DarkOnBackground     = Color(0xFFEEEAE2)
val DarkOnSurface        = Color(0xFFEEEAE2)
val DarkOnSurfaceVariant = Color(0xFF9E9880)

// ── Mood Colours — Dark ───────────────────────────────────────────────────────
val MoodHappyDark   = Color(0xFF6FA880)
val MoodSadDark     = Color(0xFF5F9FC4)
val MoodAnxiousDark = Color(0xFF9B85C8)
val MoodAngryDark   = Color(0xFFD4756A)
val MoodNeutralDark = Color(0xFFC9A96E)
val MoodFearDark    = Color(0xFFA89080)

// Soft BG Dark (use with alpha)
val MoodHappySoftDark   = Color(0x266FA880)  // rgba(111,168,128,0.15)
val MoodSadSoftDark     = Color(0x265F9FC4)
val MoodAnxiousSoftDark = Color(0x269B85C8)
val MoodAngrySoftDark   = Color(0x26D4756A)
val MoodNeutralSoftDark = Color(0x26C9A96E)
val MoodFearSoftDark    = Color(0x26A89080)

// ── Light Palette ─────────────────────────────────────────────────────────────
// Per spec (CLAUDE.md): LightColorScheme uses darkColorScheme() as base;
// ONLY background and onBackground are overridden.
val LightBackground  = Color(0xFFF0EBE1)
val LightOnBackground = Color(0xFF2C2318)

// ── Mood Colours — Light ──────────────────────────────────────────────────────
val MoodHappyLight   = Color(0xFF4A9E6A)
val MoodSadLight     = Color(0xFF5A90C0)
val MoodAnxiousLight = Color(0xFF8B79C2)
val MoodAngryLight   = Color(0xFFC07068)
val MoodNeutralLight = Color(0xFFB8986A)
val MoodFearLight    = Color(0xFF8A7B6A)

// Soft BG Light
val MoodHappySoftLight   = Color(0xFFE8F5EE)
val MoodSadSoftLight     = Color(0xFFE3EFF8)
val MoodAnxiousSoftLight = Color(0xFFEEE9F8)
val MoodAngrySoftLight   = Color(0xFFF8EEEC)
val MoodNeutralSoftLight = Color(0xFFF5EDE0)
val MoodFearSoftLight    = Color(0xFFEDE9E3)

// ── Colour Schemes ────────────────────────────────────────────────────────────
val DarkColorScheme = darkColorScheme(
    background        = DarkBackground,
    surface           = DarkSurface,
    surfaceVariant    = DarkSurfaceVariant,
    surfaceContainer  = DarkSurfaceContainer,
    primary           = DarkPrimary,
    onPrimary         = DarkOnPrimary,
    primaryContainer  = DarkPrimaryContainer,
    secondary         = DarkSecondary,
    tertiary          = DarkTertiary,
    error             = DarkError,
    outline           = DarkOutline,
    onBackground      = DarkOnBackground,
    onSurface         = DarkOnSurface,
    onSurfaceVariant  = DarkOnSurfaceVariant,
)

// Light: dark base + only background/onBackground overridden (CLAUDE.md hard rule)
val LightColorScheme = darkColorScheme(
    background        = LightBackground,
    surface           = DarkSurface,
    surfaceVariant    = DarkSurfaceVariant,
    surfaceContainer  = DarkSurfaceContainer,
    primary           = DarkPrimary,
    onPrimary         = DarkOnPrimary,
    primaryContainer  = DarkPrimaryContainer,
    secondary         = DarkSecondary,
    tertiary          = DarkTertiary,
    error             = DarkError,
    outline           = DarkOutline,
    onBackground      = LightOnBackground,
    onSurface         = DarkOnSurface,
    onSurfaceVariant  = DarkOnSurfaceVariant,
)
