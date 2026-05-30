package dev.weft.undercurrent.core.domain.usecase.chat

import dev.weft.undercurrent.core.domain.AgentSummary
import dev.weft.undercurrent.core.domain.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Observe the chat layer's reactive surface as a single composite
 * snapshot. Folds the repository's four [ChatRepository]
 * StateFlows ([currentConversationId], [isReady], [activeAgentName],
 * [availableAgents]) into one [ChatStateSnapshot] the VM collects
 * once + projects into its [ChatState].
 *
 * Saves the VM from juggling four `viewModelScope.launch` collectors
 * — one collect, one update path.
 */
class ObserveChatStateUseCase(
    private val repo: ChatRepository,
) {
    operator fun invoke(): Flow<ChatStateSnapshot> = combine(
        repo.currentConversationId,
        repo.isReady,
        repo.activeAgentName,
        repo.availableAgents,
    ) { conversationId, ready, agentName, agents ->
        ChatStateSnapshot(
            currentConversationId = conversationId,
            isReady = ready,
            activeAgentName = agentName,
            availableAgents = agents,
        )
    }
}

/**
 * Read-model emitted by [ObserveChatStateUseCase]. Composite of the
 * Repository's StateFlows so the ViewModel collects once.
 */
data class ChatStateSnapshot(
    val currentConversationId: String?,
    val isReady: Boolean,
    val activeAgentName: String,
    val availableAgents: List<AgentSummary>,
)
