package dev.weft.undercurrent.feature.miniapps

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class OfferableActionsTest : BehaviorSpec({

    Given("the app's offerable-action menu") {
        Then("it is the read-mostly v1 set") {
            OfferableActions.names shouldContainAll setOf(
                "http_fetch", "store_get", "store_set", "share", "clipboard_read", "system_info",
            )
        }
        Then("destructive / sensitive actions are not offerable") {
            OfferableActions.names shouldNotContain "delete_data"
            OfferableActions.names shouldNotContain "send_message"
            OfferableActions.names shouldNotContain "get_location"
            OfferableActions.isOfferable("spend_money") shouldBe false
        }
        Then("a known offerable action reads as offerable") {
            OfferableActions.isOfferable("clipboard_read") shouldBe true
        }
    }

    Given("a mini-app requesting a mix of offerable and non-offerable actions") {
        val requested = setOf("system_info", "share", "delete_data", "send_message")
        When("the request is screened") {
            val result = OfferableActions.screen(requested)
            Then("only offerable actions may proceed to approval") {
                result.offerable shouldBe setOf("system_info", "share")
            }
            Then("non-offerable actions are rejected before the user is asked") {
                result.rejected shouldBe setOf("delete_data", "send_message")
            }
        }
    }

    Given("a mini-app requesting only offerable actions") {
        When("screened") {
            val result = OfferableActions.screen(setOf("http_fetch", "store_set"))
            Then("nothing is rejected") {
                result.rejected shouldBe emptySet()
                result.offerable shouldBe setOf("http_fetch", "store_set")
            }
        }
    }
})
