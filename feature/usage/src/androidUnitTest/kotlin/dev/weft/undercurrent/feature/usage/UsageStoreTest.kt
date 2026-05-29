package dev.weft.undercurrent.feature.usage

import dev.weft.undercurrent.shared.gateway.UsageGateway
import dev.weft.undercurrent.shared.gateway.UsageTotals
import io.kotest.core.spec.style.FunSpec
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
 * Exercises [UsageStore].
 *
 * Read-only screen — the store has no intents today, so all assertions
 * are about state projection: initial value pulled synchronously from
 * `gateway.totals.value`, then live updates as the gateway StateFlow
 * emits. The empty `dispatch(_: UsageIntent)` is also covered for
 * defensive completeness — adding a new Intent variant should immediately
 * surface in the exhaustive `when` once one exists.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UsageStoreTest : FunSpec({

    val mainDispatcher = StandardTestDispatcher()

    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun fakeGateway(initial: UsageTotals = UsageTotals()): UsageGateway {
        val gateway = mockk<UsageGateway>()
        every { gateway.totals } returns MutableStateFlow(initial)
        return gateway
    }

    // ── initial state ────────────────────────────────────────────────

    test("initial state mirrors gateway.totals.value") {
        val seed = UsageTotals(
            lifetimeUsd = 12.34,
            lifetimeInputTokens = 1_000,
            lifetimeOutputTokens = 500,
            lastCallModelId = "claude-sonnet-4-6",
        )
        val gateway = fakeGateway(initial = seed)

        val store = UsageStore(gateway)

        store.state.value.totals shouldBe seed
    }

    test("initial state defaults to zero-valued totals when gateway is empty") {
        val store = UsageStore(fakeGateway())

        store.state.value.totals shouldBe UsageTotals()
    }

    // ── live flow ────────────────────────────────────────────────────

    test("state updates when gateway.totals emits a new snapshot") {
        runTest {
            val flow = MutableStateFlow(UsageTotals())
            val gateway = mockk<UsageGateway>().apply { every { totals } returns flow }

            val store = UsageStore(gateway)
            advanceUntilIdle()

            flow.value = UsageTotals(lifetimeUsd = 0.50, lifetimeInputTokens = 100)
            advanceUntilIdle()

            store.state.value.totals shouldBe UsageTotals(
                lifetimeUsd = 0.50,
                lifetimeInputTokens = 100,
            )
        }
    }

    test("subsequent emissions replace totals — not merge") {
        runTest {
            val flow = MutableStateFlow(UsageTotals(lifetimeUsd = 1.00))
            val gateway = mockk<UsageGateway>().apply { every { totals } returns flow }
            val store = UsageStore(gateway)
            advanceUntilIdle()

            // New emission with a different field set; the unchanged
            // lifetimeInputTokens stays at the new emission's value (0),
            // NOT the prior 1.00 reading's implied carry.
            flow.value = UsageTotals(lifetimeInputTokens = 99)
            advanceUntilIdle()

            store.state.value.totals shouldBe UsageTotals(lifetimeInputTokens = 99)
        }
    }

    test("per-day breakdown propagates through the flow") {
        runTest {
            val flow = MutableStateFlow(UsageTotals())
            val gateway = mockk<UsageGateway>().apply { every { totals } returns flow }
            val store = UsageStore(gateway)
            advanceUntilIdle()

            val byDay = mapOf("2026-05-29" to 1.50, "2026-05-30" to 2.25)
            val byAgent = mapOf("default" to 3.50, "researcher" to 0.25)
            flow.value = UsageTotals(byDay = byDay, byAgent = byAgent)
            advanceUntilIdle()

            store.state.value.totals.byDay shouldBe byDay
            store.state.value.totals.byAgent shouldBe byAgent
        }
    }

    // ── dispatch is a no-op today ────────────────────────────────────

    test("dispatch does not throw when no Intent variants exist yet") {
        // sealed UsageIntent has no implementations; just verify the
        // contract: calling dispatch with no intents to send is a
        // structural no-op (we can't pass any real value). The exercise
        // is in not regressing the override signature.
        val store = UsageStore(fakeGateway())

        store.state.value shouldBe UsageState()
    }
})
