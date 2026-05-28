package dev.weft.undercurrent.data

import dev.weft.contracts.DataSource
import dev.weft.contracts.QueryResult
import dev.weft.contracts.SortOrder
import dev.weft.contracts.SortSpec
import dev.weft.contracts.UpsertResult
import dev.weft.undercurrent.db.UndercurrentDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

/**
 * Persistent [DataSource] backed by SQLDelight. Records live in the
 * `records` table (schema in `:data:sqldelight`'s `Records.sq`), one row
 * per record, payload encoded as a JSON object string.
 *
 * **Sources are name slots, not tables.** One [SqlDelightDataSource]
 * instance per source name (`notes`, `tasks`, …); the underlying
 * [UndercurrentDatabase] is shared. The `source` column on `records`
 * separates them so each instance only sees its own rows.
 *
 * **Filtering policy.** The contract's `filter` is JsonObject equality
 * across all keys. We pull all rows for the source (newest first) and
 * filter in Kotlin — fine for personal-app dataset sizes. For
 * collections growing past ~10k rows, push the filter into SQLite via
 * JSON1 (`json_extract`).
 */
internal class SqlDelightDataSource(
    override val name: String,
    private val database: UndercurrentDatabase,
    override val description: String = "",
) : DataSource {

    private val json = Json { ignoreUnknownKeys = true }

    private val _changes = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 8)
    override val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    override suspend fun query(
        filter: JsonObject,
        sort: List<SortSpec>,
        projection: List<String>,
        limit: Int,
    ): QueryResult = withContext(Dispatchers.IO) {
        val all = database.recordsQueries
            .selectBySource(name)
            .executeAsList()
            .map { row -> decodeRecord(row.id, row.payload_json) }

        val matching = all.filter { record ->
            filter.entries.all { (key, value) -> record[key] == value }
        }

        val sorted = if (sort.isEmpty()) matching else sortRecords(matching, sort)
        val items = sorted.take(limit)
        QueryResult(
            items = items,
            total = sorted.size,
            hasMore = sorted.size > items.size,
        )
    }

    override suspend fun upsert(
        record: JsonObject,
        idempotencyKey: String?,
    ): UpsertResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = idempotencyKey
            ?: record["id"]?.jsonPrimitive?.contentOrNull()
            ?: "rec.${UUID.randomUUID().toString().take(12)}"
        val payload = ensureId(record, id)
        val payloadJson = json.encodeToString(JsonObject.serializer(), payload)

        val existing = database.recordsQueries.selectById(id).executeAsOneOrNull()
        val created = existing == null
        if (created) {
            database.recordsQueries.insertRecord(
                id = id,
                source = name,
                payload_json = payloadJson,
                created_at_ms = now,
            )
        } else {
            database.recordsQueries.updateRecord(
                id = id,
                payload_json = payloadJson,
                created_at_ms = now,
            )
        }
        _changes.tryEmit(Unit)
        UpsertResult(id = id, created = created)
    }

    override suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        val existed = database.recordsQueries.selectById(id).executeAsOneOrNull() != null
        if (existed) {
            database.recordsQueries.deleteById(id)
            _changes.tryEmit(Unit)
        }
        existed
    }

    private fun decodeRecord(id: String, payloadJson: String): JsonObject {
        val decoded = runCatching {
            json.decodeFromString(JsonObject.serializer(), payloadJson)
        }.getOrElse { JsonObject(emptyMap()) }
        return ensureId(decoded, id)
    }

    private fun ensureId(record: JsonObject, id: String): JsonObject {
        if (record["id"]?.jsonPrimitive?.contentOrNull() == id) return record
        return buildJsonObject {
            put("id", JsonPrimitive(id))
            record.entries.forEach { (k, v) -> if (k != "id") put(k, v) }
        }
    }

    private fun sortRecords(records: List<JsonObject>, sort: List<SortSpec>): List<JsonObject> {
        val comparator = sort
            .map { spec ->
                Comparator<JsonObject> { a, b ->
                    val av = a[spec.field]?.toString().orEmpty()
                    val bv = b[spec.field]?.toString().orEmpty()
                    av.compareTo(bv)
                }.let { if (spec.order == SortOrder.DESC) it.reversed() else it }
            }
            .reduceOrNull { acc, c -> acc.then(c) }
        return if (comparator == null) records else records.sortedWith(comparator)
    }

    private fun JsonPrimitive.contentOrNull(): String? = if (isString) content else null
}
