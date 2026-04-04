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
import java.time.DayOfWeek
import java.time.LocalDate
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
    val hasPermission       by vm.usagePermissionGranted.collectAsState()
    val summary             by vm.screenTimeSummary.collectAsState()
    val appLimits           by vm.appLimits.collectAsState()
    val installedApps       by vm.installedApps.collectAsState()
    val todayUsageMap       by vm.todayUsageMap.collectAsState()
    val activeLimitsCount   by vm.activeLimitsCount.collectAsState()
    val limitsHitToday      by vm.limitsHitToday.collectAsState()
    val weeklyUsageByDay    by vm.weeklyUsageByDay.collectAsState()
    val screenTimeGoalMin   by vm.screenTimeGoalMinutes.collectAsState()
    val context             = LocalContext.current

    var showAddSheet   by remember { mutableStateOf(false) }
    var editingLimit   by remember { mutableStateOf<ManualAppLimit?>(null) }
    var showGoalSheet  by remember { mutableStateOf(false) }

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
        StatSummaryRow(
            summary      = summary,
            goalMinutes  = screenTimeGoalMin,
            onEditGoal   = { showGoalSheet = true },
        )

        // ── Section B: Breathing Quick Access ────────────────────────────────
        BreathingQuickAccessCard(onClick = { navController.navigate(Routes.microtask("BREATHING")) })

        // ── Screen time heatmap ───────────────────────────────────────────────
        ScreenTimeHeatmapCard(
            weeklyUsageByDay = weeklyUsageByDay,
            goalMinutes      = screenTimeGoalMin,
            hasPermission    = hasPermission,
            onEnablePermission = { vm.requestUsagePermission(context) },
        )

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

    // ── Screen time goal sheet ────────────────────────────────────────────────
    if (showGoalSheet) {
        ScreenTimeGoalSheet(
            currentGoalMinutes = screenTimeGoalMin,
            onSave             = { minutes ->
                vm.setScreenTimeGoal(minutes)
                showGoalSheet = false
            },
            onDismiss          = { showGoalSheet = false },
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
fun StatSummaryRow(
    summary: ScreenTimeSummary,
    goalMinutes: Int,
    onEditGoal: () -> Unit,
) {
    val (goalSub, goalSubColor) = when {
        !summary.hasGoal     -> "No goal set" to CardText.copy(alpha = 0.35f)
        summary.vsGoalMs > 0 -> "↑ ${summary.vsGoalMs / 60_000}min over goal" to Blush
        else                 -> "↓ ${abs(summary.vsGoalMs) / 60_000}min under goal" to Sage
    }

    val goalLabel = if (goalMinutes > 0) formatDuration(goalMinutes * 60_000L) else "—"
    val goalLabelColor = if (goalMinutes > 0) CardText else CardText.copy(alpha = 0.4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        // Card 1: screen time today
        StatCard(
            modifier = Modifier.weight(1f),
            label    = formatDuration(summary.totalMs),
            sub      = goalSub,
            subLabel = "today",
            subColor = goalSubColor,
        )
        // Card 2: daily goal (tappable → opens goal sheet)
        StatCard(
            modifier   = Modifier.weight(1f),
            label      = goalLabel,
            labelColor = goalLabelColor,
            sub        = "Edit →",
            subLabel   = "daily goal",
            subColor   = Gold,
            onClick    = onEditGoal,
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    labelColor: Color = CardText,
    sub: String,
    subLabel: String = "",
    subColor: Color,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .background(CardBg, RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(13.dp),
    ) {
        Column {
            Text(
                text  = label,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = labelColor,
            )
            if (subLabel.isNotEmpty()) {
                Text(
                    text  = subLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
            }
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

// ── Goal Setting Card ─────────────────────────────────────────────────────────

@Composable
private fun GoalSettingCard(goalMinutes: Int, onClick: () -> Unit) {
    val subtitleText = if (goalMinutes > 0) {
        val h = goalMinutes / 60; val m = goalMinutes % 60
        "Goal: " + when {
            h == 0  -> "${m}min/day"
            m == 0  -> "${h}h/day"
            else    -> "${h}h ${m}min/day"
        }
    } else {
        "Tap to set a goal"
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 22.dp, vertical = 6.dp)
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF252D44), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Gold.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("🎯", fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = "Daily screen time goal",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = CardText,
            )
            Text(
                text  = subtitleText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text  = "Edit",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Gold,
        )
    }
}

// ── Screen Time Heatmap Card ──────────────────────────────────────────────────

@Composable
private fun ScreenTimeHeatmapCard(
    weeklyUsageByDay: Map<String, Long>,
    goalMinutes: Int,
    hasPermission: Boolean,
    onEnablePermission: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 22.dp, vertical = 7.dp)
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(18.dp))
            .padding(15.dp),
    ) {
        if (!hasPermission) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text  = "Enable screen time to see your weekly usage heatmap",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Box(
                    modifier = Modifier
                        .background(Gold.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { onEnablePermission() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text  = "Allow in Settings →",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Gold,
                    )
                }
            }
            return@Box
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Column {
                Text(
                    text  = "SCREEN TIME THIS WEEK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.08.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text  = "Daily usage · last 7 days",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }

            val today = LocalDate.now()
            val days  = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val dayLetters = mapOf(
                DayOfWeek.MONDAY to "M", DayOfWeek.TUESDAY to "T",
                DayOfWeek.WEDNESDAY to "W", DayOfWeek.THURSDAY to "T",
                DayOfWeek.FRIDAY to "F", DayOfWeek.SATURDAY to "S",
                DayOfWeek.SUNDAY to "S",
            )
            val goalMs = goalMinutes * 60_000L

            Row(modifier = Modifier.fillMaxWidth()) {
                days.forEach { day ->
                    val dateStr  = day.toString()
                    val isToday  = day == today
                    val usageMs  = weeklyUsageByDay[dateStr] ?: -1L
                    val cellColor = heatmapCellColor(usageMs, goalMs)

                    Column(
                        modifier  = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        // Day letter
                        Text(
                            text  = dayLetters[day.dayOfWeek] ?: "?",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                            color = if (isToday) Gold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        )
                        // Cell
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp)
                                .height(32.dp)
                                .then(
                                    if (isToday) Modifier.border(1.5.dp, Gold, RoundedCornerShape(6.dp))
                                    else Modifier
                                )
                                .background(cellColor, RoundedCornerShape(6.dp)),
                        )
                        // Usage label
                        Text(
                            text  = if (usageMs < 0) "—" else formatCellUsage(usageMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        )
                    }
                }
            }

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeatmapLegendDot(Sage.copy(alpha = 0.6f), "On track")
                HeatmapLegendDot(Amber.copy(alpha = 0.6f), "Getting close")
                HeatmapLegendDot(Blush.copy(alpha = 0.75f), "Over limit")
            }
        }
    }
}

private fun heatmapCellColor(usageMs: Long, goalMs: Long): Color {
    if (usageMs < 0) return Color(0xFF252D44)
    return if (goalMs > 0) {
        val ratio = usageMs.toFloat() / goalMs
        when {
            ratio < 0.5f  -> Color(0xFF6FA880).copy(alpha = 0.50f)
            ratio < 0.8f  -> Color(0xFFE8A84D).copy(alpha = 0.50f)
            ratio < 1.0f  -> Color(0xFFD4756A).copy(alpha = 0.60f)
            else          -> Color(0xFFD4756A).copy(alpha = 0.85f)
        }
    } else {
        val hours = usageMs / 3_600_000L
        when {
            hours < 2  -> Color(0xFF6FA880).copy(alpha = 0.50f)
            hours < 4  -> Color(0xFFE8A84D).copy(alpha = 0.50f)
            hours < 6  -> Color(0xFFD4756A).copy(alpha = 0.60f)
            else       -> Color(0xFFD4756A).copy(alpha = 0.85f)
        }
    }
}

private fun formatCellUsage(ms: Long): String {
    val totalMin = ms / 60_000
    val h = totalMin / 60
    return if (h > 0) "${h}h" else "${totalMin}m"
}

@Composable
private fun HeatmapLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(modifier = Modifier.size(9.dp).background(color, RoundedCornerShape(3.dp)))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
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
