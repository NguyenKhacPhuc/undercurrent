package dev.weft.undercurrent.features.memories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.weft.android.WeftRuntime
import dev.weft.harness.memory.MemoryEntry
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Screen-scoped state for [dev.weft.undercurrent.features.memories.AgentMemoriesScreen].
 * Owns the memory-store dependency + the two destructive actions
 * (delete-one, clear-all) so the screen doesn't have to
 * `rememberCoroutineScope` for them.
 *
 * The memory store itself is a runtime singleton — both this VM and the
 * agent's tools (`memory_save`, `memory_search`) write to the same
 * underlying store, so deletes show up to the agent immediately.
 */
internal class MemoriesViewModel(
    runtime: WeftRuntime,
) : ViewModel() {
    private val store = runtime.memoryStore

    val memories: StateFlow<List<MemoryEntry>> = store.memories

    fun delete(id: String) {
        viewModelScope.launch { store.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { store.clear() }
    }
}
