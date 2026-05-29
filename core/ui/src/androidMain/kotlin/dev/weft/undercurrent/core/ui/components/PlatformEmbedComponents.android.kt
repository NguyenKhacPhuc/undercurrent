package dev.weft.undercurrent.core.ui.components

import dev.weft.compose.components.EmbedComponents
import dev.weft.compose.components.WeftComponent

/**
 * Android contribution to the embed-style palette: HtmlComponent +
 * WebViewComponent, both pulled directly from the substrate's
 * `:android-compose-defaults` (which can't go commonMain because it
 * wraps `android.webkit.WebView` + `androidx.browser.customtabs`).
 */
actual val platformEmbedComponents: List<WeftComponent<*>> = EmbedComponents
