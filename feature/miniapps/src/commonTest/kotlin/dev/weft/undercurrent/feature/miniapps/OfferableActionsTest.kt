package dev.weft.undercurrent.feature.miniapps

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class OfferableActionsTest : BehaviorSpec({

    val registry = OfferableActions.readMostlyDefaults()

    Given("the read-mostly v1 registry") {
        Then("its offerable set is the v1 actions") {
            registry.offerableNames shouldContainAll setOf(
                "http_fetch", "store_get", "store_set", "share", "clipboard_read", "system_info",
            )
        }
        Then("a known offerable action reads as offerable") {
            registry.isOfferable("clipboard_read") shouldBe true
        }
        Then("an unregistered action is not offerable") {
            registry.isOfferable("delete_data") shouldBe false
        }
    }

    Given("the app registering all of its actions, some not offerable") {
        val full = OfferableActions.readMostlyDefaults() + listOf(
            AppAction("set_theme_palette", offerable = false),
            AppAction("open_personas", offerable = false),
            AppAction("delete_data", offerable = false),
        )
        Then("app-only actions are available but not offerable to mini-apps") {
            full.available.map { it.name } shouldContain "set_theme_palette"
            full.isOfferable("set_theme_palette") shouldBe false
        }
        Then("the offerable subset is unchanged by registering app-only actions") {
            full.offerableNames shouldNotContain "delete_data"
            full.isOfferable("http_fetch") shouldBe true
        }
        When("a mini-app requests an app-only action") {
            val result = full.screen(setOf("system_info", "delete_data"))
            Then("the app-only action is rejected before approval") {
                result.offerable shouldBe setOf("system_info")
                result.rejected shouldBe setOf("delete_data")
            }
        }
    }

    Given("the app registering an additional offerable action") {
        val extended = OfferableActions.readMostlyDefaults() + listOf(
            AppAction("read_calendar", "Read upcoming events"),
        )
        Then("the new action becomes offerable") {
            extended.isOfferable("read_calendar") shouldBe true
            extended.screen(setOf("read_calendar")).offerable shouldBe setOf("read_calendar")
        }
    }

    Given("a mini-app requesting a mix of offerable and unknown actions") {
        When("the request is screened") {
            val result = registry.screen(setOf("system_info", "share", "delete_data", "send_message"))
            Then("only offerable actions may proceed and the rest are rejected") {
                result.offerable shouldBe setOf("system_info", "share")
                result.rejected shouldBe setOf("delete_data", "send_message")
            }
        }
    }
})
