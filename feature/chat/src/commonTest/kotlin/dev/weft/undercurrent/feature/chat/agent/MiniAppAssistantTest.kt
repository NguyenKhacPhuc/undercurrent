package dev.weft.undercurrent.feature.chat.agent

import dev.weft.harness.behavior.Turn
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * The reply-extraction backing a mini-app's `window.weft.sendMessage`:
 * after a one-shot turn, the mini-app gets the agent's most recent
 * assistant text. Locks the selection rule shared by Android + iOS.
 */
class MiniAppAssistantTest : BehaviorSpec({

    Given("a history ending in an assistant turn") {
        Then("the most recent assistant text is returned") {
            lastAssistantText(
                listOf(
                    Turn.User("what's the weather?"),
                    Turn.Assistant("an earlier reply"),
                    Turn.User("and tomorrow?"),
                    Turn.Assistant("the latest reply"),
                ),
            ) shouldBe "the latest reply"
        }
    }

    Given("a history with no assistant turn yet") {
        Then("null is returned (the caller rejects the mini-app's request)") {
            lastAssistantText(listOf(Turn.User("hi"), Turn.System("be helpful"))) shouldBe null
        }
    }

    Given("an empty history") {
        Then("null is returned") {
            lastAssistantText(emptyList()) shouldBe null
        }
    }
})
