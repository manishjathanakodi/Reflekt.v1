package com.reflekt.journal.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

// ── Route constants ───────────────────────────────────────────────────────────
object Routes {
    const val SPLASH                = "splash"
    const val ONBOARDING_DEMO       = "onboarding/demographics"
    const val ONBOARDING_STATUS     = "onboarding/status"
    const val ONBOARDING_RELATIONS  = "onboarding/relations"
    const val ONBOARDING_PERMISSIONS = "onboarding/permissions"
    const val AUTH_LOGIN            = "auth/login"
    const val HOME                  = "home"
    const val JOURNAL_NEW           = "journal/new"
    const val JOURNAL_ENTRY         = "journal/{entryId}"
    const val JOURNAL_SAVED         = "journal/saved"
    const val HISTORY               = "history"
    const val ANALYTICS             = "analytics"
    const val HABITS                = "habits"
    const val TODOS                 = "todos"
    const val GOALS                 = "goals"
    const val GOAL_DETAIL           = "goals/{goalId}"
    const val WELLBEING             = "wellbeing"
    const val WELLBEING_APP         = "wellbeing/app/{packageName}"
    const val SETTINGS              = "settings"
    const val SETTINGS_EXPORT       = "settings/export"
    const val SETTINGS_IMPORT       = "settings/import"
    const val CRISIS                = "crisis"
    const val BLOCKED               = "blocked/{packageName}"
    const val MICROTASK             = "microtask/{taskType}"
    const val MICROTASK_SUCCESS     = "microtask/success"

    fun journalEntry(entryId: String)    = "journal/$entryId"
    fun goalDetail(goalId: String)       = "goals/$goalId"
    fun wellbeingApp(packageName: String) = "wellbeing/app/$packageName"
    fun blocked(packageName: String)     = "blocked/$packageName"
    fun microtask(taskType: String)      = "microtask/$taskType"
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
            Text("SplashScreen placeholder")
        }

        // 2. Onboarding — Demographics
        composable(Routes.ONBOARDING_DEMO) {
            Text("DemographicsScreen placeholder")
        }

        // 3. Onboarding — Status
        composable(Routes.ONBOARDING_STATUS) {
            Text("StatusScreen placeholder")
        }

        // 4. Onboarding — Relations
        composable(Routes.ONBOARDING_RELATIONS) {
            Text("RelationMapScreen placeholder")
        }

        // 5. Onboarding — Permissions
        composable(Routes.ONBOARDING_PERMISSIONS) {
            Text("PermissionsScreen placeholder")
        }

        // 6. Auth — Login
        composable(Routes.AUTH_LOGIN) {
            Text("AuthScreen placeholder")
        }

        // 7. Home
        composable(Routes.HOME) {
            Text("HomeScreen placeholder")
        }

        // 8. Journal — New entry
        composable(Routes.JOURNAL_NEW) {
            Text("JournalEntryScreen placeholder")
        }

        // 9. Journal — Existing entry
        composable(
            route     = Routes.JOURNAL_ENTRY,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
        ) {
            Text("JournalEntryScreen placeholder")
        }

        // 10. Journal — Post-save
        composable(Routes.JOURNAL_SAVED) {
            Text("PostJournalSaveScreen placeholder")
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
