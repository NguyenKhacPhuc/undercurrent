package dev.weft.undercurrent.data.network.auth

import dev.weft.undercurrent.core.domain.AuthClient
import dev.weft.undercurrent.core.domain.AuthResponse
import dev.weft.undercurrent.core.domain.AuthResult
import dev.weft.undercurrent.core.domain.MeResponse
import dev.weft.undercurrent.core.domain.SessionTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Ktor-backed [AuthClient]. Talks to the BE at [baseUrl] using the
 * shared [httpClient]. Reads/writes the bearer via [sessionTokenStore]
 * for authed endpoints.
 *
 * Maps every observable BE outcome to an [AuthResult] variant:
 *  - 2xx → [AuthResult.Success]
 *  - 400 `invalid_request` → [AuthResult.InvalidRequest] (with field errors)
 *  - 401 `unauthenticated` → [AuthResult.Unauthenticated]
 *  - 409 `email_already_registered` → [AuthResult.EmailAlreadyRegistered]
 *  - 429 `rate_limited` → [AuthResult.RateLimited]
 *  - any network exception → [AuthResult.NetworkError]
 *  - anything else → [AuthResult.UnknownError]
 */
class HttpAuthClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessionTokenStore: SessionTokenStore,
) : AuthClient {

    override suspend fun signUp(
        displayName: String,
        email: String,
        password: String,
    ): AuthResult<AuthResponse> = withMappedExceptions {
        val response = httpClient.post("$baseUrl/v1/auth/sign-up") {
            contentType(ContentType.Application.Json)
            setBody(SignUpRequest(displayName = displayName, email = email, password = password))
        }
        when (response.status) {
            HttpStatusCode.Created -> AuthResult.Success(response.body<AuthResponse>())
            HttpStatusCode.BadRequest -> response.toInvalidRequest()
            HttpStatusCode.Conflict -> AuthResult.EmailAlreadyRegistered
            else -> response.toUnknownError()
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult<AuthResponse> =
        withMappedExceptions {
            val response = httpClient.post("$baseUrl/v1/auth/sign-in") {
                contentType(ContentType.Application.Json)
                setBody(SignInRequest(email = email, password = password))
            }
            when (response.status) {
                HttpStatusCode.OK -> AuthResult.Success(response.body<AuthResponse>())
                HttpStatusCode.BadRequest -> response.toInvalidRequest()
                HttpStatusCode.Unauthorized -> AuthResult.Unauthenticated
                HttpStatusCode.TooManyRequests -> AuthResult.RateLimited
                else -> response.toUnknownError()
            }
        }

    override suspend fun getMe(): AuthResult<MeResponse> = withMappedExceptions {
        val token = sessionTokenStore.read() ?: return@withMappedExceptions AuthResult.Unauthenticated
        val response = httpClient.get("$baseUrl/v1/me") { bearerAuth(token) }
        when (response.status) {
            HttpStatusCode.OK -> AuthResult.Success(response.body<MeResponse>())
            HttpStatusCode.Unauthorized -> AuthResult.Unauthenticated
            else -> response.toUnknownError()
        }
    }

    override suspend fun signOut(): AuthResult<Unit> = withMappedExceptions {
        val token = sessionTokenStore.read() ?: return@withMappedExceptions AuthResult.Success(Unit)
        val response = httpClient.post("$baseUrl/v1/auth/sign-out") { bearerAuth(token) }
        when (response.status) {
            // BE spec says ALWAYS 204 — but accept any 2xx for robustness.
            in HttpStatusCode.OK..HttpStatusCode.NoContent -> AuthResult.Success(Unit)
            else -> AuthResult.Success(Unit) // sign-out is best-effort per Inception D4
        }
    }

    private suspend inline fun <T> withMappedExceptions(
        block: () -> AuthResult<T>,
    ): AuthResult<T> = try {
        block()
    } catch (e: HttpRequestTimeoutException) {
        AuthResult.NetworkError
    } catch (e: ConnectTimeoutException) {
        AuthResult.NetworkError
    } catch (e: SocketTimeoutException) {
        AuthResult.NetworkError
    } catch (e: UnresolvedAddressException) {
        AuthResult.NetworkError
    } catch (e: SerializationException) {
        AuthResult.UnknownError(httpCode = null, message = "Malformed server response")
    }

    private suspend fun HttpResponse.toInvalidRequest(): AuthResult<Nothing> {
        val envelope = parseErrorEnvelope(bodyAsText())
        val fieldErrors = envelope?.error?.details ?: emptyMap()
        return AuthResult.InvalidRequest(fieldErrors)
    }

    private suspend fun HttpResponse.toUnknownError(): AuthResult<Nothing> {
        val envelope = parseErrorEnvelope(bodyAsText())
        return AuthResult.UnknownError(httpCode = status.value, message = envelope?.error?.message)
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
