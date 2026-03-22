package com.reflekt.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflekt.journal.ai.engine.MoodTag

private val SheetBg  = Color(0xFF1A2030)
private val CellBg   = Color(0xFF1E2538)
private val Gold     = Color(0xFFC9A96E)
private val CardText = Color(0xFFEEEAE2)
private val Muted    = Color(0xFF8A95A8)

private data class MoodOption(val tag: MoodTag, val emoji: String, val label: String)

private val MOODS = listOf(
    MoodOption(MoodTag.HAPPY,   "😊", "Happy"),
    MoodOption(MoodTag.SAD,     "😔", "Sad"),
    MoodOption(MoodTag.ANGRY,   "😤", "Angry"),
    MoodOption(MoodTag.ANXIOUS, "😰", "Anxious"),
    MoodOption(MoodTag.NEUTRAL, "😐", "Neutral"),
    MoodOption(MoodTag.FEAR,    "😨", "Fearful"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodCheckInDialog(
    isOpening: Boolean,
    onMoodSelected: (MoodTag) -> Unit,
    onSkip: () -> Unit,
) {
    var selected by remember { mutableStateOf<MoodTag?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onSkip,
        sheetState = sheetState,
        containerColor = SheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3A4A60)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title + subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    if (isOpening) "How are you feeling right now?" else "How are you feeling now?",
                    style = MaterialTheme.typography.titleLarge,
                    color = CardText,
                )
                Text(
                    if (isOpening) "Tap a mood to start your session" else "Compare to how you started",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }

            // Mood grid — 3 columns × 2 rows
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MOODS.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { mood ->
                            MoodCell(
                                mood = mood,
                                isSelected = selected == mood.tag,
                                onClick = { selected = mood.tag },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // CTA
            PrimaryButton(
                text = if (isOpening) "Start journaling" else "Save & close",
                onClick = { selected?.let { onMoodSelected(it) } },
                enabled = selected != null,
            )

            // Skip
            TextButton(onClick = onSkip) {
                Text("Skip for now", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
    }
}

@Composable
private fun MoodCell(
    mood: MoodOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg     = if (isSelected) Gold.copy(alpha = 0.15f) else CellBg
    val border = if (isSelected) Gold else Color(0xFF2A3548)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(mood.emoji, fontSize = 26.sp)
        Text(
            mood.label,
            fontSize = 11.sp,
            color = if (isSelected) Gold else Muted,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
