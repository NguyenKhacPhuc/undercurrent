package dev.weft.undercurrent.core.ext

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Open [url] in a Chrome Custom Tab — keeps the user inside the app's
 * task (back button returns to wherever they tapped), themes the toolbar
 * to match our palette so the transition isn't jarring.
 *
 * Falls back to a plain `ACTION_VIEW` intent for non-http schemes (mailto:,
 * tel:, etc.) and for devices without a CCT-capable browser installed.
 * All launches wrap in [runCatching] so a malformed URI or missing
 * resolver doesn't crash the caller.
 *
 * Shared between [MarkdownText] (assistant message link taps) and the
 * BYOK flow ("Get an API key" buttons in KeyPasteScreen + ProvidersScreen).
 */
fun openInBrowser(context: Context, url: String, toolbarColorArgb: Int) {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
    val scheme = uri.scheme?.lowercase()
    if (scheme == "http" || scheme == "https") {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(toolbarColorArgb)
                    .build(),
            )
            .build()
        runCatching { intent.launchUrl(context, uri) }
    } else {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}
