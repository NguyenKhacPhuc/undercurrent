@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.weft.undercurrent.core.domain.prompt

import dev.weft.undercurrent.core.domain.SessionTokenStore
import dev.weft.undercurrent.core.model.PromptConfig
import dev.weft.undercurrent.data.network.common.defaultHttpClient
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

private const val BASE = "https://be.test"
private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private class FakeStore(initial: String? = "session-token") : SessionTokenStore {
    private var token: String? = initial
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { this.token = null }
}

private class FakeCache(initial: PromptConfig? = null) : PromptConfigCache {
    private val state = MutableStateFlow(initial)
    override val cached: Flow<PromptConfig?> = state
    override suspend fun save(config: PromptConfig) { state.value = config }
    fun value(): PromptConfig? = state.value
}

private fun repo(
    engine: MockEngine,
    store: SessionTokenStore = FakeStore(),
    cache: PromptConfigCache = FakeCache(),
) = PromptConfigRepositoryImpl(
    httpClient = defaultHttpClient(engine),
    baseUrl = BASE,
    sessionTokenStore = store,
    cache = cache,
    ioDispatcher = UnconfinedTestDispatcher(),
)

private fun envelope(preamble: String, revision: String, updatedAtMs: Long) =
    """{"success":true,"data":{"preamble":"$preamble","revision":"$revision","updatedAtMs":$updatedAtMs},"code":"ok"}"""

class PromptConfigRepositoryImplTest : BehaviorSpec({

    Given("a signed-in client and the backend serving a prompt") {
        Then("refresh fetches it, provides it, and caches it") {
            runTest {
                val cache = FakeCache()
                val engine = MockEngine { respond(envelope("You are Undercurrent", "rev.abc", 1750000000000), HttpStatusCode.OK, jsonHeaders) }
                val r = repo(engine, cache = cache)

                val fetched = r.refresh()
                fetched.shouldNotBeNull()
                fetched.preamble shouldBe "You are Undercurrent"
                fetched.revision shouldBe "rev.abc"
                r.current.first()!!.preamble shouldBe "You are Undercurrent"
                cache.value()!!.revision shouldBe "rev.abc"
            }
        }
    }

    Given("no prompt has ever been fetched") {
        Then("the provided prompt is null — a distinct 'not ready' state, not an empty string") {
            runTest {
                repo(MockEngine { respond(envelope("x", "rev.x", 1), HttpStatusCode.OK, jsonHeaders) })
                    .current.first().shouldBeNull()
            }
        }
    }

    Given("a previously fetched prompt, then a failing fetch (offline / server error)") {
        Then("the failure does not discard the previous prompt") {
            runTest {
                val cache = FakeCache(PromptConfig("old prompt", "rev.old", 1))
                val engine = MockEngine { respondError(HttpStatusCode.ServiceUnavailable) }
                val r = repo(engine, cache = cache)

                r.refresh().shouldBeNull()
                r.current.first()!!.preamble shouldBe "old prompt"
                cache.value()!!.preamble shouldBe "old prompt"
            }
        }
    }

    Given("no session token on the device") {
        Then("refresh fails without touching the cache (can't fetch unauthenticated)") {
            runTest {
                val cache = FakeCache()
                val r = repo(MockEngine { respond(envelope("x", "rev.x", 1), HttpStatusCode.OK, jsonHeaders) }, store = FakeStore(null), cache = cache)

                r.refresh().shouldBeNull()
                cache.value().shouldBeNull()
            }
        }
    }
})
