package dev.weft.undercurrent.features.voice

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.theme.UndercurrentTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * Rolling waveform of recent audio levels — a row of [barCount] vertical
 * bars whose heights scroll right-to-left as new RMS samples arrive from
 * [rms]. Each bar animates between values for smooth motion.
 *
 * Tuned for [voice.VoiceRecognizer]'s `rmsdB` output: speech typically
 * lands in `-2.0 .. +10.0` dB, so we map that band to `0..1` with a
 * minimum visible height so silent bars don't disappear entirely.
 */
@Composable
internal fun WaveformBars(
    rms: StateFlow<Float>,
    modifier: Modifier = Modifier,
    barCount: Int = 14,
) {
    val colors = UndercurrentTheme.colors

    // Rolling window of the last [barCount] normalized samples.
    // SnapshotStateList so mutations trigger recomposition.
    val samples = remember {
        mutableStateListOf<Float>().apply { repeat(barCount) { add(MIN_BAR_FRACTION) } }
    }

    LaunchedEffect(rms) {
        rms.collect { dB ->
            val normalized = ((dB + RMS_FLOOR_DB) / RMS_BAND_DB).coerceIn(0f, 1f)
            // Shift left, append at the tail. removeAt(0) is O(n) but n=14.
            samples.removeAt(0)
            samples.add(normalized.coerceAtLeast(MIN_BAR_FRACTION))
        }
    }

    Row(
        modifier = modifier.height(BAR_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until barCount) {
            // Animate height between sample changes for a less jittery feel.
            val target = samples.getOrElse(i) { MIN_BAR_FRACTION }
            val animated by animateFloatAsState(
                targetValue = target,
                animationSpec = tween(durationMillis = 90),
                label = "bar-$i",
            )
            Box(
                modifier = Modifier
                    .width(BAR_WIDTH)
                    .fillMaxHeight(animated)
                    .clip(BarShape)
                    .background(colors.accent),
            )
        }
    }
}

private val BAR_HEIGHT = 24.dp
private val BAR_WIDTH = 3.dp
private val BarShape = RoundedCornerShape(2.dp)

// RMS normalization: speech lands roughly in [-2, 10] dB. Shift up by
// [RMS_FLOOR_DB] so silence (~-2 dB) maps to 0; divide by [RMS_BAND_DB]
// so peak speech (~10 dB) maps to ~1.
private const val RMS_FLOOR_DB = 2f
private const val RMS_BAND_DB = 12f

// Even at full silence, show a thin sliver so the bar row stays visible
// and pulses subtly rather than collapsing to a flat line.
private const val MIN_BAR_FRACTION = 0.08f
