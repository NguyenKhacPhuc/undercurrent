package dev.weft.undercurrent.data.weft.tools

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.serialization.typeToken
import dev.weft.tools.WeftContext
import dev.weft.tools.WeftTool
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Search the live web via the Tavily API and return ranked results.
 *
 * This is the host's answer to "search before asserting present-day
 * facts" — the substrate ships `network_fetch` (raw HTTP to one known
 * URL), but has no search primitive. The agent preamble's honesty +
 * freshness rules lean on this tool existing.
 *
 * ### Why its own HttpClient
 *
 * The app's Koin-registered [HttpClient] is the authenticated BE client:
 * it attaches the user's session token, forces the Railway base URL, and
 * throws on any non-2xx via its response validator. All three are wrong
 * for a third-party search endpoint, so this tool owns a plain,
 * unauthenticated client. (It also bypasses the substrate's network
 * allowlist by design — the Tavily host is fixed, not user-supplied.)
 *
 * ### Key handling
 *
 * The Tavily key is read from `BuildConfig.TAVILY_API_KEY` at the DI
 * site (sourced from `local.properties` / env at build time) and passed
 * in here. A blank key yields a structured `NO_API_KEY` error rather
 * than a crash, so debug builds without a key degrade gracefully.
 */
class WebSearchTool(
    ctx: WeftContext,
    private val apiKey: String,
) : WeftTool<WebSearchTool.Args, WebSearchTool.Result>(
    ctx = ctx,
    argsType = typeToken<Args>(),
    resultType = typeToken<Result>(),
    descriptor = ToolDescriptor(
        name = "web_search",
        description = "Search the live web and return ranked results with titles, " +
            "URLs, and text snippets. Use for any present-day fact that can change — " +
            "prices, who holds a role, latest releases, recent events, niche details " +
            "not in your training. NOT for fetching one known URL — use network_fetch.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                "query",
                "What to search for, phrased as a natural-language query.",
                ToolParameterType.String,
            ),
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor(
                "maxResults",
                "How many results to return (1–10). Defaults to 5.",
                ToolParameterType.Integer,
            ),
            ToolParameterDescriptor(
                "recency",
                "Restrict to recent results: 'day', 'week', 'month', or 'year'. " +
                    "Omit for no time filter.",
                ToolParameterType.String,
            ),
        ),
    ),
) {

    @Serializable
    data class Args(
        val query: String,
        val maxResults: Int = 5,
        val recency: String? = null,
    )

    @Serializable
    data class Result(
        val results: List<Hit> = emptyList(),
        val error: Boolean = false,
        val code: String? = null,
        val message: String? = null,
    )

    @Serializable
    data class Hit(
        val title: String,
        val url: String,
        val snippet: String,
        val publishedDate: String? = null,
    )

    override suspend fun executeWeft(args: Args): Result {
        if (apiKey.isBlank()) {
            return Result(
                error = true,
                code = "NO_API_KEY",
                message = "Web search isn't configured for this build (missing " +
                    "TAVILY_API_KEY). Tell the user web search is unavailable here.",
            )
        }
        val request = TavilyRequest(
            apiKey = apiKey,
            query = args.query,
            maxResults = args.maxResults.coerceIn(1, 10),
            timeRange = args.recency?.lowercase()?.takeIf { it in VALID_RANGES },
        )
        return try {
            val raw = client.post(ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(TavilyRequest.serializer(), request))
            }.bodyAsText()
            val response = json.decodeFromString(TavilyResponse.serializer(), raw)
            Result(
                results = response.results.map {
                    Hit(
                        title = it.title,
                        url = it.url,
                        snippet = it.content,
                        publishedDate = it.publishedDate,
                    )
                },
            )
        } catch (e: Throwable) {
            Result(
                error = true,
                code = "SEARCH_ERROR",
                message = e.message ?: e::class.simpleName.orEmpty(),
            )
        }
    }

    @Serializable
    private data class TavilyRequest(
        @SerialName("api_key") val apiKey: String,
        val query: String,
        @SerialName("max_results") val maxResults: Int,
        @SerialName("search_depth") val searchDepth: String = "basic",
        @SerialName("time_range") val timeRange: String? = null,
    )

    @Serializable
    private data class TavilyResponse(
        val results: List<TavilyHit> = emptyList(),
    )

    @Serializable
    private data class TavilyHit(
        val title: String = "",
        val url: String = "",
        val content: String = "",
        @SerialName("published_date") val publishedDate: String? = null,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 15_000
        }
    }

    private companion object {
        const val ENDPOINT = "https://api.tavily.com/search"
        val VALID_RANGES = setOf("day", "week", "month", "year")
    }
}
