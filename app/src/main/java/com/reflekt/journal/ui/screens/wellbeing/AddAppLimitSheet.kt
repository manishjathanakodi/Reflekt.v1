package com.reflekt.journal.ui.screens.wellbeing

import android.app.NotificationChannel
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.reflekt.journal.data.db.ManualAppLimit
import com.reflekt.journal.ui.components.PrimaryButton
import com.reflekt.journal.ui.theme.DarkSecondary

private val SheetBg      = Color(0xFF1A2030)
private val SheetCard    = Color(0xFF252D44)
private val SheetDivider = Color(0xFF2A3450)
private val SheetGold    = Color(0xFFC9A96E)
private val SheetText    = Color(0xFFEEEAE2)
private val SheetSage    = DarkSecondary

private val COMMON_PACKAGES = listOf(
    "com.instagram.android",
    "com.google.android.youtube",
    "com.twitter.android",
    "com.zhiliaoapp.musically",
    "com.facebook.katana",
    "com.whatsapp",
    "com.reddit.frontpage",
    "com.netflix.mediaclient",
    "com.snapchat.android",
    "com.google.android.gm",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppLimitSheet(
    installedApps: List<InstalledApp>,
    existingLimit: ManualAppLimit?,
    onSave: (ManualAppLimit) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    var searchQuery    by remember { mutableStateOf("") }
    var selectedApp    by remember {
        mutableStateOf(
            if (existingLimit != null)
                installedApps.firstOrNull { it.packageName == existingLimit.packageName }
                    ?: InstalledApp(existingLimit.packageName, existingLimit.appLabel, null)
            else null
        )
    }
    var limitMinutes   by remember { mutableIntStateOf(existingLimit?.limitMinutes ?: 30) }
    var autoBlock      by remember { mutableStateOf(existingLimit?.autoBlock ?: false) }
    var requireBreath  by remember { mutableStateOf(existingLimit?.requireMicrotask ?: true) }
    var showAccessDlg  by remember { mutableStateOf(false) }

    // Build filtered list: common apps first, then rest, max 50
    val filteredApps = remember(searchQuery, installedApps) {
        val query = searchQuery.trim().lowercase()
        val all = if (query.isEmpty()) {
            val common = installedApps.filter { it.packageName in COMMON_PACKAGES }
                .sortedBy { COMMON_PACKAGES.indexOf(it.packageName) }
            val rest = installedApps.filter { it.packageName !in COMMON_PACKAGES }
            common + rest
        } else {
            installedApps.filter {
                it.label.lowercase().contains(query) ||
                        it.packageName.lowercase().contains(query)
            }
        }
        all.take(50)
    }

    fun isAccessibilityEnabled(): Boolean {
        val service = "${context.packageName}/com.reflekt.journal.wellbeing.blocker.AppBlockerService"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(":").any { it.equals(service, ignoreCase = true) }
    }

    fun save() {
        val app = selectedApp ?: return
        if (autoBlock && !isAccessibilityEnabled()) {
            showAccessDlg = true
            return
        }
        onSave(
            ManualAppLimit(
                packageName     = app.packageName,
                appLabel        = app.label,
                limitMinutes    = limitMinutes,
                autoBlock       = autoBlock,
                requireMicrotask = requireBreath,
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = SheetBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // Title
            Text(
                text  = if (existingLimit != null) "Edit limit" else "Which app do you want to limit?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SheetText,
            )

            // ── STEP 1: Search & select app ──────────────────────────────────
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder   = { Text("Search installed apps...", style = MaterialTheme.typography.bodySmall) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = SheetGold.copy(alpha = 0.6f),
                    unfocusedBorderColor = SheetDivider,
                    focusedTextColor     = SheetText,
                    unfocusedTextColor   = SheetText,
                    cursorColor          = SheetGold,
                    focusedContainerColor   = SheetCard,
                    unfocusedContainerColor = SheetCard,
                ),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isSelected = selectedApp?.packageName == app.packageName
                    AppRow(
                        app        = app,
                        isSelected = isSelected,
                        onClick    = {
                            selectedApp = if (isSelected) null else app
                        },
                    )
                }
            }

            // ── STEP 2: Limit settings (animated, shown when app selected) ───
            AnimatedVisibility(
                visible = selectedApp != null,
                enter   = expandVertically(),
                exit    = shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    HorizontalDivider(color = SheetDivider)

                    // Slider
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text  = "Daily limit",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = SheetText,
                            )
                            Text(
                                text  = formatLimit(limitMinutes),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SheetGold,
                            )
                        }
                        Slider(
                            value         = limitMinutes.toFloat(),
                            onValueChange = { limitMinutes = (it / 15).toInt() * 15 },
                            valueRange    = 15f..180f,
                            steps         = 10,
                            colors        = SliderDefaults.colors(
                                thumbColor         = SheetGold,
                                activeTrackColor   = SheetGold,
                                inactiveTrackColor = SheetDivider,
                            ),
                        )
                        // Quick chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15 to "15 min", 30 to "30 min", 60 to "1 hr", 120 to "2 hr")
                                .forEach { (value, label) ->
                                    val sel = limitMinutes == value
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (sel) SheetGold.copy(alpha = 0.18f) else SheetCard,
                                                RoundedCornerShape(8.dp),
                                            )
                                            .border(
                                                1.dp,
                                                if (sel) SheetGold.copy(alpha = 0.55f) else SheetDivider,
                                                RoundedCornerShape(8.dp),
                                            )
                                            .clickable { limitMinutes = value }
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                    ) {
                                        Text(
                                            text  = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (sel) SheetGold else SheetText.copy(alpha = 0.55f),
                                        )
                                    }
                                }
                        }
                    }

                    // Auto-block toggle
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("🚫", fontSize = 18.sp)
                                Text(
                                    text  = "Block when limit reached",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = SheetText,
                                )
                            }
                            Switch(
                                checked         = autoBlock,
                                onCheckedChange = { autoBlock = it },
                                colors          = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SheetSage,
                                ),
                            )
                        }
                        Text(
                            text     = "App will be paused when you hit your daily limit",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 28.dp),
                        )
                    }

                    // Microtask toggle (only when auto-block on)
                    if (autoBlock) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("🧘", fontSize = 18.sp)
                                Text(
                                    text  = "Require breathing exercise to unlock",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = SheetText,
                                )
                            }
                            Switch(
                                checked         = requireBreath,
                                onCheckedChange = { requireBreath = it },
                                colors          = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SheetSage,
                                ),
                            )
                        }
                    }
                }
            }

            // ── STEP 3: Save button ──────────────────────────────────────────
            PrimaryButton(
                text    = if (existingLimit != null) "Update limit" else "Add limit",
                enabled = selectedApp != null,
                onClick = { save() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Accessibility settings dialog
    if (showAccessDlg) {
        AlertDialog(
            onDismissRequest = { showAccessDlg = false },
            title = { Text("Accessibility access needed") },
            text  = {
                Text(
                    "To auto-block apps, enable Reflekt in Accessibility Settings.",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAccessDlg = false
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAccessDlg = false
                    // Save without auto-block
                    val app = selectedApp ?: return@TextButton
                    onSave(
                        ManualAppLimit(
                            packageName      = app.packageName,
                            appLabel         = app.label,
                            limitMinutes     = limitMinutes,
                            autoBlock        = false,
                            requireMicrotask = false,
                        )
                    )
                }) { Text("Skip for now") }
            },
            containerColor = Color(0xFF1A2030),
        )
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) SheetCard else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (app.icon != null) {
            val bmp = remember(app.icon) { app.icon.toBitmap().asImageBitmap() }
            Image(
                bitmap             = bmp,
                contentDescription = app.label,
                modifier           = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(SheetCard, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("📱", fontSize = 18.sp)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = app.label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = SheetText,
            )
            Text(
                text  = app.packageName,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = SheetText.copy(alpha = 0.35f),
                maxLines = 1,
            )
        }
        if (isSelected) {
            Text(
                text  = "✓",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = SheetGold,
            )
        }
    }
}

private fun formatLimit(minutes: Int): String = when {
    minutes < 60      -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else              -> "${minutes / 60}h ${minutes % 60}min"
}
