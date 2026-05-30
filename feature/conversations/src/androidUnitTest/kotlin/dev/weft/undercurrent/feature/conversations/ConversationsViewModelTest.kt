package dev.weft.undercurrent.feature.conversations

import dev.weft.undercurrent.core.domain.ConversationStoreRepository
import dev.weft.undercurrent.core.domain.ConversationSummary
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * MockK-only interaction tests for [ConversationsViewModel].
 *
 * State-projection coverage (initial state, live flow updates, query
 * propagation into state) lives in commonTest at
 * `ConversationsViewModelStateTest.kt` and runs on Android + iOS. The
 * Thens here are exclusively about:
 *
 *  - The resubscribe pattern: SetQuery cancels the old search() Flow
 *    and starts a new one, requiring per-query stubbing only MockK
 *    handles cleanly.
 *  - Stale-emission isolation: after SetQuery, emissions on the old
 *    flow MUST NOT leak into state.
 *  - Delete + ClearAll forwarding to the gateway.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsMviViewModelTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun summary(id: String, title: String = id, ts: Long = 0L): ConversationSummary =
        ConversationSummary(id = id, title = title, createdAtMs = ts, lastMessageAtMs = ts)

    fun fakeGateway(
        initialSearchResults: MutableStateFlow<List<ConversationSummary>> =
            MutableStateFlow(emptyList()),
    ): ConversationStoreRepository {
        val gateway = mockk<ConversationStoreRepository>()
        every { gateway.search(any()) } returns initialSearchResults
        coEvery { gateway.deleteConversation(any()) } returns Unit
        coEvery { gateway.clearAll() } returns Unit
        return gateway
    }

    Given("a store with an active subscription") {
        When("init runs against a gateway returning two conversations") {
            Then("gateway.search('') is invoked exactly once") {
                runTest {
                    val results = MutableStateFlow(listOf(summary("a"), summary("b")))
                    val gateway = fakeGateway(results)

                    ConversationsViewModel(gateway)
                    advanceUntilIdle()

                    coVerify(exactly = 1) { gateway.search("") }
                }
            }
        }

        When("SetQuery('dogs') is dispatched and the gateway returns a 'dogs' flow") {
            Then("the store resubscribes to gateway.search('dogs') and shows the new results") {
                runTest {
                    val initialResults = MutableStateFlow(listOf(summary("old")))
                    val gateway = mockk<ConversationStoreRepository>().apply {
                        every { search("") } returns initialResults
                        every { search("dogs") } returns flowOf(listOf(summary("rex")))
                        coEvery { deleteConversation(any()) } returns Unit
                        coEvery { clearAll() } returns Unit
                    }
                    val store = ConversationsViewModel(gateway)
                    advanceUntilIdle()

                    store.dispatch(ConversationsIntent.SetQuery("dogs"))
                    advanceUntilIdle()

                    store.state.value.conversations shouldBe listOf(summary("rex"))
                    coVerify(exactly = 1) { gateway.search("") }
                    coVerify(exactly = 1) { gateway.search("dogs") }
                }
            }
        }

        When("the old query's flow keeps emitting after SetQuery resubscribes") {
            Then("the stale emissions do NOT update state — the old job is cancelled") {
                runTest {
                    val initialFlow = MutableStateFlow(listOf(summary("first")))
                    val newFlow = MutableStateFlow(listOf(summary("new")))
                    val gateway = mockk<ConversationStoreRepository>().apply {
                        every { search("") } returns initialFlow
                        every { search("rebuild") } returns newFlow
                        coEvery { deleteConversation(any()) } returns Unit
                        coEvery { clearAll() } returns Unit
                    }
                    val store = ConversationsViewModel(gateway)
                    advanceUntilIdle()
                    store.state.value.conversations shouldBe listOf(summary("first"))

                    store.dispatch(ConversationsIntent.SetQuery("rebuild"))
                    advanceUntilIdle()
                    store.state.value.conversations shouldBe listOf(summary("new"))

                    initialFlow.value = listOf(summary("first"), summary("stale"))
                    advanceUntilIdle()
                    store.state.value.conversations shouldBe listOf(summary("new"))
                }
            }
        }

        When("SetQuery('') is dispatched after the init subscription") {
            Then("gateway.search('') is invoked again — there is no de-dupe") {
                runTest {
                    val gateway = fakeGateway()
                    val store = ConversationsViewModel(gateway)
                    advanceUntilIdle()

                    store.dispatch(ConversationsIntent.SetQuery(""))
                    advanceUntilIdle()

                    coVerify(exactly = 2) { gateway.search("") }
                }
            }
        }
    }

    Given("a fresh store with a stubbed gateway") {
        When("Delete('conv-42') is dispatched") {
            Then("gateway.deleteConversation('conv-42') is called once") {
                runTest {
                    val gateway = fakeGateway()
                    val store = ConversationsViewModel(gateway)

                    store.dispatch(ConversationsIntent.Delete("conv-42"))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { gateway.deleteConversation("conv-42") }
                }
            }
        }

        When("ClearAll is dispatched") {
            Then("gateway.clearAll is called and deleteConversation is not") {
                runTest {
                    val gateway = fakeGateway()
                    val store = ConversationsViewModel(gateway)

                    store.dispatch(ConversationsIntent.ClearAll)
                    advanceUntilIdle()

                    coVerify(exactly = 1) { gateway.clearAll() }
                    coVerify(exactly = 0) { gateway.deleteConversation(any()) }
                }
            }
        }
    }
})
