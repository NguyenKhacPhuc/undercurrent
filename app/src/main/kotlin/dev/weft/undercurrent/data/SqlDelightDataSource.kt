package dev.weft.undercurrent.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import android.content.Context
import dev.weft.contracts.DataSource
import dev.weft.contracts.QueryResult
import dev.weft.contracts.SortOrder
import dev.weft.contracts.SortSpec
import dev.weft.contracts.UpsertResult
import dev.weft.undercurrent.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

/**
 * Persistent [DataSource] backed by SQLDelight. Records live in the
 * `records` table (schema in `app/src/main/sqldelight/.../Records.sq`),
 * one row per record, payload encoded as a JSON object string.
 *
 * Survives app restarts — unlike [InMemoryDataSource], which exists
 * for tests and demos but loses everything on process death. The
 * agent's `data_*` tools should all be wired to this implementation
 * in production builds.
 *
 * **Sources are name slots, not tables.** One [SqlDelightDataSource]
 * instance corresponds to one source name (`notes`, `tasks`, …); the
 * underlying [AppDatabase] is shared. The `source` column on `records`
 * separates them so each instance only ever sees its own rows.
 *
 * **Filtering policy.** The contract's `filter` is JsonObject equality
 * across all keys. We pull all rows for the source (newest first) and
 * filter in Kotlin — fine for the dataset sizes a personal app sees
 * (hundreds of records per source). For collections that ever grow
 * past ~10k rows, push the filter into SQLite via JSON1 (`json_extract`).
 */
internal class SqlDelightDataSource(
    override val name: String,
    private val database: AppDatabase,
) : DataSource {

    private val json = Json { ignoreUnknownKeys = true }

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

        // Filter — every key/value in `filter` must match the
        // corresponding field in the record. The contract leaves
        // unmatched keys as no-ops (record without the field fails
        // the predicate), which is what `[key] == value` here does.
        val matching = all.filter { record ->
            filter.entries.all { (key, value) -> record[key] == value }
        }

        // Sort — schema-default is newest-first (the SELECT does this);
        // the contract's [sort] override re-sorts on the requested
        // field as a string. Mixed-type fields fall back to JSON
        // string comparison which is consistent if not always
        // semantically correct (e.g., "10" < "2" lexicographically).
        // Apps that care can include a normalized field.
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
        // Use the idempotency key if provided; else the record's
        // declared id; else generate one. The agent doesn't usually
        // supply ids — it's the storage layer's job to mint stable
        // ones.
        val id = idempotencyKey
            ?: record["id"]?.jsonPrimitive?.contentOrNull()
            ?: "rec.${UUID.randomUUID().toString().take(12)}"
        val payload = ensureId(record, id)
        val payloadJson = json.encodeToString(JsonObject.serializer(), payload)

        // upsert via select-then-insert-or-update. SQLite has
        // INSERT...ON CONFLICT but the SQLDelight 2.x driver requires
        // an explicit conflict-target list; the two-step path here
        // reads cleanly and the index on (id) keeps it cheap.
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
        UpsertResult(id = id, created = created)
    }

    override suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        val existed = database.recordsQueries.selectById(id).executeAsOneOrNull() != null
        if (existed) database.recordsQueries.deleteById(id)
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

/**
 * Build the SQLDelight driver + database. Single instance per app —
 * the Koin module wires this and hands a shared [AppDatabase] to each
 * [SqlDelightDataSource] (one per registered source name).
 *
 * The driver writes to `undercurrent_records.db` in the app's
 * private storage. Same on-disk file across upgrades; schema
 * migrations go in `app/src/main/sqldelight/dev/weft/undercurrent/db/migrations/`
 * once we need them.
 */
internal object AppDatabaseFactory {
    fun create(context: Context): AppDatabase {
        val driver: SqlDriver = AndroidSqliteDriver(
            schema = AppDatabase.Schema,
            context = context.applicationContext,
            name = "undercurrent_records.db",
        )
        return AppDatabase(driver)
    }
}
