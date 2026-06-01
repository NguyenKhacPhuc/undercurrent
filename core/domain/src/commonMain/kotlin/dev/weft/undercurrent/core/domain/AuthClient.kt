package dev.weft.undercurrent.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Client surface for the BE auth endpoints. Wraps the 4 endpoints
 * (`/v1/auth/sign-up`, `/v1/auth/sign-in`, `/v1/me`, `/v1/auth/sign-out`)
 * documented in `inception/260601-0040-mobile-auth-wiring/api-contract.md`.
 *
 * Each method returns a `Flow<Result<T>>` from `core/ext` so consumers
 * get a natural Loading / Success / Error tri-state without managing UI
 * state by hand. On failure, the [Result.Error.exception] is always an
 * [AuthException] — the ViewModel pattern-matches on `.status` /
 * `.errorCode` to map to UI states. The data layer does NOT pre-categorize
 * HTTP responses into semantic buckets — that's ViewModel work.
 *
 * Authed methods (`getMe`, `signOut`) read the bearer from
 * [SessionTokenStore]:
 *  - `getMe()` emits an [AuthException.Http] with status 401 if no token
 *    is stored (no HTTP call made — same shape as the BE returning 401)
 *  - `signOut()` emits `Success(Unit)` if no token is stored (nothing to
 *    revoke); HTTP failures during sign-out are swallowed (best-effort
 *    per Inception D4) and a `Success(Unit)` still emits so callers can
 *    proceed to wipe the local token.
 */
interface AuthClient {
    fun signUp(displayName: String, email: String, password: String): Flow<Result<AuthResponse>>
    fun signIn(email: String, password: String): Flow<Result<AuthResponse>>
    fun getMe(): Flow<Result<MeResponse>>
    fun signOut(): Flow<Result<Unit>>
}

/**
 * Carries the data the ViewModel needs to map a failed auth call to UI.
 * Sealed so the consumer's `when` site is exhaustive.
 *
 * Lives at the data-layer boundary on purpose — the ViewModel translates
 * `Http(401)` → "Invalid email or password", `Http(409)` → "Email already
 * registered", `Network(…)` → "Couldn't reach the server", and so on.
 */
sealed class AuthException(cause: Throwable? = null) : Exception(cause) {
    /**
     * The BE returned a non-2xx response. Fields decoded from the
     * `ErrorEnvelope` shape documented in api-contract.md. The ViewModel
     * branches on [status] (and/or [errorCode]) to render the right copy.
     */
    class Http(
        val status: Int,
        val errorCode: String?,
        val errorMessage: String?,
        val fieldErrors: Map<String, String>?,
    ) : AuthException() {
        override val message: String
            get() = "HTTP $status${errorCode?.let { " ($it)" }.orEmpty()}: ${errorMessage ?: "no body"}"
    }

    /** Timeout, unreachable host, no internet, malformed response, etc. */
    class Network(cause: Throwable) : AuthException(cause)
}

@Serializable
data class AccountDto(
    val id: String,
    val displayName: String,
    val email: String,
    val createdAtMs: Long,
)

@Serializable
data class SessionDto(
    val token: String,
    val expiresAtMs: Long,
)

@Serializable
data class AuthResponse(
    val account: AccountDto,
    val session: SessionDto,
)

@Serializable
data class MeResponse(
    val account: AccountDto,
)
