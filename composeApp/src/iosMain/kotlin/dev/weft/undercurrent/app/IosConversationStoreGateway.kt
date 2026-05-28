package dev.weft.undercurrent.app

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.weft.undercurrent.db.Conversations
import dev.weft.undercurrent.db.UndercurrentDatabase
import dev.weft.undercurrent.shared.gateway.ConversationStoreGateway
import dev.weft.undercurrent.shared.gateway.ConversationSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * iOS impl of [ConversationStoreGateway] backed by the
 * `UndercurrentDatabase`'s `conversations` + `messages` tables.
 * Reactive — the conversations list re-emits whenever IosAppStore
 * writes via `insertConversation` / `touchConversation` / etc.
 *
 * The `search(query)` filter is a case-insensitive substring match
 * against the title in Kotlin (after pulling the full list). For
 * personal-app dataset sizes (dozens of threads max) this is fine;
 * for thousands we'd push it into SQLite with a LIKE clause.
 */
internal class IosConversationStoreGateway(
    private val db: UndercurrentDatabase,
) : ConversationStoreGateway {

    override fun search(query: String): Flow<List<ConversationSummary>> =
        db.conversationsQueries
            .listConversations()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                val q = query.trim()
                rows.filter { it.matchesQuery(q) }.map { it.toCommon() }
            }

    override suspend fun deleteConversation(id: String) {
        withContext(Dispatchers.Default) {
            db.transaction {
                db.conversationsQueries.deleteMessagesForConversation(id)
                db.conversationsQueries.deleteConversation(id)
            }
        }
    }

    override suspend fun clearAll() {
        withContext(Dispatchers.Default) {
            db.transaction {
                db.conversationsQueries.clearAllMessages()
                db.conversationsQueries.clearAllConversations()
            }
        }
    }
}

private fun Conversations.matchesQuery(q: String): Boolean =
    q.isEmpty() || title.contains(q, ignoreCase = true)

private fun Conversations.toCommon(): ConversationSummary = ConversationSummary(
    id = id,
    title = title,
    createdAtMs = created_at_ms,
    lastMessageAtMs = last_message_at_ms,
)
