package com.reflekt.journal.ui.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.reflekt.journal.ai.engine.MoodTag
import com.reflekt.journal.ui.components.PrimaryButton
import com.reflekt.journal.ui.navigation.Routes

private val SaveCardBg  = Color(0xFF1A2030)
private val SaveInputBg = Color(0xFF1E2538)
private val SaveGold    = Color(0xFFC9A96E)
private val SaveSage    = Color(0xFF6FA880)
private val SaveText    = Color(0xFFEEEAE2)
private val SaveMuted   = Color(0x80EEEAE2)

private fun moodEmoji(mood: MoodTag?) = when (mood) {
    MoodTag.HAPPY   -> "😊"
    MoodTag.SAD     -> "😔"
    MoodTag.ANXIOUS -> "😰"
    MoodTag.ANGRY   -> "😤"
    MoodTag.NEUTRAL -> "😐"
    MoodTag.FEAR    -> "😨"
    null            -> "📝"
}

@Composable
fun PostJournalSaveScreen(
    navController: NavController,
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val saveState by viewModel.structuredSaveState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Title ──────────────────────────────────────────────────────────────
        Text(
            "Entry saved ✦",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (saveState != null) {
            val state = saveState!!

            // ── Mood comparison card ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SaveCardBg)
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "MOOD SHIFT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaveGold,
                        letterSpacing = 0.1.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(moodEmoji(state.initialMood), fontSize = 32.sp)
                            Text(
                                state.initialMood?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Not set",
                                fontSize = 10.sp,
                                color = SaveMuted,
                            )
                        }
                        Text(
                            "  →  ",
                            fontSize = 18.sp,
                            color = SaveGold,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(moodEmoji(state.closingMood), fontSize = 32.sp)
                            Text(
                                state.closingMood?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Not set",
                                fontSize = 10.sp,
                                color = SaveMuted,
                            )
                        }
                    }
                }
            }

            // ── Affirmation echo ───────────────────────────────────────────────
            if (state.affirmation.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SaveSage.copy(alpha = 0.10f))
                        .padding(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "✦ YOUR AFFIRMATION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaveSage,
                            letterSpacing = 0.1.sp,
                        )
                        Text(
                            state.affirmation,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = SaveText,
                        )
                    }
                }
            }

            // ── Section completion dots ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SaveCardBg)
                    .padding(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "SECTIONS COMPLETED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaveMuted,
                        letterSpacing = 0.1.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(8) { index ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (index < state.filledSections) SaveGold
                                        else SaveInputBg,
                                    ),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${state.filledSections} / 8",
                            fontSize = 10.sp,
                            color = SaveGold,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // ── Quote ──────────────────────────────────────────────────────────
            if (state.quote.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SaveInputBg)
                        .padding(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "✦ TODAY'S QUOTE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaveGold,
                            letterSpacing = 0.1.sp,
                        )
                        Text(
                            state.quote,
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = SaveMuted,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Navigation buttons ─────────────────────────────────────────────────
        PrimaryButton(
            text = "Back to home",
            onClick = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                }
            },
        )

        PrimaryButton(
            text = "View history",
            onClick = { navController.navigate(Routes.HISTORY) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
}
