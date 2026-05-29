package dev.weft.undercurrent.feature.traces

import dev.weft.undercurrent.shared.gateway.AgentTrace
import dev.weft.undercurrent.shared.gateway.TraceFeedback
import dev.weft.undercurrent.shared.gateway.TraceStatus
import dev.weft.undercurrent.shared.gateway.TraceStoreGateway
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
 * KMP-portable state-projection tests for [TracesStore]. Runs on
 * Android + iOS.
 *
 * Uses a hand-rolled [FakeTraceStoreGateway] instead of MockK so the
 * spec can live in commonTest. Per-enum-variant SetFeedback
 * forwarding + ClearAll invocation verification live in
 * `TracesStoreTest` under androidUnitTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TracesStoreStateTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun trace(
        id: String,
        feedback: TraceFeedback = TraceFeedback.NONE,
    ): AgentTrace = AgentTrace(
        id = id,
        conversationId = "conv-1",
        startEpochMs = 0L,
        endEpochMs = 100L,
        userMessage = "msg-$id",
        finalAssistantMessage = "reply",
        status = TraceStatus.COMPLETED,
        feedback = feedback,
    )

    Given("a gateway seeded with two traces") {
        val seed = listOf(trace("a"), trace("b"))
        val store = TracesStore(FakeTraceStoreGateway(initial = seed))

        Then("the initial state mirrors the seed list") {
            store.state.value shouldBe TracesState(traces = seed)
        }
    }

    Given("a gateway with no traces") {
        val store = TracesStore(FakeTraceStoreGateway())

        Then("the initial state has an empty list") {
            store.state.value shouldBe TracesState(traces = emptyList())
        }
    }

    Given("a store subscribed to a mutable traces flow") {
        val gateway = FakeTraceStoreGateway()
        val store = TracesStore(gateway)

        When("the gateway emits a new list") {
            Then("the store's state reflects it") {
                runTest {
                    advanceUntilIdle()
                    gateway.emit(listOf(trace("new")))
                    advanceUntilIdle()

                    store.state.value.traces shouldBe listOf(trace("new"))
                }
            }
        }

        When("the gateway re-emits a trace whose feedback flipped to THUMBS_UP") {
            Then("the store reflects the feedback change") {
                runTest {
                    val initial = trace("t1", feedback = TraceFeedback.NONE)
                    gateway.emit(listOf(initial))
                    advanceUntilIdle()

                    gateway.emit(listOf(initial.copy(feedback = TraceFeedback.THUMBS_UP)))
                    advanceUntilIdle()

                    store.state.value.traces.first().feedback shouldBe TraceFeedback.THUMBS_UP
                }
            }
        }
    }
})

/**
 * KMP-portable [TraceStoreGateway] fake. Suspend methods are no-ops;
 * call-verification tests live in TracesStoreTest under
 * androidUnitTest.
 */
private class FakeTraceStoreGateway(
    initial: List<AgentTrace> = emptyList(),
) : TraceStoreGateway {
    private val _traces = MutableStateFlow(initial)
    override val traces: StateFlow<List<AgentTrace>> get() = _traces

    fun emit(value: List<AgentTrace>) {
        _traces.value = value
    }

    override suspend fun setFeedback(traceId: String, feedback: TraceFeedback) { /* no-op */ }
    override suspend fun clear() { /* no-op */ }
}
