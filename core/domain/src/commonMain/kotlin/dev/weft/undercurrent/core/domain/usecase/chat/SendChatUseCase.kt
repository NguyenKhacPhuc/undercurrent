package dev.weft.undercurrent.core.domain.usecase.chat

import dev.weft.undercurrent.core.domain.ChatChunk
import dev.weft.undercurrent.core.domain.ChatRepository
import dev.weft.undercurrent.core.model.ModelTier
import kotlinx.coroutines.flow.Flow

/**
 * Send a chat turn. Thin delegation to [ChatRepository.send] — the
 * Repository's impl carries the streaming + skill-resolution work.
 *
 * Exists as a UseCase (not a direct repo call inside ChatViewModel)
 * because it's used from multiple presentation surfaces:
 *  - ChatViewModel — dispatch of [ChatIntent.SendChat].
 *  - CreatorViewModel — kickoff prompt when StartCreator fires.
 *  - MiniAppViewModel — trigger prompt when InvokeMiniApp fires.
 *
 * Same operation, three call sites. Injecting the UseCase into each
 * VM keeps the call site small + lets us swap behavior (e.g. add
 * rate-limit / logging) in one place.
 */
class SendChatUseCase(
    private val repo: ChatRepository,
) {
    operator fun invoke(text: String, modelTier: ModelTier? = null): Flow<ChatChunk> =
        repo.send(text, modelTier)
}
