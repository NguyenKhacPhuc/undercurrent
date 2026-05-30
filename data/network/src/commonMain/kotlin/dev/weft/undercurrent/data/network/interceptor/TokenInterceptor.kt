package dev.weft.undercurrent.data.network.interceptor

import dev.weft.undercurrent.data.network.common.CustomHeader
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Ensures only one refresh-token call runs at a time. Without this
// every parallel request that races past an expiry would fire its own
// refresh, the server would invalidate all but the last, and the rest
// of the in-flight calls would 401 with a token that was JUST issued.
private val refreshTokenMutex = Mutex()

/**
 * Installs a [HttpSend] interceptor that:
 *
 *   1. Checks if the access token is expired (locally, monotonic-clock-
 *      based) and refreshes it before sending the request. The refresh
 *      itself is the consumer-supplied [refreshTokenCall] — this module
 *      doesn't know which endpoint to hit.
 *   2. After the response arrives, on 401 retries once with the latest
 *      token. If the local clock said the token was still valid but the
 *      server disagreed, we treat that as an emulator-clock-skew
 *      symptom and force a session clear instead of an infinite refresh
 *      loop.
 *   3. On 403, surfaces the response unchanged — the response validator
 *      decides whether to clear the session.
 *
 * The set of endpoints that should NOT trigger the auth-attach + retry
 * dance (refresh itself, public endpoints) is supplied via
 * [skipAuthForPaths] — exact match on `encodedPath`.
 */
fun HttpClient.addTokenInterceptor(
    tokenManager: TokenManager,
    refreshTokenCall: RefreshTokenCall,
    skipAuthForPaths: Set<String> = emptySet(),
) {
    this.plugin(HttpSend).intercept { request ->

        if (request.url.encodedPath in skipAuthForPaths) {
            return@intercept execute(request)
        }

        // Pre-flight: refresh if our local check thinks the token is
        // dead. `wasLocallyValid` captures the pre-call state so we can
        // distinguish "server rejected a token we thought was fresh"
        // from "server rejected a token we knew was stale".
        val wasLocallyValid = ensureFreshToken(tokenManager, refreshTokenCall)

        // Always re-attach the latest token — another coroutine may have
        // refreshed it while we were waiting on the mutex above.
        tokenManager.getAccessToken()?.let {
            request.headers[CustomHeader.ACCESS_TOKEN] = it
        }

        val originalCall = execute(request)

        when (originalCall.response.status.value) {
            HttpStatusCode.Unauthorized.value -> {
                if (wasLocallyValid) {
                    // We sent a token the server rejected even though our
                    // expiry check said it was fine. That usually means
                    // the monotonic clock is wrong (emulator quick-boot).
                    // Force the user to log in again rather than burn
                    // through refresh tokens.
                    tokenManager.clearSession()
                    originalCall
                } else {
                    // Refresh and retry once.
                    if (ensureFreshToken(tokenManager, refreshTokenCall)) {
                        tokenManager.getAccessToken()?.let {
                            request.headers[CustomHeader.ACCESS_TOKEN] = it
                        }
                    }
                    execute(request)
                }
            }

            else -> originalCall
        }
    }
}

/**
 * Returns true if the access token is (or has just been refreshed to
 * be) valid; false if we couldn't refresh because there's no refresh
 * token or the refresh call returned null.
 *
 * Synchronized on [refreshTokenMutex] so parallel callers share one
 * refresh attempt instead of racing.
 */
private suspend fun ensureFreshToken(
    tokenManager: TokenManager,
    refreshTokenCall: RefreshTokenCall,
): Boolean = refreshTokenMutex.withLock {
    if (!tokenManager.isAccessTokenExpired()) return true

    val refreshToken = tokenManager.getRefreshToken() ?: return false

    try {
        val refreshed = refreshTokenCall.refresh(refreshToken)
        if (refreshed == null) {
            tokenManager.clearSession()
            return false
        }
        tokenManager.setUserToken(refreshed)
        return true
    } catch (e: Exception) {
        // Network error during refresh — leave the session alone; the
        // user will see a "no connection" error and can retry. Don't
        // force-logout on a transient failure.
        e.printStackTrace()
        return false
    }
}
