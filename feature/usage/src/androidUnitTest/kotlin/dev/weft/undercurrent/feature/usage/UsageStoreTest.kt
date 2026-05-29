package dev.weft.undercurrent.feature.usage

import dev.weft.undercurrent.shared.gateway.UsageGateway
import dev.weft.undercurrent.shared.gateway.UsageTotals
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Exercises [UsageStore]. Read-only screen — no intents — so all
 * assertions are about state projection from the gateway's totals
 * StateFlow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UsageStoreTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun fakeGateway(initial: UsageTotals = UsageTotals()): UsageGateway {
        val gateway = mockk<UsageGateway>()
        every { gateway.totals } returns MutableStateFlow(initial)
        return gateway
    }

    Given("a gateway seeded with a populated UsageTotals snapshot") {
        val seed = UsageTotals(
            lifetimeUsd = 12.34,
            lifetimeInputTokens = 1_000,
            lifetimeOutputTokens = 500,
            lastCallModelId = "claude-sonnet-4-6",
        )
        val store = UsageStore(fakeGateway(initial = seed))

        Then("the initial state mirrors the snapshot exactly") {
            store.state.value.totals shouldBe seed
        }
    }

    Given("a gateway with no usage recorded yet") {
        val store = UsageStore(fakeGateway())

        Then("the initial state defaults to zero-valued totals") {
            store.state.value.totals shouldBe UsageTotals()
        }
    }

    Given("a store subscribed to a mutable totals flow") {
        val flow = MutableStateFlow(UsageTotals())
        val gateway = mockk<UsageGateway>().apply { every { totals } returns flow }
        val store = UsageStore(gateway)

        When("the gateway emits a new totals snapshot") {
            Then("the store's state reflects the new snapshot") {
                runTest {
                    advanceUntilIdle()
                    flow.value = UsageTotals(lifetimeUsd = 0.50, lifetimeInputTokens = 100)
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
                    flow.value = UsageTotals(lifetimeUsd = 1.00)
                    advanceUntilIdle()
                    flow.value = UsageTotals(lifetimeInputTokens = 99)
                    advanceUntilIdle()

                    // lifetimeUsd resets to the new emission's 0.0,
                    // not the prior 1.00.
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
                    flow.value = UsageTotals(byDay = byDay, byAgent = byAgent)
                    advanceUntilIdle()

                    store.state.value.totals.byDay shouldBe byDay
                    store.state.value.totals.byAgent shouldBe byAgent
                }
            }
        }
    }

    Given("a UsageStore with no intent variants defined yet") {
        val store = UsageStore(fakeGateway())

        Then("constructing the store doesn't throw and produces a default state") {
            // sealed UsageIntent has no implementations; this test
            // documents the structural contract — adding a new variant
            // later should surface in the exhaustive `when` inside
            // dispatch() and (likely) require a new Given block here.
            store.state.value shouldBe UsageState()
        }
    }
})
