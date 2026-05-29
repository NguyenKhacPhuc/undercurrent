package dev.weft.undercurrent.core.ui.components

import dev.weft.compose.components.WeftComponent

/**
 * Platform-supplied embed-style WeftComponents (HTML / WebView).
 *
 * Lives as `expect val` so the commonMain `undercurrentComponents`
 * factory can reference it once and let each platform decide what to
 * contribute:
 *
 *  - Android: the substrate's `EmbedComponents` from
 *    `:android-compose-defaults` — `HtmlComponent` (raw HTML with
 *    optional JS) + `WebViewComponent` (URL load). Both are backed by
 *    `android.webkit.WebView` and can't go commonMain.
 *  - iOS: an empty list for v1. A `WKWebView`-backed pair is the
 *    natural follow-up but isn't required for the core palette to
 *    render — interactive games / web-page embeds just won't be
 *    available until then.
 */
expect val platformEmbedComponents: List<WeftComponent<*>>
