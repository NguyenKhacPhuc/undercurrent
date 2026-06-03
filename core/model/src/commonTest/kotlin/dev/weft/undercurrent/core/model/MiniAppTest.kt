package dev.weft.undercurrent.core.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class MiniAppTest : BehaviorSpec({

    val json = Json { ignoreUnknownKeys = true }

    Given("a persisted mini-app row from before HTML mini-apps shipped") {
        val legacy = """{"id":"feature.x","name":"X","emoji":"*","triggerPrompt":"hi","createdAtEpochMs":1}"""
        When("it is decoded") {
            val app = json.decodeFromString<MiniApp>(legacy)
            Then("the new fields default rather than failing to parse") {
                app.htmlDocument shouldBe null
                app.declaredScopes shouldBe emptySet()
                app.approvedScopes shouldBe emptySet()
                app.stateJson shouldBe null
            }
        }
    }

    Given("an HTML mini-app with scopes and state") {
        val app = MiniApp(
            id = "feature.h",
            name = "H",
            emoji = "*",
            triggerPrompt = "",
            createdAtEpochMs = 1L,
            htmlDocument = "<h1>hi</h1>",
            declaredScopes = setOf("system_info"),
            approvedScopes = setOf("system_info"),
            stateJson = """{"n":1}""",
        )
        Then("it round-trips through JSON") {
            json.decodeFromString<MiniApp>(json.encodeToString(MiniApp.serializer(), app)) shouldBe app
        }
    }
})
