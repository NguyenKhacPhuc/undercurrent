package dev.weft.undercurrent.feature.chat.llm

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicClientSettings
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import dev.weft.harness.agents.WeftAgent
import dev.weft.harness.agents.routing.ModelPool
import dev.weft.harness.agents.routing.StaticModelRouter
import dev.weft.harness.conversation.ConversationStore
import dev.weft.harness.conversation.InMemoryConversationStore
import dev.weft.harness.observability.InMemoryTraceStore
import dev.weft.harness.observability.TraceStore
import dev.weft.harness.prompt.cache.AnthropicCacheBinder

/**
 * Construct a substrate-backed [WeftAgent] for iOS. Mirrors the minimum
 * surface of `WeftRuntime.buildAgent` from the substrate's Android
 * runtime — enough to chat against Anthropic with the substrate's
 * agent loop (model routing, retry, circuit breaker, observability,
 * cache binding) instead of the parallel Ktor `LlmClient` path.
 *
 * Anthropic-only for v1. Empty tool registry (waiting on iOS
 * OsCapabilities). In-memory stores; the SQLDelight-backed
 * ConversationStore wiring lands when iOS adopts the substrate's
 * full conversation persistence.
 */
internal object IosWeftAgentFactory {

    fun buildAnthropic(
        apiKey: String,
        systemPrompt: String,
        conversationStore: ConversationStore? = null,
        traceStore: TraceStore = InMemoryTraceStore(),
    ): WeftAgent {
        val client = AnthropicLLMClient(
            apiKey = apiKey,
            settings = AnthropicClientSettings(),
            httpClientFactory = KtorKoogHttpClient.Factory(),
        )

        val standard = AnthropicModels.Sonnet_4_5
        val pool = ModelPool(
            cheap = standard,
            standard = standard,
            vision = standard,
            heavy = standard,
        )

        return WeftAgent(
            executor = MultiLLMPromptExecutor(client),
            modelPool = pool,
            modelRouter = StaticModelRouter(standard),
            toolRegistry = ToolRegistry.EMPTY,
            traceStore = traceStore,
            baseSystemPromptSupplier = { systemPrompt },
            conversationStore = conversationStore ?: InMemoryConversationStore(),
            cacheBinder = AnthropicCacheBinder,
        )
    }
}
