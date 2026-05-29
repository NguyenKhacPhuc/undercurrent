package dev.weft.undercurrent.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.Serializable
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebpagePreferences

// =============================================================================
// IosWebViewComponent — load a URL inside a WKWebView (iOS counterpart to
// the substrate's :android-compose-defaults WebViewComponent).
// =============================================================================

@Serializable
internal data class IosWebViewProps(
    val url: String,
    /** Token: "md" (300dp), "lg" (500dp), "xl" (700dp), "fill" (default — 600dp). */
    val height: String = "fill",
    val title: String = "",
)

internal class IosWebViewComponent : WeftComponent<IosWebViewProps>(
    name = "WebView",
    description = "Embed a web page inside the app (user stays in the app, sees your top bar). " +
        "Required: url. Optional: height (token, default 'fill'), title. Use this when content " +
        "needs to come from the web but the user shouldn't have to leave the app — Wikipedia " +
        "articles, news pages, generic search results. Prefer rendering proper UI components " +
        "over WebView; prefer WebView over external_open_url.",
    category = ComponentCategory.EMBED,
    propsSerializer = IosWebViewProps.serializer(),
) {
    @OptIn(ExperimentalForeignApi::class)
    @Composable
    override fun Render(
        props: IosWebViewProps,
        children: @Composable () -> Unit,
        onEvent: (ComponentEvent) -> Unit,
    ) {
        val heightMod = when (props.height.lowercase()) {
            "md" -> Modifier.height(WEBVIEW_MD_DP.dp)
            "lg" -> Modifier.height(WEBVIEW_LG_DP.dp)
            "xl" -> Modifier.height(WEBVIEW_XL_DP.dp)
            else -> Modifier.height(WEBVIEW_DEFAULT_HEIGHT_DP.dp)
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            if (props.title.isNotBlank()) {
                Text(
                    text = props.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            UIKitView(
                factory = {
                    val webView = WKWebView(
                        frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
                        configuration = WKWebViewConfiguration(),
                    )
                    val nsurl = NSURL.URLWithString(props.url)
                    if (nsurl != null) {
                        webView.loadRequest(NSURLRequest.requestWithURL(nsurl))
                    }
                    webView
                },
                modifier = Modifier.fillMaxWidth().then(heightMod),
                update = { webView ->
                    val current = webView.URL?.absoluteString
                    if (current != props.url) {
                        val nsurl = NSURL.URLWithString(props.url)
                        if (nsurl != null) {
                            webView.loadRequest(NSURLRequest.requestWithURL(nsurl))
                        }
                    }
                },
            )
        }
    }
}

// =============================================================================
// IosHtmlComponent — render a raw HTML snippet (iOS counterpart to the
// substrate's :android-compose-defaults HtmlComponent).
//
// Mirrors the substrate's HtmlProps shape so the agent emits the same
// `{"type": "Html", "props": {…}}` payload regardless of target — the
// platform-specific renderer just deserializes it locally.
// =============================================================================

@Serializable
internal data class IosHtmlProps(
    /** The HTML content to render. Supports basic tags + inline CSS. */
    val html: String,
    /**
     * Token: "wrap" (200dp), "md" (300dp), "lg" (500dp), "xl" (700dp),
     * "fill" (600dp full-width). Default is content-aware: "wrap" for
     * the rich-text path, "xl" for the interactive path (runScripts).
     */
    val height: String = "default",
    val title: String = "",
    /**
     * If true, JavaScript runs inside this HTML — enables self-contained
     * interactive widgets (calculator, countdown, animations, drag-drop).
     * Default false (safer; display-only).
     */
    val runScripts: Boolean = false,
)

internal class IosHtmlComponent : WeftComponent<IosHtmlProps>(
    name = "Html",
    description = "Render a raw HTML snippet inline (no URL). Required: html (string). " +
        "Optional: title, height (token), runScripts (default false). " +
        "Use for: rich text / prose with formatting (lists, bold, headings, links), " +
        "and — when runScripts=true — self-contained interactive widgets (calculator, " +
        "countdown, animations, drag-drop). With runScripts=true, JS is sandboxed: " +
        "no native bridge, no cross-origin XHR. " +
        "Use Material components instead when the UI needs to talk to the agent (fire events) " +
        "or trigger device features (notifications, sharing, calendar, etc).",
    category = ComponentCategory.EMBED,
    propsSerializer = IosHtmlProps.serializer(),
    example = """{"type": "Html", "props": {"html": "<h2>Summary</h2><ul><li>Point one</li><li>Point two with <b>emphasis</b></li></ul>"}}""",
) {
    @OptIn(ExperimentalForeignApi::class)
    @Composable
    override fun Render(
        props: IosHtmlProps,
        children: @Composable () -> Unit,
        onEvent: (ComponentEvent) -> Unit,
    ) {
        // Resolve "default" against runScripts — same logic the substrate's
        // Android HtmlComponent applies. Interactive widgets need room for
        // canvas + controls; rich-text snippets do not.
        val resolvedHeight = when (props.height.lowercase()) {
            "default" -> if (props.runScripts) "xl" else "wrap"
            else -> props.height.lowercase()
        }
        val heightMod = when (resolvedHeight) {
            "md" -> Modifier.height(HTML_MD_DP.dp)
            "lg" -> Modifier.height(HTML_LG_DP.dp)
            "xl" -> Modifier.height(HTML_XL_DP.dp)
            "fill" -> Modifier.fillMaxWidth().height(HTML_FILL_DP.dp)
            else -> Modifier.height(HTML_WRAP_DP.dp)
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            if (props.title.isNotBlank()) {
                Text(
                    text = props.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            UIKitView(
                factory = {
                    val config = WKWebViewConfiguration()
                    // JS opt-in via the modern per-navigation API
                    // (WKPreferences.javaScriptEnabled was deprecated
                    // in iOS 14 and dropped from the K/N bindings).
                    config.defaultWebpagePreferences = WKWebpagePreferences().apply {
                        allowsContentJavaScript = props.runScripts
                    }
                    val webView = WKWebView(
                        frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
                        configuration = config,
                    )
                    webView.loadHTMLString(props.html, baseURL = null)
                    webView
                },
                modifier = Modifier.fillMaxWidth().then(heightMod),
                update = { webView ->
                    // Reload when html OR runScripts changes. WKWebView's
                    // preferences are configured at init time; runScripts
                    // change requires a fresh view, but loadHTMLString
                    // recreates the document either way.
                    webView.loadHTMLString(props.html, baseURL = null)
                },
            )
        }
    }
}

internal val IosEmbedComponents: List<WeftComponent<*>> = listOf(
    IosWebViewComponent(),
    IosHtmlComponent(),
)

// ─── Dimension tokens (mirror the substrate's Embed.kt) ─────────────────

private const val WEBVIEW_DEFAULT_HEIGHT_DP = 600
private const val WEBVIEW_MD_DP = 300
private const val WEBVIEW_LG_DP = 500
private const val WEBVIEW_XL_DP = 700

private const val HTML_WRAP_DP = 200
private const val HTML_MD_DP = 300
private const val HTML_LG_DP = 500
private const val HTML_XL_DP = 700
private const val HTML_FILL_DP = 600
