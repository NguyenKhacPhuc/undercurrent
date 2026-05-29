package dev.weft.undercurrent.core.ui.components

import dev.weft.compose.components.WeftComponent

/**
 * Platform-supplied embed-style WeftComponents (HTML / WebView).
 *
 * Lives as `expect val` so the commonMain `undercurrentComponents`
 * factory can reference it once and let each platform pull from its
 * own substrate-supplied source. Both targets now route to
 * `:compose-defaults` `EmbedComponents`:
 *
 *  - Android: `HtmlComponent` + `WebViewComponent` backed by
 *    `android.webkit.WebView` + `androidx.browser.customtabs`.
 *  - iOS: `HtmlComponent` + `WebViewComponent` backed by `WKWebView`
 *    via Compose Multiplatform's `UIKitView` interop.
 *
 * The agent emits the same `{"type": "Html"|"WebView", …}` payload
 * regardless of target — only the renderer differs.
 */
expect val platformEmbedComponents: List<WeftComponent<*>>
