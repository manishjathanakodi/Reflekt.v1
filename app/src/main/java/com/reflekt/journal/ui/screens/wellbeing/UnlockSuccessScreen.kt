package com.reflekt.journal.ui.screens.wellbeing

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.reflekt.journal.data.db.InterventionDao
import com.reflekt.journal.ui.navigation.Routes
import com.reflekt.journal.ui.theme.DarkSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Sage = DarkSecondary        // #6FA880
private val Teal = Color(0xFF4DB8A4)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class UnlockSuccessViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val interventionDao: InterventionDao,
) : ViewModel() {

    private val ACCESS_WINDOW_MS = 30 * 60 * 1000L // 30 minutes

    private val _accessTimerMs = MutableStateFlow(ACCESS_WINDOW_MS)
    val accessTimerMs: StateFlow<Long> = _accessTimerMs

    val lastResolvedPackage: StateFlow<String> =
        interventionDao.getAll().map { list ->
            list.firstOrNull { it.status == "RESOLVED" }?.packageName ?: ""
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val taskStreak: StateFlow<Int> =
        interventionDao.getAll().map { list ->
            list.count { it.microtaskCompleted }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val totalTasksDone: StateFlow<Int> = taskStreak

    init {
        startAccessTimer()
    }

    private fun startAccessTimer() {
        viewModelScope.launch {
            while (_accessTimerMs.value > 0) {
                delay(1_000)
                _accessTimerMs.value = (_accessTimerMs.value - 1_000).coerceAtLeast(0)
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun UnlockSuccessScreen(navController: NavController) {
    val vm: UnlockSuccessViewModel = hiltViewModel()
    val accessTimerMs by vm.accessTimerMs.collectAsState()
    val packageName   by vm.lastResolvedPackage.collectAsState()
    val streak        by vm.taskStreak.collectAsState()
    val totalTasks    by vm.totalTasksDone.collectAsState()
    val context       = LocalContext.current

    val accessWindowMs = 30 * 60 * 1000L
    val progress = accessTimerMs.toFloat() / accessWindowMs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    listOf(Sage.copy(alpha = 0.10f), Color.Transparent)
                )
            )
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SuccessRing()
            Spacer(Modifier.height(20.dp))

            Text(
                text      = "Well done.",
                style     = MaterialTheme.typography.headlineMedium,
                color     = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text      = "You completed your mindful pause. The app is unlocked for 30 minutes. Take it slow.",
                style     = MaterialTheme.typography.bodySmall.copy(lineHeight = 22.sp),
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(26.dp))

            StatCards(streak = streak, accessWindow = "30m", totalTasks = totalTasks)
            Spacer(Modifier.height(20.dp))

            AccessTimerCard(timerMs = accessTimerMs, progress = progress, packageName = packageName)
            Spacer(Modifier.height(18.dp))

            if (packageName.isNotBlank()) {
                OpenAppButton(packageName = packageName, context = context)
                Spacer(Modifier.height(11.dp))
            }

            ReturnToJournalLink {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = false }
                }
            }
        }
    }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
private fun SuccessRing() {
    var triggered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue    = if (triggered) 1f else 0f,
        animationSpec  = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow,
        ),
        label = "successRingScale",
    )
    LaunchedEffect(Unit) { triggered = true }

    Box(
        modifier = Modifier
            .size(98.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(Sage.copy(alpha = 0.12f))
            .border(2.dp, Sage.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("✓", fontSize = 42.sp, color = Sage)
    }
}

@Composable
private fun StatCards(streak: Int, accessWindow: String, totalTasks: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        StatMiniCard(Modifier.weight(1f), "🔥", "$streak", "Task streak")
        StatMiniCard(Modifier.weight(1f), "⏱️", accessWindow, "Access window")
        StatMiniCard(Modifier.weight(1f), "🧘", "$totalTasks", "Tasks done")
    }
}

private val CardBg   = Color(0xFF1E2538)
private val CardText = Color(0xFFEEEAE2)
private val CardMuted = Color(0x80EEEAE2)

@Composable
private fun StatMiniCard(modifier: Modifier, emoji: String, value: String, label: String) {
    Column(
        modifier = modifier
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            text  = value,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = CardText,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = CardMuted,
        )
    }
}

@Composable
private fun AccessTimerCard(timerMs: Long, progress: Float, packageName: String) {
    val totalSecs = timerMs / 1_000
    val minutes   = totalSecs / 60
    val seconds   = totalSecs % 60
    val timeStr   = "$minutes:${"%02d".format(seconds)} remaining"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text  = "App access",
                style = MaterialTheme.typography.bodySmall,
                color = CardMuted,
            )
            Text(
                text  = timeStr,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = Sage,
            )
        }
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(
            progress  = { progress },
            modifier  = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(2.5.dp)),
            color      = Sage,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun OpenAppButton(packageName: String, context: android.content.Context) {
    val pm        = context.packageManager
    val appLabel  = try {
        val info = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(info).toString()
    } catch (_: Exception) { "App" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50.dp))
            .background(Sage)
            .clickable {
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) context.startActivity(launchIntent)
            }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = "Open $appLabel",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF0D1F14),
        )
    }
}

@Composable
private fun ReturnToJournalLink(onClick: () -> Unit) {
    Text(
        text           = "Return to journal",
        style          = MaterialTheme.typography.bodySmall.copy(
            textDecoration = TextDecoration.None,
        ),
        color          = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier       = Modifier.clickable(onClick = onClick),
    )
}
