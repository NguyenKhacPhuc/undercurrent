package dev.weft.undercurrent.core.domain.usecase.chat

import dev.weft.undercurrent.core.domain.ChatRepository

/**
 * Delete whichever conversation is currently active. Compound —
 * reads [ChatRepository.currentConversationId] then dispatches
 * delete. Returns whether anything was deleted (false when no
 * active conversation exists yet, e.g. immediately after boot
 * before the first send).
 */
class DeleteCurrentConversationUseCase(
    private val repo: ChatRepository,
) {
    suspend operator fun invoke(): Boolean {
        val id = repo.currentConversationId.value ?: return false
        repo.deleteConversation(id)
        return true
    }
}
