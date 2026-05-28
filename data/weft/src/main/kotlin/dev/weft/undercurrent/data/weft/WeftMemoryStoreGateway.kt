package dev.weft.undercurrent.data.weft

import dev.weft.harness.memory.MemoryStore
import dev.weft.undercurrent.shared.gateway.MemoryEntry
import dev.weft.undercurrent.shared.gateway.MemoryScope
import dev.weft.undercurrent.shared.gateway.MemoryStoreGateway
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import dev.weft.harness.memory.MemoryEntry as WeftMemoryEntry
import dev.weft.harness.memory.MemoryScope as WeftMemoryScope

/**
 * Android impl of [MemoryStoreGateway] backed by Weft's [MemoryStore].
 * Maps Weft entries to the commonMain mirror; the agent loop continues
 * to write through the underlying Weft store via the `memory_store`
 * tool, so deletes propagate immediately.
 */
class WeftMemoryStoreGateway(
    private val store: MemoryStore,
) : MemoryStoreGateway {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val memories: StateFlow<List<MemoryEntry>> = store.memories
        .map { list -> list.map { it.toCommon() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun delete(id: String) {
        store.delete(id)
    }

    override suspend fun clear() {
        store.clear()
    }

    private fun WeftMemoryEntry.toCommon(): MemoryEntry = MemoryEntry(
        id = id,
        content = content,
        tags = tags,
        scope = scope.toCommon(),
        storedAtEpochMs = storedAtEpochMs,
    )

    private fun WeftMemoryScope.toCommon(): MemoryScope = when (this) {
        WeftMemoryScope.SESSION -> MemoryScope.SESSION
        WeftMemoryScope.PERMANENT -> MemoryScope.PERMANENT
        WeftMemoryScope.ANY -> MemoryScope.ANY
    }
}
