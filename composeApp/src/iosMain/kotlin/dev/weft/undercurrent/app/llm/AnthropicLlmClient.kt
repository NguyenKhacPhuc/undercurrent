package dev.weft.undercurrent.app.llm

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Anthropic Messages-API client. SSE streaming via
 * `stream=true` + content_block_delta events.
 *
 * Wire format reference:
 *   https://docs.anthropic.com/en/api/messages-streaming
 *
 * Only the `content_block_delta` events of type `text_delta` are
 * surfaced as [LlmChunk.TextDelta]; the message-level lifecycle events
 * (message_start / message_delta / message_stop / ping) are consumed
 * silently. We treat `message_stop` as the terminator that emits
 * [LlmChunk.Done].
 *
 * Pinned to `claude-haiku-4-5-20251001`. The providers screen could
 * drive the model id later but for now it's hard-coded — minimum
 * viable.
 */
internal class AnthropicLlmClient(
    private val getApiKey: suspend () -> String?,
    override val modelId: String = "claude-haiku-4-5-20251001",
    override val displayName: String = "Claude Haiku 4.5",
) : LlmClient {

    private val http = HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override fun send(history: List<LlmMessage>, systemPrompt: String): Flow<LlmChunk> = flow {
        val key = getApiKey()
        if (key.isNullOrBlank()) {
            emit(LlmChunk.Error("No Anthropic API key configured."))
            emit(LlmChunk.Done)
            return@flow
        }
        try {
            http.preparePost("https://api.anthropic.com/v1/messages") {
                header("x-api-key", key)
                header("anthropic-version", ANTHROPIC_VERSION)
                contentType(ContentType.Application.Json)
                setBody(
                    Request(
                        model = modelId,
                        maxTokens = 1024,
                        system = systemPrompt,
                        messages = history,
                        stream = true,
                    ),
                )
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    val errBody = runCatching { response.bodyAsChannel().readRemainingAsString() }
                        .getOrDefault("")
                    emit(
                        LlmChunk.Error(
                            "Anthropic API ${response.status.value}: ${errBody.take(400)}",
                        ),
                    )
                    return@execute
                }
                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isEmpty()) continue
                    val event = runCatching {
                        STREAM_JSON.decodeFromString(StreamEvent.serializer(), payload)
                    }.getOrNull() ?: continue
                    when (event.type) {
                        "content_block_delta" -> {
                            val text = event.delta?.text
                            if (!text.isNullOrEmpty()) emit(LlmChunk.TextDelta(text))
                        }
                        "message_stop" -> {
                            emit(LlmChunk.Done)
                            return@execute
                        }
                        // message_start / content_block_start / content_block_stop /
                        // message_delta / ping — ignored. Their info is duplicated in
                        // the events we already handle.
                    }
                }
                // Server closed the channel without a message_stop —
                // treat as a successful end-of-stream anyway.
                emit(LlmChunk.Done)
            }
        } catch (t: Throwable) {
            emit(LlmChunk.Error(t.message ?: t::class.simpleName.orEmpty()))
            emit(LlmChunk.Done)
        }
    }

    @Serializable
    private data class Request(
        val model: String,
        @SerialName("max_tokens") val maxTokens: Int,
        val system: String? = null,
        val messages: List<LlmMessage>,
        val stream: Boolean = false,
    )

    @Serializable
    private data class StreamEvent(
        val type: String,
        val delta: Delta? = null,
    )

    @Serializable
    private data class Delta(
        val type: String = "",
        val text: String? = null,
    )

    private companion object {
        const val ANTHROPIC_VERSION: String = "2023-06-01"
        val STREAM_JSON: Json = Json { ignoreUnknownKeys = true }
    }
}

/** Drain the remaining bytes of a ByteReadChannel as a UTF-8 string. */
private suspend fun io.ktor.utils.io.ByteReadChannel.readRemainingAsString(): String =
    buildString {
        while (!this@readRemainingAsString.isClosedForRead) {
            val line = readUTF8Line() ?: break
            append(line)
            append('\n')
        }
    }
