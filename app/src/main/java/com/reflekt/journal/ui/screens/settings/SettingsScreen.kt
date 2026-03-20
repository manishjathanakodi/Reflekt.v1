package com.reflekt.journal.ui.screens.settings

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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.reflekt.journal.ui.components.ZeroCloudBadge
import com.reflekt.journal.ui.navigation.Routes
import com.reflekt.journal.ui.theme.DarkError
import com.reflekt.journal.ui.theme.DarkSecondary
import com.reflekt.journal.ui.theme.MonoStyle
import com.reflekt.journal.ui.theme.ThemePreference

private val CardBg  = Color(0xFF1E2538) // stays dark per spec
private val Gold    = Color(0xFFC9A96E)
private val Sage    = DarkSecondary
private val Blush   = DarkError

@Composable
fun SettingsScreen(navController: NavController) {
    val vm: SettingsViewModel = hiltViewModel()
    val profile            by vm.userProfile.collectAsState()
    val themePreference    by vm.themePreference.collectAsState()
    val biometricEnabled   by vm.biometricEnabled.collectAsState()
    val notificationsEnabled by vm.notificationsEnabled.collectAsState()
    val context = LocalContext.current

    var showDeleteDialog    by remember { mutableStateOf(false) }
    var showBiometricOffDialog by remember { mutableStateOf(false) }
    var showEditNameDialog  by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            Text(
                text  = "Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        ProfileCard(
            name       = profile?.name ?: "—",
            occupation = profile?.occupation ?: "—",
            streak     = 0,
            onEditClick = { showEditNameDialog = true },
        )
        Spacer(Modifier.height(20.dp))

        SettingsSection("Appearance") {
            ThemeSelector(
                current  = themePreference,
                onChange = { vm.updateThemePreference(it) },
            )
        }
        Spacer(Modifier.height(16.dp))

        SettingsSection("Account & Security") {
            SettingsItem(
                icon     = "🔑",
                title    = "Change password",
                subtitle = "PIN / biometric access",
                trailing = { ChevronText() },
                onClick  = { /* Phase 10 */ },
            )
            Spacer(Modifier.height(2.dp))
            SettingsItem(
                icon     = "🔒",
                title    = "Biometric unlock",
                subtitle = "Use fingerprint to open Reflekt",
                trailing = {
                    Switch(
                        checked         = biometricEnabled,
                        onCheckedChange = { on ->
                            if (!on) showBiometricOffDialog = true
                            else vm.toggleBiometric(true)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Sage, checkedTrackColor = Sage.copy(0.4f)),
                    )
                },
                onClick = null,
            )
            Spacer(Modifier.height(2.dp))
            SettingsItem(
                icon     = "🗝️",
                title    = "Encryption",
                subtitle = "reflekt_db_key · Android Keystore",
                trailing = { Text("AES-256", style = MonoStyle.copy(fontSize = 10.sp), color = Color(0x80EEEAE2)) },
                onClick  = null,
            )
        }
        Spacer(Modifier.height(16.dp))

        SettingsSection("Notifications") {
            SettingsItem(
                icon     = "🔔",
                title    = "Daily reminder",
                subtitle = "Journal prompt at 9 PM",
                trailing = {
                    Switch(
                        checked         = notificationsEnabled,
                        onCheckedChange = { vm.toggleNotifications(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Sage, checkedTrackColor = Sage.copy(0.4f)),
                    )
                },
                onClick = null,
            )
        }
        Spacer(Modifier.height(16.dp))

        SettingsSection("Data & Privacy") {
            SettingsItem(
                icon     = "📤",
                title    = "Export backup",
                subtitle = "Encrypted .enc file to Downloads",
                trailing = { ChevronText() },
                onClick  = { navController.navigate(Routes.SETTINGS_EXPORT) },
            )
            Spacer(Modifier.height(2.dp))
            SettingsItem(
                icon     = "📥",
                title    = "Import backup",
                subtitle = "Restore from encrypted .enc file",
                trailing = { ChevronText() },
                onClick  = { navController.navigate(Routes.SETTINGS_IMPORT) },
            )
            Spacer(Modifier.height(2.dp))
            SettingsItem(
                icon      = "🗑️",
                title     = "Delete all data",
                titleColor = Blush,
                subtitle  = "Permanently removes all entries and settings",
                trailing  = { ChevronText() },
                onClick   = { showDeleteDialog = true },
            )
        }

        Spacer(Modifier.height(28.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ZeroCloudBadge()
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Reflekt v1.0 · All data stays on this device",
            modifier  = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            style     = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }

    if (showDeleteDialog) {
        DeleteAllDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                vm.deleteAllData {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
        )
    }

    if (showBiometricOffDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricOffDialog = false },
            title = { Text("Disable biometric?") },
            text  = { Text("You'll need to enter your PIN to open Reflekt.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.toggleBiometric(false)
                    showBiometricOffDialog = false
                }) { Text("Disable") }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricOffDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showEditNameDialog) {
        EditNameDialog(
            currentName       = profile?.name ?: "",
            currentOccupation = profile?.occupation ?: "",
            onDismiss  = { showEditNameDialog = false },
            onSave     = { name, occ ->
                vm.updateUserName(name)
                vm.updateOccupation(occ)
                showEditNameDialog = false
            },
        )
    }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
private fun ProfileCard(name: String, occupation: String, streak: Int, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Gradient avatar
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Gold.copy(0.6f), Gold.copy(0.2f)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1A1208),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFEEEAE2))
            Text(occupation, style = MaterialTheme.typography.bodySmall, color = Color(0x80EEEAE2))
            if (streak > 0) {
                Text("🔥 $streak day streak", style = MaterialTheme.typography.labelSmall, color = Gold)
            }
        }

        Text(
            text  = "Edit",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Gold,
            modifier = Modifier.clickable(onClick = onEditClick),
        )
    }
}

@Composable
private fun ThemeSelector(current: ThemePreference, onChange: (ThemePreference) -> Unit) {
    val options = listOf(ThemePreference.LIGHT to "Light",
        ThemePreference.DARK to "Dark",
        ThemePreference.SYSTEM to "System")

    Row(
        modifier = Modifier
            .padding(horizontal = 22.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(14.dp))
            .padding(5.dp),
    ) {
        options.forEach { (pref, label) ->
            val selected = current == pref
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Gold else Color.Transparent)
                    .clickable { onChange(pref) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                    color = if (selected) Color(0xFF1A1208) else Color(0x80EEEAE2),
                )
            }
        }
    }
}

@Composable
fun SettingsSection(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 22.dp)) {
        Text(
            text  = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.08.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        content()
    }
}

@Composable
fun SettingsItem(
    icon: String,
    title: String,
    titleColor: Color = Color(0xFFEEEAE2),
    subtitle: String = "",
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(icon, fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = titleColor)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color(0x80EEEAE2))
            }
        }
        trailing()
    }
}

@Composable
private fun ChevronText() {
    Text("›", style = MaterialTheme.typography.bodyLarge, color = Color(0x40EEEAE2))
}

@Composable
private fun DeleteAllDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var step         by remember { mutableStateOf(1) }
    var confirmText  by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 1) "Are you sure?" else "This cannot be undone") },
        text  = {
            if (step == 1) {
                Text(
                    "All journal entries, habits, goals, and settings will be permanently deleted.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Column {
                    Text(
                        "Type DELETE to confirm:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Blush,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value         = confirmText,
                        onValueChange = { confirmText = it },
                        singleLine    = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (step == 1) step = 2
                    else if (confirmText == "DELETE") onConfirm()
                },
            ) {
                Text(
                    text  = if (step == 1) "Continue" else "Delete everything",
                    color = Blush,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun EditNameDialog(
    currentName: String,
    currentOccupation: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    var occ  by remember { mutableStateOf(currentOccupation) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(value = occ,  onValueChange = { occ  = it }, label = { Text("Occupation") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, occ) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
