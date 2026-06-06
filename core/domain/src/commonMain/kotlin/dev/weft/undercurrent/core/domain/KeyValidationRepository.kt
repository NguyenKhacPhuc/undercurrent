package dev.weft.undercurrent.core.domain

import dev.weft.undercurrent.core.model.ProviderKind

/**
 * Validates that an API key actually authorizes completions against
 * the named provider. Backed by Koog's per-provider LLM client on
 * Android (which is the only place Koog's clients run); iOS stub
 * returns [ValidationResult.Invalid] with a "not supported" message.
 *
 * Used by [dev.weft.undercurrent.feature.onboarding.KeyPasteScreen]'s
 * "Connect" path: paste a key → ping the provider with one token →
 * surface OK or a typed error.
 *
 * Token cost is negligible (~$0.0001 per call) — that's why we ping
 * with a real chat request instead of a `/v1/models`-style health
 * check: it exercises the same auth path as a real send, so a key
 * that validates here is guaranteed to send.
 */
interface KeyValidationRepository {
    suspend fun validateKey(provider: ProviderKind, apiKey: String): ValidationResult
}

sealed interface ValidationResult {
    data object Ok : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}
