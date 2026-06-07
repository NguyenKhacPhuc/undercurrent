package dev.weft.undercurrent.feature.settings.providers

import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind

/**
 * Provider-configuration intents — picking the active provider,
 * saving / removing keys, choosing the default tier, and per-tier
 * model overrides.
 *
 * [SubmitKey] lives here (not in onboarding / keypaste) because it's
 * the same plumbing as [SaveProviderKey] — both write to KeyVault
 * and rebuild the agent. KeyPaste happens to be the first surface
 * that uses it; Settings → Providers is the second.
 */
sealed interface ProviderIntent {

    /**
     * Build the agent for the freshly-pasted key and navigate to
     * Chat. Used by the KeyPaste screen on first run / after a
     * RemoveProviderKey.
     */
    data class SubmitKey(val key: String) : ProviderIntent

    data class SetProvider(val provider: ProviderKind) : ProviderIntent

    data class SaveProviderKey(
        val provider: ProviderKind,
        val apiKey: String,
    ) : ProviderIntent

    /**
     * Validate [apiKey] for [provider]; on success persist it (same
     * effect as [SaveProviderKey]) and reflect it in state. Drives the
     * Providers screen's "Save key" action — validation status is
     * surfaced via [ProviderState.keyValidation].
     */
    data class ValidateAndSaveProviderKey(
        val provider: ProviderKind,
        val apiKey: String,
    ) : ProviderIntent

    /** Reset [ProviderState.keyValidation] to idle (e.g. on key edit). */
    data object ClearKeyValidation : ProviderIntent

    data class RemoveProviderKey(val provider: ProviderKind) : ProviderIntent

    data class SetDefaultTier(val tier: ModelTier?) : ProviderIntent

    /** Set the model that fills a specific (provider, tier) slot. */
    data class SetModelForTier(
        val provider: ProviderKind,
        val tier: ModelTier,
        val modelId: String?,
    ) : ProviderIntent
}
