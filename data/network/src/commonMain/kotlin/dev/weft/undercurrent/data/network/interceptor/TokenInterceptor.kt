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

fun HttpClient.addTokenInterceptor(
    tokenManager: TokenManager,
    refreshTokenCall: RefreshTokenCall,
    skipAuthForPaths: Set<String> = emptySet(),
) {
    this.plugin(HttpSend).intercept { request ->

        if (request.url.encodedPath in skipAuthForPaths) {
            return@intercept execute(request)
        }

        val wasLocallyValid = ensureFreshToken(tokenManager, refreshTokenCall)

        tokenManager.getAccessToken()?.let {
            request.headers[CustomHeader.ACCESS_TOKEN] = it
        }

        val originalCall = execute(request)

        when (originalCall.response.status.value) {
            HttpStatusCode.Unauthorized.value -> {
                if (wasLocallyValid) {
                    tokenManager.clearSession()
                    originalCall
                } else {
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
        e.printStackTrace()
        return false
    }
}
