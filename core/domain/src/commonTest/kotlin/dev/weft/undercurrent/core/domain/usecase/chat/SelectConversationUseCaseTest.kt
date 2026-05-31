package dev.weft.undercurrent.core.domain.usecase.chat

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
class SelectConversationUseCaseTest : BehaviorSpec({
    val mainDispatcher = StandardTestDispatcher()
    beforeTest { kotlinx.coroutines.Dispatchers.setMain(mainDispatcher) }
    afterTest { kotlinx.coroutines.Dispatchers.resetMain() }

    Given("the requested id differs from the current conversation") {
        When("invoke(id) is called") {
            Then("it issues selectConversation then loadMessages and returns the history") {
                runTest {
                    val repo = FakeChatRepository(initialConversationId = "old")
                    val history = listOf(ChatMessage(ChatRole.USER, "hi"))
                    repo.loadMessagesResult = history
                    val useCase = SelectConversationUseCase(repo)

                    val result = useCase("new")

                    repo.selectConversationCalls shouldContainExactly listOf("new")
                    repo.loadMessagesCalls shouldContainExactly listOf("new")
                    result shouldBe history
                }
            }
        }
    }

    Given("the requested id already matches the current conversation") {
        When("invoke(id) is called") {
            Then("selectConversation is skipped but loadMessages still runs") {
                runTest {
                    val repo = FakeChatRepository(initialConversationId = "same")
                    val history = listOf(ChatMessage(ChatRole.ASSISTANT, "hey"))
                    repo.loadMessagesResult = history
                    val useCase = SelectConversationUseCase(repo)

                    val result = useCase("same")

                    repo.selectConversationCalls shouldBe emptyList()
                    repo.loadMessagesCalls shouldContainExactly listOf("same")
                    result shouldBe history
                }
            }
        }
    }
})
