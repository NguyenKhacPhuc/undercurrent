package dev.weft.undercurrent.feature.miniapps

import dev.weft.compose.components.MiniAppStateStore
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private class MapStateStore : MiniAppStateStore {
    private val blobs = mutableMapOf<String, String>()
    override suspend fun get(miniAppId: String?): String? = miniAppId?.let { blobs[it] }
    override suspend fun set(miniAppId: String?, stateJson: String) {
        if (miniAppId != null) blobs[miniAppId] = stateJson
    }
}

class MiniAppActionInvokerFactoryTest : BehaviorSpec({

    fun invokerFor(store: MiniAppStateStore) = miniAppActionInvoker(
        offerable = OfferableActions.readMostlyDefaults(),
        stateStore = store,
        httpClient = HttpClient(MockEngine { respond("pong", HttpStatusCode.OK) }),
    )

    Given("a single assembled invoker serving every mini-app") {
        When("store_set then store_get round-trip for a given mini-app") {
            Then("the value persists via the wired state store, keyed by the call's id") {
                runTest {
                    val invoker = invokerFor(MapStateStore())
                    invoker.invoke("app1", "store_set", """{"key":"n","value":7}""") shouldBe "true"
                    invoker.invoke("app1", "store_get", """{"key":"n"}""") shouldBe "7"
                }
            }
        }

        When("two mini-apps use the same invoker") {
            Then("their storage is isolated by id") {
                runTest {
                    val invoker = invokerFor(MapStateStore())
                    invoker.invoke("a", "store_set", """{"key":"v","value":1}""")
                    invoker.invoke("b", "store_set", """{"key":"v","value":2}""")
                    invoker.invoke("a", "store_get", """{"key":"v"}""") shouldBe "1"
                    invoker.invoke("b", "store_get", """{"key":"v"}""") shouldBe "2"
                }
            }
        }

        When("http_fetch is dispatched through it") {
            Then("the wired client answers") {
                runTest {
                    val out = invokerFor(MapStateStore())
                        .invoke("app1", "http_fetch", """{"url":"https://api.example.com/ping"}""")!!
                    Json.parseToJsonElement(out).jsonObject["body"]!!.jsonPrimitive.content shouldBe "pong"
                }
            }
        }

        When("a non-offerable action is dispatched") {
            Then("it resolves to null") {
                runTest {
                    invokerFor(MapStateStore()).invoke("app1", "delete_data", "null") shouldBe null
                }
            }
        }
    }
})
