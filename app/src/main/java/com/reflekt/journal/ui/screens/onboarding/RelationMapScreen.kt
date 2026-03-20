package com.reflekt.journal.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.reflekt.journal.ui.components.OnboardingProgressBar
import com.reflekt.journal.ui.components.PrimaryButton
import com.reflekt.journal.ui.navigation.Routes

private val Gold = Color(0xFFC9A96E)
private val CardSurface = Color(0xFF1E2538)
private val CardText = Color(0xFFEEEAE2)

private val emojiOptions = listOf("👨", "👩", "👦", "👧", "👴", "👵", "🧑", "💑")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationMapScreen(
    viewModel: OnboardingViewModel,
    navController: NavController,
) {
    val draft by viewModel.draft.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        OnboardingProgressBar(currentStep = 3, totalSteps = 4)

        Text(
            text = "✦ Step 3 of 4".toUpperCase(Locale.current),
            style = MaterialTheme.typography.labelSmall,
            color = Gold,
            letterSpacing = androidx.compose.ui.unit.TextUnit(0.1f, androidx.compose.ui.unit.TextUnitType.Em),
        )

        Text(
            "Your support network",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            "Add the people who matter most to you. Reflekt will help you understand how your relationships affect your wellbeing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Relation cards
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            draft.relationMap.forEachIndexed { index, entry ->
                RelationCard(
                    entry = entry,
                    onDelete = { viewModel.removeRelation(index) },
                )
            }

            // "Add a person" dashed button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { showSheet = true }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+ Add a person",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        PrimaryButton(
            text = "Continue →",
            onClick = {
                viewModel.nextStep()
                navController.navigate(Routes.ONBOARDING_PERMISSIONS)
            },
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = CardSurface,
        ) {
            AddPersonSheet(
                onAdd = { entry ->
                    viewModel.addRelation(entry)
                    showSheet = false
                },
                onDismiss = { showSheet = false },
            )
        }
    }
}

@Composable
private fun RelationCard(
    entry: RelationEntry,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF252D44)),
            contentAlignment = Alignment.Center,
        ) {
            Text(entry.emoji, fontSize = 22.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.bodyMedium, color = CardText)
            Text(entry.role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPersonSheet(
    onAdd: (RelationEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🧑") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Add a person",
            style = MaterialTheme.typography.titleMedium,
            color = CardText,
        )

        // Emoji picker
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            emojiOptions.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (emoji == selectedEmoji) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color(0xFF252D44),
                        )
                        .border(
                            width = if (emoji == selectedEmoji) 2.dp else 0.dp,
                            color = if (emoji == selectedEmoji) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { selectedEmoji = emoji },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, fontSize = 24.sp)
                }
            }
        }

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF252D44),
                unfocusedContainerColor = Color(0xFF252D44),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFF252D44),
                focusedTextColor = CardText,
                unfocusedTextColor = CardText,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            singleLine = true,
        )

        // Role field
        OutlinedTextField(
            value = role,
            onValueChange = { role = it },
            label = { Text("Role", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            placeholder = { Text("e.g. Partner, Best friend, Therapist", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF252D44),
                unfocusedContainerColor = Color(0xFF252D44),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFF252D44),
                focusedTextColor = CardText,
                unfocusedTextColor = CardText,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            singleLine = true,
        )

        PrimaryButton(
            text = "Add",
            onClick = {
                if (name.isNotBlank()) {
                    onAdd(RelationEntry(name = name.trim(), role = role.trim(), emoji = selectedEmoji))
                }
            },
            enabled = name.isNotBlank(),
        )
    }
}
