package com.reflekt.journal.ui.screens.onboarding

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.reflekt.journal.data.preferences.setBiometricEnabled
import com.reflekt.journal.ui.components.OnboardingProgressBar
import com.reflekt.journal.ui.components.PrimaryButton
import com.reflekt.journal.ui.components.ZeroCloudBadge
import com.reflekt.journal.ui.navigation.Routes
import kotlinx.coroutines.launch

private val Gold = Color(0xFFC9A96E)
private val SageGreen = Color(0xFF6FA880)
private val Lavender = Color(0xFF9B85C8)
private val CardSurface = Color(0xFF1E2538)
private val CardText = Color(0xFFEEEAE2)
private val CardSubSurface = Color(0xFF252D44)

@Composable
fun PermissionsScreen(
    viewModel: OnboardingViewModel,
    navController: NavController,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var biometricEnabled by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled passively */ }

    fun navigateHome() {
        viewModel.saveProfile {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.ONBOARDING_DEMO) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        OnboardingProgressBar(currentStep = 4, totalSteps = 4)

        Text(
            text = "✦ Setup · Step 4 of 4".toUpperCase(Locale.current),
            style = MaterialTheme.typography.labelSmall,
            color = Gold,
            letterSpacing = androidx.compose.ui.unit.TextUnit(0.1f, androidx.compose.ui.unit.TextUnitType.Em),
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Allow Reflekt to help you fully",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "These permissions let Reflekt connect your digital habits to your emotional wellbeing. Everything stays on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Card 1 — Screen Time Access
        PermissionCard(
            iconEmoji = "📱",
            iconTint = CardSubSurface,
            title = "Screen Time Access",
            description = "Reads app usage stats to find links between your digital habits and mood patterns.",
            action = {
                PrimaryButton(
                    text = "Allow in Settings →",
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                )
            },
        )

        // Card 2 — Notifications
        PermissionCard(
            iconEmoji = "🔔",
            iconTint = SageGreen.copy(alpha = 0.15f),
            title = "Notifications",
            description = "Sends AI-generated check-in prompts and wellbeing nudges at the right moment.",
            action = {
                PrimaryButton(
                    text = "Allow Notifications",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    containerColor = SageGreen,
                    contentColor = Color(0xFF0D1F14),
                )
            },
        )

        // Card 3 — Biometric
        PermissionCard(
            iconEmoji = "🫆",
            iconTint = Lavender.copy(alpha = 0.15f),
            title = "Biometric Authentication",
            description = "Secures your journal with fingerprint or face lock. Your data, only yours.",
            action = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { enabled ->
                            val canAuth = BiometricManager.from(context)
                                .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
                            if (enabled && canAuth) {
                                biometricEnabled = true
                                scope.launch { context.setBiometricEnabled(true) }
                            } else if (!enabled) {
                                biometricEnabled = false
                                scope.launch { context.setBiometricEnabled(false) }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SageGreen,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = CardSubSurface,
                        ),
                    )
                    if (biometricEnabled) {
                        Text(
                            "Enabled",
                            style = MaterialTheme.typography.labelMedium,
                            color = SageGreen,
                        )
                    }
                }
            },
        )

        ZeroCloudBadge()

        Spacer(Modifier.height(4.dp))

        PrimaryButton(
            text = "Continue →",
            onClick = { navigateHome() },
        )

        TextButton(
            onClick = { navigateHome() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Skip for now — set up later",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    iconEmoji: String,
    iconTint: Color,
    title: String,
    description: String,
    action: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconTint),
                contentAlignment = Alignment.Center,
            ) {
                Text(iconEmoji, fontSize = 22.sp)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = CardText,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        action()
    }
}
