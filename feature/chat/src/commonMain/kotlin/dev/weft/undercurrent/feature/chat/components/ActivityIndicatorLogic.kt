package dev.weft.undercurrent.feature.chat.components

/**
 * Timing knobs for the live activity indicator's dead-air behavior
 * (live-activity Q1). [quietThresholdMs] is how long a quiet stretch must
 * last before hints start (so a quick reply never flashes a hint);
 * [rotationCadenceMs] is how long each hint holds before the next. Calm by
 * design — fast enough to feel alive, slow enough not to distract. Tune
 * against real feel before release.
 */
data class ActivityIndicatorTimings(
    val quietThresholdMs: Long = 1500L,
    val rotationCadenceMs: Long = 3500L,
)

/**
 * Which dead-air hint to show after [quietElapsedMs] of quiet, or `null`
 * to hold the base label (before the threshold, or when there are no
 * hints). Past the threshold the hints rotate by [ActivityIndicatorTimings.rotationCadenceMs],
 * wrapping around [hintCount].
 */
fun deadAirHintIndex(
    quietElapsedMs: Long,
    hintCount: Int,
    timings: ActivityIndicatorTimings,
): Int? {
    if (hintCount <= 0) return null
    if (quietElapsedMs < timings.quietThresholdMs) return null
    val sinceThreshold = quietElapsedMs - timings.quietThresholdMs
    return ((sinceThreshold / timings.rotationCadenceMs) % hintCount).toInt()
}
