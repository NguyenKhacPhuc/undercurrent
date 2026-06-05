package dev.weft.undercurrent.feature.miniapps

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

class MiniAppHttpInvokerTest : BehaviorSpec({

    val invoker = miniAppHttpInvoker(
        offerable = OfferableActions.readMostlyDefaults(),
        httpClient = HttpClient(MockEngine { respond("pong", HttpStatusCode.OK) }),
    )

    Given("the singleton http-only invoker") {
        When("http_fetch is dispatched") {
            Then("it routes to the wired client") {
                runTest {
                    val out = invoker.invoke("http_fetch", """{"url":"https://api.example.com/ping"}""")!!
                    Json.parseToJsonElement(out).jsonObject["body"]!!.jsonPrimitive.content shouldBe "pong"
                }
            }
        }
        When("store_get is dispatched (deferred, not wired here)") {
            Then("it resolves to null — no such action on the singleton invoker") {
                runTest {
                    invoker.invoke("store_get", """{"key":"x"}""") shouldBe null
                }
            }
        }
    }
})
