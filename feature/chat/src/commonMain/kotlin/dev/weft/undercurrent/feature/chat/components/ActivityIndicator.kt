package dev.weft.undercurrent.feature.chat.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Starter set of dead-air hint lines (live-activity Q2 placeholder — the
 * mob does a tone pass before release). Shown only after a quiet stretch
 * crosses the threshold, so a quick reply never flashes one.
 */
val DeadAirHints: List<String> = listOf(
    "Still thinking…",
    "Working on it…",
    "Gathering my thoughts…",
    "Almost there…",
)

/**
 * The live waiting indicator that replaces the static "Thinking…" label.
 * It gently pulses (so it always reads as alive) and, once a quiet stretch
 * passes [ActivityIndicatorTimings.quietThresholdMs], cross-fades through
 * [hints] at a calm cadence. Appears only where the old static label did —
 * inside the visible chat's in-flight state — so off-the-record turns never
 * surface it.
 */
@Composable
fun ActivityIndicator(
    baseLabel: String,
    currentActionText: String? = null,
    hints: List<String> = DeadAirHints,
    timings: ActivityIndicatorTimings = ActivityIndicatorTimings(),
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    // Coarse quiet clock — only seconds matter (threshold/cadence), so a
    // light tick keeps recomposition cheap while in-flight. Restart it
    // whenever the action changes, and only tick during genuine dead air
    // (no specific action showing) so hints resume cleanly between steps.
    var quietElapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(currentActionText) {
        quietElapsedMs = 0L
        if (currentActionText != null) return@LaunchedEffect
        val step = 200L
        while (true) {
            delay(step)
            quietElapsedMs += step
        }
    }

    val pulse = rememberInfiniteTransition(label = "activity-pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "activity-alpha",
    )

    val text = currentActionText
        ?: deadAirHintIndex(quietElapsedMs, hints.size, timings)?.let { hints[it] }
        ?: baseLabel

    Crossfade(
        targetState = text,
        animationSpec = tween(durationMillis = 280),
        label = "activity-text",
    ) { shown ->
        Text(
            text = shown,
            style = typography.sansSmall.copy(color = colors.inkMuted.copy(alpha = alpha)),
        )
    }
}

@Preview
@Composable
private fun ActivityIndicatorPreview() {
    UndercurrentTheme {
        ActivityIndicator(baseLabel = "Thinking…")
    }
}

@Preview
@Composable
private fun ActivityIndicatorNarratingPreview() {
    UndercurrentTheme {
        ActivityIndicator(baseLabel = "Thinking…", currentActionText = "Looking at the map…")
    }
}
