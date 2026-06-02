package dev.weft.undercurrent.core.domain

import dev.weft.harness.conversation.ConversationStore
import dev.weft.undercurrent.core.domain.ConversationStoreRepository
import dev.weft.undercurrent.core.domain.ConversationSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dev.weft.harness.conversation.ConversationSummary as WeftConversationSummary

/**
 * Android impl of [ConversationStoreRepository] backed by Weft's
 * [ConversationStore]. Maps Weft summaries to the commonMain mirror so
 * feature modules stay KMP-clean.
 */
class WeftConversationStoreRepository(
    private val store: ConversationStore,
) : ConversationStoreRepository {

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
