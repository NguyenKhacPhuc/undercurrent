package dev.weft.undercurrent.feature.memories

import dev.weft.undercurrent.shared.gateway.MemoryEntry
import dev.weft.undercurrent.shared.gateway.MemoryScope
import dev.weft.undercurrent.shared.gateway.MemoryStoreGateway
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
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
 * Exercises [MemoriesStore] in BDD style.
 *
 * Read-only screen — writes happen in the agent loop — so the store
 * exposes only Delete + ClearAll. The interesting behavior is the
 * dual projection: the constructor reads `store.memories.value`
 * synchronously for initial state, and the init block subscribes for
 * live updates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MemoriesStoreTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun memory(
        id: String,
        content: String = "memo-$id",
        scope: MemoryScope = MemoryScope.PERMANENT,
        ts: Long = 0L,
    ): MemoryEntry = MemoryEntry(
        id = id,
        content = content,
        tags = emptyList(),
        scope = scope,
        storedAtEpochMs = ts,
    )

    fun fakeGateway(initial: List<MemoryEntry> = emptyList()): MemoryStoreGateway {
        val gateway = mockk<MemoryStoreGateway>()
        every { gateway.memories } returns MutableStateFlow(initial)
        coEvery { gateway.delete(any()) } returns Unit
        coEvery { gateway.clear() } returns Unit
        return gateway
    }

    Given("a gateway seeded with two memories") {
        val seed = listOf(memory("a"), memory("b"))
        val store = MemoriesStore(fakeGateway(initial = seed))

        Then("the initial state mirrors the seed list") {
            store.state.value shouldBe MemoriesState(memories = seed)
        }
    }

    Given("a gateway with no memories") {
        val store = MemoriesStore(fakeGateway())

        Then("the initial state has an empty list") {
            store.state.value shouldBe MemoriesState(memories = emptyList())
        }
    }

    Given("a store subscribed to a mutable memories flow") {
        val flow = MutableStateFlow<List<MemoryEntry>>(emptyList())
        val gateway = mockk<MemoryStoreGateway>().apply {
            every { memories } returns flow
            coEvery { delete(any()) } returns Unit
            coEvery { clear() } returns Unit
        }
        val store = MemoriesStore(gateway)

        When("the gateway emits a new list") {
            Then("the store reflects the new list") {
                runTest {
                    advanceUntilIdle()
                    flow.value = listOf(memory("new"))
                    advanceUntilIdle()

                    store.state.value.memories shouldBe listOf(memory("new"))
                }
            }
        }

        When("two emissions arrive in sequence") {
            Then("the second emission replaces — does not append to — the first") {
                runTest {
                    advanceUntilIdle()
                    flow.value = listOf(memory("one"))
                    advanceUntilIdle()
                    flow.value = listOf(memory("two"), memory("three"))
                    advanceUntilIdle()

                    store.state.value.memories shouldBe listOf(memory("two"), memory("three"))
                }
            }
        }
    }

    Given("a fresh store with a stubbed gateway") {
        When("Delete is dispatched for id 'mem-42'") {
            Then("the gateway.delete('mem-42') is called once") {
                runTest {
                    val gateway = fakeGateway()
                    val store = MemoriesStore(gateway)

                    store.dispatch(MemoriesIntent.Delete("mem-42"))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { gateway.delete("mem-42") }
                    coVerify(exactly = 0) { gateway.clear() }
                }
            }
        }

        When("ClearAll is dispatched") {
            Then("gateway.clear is called once and delete is not called") {
                runTest {
                    val gateway = fakeGateway()
                    val store = MemoriesStore(gateway)

                    store.dispatch(MemoriesIntent.ClearAll)
                    advanceUntilIdle()

                    coVerify(exactly = 1) { gateway.clear() }
                    coVerify(exactly = 0) { gateway.delete(any()) }
                }
            }
        }

        When("ClearAll then Delete are dispatched in sequence") {
            Then("each method fires exactly once") {
                runTest {
                    val gateway = fakeGateway()
                    val store = MemoriesStore(gateway)

                    store.dispatch(MemoriesIntent.ClearAll)
                    store.dispatch(MemoriesIntent.Delete("solo"))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { gateway.clear() }
                    coVerify(exactly = 1) { gateway.delete("solo") }
                }
            }
        }
    }

    Given("a store seeded with two memories and a Delete dispatched") {
        Then("local state stays unchanged — the store waits for the gateway flow to re-emit") {
            runTest {
                val seed = listOf(memory("a"), memory("b"))
                val gateway = fakeGateway(initial = seed)
                val store = MemoriesStore(gateway)
                advanceUntilIdle()

                store.dispatch(MemoriesIntent.Delete("a"))
                advanceUntilIdle()

                // No optimistic local-state mutation; the store doesn't
                // assume the gateway succeeded.
                store.state.value.memories shouldBe seed
            }
        }
    }
})
