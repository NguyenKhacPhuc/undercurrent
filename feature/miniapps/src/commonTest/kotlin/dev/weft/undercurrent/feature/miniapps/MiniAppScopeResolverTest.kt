package dev.weft.undercurrent.feature.miniapps

import dev.weft.undercurrent.core.model.MiniApp
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class MiniAppScopeResolverTest : BehaviorSpec({

    fun miniApp(id: String, approved: Set<String>) = MiniApp(
        id = id,
        name = id,
        emoji = "*",
        triggerPrompt = "",
        createdAtEpochMs = 0L,
        approvedScopes = approved,
    )

    val offerable = OfferableActions.readMostlyDefaults()

    Given("a catalog with one approved mini-app") {
        val catalog = listOf(miniApp("feature.a", setOf("system_info", "share")))
        val resolve = miniAppScopeResolver({ catalog }, offerable)

        Then("its approved-and-offerable scopes are returned") {
            resolve("feature.a") shouldBe setOf("system_info", "share")
        }
    }

    Given("an approved scope that is no longer offerable") {
        val catalog = listOf(miniApp("feature.a", setOf("system_info", "delete_data")))
        val resolve = miniAppScopeResolver({ catalog }, offerable)

        Then("the non-offerable scope is screened out of the resolved set") {
            resolve("feature.a") shouldBe setOf("system_info")
        }
    }

    Given("an unknown or unidentified mini-app") {
        val resolve = miniAppScopeResolver({ emptyList() }, offerable)

        Then("an unknown id resolves to the empty set, not null (gated, deny all)") {
            resolve("feature.missing") shouldBe emptySet()
        }
        Then("a null id resolves to the empty set") {
            resolve(null) shouldBe emptySet()
        }
    }
})
