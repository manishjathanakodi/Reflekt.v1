package com.reflekt.journal.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SageGreen = Color(0xFF6FA880)

@Composable
fun ZeroCloudBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = SageGreen.copy(alpha = 0.20f),
                shape = RoundedCornerShape(13.dp),
            ),
        shape = RoundedCornerShape(13.dp),
        color = SageGreen.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text("🔒", fontSize = 14.sp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Zero-cloud guarantee.",
                    style = MaterialTheme.typography.labelMedium,
                    color = SageGreen,
                )
                Text(
                    "Permissions are used only on-device. No data is transmitted to servers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
