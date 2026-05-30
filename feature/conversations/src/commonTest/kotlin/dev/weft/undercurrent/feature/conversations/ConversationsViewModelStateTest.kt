package dev.weft.undercurrent.feature.conversations

import dev.weft.undercurrent.core.domain.ConversationStoreRepository
import dev.weft.undercurrent.core.domain.ConversationSummary
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * KMP-portable state-projection tests for [ConversationsViewModel]. Runs
 * on Android + iOS.
 *
 * Uses a hand-rolled [FakeConversationStoreGateway] instead of MockK
 * so the spec can live in commonTest. The dispatch + interaction
 * tests (SetQuery resubscribe verification, Delete / ClearAll
 * forwarding) live in `ConversationsMviViewModelTest` under
 * androidUnitTest — MockK's `coVerify` is JVM-only.
 *
 * What's NOT here: the stale-emission isolation test (verifying the
 * old job is cancelled when SetQuery resubscribes) is in the
 * androidUnitTest spec because it requires per-query Flow stubbing
 * that's a lot tidier with MockK's `every { search("a") } returns …`
 * than with a hand-rolled fake.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsViewModelStateTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun summary(id: String, title: String = id, ts: Long = 0L): ConversationSummary =
        ConversationSummary(id = id, title = title, createdAtMs = ts, lastMessageAtMs = ts)

    Given("a fresh ConversationsViewModel") {
        val store = ConversationsViewModel(FakeConversationStoreGateway())

        Then("the initial state has empty query and empty conversations") {
            store.state.value shouldBe ConversationsState()
        }
    }

    Given("a gateway whose initial search returns two conversations") {
        Then("the store reflects them after the init subscription drains") {
            runTest {
                val results = listOf(summary("a"), summary("b"))
                val gateway = FakeConversationStoreGateway(initialResults = results)

                val store = ConversationsViewModel(gateway)
                advanceUntilIdle()

                store.state.value.conversations shouldBe results
            }
        }
    }

    Given("a store with an active subscription") {
        When("SetQuery('hello') is dispatched") {
            Then("the query slot updates immediately on the state flow") {
                val store = ConversationsViewModel(FakeConversationStoreGateway())
                store.dispatch(ConversationsIntent.SetQuery("hello"))

                store.state.value.query shouldBe "hello"
            }
        }
    }

    Given("a store with a mutable search flow") {
        Then("emissions update conversations live as they arrive") {
            runTest {
                val gateway = FakeConversationStoreGateway()
                val store = ConversationsViewModel(gateway)
                advanceUntilIdle()

                gateway.emit(listOf(summary("a")))
                advanceUntilIdle()
                store.state.value.conversations shouldBe listOf(summary("a"))

                gateway.emit(listOf(summary("a"), summary("b")))
                advanceUntilIdle()
                store.state.value.conversations shouldBe listOf(summary("a"), summary("b"))
            }
        }
    }

    Given("a store with the user typing into the query slot") {
        Then("the query value is not touched by a Delete dispatch") {
            runTest {
                val gateway = FakeConversationStoreGateway()
                val store = ConversationsViewModel(gateway)
                store.dispatch(ConversationsIntent.SetQuery("keeper"))
                advanceUntilIdle()

                store.dispatch(ConversationsIntent.Delete("conv-42"))
                advanceUntilIdle()

                store.state.value.query shouldBe "keeper"
            }
        }
    }
})

/**
 * KMP-portable [ConversationStoreRepository] fake. The search()
 * implementation returns the same flow for every query — the
 * production behavior where different queries yield different results
 * isn't covered here (it's tested via MockK in
 * ConversationsMviViewModelTest under androidUnitTest).
 */
private class FakeConversationStoreGateway(
    initialResults: List<ConversationSummary> = emptyList(),
) : ConversationStoreRepository {
    private val _results = MutableStateFlow(initialResults)

    override fun search(query: String): Flow<List<ConversationSummary>> = _results

    /** Push a new list into the shared search flow. */
    fun emit(value: List<ConversationSummary>) {
        _results.value = value
    }

    override suspend fun deleteConversation(id: String) { /* no-op */ }
    override suspend fun clearAll() { /* no-op */ }
}
