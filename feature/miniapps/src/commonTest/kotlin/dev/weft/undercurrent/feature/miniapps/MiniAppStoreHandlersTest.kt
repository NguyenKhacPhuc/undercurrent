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
        val set = storeSetHandler(store, miniAppId = "app1")

        When("a key is written") {
            Then("it returns true and persists the value into the mini-app's blob") {
                runTest {
                    set.handle("""{"key":"count","value":3}""") shouldBe "true"
                    store.blobs["app1"] shouldBe """{"count":3}"""
                }
            }
        }
    }

    Given("a store with an existing blob") {
        val store = InMemoryStateStore().apply { blobs["app1"] = """{"count":3,"name":"a"}""" }
        val get = storeGetHandler(store, miniAppId = "app1")
        val set = storeSetHandler(store, miniAppId = "app1")

        When("an existing key is read") {
            Then("its value comes back as JSON") {
                runTest { get.handle("""{"key":"count"}""") shouldBe "3" }
            }
        }
        When("a missing key is read") {
            Then("it resolves to null") {
                runTest { get.handle("""{"key":"absent"}""") shouldBe "null" }
            }
        }
        When("an existing key is overwritten") {
            Then("only that key changes and the rest is preserved") {
                runTest {
                    set.handle("""{"key":"count","value":9}""") shouldBe "true"
                    get.handle("""{"key":"count"}""") shouldBe "9"
                    get.handle("""{"key":"name"}""") shouldBe "\"a\""
                }
            }
        }
    }

    Given("a store_get for a mini-app that never saved state") {
        val get = storeGetHandler(InMemoryStateStore(), miniAppId = "fresh")
        When("any key is read") {
            Then("it resolves to null rather than failing") {
                runTest { get.handle("""{"key":"anything"}""") shouldBe "null" }
            }
        }
    }
})
