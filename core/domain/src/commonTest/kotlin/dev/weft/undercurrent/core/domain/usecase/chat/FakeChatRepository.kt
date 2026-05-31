package dev.weft.undercurrent.core.domain.usecase.chat

import dev.weft.undercurrent.core.domain.AgentSummary
import dev.weft.undercurrent.core.domain.ChatChunk
import dev.weft.undercurrent.core.domain.ChatMessage
import dev.weft.undercurrent.core.domain.ChatRepository
import dev.weft.undercurrent.core.model.ModelTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

class FakeChatRepository(
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
