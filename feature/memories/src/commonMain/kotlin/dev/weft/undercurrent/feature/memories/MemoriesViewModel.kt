package dev.weft.undercurrent.feature.memories

import dev.weft.undercurrent.core.domain.MemoryEntry
import dev.weft.undercurrent.core.domain.MemoryStoreRepository
import dev.weft.undercurrent.shared.mvi.MviViewModel

data class MemoriesState(val memories: List<MemoryEntry> = emptyList())

sealed interface MemoriesIntent {
    data class Delete(val id: String) : MemoriesIntent
    data object ClearAll : MemoriesIntent
}

sealed interface MemoriesEffect

class MemoriesViewModel(
    private val store: MemoryStoreRepository,
) : MviViewModel<MemoriesState, MemoriesIntent, MemoriesEffect>(
    initialState = MemoriesState(memories = store.memories.value),
) {
    init {
        store.memories.collectInto { copy(memories = it) }
    }

    override fun dispatch(intent: MemoriesIntent) = launch {
        when (intent) {
            is MemoriesIntent.Delete -> store.delete(intent.id)
            MemoriesIntent.ClearAll -> store.clear()
        }
    }
}
