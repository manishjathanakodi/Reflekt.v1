package com.reflekt.journal.ui.screens.history

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.reflekt.journal.ai.engine.MoodTag
import com.reflekt.journal.data.db.JournalEntry
import com.reflekt.journal.data.db.MoodLog
import com.reflekt.journal.ui.components.AiTagBadge
import com.reflekt.journal.ui.components.EmptyStateCard
import com.reflekt.journal.ui.components.MoodBadge
import com.reflekt.journal.ui.components.TriggerChip
import com.reflekt.journal.ui.navigation.Routes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Colour tokens (always dark) ───────────────────────────────────────────────
private val CardBg   = Color(0xFF1A2030)
private val Surface2 = Color(0xFF1E2538)
private val Surface3 = Color(0xFF252D44)
private val CardText = Color(0xFFEEEAE2)
private val CardMuted= Color(0x80EEEAE2)
private val CardDim  = Color(0x40EEEAE2)
private val Gold     = Color(0xFFC9A96E)
private val GoldSoft = Color(0x26C9A96E)
private val Sage     = Color(0xFF6FA880)
private val Sky      = Color(0xFF5F9FC4)
private val Lavender = Color(0xFF9B85C8)
private val Blush    = Color(0xFFD4756A)
private val Amber    = Color(0xFFE8A84D)
private val Border   = Color(0x14FFFFFF)

// ── Chat turn for conversation JSON ───────────────────────────────────────────
@Serializable
private data class ChatTurn(val role: String = "", val content: String = "")

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

// ── Mood colours for heatmap cells ────────────────────────────────────────────
private fun moodCellBg(tag: MoodTag) = when (tag) {
    MoodTag.HAPPY   -> Color(0x73_6F_A8_80.toInt())   // rgba(111,168,128,0.45)
    MoodTag.SAD     -> Color(0x73_5F_9F_C4.toInt())   // rgba(95,159,196,0.45)
    MoodTag.ANXIOUS -> Color(0x73_9B_85_C8.toInt())   // rgba(155,133,200,0.45)
    MoodTag.ANGRY   -> Color(0x73_D4_75_6A.toInt())   // rgba(212,117,106,0.45)
    MoodTag.NEUTRAL -> Color(0x59_C9_A9_6E.toInt())   // rgba(201,169,110,0.35)
    MoodTag.FEAR    -> Color(0x73_A0_90_B0.toInt())
}

private fun moodEmoji(tag: MoodTag) = when (tag) {
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
fun HistoryScreen(
    navController: NavController,
    initialMoodFilter: String? = null,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val searchQuery  by viewModel.searchQuery.collectAsState()
    val moodFilter   by viewModel.moodFilter.collectAsState()
    val heatmapData  by viewModel.heatmapData.collectAsState()
    val selectedEntry by viewModel.selectedEntry.collectAsState()
    val entries = viewModel.entries.collectAsLazyPagingItems()

    // Apply optional mood filter from nav arg (e.g. tap from analytics donut)
    LaunchedEffect(initialMoodFilter) {
        if (initialMoodFilter != null) {
            val tag = try { MoodTag.valueOf(initialMoodFilter) } catch (_: Exception) { null }
            viewModel.onMoodFilterChanged(tag)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item(key = "header") {
                HistoryHeader(onBack = { navController.popBackStack() })
            }

            // ── Mood heatmap ──────────────────────────────────────────────────
            item(key = "heatmap") {
                MoodHeatmapCard(
                    moodLogs = heatmapData,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                )
            }

            // ── Search bar ────────────────────────────────────────────────────
            item(key = "search") {
                HistorySearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp),
                )
            }

            // ── Filter chips ──────────────────────────────────────────────────
            item(key = "filters") {
                MoodFilterChipRow(
                    selected = moodFilter,
                    onSelect  = viewModel::onMoodFilterChanged,
                    modifier  = Modifier.padding(bottom = 14.dp),
                )
            }

            // ── Loading state ─────────────────────────────────────────────────
            if (entries.loadState.refresh is LoadState.Loading) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                        CircularProgressIndicator(color = Gold)
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (entries.loadState.refresh !is LoadState.Loading && entries.itemCount == 0) {
                item(key = "empty") {
                    EmptyStateCard(
                        emoji    = "📖",
                        title    = "Nothing here yet",
                        subtitle = "Start your first journal entry to see it here.",
                    )
                }
            }

            // ── Paged entry cards with date group headers ─────────────────────
            items(entries.itemCount) { index ->
                val entry    = entries[index]
                val prevEntry = if (index > 0) entries.peek(index - 1) else null

                if (entry != null) {
                    val entryDate = Instant.ofEpochMilli(entry.timestamp)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    val prevDate  = prevEntry?.let {
                        Instant.ofEpochMilli(it.timestamp)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    if (entryDate != prevDate) {
                        DateGroupHeader(
                            date = entryDate,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    EntryCard(
                        entry   = entry,
                        onClick = { viewModel.onEntrySelected(entry) },
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                }
            }

            // ── Append loading indicator ──────────────────────────────────────
            if (entries.loadState.append is LoadState.Loading) {
                item(key = "append_loading") {
                    Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                        CircularProgressIndicator(color = Gold, modifier = Modifier.size(24.dp))
                    }
                }
            }

            item(key = "bottom_space") { Spacer(Modifier.height(32.dp)) }
        }

        // ── Entry detail bottom sheet ─────────────────────────────────────────
        if (selectedEntry != null) {
            EntryDetailBottomSheet(
                entry    = selectedEntry!!,
                onDismiss = { viewModel.onEntryDismissed() },
                onDelete  = { viewModel.onDeleteEntry(it) },
                onEdit    = { id ->
                    viewModel.onEntryDismissed()
                    navController.navigate(Routes.journalEntry(id))
                },
            )
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
fun HistoryHeader(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
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
            "Your journal",
            style = MaterialTheme.typography.headlineMedium.copy(fontStyle = FontStyle.Normal),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
fun MoodHeatmapCard(
    moodLogs: List<MoodLog>,
    modifier: Modifier = Modifier,
) {
    // Build a map from date string → MoodLog
    val logByDate = remember(moodLogs) { moodLogs.associateBy { it.date } }

    // Current week: Monday → Sunday
    val today  = LocalDate.now()
    val monday = today.with(DayOfWeek.MONDAY)
    val weekDays = (0..6).map { monday.plusDays(it.toLong()) }
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "THIS WEEK",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = CardMuted,
            letterSpacing = 0.1.sp,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            weekDays.forEachIndexed { i, date ->
                val log   = logByDate[date.toString()]
                val mood  = log?.dominantMood?.let { tag ->
                    try { MoodTag.valueOf(tag) } catch (_: Exception) { null }
                }
                val bgColor = if (mood != null) moodCellBg(mood) else Surface3

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(dayLabels[i], fontSize = 8.sp, color = CardDim, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (mood != null) moodEmoji(mood) else "·",
                        fontSize = if (mood != null) 14.sp else 10.sp,
                        color = CardText,
                    )
                }
            }
        }

        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Today: ${today.format(DateTimeFormatter.ofPattern("MMM d"))}", fontSize = 9.sp, color = CardDim)
            if (logByDate.isEmpty()) {
                Text("No entries yet this week", fontSize = 9.sp, color = CardDim)
            }
        }
    }
}

@Composable
fun HistorySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value         = query,
        onValueChange = onQueryChange,
        placeholder   = { Text("Search entries…", color = CardDim) },
        singleLine    = true,
        modifier      = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor   = Surface2,
            unfocusedContainerColor = Surface2,
            focusedTextColor        = CardText,
            unfocusedTextColor      = CardText,
            cursorColor             = Gold,
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
fun MoodFilterChipRow(
    selected: MoodTag?,
    onSelect: (MoodTag?) -> Unit,
    modifier: Modifier = Modifier,
) {
    data class Chip(val label: String, val mood: MoodTag?)
    val chips = listOf(
        Chip("All", null),
        Chip("😊 Happy",   MoodTag.HAPPY),
        Chip("😰 Anxious", MoodTag.ANXIOUS),
        Chip("😔 Sad",     MoodTag.SAD),
        Chip("😤 Angry",   MoodTag.ANGRY),
        Chip("😐 Neutral", MoodTag.NEUTRAL),
    )

    LazyRow(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(chips) { chip ->
            val isSelected = chip.mood == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) GoldSoft else Surface2)
                    .border(
                        1.dp,
                        if (isSelected) Gold else Border,
                        RoundedCornerShape(50),
                    )
                    .clickable { onSelect(chip.mood) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    chip.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Gold else CardMuted,
                )
            }
        }
    }
}

@Composable
fun DateGroupHeader(date: LocalDate, modifier: Modifier = Modifier) {
    val today     = LocalDate.now()
    val yesterday = today.minusDays(1)
    val label = when (date) {
        today     -> "Today"
        yesterday -> "Yesterday"
        else      -> date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
    }
    Text(
        label,
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryCard(
    entry: JournalEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mood = try { MoodTag.valueOf(entry.moodTag) } catch (_: Exception) { MoodTag.NEUTRAL }
    val triggers = try {
        lenientJson.decodeFromString<List<String>>(entry.triggersJson)
    } catch (_: Exception) { emptyList() }
    val time = Instant.ofEpochMilli(entry.timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(time, fontSize = 10.sp, color = CardMuted)
            MoodBadge(mood)
        }
        if (entry.aiSummary.isNotBlank()) {
            Text(
                entry.aiSummary,
                fontSize = 11.sp,
                color = CardMuted,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                lineHeight = 16.sp,
            )
        }
        if (triggers.isNotEmpty() || entry.triageTier > 1) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                triggers.take(3).forEach { TriggerChip(it) }
                if (entry.moodTag.isNotBlank()) AiTagBadge()
            }
        }
    }
}

// ── Entry detail bottom sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryDetailBottomSheet(
    entry: JournalEntry,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val mood = try { MoodTag.valueOf(entry.moodTag) } catch (_: Exception) { MoodTag.NEUTRAL }
    val triggers = try {
        lenientJson.decodeFromString<List<String>>(entry.triggersJson)
    } catch (_: Exception) { emptyList() }
    val turns = try {
        lenientJson.decodeFromString<List<ChatTurn>>(entry.conversationJson)
    } catch (_: Exception) { emptyList() }
    val dateStr = Instant.ofEpochMilli(entry.timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEEE, MMMM d · h:mm a", Locale.getDefault()))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface2,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Date + mood
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(dateStr, fontSize = 11.sp, color = CardMuted)
                MoodBadge(mood)
            }

            // Read-only chat conversation
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                turns.forEach { turn ->
                    val isAi = turn.role == "ai"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = if (isAi) 4.dp else 14.dp,
                                        topEnd = if (isAi) 14.dp else 4.dp,
                                        bottomStart = 14.dp,
                                        bottomEnd = 14.dp,
                                    )
                                )
                                .background(if (isAi) Surface3 else Gold.copy(alpha = 0.2f))
                                .padding(10.dp),
                        ) {
                            Text(
                                turn.content,
                                fontSize = 12.sp,
                                color = if (isAi) CardMuted else Gold,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }

            // Triggers
            if (triggers.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "IDENTIFIED TRIGGERS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CardMuted,
                        letterSpacing = 0.1.sp,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        triggers.forEach { TriggerChip(it) }
                    }
                }
            }

            // AI summary
            if (entry.aiSummary.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldSoft)
                        .border(1.dp, Gold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "✦ AI SUMMARY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        letterSpacing = 0.1.sp,
                    )
                    Text(entry.aiSummary, fontSize = 12.sp, color = CardText, lineHeight = 18.sp)
                }
            }

            // Clinical summary (tier 3)
            if (entry.triageTier == 3 && !entry.clinicalSummaryJson.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Blush.copy(alpha = 0.10f))
                        .border(1.dp, Blush.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "⚠ CLINICAL NOTE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Blush,
                        letterSpacing = 0.1.sp,
                    )
                    Text(entry.clinicalSummaryJson, fontSize = 12.sp, color = CardText, lineHeight = 18.sp)
                }
            }

            HorizontalDivider(color = Border)

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { showDeleteDialog = true }) {
                    Text("Delete", color = Blush, fontSize = 13.sp)
                }
                TextButton(onClick = { onEdit(entry.entryId) }) {
                    Text("Edit entry →", color = Gold, fontSize = 13.sp)
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = Surface2,
            title = { Text("Delete entry?", color = CardText) },
            text  = { Text("This entry will be removed from your journal.", color = CardMuted) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(entry.entryId)
                }) { Text("Delete", color = Blush) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = CardMuted)
                }
            },
        )
    }
}
