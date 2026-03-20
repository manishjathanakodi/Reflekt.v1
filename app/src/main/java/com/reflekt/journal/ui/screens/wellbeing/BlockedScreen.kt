package com.reflekt.journal.ui.screens.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reflekt.journal.ui.theme.DarkError
import com.reflekt.journal.ui.theme.DarkSecondary
import com.reflekt.journal.ui.theme.DarkTertiary

private val Blush = DarkError   // #D4756A
private val Sage  = DarkSecondary
private val Gold  = Color(0xFFC9A96E)

// ── Stub PIN — in production read from secure DataStore ──────────────────────
private const val STUB_PIN = "1234"

@Composable
fun BlockedScreen(
    packageName: String,
    onNavigateToMicrotask: (taskType: String) -> Unit,
    onFinish: () -> Unit,
) {
    val vm: BlockedViewModel = hiltViewModel()
    val appLabel by vm.appLabel.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinError      by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    listOf(Blush.copy(alpha = 0.08f), Color.Transparent),
                ),
            )
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            BlockedAppIcon()
            Spacer(Modifier.height(24.dp))

            PausedBadge(appLabel)
            Spacer(Modifier.height(16.dp))

            ReasonHeadline()
            Spacer(Modifier.height(9.dp))

            ReasonBody(appLabel)
            Spacer(Modifier.height(26.dp))

            MicrotaskOptionsCard(
                onSelect = { taskType ->
                    vm.selectMicrotask(taskType)
                    onNavigateToMicrotask(taskType)
                },
            )
            Spacer(Modifier.height(14.dp))

            PinOverrideLink(onClick = { showPinDialog = true })
        }
    }

    if (showPinDialog) {
        PinDialog(
            onDismiss = { showPinDialog = false; pinError = false },
            onConfirm = { pin ->
                vm.overrideWithPin(pin, STUB_PIN, onSuccess = {
                    showPinDialog = false
                    onFinish()
                })
                if (pin != STUB_PIN) pinError = true
            },
            hasError = pinError,
        )
    }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
private fun BlockedAppIcon() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(82.dp),
    ) {
        // Greyed app icon (using emoji placeholder) — alpha + saturation via graphicsLayer
        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
                    )
                )
                .graphicsLayer { alpha = 0.65f },
            contentAlignment = Alignment.Center,
        ) {
            Text("📸", fontSize = 36.sp)
        }

        // Blush ring overlay with 🚫
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Color(0x38D4756A))
                .border(2.5.dp, Blush, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("🚫", fontSize = 24.sp)
        }
    }
}

@Composable
private fun PausedBadge(appLabel: String) {
    Box(
        modifier = Modifier
            .background(Blush.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
            .border(1.dp, Blush.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
            .padding(horizontal = 16.dp, vertical = 5.dp),
    ) {
        Text(
            text  = "${appLabel.ifBlank { "App" }} · Paused",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.1.sp,
            ),
            color = Blush,
        )
    }
}

@Composable
private fun ReasonHeadline() {
    Text(
        text      = "Time for a breather.",
        style     = MaterialTheme.typography.headlineSmall,
        color     = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ReasonBody(appLabel: String) {
    Text(
        text      = "You've been spending a lot of time on ${appLabel.ifBlank { "this app" }}. It has been linked to lower mood scores. A short break can help you reset.",
        style     = MaterialTheme.typography.bodySmall.copy(lineHeight = 22.sp),
        color     = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun MicrotaskOptionsCard(onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text(
            text  = "✦ Complete a micro-task to unlock",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.1.sp,
            ),
            color = Gold,
        )
        Spacer(Modifier.height(11.dp))

        MicrotaskOption(
            emoji    = "🌬️",
            title    = "2-min breathing exercise",
            subtitle = "Guided box breathing",
            onClick  = { onSelect("BREATHING") },
        )
        Spacer(Modifier.height(7.dp))

        MicrotaskOption(
            emoji    = "🙏",
            title    = "Write a gratitude note",
            subtitle = "3 things you're thankful for",
            onClick  = { onSelect("GRATITUDE") },
        )
        Spacer(Modifier.height(7.dp))

        MicrotaskOption(
            emoji    = "🧘",
            title    = "Body scan meditation",
            subtitle = "5-min guided check-in",
            onClick  = { onSelect("BODY_SCAN") },
        )
    }
}

@Composable
private fun MicrotaskOption(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text(emoji, fontSize = 19.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("›", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
    }
}

@Composable
private fun PinOverrideLink(onClick: () -> Unit) {
    Text(
        text           = "Override with PIN (logged)",
        style          = MaterialTheme.typography.bodySmall.copy(
            textDecoration = TextDecoration.Underline,
        ),
        color          = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier       = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun PinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit, hasError: Boolean) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter PIN") },
        text  = {
            Column {
                OutlinedTextField(
                    value               = pin,
                    onValueChange       = { pin = it },
                    label               = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError             = hasError,
                    singleLine          = true,
                )
                if (hasError) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Incorrect PIN",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
