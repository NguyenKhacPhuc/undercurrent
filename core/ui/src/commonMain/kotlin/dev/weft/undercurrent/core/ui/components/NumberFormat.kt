package dev.weft.undercurrent.core.ui.components

import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Tiny commonMain replacements for the `String.format(...)` calls that
 * the component palette used to make against `"%.1f"` / `"%.2f"` /
 * `"%d:%02d"`. `String.format` is JVM-only; these helpers do the
 * minimum the palette needs without pulling a wider formatting lib.
 */

/**
 * Format [value] with exactly [places] decimal places. Rounds half-up
 * via `roundToLong`. Negative values keep their sign on the integer
 * half.
 *
 *   5.567.toFixed(1)  -> "5.6"
 *   5.0.toFixed(2)    -> "5.00"
 *   (-2.345).toFixed(1) -> "-2.3"
 */
internal fun Double.toFixed(places: Int): String {
    require(places >= 0) { "places must be >= 0, got $places" }
    if (places == 0) return roundToLong().toString()
    val factor = 10.0.pow(places)
    val scaled = (this * factor).roundToLong()
    val sign = if (scaled < 0L) "-" else ""
    val abs = scaled.absoluteValue
    val whole = abs / factor.toLong()
    val frac = (abs % factor.toLong()).toString().padStart(places, '0')
    return "$sign$whole.$frac"
}

internal fun Float.toFixed(places: Int): String = toDouble().toFixed(places)

/**
 * Format [minutes]:[seconds] with two-digit seconds — the canonical
 * "mm:ss" duration label used by the audio / video media components.
 */
internal fun formatDuration(minutes: Int, seconds: Int): String =
    "$minutes:${seconds.toString().padStart(2, '0')}"
