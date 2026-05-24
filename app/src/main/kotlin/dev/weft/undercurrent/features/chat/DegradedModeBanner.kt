package dev.weft.undercurrent.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.weft.harness.reliability.CircuitBreaker
import dev.weft.undercurrent.theme.UndercurrentTheme
import kotlinx.coroutines.delay

/**
 * Reusable banner that appears when the substrate's LLM circuit breaker is
 * OPEN (the API has been failing for [CircuitBreaker.failureThreshold]
 * consecutive calls). Shows the remaining cooldown counting down once a
 * second, so the user understands why "Send" appears unresponsive instead
 * of staring at silent failure.
 *
 * When the breaker is CLOSED or HALF_OPEN, the composable emits nothing —
 * just drop it into your chat surface above the message list and it stays
 * out of the way until needed.
 *
 * Hosts that bypass [WeftAgent] (using a custom executor) can still
 * use this — pass any [CircuitBreaker] instance.
 */
@Composable
public fun DegradedModeBanner(
    circuitBreaker: CircuitBreaker,
    modifier: Modifier = Modifier,
) {
    val state by circuitBreaker.state.collectAsState()
    val open = state as? CircuitBreaker.State.Open ?: return

    // 1Hz local ticker. We can't subscribe to "wall-clock changed" so a
    // delay loop is the right fit; cheap because the composable is gone
    // entirely whenever the breaker isn't OPEN.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(open) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(TICK_MS)
        }
    }

    val cooldownMs = circuitBreaker.openDuration.inWholeMilliseconds
    val elapsedMs = (nowMs - open.openedAtEpochMs).coerceAtLeast(0)
    val remainingMs = (cooldownMs - elapsedMs).coerceAtLeast(0)
    val remainingSec = (remainingMs + MS_PER_SEC - 1) / MS_PER_SEC  // round up so "0s" only at zero

    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceMuted)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = "Assistant unreachable",
            style = typography.sansLabel.copy(color = colors.error),
        )
        Spacer(Modifier.padding(1.dp))
        Text(
            text = if (remainingSec > 0L) {
                "The LLM API has been failing — pausing for ${remainingSec}s before retrying."
            } else {
                "Retrying on next send…"
            },
            style = typography.sansSmall.copy(color = colors.inkMuted),
        )
    }
}

private const val TICK_MS: Long = 500L
private const val MS_PER_SEC: Long = 1000L
