package com.reflekt.journal.ui.screens.track

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.reflekt.journal.data.db.Habit
import com.reflekt.journal.data.db.Todo
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ── Colour tokens ─────────────────────────────────────────────────────────────
private val CardBg   = Color(0xFF1A2030)
private val Surface2 = Color(0xFF1E2538)
private val Surface3 = Color(0xFF252D44)
private val CardText = Color(0xFFEEEAE2)
private val CardMuted= Color(0x80EEEAE2)
private val Gold     = Color(0xFFC9A96E)
private val GoldSoft = Color(0x26C9A96E)
private val Sage     = Color(0xFF6FA880)
private val Amber    = Color(0xFFE8A84D)

@Composable
fun GoalDetailScreen(
    navController: NavController,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val gwp                by viewModel.goal.collectAsState()
    val linkedHabits       by viewModel.linkedHabits.collectAsState()
    val linkedTodos        by viewModel.linkedTodos.collectAsState()
    val narrative          by viewModel.progressNarrative.collectAsState()
    val isGenerating       by viewModel.isGeneratingNarrative.collectAsState()

    val goal = gwp?.goal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CardText)
            }
            if (goal != null) {
                Text(goal.emoji, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = CardText, lineHeight = 22.sp)
                    goal.targetDate?.let { date ->
                        val days = runCatching {
                            ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(date)).toInt()
                        }.getOrNull()
                        days?.let {
                            Text(
                                when {
                                    it < 0  -> "Overdue by ${-it} days"
                                    it == 0 -> "Due today"
                                    else    -> "$it days left"
                                },
                                fontSize = 11.sp,
                                color = if (it < 7) Amber else CardMuted,
                            )
                        }
                    }
                }
            }
        }

        if (goal == null) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Gold)
            }
            return@Column
        }

        val goalColor = runCatching { android.graphics.Color.parseColor(goal.color) }
            .getOrNull()?.let { Color(it) } ?: Gold

        // ── Progress ring + motivation ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GoalProgressRing(progressPercent = gwp!!.progressPercent, goalColor = goalColor)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${gwp!!.progressPercent.toInt()}% complete", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CardText)
                if (!goal.description.isNullOrBlank()) {
                    MotivationQuote(text = goal.description)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── AI Narrative ──────────────────────────────────────────────────────
        AiNarrativeCard(
            narrative    = narrative,
            isGenerating = isGenerating,
            onGenerate   = { viewModel.generateNarrative() },
            modifier     = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(12.dp))

        // ── Milestones ────────────────────────────────────────────────────────
        val milestones = runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString<List<MilestoneItem>>(goal.milestonesJson)
        }.getOrElse { emptyList() }

        if (milestones.isNotEmpty()) {
            MilestonesCard(
                milestones = milestones,
                onToggle   = { viewModel.toggleMilestone(it) },
                modifier   = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
        }

        // ── Linked Habits ─────────────────────────────────────────────────────
        if (linkedHabits.isNotEmpty()) {
            LinkedHabitsList(habits = linkedHabits, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))
        }

        // ── Linked Todos ──────────────────────────────────────────────────────
        if (linkedTodos.isNotEmpty()) {
            LinkedTodosList(todos = linkedTodos, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── GoalProgressRing ──────────────────────────────────────────────────────────

@Composable
fun GoalProgressRing(progressPercent: Float, goalColor: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(90.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset  = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color      = Surface3,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft    = Offset(inset, inset), size = arcSize,
                style      = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (progressPercent > 0f) {
                drawArc(
                    color      = goalColor,
                    startAngle = -90f, sweepAngle = 360f * (progressPercent / 100f), useCenter = false,
                    topLeft    = Offset(inset, inset), size = arcSize,
                    style      = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            "${progressPercent.toInt()}%",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = CardText,
        )
    }
}

// ── MotivationQuote ───────────────────────────────────────────────────────────

@Composable
fun MotivationQuote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style    = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
        color    = CardMuted,
        modifier = modifier,
    )
}

// ── AiNarrativeCard ───────────────────────────────────────────────────────────

@Composable
fun AiNarrativeCard(
    narrative: String?,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GoldSoft)
            .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("✦ AI Progress Insight", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Gold, letterSpacing = 0.05.sp)
        when {
            isGenerating -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Gold, strokeWidth = 2.dp)
                    Text("Generating insight…", fontSize = 12.sp, color = CardMuted)
                }
            }
            narrative != null -> {
                Text(narrative, fontSize = 13.sp, color = CardText, lineHeight = 20.sp)
                Text(
                    "↺ Regenerate",
                    fontSize  = 11.sp,
                    color     = Gold,
                    modifier  = Modifier.clickable(onClick = onGenerate),
                )
            }
            else -> {
                Text("Get an AI-powered summary of your progress.", fontSize = 12.sp, color = CardMuted)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Gold)
                        .clickable(onClick = onGenerate)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("Generate insight", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1208))
                }
            }
        }
    }
}

// ── MilestonesCard ────────────────────────────────────────────────────────────

@Composable
fun MilestonesCard(
    milestones: List<MilestoneItem>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val done  = milestones.count { it.isCompleted }
    val total = milestones.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Milestones", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CardText)
            Text("$done/$total", fontSize = 11.sp, color = Gold)
        }
        milestones.forEach { milestone ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(milestone.id) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val dotColor = when {
                    milestone.isCompleted -> Sage
                    else -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(dotColor)
                        .border(
                            1.5.dp,
                            when {
                                milestone.isCompleted -> Sage
                                else -> Surface3
                            },
                            RoundedCornerShape(50),
                        ),
                )
                Text(
                    milestone.title,
                    fontSize = 13.sp,
                    color    = if (milestone.isCompleted) CardMuted else CardText,
                )
            }
        }
    }
}

// ── LinkedHabitsList ──────────────────────────────────────────────────────────

@Composable
fun LinkedHabitsList(habits: List<Habit>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Linked Habits", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CardText)
        habits.forEach { habit ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(habit.emoji, fontSize = 16.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(habit.title, fontSize = 13.sp, color = CardText)
                    if (habit.streak > 0) {
                        Text("🔥 ${habit.streak} day streak", fontSize = 10.sp, color = Amber)
                    }
                }
            }
        }
    }
}

// ── LinkedTodosList ───────────────────────────────────────────────────────────

@Composable
fun LinkedTodosList(todos: List<Todo>, modifier: Modifier = Modifier) {
    val done  = todos.count { it.isCompleted }
    val total = todos.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Linked Todos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CardText)
            Text("$done/$total done", fontSize = 11.sp, color = Gold)
        }
        LinearProgressIndicator(
            progress      = { if (total == 0) 0f else done.toFloat() / total },
            modifier      = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
            color         = Gold,
            trackColor    = Surface3,
            strokeCap     = StrokeCap.Round,
        )
        todos.take(5).forEach { todo ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (todo.isCompleted) Sage else Surface3),
                )
                Text(
                    todo.title,
                    fontSize = 13.sp,
                    color    = if (todo.isCompleted) CardMuted else CardText,
                )
            }
        }
        if (todos.size > 5) {
            Text("+${todos.size - 5} more", fontSize = 10.sp, color = CardMuted)
        }
    }
}
