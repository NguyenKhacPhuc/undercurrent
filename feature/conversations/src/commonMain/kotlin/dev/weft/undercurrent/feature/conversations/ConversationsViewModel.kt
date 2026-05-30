package dev.weft.undercurrent.feature.conversations

import dev.weft.undercurrent.core.domain.ConversationStoreRepository
import dev.weft.undercurrent.core.domain.ConversationSummary
import dev.weft.undercurrent.shared.mvi.MviViewModel
import kotlinx.coroutines.Job

data class ConversationsState(
    val query: String = "",
    val conversations: List<ConversationSummary> = emptyList(),
)

sealed interface ConversationsIntent {
    data class SetQuery(val query: String) : ConversationsIntent
    data class Delete(val id: String) : ConversationsIntent
    data object ClearAll : ConversationsIntent
}

sealed interface ConversationsEffect

/**
 * Search is query-shaped — the user types into a search bar and the
 * conversations list re-filters. We don't want a separate Flow per
 * query lifetime, so the store holds a single subscription Job and
 * restarts it whenever [ConversationsIntent.SetQuery] arrives.
 */
class ConversationsViewModel(
    private val store: ConversationStoreRepository,
) : MviViewModel<ConversationsState, ConversationsIntent, ConversationsEffect>(
    initialState = ConversationsState(),
) {
    private var searchJob: Job? = null

    init {
        // Initial subscription with empty-string query (matches everything).
        resubscribe("")
    }

    override fun dispatch(intent: ConversationsIntent) = launch {
        when (intent) {
            is ConversationsIntent.SetQuery -> {
                update { it.copy(query = intent.query) }
                resubscribe(intent.query)
            }
            is ConversationsIntent.Delete -> store.deleteConversation(intent.id)
            ConversationsIntent.ClearAll -> store.clearAll()
        }
    }

    private fun resubscribe(query: String) {
        searchJob?.cancel()
        searchJob = store.search(query).collectInto { copy(conversations = it) }
    }
}
