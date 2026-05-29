package dev.weft.undercurrent.feature.conversations

import dev.weft.undercurrent.shared.gateway.ConversationStoreGateway
import dev.weft.undercurrent.shared.gateway.ConversationSummary
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
 * Exercises [ConversationsStore] in BDD style.
 *
 * Key behavior under test: SetQuery cancels the previous search
 * subscription and starts a new one — the resubscribe pattern — so
 * stale emissions from the old query's flow stop propagating into
 * state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsStoreTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun summary(id: String, title: String = id, ts: Long = 0L): ConversationSummary =
        ConversationSummary(id = id, title = title, createdAtMs = ts, lastMessageAtMs = ts)

    fun fakeGateway(
        initialSearchResults: MutableStateFlow<List<ConversationSummary>> =
            MutableStateFlow(emptyList()),
    ): ConversationStoreGateway {
        val gateway = mockk<ConversationStoreGateway>()
        every { gateway.search(any()) } returns initialSearchResults
        coEvery { gateway.deleteConversation(any()) } returns Unit
        coEvery { gateway.clearAll() } returns Unit
        return gateway
    }

    Given("a fresh ConversationsStore") {
        val store = ConversationsStore(fakeGateway())

        Then("the initial state has empty query and empty conversations") {
            store.state.value shouldBe ConversationsState()
        }
    }

    Given("a gateway whose initial search returns two conversations") {
        Then("init kicks off a search with the empty query and the store reflects the results") {
            runTest {
                val results = MutableStateFlow(listOf(summary("a"), summary("b")))
                val gateway = fakeGateway(results)

                val store = ConversationsStore(gateway)
                advanceUntilIdle()

                store.state.value.conversations shouldBe listOf(summary("a"), summary("b"))
                coVerify(exactly = 1) { gateway.search("") }
            }
        }
    }

    Given("a store with an active subscription") {
        When("SetQuery('hello') is dispatched") {
            Then("the query slot updates immediately") {
                val store = ConversationsStore(fakeGateway())
                store.dispatch(ConversationsIntent.SetQuery("hello"))

                store.state.value.query shouldBe "hello"
            }
        }

        When("SetQuery('dogs') is dispatched and the gateway returns a 'dogs' flow") {
            Then("the store resubscribes to gateway.search('dogs') and shows the new results") {
                runTest {
                    val initialResults = MutableStateFlow(listOf(summary("old")))
                    val gateway = mockk<ConversationStoreGateway>().apply {
                        every { search("") } returns initialResults
                        every { search("dogs") } returns flowOf(listOf(summary("rex")))
                        coEvery { deleteConversation(any()) } returns Unit
                        coEvery { clearAll() } returns Unit
                    }
                    val store = ConversationsStore(gateway)
                    advanceUntilIdle()

                    store.dispatch(ConversationsIntent.SetQuery("dogs"))
                    advanceUntilIdle()

                    store.state.value.conversations shouldBe listOf(summary("rex"))
                    coVerify(exactly = 1) { gateway.search("") }
                    coVerify(exactly = 1) { gateway.search("dogs") }
                }
            }
        }

        When("the old query's flow continues to emit after SetQuery resubscribes") {
            Then("the stale emissions do NOT update state — the old job is cancelled") {
                runTest {
                    val initialFlow = MutableStateFlow(listOf(summary("first")))
                    val newFlow = MutableStateFlow(listOf(summary("new")))
                    val gateway = mockk<ConversationStoreGateway>().apply {
                        every { search("") } returns initialFlow
                        every { search("rebuild") } returns newFlow
                        coEvery { deleteConversation(any()) } returns Unit
                        coEvery { clearAll() } returns Unit
                    }
                    val store = ConversationsStore(gateway)
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
                    val store = ConversationsStore(gateway)
                    advanceUntilIdle()

                    store.dispatch(ConversationsIntent.SetQuery(""))
                    advanceUntilIdle()

                    coVerify(exactly = 2) { gateway.search("") }
                }
            }
        }
    }

    Given("a store with the user typing into the query slot") {
        When("Delete('conv-42') is dispatched") {
            Then("gateway.deleteConversation('conv-42') is called once") {
                runTest {
                    val gateway = fakeGateway()
                    val store = ConversationsStore(gateway)

                    store.dispatch(ConversationsIntent.Delete("conv-42"))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { gateway.deleteConversation("conv-42") }
                }
            }

            Then("the query value is not touched by the Delete") {
                runTest {
                    val gateway = fakeGateway()
                    val store = ConversationsStore(gateway)
                    store.dispatch(ConversationsIntent.SetQuery("keeper"))
                    advanceUntilIdle()

                    store.dispatch(ConversationsIntent.Delete("conv-42"))
                    advanceUntilIdle()

                    store.state.value.query shouldBe "keeper"
                }
            }
        }

        When("ClearAll is dispatched") {
            Then("gateway.clearAll is called and deleteConversation is not") {
                runTest {
                    val gateway = fakeGateway()
                    val store = ConversationsStore(gateway)

                    store.dispatch(ConversationsIntent.ClearAll)
                    advanceUntilIdle()

                    coVerify(exactly = 1) { gateway.clearAll() }
                    coVerify(exactly = 0) { gateway.deleteConversation(any()) }
                }
            }
        }
    }

    Given("a store with a mutable search flow") {
        Then("emissions update conversations live as they arrive") {
            runTest {
                val flow = MutableStateFlow<List<ConversationSummary>>(emptyList())
                val gateway = fakeGateway(flow)
                val store = ConversationsStore(gateway)
                advanceUntilIdle()

                flow.value = listOf(summary("a"))
                advanceUntilIdle()
                store.state.value.conversations shouldBe listOf(summary("a"))

                flow.value = listOf(summary("a"), summary("b"))
                advanceUntilIdle()
                store.state.value.conversations shouldBe listOf(summary("a"), summary("b"))
            }
        }
    }
})
