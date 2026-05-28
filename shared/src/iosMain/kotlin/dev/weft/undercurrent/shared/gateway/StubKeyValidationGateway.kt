package dev.weft.undercurrent.shared.gateway

import dev.weft.undercurrent.core.model.ProviderKind

/**
 * iOS stub. Koog's LLM clients are JVM/Android-only — a real iOS
 * impl would either ship Koog to iOS or call providers via Ktor
 * directly. For v1 the keypaste flow is Android-only.
 */
public class StubKeyValidationGateway : KeyValidationGateway {
    override suspend fun validateKey(provider: ProviderKind, apiKey: String): ValidationResult =
        ValidationResult.Invalid("Key validation not supported on iOS yet.")
}
