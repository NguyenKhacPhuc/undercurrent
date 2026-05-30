package dev.weft.undercurrent.core.domain.usecase.chat

import dev.weft.undercurrent.core.domain.ChatRepository

/**
 * Forward a user-initiated `ui_render` event (button tap, form
 * submit) from the rendered-tree screen back to the agent. iOS
 * impl no-ops until iOS gains the substrate.
 *
 * Lives in the chat slice (not its own ui-bridge slice) because
 * the event is routed through the agent — same underlying
 * communication channel as a SendChat.
 */
class SendUiEventUseCase(
    private val repo: ChatRepository,
) {
    suspend operator fun invoke(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    ) = repo.sendUiEvent(action, sourceLabel, fieldValues)
}
