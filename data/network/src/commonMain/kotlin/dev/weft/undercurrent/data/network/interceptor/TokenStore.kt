package dev.weft.undercurrent.data.network.interceptor

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * The auth-token payload [TokenManager] reads + writes. Fields:
 *
 *   - [accessToken] / [refreshToken] — opaque strings the server issues.
 *   - [durationExpireTimeInSecond] — TTL of the access token as
 *     reported by the server at issue-time. Combined with
 *     [tokenReceiptElapsedTime] (a monotonic-clock snapshot from
 *     [ElapsedRealtime] taken at the moment of receipt) to decide
 *     "should I refresh before sending this request?".
 *
 * Using monotonic time, not wall-clock, defends against the device's
 * clock jumping (timezone change, NTP correction, manual edit).
 */
@Serializable
data class UserToken(
    val accessToken: String,
    val refreshToken: String,
    val durationExpireTimeInSecond: Int,
    val tokenReceiptElapsedTime: Long,
)

/**
 * Persistence boundary for [UserToken]. The consumer supplies the
 * impl — DataStore-Preferences, Keychain, in-memory for tests, etc.
 *
 * [userToken] should emit `null` for "no session" and the latest
 * stored [UserToken] otherwise. Network-module DI is wired to take a
 * `TokenStore` and pass it into [TokenManager]; consumers swap impls
 * without touching network code.
 */
interface TokenStore {
    val userToken: Flow<UserToken?>
    suspend fun setUserToken(token: UserToken)
    suspend fun clearUserToken()
}

/**
 * Pure-suspend function the [TokenInterceptor] calls when the access
 * token has expired. The consumer supplies an impl that POSTs to its
 * specific refresh-token endpoint (`/auth/refresh`, `/oauth/token`,
 * whatever) and returns the new [UserToken] — or `null` to indicate
 * the refresh token is also invalid (forces clearSession).
 *
 * Kept as a function-interface (not baked into [TokenStore]) because
 * refresh is a *network* call against the server, not a persistence
 * concern — separating it lets tests pass a deterministic fake.
 */
fun interface RefreshTokenCall {
    suspend fun refresh(refreshToken: String): UserToken?
}
