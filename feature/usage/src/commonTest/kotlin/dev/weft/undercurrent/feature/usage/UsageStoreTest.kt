package dev.weft.undercurrent.feature.usage

import dev.weft.undercurrent.shared.gateway.UsageGateway
import dev.weft.undercurrent.shared.gateway.UsageTotals
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Exercises [UsageStore]. KMP — runs on Android + iOS.
 *
 * Read-only screen (no intents) so every assertion is about state
 * projection from `gateway.totals`. Uses a hand-rolled
 * [FakeUsageGateway] instead of MockK so the spec can live in
 * commonTest and run on every target.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UsageStoreTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    Given("a gateway seeded with a populated UsageTotals snapshot") {
        val seed = UsageTotals(
            lifetimeUsd = 12.34,
            lifetimeInputTokens = 1_000,
            lifetimeOutputTokens = 500,
            lastCallModelId = "claude-sonnet-4-6",
        )
        val store = UsageStore(FakeUsageGateway(initial = seed))

        Then("the initial state mirrors the snapshot exactly") {
            store.state.value.totals shouldBe seed
        }
    }

    Given("a gateway with no usage recorded yet") {
        val store = UsageStore(FakeUsageGateway())

        Then("the initial state defaults to zero-valued totals") {
            store.state.value.totals shouldBe UsageTotals()
        }
    }

    Given("a store subscribed to a mutable totals flow") {
        val gateway = FakeUsageGateway()
        val store = UsageStore(gateway)

        When("the gateway emits a new totals snapshot") {
            Then("the store's state reflects the new snapshot") {
                runTest {
                    advanceUntilIdle()
                    gateway.emit(UsageTotals(lifetimeUsd = 0.50, lifetimeInputTokens = 100))
                    advanceUntilIdle()

                    store.state.value.totals shouldBe UsageTotals(
                        lifetimeUsd = 0.50,
                        lifetimeInputTokens = 100,
                    )
                }
            }
        }

        When("the gateway emits a totals snapshot with a different field set") {
            Then("the new snapshot replaces — does not merge with — the old one") {
                runTest {
                    advanceUntilIdle()
                    gateway.emit(UsageTotals(lifetimeUsd = 1.00))
                    advanceUntilIdle()
                    gateway.emit(UsageTotals(lifetimeInputTokens = 99))
                    advanceUntilIdle()

                    store.state.value.totals shouldBe UsageTotals(lifetimeInputTokens = 99)
                }
            }
        }

        When("the gateway emits totals with byDay + byAgent breakdowns") {
            Then("the breakdown maps propagate verbatim into state") {
                runTest {
                    advanceUntilIdle()
                    val byDay = mapOf("2026-05-29" to 1.50, "2026-05-30" to 2.25)
                    val byAgent = mapOf("default" to 3.50, "researcher" to 0.25)
                    gateway.emit(UsageTotals(byDay = byDay, byAgent = byAgent))
                    advanceUntilIdle()

                    store.state.value.totals.byDay shouldBe byDay
                    store.state.value.totals.byAgent shouldBe byAgent
                }
            }
        }
    }

    Given("a UsageStore with no intent variants defined yet") {
        val store = UsageStore(FakeUsageGateway())

        Then("constructing the store doesn't throw and produces a default state") {
            // sealed UsageIntent has no implementations; documents the
            // structural contract — adding a new variant later would
            // surface in the exhaustive `when` inside dispatch() and
            // (likely) require a new Given block here.
            store.state.value shouldBe UsageState()
        }
    }
})

/**
 * KMP-portable [UsageGateway] double. The MockK-using JVM tests reach
 * for `every { gateway.totals } returns MutableStateFlow(seed)`; here
 * the same shape is a regular class.
 */
private class FakeUsageGateway(
    initial: UsageTotals = UsageTotals(),
) : UsageGateway {
    private val _totals = MutableStateFlow(initial)
    override val totals: StateFlow<UsageTotals> get() = _totals

    /** Push a new value into the totals flow — used by live-emission tests. */
    fun emit(value: UsageTotals) {
        _totals.value = value
    }
}
