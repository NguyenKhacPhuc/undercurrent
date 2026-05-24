package dev.weft.undercurrent.data

import dev.weft.contracts.DataSource
import dev.weft.contracts.QueryResult
import dev.weft.contracts.SortSpec
import dev.weft.contracts.UpsertResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * In-memory [DataSource] for demos and tests. Records are JsonObject; the
 * "id" field is the primary key. upsert auto-generates ids; query supports
 * `{field == value}` equality filters and `limit` only — full filter parsing
 * is the app's responsibility in production.
 *
 * Lives in the app (not the SDK) on purpose: the substrate ships the
 * [DataSource] contract and the `data_*` tools that consume it, but the
 * choice of *which* backend an app uses (in-memory, SQLite, Room, network)
 * is an app product decision. Undercurrent uses this in-memory variant
 * for demos; a production app would swap in a persistent impl that writes
 * to disk or a remote API.
 */
public class InMemoryDataSource(
    override val name: String,
    seed: List<JsonObject> = emptyList(),
) : DataSource {
    private val byId = mutableMapOf<String, JsonObject>()
    private var nextId = 1L

    private val _changes = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 8)
    override val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    init {
        seed.forEach { record ->
            val id = record["id"]?.jsonPrimitive?.content ?: nextId().toString()
            byId[id] = ensureId(record, id)
        }
    }

    override suspend fun query(
        filter: JsonObject,
        sort: List<SortSpec>,
        projection: List<String>,
        limit: Int,
    ): QueryResult {
        val matching = byId.values.filter { record ->
            filter.entries.all { (key, value) -> record[key] == value }
        }
        val items = matching.take(limit).toList()
        return QueryResult(items = items, total = matching.size, hasMore = matching.size > items.size)
    }

    override suspend fun upsert(record: JsonObject, idempotencyKey: String?): UpsertResult {
        val explicitId = record["id"]?.jsonPrimitive?.content
        val result = if (explicitId != null && byId.containsKey(explicitId)) {
            byId[explicitId] = ensureId(record, explicitId)
            UpsertResult(id = explicitId, created = false)
        } else {
            val id = explicitId ?: nextId().toString()
            byId[id] = ensureId(record, id)
            UpsertResult(id = id, created = true)
        }
        _changes.tryEmit(Unit)
        return result
    }

    override suspend fun delete(id: String): Boolean {
        val removed = byId.remove(id) != null
        if (removed) _changes.tryEmit(Unit)
        return removed
    }

    public fun snapshot(): Map<String, JsonObject> = byId.toMap()

    private fun nextId(): Long = nextId++

    private fun ensureId(record: JsonObject, id: String): JsonObject = buildJsonObject {
        put("id", JsonPrimitive(id))
        record.forEach { (k, v) -> if (k != "id") put(k, v) }
    }
}
