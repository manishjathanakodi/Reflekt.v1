package com.reflekt.journal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.reflekt.journal.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

private val LoraFont    = GoogleFont("Lora")
private val OutfitFont  = GoogleFont("Outfit")
private val JetBrainsMonoFont = GoogleFont("JetBrains Mono")

val LoraFamily = FontFamily(
    Font(googleFont = LoraFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = LoraFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = LoraFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = LoraFont, fontProvider = provider, weight = FontWeight.Bold),
)

val OutfitFamily = FontFamily(
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Bold),
)

val JetBrainsMonoFamily = FontFamily(
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Normal),
)

// Section 9.2 — Typography Scale
val ReflektTypography = Typography(
    // displayLarge — Lora 32sp Medium
    displayLarge = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Medium,
        fontSize   = 32.sp,
    ),
    // displayMedium — Lora 26sp Regular
    displayMedium = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 26.sp,
    ),
    // headlineMedium — Lora 22sp Regular
    headlineMedium = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 22.sp,
    ),
    // titleLarge — Outfit 20sp SemiBold
    titleLarge = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 20.sp,
    ),
    // titleMedium — Outfit 16sp SemiBold
    titleMedium = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp,
    ),
    // bodyLarge — Outfit 15sp Regular
    bodyLarge = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 15.sp,
    ),
    // bodyMedium — Outfit 13sp Regular
    bodyMedium = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
    ),
    // bodySmall — Outfit 11sp Regular
    bodySmall = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 11.sp,
    ),
    // labelMedium — Outfit 12sp SemiBold
    labelMedium = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 12.sp,
    ),
    // labelSmall — Outfit 10sp Bold
    labelSmall = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 10.sp,
    ),
)

// JetBrains Mono style — used for encryption keys, code, technical values
val MonoStyle = TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.Normal,
    fontSize   = 12.sp,
)
