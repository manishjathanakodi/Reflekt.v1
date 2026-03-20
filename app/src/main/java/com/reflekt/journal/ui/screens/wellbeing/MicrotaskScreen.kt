package com.reflekt.journal.ui.screens.wellbeing

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.reflekt.journal.ui.navigation.Routes
import com.reflekt.journal.ui.theme.DarkSecondary
import kotlinx.coroutines.launch

private val Sky  = Color(0xFF5F9FC4)
private val Sage = DarkSecondary

@Composable
fun MicrotaskScreen(navController: NavController) {
    val vm: MicrotaskViewModel = hiltViewModel()
    val isComplete by vm.isComplete.collectAsState()

    LaunchedEffect(isComplete) {
        if (isComplete) {
            navController.navigate(Routes.MICROTASK_SUCCESS) {
                popUpTo(Routes.MICROTASK) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(listOf(Sky.copy(alpha = 0.07f), Color.Transparent))
            )
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (vm.taskType) {
            MicrotaskType.BREATHING -> BreathingContent(vm)
            MicrotaskType.GRATITUDE -> GratitudeContent(vm)
            MicrotaskType.BODY_SCAN -> BodyScanContent(vm)
        }
    }
}

// ── BREATHING ─────────────────────────────────────────────────────────────────

@Composable
private fun BreathingContent(vm: MicrotaskViewModel) {
    val timeRemaining by vm.timeRemainingSeconds.collectAsState()
    val phase         by vm.currentPhase.collectAsState()
    val cycleCount    by vm.cycleCount.collectAsState()
    val totalSeconds  = 120

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("←", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) }
            Text(
                text      = "Micro-task · 1 of 1",
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(36.dp))
        }

        Text(
            text  = "🌬️ Box Breathing",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.1.sp,
                color         = Sky,
            ),
        )
        Spacer(Modifier.height(11.dp))
        Text(
            text      = "Breathe with the circle",
            style     = MaterialTheme.typography.titleLarge,
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text      = "Follow the expanding ring. 4 counts in, hold, out, hold.",
            style     = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))

        BreathingRings(phase = phase, cycleCount = cycleCount)
        Spacer(Modifier.height(24.dp))

        PhaseDots(phase = phase)
        Spacer(Modifier.height(24.dp))

        TimerCard(seconds = timeRemaining, totalSeconds = totalSeconds)
        Spacer(Modifier.height(16.dp))

        Text(
            text      = "\"Breathing is the bridge between body and mind.\" — Thich Nhat Hanh",
            style     = MaterialTheme.typography.bodySmall.copy(
                fontStyle  = FontStyle.Italic,
                lineHeight = 20.sp,
            ),
            color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BreathingRings(phase: BreathPhase, cycleCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ring1",
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(8000, delayMillis = 500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ring2",
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(8000, delayMillis = 1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ring3",
    )

    Box(
        modifier = Modifier.size(190.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = scale1; scaleY = scale1 }
                .clip(CircleShape)
                .background(Sky.copy(alpha = 0.05f)),
        )
        // Middle ring
        Box(
            modifier = Modifier
                .size(154.dp)
                .graphicsLayer { scaleX = scale2; scaleY = scale2 }
                .clip(CircleShape)
                .background(Sky.copy(alpha = 0.08f))
                .border(1.5.dp, Sky.copy(alpha = 0.18f), CircleShape),
        )
        // Inner ring with counter
        Box(
            modifier = Modifier
                .size(114.dp)
                .graphicsLayer { scaleX = scale3; scaleY = scale3 }
                .clip(CircleShape)
                .background(Sky.copy(alpha = 0.14f))
                .border(2.dp, Sky.copy(alpha = 0.32f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            PhaseCounter(count = cycleCount + 1, phase = phase)
        }
    }
}

@Composable
private fun PhaseCounter(count: Int, phase: BreathPhase) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = "$count",
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp),
            color = Sky,
        )
        Text(
            text  = phase.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PhaseDots(phase: BreathPhase) {
    val phases = BreathPhase.values()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        phases.forEach { p ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (p == phase) Sky else MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

@Composable
fun TimerCard(seconds: Int, totalSeconds: Int) {
    val minutes = seconds / 60
    val secs    = seconds % 60
    val progress = if (totalSeconds > 0) seconds.toFloat() / totalSeconds else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text  = "Time remaining",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text  = "$minutes:${"%02d".format(secs)}",
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(11.dp))
        LinearProgressIndicator(
            progress  = { progress },
            modifier  = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(2.5.dp)),
            color      = Sky,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

// ── BODY SCAN ─────────────────────────────────────────────────────────────────

@Composable
private fun BodyScanContent(vm: MicrotaskViewModel) {
    val timeRemaining by vm.timeRemainingSeconds.collectAsState()
    val totalSeconds  = 300

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🧘", fontSize = 56.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
            text      = "Body Scan Meditation",
            style     = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Close your eyes. Slowly bring awareness from head to toe, noticing each sensation without judgement.",
            style     = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        TimerCard(seconds = timeRemaining, totalSeconds = totalSeconds)
    }
}

// ── GRATITUDE ─────────────────────────────────────────────────────────────────

@Composable
private fun GratitudeContent(vm: MicrotaskViewModel) {
    val feedback    by vm.gratitudeFeedback.collectAsState()
    val submitting  by vm.gratitudeSubmitting.collectAsState()
    var text        by remember { mutableStateOf("") }
    val scope       = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🙏", fontSize = 56.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
            text      = "Write a Gratitude Note",
            style     = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "List three things you're thankful for today. Be specific and heartfelt.",
            style     = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value         = text,
            onValueChange = { text = it },
            modifier      = Modifier
                .fillMaxWidth()
                .height(160.dp),
            label         = { Text("I'm grateful for...") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            isError       = feedback.isNotBlank(),
        )

        if (feedback.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text  = feedback,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { scope.launch { vm.submitGratitude(text) } },
            enabled = !submitting,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = Sage),
            shape    = RoundedCornerShape(50.dp),
        ) {
            if (submitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Submit", fontWeight = FontWeight.Bold, color = Color(0xFF0D1F14))
            }
        }
    }
}
