package com.reflekt.journal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmptyStateCard(
    emoji: String,
    title: String,
    subtitle: String,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(emoji, fontSize = 48.sp, lineHeight = 48.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text      = title,
            style     = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color     = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text      = subtitle,
            style     = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (ctaLabel != null && onCta != null) {
            Spacer(Modifier.height(4.dp))
            PrimaryButton(text = ctaLabel, onClick = onCta)
        }
    }
}
