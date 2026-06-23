package dev.weft.undercurrent.feature.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.weft.undercurrent.core.domain.ChatChunk
import dev.weft.undercurrent.core.domain.ChatMessage
import dev.weft.undercurrent.core.domain.ChatRepository
import dev.weft.undercurrent.core.domain.ChatRole
import dev.weft.undercurrent.core.domain.usecase.chat.DeleteCurrentConversationUseCase
import dev.weft.undercurrent.core.domain.usecase.chat.ObserveChatStateUseCase
import dev.weft.undercurrent.core.domain.usecase.chat.SelectAgentUseCase
import dev.weft.undercurrent.core.domain.usecase.chat.SelectConversationUseCase
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.feature.chat.components.DisplayMessage
import dev.weft.undercurrent.shared.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

class ChatViewModel(
    private val repo: ChatRepository,
    private val selectConversation: SelectConversationUseCase,
    private val deleteCurrentConversation: DeleteCurrentConversationUseCase,
    private val selectAgent: SelectAgentUseCase,
    observeChatState: ObserveChatStateUseCase,
    initialSkills: List<SkillSummary> = emptyList(),
) : MviViewModel<ChatState, ChatIntent, ChatEffect>(
    initialState = ChatState.initial(),
) {

    val displayMessages: SnapshotStateList<DisplayMessage> = mutableStateListOf()

    var skills: List<SkillSummary> = initialSkills
        private set

    private var streamingTurn: Job? = null
    private var streamingMessageId: Long? = null

    init {
        observeChatState().collectInto { snap ->
            if (
                currentConversationId == snap.currentConversationId &&
                agentReady == snap.isReady &&
                activeAgentName == snap.activeAgentName &&
                availableAgents == snap.availableAgents
            ) this
            else copy(
                currentConversationId = snap.currentConversationId,
                agentReady = snap.isReady,
                activeAgentName = snap.activeAgentName,
                availableAgents = snap.availableAgents,
            )
        }
    }

    fun resume() {
        launch { repo.resume() }
    }

    fun clear() {
        displayMessages.clear()
        update { it.copy(inFlight = false, lastError = null) }
    }

    fun reload(conversationId: String? = null) {
        val id = conversationId ?: current.currentConversationId ?: return
        launch {
            val msgs = repo.loadMessages(id)
            reseedDisplayMessages(msgs)
        }
    }

    suspend fun send(text: String, modelTier: ModelTier? = null) {
        runStreamingTurn(text, repo.send(text, modelTier))
    }

    suspend fun sendUiEvent(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    ) {
        displayMessages += DisplayMessage.event(action, sourceLabel, fieldValues)
        repo.sendUiEvent(action, sourceLabel, fieldValues)
    }

    override fun dispatch(intent: ChatIntent) = launch {
        when (intent) {
            is ChatIntent.SendChat ->
                runStreamingTurn(intent.text, repo.send(intent.text, intent.modelTier))
            ChatIntent.RegenerateLast ->
                runStreamingTurn(userTextForRegenerate = null, repo.regenerateLast())
            ChatIntent.StopResponse -> {
                repo.cancelCurrentTurn()
                streamingTurn?.cancel()
                streamingTurn = null
                streamingMessageId = null
                if (current.inFlight) update { it.copy(inFlight = false) }
            }
            ChatIntent.NewChat -> {
                repo.newChat()
                displayMessages.clear()
                update { it.copy(inFlight = false, lastError = null) }
            }
            is ChatIntent.SelectConversation -> {
                if (current.currentConversationId == intent.id) return@launch
                val msgs = selectConversation(intent.id)
                reseedDisplayMessages(msgs)
                update { it.copy(inFlight = false, lastError = null) }
            }
            ChatIntent.DeleteCurrentConversation -> {
                val deleted = deleteCurrentConversation()
                if (deleted) {
                    displayMessages.clear()
                    update { it.copy(inFlight = false, lastError = null) }
                }
            }
            is ChatIntent.DeleteConversation -> {
                val wasActive = current.currentConversationId == intent.id
                repo.deleteConversation(intent.id)
                if (wasActive) {
                    displayMessages.clear()
                    update { it.copy(inFlight = false, lastError = null) }
                }
            }
            is ChatIntent.SelectAgent -> {
                val msgs = selectAgent(intent.name)
                if (msgs.isNotEmpty()) reseedDisplayMessages(msgs)
            }
        }
    }

    private suspend fun runStreamingTurn(
        userTextForRegenerate: String?,
        stream: Flow<ChatChunk>,
    ) {
        if (current.inFlight) return
        val prior = streamingTurn
        if (prior != null && prior.isActive) return

        if (userTextForRegenerate != null) {
            val trimmed = userTextForRegenerate.trim()
            if (trimmed.isBlank()) return
            displayMessages += DisplayMessage.user(trimmed)
        }

        streamingMessageId = null
        update { it.copy(inFlight = true, lastError = null) }

        streamingTurn = launch {
            stream.collect { chunk -> foldChunk(chunk) }
            streamingMessageId = null
            if (current.inFlight) {
                update { it.copy(inFlight = false) }
            }
            // Turn ended without an explicit Done — drop any lingering
            // action so the indicator never narrates a stale step.
            if (current.currentAction != null) {
                update { it.copy(currentAction = null) }
            }
        }
        streamingTurn?.join()
    }

    private fun foldChunk(chunk: ChatChunk) {
        update { it.copy(currentAction = nextCurrentAction(it.currentAction, chunk)) }
        when (chunk) {
            is ChatChunk.TextDelta -> {
                val id = streamingMessageId
                if (id == null) {
                    val msg = DisplayMessage.assistant(
                        text = chunk.text,
                        agentName = current.activeAgentName,
                    )
                    streamingMessageId = msg.id
                    displayMessages += msg
                } else {
                    val idx = displayMessages.indexOfLast { it.id == id }
                    if (idx >= 0) {
                        val existing = displayMessages[idx]
                        displayMessages[idx] = existing.copy(text = existing.text + chunk.text)
                    }
                }
            }
            is ChatChunk.ToolStart ->
                displayMessages += DisplayMessage.toolStart(chunk.toolName)
            is ChatChunk.ToolDone ->
                displayMessages += DisplayMessage.toolDone(chunk.toolName)
            is ChatChunk.ToolFail -> {
                val payload = chunk.permissionDialog
                if (payload != null) {
                    displayMessages += DisplayMessage.toolFail(
                        chunk.toolName,
                        "Needs permission — see dialog.",
                    )
                    emit(ChatEffect.PermissionNeeded(payload))
                } else {
                    displayMessages += DisplayMessage.toolFail(chunk.toolName, chunk.message)
                }
            }
            ChatChunk.Done -> {
                streamingMessageId = null
                update { it.copy(inFlight = false) }
            }
            is ChatChunk.Error -> {
                update { it.copy(inFlight = false, lastError = chunk.message) }
                emit(ChatEffect.Error(chunk.message))
            }
        }
    }

    private fun reseedDisplayMessages(msgs: List<ChatMessage>) {
        displayMessages.clear()
        for (m in msgs) {
            displayMessages += m.toDisplay()
        }
    }

    private fun ChatMessage.toDisplay(): DisplayMessage = when (role) {
        ChatRole.USER -> DisplayMessage.user(content)
        ChatRole.ASSISTANT -> DisplayMessage.assistant(
            text = content,
            agentName = agentName,
        )
    }
}
