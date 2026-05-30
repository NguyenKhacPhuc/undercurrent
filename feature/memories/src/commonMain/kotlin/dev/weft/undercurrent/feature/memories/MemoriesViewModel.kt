package dev.weft.undercurrent.feature.memories

import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.shared.gateway.MemoryEntry
import dev.weft.undercurrent.shared.gateway.MemoryStoreGateway
import dev.weft.undercurrent.shared.mvi.MviViewModel
import kotlinx.coroutines.launch

data class MemoriesState(val memories: List<MemoryEntry> = emptyList())

sealed interface MemoriesIntent {
    data class Delete(val id: String) : MemoriesIntent
    data object ClearAll : MemoriesIntent
}

sealed interface MemoriesEffect

class MemoriesViewModel(
    private val store: MemoryStoreGateway,
) : MviViewModel<MemoriesState, MemoriesIntent, MemoriesEffect>(
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
