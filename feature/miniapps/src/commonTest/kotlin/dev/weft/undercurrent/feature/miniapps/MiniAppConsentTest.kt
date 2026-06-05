package dev.weft.undercurrent.feature.miniapps

import dev.weft.undercurrent.core.model.MiniApp
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

private fun htmlApp(
    declared: Set<String> = setOf("http_fetch"),
    consentedAt: Long? = null,
    html: String? = "<h1>Hi</h1>",
) = MiniApp(
    id = "a.1",
    name = "App",
    emoji = "A",
    triggerPrompt = "",
    createdAtEpochMs = 0,
    htmlDocument = html,
    declaredScopes = declared,
    consentedAt = consentedAt,
)

class MiniAppConsentTest : BehaviorSpec({

    val offerable = OfferableActions.readMostlyDefaults()

    Given("a flexible HTML mini-app that declares actions, never decided") {
        Then("it needs consent on first run") {
            htmlApp().needsConsent() shouldBe true
        }
    }

    Given("a mini-app whose grant was already decided") {
        Then("it does not prompt again — even if nothing was approved") {
            htmlApp(consentedAt = 123L).needsConsent() shouldBe false
        }
    }

    Given("a mini-app that declares no actions") {
        Then("there is nothing to consent to") {
            htmlApp(declared = emptySet()).needsConsent() shouldBe false
        }
    }

    Given("a native (non-HTML) mini-app") {
        Then("the HTML consent flow does not apply") {
            htmlApp(html = null).needsConsent() shouldBe false
        }
    }

    Given("a mini-app declaring a mix of offerable and app-only actions") {
        val app = htmlApp(declared = setOf("http_fetch", "delete_data", "store_set"))
        When("the consent prompt's actions are resolved") {
            val actions = app.requestedActions(offerable)
            Then("only the offerable ones appear, with their descriptions") {
                actions.map { it.name } shouldBe listOf("http_fetch", "store_set")
                actions.first().description shouldBe "Fetch from an allowlisted URL"
            }
        }
        When("the grant to record on approval is computed") {
            Then("it is exactly the offerable subset") {
                app.approvableScopes(offerable) shouldBe setOf("http_fetch", "store_set")
            }
        }
    }
})
