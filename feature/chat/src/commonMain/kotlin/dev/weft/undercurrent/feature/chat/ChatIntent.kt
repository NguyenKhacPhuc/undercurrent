package dev.weft.undercurrent.feature.chat

import dev.weft.undercurrent.core.model.ModelTier

/**
 * Chat-surface intents — sending messages, regenerating, switching
 * conversations / agents, deleting threads. Routed by the root VM
 * to the chat slice.
 *
 * `SubmitKey` deliberately lives in `:feature:providers` ([dev.weft
 * .undercurrent.feature.providers.ProviderIntent]) — saving a key is
 * a provider concern, even if KeyPaste happens to drop the user into
 * chat after.
 */
sealed interface ChatIntent {

    /**
     * Send a chat message. Routes through skill resolution first;
     * non-skill input falls through to the agent's streaming flow.
     */
    data class SendChat(
        val text: String,
        val modelTier: ModelTier? = null,
    ) : ChatIntent

    /** "Ask again" — re-send the last user message. */
    data object RegenerateLast : ChatIntent

    data object NewChat : ChatIntent

    data class SelectConversation(val id: String) : ChatIntent

    data object DeleteCurrentConversation : ChatIntent

    data class DeleteConversation(val id: String) : ChatIntent

    /** Switch the active agent. */
    data class SelectAgent(val name: String) : ChatIntent
}
