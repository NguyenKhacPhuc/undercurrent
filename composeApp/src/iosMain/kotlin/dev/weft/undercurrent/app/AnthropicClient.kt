package dev.weft.undercurrent.app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Minimal iOS Anthropic client. One-shot (no SSE streaming yet) calls
 * to the Messages API. Just enough to power text-only chat on iOS —
 * no tool calls, no multimodal, no system prompt beyond a stock
 * "you are a helpful assistant" string.
 *
 * Designed to be replaced — the Phase 2 plan adds streaming + multi-
 * provider support. Keep this file small and dependency-free for now.
 *
 * The model id is pinned to `claude-haiku-4-5` — the cheapest current
 * Claude model. Surface it via [model] when the providers screen
 * needs to drive it later.
 */
internal class AnthropicClient(
    private val getApiKey: suspend () -> String?,
    public val model: String = "claude-haiku-4-5-20251001",
    private val systemPrompt: String = DEFAULT_SYSTEM,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val http = HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(this@AnthropicClient.json)
        }
    }

    /**
     * Send the conversation history and return the assistant's reply.
     * Throws on transport / auth failure or empty response.
     *
     * The [messages] list must alternate user/assistant; the caller
     * (IosAppStore) appends the new user turn before calling.
     */
    suspend fun send(messages: List<Message>): String {
        val apiKey = getApiKey()
            ?: error("No Anthropic API key configured. Paste one in Settings.")
        val response = http.post("https://api.anthropic.com/v1/messages") {
            header("x-api-key", apiKey)
            header("anthropic-version", ANTHROPIC_VERSION)
            contentType(ContentType.Application.Json)
            setBody(
                Request(
                    model = model,
                    maxTokens = 1024,
                    system = systemPrompt,
                    messages = messages,
                ),
            )
        }
        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText() }.getOrDefault("")
            error("Anthropic API error ${response.status.value}: ${body.take(500)}")
        }
        val parsed: Response = response.body()
        val text = parsed.content.firstOrNull { it.type == "text" }?.text
        return text?.takeIf { it.isNotBlank() }
            ?: error("Anthropic returned no text content (stop_reason=${parsed.stopReason})")
    }

    @Serializable
    data class Message(val role: String, val content: String) {
        companion object {
            const val ROLE_USER = "user"
            const val ROLE_ASSISTANT = "assistant"
        }
    }

    @Serializable
    private data class Request(
        val model: String,
        @SerialName("max_tokens") val maxTokens: Int,
        val system: String? = null,
        val messages: List<Message>,
    )

    @Serializable
    private data class Response(
        val id: String = "",
        val type: String = "",
        val role: String = "",
        val content: List<ContentBlock> = emptyList(),
        @SerialName("stop_reason") val stopReason: String? = null,
    )

    @Serializable
    private data class ContentBlock(
        val type: String,
        val text: String? = null,
    )

    private companion object {
        const val ANTHROPIC_VERSION: String = "2023-06-01"
        const val DEFAULT_SYSTEM: String = """
You are a helpful assistant running on the user's iPhone. Keep replies
concise. You don't have access to device tools yet (calendar, location,
notifications, etc.) — those will come later. If the user asks for
something tool-shaped, acknowledge the request and explain what you'd
do, but don't pretend the tool ran.
"""
    }
}
