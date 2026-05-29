package dev.weft.undercurrent.feature.memories

import dev.weft.undercurrent.shared.gateway.MemoryEntry
import dev.weft.undercurrent.shared.gateway.MemoryScope
import dev.weft.undercurrent.shared.gateway.MemoryStoreGateway
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
 * KMP-portable state-projection tests for [MemoriesStore]. Runs on
 * Android + iOS.
 *
 * Uses a hand-rolled [FakeMemoryStoreGateway] instead of MockK so the
 * spec can live in commonTest. Tests that need to verify the gateway
 * was *called* (interaction tests for Delete / ClearAll forwarding)
 * live in `MemoriesStoreTest` under `androidUnitTest` — MockK's
 * `coVerify` is JVM-only and there's no clean way to assert "this
 * suspend method was invoked with these args" without it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MemoriesStoreStateTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun memory(id: String): MemoryEntry = MemoryEntry(
        id = id,
        content = "memo-$id",
        tags = emptyList(),
        scope = MemoryScope.PERMANENT,
        storedAtEpochMs = 0L,
    )

    Given("a gateway seeded with two memories") {
        val seed = listOf(memory("a"), memory("b"))
        val store = MemoriesStore(FakeMemoryStoreGateway(initial = seed))

        Then("the initial state mirrors the seed list") {
            store.state.value shouldBe MemoriesState(memories = seed)
        }
    }

    Given("a gateway with no memories") {
        val store = MemoriesStore(FakeMemoryStoreGateway())

        Then("the initial state has an empty list") {
            store.state.value shouldBe MemoriesState(memories = emptyList())
        }
    }

    Given("a store subscribed to a mutable memories flow") {
        val gateway = FakeMemoryStoreGateway()
        val store = MemoriesStore(gateway)

        When("the gateway emits a new list") {
            Then("the store reflects the new list") {
                runTest {
                    advanceUntilIdle()
                    gateway.emit(listOf(memory("new")))
                    advanceUntilIdle()

                    store.state.value.memories shouldBe listOf(memory("new"))
                }
            }
        }

        When("two emissions arrive in sequence") {
            Then("the second emission replaces — does not append to — the first") {
                runTest {
                    advanceUntilIdle()
                    gateway.emit(listOf(memory("one")))
                    advanceUntilIdle()
                    gateway.emit(listOf(memory("two"), memory("three")))
                    advanceUntilIdle()

                    store.state.value.memories shouldBe listOf(memory("two"), memory("three"))
                }
            }
        }
    }

    Given("a seeded store with a Delete dispatched") {
        Then("local state stays unchanged — the store waits for the gateway flow to re-emit") {
            runTest {
                val seed = listOf(memory("a"), memory("b"))
                val gateway = FakeMemoryStoreGateway(initial = seed)
                val store = MemoriesStore(gateway)
                advanceUntilIdle()

                store.dispatch(MemoriesIntent.Delete("a"))
                advanceUntilIdle()

                // No optimistic local-state mutation; the store doesn't
                // assume the gateway succeeded. The interaction side
                // (gateway.delete being CALLED) is verified in
                // MemoriesStoreTest under androidUnitTest.
                store.state.value.memories shouldBe seed
            }
        }
    }
})

/**
 * KMP-portable [MemoryStoreGateway] fake. Suspend methods are no-ops —
 * tests that need to assert *that they were called* live in the
 * androidUnitTest spec with MockK.
 */
private class FakeMemoryStoreGateway(
    initial: List<MemoryEntry> = emptyList(),
) : MemoryStoreGateway {
    private val _memories = MutableStateFlow(initial)
    override val memories: StateFlow<List<MemoryEntry>> get() = _memories

    /** Push a new list into the memories flow — used by live-emission tests. */
    fun emit(value: List<MemoryEntry>) {
        _memories.value = value
    }

    override suspend fun delete(id: String) { /* no-op; see MemoriesStoreTest */ }
    override suspend fun clear() { /* no-op; see MemoriesStoreTest */ }
}
