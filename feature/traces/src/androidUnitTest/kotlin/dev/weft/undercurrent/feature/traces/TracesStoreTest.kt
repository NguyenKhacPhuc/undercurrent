package dev.weft.undercurrent.feature.traces

import dev.weft.undercurrent.shared.gateway.AgentTrace
import dev.weft.undercurrent.shared.gateway.TraceFeedback
import dev.weft.undercurrent.shared.gateway.TraceStatus
import dev.weft.undercurrent.shared.gateway.TraceStoreGateway
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
 * Exercises [TracesStore] in BDD style.
 *
 * Intent coverage: every [TraceFeedback] enum value through
 * SetFeedback, plus ClearAll.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TracesStoreTest : BehaviorSpec({

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

    Given("a gateway seeded with two traces") {
        val seed = listOf(trace("a"), trace("b"))
        val store = TracesStore(fakeGateway(initial = seed))

        Then("the initial state mirrors the seed list") {
            store.state.value shouldBe TracesState(traces = seed)
        }
    }

    Given("a gateway with no traces") {
        val store = TracesStore(fakeGateway())

        Then("the initial state has an empty list") {
            store.state.value shouldBe TracesState(traces = emptyList())
        }
    }

    Given("a store subscribed to a mutable traces flow") {
        val flow = MutableStateFlow<List<AgentTrace>>(emptyList())
        val gateway = mockk<TraceStoreGateway>().apply {
            every { traces } returns flow
            coEvery { setFeedback(any(), any()) } returns Unit
            coEvery { clear() } returns Unit
        }
        val store = TracesStore(gateway)

        When("the gateway emits a new list") {
            Then("the store's state reflects it") {
                runTest {
                    advanceUntilIdle()
                    flow.value = listOf(trace("new"))
                    advanceUntilIdle()

                    store.state.value.traces shouldBe listOf(trace("new"))
                }
            }
        }

        When("the gateway re-emits a trace whose feedback flipped to THUMBS_UP") {
            Then("the store reflects the feedback change") {
                runTest {
                    val initial = trace("t1", feedback = TraceFeedback.NONE)
                    val (gw2, flow2) = run {
                        val f = MutableStateFlow(listOf(initial))
                        val g = mockk<TraceStoreGateway>().apply {
                            every { traces } returns f
                            coEvery { setFeedback(any(), any()) } returns Unit
                            coEvery { clear() } returns Unit
                        }
                        g to f
                    }
                    val s = TracesStore(gw2)
                    advanceUntilIdle()

                    flow2.value = listOf(initial.copy(feedback = TraceFeedback.THUMBS_UP))
                    advanceUntilIdle()

                    s.state.value.traces.first().feedback shouldBe TraceFeedback.THUMBS_UP
                }
            }
        }
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
