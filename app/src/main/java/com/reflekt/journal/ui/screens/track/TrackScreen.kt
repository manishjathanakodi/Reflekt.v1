package com.reflekt.journal.ui.screens.track

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

// ── Colour tokens ─────────────────────────────────────────────────────────────
private val Gold = Color(0xFFC9A96E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    navController: NavController,
    startTab: Int = 0,
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingUndoTodoId by viewModel.pendingUndoTodoId.collectAsState()

    // Show undo snackbar when a todo is pending undo
    LaunchedEffect(pendingUndoTodoId) {
        val todoId = pendingUndoTodoId ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message    = "Todo removed",
            actionLabel = "UNDO",
            withDismissAction = false,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDeleteTodo(todoId)
        }
        viewModel.setPendingUndo(null)
    }

    var selectedTab by remember { mutableIntStateOf(startTab) }
    val tabs = listOf("Habits", "Todos", "Goals")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.background,
                contentColor     = Gold,
                edgePadding      = 16.dp,
                indicator        = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color    = Gold,
                        )
                    }
                },
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Medium else FontWeight.Normal,
                                color = if (selectedTab == index) Gold
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }

            when (selectedTab) {
                0 -> HabitsTab(viewModel = viewModel)
                1 -> TodosTab(viewModel = viewModel)
                2 -> GoalsTab(navController = navController, viewModel = viewModel)
            }
        }
    }
}
