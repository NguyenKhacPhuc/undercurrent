package dev.weft.undercurrent.core.domain

/**
 * Per-device secure storage for the BE-issued session bearer token.
 *
 * Single-slot — only one token is held at a time; [save] overwrites any
 * previously-stored value. [clear] removes the token.
 *
 * Implementations live per-platform:
 *  - `androidMain`: `EncryptedSharedPreferences`-backed (mobile-auth-wiring/02)
 *  - `iosMain`:     Keychain Services-backed (mobile-auth-wiring/03)
 *
 * Tests share a single behavior contract — see
 * `SessionTokenStoreContractSpec` in `commonTest`.
 */
interface SessionTokenStore {
    /**
     * Persist [token]. Overwrites any previously-stored value at the
     * same slot. Throws on storage failure (locked Keystore, Keychain
     * access denied, etc.).
     */
    suspend fun save(token: String)

    /**
     * Return the currently-stored token, or `null` when no token is
     * stored or the underlying store rejected the read.
     */
    suspend fun read(): String?

    /**
     * Remove the stored token. No-op if nothing is stored.
     */
    suspend fun clear()
}
