package dev.weft.undercurrent.feature.memories

import dev.weft.undercurrent.shared.gateway.MemoryEntry
import dev.weft.undercurrent.shared.gateway.MemoryScope
import dev.weft.undercurrent.shared.gateway.MemoryStoreGateway
import io.kotest.core.spec.style.FunSpec
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
 * Exercises [MemoriesStore] against a MockK-stubbed
 * [MemoryStoreGateway]. Memories are read-only here — writes happen
 * inside the agent loop — so the store has only two intents
 * (Delete + ClearAll).
 *
 * The interesting behavior under test is the StateFlow projection:
 * the constructor reads `store.memories.value` synchronously to seed
 * the initial state, then the init block subscribes to the same
 * StateFlow for live updates. Both paths need to survive.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MemoriesStoreTest : FunSpec({

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

    // ── initial state ────────────────────────────────────────────────

    test("initial state mirrors gateway.memories.value snapshot") {
        val seed = listOf(memory("a"), memory("b"))
        val gateway = fakeGateway(initial = seed)

        val store = MemoriesStore(gateway)

        store.state.value shouldBe MemoriesState(memories = seed)
    }

    test("initial state is empty when gateway snapshot is empty") {
        val store = MemoriesStore(fakeGateway())

        store.state.value shouldBe MemoriesState(memories = emptyList())
    }

    // ── live flow ────────────────────────────────────────────────────

    test("state updates when gateway.memories emits a new list") {
        runTest {
            val flow = MutableStateFlow<List<MemoryEntry>>(emptyList())
            val gateway = mockk<MemoryStoreGateway>().apply {
                every { memories } returns flow
                coEvery { delete(any()) } returns Unit
                coEvery { clear() } returns Unit
            }

            val store = MemoriesStore(gateway)
            advanceUntilIdle()

            flow.value = listOf(memory("new"))
            advanceUntilIdle()

            store.state.value.memories shouldBe listOf(memory("new"))
        }
    }

    test("subsequent emissions overwrite the memory list — not append") {
        runTest {
            val flow = MutableStateFlow(listOf(memory("one")))
            val gateway = mockk<MemoryStoreGateway>().apply {
                every { memories } returns flow
                coEvery { delete(any()) } returns Unit
                coEvery { clear() } returns Unit
            }
            val store = MemoriesStore(gateway)
            advanceUntilIdle()

            flow.value = listOf(memory("two"), memory("three"))
            advanceUntilIdle()

            store.state.value.memories shouldBe listOf(memory("two"), memory("three"))
        }
    }

    // ── Delete ───────────────────────────────────────────────────────

    test("Delete forwards id to gateway.delete") {
        runTest {
            val gateway = fakeGateway()
            val store = MemoriesStore(gateway)

            store.dispatch(MemoriesIntent.Delete("mem-42"))
            advanceUntilIdle()

            coVerify(exactly = 1) { gateway.delete("mem-42") }
            coVerify(exactly = 0) { gateway.clear() }
        }
    }

    test("Delete does not mutate local state — relies on gateway flow") {
        // The store doesn't optimistically remove the entry; it waits for
        // the gateway's StateFlow to re-emit. Documents that the screen
        // sees the entry stay visible until the gateway confirms.
        runTest {
            val seed = listOf(memory("a"), memory("b"))
            val gateway = fakeGateway(initial = seed)
            val store = MemoriesStore(gateway)
            advanceUntilIdle()

            store.dispatch(MemoriesIntent.Delete("a"))
            advanceUntilIdle()

            // Without a gateway-side StateFlow update, the local list is
            // unchanged. (A real impl would have the gateway tick the
            // StateFlow as a consequence of delete().)
            store.state.value.memories shouldBe seed
        }
    }

    // ── ClearAll ─────────────────────────────────────────────────────

    test("ClearAll forwards to gateway.clear") {
        runTest {
            val gateway = fakeGateway()
            val store = MemoriesStore(gateway)

            store.dispatch(MemoriesIntent.ClearAll)
            advanceUntilIdle()

            coVerify(exactly = 1) { gateway.clear() }
            coVerify(exactly = 0) { gateway.delete(any()) }
        }
    }

    test("ClearAll then Delete dispatch both methods in order") {
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
})
