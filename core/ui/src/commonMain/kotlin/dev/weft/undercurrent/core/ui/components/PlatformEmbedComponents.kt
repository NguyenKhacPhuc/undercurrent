package dev.weft.undercurrent.core.ui.components

import dev.weft.compose.components.MiniAppActionInvoker
import dev.weft.compose.components.MiniAppAssistantHandler
import dev.weft.compose.components.MiniAppScopeResolver
import dev.weft.compose.components.MiniAppStateStore
import dev.weft.compose.components.WeftComponent

/**
 * Platform-supplied embed-style WeftComponents (HTML / WebView).
 *
 * A function (not a `val`) so the host can hand the bridge bindings to
 * the `HtmlComponent`: when an HTML mini-app runs scripts, its
 * `window.weft` bridge routes `callTool` through [invoker], gates it via
 * [scopeResolver], and persists state via [stateStore]. All three
 * default to `null` — an ungated, sandboxed `HtmlComponent`, exactly the
 * pre-bridge behavior — so callers that don't wire the bindings (e.g.
 * previews) get the safe default.
 *
 *  - Android: `HtmlComponent` + `WebViewComponent` backed by
 *    `android.webkit.WebView`.
 *  - iOS: `HtmlComponent` + `WebViewComponent` backed by `WKWebView`
 *    via Compose Multiplatform's `UIKitView` interop.
 *
 * The agent emits the same `{"type": "Html"|"WebView", …}` payload
 * regardless of target — only the renderer differs.
 */
expect fun platformEmbedComponents(
    invoker: MiniAppActionInvoker? = null,
    scopeResolver: MiniAppScopeResolver? = null,
    stateStore: MiniAppStateStore? = null,
    assistant: MiniAppAssistantHandler? = null,
): List<WeftComponent<*>>
