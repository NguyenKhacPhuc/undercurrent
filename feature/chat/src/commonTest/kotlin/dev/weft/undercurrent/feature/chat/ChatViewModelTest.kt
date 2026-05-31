package dev.weft.undercurrent.feature.chat

import dev.weft.undercurrent.core.domain.AgentSummary
import dev.weft.undercurrent.core.domain.ChatChunk
import dev.weft.undercurrent.core.domain.ChatMessage
import dev.weft.undercurrent.core.domain.ChatRepository
import dev.weft.undercurrent.core.domain.ChatRole
import dev.weft.undercurrent.core.domain.PermissionDialogPayload
import dev.weft.undercurrent.core.domain.usecase.chat.DeleteCurrentConversationUseCase
import dev.weft.undercurrent.core.domain.usecase.chat.ObserveChatStateUseCase
import dev.weft.undercurrent.core.domain.usecase.chat.SelectAgentUseCase
import dev.weft.undercurrent.core.domain.usecase.chat.SelectConversationUseCase
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.feature.chat.components.DisplayRole
import dev.weft.undercurrent.feature.chat.components.ToolStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * KMP-portable BDD coverage for [ChatViewModel]. Builds a
 * [FakeChatRepository] and stacks real use cases on top — mirrors the
 * production wiring shape so test-time behavior matches what the
 * Koin module assembles at runtime.
 *
 * Covers:
 *   - initial hydration from [ObserveChatStateUseCase] (conversation
 *     id, ready flag, agent name, available agents)
 *   - intent dispatch effects on [ChatViewModel.displayMessages] +
 *     [ChatState] (NewChat, SelectConversation, DeleteCurrentConversation,
 *     DeleteConversation, SelectAgent)
 *   - streaming-fold logic in `runStreamingTurn` + `foldChunk`
 *     (TextDelta accumulation, ToolStart/Done/Fail, Error, Done)
 *   - the `if (current.inFlight) return` re-entrance guard
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun build(repo: FakeChatRepository): ChatViewModel = ChatViewModel(
        repo = repo,
        selectConversation = SelectConversationUseCase(repo),
        deleteCurrentConversation = DeleteCurrentConversationUseCase(repo),
        selectAgent = SelectAgentUseCase(repo),
        observeChatState = ObserveChatStateUseCase(repo),
    )

    // ── initial hydration ────────────────────────────────────────────

    Given("a ChatViewModel built over an empty repo") {
        Then("initial state is the ChatState.initial() snapshot") {
            runTest {
                val repo = FakeChatRepository()
                val vm = build(repo)
                advanceUntilIdle()

                vm.state.value.inFlight shouldBe false
                vm.state.value.lastError shouldBe null
                vm.state.value.currentConversationId shouldBe null
                vm.state.value.agentReady shouldBe false
                vm.displayMessages.isEmpty() shouldBe true
            }
        }
    }

    Given("a ChatViewModel built over a repo seeded with conversation + ready state") {
        Then("state hydrates from the observed snapshot after init drains") {
            runTest {
                val agents = listOf(AgentSummary("default", "Default", "desc"))
                val repo = FakeChatRepository(
                    initialConversationId = "conv-1",
                    initialIsReady = true,
                    initialAgentName = "default",
                    initialAgents = agents,
                )
                val vm = build(repo)
                advanceUntilIdle()

                vm.state.value.currentConversationId shouldBe "conv-1"
                vm.state.value.agentReady shouldBe true
                vm.state.value.activeAgentName shouldBe "default"
                vm.state.value.availableAgents shouldBe agents
            }
        }
    }

    Given("a ChatViewModel observing live repository emissions") {
        Then("state mirrors the repo as flows re-emit") {
            runTest {
                val repo = FakeChatRepository()
                val vm = build(repo)
                advanceUntilIdle()

                repo._currentConversationId.value = "conv-99"
                repo._isReady.value = true
                advanceUntilIdle()

                vm.state.value.currentConversationId shouldBe "conv-99"
                vm.state.value.agentReady shouldBe true
            }
        }
    }

    // ── streaming fold: TextDelta accumulation ───────────────────────

    Given("a ChatViewModel handed a stream of two TextDeltas") {
        Then("the user message and a single coalesced assistant message land") {
            runTest {
                val repo = FakeChatRepository()
                repo.sendFlow = flowOf(
                    ChatChunk.TextDelta("Hello, "),
                    ChatChunk.TextDelta("world"),
                    ChatChunk.Done,
                )
                val vm = build(repo)
                advanceUntilIdle()

                vm.dispatch(ChatIntent.SendChat("hi"))
                advanceUntilIdle()

                vm.displayMessages.size shouldBe 2
                vm.displayMessages[0].role shouldBe DisplayRole.USER
                vm.displayMessages[0].text shouldBe "hi"
                vm.displayMessages[1].role shouldBe DisplayRole.ASSISTANT
                vm.displayMessages[1].text shouldBe "Hello, world"
                vm.state.value.inFlight shouldBe false
            }
        }
    }

    // ── streaming fold: tool lifecycle ───────────────────────────────

    Given("a ChatViewModel handed a stream with ToolStart + ToolDone") {
        Then("the user message + two tool bubbles append in order") {
            runTest {
                val repo = FakeChatRepository()
                repo.sendFlow = flowOf(
                    ChatChunk.ToolStart("read_file"),
                    ChatChunk.ToolDone("read_file"),
                    ChatChunk.Done,
                )
                val vm = build(repo)
                advanceUntilIdle()

                vm.dispatch(ChatIntent.SendChat("run tool"))
                advanceUntilIdle()

                vm.displayMessages.size shouldBe 3
                vm.displayMessages[0].role shouldBe DisplayRole.USER
                vm.displayMessages[1].role shouldBe DisplayRole.TOOL
                vm.displayMessages[1].tool?.status shouldBe ToolStatus.RUNNING
                vm.displayMessages[2].role shouldBe DisplayRole.TOOL
                vm.displayMessages[2].tool?.status shouldBe ToolStatus.DONE
            }
        }
    }

    Given("a ChatViewModel handed a ToolFail without a permission payload") {
        Then("a failed tool bubble appends after the user message") {
            runTest {
                val repo = FakeChatRepository()
                repo.sendFlow = flowOf(
                    ChatChunk.ToolFail("write_file", "disk full"),
                    ChatChunk.Done,
                )
                val vm = build(repo)
                advanceUntilIdle()

                vm.dispatch(ChatIntent.SendChat("try"))
                advanceUntilIdle()

                vm.displayMessages.size shouldBe 2
                vm.displayMessages[0].role shouldBe DisplayRole.USER
                vm.displayMessages[1].tool?.status shouldBe ToolStatus.FAILED
                vm.displayMessages[1].tool?.resultPreview shouldBe "disk full"
            }
        }
    }

    Given("a ChatViewModel handed a ToolFail with a permission payload") {
        Then("the failed tool bubble lands and carries the friendly copy") {
            runTest {
                val payload = PermissionDialogPayload(
                    toolName = "open_camera",
                    friendlyTitle = "Camera needed",
                    friendlyBody = "Open settings",
                )
                val repo = FakeChatRepository()
                repo.sendFlow = flowOf(
                    ChatChunk.ToolFail("open_camera", "Permission denied", payload),
                    ChatChunk.Done,
                )
                val vm = build(repo)
                advanceUntilIdle()

                vm.dispatch(ChatIntent.SendChat("snap"))
                advanceUntilIdle()

                vm.displayMessages.size shouldBe 2
                val tool = vm.displayMessages[1].tool
                tool shouldNotBe null
                tool?.status shouldBe ToolStatus.FAILED
            }
        }
    }

    // ── streaming fold: error path ───────────────────────────────────

    Given("a ChatViewModel handed an Error chunk") {
        Then("inFlight clears and lastError is set") {
            runTest {
                val repo = FakeChatRepository()
                repo.sendFlow = flowOf(ChatChunk.Error("boom"))
                val vm = build(repo)
                advanceUntilIdle()

                vm.dispatch(ChatIntent.SendChat("hi"))
                advanceUntilIdle()

                vm.state.value.inFlight shouldBe false
                vm.state.value.lastError shouldBe "boom"
            }
        }
    }

    // ── re-entrance guard ────────────────────────────────────────────

    Given("a ChatViewModel where the first stream is held mid-flight via a Channel") {
        Then("a second SendChat dispatched while inFlight is short-circuited by the guard") {
            runTest {
                val channel = kotlinx.coroutines.channels.Channel<ChatChunk>(capacity = Int.MAX_VALUE)
                val repo = FakeChatRepository()
                repo.sendFlow = channel.consumeAsFlow()
                val vm = build(repo)
                advanceUntilIdle()

                // First dispatch — kicks off runStreamingTurn which
                // suspends inside the Channel's consumeAsFlow collect
                // (no Done emitted yet, channel still open).
                vm.dispatch(ChatIntent.SendChat("first"))
                advanceUntilIdle()
                vm.state.value.inFlight shouldBe true
                val sendsAfterFirst = repo.sendCalls.size
                val displaySizeAfterFirst = vm.displayMessages.size

                // Second dispatch arrives while inFlight is still true.
                // The guard inside runStreamingTurn returns early
                // *before* the user-message is appended (the guard
                // runs before the displayMessages += user branch).
                vm.dispatch(ChatIntent.SendChat("second"))
                advanceUntilIdle()
                vm.state.value.inFlight shouldBe true

                // sendChat use case still runs (eager Flow build) so
                // sendCalls grows by one — the guard kicks in *after*
                // the Flow is materialized but before the user-message
                // is appended.
                repo.sendCalls.size shouldBe (sendsAfterFirst + 1)
                vm.displayMessages.size shouldBe displaySizeAfterFirst

                // Release the first stream so runTest doesn't hang on
                // a dangling viewModelScope coroutine.
                channel.close()
                advanceUntilIdle()
                vm.state.value.inFlight shouldBe false
            }
        }
    }

    // ── intent dispatch: NewChat ─────────────────────────────────────

    Given("a ChatViewModel with display messages present") {
        Then("NewChat clears displayMessages and resets transient state") {
            runTest {
                val repo = FakeChatRepository()
                repo.sendFlow = flowOf(ChatChunk.TextDelta("hi"), ChatChunk.Done)
                val vm = build(repo)
                advanceUntilIdle()
                vm.dispatch(ChatIntent.SendChat("hello"))
                advanceUntilIdle()
                vm.displayMessages.isEmpty() shouldBe false

                vm.dispatch(ChatIntent.NewChat)
                advanceUntilIdle()

                vm.displayMessages.isEmpty() shouldBe true
                vm.state.value.inFlight shouldBe false
                vm.state.value.lastError shouldBe null
                repo.newChatCalls shouldBe 1
            }
        }
    }

    // ── intent dispatch: SelectConversation ──────────────────────────

    Given("a ChatViewModel asked to SelectConversation by a new id") {
        Then("display messages reseed from the loaded history") {
            runTest {
                val repo = FakeChatRepository(initialConversationId = "old")
                repo.loadMessagesResult = listOf(
                    ChatMessage(ChatRole.USER, "u-1"),
                    ChatMessage(ChatRole.ASSISTANT, "a-1"),
                )
                val vm = build(repo)
                advanceUntilIdle()

                vm.dispatch(ChatIntent.SelectConversation("new"))
                advanceUntilIdle()

                vm.displayMessages.size shouldBe 2
                vm.displayMessages[0].role shouldBe DisplayRole.USER
                vm.displayMessages[0].text shouldBe "u-1"
                vm.displayMessages[1].role shouldBe DisplayRole.ASSISTANT
                vm.displayMessages[1].text shouldBe "a-1"
                repo.selectConversationCalls shouldBe mutableListOf("new")
            }
        }
    }

    Given("a ChatViewModel asked to SelectConversation by the active id") {
        Then("the call is short-circuited — no repo write, no display change") {
            runTest {
                val repo = FakeChatRepository(initialConversationId = "same")
                val vm = build(repo)
                advanceUntilIdle()

                vm.dispatch(ChatIntent.SelectConversation("same"))
                advanceUntilIdle()

                repo.selectConversationCalls.shouldBeEmpty()
            }
        }
    }

    // ── intent dispatch: DeleteCurrentConversation ───────────────────

    Given("a ChatViewModel with an active conversation") {
        Then("DeleteCurrentConversation clears displayMessages and resets state") {
            runTest {
                val repo = FakeChatRepository(initialConversationId = "active")
                repo.sendFlow = flowOf(ChatChunk.TextDelta("hi"), ChatChunk.Done)
                val vm = build(repo)
                advanceUntilIdle()
                vm.dispatch(ChatIntent.SendChat("seed"))
                advanceUntilIdle()
                vm.displayMessages.isEmpty() shouldBe false

                vm.dispatch(ChatIntent.DeleteCurrentConversation)
                advanceUntilIdle()

                vm.displayMessages.isEmpty() shouldBe true
                vm.state.value.inFlight shouldBe false
                vm.state.value.lastError shouldBe null
                repo.deleteConversationCalls shouldBe mutableListOf("active")
            }
        }
    }

    Given("a ChatViewModel with no active conversation") {
        Then("DeleteCurrentConversation is a no-op (use case returns false)") {
            runTest {
                val repo = FakeChatRepository(initialConversationId = null)
                val vm = build(repo)
                advanceUntilIdle()

                vm.dispatch(ChatIntent.DeleteCurrentConversation)
                advanceUntilIdle()

                repo.deleteConversationCalls.shouldBeEmpty()
            }
        }
    }

    // ── intent dispatch: DeleteConversation ──────────────────────────

    Given("a ChatViewModel deleting the currently active conversation by id") {
        Then("displayMessages clear + state resets after the delete") {
            runTest {
                val repo = FakeChatRepository(initialConversationId = "c-1")
                repo.sendFlow = flowOf(ChatChunk.TextDelta("hi"), ChatChunk.Done)
                val vm = build(repo)
                advanceUntilIdle()
                vm.dispatch(ChatIntent.SendChat("seed"))
                advanceUntilIdle()

                vm.dispatch(ChatIntent.DeleteConversation("c-1"))
                advanceUntilIdle()

                vm.displayMessages.isEmpty() shouldBe true
                vm.state.value.inFlight shouldBe false
                repo.deleteConversationCalls shouldBe mutableListOf("c-1")
            }
        }
    }

    Given("a ChatViewModel deleting a non-active conversation by id") {
        Then("displayMessages persist — only the inactive thread is removed") {
            runTest {
                val repo = FakeChatRepository(initialConversationId = "active")
                repo.sendFlow = flowOf(ChatChunk.TextDelta("hi"), ChatChunk.Done)
                val vm = build(repo)
                advanceUntilIdle()
                vm.dispatch(ChatIntent.SendChat("seed"))
                advanceUntilIdle()
                val sizeBefore = vm.displayMessages.size

                vm.dispatch(ChatIntent.DeleteConversation("other"))
                advanceUntilIdle()

                vm.displayMessages.size shouldBe sizeBefore
                repo.deleteConversationCalls shouldBe mutableListOf("other")
            }
        }
    }

    // ── intent dispatch: SelectAgent ─────────────────────────────────

    Given("a ChatViewModel asked to switch to a registered different agent") {
        Then("displayMessages reseed from the new agent's loaded history") {
            runTest {
                val agents = listOf(
                    AgentSummary("default", "Default", ""),
                    AgentSummary("researcher", "Researcher", ""),
                )
                val repo = FakeChatRepository(
                    initialConversationId = "c-1",
                    initialAgentName = "default",
                    initialAgents = agents,
                )
                repo.loadMessagesResult = listOf(ChatMessage(ChatRole.USER, "u"))
                val vm = build(repo)
                advanceUntilIdle()

                vm.dispatch(ChatIntent.SelectAgent("researcher"))
                advanceUntilIdle()

                repo.selectAgentCalls shouldBe mutableListOf("researcher")
                vm.displayMessages.size shouldBe 1
                vm.displayMessages[0].role shouldBe DisplayRole.USER
            }
        }
    }

    Given("a ChatViewModel asked to switch to the already-active agent") {
        Then("the use case short-circuits — no repo write, no reseed") {
            runTest {
                val agents = listOf(AgentSummary("default", "Default", ""))
                val repo = FakeChatRepository(
                    initialConversationId = "c-1",
                    initialAgentName = "default",
                    initialAgents = agents,
                )
                val vm = build(repo)
                advanceUntilIdle()

                vm.dispatch(ChatIntent.SelectAgent("default"))
                advanceUntilIdle()

                repo.selectAgentCalls.shouldBeEmpty()
            }
        }
    }

    // ── send() with explicit modelTier ───────────────────────────────

    Given("a ChatViewModel where SendChat carries a modelTier") {
        Then("the use case forwards the tier into the repo call") {
            runTest {
                val repo = FakeChatRepository()
                repo.sendFlow = flowOf(ChatChunk.Done)
                val vm = build(repo)
                advanceUntilIdle()

                vm.dispatch(ChatIntent.SendChat("text", modelTier = ModelTier.Heavy))
                advanceUntilIdle()

                repo.sendCalls shouldHaveSize 1
                repo.sendCalls[0] shouldBe ("text" to ModelTier.Heavy)
            }
        }
    }

    // ── RegenerateLast ───────────────────────────────────────────────

    Given("a ChatViewModel asked to RegenerateLast") {
        Then("the repository's regenerate path is invoked and Done clears in-flight") {
            runTest {
                val repo = FakeChatRepository()
                repo.regenerateFlow = flowOf(
                    ChatChunk.TextDelta("again"),
                    ChatChunk.Done,
                )
                val vm = build(repo)
                advanceUntilIdle()

                vm.dispatch(ChatIntent.RegenerateLast)
                advanceUntilIdle()

                repo.regenerateCalls shouldBe 1
                vm.state.value.inFlight shouldBe false
                vm.displayMessages.size shouldBe 1
                vm.displayMessages[0].text shouldBe "again"
            }
        }
    }
})

private class FakeChatRepository(
    initialConversationId: String? = null,
    initialIsReady: Boolean = false,
    initialAgentName: String = "",
    initialAgents: List<AgentSummary> = emptyList(),
) : ChatRepository {

    val _currentConversationId = MutableStateFlow(initialConversationId)
    val _isReady = MutableStateFlow(initialIsReady)
    val _activeAgentName = MutableStateFlow(initialAgentName)
    val _availableAgents = MutableStateFlow(initialAgents)

    override val currentConversationId: StateFlow<String?> get() = _currentConversationId
    override val isReady: StateFlow<Boolean> get() = _isReady
    override val activeAgentName: StateFlow<String> get() = _activeAgentName
    override val availableAgents: StateFlow<List<AgentSummary>> get() = _availableAgents

    var sendFlow: Flow<ChatChunk> = flowOf(ChatChunk.Done)
    var regenerateFlow: Flow<ChatChunk> = flowOf(ChatChunk.Done)
    var loadMessagesResult: List<ChatMessage> = emptyList()

    var sendCalls: MutableList<Pair<String, ModelTier?>> = mutableListOf()
    var regenerateCalls: Int = 0
    var resumeCalls: Int = 0
    var newChatCalls: Int = 0
    var selectConversationCalls: MutableList<String> = mutableListOf()
    var deleteConversationCalls: MutableList<String> = mutableListOf()
    var selectAgentCalls: MutableList<String> = mutableListOf()
    var loadMessagesCalls: MutableList<String> = mutableListOf()
    var sendUiEventCalls: MutableList<Triple<String, String?, Map<String, String>>> = mutableListOf()

    override fun send(text: String, modelTier: ModelTier?): Flow<ChatChunk> {
        sendCalls += text to modelTier
        return sendFlow
    }

    override fun regenerateLast(): Flow<ChatChunk> {
        regenerateCalls++
        return regenerateFlow
    }

    override suspend fun resume() {
        resumeCalls++
    }

    override suspend fun newChat() {
        newChatCalls++
    }

    override suspend fun selectConversation(id: String) {
        selectConversationCalls += id
    }

    override suspend fun deleteConversation(id: String) {
        deleteConversationCalls += id
    }

    override suspend fun selectAgent(name: String) {
        selectAgentCalls += name
    }

    override suspend fun loadMessages(conversationId: String): List<ChatMessage> {
        loadMessagesCalls += conversationId
        return loadMessagesResult
    }

    override suspend fun sendUiEvent(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    ) {
        sendUiEventCalls += Triple(action, sourceLabel, fieldValues)
    }
}

