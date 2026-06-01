package dev.weft.undercurrent.feature.integrations

import dev.weft.undercurrent.core.domain.OAuthConfig

/**
 * One supported third-party integration. Each integration is gated by
 * an OAuth 2.0 + PKCE flow and, once connected, exposes its
 * capabilities to the agent as MCP tools.
 *
 * The substrate's MCP server config gets built lazily from this
 * descriptor at runtime construction time (see the host app's DI
 * module). We separate the descriptor from the runtime config so the
 * Integrations screen can render every supported integration even if
 * the user hasn't enabled it yet.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/integrations/Integration.kt`. Uses the
 * commonMain [OAuthConfig] mirror from `:shared/gateway` (was Weft's
 * `dev.weft.oauth.OAuthConfig`).
 *
 * @property id stable identifier — used as the OAuth `connectorId`
 *   (token-store alias) AND the MCP server `name` (tool prefix).
 * @property displayName user-facing label.
 * @property tagline one-liner shown under the name in the picker.
 * @property mcpUrl the integration's MCP server endpoint.
 * @property oauth OAuth flow parameters. PKCE makes client id non-
 *   secret; embed the production value or expose an override.
 */
data class Integration(
    val id: String,
    val displayName: String,
    val tagline: String,
    val mcpUrl: String,
    val oauth: OAuthConfig,
)

/**
 * The shared OAuth redirect URI for every integration. Registered as
 * an `intent-filter` on the host activity. Per-connector path suffix
 * (`/linear`, `/notion`, …) lets the activity disambiguate redirects.
 */
const val OAUTH_REDIRECT_SCHEME: String = "undercurrent"
const val OAUTH_REDIRECT_HOST: String = "oauth"
const val OAUTH_REDIRECT_BASE: String =
    "$OAUTH_REDIRECT_SCHEME://$OAUTH_REDIRECT_HOST"

/**
 * The shipped integration catalog. Add entries here to expand
 * support. Each entry's `oauth.clientId` is a placeholder — register
 * a real OAuth app at the provider's developer portal, set
 * [OAUTH_REDIRECT_BASE]/{id} as the redirect URI, and substitute the
 * client id below.
 */
object Integrations {

    val Linear: Integration = Integration(
        id = "linear",
        displayName = "Linear",
        // Localized in the UI via Integration.taglineRes() (IntegrationsScreen).
        tagline = "",
        mcpUrl = "https://mcp.linear.app/mcp",
        oauth = OAuthConfig(
            clientId = LINEAR_OAUTH_CLIENT_ID,
            authorizationEndpoint = "https://linear.app/oauth/authorize",
            tokenEndpoint = "https://api.linear.app/oauth/token",
            redirectUri = "$OAUTH_REDIRECT_BASE/linear",
            scopes = listOf("read", "write"),
        ),
    )

    val All: List<Integration> = listOf(Linear)

    fun byId(id: String): Integration? = All.firstOrNull { it.id == id }
}

private const val LINEAR_OAUTH_CLIENT_ID: String = "TODO_LINEAR_OAUTH_CLIENT_ID"
