package dev.weft.undercurrent.features.integrations

import dev.weft.oauth.OAuthConfig

/**
 * One supported third-party integration. Each integration is gated by an
 * OAuth 2.0 + PKCE flow and, once connected, exposes its capabilities to
 * the agent as MCP tools.
 *
 * The substrate's [dev.weft.mcp.McpServerConfig] gets built lazily from
 * this descriptor at runtime construction time — see [AppModule] for the
 * actual wiring. We separate the descriptor from the runtime config so
 * the Integrations screen can render every supported integration even
 * if the user hasn't enabled it yet.
 *
 * @property id stable identifier — used as the OAuth `connectorId`
 *   (token-store alias) AND the MCP server `name` (tool prefix). Keep
 *   it short, lowercase, no whitespace. Tools surface as
 *   `<id>:<remote_tool>` in the agent's catalog (e.g. `linear:create_issue`).
 * @property displayName user-facing label.
 * @property tagline one-liner shown under the name in the picker.
 * @property mcpUrl the integration's MCP server endpoint. Verified at
 *   runtime when the user connects — bad URLs surface as connection
 *   errors during MCP `initialize`.
 * @property oauth the OAuth flow parameters. [OAuthConfig.clientId] is
 *   the public PKCE client identifier — embed the production value here
 *   (it's safe; PKCE makes the client id non-secret) or expose a
 *   user-supplied override mechanism for personal deployments.
 */
internal data class Integration(
    val id: String,
    val displayName: String,
    val tagline: String,
    val mcpUrl: String,
    val oauth: OAuthConfig,
)

/**
 * The shared OAuth redirect URI for every integration. Registered as an
 * `intent-filter` on `MainActivity` (see `AndroidManifest.xml`).
 *
 * Path-suffix per connector (`/linear`, `/notion`, …) lets the host
 * activity disambiguate which flow a given redirect belongs to without
 * having to register a separate scheme per integration.
 *
 * **Registering OAuth apps:** when you set up the OAuth app at the
 * integration's developer portal, paste the matching URI here as the
 * "Redirect URI" / "Callback URL".
 */
internal const val OAUTH_REDIRECT_SCHEME: String = "undercurrent"
internal const val OAUTH_REDIRECT_HOST: String = "oauth"
internal const val OAUTH_REDIRECT_BASE: String =
    "$OAUTH_REDIRECT_SCHEME://$OAUTH_REDIRECT_HOST"

/**
 * The shipped integration catalog. Add entries here to expand support.
 *
 * Each entry's `oauth.clientId` is a placeholder — register a real OAuth
 * app at the provider's developer portal, paste the redirect URI shown
 * in [OAUTH_REDIRECT_BASE]/{id}, and substitute the client id below.
 * Without that step the Connect button surfaces an "invalid_client"
 * error from the provider during the token exchange.
 */
internal object Integrations {

    /**
     * Linear (linear.app) — issues, projects, comments, search.
     *
     * Setup:
     *   1. Visit linear.app → Settings → API → OAuth applications.
     *   2. Create an application. Add `undercurrent://oauth/linear` as
     *      a redirect URI.
     *   3. Copy the client id into [LINEAR_OAUTH_CLIENT_ID] below.
     *   4. Optionally narrow [scopes] if "read,write" is too broad for
     *      your use case ("read" alone disables tool calls that mutate).
     */
    val Linear: Integration = Integration(
        id = "linear",
        displayName = "Linear",
        tagline = "Issues, projects, and comments via natural language.",
        mcpUrl = "https://mcp.linear.app/mcp",
        oauth = OAuthConfig(
            clientId = LINEAR_OAUTH_CLIENT_ID,
            authorizationEndpoint = "https://linear.app/oauth/authorize",
            tokenEndpoint = "https://api.linear.app/oauth/token",
            redirectUri = "$OAUTH_REDIRECT_BASE/linear",
            scopes = listOf("read", "write"),
        ),
    )

    /** Every supported integration, in display order. */
    val All: List<Integration> = listOf(Linear)

    fun byId(id: String): Integration? = All.firstOrNull { it.id == id }
}

/**
 * Placeholder — replace with a real Linear OAuth client id before
 * shipping. Defined as a top-level const (not BuildConfig) so personal
 * builds can edit it without a Gradle round-trip.
 *
 * Public-PKCE clients don't treat the client id as a secret, so
 * committing the production value here is fine. Move it to BuildConfig
 * if you want different ids per build variant.
 */
private const val LINEAR_OAUTH_CLIENT_ID: String = "TODO_LINEAR_OAUTH_CLIENT_ID"
