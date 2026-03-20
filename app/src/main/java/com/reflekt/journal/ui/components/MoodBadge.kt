package com.reflekt.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflekt.journal.ai.engine.MoodTag

@Composable
fun MoodBadge(mood: MoodTag, modifier: Modifier = Modifier) {
    val (emoji, bg, fg) = when (mood) {
        MoodTag.HAPPY   -> Triple("😊", Color(0x266FA880), Color(0xFF6FA880))
        MoodTag.SAD     -> Triple("😢", Color(0x265F9FC4), Color(0xFF5F9FC4))
        MoodTag.ANXIOUS -> Triple("😰", Color(0x269B85C8), Color(0xFF9B85C8))
        MoodTag.ANGRY   -> Triple("😠", Color(0x26D4756A), Color(0xFFD4756A))
        MoodTag.NEUTRAL -> Triple("😐", Color(0x26C9A96E), Color(0xFFC9A96E))
        MoodTag.FEAR    -> Triple("😨", Color(0x26A89080), Color(0xFFA89080))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("$emoji ${mood.name.lowercase().replaceFirstChar { it.uppercase() }}", fontSize = 10.sp, color = fg)
    }
}
