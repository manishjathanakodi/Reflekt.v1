package com.reflekt.journal.ui.screens.journal

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.reflekt.journal.ai.engine.MoodTag
import com.reflekt.journal.ui.navigation.Routes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Colour tokens (always dark — per CLAUDE.md) ───────────────────────────────
private val JCardBg   = Color(0xFF1A2030)
private val JInputBg  = Color(0xFF1E2538)
private val JGold     = Color(0xFFC9A96E)
private val JSage     = Color(0xFF6FA880)
private val JCardText = Color(0xFFEEEAE2)
private val JMuted    = Color(0x80EEEAE2)

private val MOOD_OPTIONS = listOf(
    MoodTag.HAPPY   to "😊",
    MoodTag.SAD     to "😔",
    MoodTag.ANGRY   to "😤",
    MoodTag.ANXIOUS to "😰",
    MoodTag.NEUTRAL to "😐",
    MoodTag.FEAR    to "😨",
)

@Composable
fun JournalEntryScreen(
    navController: NavController,
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val form       by viewModel.formState.collectAsState()
    val isSaving   by viewModel.isSaving.collectAsState()
    val filled     = countFilledSections(form)

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                JournalNavEvent.NavigateToCrisis -> navController.navigate(Routes.CRISIS) {
                    popUpTo(Routes.JOURNAL_NEW) { inclusive = true }
                }
                JournalNavEvent.NavigateToSaved -> navController.navigate(Routes.JOURNAL_SAVED)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // ── Sticky header ──────────────────────────────────────────────────────
        StructuredHeader(
            onBack = { navController.popBackStack() },
            onSave = { viewModel.onSave() },
            filled = filled,
            total = 8,
            isSaving = isSaving,
        )

        // ── Scrollable body ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // 1. Opening mood
            SectionCard(label = "How are you feeling right now?") {
                InlineMoodPicker(
                    selected = form.initialMood,
                    onSelect = viewModel::onInitialMoodSelected,
                )
            }

            // 2. Affirmation
            SectionCard(label = "Today's affirmation") {
                JournalTextField(
                    value = form.affirmation,
                    onValueChange = viewModel::onAffirmationChanged,
                    placeholder = "I am...",
                )
            }

            // 3–5. Gratitude
            SectionCard(label = "Three things I'm grateful for") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    JournalTextField(
                        value = form.gratitude1,
                        onValueChange = viewModel::onGratitude1Changed,
                        placeholder = "1. I'm grateful for...",
                    )
                    JournalTextField(
                        value = form.gratitude2,
                        onValueChange = viewModel::onGratitude2Changed,
                        placeholder = "2.",
                    )
                    JournalTextField(
                        value = form.gratitude3,
                        onValueChange = viewModel::onGratitude3Changed,
                        placeholder = "3.",
                    )
                }
            }

            // 6. Best part of today
            SectionCard(label = "Best part of today") {
                JournalTextField(
                    value = form.bestPartOfDay,
                    onValueChange = viewModel::onBestPartChanged,
                    placeholder = "The highlight was...",
                )
            }

            // 7. Challenge
            SectionCard(label = "One challenge I faced") {
                JournalTextField(
                    value = form.challenge,
                    onValueChange = viewModel::onChallengeChanged,
                    placeholder = "Something I found difficult...",
                )
            }

            // 8. Free write
            SectionCard(label = "Free write") {
                JournalTextField(
                    value = form.freeWrite,
                    onValueChange = viewModel::onFreeWriteChanged,
                    placeholder = "Let your thoughts flow...",
                    minLines = 4,
                )
            }

            // 9. Tomorrow intention
            SectionCard(label = "Tomorrow, I intend to...") {
                JournalTextField(
                    value = form.tomorrowIntent,
                    onValueChange = viewModel::onTomorrowIntentChanged,
                    placeholder = "One thing I want to do or be...",
                )
            }

            // 10. Closing mood
            SectionCard(label = "How are you feeling now?") {
                InlineMoodPicker(
                    selected = form.closingMood,
                    onSelect = viewModel::onClosingMoodSelected,
                )
            }

            // Quote of the day (read-only)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(JInputBg)
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "✦ TODAY'S QUOTE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = JGold,
                        letterSpacing = 0.1.sp,
                    )
                    Text(
                        viewModel.selectedQuote,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = JMuted,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun StructuredHeader(
    onBack: () -> Unit,
    onSave: () -> Unit,
    filled: Int,
    total: Int,
    isSaving: Boolean,
) {
    val today = LocalDate.now()
    val dateStr = today.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault()))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
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
                    "Today's journal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp),
                    color = JGold,
                    strokeWidth = 2.dp,
                )
            } else {
                TextButton(onClick = onSave) {
                    Text("Save", color = JGold, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else filled.toFloat() / total },
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = JGold,
                trackColor = JInputBg,
                strokeCap = StrokeCap.Round,
            )
            Text(
                "$filled / $total",
                fontSize = 9.sp,
                color = JGold,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SectionCard(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JCardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = JCardText,
        )
        content()
    }
}

@Composable
private fun InlineMoodPicker(selected: MoodTag?, onSelect: (MoodTag) -> Unit) {
    val rows = MOOD_OPTIONS.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (mood, emoji) ->
                    val isSelected = selected == mood
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) JGold.copy(alpha = 0.15f) else JInputBg)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) JGold else Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelect(mood) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(emoji, fontSize = 20.sp)
                            Text(
                                mood.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 9.sp,
                                color = if (isSelected) JGold else JMuted,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = JMuted, fontSize = 13.sp) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = JInputBg,
            unfocusedContainerColor = JInputBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = JCardText,
            unfocusedTextColor = JCardText,
            cursorColor = JGold,
        ),
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        minLines = minLines,
        maxLines = if (minLines > 1) 10 else 4,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = JCardText, fontSize = 13.sp),
    )
}

// Counts how many of the 8 sections are filled
private fun countFilledSections(form: JournalFormState): Int {
    var count = 0
    if (form.initialMood != null) count++
    if (form.affirmation.isNotBlank()) count++
    if (form.gratitude1.isNotBlank() || form.gratitude2.isNotBlank() || form.gratitude3.isNotBlank()) count++
    if (form.bestPartOfDay.isNotBlank()) count++
    if (form.challenge.isNotBlank()) count++
    if (form.freeWrite.isNotBlank()) count++
    if (form.tomorrowIntent.isNotBlank()) count++
    if (form.closingMood != null) count++
    return count
}
