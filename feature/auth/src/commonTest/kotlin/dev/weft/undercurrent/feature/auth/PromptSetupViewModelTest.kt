@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.weft.undercurrent.feature.auth

import dev.weft.undercurrent.core.domain.prompt.PromptConfigRepository
import dev.weft.undercurrent.core.model.PromptConfig
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

private class FakeRepo(cached: PromptConfig? = null) : PromptConfigRepository {
    val state = MutableStateFlow(cached)
    override val current: Flow<PromptConfig?> = state
    var onRefresh: () -> PromptConfig? = { null }
    override suspend fun refresh(): PromptConfig? {
        val c = onRefresh()
        if (c != null) state.value = c
        return c
    }
}

private val sample = PromptConfig("You are…", "rev.1", 1L)

class PromptSetupViewModelTest : BehaviorSpec({
    val dispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(dispatcher) }
    afterTest { Dispatchers.resetMain() }

    Given("a prompt is already cached") {
        Then("the gate resolves to Ready without needing a fetch") {
            runTest {
                val vm = PromptSetupViewModel(FakeRepo(cached = sample))
                advanceUntilIdle()
                vm.state.value.phase shouldBe PromptSetupPhase.Ready
            }
        }
    }

    Given("no cached prompt and the fetch succeeds") {
        Then("the gate fetches then resolves to Ready") {
            runTest {
                val repo = FakeRepo().apply { onRefresh = { sample } }
                val vm = PromptSetupViewModel(repo)
                advanceUntilIdle()
                vm.state.value.phase shouldBe PromptSetupPhase.Ready
            }
        }
    }

    Given("no cached prompt and the fetch fails") {
        Then("the gate shows the Failed (couldn't connect) state") {
            runTest {
                val repo = FakeRepo().apply { onRefresh = { null } }
                val vm = PromptSetupViewModel(repo)
                advanceUntilIdle()
                vm.state.value.phase shouldBe PromptSetupPhase.Failed
            }
        }
    }

    Given("the gate is in the Failed state") {
        Then("retrying after the connection returns resolves to Ready") {
            runTest {
                val repo = FakeRepo().apply { onRefresh = { null } }
                val vm = PromptSetupViewModel(repo)
                advanceUntilIdle()
                vm.state.value.phase shouldBe PromptSetupPhase.Failed

                repo.onRefresh = { sample } // connection returns
                vm.dispatch(PromptSetupIntent.Retry)
                advanceUntilIdle()
                vm.state.value.phase shouldBe PromptSetupPhase.Ready
            }
        }
    }
})
