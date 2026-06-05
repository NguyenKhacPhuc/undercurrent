package dev.weft.undercurrent.feature.miniapps

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val HTTP_JSON = Json { isLenient = true }

/**
 * `http_fetch`: perform an HTTP request and return `{status, body}` as
 * JSON. Args: `{url, method?, headers?, body?}` (method defaults to GET).
 * Any HTTP status — including 4xx/5xx — comes back as a result so the
 * mini-app can inspect it; only a transport-level failure (including the
 * host-supplied [client]'s NetworkPolicy allowlist rejecting a blocked
 * host) throws, which the bridge surfaces as a rejected Promise.
 *
 * The allowlist is not enforced here: the host wires it onto [client]
 * via the substrate's NetworkPolicy ktor plugin, so every call this
 * handler makes is already constrained.
 */
fun httpFetchHandler(client: HttpClient): MiniAppActionHandler =
    MiniAppActionHandler { _, argsJson ->
        val args = HTTP_JSON.parseToJsonElement(argsJson).jsonObject
        val url = args["url"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("http_fetch requires a 'url'")
        val methodName = args["method"]?.jsonPrimitive?.contentOrNull ?: "GET"
        val body = args["body"]?.jsonPrimitive?.contentOrNull
        val headers = args["headers"] as? JsonObject

        val response = client.request(url) {
            method = HttpMethod.parse(methodName.uppercase())
            headers?.forEach { (name, value) -> header(name, value.jsonPrimitive.content) }
            if (body != null) setBody(body)
        }
        buildJsonObject {
            put("status", response.status.value)
            put("body", response.bodyAsText())
        }.toString()
    }
