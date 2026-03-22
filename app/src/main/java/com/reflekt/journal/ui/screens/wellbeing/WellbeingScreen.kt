package com.reflekt.journal.ui.screens.wellbeing

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.reflekt.journal.data.db.ManualAppLimit
import com.reflekt.journal.ui.components.EmptyStateCard
import com.reflekt.journal.ui.components.PrimaryButton
import com.reflekt.journal.ui.navigation.Routes
import com.reflekt.journal.ui.theme.DarkError
import com.reflekt.journal.ui.theme.DarkSecondary
import com.reflekt.journal.ui.theme.DarkTertiary
import kotlin.math.abs

private val Blush = DarkError        // #D4756A
private val Sage  = DarkSecondary   // #6FA880
private val Lav   = DarkTertiary    // #9B85C8
private val Amber = Color(0xFFE8A84D)
private val Sky   = Color(0xFF5F9FC4)
private val Gold  = Color(0xFFC9A96E)
private val CardBg   = Color(0xFF1E2538)
private val CardText = Color(0xFFEEEAE2)

@Composable
fun WellbeingScreen(navController: NavController) {
    val vm: WellbeingViewModel = hiltViewModel()
    val hasPermission  by vm.usagePermissionGranted.collectAsState()
    val summary        by vm.screenTimeSummary.collectAsState()
    val appLimits      by vm.appLimits.collectAsState()
    val installedApps  by vm.installedApps.collectAsState()
    val todayUsageMap  by vm.todayUsageMap.collectAsState()
    val context        = LocalContext.current

    var showAddSheet   by remember { mutableStateOf(false) }
    var editingLimit   by remember { mutableStateOf<ManualAppLimit?>(null) }

    LaunchedEffect(Unit) { vm.refreshPermission() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        WellbeingHeader()

        if (!hasPermission) {
            PermissionEmptyState(onEnable = { vm.requestUsagePermission(context) })
            return@Column
        }

        // ── Section A: Screen Time Summary ───────────────────────────────────
        StatSummaryRow(summary = summary)

        // ── Section B: Breathing Quick Access ────────────────────────────────
        BreathingQuickAccessCard(onClick = { navController.navigate(Routes.microtask("BREATHING")) })

        // ── Section C: My App Limits ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text  = "My App Limits",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text     = "+ Add app",
                style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color    = Gold,
                modifier = Modifier.clickable { showAddSheet = true },
            )
        }

        if (appLimits.isEmpty()) {
            EmptyStateCard(
                emoji    = "📵",
                title    = "No app limits yet",
                subtitle = "Add apps you want to be more mindful about. Reflekt will remind you when you've hit your limit.",
                ctaLabel = "+ Add your first app",
                onCta    = { showAddSheet = true },
                modifier = Modifier.padding(horizontal = 22.dp),
            )
        } else {
            appLimits.forEach { limit ->
                AppLimitCard(
                    limit        = limit,
                    todayUsageMs = todayUsageMap[limit.packageName],
                    hasPermission = hasPermission,
                    onEdit       = { editingLimit = limit; showAddSheet = true },
                    onRemove     = { vm.removeLimit(limit.packageName) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // ── Add / Edit sheet ─────────────────────────────────────────────────────
    if (showAddSheet) {
        AddAppLimitSheet(
            installedApps = installedApps,
            existingLimit = editingLimit,
            onSave        = { limit ->
                vm.addLimit(limit)
                showAddSheet = false
                editingLimit = null
            },
            onDismiss     = {
                showAddSheet = false
                editingLimit = null
            },
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun WellbeingHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Text(
            text  = "Digital Wellbeing",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text  = "Screen time & mindful limits",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Stat Summary Row ──────────────────────────────────────────────────────────

@Composable
fun StatSummaryRow(summary: ScreenTimeSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        StatCard(
            modifier  = Modifier.weight(1f),
            label     = formatDuration(summary.totalMs),
            sub       = buildDeltaLabel(summary.vsGoalMs),
            subColor  = if (summary.vsGoalMs > 0) Blush else Sage,
        )
        StatCard(
            modifier  = Modifier.weight(1f),
            label     = "${summary.pickupCount}",
            sub       = "pickups today",
            subColor  = CardText.copy(alpha = 0.5f),
        )
        StatCard(
            modifier   = Modifier.weight(1f),
            label      = "${"%.1f".format(summary.moodScore)} / 10",
            labelColor = Lav,
            sub        = if (summary.moodTrend >= 0)
                "↑ from ${"%.1f".format(summary.moodScore - summary.moodTrend)}"
            else
                "↓ from ${"%.1f".format(summary.moodScore - summary.moodTrend)}",
            subColor   = if (summary.moodTrend < 0) Blush else Sage,
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    labelColor: Color = CardText,
    sub: String,
    subColor: Color,
) {
    Box(
        modifier = modifier
            .background(CardBg, RoundedCornerShape(18.dp))
            .padding(13.dp),
    ) {
        Column {
            Text(
                text  = label,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = labelColor,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text  = sub,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = subColor,
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalMin = ms / 60_000
    val h = totalMin / 60; val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun buildDeltaLabel(vsGoalMs: Long): String {
    val absMins = abs(vsGoalMs) / 60_000
    return if (vsGoalMs > 0) "↑ ${absMins}min vs goal" else "↓ ${absMins}min vs goal"
}

// ── Breathing Quick Access Card ───────────────────────────────────────────────

@Composable
private fun BreathingQuickAccessCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 22.dp, vertical = 7.dp)
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .border(1.dp, Sky.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Sky.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("🌬️", fontSize = 24.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = "2-min breathing reset",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = CardText,
            )
            Text(
                text  = "Clear your head anytime",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .background(Sky.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .border(1.dp, Sky.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text  = "Start →",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Sky,
            )
        }
    }
}

// ── App Limit Card ────────────────────────────────────────────────────────────

@Composable
private fun AppLimitCard(
    limit: ManualAppLimit,
    todayUsageMs: Long?,
    hasPermission: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val usageMs  = todayUsageMs ?: 0L
    val limitMs  = limit.limitMinutes * 60_000L
    val progress = if (limitMs > 0) (usageMs.toFloat() / limitMs).coerceIn(0f, 1f) else 0f
    val usedMin  = usageMs / 60_000
    val remMin   = (limit.limitMinutes - usedMin).coerceAtLeast(0)
    val nearLimit = progress >= 0.8f

    val barColor = when {
        progress < 0.5f -> Sage
        progress < 0.8f -> Amber
        else            -> Blush
    }

    var menuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val iconDrawable = remember(limit.packageName) {
        try { context.packageManager.getApplicationIcon(limit.packageName) }
        catch (_: Exception) { null }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 22.dp, vertical = 4.dp)
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF252D44), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ROW 1: icon + name + limit badge + 3-dot menu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // App icon
            if (iconDrawable != null) {
                val bmp = remember(iconDrawable) { iconDrawable.toBitmap().asImageBitmap() }
                Image(
                    bitmap             = bmp,
                    contentDescription = limit.appLabel,
                    modifier           = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(CardBg, RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF2A3450), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("📱", fontSize = 20.sp)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = limit.appLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = CardText,
                )
                Text(
                    text  = "${limit.limitMinutes} min/day",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gold,
                )
            }

            // 3-dot menu
            Box {
                Text(
                    text     = "⋮",
                    fontSize = 20.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { menuExpanded = true },
                )
                DropdownMenu(
                    expanded         = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text    = { Text("Edit") },
                        onClick = { menuExpanded = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text    = { Text("Remove", color = Blush) },
                        onClick = { menuExpanded = false; onRemove() },
                    )
                }
            }
        }

        // ROW 2: usage progress
        if (!hasPermission) {
            Text(
                text  = "Enable screen time to track usage",
                style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        } else {
            Text(
                text  = "Today: ${usedMin}min used of ${limit.limitMinutes}min",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color      = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

        // ROW 3: auto-block badge
        if (limit.autoBlock) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("🚫", fontSize = 11.sp)
                Text(
                    text  = "Auto-block enabled",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Sage.copy(alpha = 0.75f),
                )
            }
        }

        // ROW 4: near-limit warning
        if (nearLimit && hasPermission) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Blush.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("⚠️", fontSize = 11.sp)
                Text(
                    text  = "${remMin}min remaining today",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Blush,
                )
            }
        }
    }
}

// ── Permission empty state ────────────────────────────────────────────────────

@Composable
fun PermissionEmptyState(onEnable: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Text("📱", fontSize = 56.sp)
        Text(
            text      = "Enable Screen Time",
            style     = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color     = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text      = "Reflekt needs usage access to show your screen time. Your data stays 100% on-device.",
            style     = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton(text = "Grant Screen Time Access", onClick = onEnable)
    }
}
