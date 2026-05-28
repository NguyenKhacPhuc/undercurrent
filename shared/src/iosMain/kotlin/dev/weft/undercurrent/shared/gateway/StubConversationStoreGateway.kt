package dev.weft.undercurrent.shared.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * iOS stub. Conversations persist via Weft's SQLDelight store, which is
 * Android-only for v1. The list always reads as empty; deletes no-op.
 */
public class StubConversationStoreGateway : ConversationStoreGateway {
    override fun search(query: String): Flow<List<ConversationSummary>> = flowOf(emptyList())

    override suspend fun deleteConversation(id: String) = Unit

    override suspend fun clearAll() = Unit
}
