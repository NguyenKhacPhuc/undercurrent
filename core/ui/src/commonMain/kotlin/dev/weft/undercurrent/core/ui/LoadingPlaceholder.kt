package dev.weft.undercurrent.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme

/**
 * Centered "Undercurrent / Starting…" splash. Rendered while the
 * agent + boot flow initializes and the destination screen hasn't
 * been picked yet.
 *
 * KMP — commonMain. Moved from `app/.../core/LoadingPlaceholder.kt`.
 * The `appName` + `subtitle` are parameterized so :core:ui stays
 * app-agnostic — `:composeApp` passes "Undercurrent" + "Starting…".
 */
@Composable
fun LoadingPlaceholder(
    appName: String = "Undercurrent",
    subtitle: String = "Starting…",
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = appName,
            style = typography.sansHeader.copy(color = colors.ink),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = typography.sansSmall.copy(color = colors.inkMuted),
        )
    }
}
