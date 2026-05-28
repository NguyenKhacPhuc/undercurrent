package dev.weft.undercurrent.shared.gateway

import dev.weft.undercurrent.core.model.ProviderKind

/**
 * Per-provider API-key storage. The Android impl in `:data:weft` writes
 * through Weft's `KeyVault` (backed by Android Keystore-encrypted prefs).
 * iOS stub throws — Keychain-backed impl arrives when keypaste lands on
 * iOS.
 *
 * The gateway is intentionally narrow: write + check-existence. Reads are
 * never exposed to feature code; only the agent runtime reads keys.
 */
public interface KeyVaultGateway {

    /**
     * Persist [apiKey] for [provider]. Overwrites any previous value at
     * the same provider's alias. Throws on storage failure (locked
     * Keystore, etc.).
     */
    public suspend fun putApiKey(provider: ProviderKind, apiKey: String)

    /**
     * Whether a key is currently stored for [provider]. The keypaste
     * screen uses this to render "Configured ✓" vs "Add key" without
     * exposing the key itself.
     */
    public suspend fun hasApiKey(provider: ProviderKind): Boolean

    /**
     * Remove the stored key for [provider]. No-op if nothing's stored.
     */
    public suspend fun clearApiKey(provider: ProviderKind)
}
