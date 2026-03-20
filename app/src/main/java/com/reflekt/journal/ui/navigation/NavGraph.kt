package com.reflekt.journal.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.reflekt.journal.ui.screens.auth.AuthScreen
import com.reflekt.journal.ui.screens.home.HomeScreen
import com.reflekt.journal.ui.screens.journal.JournalEntryScreen
import com.reflekt.journal.ui.screens.journal.JournalViewModel
import com.reflekt.journal.ui.screens.journal.PostJournalSaveScreen
import com.reflekt.journal.ui.screens.onboarding.DemographicsScreen
import com.reflekt.journal.ui.screens.onboarding.OnboardingViewModel
import com.reflekt.journal.ui.screens.onboarding.PermissionsScreen
import com.reflekt.journal.ui.screens.onboarding.RelationMapScreen
import com.reflekt.journal.ui.screens.onboarding.StatusScreen
import com.reflekt.journal.ui.screens.splash.SplashScreen

// ── Route constants ───────────────────────────────────────────────────────────
object Routes {
    const val SPLASH                 = "splash"
    const val ONBOARDING_DEMO        = "onboarding/demographics"
    const val ONBOARDING_STATUS      = "onboarding/status"
    const val ONBOARDING_RELATIONS   = "onboarding/relations"
    const val ONBOARDING_PERMISSIONS = "onboarding/permissions"
    const val AUTH_LOGIN             = "auth/login"
    const val HOME                   = "home"
    const val JOURNAL_NEW            = "journal/new"
    const val JOURNAL_ENTRY          = "journal/{entryId}"
    const val JOURNAL_SAVED          = "journal/saved"
    const val HISTORY                = "history"
    const val ANALYTICS              = "analytics"
    const val HABITS                 = "habits"
    const val TODOS                  = "todos"
    const val GOALS                  = "goals"
    const val GOAL_DETAIL            = "goals/{goalId}"
    const val WELLBEING              = "wellbeing"
    const val WELLBEING_APP          = "wellbeing/app/{packageName}"
    const val SETTINGS               = "settings"
    const val SETTINGS_EXPORT        = "settings/export"
    const val SETTINGS_IMPORT        = "settings/import"
    const val CRISIS                 = "crisis"
    const val BLOCKED                = "blocked/{packageName}"
    const val MICROTASK              = "microtask/{taskType}"
    const val MICROTASK_SUCCESS      = "microtask/success"

    fun journalEntry(entryId: String)     = "journal/$entryId"
    fun goalDetail(goalId: String)        = "goals/$goalId"
    fun wellbeingApp(packageName: String) = "wellbeing/app/$packageName"
    fun blocked(packageName: String)      = "blocked/$packageName"
    fun microtask(taskType: String)       = "microtask/$taskType"
}

// ── NavGraph ──────────────────────────────────────────────────────────────────
@Composable
fun ReflektNavGraph(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = Routes.SPLASH,
    ) {
        // 1. Splash
        composable(Routes.SPLASH) {
            SplashScreen(navController)
        }

        // 2. Onboarding — Demographics (owns the shared OnboardingViewModel)
        composable(Routes.ONBOARDING_DEMO) {
            val viewModel: OnboardingViewModel = hiltViewModel()
            DemographicsScreen(viewModel, navController)
        }

        // 3. Onboarding — Status (shares OnboardingViewModel scoped to ONBOARDING_DEMO)
        composable(Routes.ONBOARDING_STATUS) { entry ->
            val demoEntry = remember(entry) {
                navController.getBackStackEntry(Routes.ONBOARDING_DEMO)
            }
            val viewModel: OnboardingViewModel = hiltViewModel(demoEntry)
            StatusScreen(viewModel, navController)
        }

        // 4. Onboarding — Relations
        composable(Routes.ONBOARDING_RELATIONS) { entry ->
            val demoEntry = remember(entry) {
                navController.getBackStackEntry(Routes.ONBOARDING_DEMO)
            }
            val viewModel: OnboardingViewModel = hiltViewModel(demoEntry)
            RelationMapScreen(viewModel, navController)
        }

        // 5. Onboarding — Permissions
        composable(Routes.ONBOARDING_PERMISSIONS) { entry ->
            val demoEntry = remember(entry) {
                navController.getBackStackEntry(Routes.ONBOARDING_DEMO)
            }
            val viewModel: OnboardingViewModel = hiltViewModel(demoEntry)
            PermissionsScreen(viewModel, navController)
        }

        // 6. Auth — Login
        composable(Routes.AUTH_LOGIN) {
            AuthScreen(navController)
        }

        // 7. Home
        composable(Routes.HOME) {
            HomeScreen(navController)
        }

        // 8. Journal — New entry (owns the shared JournalViewModel)
        composable(Routes.JOURNAL_NEW) {
            JournalEntryScreen(navController)
        }

        // 9. Journal — Existing entry
        composable(
            route     = Routes.JOURNAL_ENTRY,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
        ) {
            Text("JournalEntryScreen placeholder")
        }

        // 10. Journal — Post-save (shares JournalViewModel scoped to JOURNAL_NEW)
        composable(Routes.JOURNAL_SAVED) { entry ->
            val journalEntry = remember(entry) {
                navController.getBackStackEntry(Routes.JOURNAL_NEW)
            }
            val viewModel: JournalViewModel = hiltViewModel(journalEntry)
            PostJournalSaveScreen(navController, viewModel)
        }

        // 11. History
        composable(Routes.HISTORY) {
            Text("HistoryScreen placeholder")
        }

        // 12. Analytics
        composable(Routes.ANALYTICS) {
            Text("AnalyticsScreen placeholder")
        }

        // 13. Track — Habits
        composable(Routes.HABITS) {
            Text("TrackScreen (Habits) placeholder")
        }

        // 14. Track — Todos
        composable(Routes.TODOS) {
            Text("TrackScreen (Todos) placeholder")
        }

        // 15. Track — Goals
        composable(Routes.GOALS) {
            Text("TrackScreen (Goals) placeholder")
        }

        // 16. Goal Detail
        composable(
            route     = Routes.GOAL_DETAIL,
            arguments = listOf(navArgument("goalId") { type = NavType.StringType }),
        ) {
            Text("GoalDetailScreen placeholder")
        }

        // 17. Wellbeing
        composable(Routes.WELLBEING) {
            Text("WellbeingScreen placeholder")
        }

        // 18. Wellbeing — App detail
        composable(
            route     = Routes.WELLBEING_APP,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
        ) {
            Text("AppDetailScreen placeholder")
        }

        // 19. Settings
        composable(Routes.SETTINGS) {
            Text("SettingsScreen placeholder")
        }

        // 20. Settings — Export
        composable(Routes.SETTINGS_EXPORT) {
            Text("ExportScreen placeholder")
        }

        // 21. Settings — Import
        composable(Routes.SETTINGS_IMPORT) {
            Text("ImportScreen placeholder")
        }

        // 22. Crisis
        composable(Routes.CRISIS) {
            Text("CrisisScreen placeholder")
        }

        // 23. Blocked
        composable(
            route     = Routes.BLOCKED,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
        ) {
            Text("BlockedScreen placeholder")
        }

        // 24. Microtask
        composable(
            route     = Routes.MICROTASK,
            arguments = listOf(navArgument("taskType") { type = NavType.StringType }),
        ) {
            Text("MicrotaskScreen placeholder")
        }

        // 25. Microtask — Success
        composable(Routes.MICROTASK_SUCCESS) {
            Text("UnlockSuccessScreen placeholder")
        }
    }
}
