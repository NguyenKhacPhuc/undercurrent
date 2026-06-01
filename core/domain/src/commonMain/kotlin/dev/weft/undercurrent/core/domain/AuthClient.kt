package dev.weft.undercurrent.core.domain

import kotlinx.serialization.Serializable

/**
 * Client surface for the BE auth endpoints. Wraps the 4 endpoints
 * (`/v1/auth/sign-up`, `/v1/auth/sign-in`, `/v1/me`, `/v1/auth/sign-out`)
 * documented in `inception/260601-0040-mobile-auth-wiring/api-contract.md`.
 *
 * All methods return a typed [AuthResult] — no exceptions cross the
 * boundary. Authed methods (`getMe`, `signOut`) read the bearer from
 * [SessionTokenStore] and short-circuit to [AuthResult.Unauthenticated]
 * when no token is present (no HTTP call made).
 */
interface AuthClient {
    suspend fun signUp(displayName: String, email: String, password: String): AuthResult<AuthResponse>
    suspend fun signIn(email: String, password: String): AuthResult<AuthResponse>
    suspend fun getMe(): AuthResult<MeResponse>
    suspend fun signOut(): AuthResult<Unit>
}

/**
 * Typed outcome for every [AuthClient] call. Exhaustive at the consumer
 * side so the sign-in screen can `when`-branch cleanly to the matching
 * UI state.
 */
sealed class AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>()

    /** BE returned 401 OR the call required a bearer and no local token is stored. */
    data object Unauthenticated : AuthResult<Nothing>()

    /** BE returned 400 — payload was missing fields or failed validation. */
    data class InvalidRequest(val fieldErrors: Map<String, String>) : AuthResult<Nothing>()

    /** Sign-up specifically: BE returned 409. */
    data object EmailAlreadyRegistered : AuthResult<Nothing>()

    /** Sign-in specifically: BE returned 429 — too many failed attempts. */
    data object RateLimited : AuthResult<Nothing>()

    /** Timeout, unreachable host, no internet, etc. — no HTTP response received. */
    data object NetworkError : AuthResult<Nothing>()

    /** Anything else — 5xx, malformed JSON, unknown error code. */
    data class UnknownError(val httpCode: Int?, val message: String?) : AuthResult<Nothing>()
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

/**
 * Validation rules the BE enforces — mirrored client-side so the
 * sign-in screen fails fast with inline errors before any submit.
 * See api-contract.md "Validation rules" section.
 */
object AuthValidation {
    const val MAX_DISPLAY_NAME_LENGTH: Int = 40
    const val MIN_PASSWORD_LENGTH: Int = 8

    fun displayNameError(value: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> "Display name is required"
            trimmed.length > MAX_DISPLAY_NAME_LENGTH ->
                "Display name must be $MAX_DISPLAY_NAME_LENGTH characters or fewer"
            else -> null
        }
    }

    fun emailError(value: String): String? {
        if (value.any { it.isWhitespace() }) return "Email must not contain spaces"
        val at = value.indexOf('@')
        if (at <= 0 || at == value.lastIndex) return "Email is not valid"
        val dot = value.indexOf('.', startIndex = at + 1)
        if (dot == -1 || dot == value.lastIndex) return "Email is not valid"
        return null
    }

    fun passwordError(value: String): String? = when {
        value.length < MIN_PASSWORD_LENGTH ->
            "Password must be at least $MIN_PASSWORD_LENGTH characters"
        else -> null
    }
}
