package dev.weft.undercurrent.core.domain.auth

import dev.weft.undercurrent.core.domain.AuthException
import dev.weft.undercurrent.core.domain.AuthRepository
import dev.weft.undercurrent.core.domain.AuthResponse
import dev.weft.undercurrent.core.domain.MeResponse
import dev.weft.undercurrent.core.domain.SessionTokenStore
import dev.weft.undercurrent.core.domain.auth.dto.SignInRequest
import dev.weft.undercurrent.core.domain.auth.dto.SignUpRequest
import dev.weft.undercurrent.core.ext.Result
import dev.weft.undercurrent.core.ext.asResult
import dev.weft.undercurrent.data.network.common.BaseResponse
import dev.weft.undercurrent.data.network.common.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

/**
 * Ktor-backed [AuthRepository]. Talks to the BE at [baseUrl] using the
 * shared [httpClient] (configured in [authRepositoryModule] with an
 * [io.ktor.client.plugins.HttpResponseValidator] that translates HTTP
 * errors → [AuthException.Http] and transport errors →
 * [AuthException.Network]). Reads/writes the bearer via
 * [sessionTokenStore] for authed endpoints.
 *
 * Each BE endpoint returns the standard
 * [dev.weft.undercurrent.data.network.common.BaseResponse] envelope
 * (`{ success, data, message, code }`); methods deserialize into
 * `BaseResponse<T>` and let [safeApiCall] unwrap `.data` to `T`.
 * `.asResult()` then yields `[Loading, Success(T) | Error(AuthException.X)]`.
 * I/O runs on [ioDispatcher].
 *
 * The ViewModel layer translates `AuthException` shape → UI state.
 */
class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessionTokenStore: SessionTokenStore,
    private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override fun signUp(
        displayName: String,
        email: String,
        password: String,
    ): Flow<Result<AuthResponse>> = safeApiCall {
        httpClient.post("$baseUrl/v1/auth/sign-up") {
            contentType(ContentType.Application.Json)
            setBody(SignUpRequest(displayName = displayName, email = email, password = password))
        }.body<BaseResponse<AuthResponse>>()
    }.asResult().flowOn(ioDispatcher)

    override fun signIn(email: String, password: String): Flow<Result<AuthResponse>> = safeApiCall {
        httpClient.post("$baseUrl/v1/auth/sign-in") {
            contentType(ContentType.Application.Json)
            setBody(SignInRequest(email = email, password = password))
        }.body<BaseResponse<AuthResponse>>()
    }.asResult().flowOn(ioDispatcher)

    override fun getMe(): Flow<Result<MeResponse>> = safeApiCall {
        val token = sessionTokenStore.read() ?: throw AuthException.Http(
            status = HttpStatusCode.Unauthorized.value,
            errorCode = "unauthenticated",
            errorMessage = "No session token stored on this device",
            fieldErrors = null,
        )
        httpClient.get("$baseUrl/v1/me") { bearerAuth(token) }.body<BaseResponse<MeResponse>>()
    }.asResult().flowOn(ioDispatcher)

    override fun signOut(): Flow<Result<Unit>> = safeApiCall {
        val token = sessionTokenStore.read()
        if (token != null) {
            // Best-effort per Inception D4. Swallow all failures so the
            // local wipe always completes; the BE session expires
            // naturally within 30 days even if this call dropped.
            try {
                httpClient.post("$baseUrl/v1/auth/sign-out") { bearerAuth(token) }
            } catch (_: Exception) {
                // intentional swallow
            }
        }
        BaseResponse(success = true, data = Unit, message = null, code = null)
    }.asResult().flowOn(ioDispatcher)
}
