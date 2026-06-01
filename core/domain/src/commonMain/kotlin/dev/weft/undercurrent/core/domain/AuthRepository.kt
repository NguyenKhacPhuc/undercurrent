package dev.weft.undercurrent.core.domain

import dev.weft.undercurrent.core.domain.auth.dto.AuthResponse
import dev.weft.undercurrent.core.domain.auth.dto.MeResponse
import dev.weft.undercurrent.core.ext.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository surface for the BE auth endpoints. Wraps the 4 endpoints
 * (`/v1/auth/sign-up`, `/v1/auth/sign-in`, `/v1/me`, `/v1/auth/sign-out`)
 * documented in `inception/260601-0040-mobile-auth-wiring/api-contract.md`.
 *
 * Each method returns a `Flow<Result<T>>` from `core/ext` so consumers
 * get a natural Loading / Success / Error tri-state without managing UI
 * state by hand. On failure, the `Result.Error.exception` is one of the
 * project's common HTTP exception types — `ApiException` (BE returned a
 * non-2xx with a parseable `BaseErrorResponse`), `HttpException` (non-2xx
 * with an unparseable body), or `NetworkException` (transport-level
 * failure). The ViewModel pattern-matches on `is ApiException` + `.code`
 * / `.httpStatus` to map to UI states.
 *
 * Authed methods (`getMe`, `signOut`) read the bearer from
 * [SessionTokenStore]:
 *  - `getMe()` emits an `ApiException(code="unauthenticated", httpStatus=401)`
 *    if no token is stored (no HTTP call made — same shape as the BE
 *    returning 401)
 *  - `signOut()` emits `Success(Unit)` if no token is stored (nothing to
 *    revoke); HTTP failures during sign-out are swallowed (best-effort
 *    per Inception D4) and a `Success(Unit)` still emits so callers can
 *    proceed to wipe the local token.
 */
interface AuthRepository {
    fun signUp(displayName: String, email: String, password: String): Flow<Result<AuthResponse>>
    fun signIn(email: String, password: String): Flow<Result<AuthResponse>>
    fun getMe(): Flow<Result<MeResponse>>
    fun signOut(): Flow<Result<Unit>>
}
