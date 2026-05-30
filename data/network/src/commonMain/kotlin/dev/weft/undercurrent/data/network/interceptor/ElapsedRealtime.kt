package dev.weft.undercurrent.data.network.interceptor

/**
 * Monotonic millisecond clock. Resets on device reboot (unlike
 * wall-clock time which can jump). Used by [TokenManager] to compute
 * "elapsed time since the token was received" against the token's TTL,
 * so a wall-clock change (user crosses timezones, NTP correction)
 * doesn't prematurely invalidate an active session.
 *
 *   - Android: `SystemClock.elapsedRealtime()`
 *   - iOS: `clock_gettime(CLOCK_MONOTONIC, …)`
 */
expect object ElapsedRealtime {
    fun millis(): Long
}
