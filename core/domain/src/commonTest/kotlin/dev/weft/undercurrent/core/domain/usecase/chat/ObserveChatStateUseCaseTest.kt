package dev.weft.undercurrent.core.domain.usecase.chat

import app.cash.turbine.test
import dev.weft.undercurrent.core.domain.AgentSummary
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ObserveChatStateUseCaseTest : BehaviorSpec({
    val mainDispatcher = StandardTestDispatcher()
    beforeTest { kotlinx.coroutines.Dispatchers.setMain(mainDispatcher) }
    afterTest { kotlinx.coroutines.Dispatchers.resetMain() }

    val coach = AgentSummary("coach", "Coach", "")
    val planner = AgentSummary("planner", "Planner", "")

    Given("a fresh repo with initial values across all four flows") {
        When("the combined flow is collected") {
            Then("the first snapshot mirrors the repo's current values") {
                runTest {
                    val repo = FakeChatRepository(
                        initialConversationId = "c1",
                        initialIsReady = true,
                        initialAgentName = "coach",
                        initialAgents = listOf(coach),
                    )
                    val useCase = ObserveChatStateUseCase(repo)

                    useCase().test {
                        awaitItem() shouldBe ChatStateSnapshot(
                            currentConversationId = "c1",
                            isReady = true,
                            activeAgentName = "coach",
                            availableAgents = listOf(coach),
                        )
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }
        }
    }

    Given("a repo whose source flows mutate over time") {
        When("each of the four flows emits a new value") {
            Then("the combined flow re-emits a fresh snapshot per change") {
                runTest {
                    val repo = FakeChatRepository(
                        initialConversationId = null,
                        initialIsReady = false,
                        initialAgentName = "",
                        initialAgents = emptyList(),
                    )
                    val useCase = ObserveChatStateUseCase(repo)

                    useCase().test {
                        awaitItem() shouldBe ChatStateSnapshot(
                            currentConversationId = null,
                            isReady = false,
                            activeAgentName = "",
                            availableAgents = emptyList(),
                        )

                        repo._currentConversationId.value = "c1"
                        awaitItem().currentConversationId shouldBe "c1"

                        repo._isReady.value = true
                        awaitItem().isReady shouldBe true

                        repo._activeAgentName.value = "planner"
                        awaitItem().activeAgentName shouldBe "planner"

                        repo._availableAgents.value = listOf(coach, planner)
                        awaitItem().availableAgents shouldBe listOf(coach, planner)

                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }
        }
    }
})
