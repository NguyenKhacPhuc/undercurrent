package dev.weft.undercurrent.feature.conversations

import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.shared.gateway.ConversationStoreGateway
import dev.weft.undercurrent.shared.gateway.ConversationSummary
import dev.weft.undercurrent.shared.mvi.Store
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

public data class ConversationsState(
    public val query: String = "",
    public val conversations: List<ConversationSummary> = emptyList(),
)

public sealed interface ConversationsIntent {
    public data class SetQuery(public val query: String) : ConversationsIntent
    public data class Delete(public val id: String) : ConversationsIntent
    public data object ClearAll : ConversationsIntent
}

public sealed interface ConversationsEffect

/**
 * Search is query-shaped — the user types into a search bar and the
 * conversations list re-filters. We don't want a separate Flow per
 * query lifetime, so the store holds a single subscription Job and
 * restarts it whenever [ConversationsIntent.SetQuery] arrives.
 */
public class ConversationsStore(
    private val store: ConversationStoreGateway,
) : Store<ConversationsState, ConversationsIntent, ConversationsEffect>(
    initialState = ConversationsState(),
) {
    private var searchJob: Job? = null

    init {
        // Initial subscription with empty-string query (matches everything).
        resubscribe("")
    }

    override fun dispatch(intent: ConversationsIntent) {
        when (intent) {
            is ConversationsIntent.SetQuery -> {
                update { it.copy(query = intent.query) }
                resubscribe(intent.query)
            }
            is ConversationsIntent.Delete -> viewModelScope.launch {
                store.deleteConversation(intent.id)
            }
            ConversationsIntent.ClearAll -> viewModelScope.launch { store.clearAll() }
        }
    }

    private fun resubscribe(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            store.search(query).collect { list ->
                update { it.copy(conversations = list) }
            }
        }
    }
}
