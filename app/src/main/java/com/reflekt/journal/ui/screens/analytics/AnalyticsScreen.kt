package com.reflekt.journal.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.reflekt.journal.ai.engine.MoodTag
import com.reflekt.journal.ui.navigation.Routes
import java.time.format.DateTimeFormatter
import kotlin.math.atan2

// ── Colour tokens (always dark) ───────────────────────────────────────────────
private val CardBg   = Color(0xFF1A2030)
private val Surface2 = Color(0xFF1E2538)
private val Surface3 = Color(0xFF252D44)
private val CardText = Color(0xFFEEEAE2)
private val CardMuted= Color(0x80EEEAE2)
private val Gold     = Color(0xFFC9A96E)
private val GoldSoft = Color(0x26C9A96E)
private val Sage     = Color(0xFF6FA880)
private val Border   = Color(0x14FFFFFF)

private fun moodSegmentColor(mood: MoodTag) = when (mood) {
    MoodTag.HAPPY   -> Color(0xFF6FA880)
    MoodTag.SAD     -> Color(0xFF5F9FC4)
    MoodTag.ANXIOUS -> Color(0xFF9B85C8)
    MoodTag.ANGRY   -> Color(0xFFD4756A)
    MoodTag.NEUTRAL -> Color(0xFFC9A96E)
    MoodTag.FEAR    -> Color(0xFFA09080)
}

private fun moodEmoji(mood: MoodTag) = when (mood) {
    MoodTag.HAPPY   -> "😊"
    MoodTag.SAD     -> "😔"
    MoodTag.ANXIOUS -> "😰"
    MoodTag.ANGRY   -> "😤"
    MoodTag.NEUTRAL -> "😐"
    MoodTag.FEAR    -> "😨"
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val period          by viewModel.period.collectAsState()
    val distribution    by viewModel.moodDistribution.collectAsState()
    val totalEntries    by viewModel.totalEntries.collectAsState()
    val trendData       by viewModel.trendData.collectAsState()
    val topTriggers     by viewModel.topTriggers.collectAsState()
    val weeklyReport    by viewModel.weeklyReport.collectAsState()
    val isGenerating    by viewModel.isGeneratingReport.collectAsState()

    var showReportSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        AnalyticsHeader(onBack = { navController.popBackStack() })

        // ── Period toggle ─────────────────────────────────────────────────────
        PeriodToggle(
            period = period,
            onPeriodChanged = viewModel::onPeriodChanged,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
        )

        // ── Mood donut chart ──────────────────────────────────────────────────
        if (distribution.isNotEmpty()) {
            Text(
                "MOOD BREAKDOWN",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = CardMuted,
                letterSpacing = 0.1.sp,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoodDonutChart(
                    distribution  = distribution,
                    totalEntries  = totalEntries,
                    onSegmentClick = { mood ->
                        navController.navigate(Routes.historyFiltered(mood))
                    },
                    modifier = Modifier.size(160.dp),
                )
                DonutLegend(
                    distribution = distribution,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Mood trend line chart ─────────────────────────────────────────────
        if (trendData.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardBg)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Text(
                    "MOOD TREND",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardMuted,
                    letterSpacing = 0.1.sp,
                )
                Spacer(Modifier.height(10.dp))
                MoodTrendLineChart(
                    data = trendData,
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                )
            }
        }

        // ── Top triggers ──────────────────────────────────────────────────────
        if (topTriggers.isNotEmpty()) {
            TopTriggersCard(
                triggers = topTriggers,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
            )
        }

        // ── Weekly report card ────────────────────────────────────────────────
        WeeklyReportCard(
            report = weeklyReport,
            isGenerating = isGenerating,
            onGenerate = viewModel::generateWeeklyReport,
            onTap = { if (weeklyReport != null) showReportSheet = true },
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
        )
    }

    // ── Full report sheet ─────────────────────────────────────────────────────
    if (showReportSheet && weeklyReport != null) {
        FullReportSheet(
            report = weeklyReport!!,
            onDismiss = { showReportSheet = false },
        )
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
fun AnalyticsHeader(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            "Insights",
            style = MaterialTheme.typography.headlineMedium.copy(fontStyle = FontStyle.Normal),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
fun PeriodToggle(
    period: Period,
    onPeriodChanged: (Period) -> Unit,
    modifier: Modifier = Modifier,
) {
    val segments = listOf(Period.SEVEN_DAYS to "7 days", Period.THIRTY_DAYS to "30 days", Period.ALL_TIME to "All time")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segments.forEach { (p, label) ->
            val isActive = p == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isActive) Gold else Color.Transparent)
                    .clickable { onPeriodChanged(p) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) Color(0xFF1A1208) else CardMuted,
                )
            }
        }
    }
}

@Composable
fun MoodDonutChart(
    distribution: Map<MoodTag, Float>,
    totalEntries: Int,
    onSegmentClick: (MoodTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Build list of (mood, startAngle, sweepAngle) starting from -90° (top)
    val segments = remember(distribution) {
        val sorted = distribution.entries.sortedByDescending { it.value }
        var cursor = -90f
        sorted.map { (mood, pct) ->
            val sweep = pct / 100f * 360f
            val start = cursor
            cursor += sweep
            Triple(mood, start, sweep)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(segments) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        // Angle from top, clockwise in [0, 360)
                        val tapAngle = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()).toDouble()).toFloat() + 90f) + 360f) % 360f
                        segments.forEach { (mood, start, sweep) ->
                            val normStart = (start + 90f + 360f) % 360f
                            val normEnd   = (normStart + sweep)  % 360f
                            val hit = if (normEnd >= normStart)
                                tapAngle in normStart..normEnd
                            else
                                tapAngle >= normStart || tapAngle <= normEnd
                            if (hit) onSegmentClick(mood)
                        }
                    }
                },
        ) {
            val strokeWidth = size.width * 0.18f
            val radius      = (size.width - strokeWidth) / 2f
            val topLeft     = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)

            // Background track
            drawArc(
                color = Surface3,
                startAngle = 0f, sweepAngle = 360f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Butt),
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
            )

            // Mood segments
            segments.forEach { (mood, start, sweep) ->
                drawArc(
                    color = moodSegmentColor(mood),
                    startAngle = start, sweepAngle = sweep - 1f,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Butt),
                    topLeft = topLeft,
                    size = Size(radius * 2, radius * 2),
                )
            }
        }

        // Centre label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                totalEntries.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CardText,
            )
            Text("entries", fontSize = 9.sp, color = CardMuted)
        }
    }
}

@Composable
fun DonutLegend(
    distribution: Map<MoodTag, Float>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        distribution.entries
            .sortedByDescending { it.value }
            .forEach { (mood, pct) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(moodSegmentColor(mood)),
                    )
                    Text(
                        "${moodEmoji(mood)} ${mood.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        fontSize = 11.sp,
                        color = CardText,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${pct.toInt()}%", fontSize = 11.sp, color = CardMuted)
                }
            }
    }
}

@Composable
fun MoodTrendLineChart(
    data: List<ChartEntry>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val maxScore = maxOf(data.maxOf { it.moodScore }, 5f)
    val minScore = minOf(data.minOf { it.moodScore }, 0f)
    val scoreRange = maxScore - minScore

    // X axis date labels — show at most 5
    val labelIndices = when {
        data.size <= 5 -> data.indices.toList()
        else -> listOf(0, data.size / 4, data.size / 2, data.size * 3 / 4, data.size - 1)
    }
    val fmt = DateTimeFormatter.ofPattern("M/d")

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val pw = size.width
            val ph = size.height
            val padH = 12f
            val padV = 12f
            val chartW = pw - padH * 2
            val chartH = ph - padV * 2

            // Y-axis grid lines
            for (i in 0..4) {
                val y = padV + chartH * (1f - i / 4f)
                drawLine(
                    color = Color(0x1AFFFFFF),
                    start = Offset(padH, y),
                    end   = Offset(pw - padH, y),
                    strokeWidth = 1f,
                )
            }

            // Line path
            val points = data.mapIndexed { idx, entry ->
                val x = padH + idx.toFloat() / (data.size - 1).coerceAtLeast(1) * chartW
                val normalizedY = if (scoreRange > 0f) (entry.moodScore - minScore) / scoreRange else 0.5f
                val y = padV + chartH * (1f - normalizedY)
                Offset(x, y)
            }

            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, color = Sage, style = Stroke(2.5f, cap = StrokeCap.Round))
            }

            // Points — coloured by mood
            points.forEachIndexed { idx, pt ->
                drawCircle(
                    color  = moodSegmentColor(data[idx].mood),
                    radius = 5f,
                    center = pt,
                )
                drawCircle(
                    color  = CardBg,
                    radius = 3f,
                    center = pt,
                )
            }
        }

        // X axis labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labelIndices.forEach { idx ->
                Text(
                    data[idx].date.format(fmt),
                    fontSize = 9.sp,
                    color = CardMuted,
                )
            }
        }
    }
}

@Composable
fun TopTriggersCard(
    triggers: List<TriggerCount>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "TOP TRIGGERS",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = CardMuted,
            letterSpacing = 0.1.sp,
        )
        triggers.forEachIndexed { i, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "${i + 1}",
                    fontSize = 11.sp,
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(16.dp),
                )
                Text(
                    item.trigger,
                    fontSize = 12.sp,
                    color = CardText,
                    modifier = Modifier.weight(1f),
                )
                // Progress bar
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Surface3),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.pct / 100f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Gold),
                    )
                }
                Text(
                    "${item.pct.toInt()}%",
                    fontSize = 10.sp,
                    color = CardMuted,
                    modifier = Modifier.width(30.dp),
                )
            }
        }
    }
}

@Composable
fun WeeklyReportCard(
    report: String?,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clickable(enabled = report != null, onClick = onTap)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "✦ WEEKLY REPORT",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Gold,
            letterSpacing = 0.1.sp,
        )

        when {
            isGenerating -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(
                        color  = Gold,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Generating your report…", fontSize = 13.sp, color = CardMuted)
                }
            }

            report == null -> {
                Text(
                    "Get a personalised AI insight about your week.",
                    fontSize = 12.sp,
                    color = CardMuted,
                    lineHeight = 18.sp,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(GoldSoft)
                        .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .clickable(onClick = onGenerate)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("Generate Report", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Gold)
                }
            }

            else -> {
                // First sentence as headline
                val headline = report.substringBefore(".").take(80) + "."
                Text(
                    headline,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = Gold,
                    lineHeight = 20.sp,
                )
                Text(
                    report,
                    fontSize = 12.sp,
                    color = CardMuted,
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                )
                Text("Tap to read full report →", fontSize = 10.sp, color = Gold.copy(alpha = 0.6f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullReportSheet(report: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface2,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "✦ YOUR WEEKLY REPORT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                letterSpacing = 0.1.sp,
            )
            Text(report, fontSize = 14.sp, color = CardText, lineHeight = 22.sp)
        }
    }
}
