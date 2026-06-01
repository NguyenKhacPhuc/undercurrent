package dev.weft.undercurrent.data.network.auth

import dev.weft.undercurrent.core.domain.AuthResponse
import dev.weft.undercurrent.core.domain.AuthResult
import dev.weft.undercurrent.core.domain.MeResponse
import dev.weft.undercurrent.core.domain.SessionTokenStore
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

private class FakeStore(initial: String? = null) : SessionTokenStore {
    private var token: String? = initial
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { this.token = null }
}

private fun jsonClient(handler: MockEngine): HttpClient = HttpClient(handler) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
}

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
private const val BASE = "https://be.test"

class HttpAuthClientTest : BehaviorSpec({

    Given("signUp() against a 201 BE response") {
        Then("returns Success with the parsed AuthResponse") {
            runTest {
                var seen: io.ktor.client.request.HttpRequestData? = null
                val engine = MockEngine { request ->
                    seen = request
                    respond(
                        content = """{"account":{"id":"acct.abc","displayName":"Phuc","email":"phuc@example.com","createdAtMs":1},"session":{"token":"tk-1","expiresAtMs":2}}""",
                        status = HttpStatusCode.Created,
                        headers = jsonHeaders,
                    )
                }
                val result = HttpAuthClient(jsonClient(engine), BASE, FakeStore())
                    .signUp("Phuc", "phuc@example.com", "hunter2-correct")

                seen!!.method shouldBe HttpMethod.Post
                seen!!.url.encodedPath shouldBe "/v1/auth/sign-up"
                (seen!!.body as TextContent).text shouldContain "\"email\":\"phuc@example.com\""

                result.shouldBeInstanceOf<AuthResult.Success<AuthResponse>>()
                (result as AuthResult.Success).value.account.id shouldBe "acct.abc"
                result.value.session.token shouldBe "tk-1"
            }
        }
    }

    Given("signUp() against a 400 BE response") {
        Then("returns InvalidRequest with field errors") {
            runTest {
                val engine = MockEngine {
                    respond(
                        content = """{"error":{"code":"invalid_request","message":"x","details":{"email":"bad"}}}""",
                        status = HttpStatusCode.BadRequest,
                        headers = jsonHeaders,
                    )
                }
                val result = HttpAuthClient(jsonClient(engine), BASE, FakeStore())
                    .signUp("Phuc", "no-at", "hunter2-correct")
                result shouldBe AuthResult.InvalidRequest(mapOf("email" to "bad"))
            }
        }
    }

    Given("signUp() against a 409 BE response") {
        Then("returns EmailAlreadyRegistered") {
            runTest {
                val engine = MockEngine {
                    respond(
                        content = """{"error":{"code":"email_already_registered","message":"taken"}}""",
                        status = HttpStatusCode.Conflict,
                        headers = jsonHeaders,
                    )
                }
                val result = HttpAuthClient(jsonClient(engine), BASE, FakeStore())
                    .signUp("Phuc", "phuc@example.com", "hunter2-correct")
                result shouldBe AuthResult.EmailAlreadyRegistered
            }
        }
    }

    Given("signIn() against a 200 BE response") {
        Then("returns Success") {
            runTest {
                val engine = MockEngine { request ->
                    request.url.encodedPath shouldBe "/v1/auth/sign-in"
                    respond(
                        content = """{"account":{"id":"acct.x","displayName":"X","email":"x@y.com","createdAtMs":1},"session":{"token":"t","expiresAtMs":2}}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }
                val result = HttpAuthClient(jsonClient(engine), BASE, FakeStore())
                    .signIn("x@y.com", "hunter2-correct")
                result.shouldBeInstanceOf<AuthResult.Success<AuthResponse>>()
            }
        }
    }

    Given("signIn() against a 401 BE response") {
        Then("returns Unauthenticated") {
            runTest {
                val engine = MockEngine {
                    respond(
                        content = """{"error":{"code":"unauthenticated","message":"Invalid email or password"}}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = jsonHeaders,
                    )
                }
                val result = HttpAuthClient(jsonClient(engine), BASE, FakeStore())
                    .signIn("x@y.com", "wrong")
                result shouldBe AuthResult.Unauthenticated
            }
        }
    }

    Given("signIn() against a 429 BE response") {
        Then("returns RateLimited") {
            runTest {
                val engine = MockEngine {
                    respond(
                        content = """{"error":{"code":"rate_limited","message":"too many"}}""",
                        status = HttpStatusCode.TooManyRequests,
                        headers = jsonHeaders,
                    )
                }
                val result = HttpAuthClient(jsonClient(engine), BASE, FakeStore())
                    .signIn("x@y.com", "wrong")
                result shouldBe AuthResult.RateLimited
            }
        }
    }

    Given("getMe() with no stored token") {
        Then("short-circuits to Unauthenticated without making an HTTP call") {
            runTest {
                var calls = 0
                val engine = MockEngine { calls++; respondOk() }
                val result = HttpAuthClient(jsonClient(engine), BASE, FakeStore(initial = null))
                    .getMe()
                result shouldBe AuthResult.Unauthenticated
                calls shouldBe 0
            }
        }
    }

    Given("getMe() with a stored token and 200 response") {
        Then("attaches the bearer and returns Success") {
            runTest {
                val engine = MockEngine { request ->
                    request.headers[HttpHeaders.Authorization] shouldBe "Bearer tk-123"
                    request.url.encodedPath shouldBe "/v1/me"
                    respond(
                        content = """{"account":{"id":"acct.a","displayName":"A","email":"a@b.c","createdAtMs":1}}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }
                val result = HttpAuthClient(jsonClient(engine), BASE, FakeStore(initial = "tk-123"))
                    .getMe()
                result.shouldBeInstanceOf<AuthResult.Success<MeResponse>>()
            }
        }
    }

    Given("getMe() with a stored token and 401 response") {
        Then("returns Unauthenticated") {
            runTest {
                val engine = MockEngine {
                    respond(
                        content = """{"error":{"code":"unauthenticated","message":"x"}}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = jsonHeaders,
                    )
                }
                val result = HttpAuthClient(jsonClient(engine), BASE, FakeStore(initial = "tk-x"))
                    .getMe()
                result shouldBe AuthResult.Unauthenticated
            }
        }
    }

    Given("signOut() with no stored token") {
        Then("returns Success without making an HTTP call") {
            runTest {
                var calls = 0
                val engine = MockEngine { calls++; respondOk() }
                val result = HttpAuthClient(jsonClient(engine), BASE, FakeStore(initial = null))
                    .signOut()
                result shouldBe AuthResult.Success(Unit)
                calls shouldBe 0
            }
        }
    }

    Given("signOut() with a token and 204 response") {
        Then("returns Success with the bearer attached") {
            runTest {
                val engine = MockEngine { request ->
                    request.headers[HttpHeaders.Authorization] shouldBe "Bearer tk-out"
                    request.url.encodedPath shouldBe "/v1/auth/sign-out"
                    respond("", HttpStatusCode.NoContent)
                }
                val result = HttpAuthClient(jsonClient(engine), BASE, FakeStore(initial = "tk-out"))
                    .signOut()
                result shouldBe AuthResult.Success(Unit)
            }
        }
    }

    Given("a network exception during signIn()") {
        Then("returns NetworkError") {
            runTest {
                val engine = MockEngine { throw SocketTimeoutException("test") }
                val result = HttpAuthClient(jsonClient(engine), BASE, FakeStore())
                    .signIn("x@y.com", "hunter2-correct")
                result shouldBe AuthResult.NetworkError
            }
        }
    }
})
