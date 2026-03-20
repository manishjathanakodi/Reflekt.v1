package com.reflekt.journal.ui.screens.track

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.reflekt.journal.ui.navigation.Routes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// ── Colour tokens ─────────────────────────────────────────────────────────────
private val CardBg   = Color(0xFF1A2030)
private val Surface2 = Color(0xFF1E2538)
private val Surface3 = Color(0xFF252D44)
private val CardText = Color(0xFFEEEAE2)
private val CardMuted= Color(0x80EEEAE2)
private val Gold     = Color(0xFFC9A96E)
private val Sage     = Color(0xFF6FA880)
private val Amber    = Color(0xFFE8A84D)

private val GOAL_EMOJIS = listOf(
    "🎯", "💪", "📚", "🧘", "🚀", "🌱", "💡", "🏆",
    "❤️", "🎵", "✍️", "🌍", "💰", "🤝", "🎓", "🌟",
)
private val GOAL_COLORS = listOf(
    "#6FA880", "#C9A96E", "#9B85C8", "#5F9FC4",
    "#D4756A", "#E8A84D", "#A09080", "#7BAC8A",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsTab(navController: NavController, viewModel: HabitsViewModel) {
    val activeGoals    by viewModel.activeGoals.collectAsState()
    val completedGoals by viewModel.completedGoals.collectAsState()
    var showSheet      by remember { mutableStateOf(false) }
    var showCompleted  by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showSheet = true },
                containerColor = Gold,
                contentColor   = Color(0xFF1A1208),
                shape          = RoundedCornerShape(50),
            ) { Icon(Icons.Default.Add, contentDescription = "Add goal") }
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
                // Goals header summary
                val activeCount    = activeGoals.size
                val completedCount = completedGoals.size
                Text(
                    "$activeCount active · $completedCount completed",
                    fontSize = 11.sp,
                    color = CardMuted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            if (activeGoals.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No goals yet — tap + to add one", color = CardMuted, fontSize = 13.sp)
                    }
                }
            }

            items(activeGoals, key = { it.goal.goalId }) { gwp ->
                GoalCard(
                    gwp     = gwp,
                    onClick = { navController.navigate(Routes.goalDetail(gwp.goal.goalId)) },
                )
            }

            if (completedGoals.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCompleted = !showCompleted }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Completed goals", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = CardMuted)
                        Icon(
                            if (showCompleted) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = CardMuted,
                        )
                    }
                }
                if (showCompleted) {
                    items(completedGoals, key = { "done_${it.goal.goalId}" }) { gwp ->
                        Box(modifier = Modifier.alpha(0.5f)) {
                            GoalCard(
                                gwp     = gwp,
                                onClick = { navController.navigate(Routes.goalDetail(gwp.goal.goalId)) },
                                showDoneBadge = true,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showSheet) {
        CreateGoalSheet(
            onDismiss = { showSheet = false },
            onSave    = { title, desc, emoji, targetDate, color, milestones ->
                viewModel.createGoal(title, desc, emoji, targetDate, color, milestones)
                showSheet = false
            },
        )
    }
}

// ── GoalCard ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalCard(
    gwp: GoalWithProgress,
    onClick: () -> Unit,
    showDoneBadge: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val goal = gwp.goal
    val goalColor = runCatching { android.graphics.Color.parseColor(goal.color) }
        .getOrNull()?.let { Color(it) } ?: Gold

    val daysLeft = goal.targetDate?.let {
        runCatching {
            ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(it)).toInt()
        }.getOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable(onClick = onClick),
    ) {
        // Coloured top strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Brush.horizontalGradient(listOf(goalColor, goalColor.copy(alpha = 0.4f)))),
        )

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(goalColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Text(goal.emoji, fontSize = 20.sp) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CardText, lineHeight = 18.sp)
                    daysLeft?.let { days ->
                        val label = when {
                            days < 0 -> "Overdue by ${-days}d"
                            days == 0 -> "Due today"
                            else -> "${days}d left"
                        }
                        Text(label, fontSize = 10.sp, color = if (days < 7) Amber else CardMuted)
                    }
                }
                if (showDoneBadge) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Sage.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    ) { Text("Done ✓", fontSize = 9.sp, color = Sage, fontWeight = FontWeight.Bold) }
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Progress", fontSize = 10.sp, color = CardMuted)
                    Text("${gwp.progressPercent.toInt()}%", fontSize = 10.sp, color = goalColor)
                }
                LinearProgressIndicator(
                    progress = { gwp.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color         = goalColor,
                    trackColor    = Surface3,
                    strokeCap     = StrokeCap.Round,
                )
            }
        }
    }
}

// ── CreateGoalSheet ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGoalSheet(
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String?, emoji: String, targetDate: String?, color: String, milestones: List<MilestoneItem>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title       by remember { mutableStateOf("") }
    var motivation  by remember { mutableStateOf("") }
    var emoji       by remember { mutableStateOf("🎯") }
    var targetDate  by remember { mutableStateOf<String?>(null) }
    var color       by remember { mutableStateOf(GOAL_COLORS[0]) }
    var showDatePicker by remember { mutableStateOf(false) }

    val milestones  = remember { mutableStateListOf<MilestoneItem>() }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        targetDate = Instant.ofEpochMilli(ms)
                            .atZone(ZoneId.of("UTC")).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("OK", color = Gold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = CardMuted) }
            },
        ) { DatePicker(state = datePickerState) }
    }

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
            Text("New Goal", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = CardText)

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

            OutlinedTextField(
                value         = motivation,
                onValueChange = { motivation = it },
                label         = { Text("Motivation (optional)", color = CardMuted) },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 2,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Gold,
                    unfocusedBorderColor = Surface3,
                    focusedTextColor     = CardText,
                    unfocusedTextColor   = CardText,
                ),
            )

            // Emoji picker
            Text("Icon", fontSize = 11.sp, color = CardMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GOAL_EMOJIS.forEach { e ->
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

            // Target date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface3)
                    .clickable { showDatePicker = true }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Target date", fontSize = 13.sp, color = CardMuted)
                Text(targetDate ?: "Pick a date (optional)", fontSize = 13.sp,
                    color = if (targetDate != null) Gold else CardMuted,
                    fontStyle = if (targetDate == null) FontStyle.Italic else FontStyle.Normal)
            }

            // Milestones
            Text("Milestones", fontSize = 11.sp, color = CardMuted)
            milestones.forEachIndexed { idx, milestone ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    var msTitle by remember { mutableStateOf(milestone.title) }
                    OutlinedTextField(
                        value         = msTitle,
                        onValueChange = { v -> msTitle = v; milestones[idx] = milestone.copy(title = v) },
                        label         = { Text("Milestone ${idx + 1}", color = CardMuted) },
                        singleLine    = true,
                        modifier      = Modifier.weight(1f),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Gold,
                            unfocusedBorderColor = Surface3,
                            focusedTextColor     = CardText,
                            unfocusedTextColor   = CardText,
                        ),
                    )
                    Text("✕", color = Amber, fontSize = 16.sp, modifier = Modifier.clickable { milestones.removeAt(idx) })
                }
            }
            TextButton(onClick = {
                milestones.add(MilestoneItem(java.util.UUID.randomUUID().toString(), "", false))
            }) {
                Text("+ Add milestone", color = Gold, fontSize = 12.sp)
            }

            // Color swatches
            Text("Color", fontSize = 11.sp, color = CardMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GOAL_COLORS.forEach { hex ->
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(if (title.isNotBlank()) Gold else Surface3)
                    .clickable(enabled = title.isNotBlank()) {
                        onSave(title, motivation.ifBlank { null }, emoji, targetDate, color,
                            milestones.filter { it.title.isNotBlank() })
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Save Goal", fontWeight = FontWeight.Bold, color = if (title.isNotBlank()) Color(0xFF1A1208) else CardMuted)
            }
        }
    }
}
