package dev.weft.undercurrent.core.domain.usecase.chat

import dev.weft.undercurrent.core.domain.ChatMessage
import dev.weft.undercurrent.core.domain.ChatRepository

/**
 * Load the persisted history for a conversation. Standalone UseCase
 * (separate from [SelectConversationUseCase]) because callers
 * sometimes need to reload the current conversation's history
 * without re-issuing a `selectConversation` write — e.g. after a
 * SelectAgent swap.
 */
class LoadMessagesUseCase(
    private val repo: ChatRepository,
) {
    suspend operator fun invoke(conversationId: String): List<ChatMessage> =
        repo.loadMessages(conversationId)
}
