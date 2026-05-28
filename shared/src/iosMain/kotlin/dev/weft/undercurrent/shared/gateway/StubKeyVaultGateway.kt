package dev.weft.undercurrent.shared.gateway

import dev.weft.undercurrent.core.model.ProviderKind

/**
 * iOS stub. Key persistence is keypaste's job and keypaste isn't on iOS
 * for v1 — providers are configured pre-build or through a future
 * Keychain-backed impl.
 */
public class StubKeyVaultGateway : KeyVaultGateway {
    override suspend fun putApiKey(provider: ProviderKind, apiKey: String): Unit =
        throw NotImplementedError("KeyVault not supported on iOS yet — use the Android build.")

    override suspend fun hasApiKey(provider: ProviderKind): Boolean = false

    override suspend fun clearApiKey(provider: ProviderKind) = Unit
}
