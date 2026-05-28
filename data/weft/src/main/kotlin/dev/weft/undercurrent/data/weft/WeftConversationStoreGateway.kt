package dev.weft.undercurrent.data.weft

import dev.weft.harness.conversation.ConversationStore
import dev.weft.undercurrent.shared.gateway.ConversationStoreGateway
import dev.weft.undercurrent.shared.gateway.ConversationSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dev.weft.harness.conversation.ConversationSummary as WeftConversationSummary

/**
 * Android impl of [ConversationStoreGateway] backed by Weft's
 * [ConversationStore]. Maps Weft summaries to the commonMain mirror so
 * feature modules stay KMP-clean.
 */
public class WeftConversationStoreGateway(
    private val store: ConversationStore,
) : ConversationStoreGateway {

    override fun search(query: String): Flow<List<ConversationSummary>> =
        store.search(query).map { list -> list.map { it.toCommon() } }

    override suspend fun deleteConversation(id: String) {
        store.deleteConversation(id)
    }

    override suspend fun clearAll() {
        store.clearAll()
    }

    private fun WeftConversationSummary.toCommon(): ConversationSummary = ConversationSummary(
        id = id,
        title = title,
        createdAtMs = createdAtMs,
        lastMessageAtMs = lastMessageAtMs,
    )
}
