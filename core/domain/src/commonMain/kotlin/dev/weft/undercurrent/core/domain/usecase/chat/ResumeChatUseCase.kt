package dev.weft.undercurrent.core.domain.usecase.chat

import dev.weft.undercurrent.core.domain.ChatRepository

/**
 * Boot the chat layer — discover the active provider's key, build
 * the agent / configure the client, hydrate the most recent
 * conversation. Idempotent; safe to call on recomposition.
 *
 * The boot path itself is composed by an `AppBootUseCase` in the
 * dissolved-VM future (onboarding check → provider check → resume
 * chat); for now this is invoked directly by ChatViewModel on
 * first dispatch.
 */
class ResumeChatUseCase(
    private val repo: ChatRepository,
) {
    suspend operator fun invoke() = repo.resume()
}
