package dev.weft.undercurrent.app

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
 * surface of `WeftRuntime.buildAgent` from the substrate's `:android`
 * module — enough to chat against Anthropic with the substrate's
 * agent loop (model routing, retry, circuit breaker, observability,
 * cache binding) instead of the parallel Ktor `LlmClient` path.
 *
 * Scope of v1:
 *   - Anthropic provider only. Other providers continue to flow
 *     through the existing `LlmClient` until the bridge supports them.
 *   - Empty tool registry. Tool support comes after the first chat
 *     turn proves the wiring works.
 *   - In-memory conversation + trace stores. SQLite-backed equivalents
 *     can plug in later via the same constructor surface.
 *   - Default behavior + retry + circuit breaker (no overrides).
 *
 * The agent is constructed per-conversation — `IosAppViewModel` rebuilds
 * it when the API key changes or the user switches providers.
 */
object IosWeftAgentFactory {

    /**
     * Build a WeftAgent talking to Anthropic with [apiKey]. [systemPrompt]
     * is wrapped in a static supplier so the substrate caches it across
     * turns. [conversationStore] / [traceStore] default to in-memory
     * instances when null.
     */
    fun buildAnthropic(
        apiKey: String,
        systemPrompt: String,
        conversationStore: ConversationStore? = null,
        traceStore: TraceStore = InMemoryTraceStore(),
    ): WeftAgent {
        // Koog's Anthropic client. Ktor engine has to be passed
        // explicitly — the ServiceLoader-based discovery the JVM
        // build uses isn't available in K/N.
        val client = AnthropicLLMClient(
            apiKey = apiKey,
            settings = AnthropicClientSettings(),
            httpClientFactory = KtorKoogHttpClient.Factory(),
        )

        // Minimal model pool: same Sonnet across cheap/standard/vision/
        // heavy slots so the router never picks an unintended model on
        // iOS until per-tier routing is exercised here.
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
