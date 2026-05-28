package dev.weft.undercurrent.shared.gateway

import dev.weft.undercurrent.core.model.ProviderKind

/**
 * iOS stub. Returns a small set of representative models per provider
 * so the providers / onboarding screens don't render "0 MODELS" labels
 * that look broken. The IDs roughly track real model names but the
 * screens never actually invoke these models — chat is a "coming to
 * iOS" placeholder until a real iOS agent runtime lands.
 */
public class StubModelCatalog : ModelCatalog {
    override fun modelsForProvider(provider: ProviderKind): List<ModelInfo> = when (provider) {
        ProviderKind.Anthropic -> listOf(
            ModelInfo("claude-haiku-4-5", "Claude Haiku 4.5", hasVision = true, hasTools = true),
            ModelInfo("claude-sonnet-4-5", "Claude Sonnet 4.5", hasVision = true, hasTools = true),
            ModelInfo("claude-opus-4-5", "Claude Opus 4.5", hasVision = true, hasTools = true),
        )
        ProviderKind.OpenAI -> listOf(
            ModelInfo("gpt-5", "GPT-5", hasVision = true, hasTools = true),
            ModelInfo("gpt-5-mini", "GPT-5 Mini", hasVision = true, hasTools = true),
            ModelInfo("gpt-4o-mini", "GPT-4o Mini", hasVision = true, hasTools = true),
        )
        ProviderKind.OpenRouter -> listOf(
            ModelInfo("anthropic/claude-sonnet-4-5", "Claude Sonnet 4.5", hasVision = true, hasTools = true),
            ModelInfo("openai/gpt-5", "GPT-5", hasVision = true, hasTools = true),
            ModelInfo("meta/llama-3-70b", "Llama 3 70B", hasVision = false, hasTools = true),
        )
        ProviderKind.DeepSeek -> listOf(
            ModelInfo("deepseek-chat", "DeepSeek Chat", hasVision = false, hasTools = true),
            ModelInfo("deepseek-reasoner", "DeepSeek Reasoner", hasVision = false, hasTools = false),
        )
    }

    override fun defaultPoolForProvider(provider: ProviderKind): ModelPool {
        val models = modelsForProvider(provider)
        val cheap = models.firstOrNull() ?: placeholder
        val standard = models.getOrElse(1) { cheap }
        return ModelPool(cheap = cheap, standard = standard)
    }

    private companion object {
        val placeholder = ModelInfo(
            id = "unavailable",
            shortName = "Unavailable",
            hasVision = false,
            hasTools = false,
        )
    }
}
