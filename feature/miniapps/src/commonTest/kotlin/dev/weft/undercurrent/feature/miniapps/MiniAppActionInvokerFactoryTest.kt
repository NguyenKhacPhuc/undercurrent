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
        miniAppId = "app1",
    )

    Given("an assembled invoker for one mini-app") {
        When("store_set then store_get round-trip through it") {
            Then("the value persists via the wired state store") {
                runTest {
                    val invoker = invokerFor(MapStateStore())
                    invoker.invoke("store_set", """{"key":"n","value":7}""") shouldBe "true"
                    invoker.invoke("store_get", """{"key":"n"}""") shouldBe "7"
                }
            }
        }

        When("http_fetch is dispatched through it") {
            Then("the wired client answers") {
                runTest {
                    val out = invokerFor(MapStateStore())
                        .invoke("http_fetch", """{"url":"https://api.example.com/ping"}""")!!
                    Json.parseToJsonElement(out).jsonObject["body"]!!.jsonPrimitive.content shouldBe "pong"
                }
            }
        }

        When("a non-offerable action is dispatched") {
            Then("it resolves to null") {
                runTest {
                    invokerFor(MapStateStore()).invoke("delete_data", "null") shouldBe null
                }
            }
        }
    }
})
