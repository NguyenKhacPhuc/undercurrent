package dev.weft.undercurrent.feature.memories

import dev.weft.undercurrent.shared.gateway.MemoryEntry
import dev.weft.undercurrent.shared.gateway.MemoryScope
import dev.weft.undercurrent.shared.gateway.MemoryStoreGateway
import io.kotest.core.spec.style.BehaviorSpec
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
 * MockK-only interaction tests for [MemoriesStore].
 *
 * State-projection coverage (initial state, live flow updates,
 * no-optimistic-mutation) lives in commonTest at
 * `MemoriesStoreStateTest.kt` and runs on Android + iOS. The Thens
 * here are exclusively about *did the gateway get called with the
 * right args* — that requires MockK's `coVerify`, which is JVM-only.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MemoriesStoreTest : BehaviorSpec({

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

    fun fakeGateway(initial: List<MemoryEntry> = emptyList()): MemoryStoreGateway {
        val gateway = mockk<MemoryStoreGateway>()
        every { gateway.memories } returns MutableStateFlow(initial)
        coEvery { gateway.delete(any()) } returns Unit
        coEvery { gateway.clear() } returns Unit
        return gateway
    }

    Given("a fresh store with a stubbed gateway") {
        When("Delete is dispatched for id 'mem-42'") {
            Then("gateway.delete('mem-42') is called once and clear is not") {
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
            Then("gateway.clear is called once and delete is not") {
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
})
