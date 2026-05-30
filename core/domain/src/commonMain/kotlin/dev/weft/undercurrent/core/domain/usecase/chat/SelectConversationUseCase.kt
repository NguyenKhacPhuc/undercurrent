package dev.weft.undercurrent.core.domain.usecase.chat

import dev.weft.undercurrent.core.domain.ChatMessage
import dev.weft.undercurrent.core.domain.ChatRepository

/**
 * Switch to an existing conversation and load its history. Compound
 * operation — combines the repository's `selectConversation` write
 * with the `loadMessages` read so callers (ChatViewModel,
 * ConversationsViewModel) get the history in one suspend call.
 *
 * Idempotent if the requested id already matches the current
 * conversation — the repo's selectConversation is a no-op there and
 * we still return the messages.
 */
class SelectConversationUseCase(
    private val repo: ChatRepository,
) {
    suspend operator fun invoke(id: String): List<ChatMessage> {
        if (repo.currentConversationId.value != id) {
            repo.selectConversation(id)
        }
        return repo.loadMessages(id)
    }
}
