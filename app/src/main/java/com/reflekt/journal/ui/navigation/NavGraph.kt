package com.reflekt.journal.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.reflekt.journal.ui.theme.LightBackground
import com.reflekt.journal.ui.screens.analytics.AnalyticsScreen
import com.reflekt.journal.ui.screens.auth.AuthScreen
import com.reflekt.journal.ui.screens.history.HistoryScreen
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
import com.reflekt.journal.ui.screens.track.GoalDetailScreen
import com.reflekt.journal.ui.screens.track.TrackScreen
import com.reflekt.journal.ui.screens.wellbeing.MicrotaskScreen
import com.reflekt.journal.ui.screens.wellbeing.UnlockSuccessScreen
import com.reflekt.journal.ui.screens.wellbeing.WellbeingScreen

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

    /** Navigate to history with a pre-selected mood filter (from analytics donut). */
    fun historyFiltered(mood: com.reflekt.journal.ai.engine.MoodTag) =
        "history?moodFilter=${mood.name}"

}

// ── Bottom nav items ──────────────────────────────────────────────────────────

private data class NavItem(val route: String, val emoji: String, val label: String)

private val NAV_ITEMS = listOf(
    NavItem(Routes.HOME,      "🏠", "Home"),
    NavItem(Routes.JOURNAL_NEW, "✏️", "Journal"),
    NavItem(Routes.HABITS,    "✅", "Track"),
    NavItem(Routes.HISTORY,   "📋", "History"),
    NavItem(Routes.ANALYTICS, "📊", "Insights"),
    NavItem(Routes.WELLBEING, "📱", "Wellbeing"),
    NavItem(Routes.SETTINGS,  "⚙️", "Settings"),
)

private val BOTTOM_NAV_ROUTES = setOf(
    Routes.HOME, Routes.JOURNAL_NEW,
    Routes.HABITS, Routes.TODOS, Routes.GOALS,
    Routes.HISTORY, Routes.ANALYTICS,
    Routes.WELLBEING, Routes.SETTINGS,
)

@Composable
fun ReflektBottomNav(currentRoute: String?, navController: NavHostController) {
    val isLightMode = MaterialTheme.colorScheme.background == LightBackground
    val navBg       = if (isLightMode) Color(0xFFE8E1D4) else Color(0xFF161B27)
    val gold        = Color(0xFFC9A96E)
    val muted       = Color(0x80EEEAE2)

    // Which nav item is "active"
    val activeRoute = when (currentRoute) {
        Routes.TODOS, Routes.GOALS -> Routes.HABITS   // Track tab
        else -> currentRoute
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(navBg),
    ) {
        // Top border line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0x0FFFFFFF))
                .align(Alignment.TopCenter),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp, vertical = 0.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            NAV_ITEMS.forEach { item ->
                val isActive = activeRoute == item.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(13.dp))
                        .background(if (isActive) gold.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable {
                            navController.navigate(item.route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                        .padding(vertical = 5.dp, horizontal = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(item.emoji, fontSize = 19.sp, lineHeight = 19.sp)
                    Text(
                        item.label,
                        fontSize   = 8.sp,
                        fontWeight = FontWeight.Medium,
                        color      = if (isActive) gold else muted,
                    )
                }
            }
        }
    }
}

// ── NavGraph ──────────────────────────────────────────────────────────────────
@Composable
fun ReflektNavGraph(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomNav = currentRoute in BOTTOM_NAV_ROUTES
        || currentRoute?.startsWith("history?") == true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomNav) {
                ReflektBottomNav(currentRoute = currentRoute, navController = navController)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Routes.SPLASH,
            modifier         = Modifier.padding(innerPadding),
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

        // 11. History — optional moodFilter query param set by Analytics donut tap
        composable(
            route = "${Routes.HISTORY}?moodFilter={moodFilter}",
            arguments = listOf(
                navArgument("moodFilter") {
                    type         = NavType.StringType
                    nullable     = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val moodFilter = backStackEntry.arguments?.getString("moodFilter")
            HistoryScreen(navController = navController, initialMoodFilter = moodFilter)
        }

        // 12. Analytics
        composable(Routes.ANALYTICS) {
            AnalyticsScreen(navController = navController)
        }

        // 13. Track — Habits (default tab = 0)
        composable(Routes.HABITS) {
            TrackScreen(navController = navController, startTab = 0)
        }

        // 14. Track — Todos (start on tab 1)
        composable(Routes.TODOS) {
            TrackScreen(navController = navController, startTab = 1)
        }

        // 15. Track — Goals (start on tab 2)
        composable(Routes.GOALS) {
            TrackScreen(navController = navController, startTab = 2)
        }

        // 16. Goal Detail
        composable(
            route     = Routes.GOAL_DETAIL,
            arguments = listOf(navArgument("goalId") { type = NavType.StringType }),
        ) {
            GoalDetailScreen(navController = navController)
        }

        // 17. Wellbeing
        composable(Routes.WELLBEING) {
            WellbeingScreen(navController = navController)
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
            MicrotaskScreen(navController = navController)
        }

        // 25. Microtask — Success
        composable(Routes.MICROTASK_SUCCESS) {
            UnlockSuccessScreen(navController = navController)
        }
    }
}
}