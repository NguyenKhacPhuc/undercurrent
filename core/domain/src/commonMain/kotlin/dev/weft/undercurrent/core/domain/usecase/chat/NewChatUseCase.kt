package dev.weft.undercurrent.core.domain.usecase.chat

import dev.weft.undercurrent.core.domain.ChatRepository

/**
 * Start a fresh conversation. The repository updates its
 * [ChatRepository.currentConversationId] StateFlow as a side effect
 * — consumers observing that flow update their state accordingly.
 */
class NewChatUseCase(
    private val repo: ChatRepository,
) {
    suspend operator fun invoke() = repo.newChat()
}
