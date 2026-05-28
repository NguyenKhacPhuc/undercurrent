package dev.weft.undercurrent.data.weft

import dev.weft.oauth.OAuthClient
import dev.weft.oauth.OAuthTokenStore
import dev.weft.oauth.TokenSet
import dev.weft.undercurrent.shared.gateway.OAuthConfig
import dev.weft.undercurrent.shared.gateway.OAuthGateway
import dev.weft.undercurrent.shared.gateway.OAuthResult
import dev.weft.undercurrent.shared.gateway.OAuthTokens
import dev.weft.oauth.OAuthConfig as WeftOAuthConfig
import dev.weft.oauth.OAuthResult as WeftOAuthResult

/**
 * Android impl of [OAuthGateway]. Translates between the commonMain
 * mirror types and Weft's `dev.weft.oauth.*` types.
 */
public class WeftOAuthGateway(
    private val oauthClient: OAuthClient,
    private val tokenStore: OAuthTokenStore,
) : OAuthGateway {

    override suspend fun authorize(config: OAuthConfig): OAuthResult =
        when (val r = oauthClient.authorize(config.toWeft())) {
            is WeftOAuthResult.Success -> OAuthResult.Success(r.tokens.toCommon())
            is WeftOAuthResult.UserCancelled -> OAuthResult.UserCancelled
            is WeftOAuthResult.ProviderError -> OAuthResult.ProviderError(r.code, r.description)
            is WeftOAuthResult.TransportError -> OAuthResult.TransportError(r.message)
            is WeftOAuthResult.StateMismatch -> OAuthResult.StateMismatch
        }

    override suspend fun putTokens(integrationId: String, tokens: OAuthTokens) {
        tokenStore.put(integrationId, tokens.toWeft())
    }

    override suspend fun removeTokens(integrationId: String) {
        tokenStore.remove(integrationId)
    }

    private fun OAuthConfig.toWeft(): WeftOAuthConfig = WeftOAuthConfig(
        clientId = clientId,
        authorizationEndpoint = authorizationEndpoint,
        tokenEndpoint = tokenEndpoint,
        redirectUri = redirectUri,
        scopes = scopes,
        extraAuthParams = extraAuthParams,
    )

    private fun TokenSet.toCommon(): OAuthTokens = OAuthTokens(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAtEpochMs = expiresAtEpochMs,
        tokenType = tokenType,
        scope = scope,
    )

    private fun OAuthTokens.toWeft(): TokenSet = TokenSet(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAtEpochMs = expiresAtEpochMs,
        tokenType = tokenType,
        scope = scope,
    )
}
