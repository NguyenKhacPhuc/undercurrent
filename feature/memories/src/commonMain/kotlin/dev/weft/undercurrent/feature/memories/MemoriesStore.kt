package dev.weft.undercurrent.feature.memories

import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.shared.gateway.MemoryEntry
import dev.weft.undercurrent.shared.gateway.MemoryStoreGateway
import dev.weft.undercurrent.shared.mvi.Store
import kotlinx.coroutines.launch

public data class MemoriesState(public val memories: List<MemoryEntry> = emptyList())

public sealed interface MemoriesIntent {
    public data class Delete(public val id: String) : MemoriesIntent
    public data object ClearAll : MemoriesIntent
}

public sealed interface MemoriesEffect

public class MemoriesStore(
    private val store: MemoryStoreGateway,
) : Store<MemoriesState, MemoriesIntent, MemoriesEffect>(
    initialState = MemoriesState(memories = store.memories.value),
) {
    init {
        viewModelScope.launch {
            store.memories.collect { ms -> update { it.copy(memories = ms) } }
        }
    }

    override fun dispatch(intent: MemoriesIntent) {
        when (intent) {
            is MemoriesIntent.Delete -> viewModelScope.launch { store.delete(intent.id) }
            MemoriesIntent.ClearAll -> viewModelScope.launch { store.clear() }
        }
    }
}
