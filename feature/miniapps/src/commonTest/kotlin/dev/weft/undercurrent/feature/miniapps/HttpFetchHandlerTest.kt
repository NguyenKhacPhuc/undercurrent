package dev.weft.undercurrent.feature.miniapps

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class HttpFetchHandlerTest : BehaviorSpec({

    Given("an http_fetch handler over a stub client") {
        When("a GET to an allowlisted url succeeds") {
            val client = HttpClient(MockEngine { respond("pong", HttpStatusCode.OK) })
            Then("it returns the status and body as JSON") {
                runTest {
                    val out = httpFetchHandler(client).handle(null, """{"url":"https://api.example.com/ping"}""")
                    val obj = Json.parseToJsonElement(out).jsonObject
                    obj["status"]!!.jsonPrimitive.content shouldBe "200"
                    obj["body"]!!.jsonPrimitive.content shouldBe "pong"
                }
            }
        }

        When("a POST with method, headers and body is requested") {
            var seenMethod = ""
            var seenHeader: String? = null
            var seenBody = ""
            val client = HttpClient(MockEngine { req ->
                seenMethod = req.method.value
                seenHeader = req.headers["X-Token"]
                seenBody = req.body.toByteArray().decodeToString()
                respond("ok", HttpStatusCode.Created, headersOf())
            })
            Then("the request forwards them verbatim") {
                runTest {
                    val args = """{"url":"https://api.example.com/x","method":"POST",""" +
                        """"headers":{"X-Token":"abc"},"body":"hello"}"""
                    val out = httpFetchHandler(client).handle(null, args)
                    Json.parseToJsonElement(out).jsonObject["status"]!!.jsonPrimitive.content shouldBe "201"
                    seenMethod shouldBe "POST"
                    seenHeader shouldBe "abc"
                    seenBody shouldBe "hello"
                }
            }
        }

        When("the response is a 404") {
            val client = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) })
            Then("the status surfaces to the mini-app rather than throwing") {
                runTest {
                    val out = httpFetchHandler(client).handle(null, """{"url":"https://api.example.com/missing"}""")
                    Json.parseToJsonElement(out).jsonObject["status"]!!.jsonPrimitive.content shouldBe "404"
                }
            }
        }

        When("the request has no url") {
            val client = HttpClient(MockEngine { respond("x", HttpStatusCode.OK) })
            Then("it fails so the bridge rejects the call") {
                runTest {
                    shouldThrow<IllegalArgumentException> { httpFetchHandler(client).handle(null, """{}""") }
                }
            }
        }

        When("the underlying client throws (e.g. a blocked host)") {
            val client = HttpClient(MockEngine { throw IllegalStateException("Host not on allowlist") })
            Then("the failure propagates to the bridge") {
                runTest {
                    shouldThrow<Throwable> {
                        httpFetchHandler(client).handle(null, """{"url":"https://blocked.example.com"}""")
                    }
                }
            }
        }
    }
})
