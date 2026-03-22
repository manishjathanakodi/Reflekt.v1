package com.reflekt.journal.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.reflekt.journal.ui.components.PrimaryButton
import com.reflekt.journal.ui.navigation.Routes

private val Gold = Color(0xFFC9A96E)
private val CardSurface = Color(0xFF1E2538)
private val SageGreen = Color(0xFF6FA880)

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsState()

    // Only auto-navigate returning users (Auth/Home). New users must tap "Begin your journey".
    LaunchedEffect(destination) {
        when (destination) {
            SplashDestination.Auth -> navController.navigate(Routes.AUTH_LOGIN) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
            SplashDestination.Home -> navController.navigate(Routes.HOME) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Logo container — stays dark in both modes per CLAUDE.md
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(CardSurface)
                    .border(
                        width = 1.dp,
                        color = Gold.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(28.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("📖", fontSize = 34.sp)
            }

            Spacer(Modifier.height(4.dp))

            // App name — adapts to theme
            Text(
                text = "Reflekt",
                style = MaterialTheme.typography.displayMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onBackground,
            )

            // Tagline
            Text(
                text = "Your private space to think, feel, and understand yourself better.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 220.dp),
            )

            Spacer(Modifier.height(8.dp))

            // Primary CTA
            PrimaryButton(
                text = "Begin your journey",
                onClick = {
                    navController.navigate(Routes.ONBOARDING_DEMO) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                modifier = Modifier.widthIn(max = 260.dp),
            )

            // Secondary link
            TextButton(onClick = {
                navController.navigate(Routes.AUTH_LOGIN) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }) {
                Text(
                    "I already have an account",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.Underline,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Privacy badge row
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔒", fontSize = 11.sp, color = SageGreen)
                Spacer(Modifier.width(4.dp))
                Text(
                    "100% on-device · Zero cloud · Encrypted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
