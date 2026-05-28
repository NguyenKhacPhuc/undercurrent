package dev.weft.undercurrent.data.weft

import dev.weft.android.WeftRuntime
import dev.weft.contracts.KeyVault
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.shared.gateway.KeyVaultGateway

/**
 * Android impl of [KeyVaultGateway]. Delegates to Weft's [KeyVault] (Android
 * Keystore-encrypted prefs) using the per-provider aliases declared on
 * [WeftRuntime].
 *
 * Constructed by DI with the [KeyVault] instance exposed by the runtime —
 * see `:androidApp` Koin module.
 */
public class WeftKeyVaultGateway(
    private val keyVault: KeyVault,
) : KeyVaultGateway {

    override suspend fun putApiKey(provider: ProviderKind, apiKey: String) {
        keyVault.put(provider.alias(), apiKey)
    }

    override suspend fun getApiKey(provider: ProviderKind): String? =
        keyVault.get(provider.alias())

    override suspend fun hasApiKey(provider: ProviderKind): Boolean =
        keyVault.exists(provider.alias())

    override suspend fun clearApiKey(provider: ProviderKind) {
        keyVault.remove(provider.alias())
    }

    private fun ProviderKind.alias(): String = when (this) {
        ProviderKind.Anthropic -> WeftRuntime.ANTHROPIC_KEY_ALIAS
        ProviderKind.OpenAI -> WeftRuntime.OPENAI_KEY_ALIAS
        ProviderKind.OpenRouter -> WeftRuntime.OPENROUTER_KEY_ALIAS
        ProviderKind.DeepSeek -> WeftRuntime.DEEPSEEK_KEY_ALIAS
    }
}
