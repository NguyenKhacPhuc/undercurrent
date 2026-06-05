package dev.weft.undercurrent.feature.miniapps

import dev.weft.compose.components.MiniAppStateStore
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

private class InMemoryStateStore : MiniAppStateStore {
    val blobs = mutableMapOf<String, String>()
    override suspend fun get(miniAppId: String?): String? = miniAppId?.let { blobs[it] }
    override suspend fun set(miniAppId: String?, stateJson: String) {
        if (miniAppId != null) blobs[miniAppId] = stateJson
    }
}

class MiniAppStoreHandlersTest : BehaviorSpec({

    Given("a store_set handler over an empty state blob") {
        val store = InMemoryStateStore()
        val set = storeSetHandler(store)

        When("a key is written for a mini-app") {
            Then("it returns true and persists the value into that mini-app's blob") {
                runTest {
                    set.handle("app1", """{"key":"count","value":3}""") shouldBe "true"
                    store.blobs["app1"] shouldBe """{"count":3}"""
                }
            }
        }
    }

    Given("a store with an existing blob for a mini-app") {
        val store = InMemoryStateStore().apply { blobs["app1"] = """{"count":3,"name":"a"}""" }
        val get = storeGetHandler(store)
        val set = storeSetHandler(store)

        When("an existing key is read") {
            Then("its value comes back as JSON") {
                runTest { get.handle("app1", """{"key":"count"}""") shouldBe "3" }
            }
        }
        When("a missing key is read") {
            Then("it resolves to null") {
                runTest { get.handle("app1", """{"key":"absent"}""") shouldBe "null" }
            }
        }
        When("an existing key is overwritten") {
            Then("only that key changes and the rest is preserved") {
                runTest {
                    set.handle("app1", """{"key":"count","value":9}""") shouldBe "true"
                    get.handle("app1", """{"key":"count"}""") shouldBe "9"
                    get.handle("app1", """{"key":"name"}""") shouldBe "\"a\""
                }
            }
        }
    }

    Given("two mini-apps sharing one store") {
        val store = InMemoryStateStore()
        val get = storeGetHandler(store)
        val set = storeSetHandler(store)

        When("each writes the same key") {
            Then("their values are isolated by mini-app id") {
                runTest {
                    set.handle("a", """{"key":"v","value":1}""")
                    set.handle("b", """{"key":"v","value":2}""")
                    get.handle("a", """{"key":"v"}""") shouldBe "1"
                    get.handle("b", """{"key":"v"}""") shouldBe "2"
                }
            }
        }
    }

    Given("a store_get for a mini-app that never saved state") {
        val get = storeGetHandler(InMemoryStateStore())
        When("any key is read") {
            Then("it resolves to null rather than failing") {
                runTest { get.handle("fresh", """{"key":"anything"}""") shouldBe "null" }
            }
        }
    }
})
