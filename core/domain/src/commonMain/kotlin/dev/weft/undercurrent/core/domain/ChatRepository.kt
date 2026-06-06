package dev.weft.undercurrent.core.domain

import dev.weft.undercurrent.core.model.ModelTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {

    val currentConversationId: StateFlow<String?>

    val isReady: StateFlow<Boolean>

    val activeAgentName: StateFlow<String>

    val availableAgents: StateFlow<List<AgentSummary>>

    fun send(text: String, modelTier: ModelTier? = null): Flow<ChatChunk>

    fun regenerateLast(): Flow<ChatChunk>

    /** Cancel the in-flight agent turn, if any. */
    suspend fun cancelCurrentTurn(): Unit = Unit

    suspend fun resume()

    suspend fun newChat()

    suspend fun selectConversation(id: String)

    suspend fun deleteConversation(id: String)

    suspend fun selectAgent(name: String)

    suspend fun loadMessages(conversationId: String): List<ChatMessage>

    suspend fun sendUiEvent(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    ): Unit = Unit
}
