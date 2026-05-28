package dev.weft.undercurrent.shared.gateway

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Read-and-delete surface for stored memories. Backed by Weft's
 * `MemoryStore` on Android; iOS stub holds an empty list.
 *
 * Memory writes happen inside the agent loop via Weft's `memory_store`
 * tool — feature code never writes here, so [putMemory] is intentionally
 * absent. The mirror types [MemoryEntry] / [MemoryScope] keep feature
 * modules clear of `dev.weft.harness.memory.*` imports.
 */
public interface MemoryStoreGateway {

    /** Snapshot of all stored memories, newest first. */
    public val memories: StateFlow<List<MemoryEntry>>

    public suspend fun delete(id: String)

    public suspend fun clear()
}

/** Mirror of `dev.weft.harness.memory.MemoryEntry`. */
@Serializable
public data class MemoryEntry(
    val id: String,
    val content: String,
    val tags: List<String>,
    val scope: MemoryScope,
    val storedAtEpochMs: Long,
)

/** Mirror of `dev.weft.harness.memory.MemoryScope`. */
@Serializable
public enum class MemoryScope { SESSION, PERMANENT, ANY }
