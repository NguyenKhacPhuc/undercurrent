package dev.weft.undercurrent.app.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * iOS-side LLM client interface. Each provider (Anthropic / OpenAI /
 * OpenRouter / DeepSeek) implements this; [send] returns a streaming
 * Flow so the UI can render token-by-token without waiting for the
 * full response.
 *
 * Intentionally minimal:
 *  - No tool calls.
 *  - One-system-prompt-string (no fragments / no per-turn injection).
 *  - No multimodal input.
 *  - No model-tier routing (each client picks one model id).
 *
 * Anything fancier waits for Phase 3+. The goal here is "user can
 * have a text conversation across all four providers."
 */
internal interface LlmClient {
    /** Display label shown in the chat header (e.g. "Claude Haiku 4.5"). */
    val displayName: String

    /** The provider model id this client sends to. */
    val modelId: String

    /**
     * Stream a reply for [history]. The flow emits [LlmChunk.TextDelta]s
     * as text fragments arrive, terminates with [LlmChunk.Done] on
     * success, or [LlmChunk.Error] on transport / auth / parse failure.
     *
     * [history] is the full conversation in alternating user/assistant
     * roles; the caller (IosAppStore) appends the new user turn before
     * calling. [systemPrompt] is sent verbatim (or fielded provider-
     * specifically — OpenAI uses a "system" message; Anthropic uses a
     * top-level field).
     */
    fun send(history: List<LlmMessage>, systemPrompt: String): Flow<LlmChunk>
}

/** Streaming event emitted by [LlmClient.send]. */
internal sealed interface LlmChunk {
    /** Text fragment to append to the in-flight assistant message. */
    data class TextDelta(val text: String) : LlmChunk

    /** Transport / auth / parse failure. Terminal — flow completes after. */
    data class Error(val message: String) : LlmChunk

    /** Successful completion. Terminal. */
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

/**
 * Stock system prompt used by every provider. Plain "you're a helpful
 * assistant on iOS, no tools yet." When tools land we'll replace this
 * with the substrate's `WeftSystemPromptDefaults`-equivalent.
 */
internal const val IOS_SYSTEM_PROMPT: String = """
You are a helpful assistant running on the user's iPhone. Keep replies
concise. You don't have access to device tools yet (calendar, location,
notifications, etc.) — those will come later. If the user asks for
something tool-shaped, acknowledge the request and explain what you'd
do, but don't pretend the tool ran.
"""
