package dev.weft.undercurrent.core.ui.components

import dev.weft.compose.components.HtmlComponent
import dev.weft.compose.components.MiniAppActionInvoker
import dev.weft.compose.components.MiniAppScopeResolver
import dev.weft.compose.components.MiniAppStateStore
import dev.weft.compose.components.WebViewComponent
import dev.weft.compose.components.WeftComponent

/**
 * iOS contribution to the embed-style palette: a bridged [HtmlComponent]
 * (backed by `WKWebView` via `UIKitView`) plus [WebViewComponent].
 * Mirrors the Android actual — same bindings, same bridge behavior.
 */
actual fun platformEmbedComponents(
    invoker: MiniAppActionInvoker?,
    scopeResolver: MiniAppScopeResolver?,
    stateStore: MiniAppStateStore?,
): List<WeftComponent<*>> = listOf(
    WebViewComponent(),
    HtmlComponent(invoker = invoker, scopeResolver = scopeResolver, stateStore = stateStore),
)
