package dev.weft.undercurrent.core.ui.components

import dev.weft.compose.components.WeftComponent

/**
 * iOS contribution to the embed-style palette: HtmlComponent +
 * WebViewComponent, both pulled directly from the substrate's
 * `:compose-defaults` (WKWebView-backed via Compose Multiplatform's
 * `UIKitView` interop). Mirrors the Android actual — same import,
 * same expression.
 */
actual val platformEmbedComponents: List<WeftComponent<*>> = dev.weft.compose.components.EmbedComponents
