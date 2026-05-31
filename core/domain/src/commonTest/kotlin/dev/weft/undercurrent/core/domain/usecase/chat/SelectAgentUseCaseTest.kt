package dev.weft.undercurrent.core.domain.usecase.chat

import dev.weft.undercurrent.core.domain.AgentSummary
import dev.weft.undercurrent.core.domain.ChatMessage
import dev.weft.undercurrent.core.domain.ChatRole
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SelectAgentUseCaseTest : BehaviorSpec({
    val mainDispatcher = StandardTestDispatcher()
    beforeTest { kotlinx.coroutines.Dispatchers.setMain(mainDispatcher) }
    afterTest { kotlinx.coroutines.Dispatchers.resetMain() }

    val coach = AgentSummary("coach", "Coach", "")
    val planner = AgentSummary("planner", "Planner", "")

    Given("the requested name matches the currently active agent") {
        When("invoke(name) is called") {
            Then("it returns empty list and does not call selectAgent") {
                runTest {
                    val repo = FakeChatRepository(
                        initialAgentName = "coach",
                        initialAgents = listOf(coach, planner),
                        initialConversationId = "c1",
                    )
                    val useCase = SelectAgentUseCase(repo)

                    val result = useCase("coach")

                    result shouldBe emptyList()
                    repo.selectAgentCalls shouldBe emptyList()
                    repo.loadMessagesCalls shouldBe emptyList()
                }
            }
        }
    }

    Given("the requested name isn't in availableAgents") {
        When("invoke(name) is called") {
            Then("it returns empty list and does not call selectAgent") {
                runTest {
                    val repo = FakeChatRepository(
                        initialAgentName = "coach",
                        initialAgents = listOf(coach),
                        initialConversationId = "c1",
                    )
                    val useCase = SelectAgentUseCase(repo)

                    val result = useCase("ghost")

                    result shouldBe emptyList()
                    repo.selectAgentCalls shouldBe emptyList()
                }
            }
        }
    }

    Given("a valid agent swap but no current conversation") {
        When("invoke(name) is called") {
            Then("it calls selectAgent then returns empty list without loading messages") {
                runTest {
                    val repo = FakeChatRepository(
                        initialAgentName = "coach",
                        initialAgents = listOf(coach, planner),
                        initialConversationId = null,
                    )
                    val useCase = SelectAgentUseCase(repo)

                    val result = useCase("planner")

                    result shouldBe emptyList()
                    repo.selectAgentCalls shouldContainExactly listOf("planner")
                    repo.loadMessagesCalls shouldBe emptyList()
                }
            }
        }
    }

    Given("a valid agent swap with a current conversation") {
        When("invoke(name) is called") {
            Then("it calls selectAgent, loads messages for the current convo, and returns them") {
                runTest {
                    val repo = FakeChatRepository(
                        initialAgentName = "coach",
                        initialAgents = listOf(coach, planner),
                        initialConversationId = "c1",
                    )
                    val history = listOf(ChatMessage(ChatRole.USER, "hello"))
                    repo.loadMessagesResult = history
                    val useCase = SelectAgentUseCase(repo)

                    val result = useCase("planner")

                    result shouldBe history
                    repo.selectAgentCalls shouldContainExactly listOf("planner")
                    repo.loadMessagesCalls shouldContainExactly listOf("c1")
                }
            }
        }
    }
})
