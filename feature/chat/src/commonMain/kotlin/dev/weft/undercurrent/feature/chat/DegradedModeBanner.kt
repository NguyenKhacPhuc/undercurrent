package dev.weft.undercurrent.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Banner that appears when the agent's circuit breaker is open
 * (the API has been failing repeatedly). Counts down the remaining
 * cooldown so the user understands why "Send" appears unresponsive
 * instead of staring at silent failure.
 *
 * The banner emits nothing when [degradedMode] is null — host passes
 * non-null only while the breaker is Open.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/chat/DegradedModeBanner.kt`. Adjustments:
 *   - Weft's `CircuitBreaker.state` (sealed `Open` / `Closed` /
 *     `HalfOpen`) replaced with a [DegradedMode] mirror — host
 *     observes the underlying breaker and passes the open snapshot.
 *   - `System.currentTimeMillis()` → `kotlin.time.Clock.System.now()`.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun DegradedModeBanner(
    degradedMode: DegradedMode?,
    modifier: Modifier = Modifier,
) {
    val open = degradedMode ?: return

    var nowMs by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(open) {
        while (true) {
            nowMs = Clock.System.now().toEpochMilliseconds()
            delay(TICK_MS)
        }
    }

    val elapsedMs = (nowMs - open.openedAtEpochMs).coerceAtLeast(0)
    val remainingMs = (open.cooldownMs - elapsedMs).coerceAtLeast(0)
    val remainingSec = (remainingMs + MS_PER_SEC - 1) / MS_PER_SEC

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
