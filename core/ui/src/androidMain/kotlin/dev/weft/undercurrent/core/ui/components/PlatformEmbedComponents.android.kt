package dev.weft.undercurrent.core.ui.components

import dev.weft.compose.components.HtmlComponent
import dev.weft.compose.components.MiniAppActionInvoker
import dev.weft.compose.components.MiniAppScopeResolver
import dev.weft.compose.components.MiniAppStateStore
import dev.weft.compose.components.WebViewComponent
import dev.weft.compose.components.WeftComponent

/**
 * Android contribution to the embed-style palette: a bridged
 * [HtmlComponent] (backed by `android.webkit.WebView`) plus
 * [WebViewComponent]. Passing the host bindings makes the `window.weft`
 * bridge live for script-enabled mini-apps; null bindings keep it
 * sandboxed.
 */
actual fun platformEmbedComponents(
    invoker: MiniAppActionInvoker?,
    scopeResolver: MiniAppScopeResolver?,
    stateStore: MiniAppStateStore?,
): List<WeftComponent<*>> = listOf(
    WebViewComponent(),
    HtmlComponent(invoker = invoker, scopeResolver = scopeResolver, stateStore = stateStore),
)
