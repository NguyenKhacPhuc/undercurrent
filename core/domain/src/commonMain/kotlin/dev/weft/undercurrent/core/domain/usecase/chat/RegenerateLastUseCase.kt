package dev.weft.undercurrent.core.domain.usecase.chat

import dev.weft.undercurrent.core.domain.ChatChunk
import dev.weft.undercurrent.core.domain.ChatRepository
import kotlinx.coroutines.flow.Flow

/**
 * "Ask again" — resend the last user turn, replacing the trailing
 * assistant reply with a fresh stream.
 */
class RegenerateLastUseCase(
    private val repo: ChatRepository,
) {
    operator fun invoke(): Flow<ChatChunk> = repo.regenerateLast()
}
