package dev.weft.undercurrent.core.domain.usecase.chat

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DeleteCurrentConversationUseCaseTest : BehaviorSpec({
    val mainDispatcher = StandardTestDispatcher()
    beforeTest { kotlinx.coroutines.Dispatchers.setMain(mainDispatcher) }
    afterTest { kotlinx.coroutines.Dispatchers.resetMain() }

    Given("the repo has no current conversation") {
        When("invoke() is called") {
            Then("it returns false and never calls deleteConversation") {
                runTest {
                    val repo = FakeChatRepository(initialConversationId = null)
                    val useCase = DeleteCurrentConversationUseCase(repo)

                    val deleted = useCase()

                    deleted shouldBe false
                    repo.deleteConversationCalls shouldBe emptyList()
                }
            }
        }
    }

    Given("the repo has a current conversation") {
        When("invoke() is called") {
            Then("it returns true and forwards the id to deleteConversation") {
                runTest {
                    val repo = FakeChatRepository(initialConversationId = "conv-7")
                    val useCase = DeleteCurrentConversationUseCase(repo)

                    val deleted = useCase()

                    deleted shouldBe true
                    repo.deleteConversationCalls shouldContainExactly listOf("conv-7")
                }
            }
        }
    }
})
