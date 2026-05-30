package dev.weft.undercurrent.feature.chat.llm

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
 * Generic client for any OpenAI-compatible Chat Completions endpoint.
 * Reused across OpenAI / OpenRouter / DeepSeek — each only differs in
 * base URL + model id.
 */
internal class OpenAICompatLlmClient(
    override val displayName: String,
    override val modelId: String,
    private val baseUrl: String,
    private val getApiKey: suspend () -> String?,
) : LlmClient {

    private val http = HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override fun send(history: List<LlmMessage>, systemPrompt: String): Flow<LlmChunk> = flow {
        val key = getApiKey()
        if (key.isNullOrBlank()) {
            emit(LlmChunk.Error("No API key configured for $displayName."))
            emit(LlmChunk.Done)
            return@flow
        }

        val messages = buildList {
            if (systemPrompt.isNotBlank()) add(LlmMessage("system", systemPrompt))
            addAll(history)
        }

        try {
            http.preparePost("$baseUrl/chat/completions") {
                header("Authorization", "Bearer $key")
                contentType(ContentType.Application.Json)
                setBody(
                    Request(
                        model = modelId,
                        messages = messages,
                        stream = true,
                    ),
                )
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    val errBody = runCatching {
                        response.bodyAsChannel().readRemainingAsString()
                    }.getOrDefault("")
                    emit(
                        LlmChunk.Error(
                            "$displayName API ${response.status.value}: ${errBody.take(400)}",
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
                    if (payload == "[DONE]") {
                        emit(LlmChunk.Done)
                        return@execute
                    }
                    val event = runCatching {
                        STREAM_JSON.decodeFromString(StreamEvent.serializer(), payload)
                    }.getOrNull() ?: continue
                    val text = event.choices.firstOrNull()?.delta?.content
                    if (!text.isNullOrEmpty()) emit(LlmChunk.TextDelta(text))
                }
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
        val messages: List<LlmMessage>,
        val stream: Boolean = false,
    )

    @Serializable
    private data class StreamEvent(
        val id: String = "",
        val choices: List<Choice> = emptyList(),
    )

    @Serializable
    private data class Choice(
        val index: Int = 0,
        val delta: Delta = Delta(),
        @SerialName("finish_reason") val finishReason: String? = null,
    )

    @Serializable
    private data class Delta(
        val role: String? = null,
        val content: String? = null,
    )

    private companion object {
        val STREAM_JSON: Json = Json { ignoreUnknownKeys = true }
    }
}

// ─── Factory helpers ─────────────────────────────────────────────────

internal fun openAIClient(getApiKey: suspend () -> String?): LlmClient =
    OpenAICompatLlmClient(
        displayName = "GPT-5 Mini",
        modelId = "gpt-5-mini",
        baseUrl = "https://api.openai.com/v1",
        getApiKey = getApiKey,
    )

internal fun openRouterClient(getApiKey: suspend () -> String?): LlmClient =
    OpenAICompatLlmClient(
        displayName = "Claude Sonnet 4.5 (OpenRouter)",
        modelId = "anthropic/claude-sonnet-4-5",
        baseUrl = "https://openrouter.ai/api/v1",
        getApiKey = getApiKey,
    )

internal fun deepSeekClient(getApiKey: suspend () -> String?): LlmClient =
    OpenAICompatLlmClient(
        displayName = "DeepSeek Chat",
        modelId = "deepseek-chat",
        baseUrl = "https://api.deepseek.com/v1",
        getApiKey = getApiKey,
    )
