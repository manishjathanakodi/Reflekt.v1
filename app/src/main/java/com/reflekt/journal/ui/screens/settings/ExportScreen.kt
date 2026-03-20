package com.reflekt.journal.ui.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.reflekt.journal.ui.screens.crisis.UiState
import com.reflekt.journal.ui.theme.DarkError
import com.reflekt.journal.ui.theme.DarkSecondary

private val ExportCardBg = Color(0xFF1E2538)
private val ExportGold   = Color(0xFFC9A96E)
private val ExportSage   = DarkSecondary
private val ExportBlush  = DarkError

@Composable
fun ExportScreen(navController: NavController) {
    val vm: SettingsViewModel = hiltViewModel()
    val exportState by vm.exportState.collectAsState()

    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.resetExportState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text     = "←",
                style    = MaterialTheme.typography.titleLarge,
                color    = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickable { navController.popBackStack() },
            )
            Text(
                text  = "Export Backup",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(8.dp))

        // Info card
        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .background(ExportCardBg, RoundedCornerShape(18.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "🔐  Encrypted Backup",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFEEEAE2),
            )
            Text(
                "Your data will be encrypted with AES-256-GCM using a key derived from your password. " +
                    "The file is saved to your Downloads folder. Keep your password safe — it cannot be recovered.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = Color(0x80EEEAE2),
            )
        }

        Spacer(Modifier.height(20.dp))

        // Password fields
        Column(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value                = password,
                onValueChange        = { password = it; validationError = null },
                label                = { Text("Password (min 8 characters)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine           = true,
                modifier             = Modifier.fillMaxWidth(),
                colors               = exportTextFieldColors(),
            )
            OutlinedTextField(
                value                = confirmPassword,
                onValueChange        = { confirmPassword = it; validationError = null },
                label                = { Text("Confirm password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine           = true,
                modifier             = Modifier.fillMaxWidth(),
                colors               = exportTextFieldColors(),
            )
            if (validationError != null) {
                Text(
                    text  = validationError!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = ExportBlush,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Action area
        Box(modifier = Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()) {
            when (val state = exportState) {
                is UiState.Loading -> {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(24.dp),
                            color       = ExportGold,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.size(12.dp))
                        Text("Encrypting…", style = MaterialTheme.typography.bodyMedium, color = Color(0x80EEEAE2))
                    }
                }
                is UiState.Success -> {
                    ExportSuccessCard(onDone = { navController.popBackStack() })
                }
                is UiState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Export failed: ${state.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ExportBlush,
                        )
                        ExportActionButton(label = "Retry") {
                            validateAndExport(password, confirmPassword,
                                onValidationError = { validationError = it },
                                onExport          = { vm.exportBackup(password) },
                            )
                        }
                    }
                }
                else -> {
                    ExportActionButton(label = "Export to Downloads") {
                        validateAndExport(password, confirmPassword,
                            onValidationError = { validationError = it },
                            onExport          = { vm.exportBackup(password) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

private fun validateAndExport(
    password: String,
    confirmPassword: String,
    onValidationError: (String) -> Unit,
    onExport: () -> Unit,
) {
    when {
        password.length < 8         -> onValidationError("Password must be at least 8 characters")
        password != confirmPassword -> onValidationError("Passwords do not match")
        else                        -> onExport()
    }
}

@Composable
private fun ExportActionButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ExportGold, RoundedCornerShape(50.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1A1208),
        )
    }
}

@Composable
private fun ExportSuccessCard(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ExportSage.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("✓", fontSize = 32.sp, color = ExportSage)
        Text(
            "Backup saved to Downloads",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = ExportSage,
        )
        Text(
            "File encrypted with AES-256-GCM · reflekt_backup_*.enc",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0x80EEEAE2),
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ExportSage, RoundedCornerShape(50.dp))
                .clickable(onClick = onDone)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Done",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1A1208),
            )
        }
    }
}

@Composable
private fun exportTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor   = ExportCardBg,
    unfocusedContainerColor = ExportCardBg,
    focusedIndicatorColor   = ExportGold,
    unfocusedIndicatorColor = Color(0x40EEEAE2),
    focusedLabelColor       = ExportGold,
    unfocusedLabelColor     = Color(0x80EEEAE2),
    focusedTextColor        = Color(0xFFEEEAE2),
    unfocusedTextColor      = Color(0xFFEEEAE2),
    cursorColor             = ExportGold,
)
