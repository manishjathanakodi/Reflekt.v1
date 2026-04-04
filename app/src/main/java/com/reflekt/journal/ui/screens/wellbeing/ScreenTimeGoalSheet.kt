package com.reflekt.journal.ui.screens.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflekt.journal.ui.components.PrimaryButton
import com.reflekt.journal.ui.theme.DarkTertiary

private val GoalGold    = Color(0xFFC9A96E)
private val GoalBg      = Color(0xFF1A2030)
private val GoalCard    = Color(0xFF1E2538)
private val GoalDivider = Color(0xFF2A3450)
private val GoalText    = Color(0xFFEEEAE2)
private val GoalLav     = DarkTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTimeGoalSheet(
    currentGoalMinutes: Int,
    onSave: (minutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initHours   = currentGoalMinutes / 60
    val initMinutes = (currentGoalMinutes % 60).let { m -> listOf(0, 15, 30, 45).minByOrNull { kotlin.math.abs(it - m) } ?: 0 }

    var hours   by remember { mutableIntStateOf(if (currentGoalMinutes > 0) initHours else 2) }
    var minutes by remember { mutableIntStateOf(if (currentGoalMinutes > 0) initMinutes else 0) }

    val totalMinutes = hours * 60 + minutes

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = GoalBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Title
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text  = "Set your daily screen time goal",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GoalText,
                )
                Text(
                    text  = "We'll show you when you're getting close to your limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = GoalDivider)

            // Preset chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60 to "1 hour", 120 to "2 hours", 180 to "3 hours", 240 to "4 hours")
                    .forEach { (value, label) ->
                        val sel = totalMinutes == value
                        Box(
                            modifier = Modifier
                                .background(
                                    if (sel) GoalGold.copy(alpha = 0.18f) else GoalCard,
                                    RoundedCornerShape(8.dp),
                                )
                                .border(
                                    1.dp,
                                    if (sel) GoalGold.copy(alpha = 0.55f) else GoalDivider,
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable { hours = value / 60; minutes = 0 }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text  = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sel) GoalGold else GoalText.copy(alpha = 0.55f),
                            )
                        }
                    }
            }

            // Hours picker
            PickerRow(
                label    = "Hours",
                value    = hours,
                onMinus  = { if (hours > 0) hours-- },
                onPlus   = { if (hours < 12) hours++ },
                display  = "$hours",
            )

            // Minutes picker
            PickerRow(
                label    = "Minutes",
                value    = minutes,
                onMinus  = { minutes = ((minutes - 15).let { if (it < 0) 45 else it }) },
                onPlus   = { minutes = (minutes + 15) % 60 },
                display  = "${minutes.toString().padStart(2, '0')}",
            )

            // Live preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoalCard, RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = "Goal: ${formatGoalPreview(hours, minutes)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontStyle  = FontStyle.Italic,
                    ),
                    color = GoalGold,
                )
            }

            // Save button
            PrimaryButton(
                text     = "Set goal",
                enabled  = totalMinutes > 0,
                onClick  = { onSave(totalMinutes) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PickerRow(
    label: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    display: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = GoalText,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick  = onMinus,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF252D44), CircleShape),
            ) {
                Text(
                    text  = "−",
                    fontSize = 18.sp,
                    color = GoalText,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text  = display,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = GoalGold,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(
                onClick  = onPlus,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF252D44), CircleShape),
            ) {
                Text(
                    text  = "+",
                    fontSize = 18.sp,
                    color = GoalText,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun formatGoalPreview(hours: Int, minutes: Int): String {
    val total = hours * 60 + minutes
    if (total == 0) return "No limit"
    return when {
        hours == 0  -> "${minutes}min per day"
        minutes == 0 -> "${hours}h per day"
        else         -> "${hours}h ${minutes}min per day"
    }
}
