package dev.weft.undercurrent.feature.chat.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * iOS-side LLM client interface. Each provider (Anthropic / OpenAI /
 * OpenRouter / DeepSeek) implements this; [send] returns a streaming
 * Flow so the UI can render token-by-token without waiting for the
 * full response.
 *
 * Intentionally minimal — no tool calls, no per-turn injection, no
 * multimodal, no model-tier routing.
 */
internal interface LlmClient {
    /** Display label shown in the chat header (e.g. "Claude Haiku 4.5"). */
    val displayName: String

    /** The provider model id this client sends to. */
    val modelId: String

    /**
     * Stream a reply for [history]. [systemPrompt] is sent verbatim
     * (or fielded provider-specifically). Flow emits [LlmChunk.TextDelta]s,
     * terminates with [LlmChunk.Done] on success or [LlmChunk.Error] on
     * failure.
     */
    fun send(history: List<LlmMessage>, systemPrompt: String): Flow<LlmChunk>
}

/** Streaming event emitted by [LlmClient.send]. */
internal sealed interface LlmChunk {
    data class TextDelta(val text: String) : LlmChunk
    data class Error(val message: String) : LlmChunk
    data object Done : LlmChunk
}

/** One turn in the conversation. */
@Serializable
internal data class LlmMessage(val role: String, val content: String) {
    companion object {
        const val ROLE_USER: String = "user"
        const val ROLE_ASSISTANT: String = "assistant"
    }
}

internal const val IOS_SYSTEM_PROMPT: String = """
You are a helpful assistant running on the user's iPhone. Keep replies
concise. You don't have access to device tools yet (calendar, location,
notifications, etc.) — those will come later. If the user asks for
something tool-shaped, acknowledge the request and explain what you'd
do, but don't pretend the tool ran.
"""
