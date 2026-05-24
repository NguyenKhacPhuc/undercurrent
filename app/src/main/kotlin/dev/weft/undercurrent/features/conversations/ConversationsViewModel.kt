package dev.weft.undercurrent.features.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.weft.android.WeftRuntime
import dev.weft.harness.conversation.ConversationStore
import dev.weft.harness.conversation.ConversationSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Screen-scoped state for [dev.weft.undercurrent.features.conversations.ConversationsListScreen]
 * and for the side drawer's recent-threads list.
 *
 * Exposes a query-shaped flow accessor so the screen can swap the search
 * string and re-subscribe; the resulting [Flow] of [ConversationSummary]
 * is collected directly in the composable (vs. cached as a StateFlow
 * here) because the search query is screen-local UI state, not a global
 * concern.
 *
 * Selection + new-chat still go through the root [dev.weft.undercurrent.core.AppStore]
 * — those flip the active agent and navigate, which is the store's job.
 * Delete + clear-all are purely store mutations, so they live here.
 */
internal class ConversationsViewModel(
    runtime: WeftRuntime,
) : ViewModel() {
    private val store: ConversationStore = runtime.conversationStore

    /** Re-subscribed each time the caller changes the [query]. */
    fun search(query: String): Flow<List<ConversationSummary>> = store.search(query)

    fun delete(id: String) {
        viewModelScope.launch { store.deleteConversation(id) }
    }

    fun clearAll() {
        viewModelScope.launch { store.clearAll() }
    }
}
