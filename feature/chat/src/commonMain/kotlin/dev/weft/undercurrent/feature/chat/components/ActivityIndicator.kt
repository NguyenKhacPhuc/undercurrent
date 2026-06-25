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
    contentKey: Any? = null,
    hints: List<String> = DeadAirHints,
    timings: ActivityIndicatorTimings = ActivityIndicatorTimings(),
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    // Coarse quiet clock — only seconds matter, so a light tick keeps
    // recomposition cheap. Restart it whenever the action changes OR new
    // content arrives ([contentKey]); time since then is how we tell a
    // silent stretch from an actively streaming reply.
    var quietElapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(currentActionText, contentKey) {
        quietElapsedMs = 0L
        val step = 200L
        while (true) {
            delay(step)
            quietElapsedMs += step
        }
    }

    // Stay out of the way while the reply is actively streaming; reappear
    // during a silent stretch (e.g. the long, text-less mini-app HTML
    // generation) even after the assistant has already spoken.
    if (!shouldShowIndicator(quietElapsedMs, currentActionText, timings.showAfterQuietMs)) return

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
        // showAfterQuietMs=0 so it renders in the static preview (the quiet
        // clock doesn't advance there).
        ActivityIndicator(
            baseLabel = "Thinking…",
            timings = ActivityIndicatorTimings(showAfterQuietMs = 0L),
        )
    }
}

@Preview
@Composable
private fun ActivityIndicatorNarratingPreview() {
    UndercurrentTheme {
        ActivityIndicator(baseLabel = "Thinking…", currentActionText = "Looking at the map…")
    }
}
