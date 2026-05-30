package dev.weft.undercurrent.core.domain.usecase.chat

import dev.weft.undercurrent.core.domain.ChatMessage
import dev.weft.undercurrent.core.domain.ChatRepository

/**
 * Switch the active agent + reload the conversation. The repository's
 * `selectAgent` rebuilds the substrate agent under the hood (model
 * pool changes, system prompt changes); we follow with a
 * `loadMessages` to seed the chat surface with the same conversation
 * under the new agent's eyes.
 *
 * Returns the seeded messages, or empty list if there's no active
 * conversation (post-boot, pre-first-send state).
 */
class SelectAgentUseCase(
    private val repo: ChatRepository,
) {
    suspend operator fun invoke(name: String): List<ChatMessage> {
        val state = repo
        if (name == state.activeAgentName.value) return emptyList()
        if (state.availableAgents.value.none { it.name == name }) return emptyList()
        repo.selectAgent(name)
        val convId = repo.currentConversationId.value ?: return emptyList()
        return repo.loadMessages(convId)
    }
}
