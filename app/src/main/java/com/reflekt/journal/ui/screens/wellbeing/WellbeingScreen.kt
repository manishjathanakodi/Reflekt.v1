package com.reflekt.journal.ui.screens.wellbeing

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.reflekt.journal.ui.components.PrimaryButton
import com.reflekt.journal.ui.theme.DarkError
import com.reflekt.journal.ui.theme.DarkSecondary
import com.reflekt.journal.ui.theme.DarkTertiary
import java.time.DayOfWeek
import kotlin.math.abs

// Colours used in this screen
private val Blush   = DarkError          // #D4756A
private val Sage    = DarkSecondary      // #6FA880
private val Lav     = DarkTertiary       // #9B85C8
private val Amber   = Color(0xFFE8A84D)
private val Sky     = Color(0xFF5F9FC4)

@Composable
fun WellbeingScreen(navController: NavController) {
    val vm: WellbeingViewModel = hiltViewModel()
    val hasPermission   by vm.usagePermissionGranted.collectAsState()
    val summary         by vm.screenTimeSummary.collectAsState()
    val insight         by vm.aiInsight.collectAsState()
    val heatmap         by vm.heatmapData.collectAsState()
    val triggerApps     by vm.triggerApps.collectAsState()
    val context = LocalContext.current

    // Refresh permission when screen appears (user may have granted it)
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

        StatSummaryRow(summary = summary)

        AnimatedVisibility(visible = insight.isNotBlank()) {
            AiInsightBanner(insight = insight)
        }

        MoodUsageHeatmap(cells = heatmap)

        if (triggerApps.isNotEmpty()) {
            Text(
                text = "Trigger Apps",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
            )
            triggerApps.forEach { app ->
                TriggerAppCard(
                    app       = app,
                    onLimit   = { /* future: create Intervention */ },
                    onWatch   = { },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Header ─────────────────────────────────────────────────────────────────────

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
            text  = "Screen time & mood correlation",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Stat Summary Row ───────────────────────────────────────────────────────────

@Composable
fun StatSummaryRow(summary: ScreenTimeSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label    = formatDuration(summary.totalMs),
            sub      = buildDeltaLabel(summary.vsGoalMs),
            subColor = if (summary.vsGoalMs > 0) Blush else Sage,
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label    = "${summary.pickupCount}",
            sub      = "Avg 52/day",
            subColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label    = "${"%.1f".format(summary.moodScore)} / 10",
            labelColor = Lav,
            sub      = if (summary.moodTrend >= 0)
                "↑ from ${"%.1f".format(summary.moodScore - summary.moodTrend)}"
            else
                "↓ from ${"%.1f".format(summary.moodScore - summary.moodTrend)}",
            subColor = if (summary.moodTrend < 0) Blush else Sage,
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    sub: String,
    subColor: Color,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
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
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun buildDeltaLabel(vsGoalMs: Long): String {
    val absMins = abs(vsGoalMs) / 60_000
    return if (vsGoalMs > 0) "↑ ${absMins}min vs goal" else "↓ ${absMins}min vs goal"
}

// ── AI Insight Banner ──────────────────────────────────────────────────────────

@Composable
fun AiInsightBanner(insight: String) {
    Box(
        modifier = Modifier
            .padding(horizontal = 22.dp, vertical = 7.dp)
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        Lav.copy(alpha = 0.15f),
                        Sky.copy(alpha = 0.08f),
                    )
                ),
                shape = RoundedCornerShape(18.dp),
            )
            .border(1.dp, Lav.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(15.dp),
    ) {
        Column {
            Text(
                text  = "✦ AI Insight",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.1.sp,
                ),
                color = Lav,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text  = insight,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ── Mood-Usage Heatmap ─────────────────────────────────────────────────────────

@Composable
fun MoodUsageHeatmap(cells: List<HeatmapCell>) {
    Box(
        modifier = Modifier
            .padding(horizontal = 22.dp, vertical = 7.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(15.dp),
    ) {
        Column {
            Text(
                text  = "MOOD-USAGE HEATMAP",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.08.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text  = "Screen time vs emotional state · This week",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(12.dp))

            val days = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            val categories = listOf("Social", "Work", "Stream")
            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

            // Header row
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(44.dp))
                dayLabels.forEach { d ->
                    Text(
                        text      = d,
                        modifier  = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style     = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                        color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Data rows
            categories.forEachIndexed { rowIdx, category ->
                val backendCategory = when (rowIdx) {
                    0 -> "SOCIAL"; 1 -> "WORK"; else -> "STREAMING"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = category,
                        style    = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.width(44.dp),
                    )
                    days.forEach { dow ->
                        val cell = cells.find { it.category == backendCategory && it.dayOfWeek == dow }
                        val score = cell?.impactScore ?: 0f
                        val opacity = (abs(score) * 2f).coerceIn(0.35f, 0.85f)
                        val color = when {
                            score < -0.4f -> Blush.copy(alpha = opacity)
                            score >  0.3f -> Sage.copy(alpha = opacity)
                            else          -> Amber.copy(alpha = opacity)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .height(20.dp)
                                .background(color, RoundedCornerShape(5.dp)),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Legend
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text  = "Impact:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
                LegendDot(Blush.copy(alpha = 0.7f), "Negative")
                LegendDot(Sage.copy(alpha = 0.6f), "Positive")
                LegendDot(Amber.copy(alpha = 0.5f), "Neutral")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

// ── Trigger App Card ───────────────────────────────────────────────────────────

@Composable
fun TriggerAppCard(
    app: AppUsageWithImpact,
    onLimit: () -> Unit,
    onWatch: () -> Unit,
) {
    val log    = app.log
    val score  = log.impactScore
    val totalMin = log.durationMs / 60_000
    val maxDailyMs = 4 * 60 * 60 * 1000L // 4h as 100%
    val progress = (log.durationMs.toFloat() / maxDailyMs).coerceIn(0f, 1f)

    val borderColor = when {
        score < -0.6f -> Blush.copy(alpha = 0.2f)
        score < -0.2f -> Amber.copy(alpha = 0.15f)
        else          -> MaterialTheme.colorScheme.outline
    }
    val barColor = when {
        score < -0.6f -> Blush
        score < -0.2f -> Amber
        else          -> Sage
    }
    val impactLabel = when {
        score < -0.6f -> "High negative impact"
        score < -0.2f -> "Moderate impact"
        else          -> "Low impact"
    }
    val buttonLabel = when {
        score < -0.6f -> "Limit"
        score < -0.2f -> "Limit"
        else          -> "Watch"
    }
    val buttonColor = when {
        score < -0.6f -> Blush
        score < -0.2f -> Amber
        else          -> Sage
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 22.dp, vertical = 4.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        // App icon placeholder (coloured box)
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    brush = Brush.linearGradient(listOf(barColor.copy(0.6f), barColor.copy(0.3f))),
                    shape = RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = log.appLabel.take(1), fontSize = 18.sp, color = Color.White)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = log.appLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = "${totalMin}min today · $impactLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress  = { progress },
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color      = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .background(buttonColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .border(1.dp, buttonColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clickable { if (buttonLabel == "Watch") onWatch() else onLimit() }
                .padding(horizontal = 11.dp, vertical = 5.dp),
        ) {
            Text(
                text  = buttonLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = buttonColor,
            )
        }
    }
}

// ── Permission empty state ─────────────────────────────────────────────────────

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
            text      = "Reflekt needs usage access to show your screen time and discover which apps affect your mood. Your data stays 100% on-device.",
            style     = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton(text = "Grant Screen Time Access", onClick = onEnable)
    }
}
