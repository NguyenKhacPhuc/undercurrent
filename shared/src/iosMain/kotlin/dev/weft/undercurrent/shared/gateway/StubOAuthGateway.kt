package dev.weft.undercurrent.shared.gateway

/**
 * iOS stub. Integrations require Custom Tabs (Android) or
 * ASWebAuthenticationSession (iOS, TBD). For v1 the iOS shell exposes no
 * integrations, so [authorize] returns [OAuthResult.UserCancelled] and
 * token-store writes are no-ops.
 */
class StubOAuthGateway : OAuthGateway {
    override suspend fun authorize(config: OAuthConfig): OAuthResult = OAuthResult.UserCancelled

    override suspend fun putTokens(integrationId: String, tokens: OAuthTokens) = Unit

    override suspend fun removeTokens(integrationId: String) = Unit
}
