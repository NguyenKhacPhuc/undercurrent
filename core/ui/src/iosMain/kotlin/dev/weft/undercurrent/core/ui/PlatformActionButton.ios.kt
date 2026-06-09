package dev.weft.undercurrent.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitViewController
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.ext.NativeViewRegistry
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * iOS rendering — bridges to a **native SwiftUI** button instead of
 * drawing with Compose. The `ComposeApp` framework can't see SwiftUI
 * types (it's built before the Swift app code), so the dependency is
 * inverted: this file declares [NativeActionButton] + [NativeViewRegistry],
 * and the Swift side (`iosApp/iosApp/ActionButtonView.swift`) conforms +
 * registers a factory at launch (`iosAppApp.swift`).
 *
 * If nothing registered (e.g. `@Preview`, or the hook was dropped), it
 * falls back to a Compose pill so the call site never breaks.
 */
@Composable
actual fun PlatformActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
) {
    val factory = NativeViewRegistry.actionButtonFactory
    if (factory == null) {
        ActionButtonComposeFallback(label, onClick, modifier, enabled)
        return
    }
    // Stable lambda handed to Swift once; always calls the latest onClick.
    val latestOnClick by rememberUpdatedState(onClick)
    val bridge = remember(factory) { factory { latestOnClick() } }
    UIKitViewController(
        factory = { bridge.viewController },
        modifier = modifier.fillMaxWidth().height(50.dp),
        // factory runs once — re-push label/enabled on each recomposition.
        update = { bridge.update(label, enabled) },
    )
}

@Composable
private fun ActionButtonComposeFallback(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(if (enabled) colors.accent else colors.divider)
            .clickable(enabled = enabled) { onClick() }
            .alpha(if (enabled) 1f else 0.6f)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typography.sansLabel.copy(color = colors.onAccent),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun PlatformActionButtonPreview() {
    UndercurrentTheme {
        // Renders the Compose fallback — the SwiftUI path is runtime-only.
        ActionButtonComposeFallback(label = "Continue", onClick = {}, modifier = Modifier, enabled = true)
    }
}
