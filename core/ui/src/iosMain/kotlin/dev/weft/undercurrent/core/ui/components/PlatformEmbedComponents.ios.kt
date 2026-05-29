package dev.weft.undercurrent.core.ui.components

import dev.weft.compose.components.WeftComponent

/**
 * iOS contribution to the embed-style palette: `IosHtmlComponent` +
 * `IosWebViewComponent`, both backed by `WKWebView` via Compose
 * Multiplatform's `UIKitView` interop. Mirror the substrate's
 * Android `EmbedComponents` registration shape (same `name`, same
 * `description`, same prop schema) so the agent emits the same
 * `{"type": "Html", …}` payload regardless of target.
 */
actual val platformEmbedComponents: List<WeftComponent<*>> = IosEmbedComponents
