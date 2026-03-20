package com.reflekt.journal.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.reflekt.journal.ui.components.ChipGroup
import com.reflekt.journal.ui.components.OnboardingProgressBar
import com.reflekt.journal.ui.components.PrimaryButton
import com.reflekt.journal.ui.navigation.Routes

private val Gold = Color(0xFFC9A96E)
private val CardSurface = Color(0xFF1E2538)
private val CardText = Color(0xFFEEEAE2)

private val genderOptions = listOf("Male", "Female", "Non-binary", "Prefer not to say")
private val relationshipOptions = listOf("Single", "In a relationship", "Married", "Divorced", "Widowed", "Prefer not to say")

@Composable
fun DemographicsScreen(
    viewModel: OnboardingViewModel,
    navController: NavController,
) {
    val draft by viewModel.draft.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Progress bar
        OnboardingProgressBar(currentStep = 1, totalSteps = 4)

        // Step badge
        Text(
            text = "✦ Step 1 of 4".toUpperCase(Locale.current),
            style = MaterialTheme.typography.labelSmall,
            color = Gold,
            letterSpacing = androidx.compose.ui.unit.TextUnit(0.1f, androidx.compose.ui.unit.TextUnitType.Em),
        )

        Text(
            "Tell us about yourself",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Name
        LabeledTextField(
            label = "Your name",
            value = draft.name,
            onValueChange = { viewModel.updateDraft { copy(name = it) } },
            placeholder = "What should we call you?",
        )

        // Age
        LabeledTextField(
            label = "Age",
            value = draft.age,
            onValueChange = { viewModel.updateDraft { copy(age = it) } },
            placeholder = "e.g. 28",
            keyboardType = KeyboardType.Number,
        )

        // Gender
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Gender", style = MaterialTheme.typography.labelMedium, color = CardText)
            ChipGroup(
                options = genderOptions,
                selected = draft.gender,
                onSelect = { viewModel.updateDraft { copy(gender = it) } },
            )
        }

        // Occupation
        LabeledTextField(
            label = "Occupation",
            value = draft.occupation,
            onValueChange = { viewModel.updateDraft { copy(occupation = it) } },
            placeholder = "e.g. Software Engineer",
        )

        // Industry
        LabeledTextField(
            label = "Industry",
            value = draft.industry,
            onValueChange = { viewModel.updateDraft { copy(industry = it) } },
            placeholder = "e.g. Technology",
        )

        // Relationship status
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Relationship status", style = MaterialTheme.typography.labelMedium, color = CardText)
            ChipGroup(
                options = relationshipOptions,
                selected = draft.relationshipStatus,
                onSelect = { viewModel.updateDraft { copy(relationshipStatus = it) } },
            )
        }

        Spacer(Modifier.height(8.dp))

        PrimaryButton(
            text = "Continue →",
            onClick = {
                viewModel.nextStep()
                navController.navigate(Routes.ONBOARDING_STATUS)
            },
        )
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFFEEEAE2))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E2538),
                unfocusedContainerColor = Color(0xFF1E2538),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFF252D44),
                focusedTextColor = Color(0xFFEEEAE2),
                unfocusedTextColor = Color(0xFFEEEAE2),
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
        )
    }
}
