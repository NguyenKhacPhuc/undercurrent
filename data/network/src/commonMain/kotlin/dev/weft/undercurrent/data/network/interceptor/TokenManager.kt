package dev.weft.undercurrent.data.network.interceptor

import kotlinx.coroutines.flow.firstOrNull

/**
 * Reads + writes the current session [UserToken] via the consumer-
 * supplied [TokenStore]. The interceptor in [TokenInterceptor] uses
 * this to:
 *
 *   - decide whether to refresh before sending a request
 *     ([isAccessTokenExpired])
 *   - attach the latest token to outgoing requests
 *     ([getAccessToken])
 *   - clear the session and signal the host app to restart when the
 *     server says the session is dead ([clearSession])
 *
 * The expiration check uses monotonic time (see [ElapsedRealtime]) —
 * a wall-clock jump (timezone change, NTP correction, reboot) won't
 * prematurely invalidate an active session.
 */
class TokenManager(
    private val tokenStore: TokenStore,
    private val appRestarter: AppRestarter,
) {

    suspend fun getAccessToken(): String? =
        tokenStore.userToken.firstOrNull()?.accessToken

    suspend fun getRefreshToken(): String? =
        tokenStore.userToken.firstOrNull()?.refreshToken

    suspend fun isAccessTokenExpired(): Boolean {
        val token = tokenStore.userToken.firstOrNull() ?: return true

        val receiptElapsed = token.tokenReceiptElapsedTime
        if (receiptElapsed == 0L) return true

        val currentElapsed = ElapsedRealtime.millis()
        val sinceReceipt = currentElapsed - receiptElapsed

        // After reboot the monotonic clock restarts at 0 while the
        // stored receipt is from before reboot — treat as expired.
        if (sinceReceipt <= 0L) return true

        val ttlSec = token.durationExpireTimeInSecond
        if (ttlSec == 0) return true

        return sinceReceipt >= ttlSec * 1_000L
    }

    suspend fun setUserToken(token: UserToken) {
        tokenStore.setUserToken(token)
    }

    /**
     * Clears the persisted token and asks the host to restart back to
     * its login screen. Called from two places:
     *   - the response validator on a 403 (server rejected the session)
     *   - the interceptor when refresh also fails
     */
    suspend fun clearSession() {
        tokenStore.clearUserToken()
        appRestarter.restart()
    }
}
