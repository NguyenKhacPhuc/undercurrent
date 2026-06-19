package dev.weft.undercurrent.feature.chat.agent

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import kotlinx.coroutines.test.runTest

/**
 * The host seam backing a mini-app's `window.weft.sendMessage`. The
 * isolated-turn behavior (no chat-history pollution) lives in the
 * substrate `WeftAgent.ask` and its test; here we only guard the
 * not-ready path, which the bridge surfaces as a rejected Promise.
 */
class MiniAppAssistantTest : BehaviorSpec({

    Given("no assistant ready yet") {
        Then("askAssistant fails so the mini-app's request is rejected, not hung") {
            runTest {
                shouldThrow<IllegalStateException> {
                    askAssistant(agent = null, text = "what's the forecast?")
                }
            }
        }
    }
})
