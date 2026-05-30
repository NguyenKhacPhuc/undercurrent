package dev.weft.undercurrent.core.domain.usecase.chat

import dev.weft.undercurrent.core.domain.ChatRepository

/**
 * Delete a conversation by id. Consumers can check whether the
 * deleted id matched [ChatRepository.currentConversationId] before
 * calling to decide if they need to clear UI state.
 */
class DeleteConversationUseCase(
    private val repo: ChatRepository,
) {
    suspend operator fun invoke(id: String) = repo.deleteConversation(id)
}
