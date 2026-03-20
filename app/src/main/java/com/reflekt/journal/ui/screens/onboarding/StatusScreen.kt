package com.reflekt.journal.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.reflekt.journal.ui.components.ChipGroup
import com.reflekt.journal.ui.components.MultiSelectChipGroup
import com.reflekt.journal.ui.components.OnboardingProgressBar
import com.reflekt.journal.ui.components.PrimaryButton
import com.reflekt.journal.ui.navigation.Routes

private val Gold = Color(0xFFC9A96E)
private val CardText = Color(0xFFEEEAE2)

private val familyOptions = listOf("No children", "Parent", "Single parent", "Expecting", "Prefer not to say")
private val struggleOptions = listOf("Social Media", "Gaming", "Streaming", "News", "Work apps")

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StatusScreen(
    viewModel: OnboardingViewModel,
    navController: NavController,
) {
    val draft by viewModel.draft.collectAsState()

    // Convert minutes to slider value: 30–480 min (30 min steps)
    val sliderRange = 30f..480f
    val sliderValue = draft.screenTimeGoalMinutes.toFloat().coerceIn(sliderRange)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        OnboardingProgressBar(currentStep = 2, totalSteps = 4)

        Text(
            text = "✦ Step 2 of 4".toUpperCase(Locale.current),
            style = MaterialTheme.typography.labelSmall,
            color = Gold,
            letterSpacing = androidx.compose.ui.unit.TextUnit(0.1f, androidx.compose.ui.unit.TextUnitType.Em),
        )

        Text(
            "Your digital life",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Family status
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Family status", style = MaterialTheme.typography.labelMedium, color = CardText)
            ChipGroup(
                options = familyOptions,
                selected = draft.familyStatus,
                onSelect = { viewModel.updateDraft { copy(familyStatus = it) } },
            )
        }

        // Digital struggle areas
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Where do you struggle most?", style = MaterialTheme.typography.labelMedium, color = CardText)
            MultiSelectChipGroup(
                options = struggleOptions,
                selected = draft.struggleAreas,
                onToggle = { option ->
                    val updated = draft.struggleAreas.toMutableSet()
                    if (option in updated) updated.remove(option) else updated.add(option)
                    viewModel.updateDraft { copy(struggleAreas = updated) }
                },
            )
        }

        // Screen time goal slider
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Daily screen time goal", style = MaterialTheme.typography.labelMedium, color = CardText)
                Text(
                    formatMinutes(draft.screenTimeGoalMinutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = Gold,
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = { viewModel.updateDraft { copy(screenTimeGoalMinutes = it.toInt()) } },
                valueRange = sliderRange,
                steps = 29, // (480-30)/15 - 1 = 29 steps of 15 min
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Gold,
                    activeTrackColor = Gold,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                thumb = {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .background(Gold, RoundedCornerShape(50))
                            .padding(6.dp),
                    )
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("30 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("8 hr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp))

        PrimaryButton(
            text = "Continue →",
            onClick = {
                viewModel.nextStep()
                navController.navigate(Routes.ONBOARDING_RELATIONS)
            },
        )
    }
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}
