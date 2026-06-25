package dev.weft.undercurrent.feature.chat.components

/**
 * Timing knobs for the live activity indicator's dead-air behavior
 * (live-activity Q1). [quietThresholdMs] is how long a quiet stretch must
 * last before hints start (so a quick reply never flashes a hint);
 * [rotationCadenceMs] is how long each hint holds before the next.
 * [showAfterQuietMs] is how long the turn must be quiet (no streaming text,
 * no action) before the indicator reappears — short enough to feel
 * responsive during a silent stretch, long enough not to flicker over an
 * actively streaming reply. Calm by design. Tune against real feel.
 */
data class ActivityIndicatorTimings(
    val quietThresholdMs: Long = 1500L,
    val rotationCadenceMs: Long = 3500L,
    val showAfterQuietMs: Long = 400L,
)

/**
 * Whether the indicator should be visible this frame. It shows immediately
 * while an action is running ([currentActionText] non-null), and otherwise
 * only once the turn has been quiet for [showAfterQuietMs] — so it stays
 * out of the way while the answer text streams, but reappears during a
 * silent stretch (e.g. the long, text-less generation of a mini-app's HTML)
 * even after the assistant has already spoken.
 */
fun shouldShowIndicator(
    quietElapsedMs: Long,
    currentActionText: String?,
    showAfterQuietMs: Long,
): Boolean = currentActionText != null || quietElapsedMs >= showAfterQuietMs

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
