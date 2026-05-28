package dev.weft.undercurrent.feature.memories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.shared.gateway.MemoryEntry
import dev.weft.undercurrent.shared.gateway.MemoryStoreGateway
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Screen-scoped state for [AgentMemoriesScreen]. Owns the gateway
 * dependency + the two destructive actions (delete-one, clear-all) so
 * the screen doesn't have to `rememberCoroutineScope` for them.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/memories/MemoriesViewModel.kt`. Now consumes
 * [MemoryStoreGateway] (was Weft's `MemoryStore` directly via
 * `WeftRuntime`).
 */
public class MemoriesViewModel(
    private val store: MemoryStoreGateway,
) : ViewModel() {

    public val memories: StateFlow<List<MemoryEntry>> = store.memories

    public fun delete(id: String) {
        viewModelScope.launch { store.delete(id) }
    }

    public fun clearAll() {
        viewModelScope.launch { store.clear() }
    }
}
