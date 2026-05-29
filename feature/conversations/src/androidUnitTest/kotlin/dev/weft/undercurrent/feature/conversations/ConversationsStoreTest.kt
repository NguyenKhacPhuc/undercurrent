package dev.weft.undercurrent.feature.conversations

import dev.weft.undercurrent.shared.gateway.ConversationStoreGateway
import dev.weft.undercurrent.shared.gateway.ConversationSummary
import io.kotest.core.spec.style.FunSpec
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
 * Exercises [ConversationsStore]. The gateway is mocked via MockK; the
 * search() flow property is backed by a `MutableStateFlow` so the test
 * can both seed initial results and emit changes mid-test. `setMain` +
 * `advanceUntilIdle` drain the `viewModelScope.launch` collectors the
 * store kicks off in its init block and in `resubscribe`.
 *
 * Key behavior under test: SetQuery cancels the previous search
 * subscription and starts a new one (the resubscribe pattern), so the
 * old flow's emissions stop propagating into state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsStoreTest : FunSpec({

    val mainDispatcher = StandardTestDispatcher()

    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun summary(id: String, title: String = id, ts: Long = 0L): ConversationSummary =
        ConversationSummary(id = id, title = title, createdAtMs = ts, lastMessageAtMs = ts)

    fun fakeGateway(
        initialSearchResults: MutableStateFlow<List<ConversationSummary>> = MutableStateFlow(emptyList()),
    ): ConversationStoreGateway {
        val gateway = mockk<ConversationStoreGateway>()
        every { gateway.search(any()) } returns initialSearchResults
        coEvery { gateway.deleteConversation(any()) } returns Unit
        coEvery { gateway.clearAll() } returns Unit
        return gateway
    }

    // ── initial state ────────────────────────────────────────────────

    test("initial state has empty query and empty conversations") {
        val store = ConversationsStore(fakeGateway())

        store.state.value shouldBe ConversationsState()
    }

    test("init kicks off a search subscription with the empty query") {
        runTest {
            val results = MutableStateFlow(listOf(summary("a"), summary("b")))
            val gateway = fakeGateway(results)

            val store = ConversationsStore(gateway)
            advanceUntilIdle()

            store.state.value.conversations shouldBe listOf(summary("a"), summary("b"))
            coVerify(exactly = 1) { gateway.search("") }
        }
    }

    // ── SetQuery: state + resubscribe ───────────────────────────────

    test("SetQuery updates the query slot immediately") {
        val store = ConversationsStore(fakeGateway())

        store.dispatch(ConversationsIntent.SetQuery("hello"))

        store.state.value.query shouldBe "hello"
    }

    test("SetQuery resubscribes to gateway.search with the new query") {
        runTest {
            val initialResults = MutableStateFlow(listOf(summary("old")))
            val gateway = mockk<ConversationStoreGateway>()
            every { gateway.search("") } returns initialResults
            every { gateway.search("dogs") } returns flowOf(listOf(summary("rex")))
            coEvery { gateway.deleteConversation(any()) } returns Unit
            coEvery { gateway.clearAll() } returns Unit

            val store = ConversationsStore(gateway)
            advanceUntilIdle()

            store.dispatch(ConversationsIntent.SetQuery("dogs"))
            advanceUntilIdle()

            store.state.value.conversations shouldBe listOf(summary("rex"))
            coVerify(exactly = 1) { gateway.search("") }
            coVerify(exactly = 1) { gateway.search("dogs") }
        }
    }

    test("emissions from the previous query's flow stop after resubscribe") {
        runTest {
            // First flow keeps emitting even after the user types a new
            // query. The store must cancel the old job so the stale flow's
            // emissions don't clobber the new query's results.
            val initialFlow = MutableStateFlow(listOf(summary("first")))
            val newFlow = MutableStateFlow(listOf(summary("new")))
            val gateway = mockk<ConversationStoreGateway>()
            every { gateway.search("") } returns initialFlow
            every { gateway.search("rebuild") } returns newFlow
            coEvery { gateway.deleteConversation(any()) } returns Unit
            coEvery { gateway.clearAll() } returns Unit

            val store = ConversationsStore(gateway)
            advanceUntilIdle()
            store.state.value.conversations shouldBe listOf(summary("first"))

            store.dispatch(ConversationsIntent.SetQuery("rebuild"))
            advanceUntilIdle()
            store.state.value.conversations shouldBe listOf(summary("new"))

            // Stale emit on the old flow MUST NOT update state.
            initialFlow.value = listOf(summary("first"), summary("stale"))
            advanceUntilIdle()
            store.state.value.conversations shouldBe listOf(summary("new"))
        }
    }

    test("SetQuery with the same string still resubscribes — kept simple") {
        // Documents current behavior: there's no de-dupe. If the user types
        // the same query twice, the store reissues search() because doing so
        // is harmless given the gateway returns a fresh Flow each time.
        runTest {
            val gateway = fakeGateway()
            val store = ConversationsStore(gateway)
            advanceUntilIdle()

            store.dispatch(ConversationsIntent.SetQuery(""))
            advanceUntilIdle()

            coVerify(exactly = 2) { gateway.search("") }
        }
    }

    // ── Delete ───────────────────────────────────────────────────────

    test("Delete forwards id to gateway.deleteConversation") {
        runTest {
            val gateway = fakeGateway()
            val store = ConversationsStore(gateway)

            store.dispatch(ConversationsIntent.Delete("conv-42"))
            advanceUntilIdle()

            coVerify(exactly = 1) { gateway.deleteConversation("conv-42") }
        }
    }

    test("Delete does not change the query") {
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

    // ── ClearAll ─────────────────────────────────────────────────────

    test("ClearAll forwards to gateway.clearAll") {
        runTest {
            val gateway = fakeGateway()
            val store = ConversationsStore(gateway)

            store.dispatch(ConversationsIntent.ClearAll)
            advanceUntilIdle()

            coVerify(exactly = 1) { gateway.clearAll() }
            coVerify(exactly = 0) { gateway.deleteConversation(any()) }
        }
    }

    // ── flow emissions update state live ─────────────────────────────

    test("gateway flow emissions update conversations live") {
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
})
