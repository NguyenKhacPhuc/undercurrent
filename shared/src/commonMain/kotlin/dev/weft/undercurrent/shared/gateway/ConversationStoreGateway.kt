package dev.weft.undercurrent.shared.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Read-only conversation list + delete operations. Backed by Weft's
 * `ConversationStore` on Android; iOS stub emits an empty list.
 *
 * The chat feature's history rebuild + append paths flow through
 * [dev.weft.undercurrent.shared.agent.AgentEngine] (via Weft directly on
 * Android) — this gateway is just the "manage threads" surface the
 * conversations list screen and side drawer consume.
 */
public interface ConversationStoreGateway {

    /**
     * Re-subscribable search across thread titles and message bodies.
     * Empty query returns every thread, newest first. Case-insensitive
     * substring match on ASCII.
     */
    public fun search(query: String): Flow<List<ConversationSummary>>

    /** Drop a single thread and all its messages. */
    public suspend fun deleteConversation(id: String)

    /** Wipe all threads. */
    public suspend fun clearAll()
}

/** Mirror of `dev.weft.harness.conversation.ConversationSummary`. */
@Serializable
public data class ConversationSummary(
    val id: String,
    val title: String,
    val createdAtMs: Long,
    val lastMessageAtMs: Long,
)
