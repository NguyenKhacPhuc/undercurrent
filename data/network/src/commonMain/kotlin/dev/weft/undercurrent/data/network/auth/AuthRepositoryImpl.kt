package dev.weft.undercurrent.data.network.auth

import dev.weft.undercurrent.core.domain.AuthException
import dev.weft.undercurrent.core.domain.AuthRepository
import dev.weft.undercurrent.core.domain.AuthResponse
import dev.weft.undercurrent.core.domain.MeResponse
import dev.weft.undercurrent.core.domain.SessionTokenStore
import dev.weft.undercurrent.core.ext.Result
import dev.weft.undercurrent.core.ext.asResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Ktor-backed [AuthRepository]. Talks to the BE at [baseUrl] using the
 * shared [httpClient]. Reads/writes the bearer via [sessionTokenStore]
 * for authed endpoints.
 *
 * On any non-2xx response from the BE, throws [AuthException.Http]
 * (carrying the structured envelope data). On transport/timeout/decode
 * failure, throws [AuthException.Network]. The [asResult] extension
 * wraps each method's `Flow<T>` as `Flow<Result<T>>`, so callers
 * never see the exception — only `Result.Error(AuthException.X)`.
 *
 * The ViewModel layer translates `AuthException` shape → UI state.
 */
class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessionTokenStore: SessionTokenStore,
) : AuthRepository {

    override fun signUp(
        displayName: String,
        email: String,
        password: String,
    ): Flow<Result<AuthResponse>> = flow {
        val response = httpClient.post("$baseUrl/v1/auth/sign-up") {
            contentType(ContentType.Application.Json)
            setBody(SignUpRequest(displayName = displayName, email = email, password = password))
        }
        emit(response.requireSuccess<AuthResponse>())
    }.toAuthFlow()

    override fun signIn(email: String, password: String): Flow<Result<AuthResponse>> = flow {
        val response = httpClient.post("$baseUrl/v1/auth/sign-in") {
            contentType(ContentType.Application.Json)
            setBody(SignInRequest(email = email, password = password))
        }
        emit(response.requireSuccess<AuthResponse>())
    }.toAuthFlow()

    override fun getMe(): Flow<Result<MeResponse>> = flow {
        val token = sessionTokenStore.read() ?: throw AuthException.Http(
            status = HttpStatusCode.Unauthorized.value,
            errorCode = "unauthenticated",
            errorMessage = "No session token stored on this device",
            fieldErrors = null,
        )
        val response = httpClient.get("$baseUrl/v1/me") { bearerAuth(token) }
        emit(response.requireSuccess<MeResponse>())
    }.toAuthFlow()

    override fun signOut(): Flow<Result<Unit>> = flow {
        val token = sessionTokenStore.read()
        if (token != null) {
            // Best-effort per Inception D4. Swallow all failures so the
            // local wipe always completes; the BE session expires naturally
            // within 30 days even if this call dropped.
            try {
                httpClient.post("$baseUrl/v1/auth/sign-out") { bearerAuth(token) }
            } catch (e: Exception) {
                // intentional swallow
            }
        }
        emit(Unit)
    }.toAuthFlow()

    /**
     * Bridge: `flow { … throw AuthException … }.toAuthFlow()` →
     * `Flow<Result<T>>` where any other Exception is rewrapped as
     * [AuthException.Network] before the standard `asResult` mapping.
     */
    private fun <T> Flow<T>.toAuthFlow(): Flow<Result<T>> = flow {
        try {
            collect { emit(it) }
        } catch (e: AuthException) {
            throw e
        } catch (e: Exception) {
            throw AuthException.Network(e)
        }
    }.asResult()

    private suspend inline fun <reified T> HttpResponse.requireSuccess(): T {
        if (status.isSuccess()) return body<T>()
        val envelope = parseErrorEnvelope(bodyAsText())
        throw AuthException.Http(
            status = status.value,
            errorCode = envelope?.error?.code,
            errorMessage = envelope?.error?.message,
            fieldErrors = envelope?.error?.details,
        )
    }

    private fun parseErrorEnvelope(body: String): ErrorEnvelope? = try {
        if (body.isBlank()) null else jsonForErrorParsing.decodeFromString(ErrorEnvelope.serializer(), body)
    } catch (e: SerializationException) {
        null
    }

    private companion object {
        val jsonForErrorParsing = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

@Serializable
private data class SignUpRequest(
    val displayName: String,
    val email: String,
    val password: String,
)

@Serializable
private data class SignInRequest(
    val email: String,
    val password: String,
)

@Serializable
private data class ErrorEnvelope(val error: ErrorBody)

@Serializable
private data class ErrorBody(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)
