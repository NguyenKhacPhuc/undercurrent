package dev.weft.undercurrent.feature.chat.components

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * When the live indicator is visible during an in-flight turn. The fix:
 * it must reappear during silent stretches (e.g. the long, text-less
 * generation of a mini-app's HTML) even after the assistant has spoken —
 * but stay out of the way while the answer text is actively streaming.
 */
class ShouldShowIndicatorTest : BehaviorSpec({

    val showAfterQuietMs = 400L

    Given("a tool is running (currentAction present)") {
        Then("the indicator shows immediately, regardless of how recently content changed") {
            shouldShowIndicator(
                quietElapsedMs = 0,
                currentActionText = "Building your mini-app…",
                showAfterQuietMs = showAfterQuietMs,
            ) shouldBe true
        }
    }

    Given("no action, and content changed very recently — text is streaming") {
        Then("the indicator stays hidden so it doesn't clutter the streaming reply") {
            shouldShowIndicator(
                quietElapsedMs = 200,
                currentActionText = null,
                showAfterQuietMs = showAfterQuietMs,
            ) shouldBe false
        }
    }

    Given("no action, and it's been quiet past the threshold — a silent stretch") {
        Then("the indicator reappears (this is the create-app gap being fixed)") {
            shouldShowIndicator(
                quietElapsedMs = 400,
                currentActionText = null,
                showAfterQuietMs = showAfterQuietMs,
            ) shouldBe true
        }
    }
})
