package dev.weft.undercurrent.feature.chat

import dev.weft.undercurrent.core.domain.ChatChunk
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * How the live tool signal folds into the chat's "current action" line
 * (live-activity story 03). The indicator reads this to narrate the
 * in-progress action; it hands back to dead-air hints between actions.
 */
class CurrentActionFoldTest : BehaviorSpec({

    Given("no action in progress") {
        When("a tool starts") {
            Then("the current action becomes that tool, running") {
                nextCurrentAction(null, ChatChunk.ToolStart("open_map")) shouldBe
                    CurrentAction("open_map", failed = false)
            }
        }
    }

    Given("an action in progress") {
        val running = CurrentAction("open_map", failed = false)
        When("the tool completes") {
            Then("the current action clears — back to dead-air") {
                nextCurrentAction(running, ChatChunk.ToolDone("open_map")) shouldBe null
            }
        }
        When("the tool fails") {
            Then("the current action is marked failed so the indicator can describe it") {
                nextCurrentAction(running, ChatChunk.ToolFail("open_map", "boom")) shouldBe
                    CurrentAction("open_map", failed = true)
            }
        }
        When("the turn finishes") {
            Then("the current action clears") {
                nextCurrentAction(running, ChatChunk.Done) shouldBe null
            }
        }
        When("assistant text streams in") {
            Then("the current action is unchanged (text isn't an action)") {
                nextCurrentAction(running, ChatChunk.TextDelta("hi")) shouldBe running
            }
        }
    }
})
