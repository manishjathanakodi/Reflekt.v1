package com.reflekt.journal.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.reflekt.journal.ai.engine.MoodTag
import com.reflekt.journal.data.db.JournalEntry
import com.reflekt.journal.ui.components.AiTagBadge
import com.reflekt.journal.ui.components.MoodBadge
import com.reflekt.journal.ui.components.TriggerChip
import com.reflekt.journal.ui.navigation.Routes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Colour tokens (always dark — per CLAUDE.md) ───────────────────────────────
private val CardBg   = Color(0xFF1A2030)
private val CardText = Color(0xFFEEEAE2)
private val Gold     = Color(0xFFC9A96E)
private val SageGreen = Color(0xFF6FA880)
private val Amber    = Color(0xFFE8A84D)
private val Lavender = Color(0xFF9B85C8)
private val Blush    = Color(0xFFD4756A)

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val userName by viewModel.userName.collectAsState()
    val recentEntries by viewModel.recentEntries.collectAsState()
    val todayHabits by viewModel.todayHabits.collectAsState()
    val streak by viewModel.journalStreak.collectAsState()
    val goalsCount by viewModel.activeGoalsCount.collectAsState()
    val overdueTodosCount by viewModel.overdueTodosCount.collectAsState()
    val lastEntryTier by viewModel.lastEntryTier.collectAsState()
    val todayPrompt by viewModel.todayPrompt.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Greeting header ────────────────────────────────────────────────────
        GreetingHeader(
            userName = userName,
            onAvatarClick = { navController.navigate(Routes.SETTINGS) },
        )

        // ── Overdue todos banner ───────────────────────────────────────────────
        if (overdueTodosCount > 0) {
            OverdueTodosBanner(
                count = overdueTodosCount,
                onClick = { navController.navigate(Routes.TODOS) },
                modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 11.dp),
            )
        }

        // ── Today's habits ─────────────────────────────────────────────────────
        if (todayHabits.isNotEmpty()) {
            Text(
                "Today's habits",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 4.dp),
            )
            TodayHabitsStrip(
                habits = todayHabits,
                onHabitClick = { navController.navigate(Routes.HABITS) },
            )
            val done = todayHabits.count { it.isDone }
            HabitProgressBar(
                done = done,
                total = todayHabits.size,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 3.dp).padding(bottom = 13.dp),
            )
        }

        // ── Today prompt card ──────────────────────────────────────────────────
        TodayPromptCard(
            prompt = todayPrompt,
            onClick = { navController.navigate(Routes.JOURNAL_NEW) },
            modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 13.dp),
        )

        // ── Stat row ───────────────────────────────────────────────────────────
        StatRow(
            streak = streak,
            goalsCount = goalsCount,
            modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 13.dp),
        )

        // ── Tier 2 banner ──────────────────────────────────────────────────────
        if (lastEntryTier == 2) {
            Tier2Banner(
                onClick = { navController.navigate(Routes.ANALYTICS) },
                modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 13.dp),
            )
        }

        // ── Recent entries ─────────────────────────────────────────────────────
        if (recentEntries.isNotEmpty()) {
            SectionHeader(
                title = "Recent entries",
                linkText = "See all",
                onLinkClick = { navController.navigate(Routes.HISTORY) },
                modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 9.dp),
            )
            recentEntries.forEach { entry ->
                RecentEntryCard(
                    entry = entry,
                    onClick = { navController.navigate(Routes.journalEntry(entry.entryId)) },
                    modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 9.dp),
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
fun GreetingHeader(
    userName: String,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val dateStr = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
    val initials = userName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2).joinToString("")
    val hour = java.time.LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                Text(
                    "$greeting, ",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "${userName.split(" ").first()}.",
                    style = MaterialTheme.typography.headlineMedium.copy(fontStyle = FontStyle.Italic),
                    color = Gold,
                )
            }
        }
        UserAvatarChip(initials = initials.ifEmpty { "?" }, onClick = onAvatarClick)
    }
}

@Composable
fun UserAvatarChip(initials: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(listOf(Gold, Lavender)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1208))
    }
}

@Composable
fun OverdueTodosBanner(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Blush.copy(alpha = 0.10f))
            .border(1.dp, Blush.copy(alpha = 0.22f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(10.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("⚠️", fontSize = 13.sp)
        Text(
            "$count overdue todo${if (count != 1) "s" else ""}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Blush,
            modifier = Modifier.weight(1f),
        )
        Text("View →", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Blush)
    }
}

@Composable
fun TodayHabitsStrip(
    habits: List<HabitWithTodayStatus>,
    onHabitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        items(habits) { item ->
            HabitPill(item = item, onClick = onHabitClick)
        }
    }
}

@Composable
fun HabitPill(item: HabitWithTodayStatus, onClick: () -> Unit) {
    val (bg, border, textColor) = if (item.isDone) {
        Triple(SageGreen.copy(alpha = 0.18f), SageGreen, SageGreen)
    } else {
        Triple(Amber.copy(alpha = 0.18f), Amber, Amber)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            "${item.habit.emoji} ${item.habit.title}${if (item.isDone) " ✓" else ""}",
            fontSize = 10.sp,
            color = textColor,
        )
    }
}

@Composable
fun HabitProgressBar(done: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)),
            color = SageGreen,
            trackColor = MaterialTheme.colorScheme.surfaceContainer,
            strokeCap = StrokeCap.Round,
        )
        Text("$done/$total", fontSize = 9.sp, color = SageGreen, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TodayPromptCard(prompt: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E2538), Color(0xFF252D44))))
            .border(1.dp, Color(0xFF252D44), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text(
            "✦ I'm listening",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Gold,
            letterSpacing = 0.1.sp,
        )
        Text(
            prompt,
            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Normal),
            color = CardText,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Gold)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(
                "Start writing →",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1208),
            )
        }
    }
}

@Composable
fun StatRow(streak: Int, goalsCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        StatCard(
            value = streak.toString(),
            label = "Day streak 🔥",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = goalsCount.toString(),
            label = "Active goals",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = "😐",
            label = "Avg mood",
            valueColor = Lavender,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = CardText) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = valueColor)
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Tier2Banner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Lavender.copy(alpha = 0.12f))
            .border(1.dp, Lavender.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("💜", fontSize = 18.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "You've had a few tough days",
                style = MaterialTheme.typography.labelMedium,
                color = Lavender,
            )
            Text(
                "Reflekt has a CBT suggestion ready for you.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("→", color = Lavender, fontSize = 16.sp)
    }
}

@Composable
fun SectionHeader(
    title: String,
    linkText: String,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        TextButton(onClick = onLinkClick, contentPadding = PaddingValues(0.dp)) {
            Text(linkText, fontSize = 11.sp, color = Gold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecentEntryCard(entry: JournalEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val date = Instant.ofEpochMilli(entry.timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    val dateStr = date.format(DateTimeFormatter.ofPattern("MMM d · h:mm a", Locale.getDefault()))
    val mood = try { MoodTag.valueOf(entry.moodTag) } catch (e: Exception) { MoodTag.NEUTRAL }
    val triggers = try {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(entry.triggersJson)
    } catch (e: Exception) { emptyList() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoodBadge(mood)
        }
        if (entry.aiSummary.isNotBlank()) {
            Text(
                entry.aiSummary,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                lineHeight = 16.sp,
            )
        }
        if (triggers.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                triggers.take(3).forEach { TriggerChip(it) }
                if (entry.moodTag.isNotBlank()) AiTagBadge()
            }
        }
    }
}
