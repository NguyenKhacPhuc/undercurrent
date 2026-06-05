package dev.weft.undercurrent.feature.miniapps

import dev.weft.compose.components.MiniAppStateStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val STORE_JSON = Json { isLenient = true }

private fun requiredKey(argsJson: String, action: String): String =
    STORE_JSON.parseToJsonElement(argsJson).jsonObject["key"]?.jsonPrimitive?.content
        ?: throw IllegalArgumentException("$action requires a 'key'")

private suspend fun MiniAppStateStore.blobObject(miniAppId: String?): JsonObject =
    get(miniAppId)?.let { STORE_JSON.parseToJsonElement(it).jsonObject } ?: JsonObject(emptyMap())

/**
 * `store_get`: read one key from the mini-app's key-value state. The
 * mini-app's state blob is a JSON object; this returns the value at
 * `args.key` as JSON, or `null` when the key (or the blob) is absent.
 */
fun storeGetHandler(store: MiniAppStateStore, miniAppId: String?): MiniAppActionHandler =
    MiniAppActionHandler { argsJson ->
        val key = requiredKey(argsJson, "store_get")
        (store.blobObject(miniAppId)[key] ?: JsonNull).toString()
    }

/**
 * `store_set`: write one key into the mini-app's key-value state,
 * merging into the existing blob so sibling keys survive. Returns
 * `"true"` once persisted.
 */
fun storeSetHandler(store: MiniAppStateStore, miniAppId: String?): MiniAppActionHandler =
    MiniAppActionHandler { argsJson ->
        val args = STORE_JSON.parseToJsonElement(argsJson).jsonObject
        val key = args["key"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("store_set requires a 'key'")
        val value = args["value"] ?: JsonNull
        val merged = JsonObject(store.blobObject(miniAppId) + (key to value))
        store.set(miniAppId, merged.toString())
        "true"
    }
