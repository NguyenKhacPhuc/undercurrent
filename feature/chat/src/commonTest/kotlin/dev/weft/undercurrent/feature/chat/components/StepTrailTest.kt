package dev.weft.undercurrent.feature.chat.components

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/**
 * The compact past-tense step trail attached to a finished reply
 * (live-activity story 04). Pure string assembly so the friendly phrasing,
 * the success/failure distinction, and the no-raw-name guarantee are
 * verifiable without the UI.
 */
class StepTrailTest : BehaviorSpec({

    Given("a successful step") {
        Then("it reads as a checked, friendly past-tense phrase — no raw name") {
            val line = trailLine(listOf(TrailStep("open_map", failed = false)))
            line shouldContain "Looked at the map"
            line shouldContain "✓"
            line shouldNotContain "_"
        }
    }

    Given("a failed step") {
        Then("it is visually distinct (✕) and uses the human failure phrase, no error text") {
            val line = trailLine(listOf(TrailStep("web_search", failed = true)))
            line shouldContain "Couldn't search the web"
            line shouldContain "✕"
            line shouldNotContain "_"
        }
    }

    Given("several steps") {
        Then("they join into one compact line in order") {
            val line = trailLine(
                listOf(
                    TrailStep("open_map", failed = false),
                    TrailStep("create_html_mini_app", failed = false),
                ),
            )
            line shouldContain "Looked at the map"
            line shouldContain "Built your mini-app"
            line shouldContain "·"
        }
    }

    Given("an unrecognized action in the trail") {
        Then("it still reads friendly via the generic fallback — never the raw name") {
            val line = trailLine(listOf(TrailStep("frobnicate_widget", failed = false)))
            line shouldNotContain "frobnicate"
            line shouldNotContain "_"
        }
    }

    Given("no steps") {
        Then("the line is empty — an actionless reply shows no trail") {
            trailLine(emptyList()) shouldBe ""
        }
    }
})
