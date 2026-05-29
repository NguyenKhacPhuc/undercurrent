package dev.weft.undercurrent.feature.traces

import dev.weft.undercurrent.shared.gateway.AgentTrace
import dev.weft.undercurrent.shared.gateway.TraceFeedback
import dev.weft.undercurrent.shared.gateway.TraceStatus
import dev.weft.undercurrent.shared.gateway.TraceStoreGateway
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
 * Exercises [TracesStore]. The gateway is mocked via MockK; traces are
 * exposed as a `StateFlow<List<AgentTrace>>` so the test seeds initial
 * data and emits live updates.
 *
 * Intent coverage: every variant of [TracesIntent.SetFeedback] (each
 * [TraceFeedback] enum value) plus [TracesIntent.ClearAll].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TracesStoreTest : FunSpec({

    val mainDispatcher = StandardTestDispatcher()

    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun trace(
        id: String,
        userMessage: String = "msg-$id",
        status: TraceStatus = TraceStatus.COMPLETED,
        feedback: TraceFeedback = TraceFeedback.NONE,
    ): AgentTrace = AgentTrace(
        id = id,
        conversationId = "conv-1",
        startEpochMs = 0L,
        endEpochMs = 100L,
        userMessage = userMessage,
        finalAssistantMessage = "reply",
        status = status,
        feedback = feedback,
    )

    fun fakeGateway(initial: List<AgentTrace> = emptyList()): TraceStoreGateway {
        val gateway = mockk<TraceStoreGateway>()
        every { gateway.traces } returns MutableStateFlow(initial)
        coEvery { gateway.setFeedback(any(), any()) } returns Unit
        coEvery { gateway.clear() } returns Unit
        return gateway
    }

    // ── initial state ────────────────────────────────────────────────

    test("initial state mirrors gateway.traces.value snapshot") {
        val seed = listOf(trace("a"), trace("b"))
        val gateway = fakeGateway(initial = seed)

        val store = TracesStore(gateway)

        store.state.value shouldBe TracesState(traces = seed)
    }

    test("initial state is empty when gateway has no traces") {
        val store = TracesStore(fakeGateway())

        store.state.value shouldBe TracesState(traces = emptyList())
    }

    // ── live flow ────────────────────────────────────────────────────

    test("state updates when gateway.traces emits a new list") {
        runTest {
            val flow = MutableStateFlow<List<AgentTrace>>(emptyList())
            val gateway = mockk<TraceStoreGateway>().apply {
                every { traces } returns flow
                coEvery { setFeedback(any(), any()) } returns Unit
                coEvery { clear() } returns Unit
            }

            val store = TracesStore(gateway)
            advanceUntilIdle()

            flow.value = listOf(trace("new"))
            advanceUntilIdle()

            store.state.value.traces shouldBe listOf(trace("new"))
        }
    }

    test("state reflects feedback mutations the gateway re-emits") {
        runTest {
            val initial = trace("t1", feedback = TraceFeedback.NONE)
            val flow = MutableStateFlow(listOf(initial))
            val gateway = mockk<TraceStoreGateway>().apply {
                every { traces } returns flow
                coEvery { setFeedback(any(), any()) } returns Unit
                coEvery { clear() } returns Unit
            }
            val store = TracesStore(gateway)
            advanceUntilIdle()

            // Simulate the gateway re-emitting after setFeedback persists.
            flow.value = listOf(initial.copy(feedback = TraceFeedback.THUMBS_UP))
            advanceUntilIdle()

            store.state.value.traces.first().feedback shouldBe TraceFeedback.THUMBS_UP
        }
    }

    // ── SetFeedback ──────────────────────────────────────────────────

    test("SetFeedback(THUMBS_UP) forwards to gateway.setFeedback") {
        runTest {
            val gateway = fakeGateway()
            val store = TracesStore(gateway)

            store.dispatch(TracesIntent.SetFeedback("trace-1", TraceFeedback.THUMBS_UP))
            advanceUntilIdle()

            coVerify(exactly = 1) { gateway.setFeedback("trace-1", TraceFeedback.THUMBS_UP) }
        }
    }

    test("SetFeedback(THUMBS_DOWN) forwards to gateway.setFeedback") {
        runTest {
            val gateway = fakeGateway()
            val store = TracesStore(gateway)

            store.dispatch(TracesIntent.SetFeedback("trace-2", TraceFeedback.THUMBS_DOWN))
            advanceUntilIdle()

            coVerify(exactly = 1) { gateway.setFeedback("trace-2", TraceFeedback.THUMBS_DOWN) }
        }
    }

    test("SetFeedback(NONE) — clearing existing feedback — forwards verbatim") {
        runTest {
            val gateway = fakeGateway()
            val store = TracesStore(gateway)

            store.dispatch(TracesIntent.SetFeedback("trace-3", TraceFeedback.NONE))
            advanceUntilIdle()

            coVerify(exactly = 1) { gateway.setFeedback("trace-3", TraceFeedback.NONE) }
            coVerify(exactly = 0) { gateway.clear() }
        }
    }

    test("multiple SetFeedback in sequence are each forwarded") {
        runTest {
            val gateway = fakeGateway()
            val store = TracesStore(gateway)

            store.dispatch(TracesIntent.SetFeedback("a", TraceFeedback.THUMBS_UP))
            store.dispatch(TracesIntent.SetFeedback("b", TraceFeedback.THUMBS_DOWN))
            advanceUntilIdle()

            coVerify(exactly = 1) { gateway.setFeedback("a", TraceFeedback.THUMBS_UP) }
            coVerify(exactly = 1) { gateway.setFeedback("b", TraceFeedback.THUMBS_DOWN) }
        }
    }

    // ── ClearAll ─────────────────────────────────────────────────────

    test("ClearAll forwards to gateway.clear") {
        runTest {
            val gateway = fakeGateway()
            val store = TracesStore(gateway)

            store.dispatch(TracesIntent.ClearAll)
            advanceUntilIdle()

            coVerify(exactly = 1) { gateway.clear() }
            coVerify(exactly = 0) { gateway.setFeedback(any(), any()) }
        }
    }

    test("ClearAll plus SetFeedback both reach the gateway") {
        runTest {
            val gateway = fakeGateway()
            val store = TracesStore(gateway)

            store.dispatch(TracesIntent.ClearAll)
            store.dispatch(TracesIntent.SetFeedback("solo", TraceFeedback.THUMBS_UP))
            advanceUntilIdle()

            coVerify(exactly = 1) { gateway.clear() }
            coVerify(exactly = 1) { gateway.setFeedback("solo", TraceFeedback.THUMBS_UP) }
        }
    }
})
