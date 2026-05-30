package dev.weft.undercurrent.core.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS stub. Memory persistence ships with the Android build; the iOS
 * shell exposes an empty list and no-op mutations.
 */
class StubMemoryStoreRepository : MemoryStoreRepository {
    override val memories: StateFlow<List<MemoryEntry>> =
        MutableStateFlow<List<MemoryEntry>>(emptyList()).asStateFlow()

    override suspend fun delete(id: String) = Unit

    override suspend fun clear() = Unit
}
