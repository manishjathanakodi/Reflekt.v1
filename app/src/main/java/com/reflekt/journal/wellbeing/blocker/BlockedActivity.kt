package com.reflekt.journal.wellbeing.blocker

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.reflekt.journal.ui.screens.wellbeing.BlockedScreen
import com.reflekt.journal.ui.theme.ReflektTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Standalone Activity launched by [AppBlockerService] as an overlay.
 * Hosts [BlockedScreen] and navigates to the main NavGraph's microtask route
 * by forwarding to MainActivity.
 */
@AndroidEntryPoint
class BlockedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full-screen, no title bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )

        val packageName = intent.getStringExtra("packageName") ?: ""

        setContent {
            ReflektTheme {
                BlockedScreen(
                    packageName          = packageName,
                    onNavigateToMicrotask = { taskType ->
                        // Launch MainActivity with microtask route
                        val intent = android.content.Intent(
                            this,
                            com.reflekt.journal.MainActivity::class.java,
                        ).apply {
                            putExtra("startRoute", "microtask/$taskType")
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        finish()
                    },
                    onFinish = { finish() },
                )
            }
        }
    }
}
