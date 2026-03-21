package com.reflekt.journal.ui.screens.track

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflekt.journal.data.db.Goal
import com.reflekt.journal.data.db.Todo
import com.reflekt.journal.ui.components.EmptyStateCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
private val Sky      = Color(0xFF5F9FC4)

private fun priorityOrder(p: String) = when (p) {
    "URGENT" -> 0; "HIGH" -> 1; "MEDIUM" -> 2; "LOW" -> 3; else -> 4
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodosTab(viewModel: HabitsViewModel) {
    val todos   by viewModel.todos.collectAsState()
    val goals   by viewModel.activeGoals.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    val today   = LocalDate.now().toString()
    val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val overdue   = todos.filter { !it.isCompleted && it.dueDate != null && it.dueDate < today }
        .sortedBy { priorityOrder(it.priority) }
    val todayList = todos.filter { !it.isCompleted && it.dueDate == today }
        .sortedBy { priorityOrder(it.priority) }
    val upcoming  = todos.filter { !it.isCompleted && (it.dueDate == null || it.dueDate > today) }
        .sortedWith(compareBy({ it.dueDate ?: "9999" }, { priorityOrder(it.priority) }))
    val completed = todos.filter {
        it.isCompleted && (it.completedAt ?: 0L) >= todayStart
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showSheet = true },
                containerColor = Gold,
                contentColor   = Color(0xFF1A1208),
                shape          = RoundedCornerShape(50),
            ) { Icon(Icons.Default.Add, contentDescription = "Add todo") }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { Spacer(Modifier.height(12.dp)) }

            if (overdue.isNotEmpty()) {
                item { TodoGroupHeader("Overdue", Blush) }
                items(overdue, key = { it.todoId }) { todo ->
                    SwipeTodoCard(
                        todo     = todo,
                        isOverdue = true,
                        onComplete = { viewModel.completeTodo(todo.todoId) },
                        onDelete   = {
                            viewModel.deleteTodo(todo.todoId)
                            viewModel.setPendingUndo(todo.todoId)
                        },
                    )
                }
            }

            if (todayList.isNotEmpty()) {
                item { TodoGroupHeader("Today", Amber) }
                items(todayList, key = { it.todoId }) { todo ->
                    SwipeTodoCard(
                        todo     = todo,
                        isOverdue = false,
                        onComplete = { viewModel.completeTodo(todo.todoId) },
                        onDelete   = {
                            viewModel.deleteTodo(todo.todoId)
                            viewModel.setPendingUndo(todo.todoId)
                        },
                    )
                }
            }

            if (upcoming.isNotEmpty()) {
                item { TodoGroupHeader("Upcoming", CardMuted) }
                items(upcoming, key = { it.todoId }) { todo ->
                    SwipeTodoCard(
                        todo     = todo,
                        isOverdue = false,
                        onComplete = { viewModel.completeTodo(todo.todoId) },
                        onDelete   = {
                            viewModel.deleteTodo(todo.todoId)
                            viewModel.setPendingUndo(todo.todoId)
                        },
                    )
                }
            }

            if (completed.isNotEmpty()) {
                item { TodoGroupHeader("Completed", CardMuted) }
                items(completed, key = { it.todoId }) { todo ->
                    TodoCard(todo = todo, isOverdue = false, onComplete = {}, onDelete = {})
                }
            }

            if (overdue.isEmpty() && todayList.isEmpty() && upcoming.isEmpty() && completed.isEmpty()) {
                item {
                    EmptyStateCard(
                        emoji    = "✓",
                        title    = "All clear",
                        subtitle = "No todos right now. Tap + to add something.",
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showSheet) {
        CreateTodoSheet(
            goals     = goals.map { it.goal },
            onDismiss = { showSheet = false },
            onSave    = { title, desc, dueDate, priority, goalId ->
                viewModel.createTodo(title, desc, dueDate, priority, goalId)
                showSheet = false
            },
        )
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun TodoGroupHeader(label: String, color: Color) {
    Text(
        label.uppercase(),
        fontSize    = 10.sp,
        fontWeight  = FontWeight.Bold,
        color       = color,
        letterSpacing = 0.08.sp,
        modifier    = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

// ── SwipeToDismiss wrapper ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeTodoCard(
    todo: Todo,
    isOverdue: Boolean,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    var consumed by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (consumed) return@rememberSwipeToDismissBoxState false
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { consumed = true; onComplete(); false }
                SwipeToDismissBoxValue.EndToStart -> { consumed = true; onDelete(); false }
                else -> false
            }
        },
    )

    LaunchedEffect(todo.todoId) { consumed = false }

    SwipeToDismissBox(
        state                    = dismissState,
        enableDismissFromStartToEnd = !todo.isCompleted,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val target = dismissState.targetValue
            val bg by animateColorAsState(
                targetValue = when (target) {
                    SwipeToDismissBoxValue.StartToEnd -> Sage.copy(alpha = 0.20f)
                    SwipeToDismissBoxValue.EndToStart -> Blush.copy(alpha = 0.20f)
                    else -> Color.Transparent
                },
                label = "swipeBg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .padding(horizontal = 20.dp),
                contentAlignment = when (target) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                },
            ) {
                when (target) {
                    SwipeToDismissBoxValue.StartToEnd ->
                        Icon(Icons.Default.Check, null, tint = Sage)
                    SwipeToDismissBoxValue.EndToStart ->
                        Icon(Icons.Default.Delete, null, tint = Blush)
                    else -> {}
                }
            }
        },
        content = { TodoCard(todo = todo, isOverdue = isOverdue, onComplete = onComplete, onDelete = onDelete) },
    )
}

// ── TodoCard ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodoCard(
    todo: Todo,
    isOverdue: Boolean,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now().toString()
    val bgColor = if (isOverdue) Blush.copy(alpha = 0.06f) else CardBg
    val borderColor = if (isOverdue) Blush.copy(alpha = 0.30f) else Surface3

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when {
                        todo.isCompleted -> Sage
                        isOverdue        -> Blush.copy(alpha = 0.20f)
                        else             -> Color.Transparent
                    }
                )
                .border(
                    1.5.dp,
                    when {
                        todo.isCompleted -> Sage
                        isOverdue        -> Blush
                        else             -> Surface3
                    },
                    RoundedCornerShape(6.dp),
                )
                .clickable(enabled = !todo.isCompleted, onClick = onComplete),
            contentAlignment = Alignment.Center,
        ) {
            when {
                todo.isCompleted -> Icon(Icons.Default.Check, null, tint = Color(0xFF1A1208), modifier = Modifier.size(14.dp))
                isOverdue        -> Text("!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Blush)
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                todo.title,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                color      = if (todo.isCompleted) CardMuted else CardText,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                PriorityBadge(todo.priority)
                todo.dueDate?.let { due ->
                    val isLate = due < today && !todo.isCompleted
                    Text(
                        due,
                        fontSize   = 9.sp,
                        color      = if (isLate) Blush else CardMuted,
                        fontWeight = if (isLate) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                if (todo.goalId != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Gold.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) { Text("⊙ Goal", fontSize = 9.sp, color = Gold) }
                }
                if (todo.completedViaJournal) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Gold.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) { Text("✦ AI", fontSize = 9.sp, color = Gold) }
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (color, label) = when (priority) {
        "URGENT" -> Blush to "URGENT"
        "HIGH"   -> Amber to "HIGH"
        "MEDIUM" -> Sky   to "MED"
        "LOW"    -> Sage  to "LOW"
        else     -> CardMuted to priority
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) { Text(label, fontSize = 9.sp, color = color, fontWeight = FontWeight.SemiBold) }
}

// ── CreateTodoSheet ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTodoSheet(
    goals: List<Goal>,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String?, dueDate: String?, priority: String, goalId: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate     by remember { mutableStateOf<String?>(null) }
    var priority    by remember { mutableStateOf("MEDIUM") }
    var goalId      by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        dueDate = Instant.ofEpochMilli(ms)
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
            Text("New Todo", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = CardText)

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
                value         = description,
                onValueChange = { description = it },
                label         = { Text("Description (optional)", color = CardMuted) },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 2,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Gold,
                    unfocusedBorderColor = Surface3,
                    focusedTextColor     = CardText,
                    unfocusedTextColor   = CardText,
                ),
            )

            // Due date
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
                Text("Due date", fontSize = 13.sp, color = CardMuted)
                Text(dueDate ?: "Pick a date", fontSize = 13.sp, color = if (dueDate != null) Gold else CardMuted, fontStyle = if (dueDate == null) FontStyle.Italic else FontStyle.Normal)
            }

            // Priority chips
            Text("Priority", fontSize = 11.sp, color = CardMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("URGENT", "HIGH", "MEDIUM", "LOW").forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick  = { priority = p },
                        label    = { Text(p, fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold.copy(alpha = 0.20f),
                            selectedLabelColor     = Gold,
                        ),
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(if (title.isNotBlank()) Gold else Surface3)
                    .clickable(enabled = title.isNotBlank()) {
                        onSave(title, description.ifBlank { null }, dueDate, priority, goalId)
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Save Todo", fontWeight = FontWeight.Bold, color = if (title.isNotBlank()) Color(0xFF1A1208) else CardMuted)
            }
        }
    }
}
