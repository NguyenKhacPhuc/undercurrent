package dev.weft.undercurrent.feature.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.shared.gateway.ConversationStoreGateway
import dev.weft.undercurrent.shared.gateway.ConversationSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Screen-scoped state for [ConversationsListScreen] and for the side
 * drawer's recent-threads list.
 *
 * Exposes a query-shaped flow accessor so the screen can swap the search
 * string and re-subscribe; the resulting [Flow] of [ConversationSummary]
 * is collected directly in the composable (vs. cached as a StateFlow
 * here) because the search query is screen-local UI state.
 *
 * Selection + new-chat still flow through the root app store (those
 * flip the active agent and navigate). Delete + clear-all are purely
 * store mutations, so they live here.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/conversations/ConversationsViewModel.kt`. Now
 * consumes [ConversationStoreGateway] from `:shared` (was Weft's
 * `ConversationStore` directly).
 */
public class ConversationsViewModel(
    private val store: ConversationStoreGateway,
) : ViewModel() {

    /** Re-subscribed each time the caller changes the [query]. */
    public fun search(query: String): Flow<List<ConversationSummary>> = store.search(query)

    public fun delete(id: String) {
        viewModelScope.launch { store.deleteConversation(id) }
    }

    public fun clearAll() {
        viewModelScope.launch { store.clearAll() }
    }
}
