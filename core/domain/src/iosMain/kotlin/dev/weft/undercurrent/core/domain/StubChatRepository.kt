package dev.weft.undercurrent.core.domain

import dev.weft.undercurrent.core.model.ModelTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS placeholder ChatRepository — keeps the host compiling and
 * routes the chat screen back to KeyPaste via [isReady] = false.
 * Replace when iOS gets the substrate (WeftAgent) end-to-end; until
 * then the iOS flow ends at KeyPaste.
 */
class StubChatRepository : ChatRepository {

    override val currentConversationId: StateFlow<String?> = MutableStateFlow(null)
    override val isReady: StateFlow<Boolean> = MutableStateFlow(false)
    override val activeAgentName: StateFlow<String> = MutableStateFlow("default")
    override val availableAgents: StateFlow<List<AgentSummary>> = MutableStateFlow(emptyList())

    override fun send(text: String, modelTier: ModelTier?): Flow<ChatChunk> = emptyFlow()
    override fun regenerateLast(): Flow<ChatChunk> = emptyFlow()

    override suspend fun resume() = Unit
    override suspend fun newChat() = Unit
    override suspend fun selectConversation(id: String) = Unit
    override suspend fun deleteConversation(id: String) = Unit
    override suspend fun selectAgent(name: String) = Unit
    override suspend fun loadMessages(conversationId: String): List<ChatMessage> = emptyList()
}
