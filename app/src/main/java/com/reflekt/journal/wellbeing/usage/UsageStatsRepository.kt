package com.reflekt.journal.wellbeing.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UsageStatsRepository"

data class AppUsageStat(
    val packageName: String,
    val appLabel: String,
    val durationMs: Long,
    val launchCount: Int,
)

private val SOCIAL_APPS = setOf(
    "com.instagram.android", "com.twitter.android", "com.facebook.katana",
    "com.snapchat.android", "com.zhiliaoapp.musically", "com.reddit.frontpage",
    "com.linkedin.android", "com.whatsapp", "org.telegram.messenger",
    "com.facebook.lite", "com.twitter.lite", "com.instagram.lite",
)

private val STREAMING_APPS = setOf(
    "com.google.android.youtube", "com.netflix.mediaclient", "com.spotify.music",
    "com.amazon.avod.thirdpartyclient", "in.startv.hotstar", "com.jio.jiocinema",
    "com.hotstar.android", "com.primevideo.android",
)

private val GAMING_KEYWORDS = listOf("game", "pubg", "freefire", "codm", "minecraft")

@Singleton
class UsageStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTodayUsage(): List<AppUsageStat> {
        if (!hasPermission()) {
            Log.w(TAG, "PACKAGE_USAGE_STATS permission not granted — returning empty list")
            return emptyList()
        }
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val startOfDay = now - 24 * 60 * 60 * 1000L

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startOfDay, now)
        if (stats.isNullOrEmpty()) return emptyList()

        return stats
            .filter { it.totalTimeInForeground > 0 }
            .map { stat ->
                AppUsageStat(
                    packageName = stat.packageName,
                    appLabel    = getAppLabel(stat.packageName),
                    durationMs  = stat.totalTimeInForeground,
                    launchCount = 0,
                )
            }
    }

    fun getAppLabel(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    fun getAppCategory(packageName: String): String {
        val lower = packageName.lowercase()
        return when {
            SOCIAL_APPS.any { lower.contains(it) || it.contains(lower) } ||
                    lower.contains("social") -> "SOCIAL"
            STREAMING_APPS.any { lower.contains(it) || it.contains(lower) } ||
                    lower.contains("video") || lower.contains("stream") -> "STREAMING"
            GAMING_KEYWORDS.any { lower.contains(it) } -> "GAMING"
            lower.contains("work") || lower.contains("office") ||
                    lower.contains("slack") || lower.contains("zoom") ||
                    lower.contains("meet") || lower.contains("notion") -> "WORK"
            else -> "OTHER"
        }
    }
}
