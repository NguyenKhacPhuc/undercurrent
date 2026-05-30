package dev.weft.undercurrent.feature.chat.agent

import dev.weft.android.WeftRuntime
import dev.weft.android.credentials.StaticDeepSeekKeyProvider
import dev.weft.android.credentials.StaticKeyProvider
import dev.weft.android.credentials.StaticOpenAIKeyProvider
import dev.weft.android.credentials.StaticOpenRouterKeyProvider
import dev.weft.android.routing.defaultPoolFor
import dev.weft.android.routing.findModelInCatalog
import dev.weft.contracts.WeftCredentialProvider
import dev.weft.harness.agents.WeftAgent
import dev.weft.harness.agents.routing.ModelPool
import dev.weft.undercurrent.core.domain.ModelPrefsRepository
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind

class WeftAgentFactory(
    private val runtime: WeftRuntime,
    private val modelPrefsRepo: ModelPrefsRepository,
) {

    suspend fun build(
        agentName: String,
        provider: ProviderKind,
        apiKey: String,
    ): WeftAgent = runtime.buildAgent(
        agentName = agentName,
        provider = credentialProviderFor(provider, apiKey),
        modelPoolOverride = modelPoolOverrideFor(provider),
    )

    private fun credentialProviderFor(
        provider: ProviderKind,
        apiKey: String,
    ): WeftCredentialProvider = when (provider) {
        ProviderKind.Anthropic -> StaticKeyProvider(apiKey)
        ProviderKind.OpenAI -> StaticOpenAIKeyProvider(apiKey)
        ProviderKind.OpenRouter -> StaticOpenRouterKeyProvider(apiKey)
        ProviderKind.DeepSeek -> StaticDeepSeekKeyProvider(apiKey)
    }

    private fun modelPoolOverrideFor(provider: ProviderKind): ModelPool? {
        val anyOverride = ModelTier.entries.any { tier ->
            modelPrefsRepo.overrideFor(provider, tier) != null
        }
        if (!anyOverride) return null

        val weftProvider = provider.toWeft()
        val defaultPool = defaultPoolFor(weftProvider)
        fun resolve(tier: ModelTier, default: ai.koog.prompt.llm.LLModel) =
            modelPrefsRepo.overrideFor(provider, tier)
                ?.let { findModelInCatalog(weftProvider, it) }
                ?: default

        return ModelPool(
            cheap = resolve(ModelTier.Cheap, defaultPool.cheap),
            standard = resolve(ModelTier.Standard, defaultPool.standard),
            vision = resolve(ModelTier.Vision, defaultPool.vision),
            heavy = resolve(ModelTier.Heavy, defaultPool.heavy),
        )
    }
}
