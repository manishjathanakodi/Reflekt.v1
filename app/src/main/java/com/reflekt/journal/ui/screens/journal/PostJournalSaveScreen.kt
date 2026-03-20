package com.reflekt.journal.ui.screens.journal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.reflekt.journal.ai.engine.MoodTag
import com.reflekt.journal.data.db.Habit
import com.reflekt.journal.data.db.Todo
import com.reflekt.journal.ui.components.MoodBadge
import com.reflekt.journal.ui.components.PrimaryButton
import com.reflekt.journal.ui.navigation.Routes
import kotlinx.coroutines.delay

private val CardBg    = Color(0xFF1A2030)
private val CardText  = Color(0xFFEEEAE2)
private val Gold      = Color(0xFFC9A96E)
private val SageGreen = Color(0xFF6FA880)

@Composable
fun PostJournalSaveScreen(
    navController: NavController,
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val postSaveState by viewModel.postSaveState.collectAsState()
    val state = postSaveState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            "Session complete ✦",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (state != null) {
            EncouragementCard(message = state.encouragement)

            MoodSummaryRow(
                mood = state.moodTag,
                done = state.habitsDoneCount,
                total = state.habitsTotalCount,
            )

            if (state.autoMarkedHabits.isNotEmpty() || state.autoMarkedTodos.isNotEmpty()) {
                AutoMarkedList(
                    habits = state.autoMarkedHabits,
                    todos = state.autoMarkedTodos,
                )
            }
        }

        // Reminder placeholder
        ReminderConfirmCard()

        Spacer(Modifier.height(8.dp))

        PrimaryButton(
            text = "Back to home",
            onClick = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                }
            },
        )

        PrimaryButton(
            text = "View full entry",
            onClick = {
                navController.navigate(Routes.HISTORY)
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun EncouragementCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(SageGreen.copy(alpha = 0.15f), SageGreen.copy(alpha = 0.05f)),
                ),
            )
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "✦ REFLEKT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = SageGreen,
                letterSpacing = 0.1.sp,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = CardText,
            )
        }
    }
}

@Composable
fun AutoMarkedList(habits: List<Habit>, todos: List<Todo>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Auto-marked via journal",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        habits.forEachIndexed { i, habit ->
            AnimatedItem(delayMs = i * 100L) {
                AutoMarkedRow(
                    emoji = habit.emoji,
                    name = habit.title,
                    streak = habit.streak + 1,
                )
            }
        }
        todos.forEachIndexed { i, todo ->
            AnimatedItem(delayMs = (habits.size + i) * 100L) {
                AutoMarkedRow(
                    emoji = "✅",
                    name = todo.title,
                    streak = null,
                )
            }
        }
    }
}

@Composable
private fun AnimatedItem(delayMs: Long, content: @Composable () -> Unit) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        )
    }
    Box(modifier = Modifier.scale(scale.value)) {
        content()
    }
}

@Composable
private fun AutoMarkedRow(emoji: String, name: String, streak: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBg)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(emoji, fontSize = 18.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.labelMedium, color = CardText)
            if (streak != null) {
                Text(
                    "Marked complete · $streak-day streak",
                    fontSize = 10.sp,
                    color = SageGreen,
                )
            } else {
                Text("Marked complete", fontSize = 10.sp, color = SageGreen)
            }
        }
    }
}

@Composable
fun MoodSummaryRow(mood: MoodTag, done: Int, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Mood", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoodBadge(mood)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Habits", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$done / $total", style = MaterialTheme.typography.titleMedium, color = CardText)
        }
    }
}

@Composable
fun ReminderConfirmCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2538))
            .padding(12.dp),
    ) {
        Text(
            "🔔 Reminders — set up in Settings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
