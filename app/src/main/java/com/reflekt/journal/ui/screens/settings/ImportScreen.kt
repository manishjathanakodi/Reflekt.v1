package com.reflekt.journal.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

private val ImportCardBg = Color(0xFF1E2538)
private val ImportGold   = Color(0xFFC9A96E)
private val ImportSage   = DarkSecondary
private val ImportBlush  = DarkError

@Composable
fun ImportScreen(navController: NavController) {
    val vm: SettingsViewModel = hiltViewModel()
    val importState by vm.importState.collectAsState()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var password    by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.resetImportState() }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) selectedUri = uri }

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
                text  = "Import Backup",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(8.dp))

        // Warning card
        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .background(ImportBlush.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .border(1.dp, ImportBlush.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "⚠️  This will merge data",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ImportBlush,
            )
            Text(
                "Importing will upsert all entries from the backup file. Existing entries with matching IDs will be overwritten.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = Color(0x80EEEAE2),
            )
        }

        Spacer(Modifier.height(20.dp))

        // File picker
        Column(modifier = Modifier.padding(horizontal = 22.dp)) {
            Text(
                "BACKUP FILE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.08.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ImportCardBg, RoundedCornerShape(14.dp))
                    .border(
                        width = 1.dp,
                        color = if (selectedUri != null) ImportGold.copy(alpha = 0.5f) else Color(0x20EEEAE2),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { filePicker.launch("*/*") }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = if (selectedUri != null) "File selected" else "Tap to choose .enc file",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selectedUri != null) Color(0xFFEEEAE2) else Color(0x80EEEAE2),
                        )
                        if (selectedUri != null) {
                            Text(
                                text  = selectedUri!!.lastPathSegment ?: selectedUri.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = ImportGold,
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        text  = if (selectedUri != null) "✓" else "📂",
                        fontSize = 18.sp,
                        color = if (selectedUri != null) ImportSage else Color(0x80EEEAE2),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Password field
        Column(modifier = Modifier.padding(horizontal = 22.dp)) {
            Text(
                "PASSWORD",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.08.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value                = password,
                onValueChange        = { password = it },
                label                = { Text("Backup password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine           = true,
                modifier             = Modifier.fillMaxWidth(),
                colors               = importTextFieldColors(),
            )
        }

        Spacer(Modifier.height(24.dp))

        // Action area
        Box(modifier = Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()) {
            when (val state = importState) {
                is UiState.Loading -> {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(24.dp),
                            color       = ImportGold,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.size(12.dp))
                        Text("Decrypting & importing…", style = MaterialTheme.typography.bodyMedium, color = Color(0x80EEEAE2))
                    }
                }
                is UiState.Success -> {
                    ImportSuccessCard(
                        summary = state.data,
                        onDone  = { navController.popBackStack() },
                    )
                }
                is UiState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Import failed: ${state.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ImportBlush,
                        )
                        ImportActionButton(
                            label   = "Retry",
                            enabled = selectedUri != null && password.isNotBlank(),
                            onClick = { vm.importBackup(selectedUri!!, password) },
                        )
                    }
                }
                else -> {
                    ImportActionButton(
                        label   = "Import Backup",
                        enabled = selectedUri != null && password.isNotBlank(),
                        onClick = { if (selectedUri != null) vm.importBackup(selectedUri!!, password) },
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ImportActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (enabled) ImportGold else ImportGold.copy(alpha = 0.3f),
                shape = RoundedCornerShape(50.dp),
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = if (enabled) Color(0xFF1A1208) else Color(0x80EEEAE2),
        )
    }
}

@Composable
private fun ImportSuccessCard(summary: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ImportSage.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("✓", fontSize = 32.sp, color = ImportSage)
        Text(
            "Import complete",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = ImportSage,
        )
        Text(
            summary,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFEEEAE2),
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ImportSage, RoundedCornerShape(50.dp))
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
private fun importTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor   = ImportCardBg,
    unfocusedContainerColor = ImportCardBg,
    focusedIndicatorColor   = ImportGold,
    unfocusedIndicatorColor = Color(0x40EEEAE2),
    focusedLabelColor       = ImportGold,
    unfocusedLabelColor     = Color(0x80EEEAE2),
    focusedTextColor        = Color(0xFFEEEAE2),
    unfocusedTextColor      = Color(0xFFEEEAE2),
    cursorColor             = ImportGold,
)
