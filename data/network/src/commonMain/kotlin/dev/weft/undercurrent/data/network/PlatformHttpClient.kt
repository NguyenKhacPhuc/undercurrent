package dev.weft.undercurrent.data.network

import io.ktor.client.engine.HttpClientEngine

/**
 * Factory for the platform-specific Ktor [HttpClientEngine] (OkHttp on
 * Android, Darwin on iOS). The platform DI module binds a singleton of
 * this, so the common [networkModule] doesn't need to know which engine
 * it gets.
 */
fun interface PlatformHttpClientEngineFactory {
    fun create(): HttpClientEngine
}

/**
 * Olson-format current time-zone id (e.g. "Asia/Singapore"). Used by
 * dynamic-headers plugins that send the user's TZ on every request.
 *
 * `kotlinx-datetime` exposes `TimeZone.currentSystemDefault().id` which
 * does the same thing — kept as an expect to mirror the R10 pattern and
 * avoid forcing kotlinx-datetime as a transitive dep on every consumer.
 */
expect fun currentTimeZoneId(): String
