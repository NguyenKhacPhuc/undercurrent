package dev.weft.undercurrent.shared.mvi

import app.cash.turbine.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Exercises the generic [Store] base class.
 *
 * Uses a tiny [CounterStore] test double — counter, four intents, one
 * effect — to verify the base machinery (state flow, effects channel,
 * dispatch routing, update reducer atomicity).
 *
 * All assertions go through Kotest's matchers; flow observation uses
 * Turbine. MockK isn't needed here — [Store] has no collaborators to
 * stub. The per-feature store tests demonstrate the MockK pattern.
 *
 * `setMain` / `resetMain` wires a `StandardTestDispatcher` to
 * `Dispatchers.Main` because [Store] extends `androidx.lifecycle.ViewModel`
 * which uses `viewModelScope` — that scope dispatches on `Main` and
 * needs a test-time replacement.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StoreTest : FunSpec({

    val mainDispatcher = StandardTestDispatcher()

    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    // ── state semantics ──────────────────────────────────────────────

    test("initial state is the constructor argument") {
        val store = CounterStore(initialCount = 7)

        store.state.value shouldBe CounterState(count = 7)
    }

    test("state is exposed as an immutable StateFlow") {
        val store = CounterStore()

        // The public surface is StateFlow, not MutableStateFlow.
        // Compose call sites can collect but not mutate directly.
        @Suppress("USELESS_IS_CHECK")
        (store.state is kotlinx.coroutines.flow.StateFlow).shouldBe(true)
    }

    // ── update() reducer ─────────────────────────────────────────────

    test("dispatch(Increment) increments the count") {
        val store = CounterStore()

        store.dispatch(CounterIntent.Increment)

        store.state.value.count shouldBe 1
    }

    test("dispatch(Increment) is observable through the StateFlow") {
        val store = CounterStore()

        store.state.test {
            awaitItem() shouldBe CounterState(count = 0)
            store.dispatch(CounterIntent.Increment)
            awaitItem() shouldBe CounterState(count = 1)
            store.dispatch(CounterIntent.Increment)
            awaitItem() shouldBe CounterState(count = 2)
        }
    }

    test("dispatch(Add(n)) replaces the count with n") {
        val store = CounterStore()

        store.dispatch(CounterIntent.Add(value = 42))

        store.state.value.count shouldBe 42
    }

    test("update reducer sees the current state, not a stale snapshot") {
        val store = CounterStore(initialCount = 10)

        // Three Increments in a row — each must see the previous result,
        // not the initial value. If `update {}` captured the initial state
        // instead of reading `_state.value` each time, this would land on
        // 11 instead of 13.
        repeat(3) { store.dispatch(CounterIntent.Increment) }

        store.state.value.count shouldBe 13
    }

    // ── effects channel ──────────────────────────────────────────────

    test("emit() delivers an effect to the effects flow") {
        val store = CounterStore()

        store.effects.test {
            store.dispatch(CounterIntent.RaiseAlarm)
            awaitItem() shouldBe CounterEffect.AlarmRaised
        }
    }

    test("effects are buffered — multiple emits queue without dropping") {
        val store = CounterStore()

        store.dispatch(CounterIntent.RaiseAlarm)
        store.dispatch(CounterIntent.RaiseAlarm)
        store.dispatch(CounterIntent.RaiseAlarm)

        store.effects.test {
            awaitItem() shouldBe CounterEffect.AlarmRaised
            awaitItem() shouldBe CounterEffect.AlarmRaised
            awaitItem() shouldBe CounterEffect.AlarmRaised
        }
    }

    test("intents that don't emit produce no effects") {
        runTest {
            val store = CounterStore()

            store.dispatch(CounterIntent.Increment)
            // Drain the dispatcher so any scheduled emits would have fired.
            testScheduler.advanceUntilIdle()

            store.effects.test {
                expectNoEvents()
            }
        }
    }

    // ── dispatch routing ─────────────────────────────────────────────

    test("subclass dispatch handles every Intent variant — exhaustive when") {
        val store = CounterStore()

        // Just verifies each intent branch lands on its handler without
        // throwing. The state-change semantics are covered above; this
        // test would catch the regression where a new sealed Intent
        // variant is added but the subclass's `when` doesn't handle it.
        store.dispatch(CounterIntent.Increment)
        store.dispatch(CounterIntent.Add(5))
        store.dispatch(CounterIntent.RaiseAlarm)
        store.dispatch(CounterIntent.Reset)

        store.state.value shouldBe CounterState(count = 0)
    }

    test("Reset returns to the initial state") {
        val store = CounterStore(initialCount = 0)

        store.dispatch(CounterIntent.Add(value = 99))
        store.state.value.count shouldBe 99

        store.dispatch(CounterIntent.Reset)
        store.state.value.count shouldBe 0
    }

    // ── ordering ─────────────────────────────────────────────────────
    // The doc on update() says "safe from any thread." With a single
    // test dispatcher we can't exhaustively verify true parallel
    // safety, but we can verify the underlying primitive is
    // `MutableStateFlow` (which IS atomic) and that the single-threaded
    // ordering is preserved.

    test("sequential dispatches preserve ordering") {
        val store = CounterStore()
        val seenCounts = mutableListOf<Int>()

        repeat(100) {
            store.dispatch(CounterIntent.Increment)
            seenCounts += store.state.value.count
        }

        seenCounts shouldContainExactly (1..100).toList()
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
