package dev.weft.undercurrent.core.ui.components

import dev.weft.compose.components.WeftComponent

/**
 * iOS contribution to the embed-style palette: empty for v1. The
 * substrate's `EmbedComponents` is `WebView`-backed (`android.webkit`)
 * and ships only in `:android-compose-defaults`. A `WKWebView`-backed
 * equivalent for iOS — `IosHtmlComponent` + `IosWebViewComponent` —
 * is the natural follow-up here, but isn't required for the rest of
 * the palette to render.
 *
 * Effect: the agent's `ui_render` palette on iOS doesn't include
 * `Html` / `WebView`, so interactive games (snake, breakout, etc.)
 * will fall back to a Text dump until the WKWebView wrapper lands.
 */
actual val platformEmbedComponents: List<WeftComponent<*>> = emptyList()
