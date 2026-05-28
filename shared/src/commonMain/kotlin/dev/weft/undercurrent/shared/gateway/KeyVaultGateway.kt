package dev.weft.undercurrent.shared.gateway

import dev.weft.undercurrent.core.model.ProviderKind

/**
 * Per-provider API-key storage. The Android impl in `:data:weft` writes
 * through Weft's `KeyVault` (backed by Android Keystore-encrypted prefs);
 * the iOS impl writes to the Keychain via `kSecClassGenericPassword`.
 *
 * Access policy:
 *  - Feature screens (KeyPaste, Providers) only call [hasApiKey] /
 *    [putApiKey] / [clearApiKey] — they never see plaintext keys.
 *  - [getApiKey] is for AppStore-level callers that need the key to
 *    build HTTP requests (iOS Anthropic client, future iOS providers).
 *    On Android, AppStore reads through `WeftRuntime.keyVault` directly
 *    instead — this gateway method is implemented for symmetry but is
 *    not the primary read path there.
 */
public interface KeyVaultGateway {

    /**
     * Persist [apiKey] for [provider]. Overwrites any previous value at
     * the same provider's alias. Throws on storage failure (locked
     * Keystore, Keychain access denied, etc.).
     */
    public suspend fun putApiKey(provider: ProviderKind, apiKey: String)

    /**
     * Read the stored key for [provider]. Returns `null` when no key is
     * stored or the underlying store rejected the read.
     *
     * AppStore-level callers only. Don't pipe this into feature screens
     * — they should display "configured" / "not configured" via
     * [hasApiKey], not the plaintext key.
     */
    public suspend fun getApiKey(provider: ProviderKind): String?

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
