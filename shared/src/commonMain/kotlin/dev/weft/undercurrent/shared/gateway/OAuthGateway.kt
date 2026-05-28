package dev.weft.undercurrent.shared.gateway

import kotlinx.serialization.Serializable

/**
 * OAuth 2.0 + PKCE for integrations. The Android impl in `:data:weft`
 * delegates to Weft's `OAuthClient` + `OAuthTokenStore`; iOS stub returns
 * [OAuthResult.UserCancelled] for v1 (Custom Tabs / ASWebAuthenticationSession
 * bridge arrives when iOS integrations land).
 *
 * Mirror types ([OAuthConfig], [OAuthResult], [OAuthTokens]) are
 * declared here in commonMain so feature modules never import
 * `dev.weft.oauth.*`.
 */
interface OAuthGateway {

    /**
     * Run the OAuth dance for one integration. Suspends across Custom
     * Tabs / browser. Persists nothing — caller decides what to do with
     * [OAuthResult.Success.tokens].
     */
    suspend fun authorize(config: OAuthConfig): OAuthResult

    /**
     * Persist tokens for [integrationId]. Overwrites any previous bundle.
     */
    suspend fun putTokens(integrationId: String, tokens: OAuthTokens)

    /**
     * Forget tokens for [integrationId]. No-op if nothing was stored.
     */
    suspend fun removeTokens(integrationId: String)
}

/**
 * commonMain mirror of `dev.weft.oauth.OAuthConfig`. The Android bridge
 * maps this to the Weft type before calling `OAuthClient.authorize`.
 */
@Serializable
data class OAuthConfig(
    val clientId: String,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val redirectUri: String,
    val scopes: List<String> = emptyList(),
    val extraAuthParams: Map<String, String> = emptyMap(),
)

/** Token bundle returned on success. Mirrors `dev.weft.oauth.TokenSet`. */
@Serializable
data class OAuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtEpochMs: Long = 0L,
    val tokenType: String = "Bearer",
    val scope: String? = null,
)

/** Mirror of `dev.weft.oauth.OAuthResult`. */
sealed class OAuthResult {
    data class Success(val tokens: OAuthTokens) : OAuthResult()
    data object UserCancelled : OAuthResult()
    data class ProviderError(val code: String, val description: String?) : OAuthResult()
    data class TransportError(val message: String) : OAuthResult()
    data object StateMismatch : OAuthResult()
}
