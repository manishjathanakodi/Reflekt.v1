package com.reflekt.journal.ui.screens.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.reflekt.journal.ai.accountability.AccountabilitySnapshot
import com.reflekt.journal.ai.engine.MoodTag
import com.reflekt.journal.ui.components.LoadingDots
import com.reflekt.journal.ui.components.MoodBadge
import com.reflekt.journal.ui.components.MoodCheckInDialog
import com.reflekt.journal.ui.navigation.Routes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Colour tokens ─────────────────────────────────────────────────────────────
private val ChatCardBg     = Color(0xFF1A2030)
private val ChatInputBg    = Color(0xFF1E2538)
private val ChatAiBubbleBg = Color(0xFF1E2538)
private val ChatUserBubble = Color(0xFFC9A96E)
private val ChatGold       = Color(0xFFC9A96E)
private val ChatSageGreen  = Color(0xFF6FA880)
private val ChatCardText   = Color(0xFFEEEAE2)

@Composable
fun JournalChatScreen(
    navController: NavController,
    viewModel: JournalChatViewModel = hiltViewModel(),
) {
    val conversation    by viewModel.conversation.collectAsState()
    val isGenerating    by viewModel.isGenerating.collectAsState()
    val isInitializing  by viewModel.isInitializing.collectAsState()
    val liveAnalysis    by viewModel.liveAnalysis.collectAsState()
    val snapshot        by viewModel.accountabilitySnapshot.collectAsState()
    val inputText       by viewModel.inputText.collectAsState()
    val listState       = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClosingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(conversation.size, isGenerating) {
        if (conversation.isNotEmpty()) {
            listState.animateScrollToItem(conversation.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                ChatNavEvent.NavigateToCrisis -> navController.navigate(Routes.CRISIS) {
                    popUpTo(Routes.JOURNAL_CHAT) { inclusive = true }
                }
                ChatNavEvent.NavigateToSaved -> navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                }
                ChatNavEvent.ShowClosingDialog -> showClosingDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.moodSnackbar.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (showClosingDialog) {
        MoodCheckInDialog(
            isOpening = false,
            onMoodSelected = { mood ->
                showClosingDialog = false
                viewModel.onClosingMoodSet(mood)
            },
            onSkip = {
                showClosingDialog = false
                viewModel.onClosingDialogSkipped()
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            ChatScreenHeader(
                onBack = { navController.popBackStack() },
                onDone = { viewModel.onDone() },
            )

            AnimatedVisibility(visible = isInitializing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E2538))
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = ChatGold,
                        strokeWidth = 2.dp,
                    )
                    Text(
                        "AI model loading — first launch only...",
                        fontSize = 12.sp,
                        color = ChatCardText.copy(alpha = 0.7f),
                    )
                }
            }

            snapshot?.let {
                ChatAccountabilityCard(
                    snapshot = it,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(conversation) { msg ->
                    when (msg) {
                        is ChatMessage.AiMessage  -> ChatAiBubble(msg.text)
                        is ChatMessage.UserMessage -> ChatUserBubble(msg.text)
                    }
                }
                if (isGenerating) {
                    item { ChatTypingIndicator() }
                }
            }

            AnimatedVisibility(visible = liveAnalysis != null, enter = fadeIn()) {
                liveAnalysis?.let {
                    ChatLiveAnalysisCard(
                        analysis = it,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            ChatMessageInputBar(
                text = inputText,
                onTextChange = { viewModel.onInputChanged(it) },
                onSend = { viewModel.onSendMessage(inputText) },
                enabled = !isGenerating && !isInitializing,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1E2538),
                    contentColor = ChatCardText,
                )
            },
        )
    }
}

@Composable
private fun ChatScreenHeader(onBack: () -> Unit, onDone: () -> Unit) {
    val today = LocalDate.now()
    val dateStr = today.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault()))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Chat mode",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onDone) {
            Text("Done", color = ChatGold, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ChatAccountabilityCard(snapshot: AccountabilitySnapshot, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ChatCardBg)
            .clickable { expanded = !expanded }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Today's accountability", style = MaterialTheme.typography.labelMedium, color = ChatCardText)
            Text(
                if (expanded) "Collapse ▲" else "Expand ▼",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            snapshot.habitsDueToday.forEach {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ChatSageGreen),
                )
            }
            snapshot.overdueHabits.take(3).forEach {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE8A84D)),
                )
            }
        }
        if (expanded) {
            if (snapshot.habitsDueToday.isNotEmpty()) {
                Text(
                    "Due today: ${snapshot.habitsDueToday.joinToString(", ") { "${it.emoji} ${it.title}" }}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (snapshot.todayTodos.isNotEmpty()) {
                Text(
                    "Todos: ${snapshot.todayTodos.take(3).joinToString(", ") { it.title }}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChatAiBubble(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(0.85f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "✦ REFLEKT AI",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = ChatGold,
            letterSpacing = 0.1.sp,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(ChatAiBubbleBg)
                .padding(12.dp),
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = ChatCardText)
        }
    }
}

@Composable
private fun ChatUserBubble(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.80f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(ChatUserBubble)
                .padding(12.dp),
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1A1208))
        }
    }
}

@Composable
private fun ChatTypingIndicator() {
    Column(
        modifier = Modifier.fillMaxWidth(0.85f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "✦ REFLEKT AI",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = ChatGold,
            letterSpacing = 0.1.sp,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(ChatAiBubbleBg)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            LoadingDots()
        }
    }
}

@Composable
private fun ChatLiveAnalysisCard(analysis: ChatAnalysisResult, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ChatCardBg)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MoodBadge(analysis.mood)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            Text("Trigger", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                analysis.trigger.ifBlank { "—" },
                fontSize = 10.sp,
                color = ChatCardText,
                maxLines = 1,
            )
        }
        if (analysis.habitDetected != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Auto ✓", fontSize = 9.sp, color = ChatSageGreen)
                Text(analysis.habitDetected, fontSize = 10.sp, color = ChatSageGreen, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ChatMessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    "Write what's on your mind…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = ChatCardText,
                unfocusedTextColor = ChatCardText,
                cursorColor = ChatGold,
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = 4,
        )
        IconButton(
            onClick = onSend,
            enabled = enabled && text.isNotBlank(),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (enabled && text.isNotBlank()) ChatGold else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
