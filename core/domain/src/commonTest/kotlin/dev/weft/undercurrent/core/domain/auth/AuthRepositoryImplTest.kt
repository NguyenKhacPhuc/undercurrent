@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.weft.undercurrent.core.domain.auth

import app.cash.turbine.test
import dev.weft.undercurrent.core.domain.SessionTokenStore
import dev.weft.undercurrent.core.domain.auth.dto.AuthResponse
import dev.weft.undercurrent.core.domain.auth.dto.MeResponse
import dev.weft.undercurrent.core.ext.Result
import dev.weft.undercurrent.data.network.common.ApiException
import dev.weft.undercurrent.data.network.common.NetworkException
import dev.weft.undercurrent.data.network.common.defaultHttpClient
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

private class FakeStore(initial: String? = null) : SessionTokenStore {
    private var token: String? = initial
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { this.token = null }
}

private fun client(engine: MockEngine): HttpClient = defaultHttpClient(engine)

private fun repo(engine: MockEngine, store: SessionTokenStore = FakeStore()): AuthRepositoryImpl =
    AuthRepositoryImpl(
        httpClient = client(engine),
        baseUrl = BASE,
        sessionTokenStore = store,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
private const val BASE = "https://be.test"

/**
 * Wraps a raw payload JSON string in the BaseResponse envelope the BE
 * emits on success. Keeps the test bodies readable.
 */
private fun envelope(dataJson: String): String =
    """{"success":true,"data":$dataJson,"message":null,"code":null}"""

class AuthRepositoryImplTest : BehaviorSpec({

    Given("signUp() against a 201 BE response") {
        Then("emits [Loading, Success(AuthResponse)]") {
            runTest {
                lateinit var seenRequest: io.ktor.client.request.HttpRequestData
                val engine = MockEngine { request ->
                    seenRequest = request
                    respond(
                        content = envelope(
                            """{"account":{"id":"acct.abc","displayName":"Phuc","email":"phuc@example.com","createdAtMs":1},"session":{"token":"tk-1","expiresAtMs":2}}""",
                        ),
                        status = HttpStatusCode.Created,
                        headers = jsonHeaders,
                    )
                }
                repo(engine)
                    .signUp("Phuc", "phuc@example.com", "hunter2-correct")
                    .test {
                        awaitItem() shouldBe Result.Loading
                        val ok = awaitItem()
                        ok.shouldBeInstanceOf<Result.Success<AuthResponse>>()
                        ok.data.account.id shouldBe "acct.abc"
                        ok.data.session.token shouldBe "tk-1"
                        awaitComplete()
                    }

                seenRequest.method shouldBe HttpMethod.Post
                seenRequest.url.encodedPath shouldBe "/v1/auth/sign-up"
                (seenRequest.body as TextContent).text shouldContain "\"email\":\"phuc@example.com\""
            }
        }
    }

    Given("signUp() against a 400 BE response with field details") {
        Then("emits [Loading, Error(ApiException(code=invalid_request, details=…))]") {
            runTest {
                val engine = MockEngine {
                    respond(
                        content = """{"code":"invalid_request","message":"x","details":{"email":"bad"}}""",
                        status = HttpStatusCode.BadRequest,
                        headers = jsonHeaders,
                    )
                }
                repo(engine)
                    .signUp("Phuc", "no-at", "hunter2-correct")
                    .test {
                        awaitItem() shouldBe Result.Loading
                        val err = awaitItem()
                        err.shouldBeInstanceOf<Result.Error>()
                        val ex = err.exception.shouldBeInstanceOf<ApiException>()
                        ex.httpStatus shouldBe 400
                        ex.code shouldBe "invalid_request"
                        ex.details!! shouldContainExactly mapOf("email" to "bad")
                        awaitComplete()
                    }
            }
        }
    }

    Given("signUp() against a 409 BE response") {
        Then("emits Error(ApiException(code=email_already_registered, httpStatus=409))") {
            runTest {
                val engine = MockEngine {
                    respond(
                        content = """{"code":"email_already_registered","message":"taken"}""",
                        status = HttpStatusCode.Conflict,
                        headers = jsonHeaders,
                    )
                }
                repo(engine)
                    .signUp("Phuc", "phuc@example.com", "hunter2-correct")
                    .test {
                        awaitItem() shouldBe Result.Loading
                        val err = awaitItem().shouldBeInstanceOf<Result.Error>()
                        val ex = err.exception.shouldBeInstanceOf<ApiException>()
                        ex.httpStatus shouldBe 409
                        ex.code shouldBe "email_already_registered"
                        awaitComplete()
                    }
            }
        }
    }

    Given("signIn() against a 200 BE response") {
        Then("emits [Loading, Success(AuthResponse)]") {
            runTest {
                val engine = MockEngine { request ->
                    request.url.encodedPath shouldBe "/v1/auth/sign-in"
                    respond(
                        content = envelope(
                            """{"account":{"id":"acct.x","displayName":"X","email":"x@y.com","createdAtMs":1},"session":{"token":"t","expiresAtMs":2}}""",
                        ),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }
                repo(engine)
                    .signIn("x@y.com", "hunter2-correct")
                    .test {
                        awaitItem() shouldBe Result.Loading
                        awaitItem().shouldBeInstanceOf<Result.Success<AuthResponse>>()
                        awaitComplete()
                    }
            }
        }
    }

    Given("signIn() against a 401 BE response") {
        Then("emits Error(ApiException(httpStatus=401))") {
            runTest {
                val engine = MockEngine {
                    respond(
                        content = """{"code":"unauthenticated","message":"Invalid email or password"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = jsonHeaders,
                    )
                }
                repo(engine)
                    .signIn("x@y.com", "wrong")
                    .test {
                        awaitItem() shouldBe Result.Loading
                        val ex = awaitItem().shouldBeInstanceOf<Result.Error>()
                            .exception.shouldBeInstanceOf<ApiException>()
                        ex.httpStatus shouldBe 401
                        ex.code shouldBe "unauthenticated"
                        awaitComplete()
                    }
            }
        }
    }

    Given("signIn() against a 429 BE response") {
        Then("emits Error(ApiException(httpStatus=429))") {
            runTest {
                val engine = MockEngine {
                    respond(
                        content = """{"code":"rate_limited","message":"too many"}""",
                        status = HttpStatusCode.TooManyRequests,
                        headers = jsonHeaders,
                    )
                }
                repo(engine)
                    .signIn("x@y.com", "wrong")
                    .test {
                        awaitItem() shouldBe Result.Loading
                        val ex = awaitItem().shouldBeInstanceOf<Result.Error>()
                            .exception.shouldBeInstanceOf<ApiException>()
                        ex.httpStatus shouldBe 429
                        ex.code shouldBe "rate_limited"
                        awaitComplete()
                    }
            }
        }
    }

    Given("getMe() with no stored token") {
        Then("emits Error(ApiException(httpStatus=401)) without making an HTTP call") {
            runTest {
                var calls = 0
                val engine = MockEngine { calls++; respondOk() }
                repo(engine, store = FakeStore(initial = null))
                    .getMe()
                    .test {
                        awaitItem() shouldBe Result.Loading
                        val ex = awaitItem().shouldBeInstanceOf<Result.Error>()
                            .exception.shouldBeInstanceOf<ApiException>()
                        ex.httpStatus shouldBe 401
                        ex.code shouldBe "unauthenticated"
                        awaitComplete()
                    }
                calls shouldBe 0
            }
        }
    }

    Given("getMe() with a stored token and 200 response") {
        Then("attaches the bearer and emits Success") {
            runTest {
                val engine = MockEngine { request ->
                    request.headers[HttpHeaders.Authorization] shouldBe "Bearer tk-123"
                    request.url.encodedPath shouldBe "/v1/me"
                    respond(
                        content = envelope(
                            """{"account":{"id":"acct.a","displayName":"A","email":"a@b.c","createdAtMs":1}}""",
                        ),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }
                repo(engine, store = FakeStore(initial = "tk-123"))
                    .getMe()
                    .test {
                        awaitItem() shouldBe Result.Loading
                        awaitItem().shouldBeInstanceOf<Result.Success<MeResponse>>()
                        awaitComplete()
                    }
            }
        }
    }

    Given("getMe() with a stored token and 401 response") {
        Then("emits Error(ApiException(httpStatus=401))") {
            runTest {
                val engine = MockEngine {
                    respond(
                        content = """{"code":"unauthenticated","message":"x"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = jsonHeaders,
                    )
                }
                repo(engine, store = FakeStore(initial = "tk-x"))
                    .getMe()
                    .test {
                        awaitItem() shouldBe Result.Loading
                        val ex = awaitItem().shouldBeInstanceOf<Result.Error>()
                            .exception.shouldBeInstanceOf<ApiException>()
                        ex.httpStatus shouldBe 401
                        awaitComplete()
                    }
            }
        }
    }

    Given("signOut() with no stored token") {
        Then("emits Success(Unit) without making an HTTP call") {
            runTest {
                var calls = 0
                val engine = MockEngine { calls++; respondOk() }
                repo(engine, store = FakeStore(initial = null))
                    .signOut()
                    .test {
                        awaitItem() shouldBe Result.Loading
                        awaitItem() shouldBe Result.Success(Unit)
                        awaitComplete()
                    }
                calls shouldBe 0
            }
        }
    }

    Given("signOut() with a token and 200 response") {
        Then("attaches the bearer and emits Success(Unit)") {
            runTest {
                val engine = MockEngine { request ->
                    request.headers[HttpHeaders.Authorization] shouldBe "Bearer tk-out"
                    request.url.encodedPath shouldBe "/v1/auth/sign-out"
                    respond(
                        content = """{"success":true,"data":null,"message":null,"code":null}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }
                repo(engine, store = FakeStore(initial = "tk-out"))
                    .signOut()
                    .test {
                        awaitItem() shouldBe Result.Loading
                        awaitItem() shouldBe Result.Success(Unit)
                        awaitComplete()
                    }
            }
        }
    }

    Given("signOut() with a token and a network failure") {
        Then("still emits Success(Unit) — best-effort per Inception D4") {
            runTest {
                val engine = MockEngine { throw SocketTimeoutException("test") }
                repo(engine, store = FakeStore(initial = "tk-out"))
                    .signOut()
                    .test {
                        awaitItem() shouldBe Result.Loading
                        awaitItem() shouldBe Result.Success(Unit)
                        awaitComplete()
                    }
            }
        }
    }

    Given("a network exception during signIn()") {
        Then("emits Error(NetworkException)") {
            runTest {
                val engine = MockEngine { throw SocketTimeoutException("test") }
                repo(engine)
                    .signIn("x@y.com", "hunter2-correct")
                    .test {
                        awaitItem() shouldBe Result.Loading
                        val err = awaitItem().shouldBeInstanceOf<Result.Error>()
                        err.exception.shouldBeInstanceOf<NetworkException>()
                        awaitComplete()
                    }
            }
        }
    }
})
