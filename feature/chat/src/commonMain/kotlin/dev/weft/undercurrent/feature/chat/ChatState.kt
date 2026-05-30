package dev.weft.undercurrent.feature.chat

import dev.weft.undercurrent.core.domain.AgentSummary

/**
 * Chat-feature state slice. Owned by [ChatViewModel] — was previously
 * a `chat` field on the root `AppState`.
 *
 * Includes both the per-turn status ([inFlight] + [lastError]) and
 * the conversation context ([currentConversationId],
 * [activeAgentName], [availableAgents]) that the chat surface
 * renders + dispatches against.
 *
 * `agentReady` lives here (not in NavigationViewModel) because the
 * "can the user send" question is fundamentally a chat concern — the
 * chat route gates on it before showing the input.
 */
data class ChatState(
    val inFlight: Boolean = false,
    val lastError: String? = null,
    /**
     * Active conversation id, mirrored from
     * [dev.weft.undercurrent.core.domain.ChatRepository
     * .currentConversationId]. Drives the drawer's "currently
     * active" highlight and the ConversationsList selection. Null
     * while the repository hasn't booted.
     */
    val currentConversationId: String? = null,
    /**
     * Whether the chat repository has a built agent / configured LLM
     * client ready. Chat surface gates send on this; ScreenRouter
     * redirects to KeyPaste when false.
     */
    val agentReady: Boolean = false,
    val activeAgentName: String = DEFAULT_AGENT_NAME,
    /**
     * User-addressable agents the host registered. Empty / one-
     * element lists hide the chat agent selector.
     */
    val availableAgents: List<AgentSummary> = emptyList(),
) {
    companion object {
        const val DEFAULT_AGENT_NAME: String = "default"

        fun initial(): ChatState = ChatState()
    }
}
