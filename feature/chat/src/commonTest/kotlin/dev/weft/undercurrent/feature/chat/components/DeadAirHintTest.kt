package dev.weft.undercurrent.feature.chat.components

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * The dead-air hint rotation rule (live-activity story 02): during a quiet
 * stretch the indicator holds the base label until a brief threshold, then
 * rotates through hints at a calm cadence. Pure logic so the Q1 timings
 * (threshold + cadence) are easy to tune and verifiable without the UI.
 */
class DeadAirHintTest : BehaviorSpec({

    val t = ActivityIndicatorTimings(quietThresholdMs = 1500, rotationCadenceMs = 3500)

    Given("the quiet stretch is still under the threshold") {
        Then("no hint shows yet — the base label holds") {
            deadAirHintIndex(quietElapsedMs = 0, hintCount = 3, timings = t) shouldBe null
            deadAirHintIndex(quietElapsedMs = 1499, hintCount = 3, timings = t) shouldBe null
        }
    }

    Given("the quiet stretch has crossed the threshold") {
        Then("the first hint appears, then later hints rotate by the cadence") {
            deadAirHintIndex(quietElapsedMs = 1500, hintCount = 3, timings = t) shouldBe 0
            deadAirHintIndex(quietElapsedMs = 1500 + 3499, hintCount = 3, timings = t) shouldBe 0
            deadAirHintIndex(quietElapsedMs = 1500 + 3500, hintCount = 3, timings = t) shouldBe 1
            deadAirHintIndex(quietElapsedMs = 1500 + 7000, hintCount = 3, timings = t) shouldBe 2
        }
        Then("the rotation wraps around the available hints") {
            deadAirHintIndex(quietElapsedMs = 1500 + 10500, hintCount = 3, timings = t) shouldBe 0
        }
    }

    Given("there are no hints to show") {
        Then("nothing rotates — the base label always holds") {
            deadAirHintIndex(quietElapsedMs = 99_999, hintCount = 0, timings = t) shouldBe null
        }
    }
})
