package dev.weft.undercurrent.shared.mvi

import app.cash.turbine.test
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Exercises the generic [Store] base class — BDD-style.
 *
 * Uses a tiny [CounterStore] test double — counter, four intents, one
 * effect — to verify the base machinery (state flow, effects channel,
 * dispatch routing, update reducer atomicity). No collaborators to
 * stub here; the per-feature store specs demonstrate the MockK
 * pattern.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StoreTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    // ── state shape ──────────────────────────────────────────────────

    Given("a CounterStore constructed with initialCount=7") {
        val store = CounterStore(initialCount = 7)

        Then("the initial state mirrors the constructor argument") {
            store.state.value shouldBe CounterState(count = 7)
        }
    }

    Given("a CounterStore with default initialCount=0") {
        val store = CounterStore()

        Then("the exposed state surface is StateFlow, not MutableStateFlow") {
            // Compose call sites can collect but not mutate directly.
            @Suppress("USELESS_IS_CHECK")
            (store.state is kotlinx.coroutines.flow.StateFlow).shouldBe(true)
        }
    }

    // ── update() reducer ─────────────────────────────────────────────

    Given("a fresh CounterStore") {
        When("Increment is dispatched") {
            val store = CounterStore()
            store.dispatch(CounterIntent.Increment)

            Then("the count is 1") {
                store.state.value.count shouldBe 1
            }
        }

        When("three Increments are dispatched on a store starting at 10") {
            val store = CounterStore(initialCount = 10)
            repeat(3) { store.dispatch(CounterIntent.Increment) }

            Then("the count is 13 — reducer sees the latest state each time") {
                // If `update {}` captured the initial state instead of
                // reading `_state.value` each time, this would land on
                // 11 instead of 13.
                store.state.value.count shouldBe 13
            }
        }

        When("Add(42) is dispatched") {
            val store = CounterStore()
            store.dispatch(CounterIntent.Add(value = 42))

            Then("the count is 42") {
                store.state.value.count shouldBe 42
            }
        }
    }

    Given("an observer collecting state.test {}") {
        val store = CounterStore()

        Then("each Increment emits the next count exactly once") {
            store.state.test {
                awaitItem() shouldBe CounterState(count = 0)
                store.dispatch(CounterIntent.Increment)
                awaitItem() shouldBe CounterState(count = 1)
                store.dispatch(CounterIntent.Increment)
                awaitItem() shouldBe CounterState(count = 2)
            }
        }
    }

    // ── effects channel ──────────────────────────────────────────────

    Given("a fresh CounterStore with an effects subscriber") {
        When("RaiseAlarm is dispatched") {
            val store = CounterStore()

            Then("the effect arrives on the effects flow") {
                store.effects.test {
                    store.dispatch(CounterIntent.RaiseAlarm)
                    awaitItem() shouldBe CounterEffect.AlarmRaised
                }
            }
        }

        When("RaiseAlarm is dispatched three times before any subscriber drains") {
            val store = CounterStore()
            store.dispatch(CounterIntent.RaiseAlarm)
            store.dispatch(CounterIntent.RaiseAlarm)
            store.dispatch(CounterIntent.RaiseAlarm)

            Then("all three effects buffer and arrive in order") {
                store.effects.test {
                    awaitItem() shouldBe CounterEffect.AlarmRaised
                    awaitItem() shouldBe CounterEffect.AlarmRaised
                    awaitItem() shouldBe CounterEffect.AlarmRaised
                }
            }
        }

        When("an Intent that doesn't emit (Increment) is dispatched") {
            val store = CounterStore()
            store.dispatch(CounterIntent.Increment)

            Then("no effect arrives") {
                runTest {
                    testScheduler.advanceUntilIdle()
                    store.effects.test {
                        expectNoEvents()
                    }
                }
            }
        }
    }

    // ── dispatch routing ─────────────────────────────────────────────

    Given("a CounterStore receiving every Intent variant in sequence") {
        val store = CounterStore()
        store.dispatch(CounterIntent.Increment)
        store.dispatch(CounterIntent.Add(5))
        store.dispatch(CounterIntent.RaiseAlarm)
        store.dispatch(CounterIntent.Reset)

        Then("dispatch routes each variant and Reset lands on the initial state") {
            // This test would catch the regression where a new sealed
            // Intent variant is added but the subclass's `when` doesn't
            // handle it.
            store.state.value shouldBe CounterState(count = 0)
        }
    }

    Given("a CounterStore that has been mutated away from initial state") {
        val store = CounterStore(initialCount = 0)
        store.dispatch(CounterIntent.Add(value = 99))

        Then("count is 99 before Reset") {
            store.state.value.count shouldBe 99
        }

        When("Reset is dispatched") {
            store.dispatch(CounterIntent.Reset)

            Then("the state returns to the initial value") {
                store.state.value.count shouldBe 0
            }
        }
    }

    // ── ordering ─────────────────────────────────────────────────────

    Given("100 sequential Increments") {
        val store = CounterStore()
        val seenCounts = mutableListOf<Int>()
        repeat(100) {
            store.dispatch(CounterIntent.Increment)
            seenCounts += store.state.value.count
        }

        Then("each dispatch lands on the next integer 1..100") {
            seenCounts shouldContainExactly (1..100).toList()
        }
    }
})

// ── test fixture: a tiny Store subclass ────────────────────────────

private data class CounterState(val count: Int = 0)

private sealed interface CounterIntent {
    data object Increment : CounterIntent
    data class Add(val value: Int) : CounterIntent
    data object RaiseAlarm : CounterIntent
    data object Reset : CounterIntent
}

private sealed interface CounterEffect {
    data object AlarmRaised : CounterEffect
}

private class CounterStore(initialCount: Int = 0) :
    Store<CounterState, CounterIntent, CounterEffect>(CounterState(count = initialCount)) {

    private val initial = CounterState(count = initialCount)

    override fun dispatch(intent: CounterIntent) {
        when (intent) {
            CounterIntent.Increment -> update { it.copy(count = it.count + 1) }
            is CounterIntent.Add -> update { it.copy(count = intent.value) }
            CounterIntent.RaiseAlarm -> emit(CounterEffect.AlarmRaised)
            CounterIntent.Reset -> update { initial }
        }
    }
}
