package dev.weft.undercurrent.feature.traces

import dev.weft.undercurrent.shared.gateway.AgentTrace
import dev.weft.undercurrent.shared.gateway.TraceFeedback
import dev.weft.undercurrent.shared.gateway.TraceStatus
import dev.weft.undercurrent.shared.gateway.TraceStoreGateway
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
 * MockK-only interaction tests for [TracesStore].
 *
 * State-projection coverage (initial state, live flow updates,
 * feedback re-emission propagation) lives in commonTest at
 * `TracesStoreStateTest.kt`. The Thens here verify gateway forwarding
 * for each `TraceFeedback` enum value and `ClearAll`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TracesStoreTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun trace(id: String): AgentTrace = AgentTrace(
        id = id,
        conversationId = "conv-1",
        startEpochMs = 0L,
        endEpochMs = 100L,
        userMessage = "msg-$id",
        finalAssistantMessage = "reply",
        status = TraceStatus.COMPLETED,
        feedback = TraceFeedback.NONE,
    )

    fun fakeGateway(initial: List<AgentTrace> = emptyList()): TraceStoreGateway {
        val gateway = mockk<TraceStoreGateway>()
        every { gateway.traces } returns MutableStateFlow(initial)
        coEvery { gateway.setFeedback(any(), any()) } returns Unit
        coEvery { gateway.clear() } returns Unit
        return gateway
    }

    Given("a fresh store with a stubbed gateway") {
        When("SetFeedback('trace-1', THUMBS_UP) is dispatched") {
            Then("gateway.setFeedback('trace-1', THUMBS_UP) is called once") {
                runTest {
                    val gateway = fakeGateway()
                    val store = TracesStore(gateway)

                    store.dispatch(TracesIntent.SetFeedback("trace-1", TraceFeedback.THUMBS_UP))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { gateway.setFeedback("trace-1", TraceFeedback.THUMBS_UP) }
                }
            }
        }

        When("SetFeedback('trace-2', THUMBS_DOWN) is dispatched") {
            Then("gateway.setFeedback('trace-2', THUMBS_DOWN) is called once") {
                runTest {
                    val gateway = fakeGateway()
                    val store = TracesStore(gateway)

                    store.dispatch(TracesIntent.SetFeedback("trace-2", TraceFeedback.THUMBS_DOWN))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { gateway.setFeedback("trace-2", TraceFeedback.THUMBS_DOWN) }
                }
            }
        }

        When("SetFeedback('trace-3', NONE) is dispatched — clearing existing feedback") {
            Then("the call is forwarded verbatim and clear is not invoked") {
                runTest {
                    val gateway = fakeGateway()
                    val store = TracesStore(gateway)

                    store.dispatch(TracesIntent.SetFeedback("trace-3", TraceFeedback.NONE))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { gateway.setFeedback("trace-3", TraceFeedback.NONE) }
                    coVerify(exactly = 0) { gateway.clear() }
                }
            }
        }

        When("SetFeedback is dispatched twice for two different traces") {
            Then("each call lands with the right (id, feedback) pair") {
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
        }

        When("ClearAll is dispatched") {
            Then("gateway.clear is called once and setFeedback is not") {
                runTest {
                    val gateway = fakeGateway()
                    val store = TracesStore(gateway)

                    store.dispatch(TracesIntent.ClearAll)
                    advanceUntilIdle()

                    coVerify(exactly = 1) { gateway.clear() }
                    coVerify(exactly = 0) { gateway.setFeedback(any(), any()) }
                }
            }
        }

        When("ClearAll then SetFeedback are dispatched in sequence") {
            Then("each method fires exactly once") {
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
        }
    }
})
