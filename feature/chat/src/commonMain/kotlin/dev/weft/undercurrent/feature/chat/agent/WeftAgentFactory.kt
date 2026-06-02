package dev.weft.undercurrent.feature.chat.agent

import dev.weft.android.WeftRuntime
import dev.weft.android.credentials.StaticDeepSeekKeyProvider
import dev.weft.android.credentials.StaticKeyProvider
import dev.weft.android.credentials.StaticOpenAIKeyProvider
import dev.weft.android.credentials.StaticOpenRouterKeyProvider
import dev.weft.contracts.WeftCredentialProvider
import dev.weft.harness.agents.WeftAgent
import dev.weft.undercurrent.core.domain.ModelPrefsRepository
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
        modelPoolOverride = resolveModelPoolOverride(provider, modelPrefsRepo),
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
}
