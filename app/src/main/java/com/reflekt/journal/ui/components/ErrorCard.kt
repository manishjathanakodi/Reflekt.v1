package com.reflekt.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflekt.journal.ui.components.PrimaryButton
import com.reflekt.journal.ui.theme.DarkError

private val Blush     = DarkError        // #D4756A
private val CardBg    = Color(0xFF1E2538)

@Composable
fun ErrorCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .border(1.dp, Blush.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(18.dp),
    ) {
        Text(
            text  = "⚠ Something went wrong",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Blush,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text  = message,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            color = Color(0xFFEEEAE2),
        )
        if (onRetry != null) {
            Spacer(Modifier.height(14.dp))
            PrimaryButton(text = "Try again", onClick = onRetry)
        }
    }
}
