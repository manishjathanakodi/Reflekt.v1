package com.reflekt.journal.ui.screens.crisis

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.reflekt.journal.ui.navigation.Routes
import com.reflekt.journal.ui.theme.DarkError
import com.reflekt.journal.ui.theme.DarkSecondary
import com.reflekt.journal.ui.theme.LoraFamily

private val Blush = DarkError          // #D4756A
private val Sage  = DarkSecondary      // #6FA880
private val CardBg = Color(0xFF1E2538) // always dark per spec

@Composable
fun CrisisScreen(navController: NavController) {
    val vm: CrisisViewModel = hiltViewModel()
    val summary     by vm.clinicalSummary.collectAsState()
    val exportState by vm.exportState.collectAsState()
    val context     = LocalContext.current

    var showBadge      by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { showBadge = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Blush radial gradient at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Blush.copy(alpha = 0.08f), Color.Transparent),
                        radius = 600f,
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = showBadge,
                enter   = slideInVertically(initialOffsetY = { -it }),
            ) {
                Tier3Badge()
            }
            Spacer(Modifier.height(20.dp))

            CrisisHeadline()
            Spacer(Modifier.height(12.dp))
            CrisisBody()
            Spacer(Modifier.height(26.dp))

            SosCard(context = context)
            Spacer(Modifier.height(14.dp))

            ResourceCard(
                icon     = "🏥",
                title    = "Find a Doctor",
                subtitle = "Locate mental health professionals near you",
                onClick  = {
                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("geo:0,0?q=mental+health+doctor+near+me"))
                    intent.setPackage("com.google.android.apps.maps")
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/search/mental+health+doctor"))
                        )
                    }
                },
            )
            Spacer(Modifier.height(10.dp))

            ResourceCard(
                icon     = "💬",
                title    = "Chat Support",
                subtitle = "iCall online counselling — free & confidential",
                onClick  = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://icallhelpline.org"))
                    )
                },
            )
            Spacer(Modifier.height(10.dp))

            ResourceCard(
                icon     = "🧘",
                title    = "Grounding Exercise",
                subtitle = "2-minute guided box breathing",
                onClick  = { navController.navigate(Routes.microtask("BREATHING")) },
            )
            Spacer(Modifier.height(20.dp))

            if (summary != null) {
                ClinicalSummaryCard(
                    summary     = summary!!,
                    exportState = exportState,
                    onExport    = { showExportDialog = true },
                    onReset     = { vm.resetExportState() },
                )
                Spacer(Modifier.height(20.dp))
            }

            BackToHomeButton(onClick = {
                navController.navigate(Routes.HOME) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            })

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showExportDialog) {
        ExportPasswordDialog(
            onDismiss = { showExportDialog = false },
            onConfirm = { password ->
                showExportDialog = false
                vm.exportClinicalSummary(password)
            },
        )
    }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
private fun Tier3Badge() {
    Box(
        modifier = Modifier
            .background(Blush.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
            .border(1.dp, Blush.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text  = "⚠️ Tier 3 — Immediate support",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.08.sp,
            ),
            color = Blush,
        )
    }
}

@Composable
private fun CrisisHeadline() {
    Text(
        text      = "You don't have to carry this alone.",
        style     = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = LoraFamily,
            fontWeight = FontWeight.Normal,
        ),
        color     = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CrisisBody() {
    Text(
        text      = "We've noticed patterns that suggest you may benefit from talking to someone. The resources below are free, confidential, and available right now.",
        style     = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        color     = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun SosCard(context: android.content.Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Blush.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            .border(1.dp, Blush.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:9152987821")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("📞", fontSize = 28.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = "iCall Helpline",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Blush,
            )
            Text(
                text  = "9152987821",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = "Available now · Free",
                style = MaterialTheme.typography.labelSmall,
                color = Sage,
            )
        }
    }
}

@Composable
fun ResourceCard(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(icon, fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFEEEAE2),
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0x80EEEAE2),
            )
        }
        Text(
            text  = "›",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0x40EEEAE2),
        )
    }
}

@Composable
private fun ClinicalSummaryCard(
    summary: ClinicalSummary,
    exportState: UiState<Uri>,
    onExport: () -> Unit,
    onReset: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier  = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text  = "Clinical Summary",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFEEEAE2),
            )
            Text(
                text  = if (expanded) "▲" else "▼",
                color = Color(0x80EEEAE2),
                style = MaterialTheme.typography.labelSmall,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(12.dp))
                SummaryRow("Duration", summary.durationOfConcern)
                SummaryRow("Primary mood", summary.primaryMood)
                SummaryRow("Key triggers", summary.keyTriggers.joinToString(", "))
                SummaryRow(
                    label     = "Risk level",
                    value     = summary.riskLevel,
                    valueColor = if (summary.riskLevel.startsWith("High")) Blush else Color(0xFFEEEAE2),
                )
                Spacer(Modifier.height(14.dp))

                when (exportState) {
                    is UiState.Loading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Blush, strokeWidth = 2.dp)
                        }
                    }
                    is UiState.Success -> {
                        Text(
                            text  = "✓ Saved to Downloads",
                            style = MaterialTheme.typography.bodySmall,
                            color = Sage,
                            modifier = Modifier.fillMaxWidth().clickable { onReset() },
                            textAlign = TextAlign.Center,
                        )
                    }
                    is UiState.Error -> {
                        Text(
                            text  = "Export failed: ${(exportState as UiState.Error).message}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Blush,
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Blush.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                                .border(1.dp, Blush.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
                                .clickable(onClick = onExport)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text  = "Export for doctor",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Blush,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = Color(0xFFEEEAE2)) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0x80EEEAE2),
            modifier = Modifier.weight(1f),
        )
        Text(
            text  = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun BackToHomeButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent, RoundedCornerShape(50.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = "Return to Home",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ExportPasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set export password") },
        text  = {
            Column {
                Text(
                    "The file will be encrypted with this password. Keep it safe — you'll need it to open the file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value               = pw,
                    onValueChange       = { pw = it },
                    label               = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine          = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (pw.isNotBlank()) onConfirm(pw) }) { Text("Export") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
