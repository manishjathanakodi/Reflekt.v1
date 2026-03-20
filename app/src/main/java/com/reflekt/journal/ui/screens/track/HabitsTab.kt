package com.reflekt.journal.ui.screens.track

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflekt.journal.data.db.Goal
import com.reflekt.journal.data.db.HabitLog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

// ── Colour tokens ─────────────────────────────────────────────────────────────
private val CardBg   = Color(0xFF1A2030)
private val Surface2 = Color(0xFF1E2538)
private val Surface3 = Color(0xFF252D44)
private val CardText = Color(0xFFEEEAE2)
private val CardMuted= Color(0x80EEEAE2)
private val Gold     = Color(0xFFC9A96E)
private val Sage     = Color(0xFF6FA880)
private val Blush    = Color(0xFFD4756A)
private val Amber    = Color(0xFFE8A84D)

private val HABIT_EMOJIS = listOf(
    "🏃", "🧘", "📚", "💧", "🌙", "💪", "🥗", "🎯",
    "🧹", "🎵", "✍️", "🌿", "😴", "🚴", "🧠", "🙏",
)
private val HABIT_COLORS = listOf(
    "#6FA880", "#C9A96E", "#9B85C8", "#5F9FC4",
    "#D4756A", "#E8A84D", "#A09080", "#7BAC8A",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsTab(viewModel: HabitsViewModel) {
    val habits   by viewModel.habits.collectAsState()
    val progress by viewModel.todayProgress.collectAsState()
    val goals    by viewModel.activeGoals.collectAsState()

    var showCreateSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick           = { showCreateSheet = true },
                containerColor    = Gold,
                contentColor      = Color(0xFF1A1208),
                shape             = RoundedCornerShape(50),
            ) { Icon(Icons.Default.Add, contentDescription = "Add habit") }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                TodayProgressRing(done = progress.done, total = progress.total)
                Spacer(Modifier.height(16.dp))
            }

            if (habits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No habits yet — tap + to add one", color = CardMuted, fontSize = 13.sp)
                    }
                }
            }

            items(habits, key = { it.habit.habitId }) { item ->
                HabitCard(
                    item    = item,
                    logsLastWeek = viewModel.getLogsForHabit(item.habit.habitId),
                    onCheck = { viewModel.completeHabit(item.habit.habitId, LocalDate.now().toString()) },
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showCreateSheet) {
        CreateHabitSheet(
            goals    = goals.map { it.goal },
            onDismiss = { showCreateSheet = false },
            onSave   = { title, emoji, freq, customDays, reminder, goalId, color ->
                viewModel.createHabit(title, emoji, freq, customDays, reminder, goalId, color)
                showCreateSheet = false
            },
        )
    }
}

// ── TodayProgressRing ─────────────────────────────────────────────────────────

@Composable
fun TodayProgressRing(done: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val progress = if (total == 0) 0f else done.toFloat() / total
        Canvas(modifier = Modifier.size(72.dp)) {
            val stroke = 7.dp.toPx()
            val inset  = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            // background arc
            drawArc(
                color       = Surface3,
                startAngle  = -90f,
                sweepAngle  = 360f,
                useCenter   = false,
                topLeft     = Offset(inset, inset),
                size        = arcSize,
                style       = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // progress arc
            if (progress > 0f) {
                drawArc(
                    color       = Sage,
                    startAngle  = -90f,
                    sweepAngle  = 360f * progress,
                    useCenter   = false,
                    topLeft     = Offset(inset, inset),
                    size        = arcSize,
                    style       = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$done/$total", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CardText)
            }
            Text("habits done today", fontSize = 11.sp, color = CardMuted)
        }
    }
}

// ── HabitCard ─────────────────────────────────────────────────────────────────

@Composable
fun HabitCard(
    item: HabitWithTodayStatus,
    logsLastWeek: List<HabitLog>,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habit    = item.habit
    val streakAtRisk = habit.frequency == "DAILY"
        && !item.isDone
        && LocalTime.now().hour >= 21

    val habitColor = runCatching { android.graphics.Color.parseColor(habit.color) }
        .getOrNull()?.let { Color(it) } ?: Gold
    val borderColor = if (streakAtRisk) Blush.copy(alpha = 0.32f) else Surface3

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Emoji icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(habitColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(habit.emoji, fontSize = 20.sp)
            }
            // Title + meta
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(habit.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CardText)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (habit.streak > 0) {
                        StreakBadge(streak = habit.streak)
                    }
                    Text(habit.frequency.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp, color = CardMuted)
                    if (item.todayLog?.completedViaJournal == true) {
                        AiTagBadgeSmall()
                    }
                }
            }
            // Check button
            CheckButton(done = item.isDone, onClick = onCheck)
        }

        if (expanded) {
            HabitCalendarStrip(logsLastWeek = logsLastWeek)
        }

        if (streakAtRisk) {
            StreakAtRiskCard(streakDays = habit.streak)
        }
    }
}

@Composable
fun StreakBadge(streak: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Amber.copy(alpha = 0.18f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text("🔥 $streak days", fontSize = 9.sp, color = Amber, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AiTagBadgeSmall() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Gold.copy(alpha = 0.14f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text("✦ AI", fontSize = 9.sp, color = Gold, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CheckButton(done: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(50))
            .background(if (done) Sage else Color.Transparent)
            .border(1.5.dp, if (done) Sage else Surface3, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1A1208), modifier = Modifier.size(16.dp))
    }
}

@Composable
fun HabitCalendarStrip(logsLastWeek: List<HabitLog>, modifier: Modifier = Modifier) {
    val today   = LocalDate.now()
    val days    = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val logMap  = logsLastWeek.associateBy { it.date }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        days.forEach { date ->
            val log   = logMap[date.toString()]
            val color = when (log?.status) {
                "COMPLETED" -> Sage
                "MISSED"    -> Blush
                else        -> Surface3
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    fontSize = 8.sp,
                    color = CardMuted,
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color),
                )
            }
        }
    }
}

@Composable
fun StreakAtRiskCard(streakDays: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Blush.copy(alpha = 0.10f))
            .border(1.dp, Blush.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("⚠️", fontSize = 13.sp)
        Text(
            "Complete today to keep your $streakDays-day streak!",
            fontSize = 11.sp,
            color = Blush,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

// ── CreateHabitSheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateHabitSheet(
    goals: List<Goal>,
    onDismiss: () -> Unit,
    onSave: (title: String, emoji: String, freq: String, customDays: List<Int>, reminder: String?, goalId: String?, color: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title      by remember { mutableStateOf("") }
    var emoji      by remember { mutableStateOf("🎯") }
    var frequency  by remember { mutableStateOf("DAILY") }
    var customDays by remember { mutableStateOf<List<Int>>(emptyList()) }
    var color      by remember { mutableStateOf(HABIT_COLORS[0]) }
    var goalId     by remember { mutableStateOf<String?>(null) }

    val freqOptions = listOf("DAILY", "WEEKDAYS", "WEEKLY", "CUSTOM")
    val dayLabels   = listOf("M", "T", "W", "T", "F", "S", "S")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color(0xFF1E2538),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("New Habit", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = CardText)

            // Title
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Title", color = CardMuted) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Gold,
                    unfocusedBorderColor = Surface3,
                    focusedTextColor     = CardText,
                    unfocusedTextColor   = CardText,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            // Emoji picker
            Text("Icon", fontSize = 11.sp, color = CardMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HABIT_EMOJIS.forEach { e ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (emoji == e) Gold.copy(alpha = 0.22f) else Surface3)
                            .border(1.dp, if (emoji == e) Gold else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, fontSize = 20.sp) }
                }
            }

            // Frequency chips
            Text("Frequency", fontSize = 11.sp, color = CardMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                freqOptions.forEach { f ->
                    FilterChip(
                        selected = frequency == f,
                        onClick  = { frequency = f },
                        label    = { Text(f.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold.copy(alpha = 0.20f),
                            selectedLabelColor     = Gold,
                        ),
                    )
                }
            }

            // Custom day row
            if (frequency == "CUSTOM") {
                Text("Days", fontSize = 11.sp, color = CardMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dayLabels.forEachIndexed { idx, label ->
                        val selected = idx in customDays
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) Gold.copy(alpha = 0.22f) else Surface3)
                                .border(1.dp, if (selected) Gold else Color.Transparent, RoundedCornerShape(50))
                                .clickable {
                                    customDays = if (selected) customDays - idx else customDays + idx
                                },
                            contentAlignment = Alignment.Center,
                        ) { Text(label, fontSize = 11.sp, color = if (selected) Gold else CardMuted) }
                    }
                }
            }

            // Color swatches
            Text("Color", fontSize = 11.sp, color = CardMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HABIT_COLORS.forEach { hex ->
                    val c = runCatching { android.graphics.Color.parseColor(hex) }.getOrNull()
                        ?.let { Color(it) } ?: Gold
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .background(c)
                            .border(2.dp, if (color == hex) CardText else Color.Transparent, RoundedCornerShape(50))
                            .clickable { color = hex },
                    )
                }
            }

            // Goal link
            if (goals.isNotEmpty()) {
                Text("Link to goal (optional)", fontSize = 11.sp, color = CardMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = goalId == null,
                        onClick  = { goalId = null },
                        label    = { Text("None", fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Surface3,
                            selectedLabelColor     = CardText,
                        ),
                    )
                    goals.forEach { g ->
                        FilterChip(
                            selected = goalId == g.goalId,
                            onClick  = { goalId = if (goalId == g.goalId) null else g.goalId },
                            label    = { Text("${g.emoji} ${g.title}", fontSize = 11.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold.copy(alpha = 0.20f),
                                selectedLabelColor     = Gold,
                            ),
                        )
                    }
                }
            }

            // Save button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(if (title.isNotBlank()) Gold else Surface3)
                    .clickable(enabled = title.isNotBlank()) {
                        onSave(title, emoji, frequency, customDays, null, goalId, color)
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Save Habit", fontWeight = FontWeight.Bold, color = if (title.isNotBlank()) Color(0xFF1A1208) else CardMuted)
            }
        }
    }
}
