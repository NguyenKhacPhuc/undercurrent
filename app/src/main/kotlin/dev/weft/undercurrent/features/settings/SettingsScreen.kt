package dev.weft.undercurrent.features.settings

import dev.weft.undercurrent.ui.ScreenScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.weft.contracts.ProviderKind
import dev.weft.undercurrent.theme.UndercurrentTheme

/**
 * Settings index. Each row drills into a dedicated sub-screen — Provider
 * (accounts + models + default tier), Appearance (palette + light/dark),
 * Usage (cost + token totals). Keeps the top-level surface scannable;
 * the heavy controls live inside their respective sub-screens.
 *
 * Subtitles give the user the current value at a glance — saves a tap if
 * they're just checking, not changing.
 */
@Composable
internal fun SettingsScreen(
    activeProvider: ProviderKind,
    onShowProvider: () -> Unit,
    onShowUsage: () -> Unit,
    onShowIntegrations: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Settings", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingsLinkRow(
                    label = "Provider",
                    subtitle = activeProvider.displayName(),
                    onClick = onShowProvider,
                )
            }
            // Appearance row removed — theme controls now live in the
            // "Add to Chat" bottom sheet (chat input → `+`). The
            // Appearance screen is still in the nav graph for backward
            // compatibility but has no in-app entry point.
            item {
                SettingsLinkRow(
                    label = "Usage",
                    subtitle = "Tokens and cost",
                    onClick = onShowUsage,
                )
            }
            item {
                SettingsLinkRow(
                    label = "Integrations",
                    subtitle = "Connect third-party services",
                    onClick = onShowIntegrations,
                )
            }
        }
    }
}

/**
 * Drill-down row: bold label on top, dim subtitle below, chevron on the
 * right. Same border + surface treatment as the document-feel cards used
 * everywhere else.
 */
@Composable
private fun SettingsLinkRow(
    label: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(colors.surface)
            .border(width = 1.dp, color = colors.divider, shape = shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = typography.sansHeader.copy(color = colors.ink),
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = typography.sansSmall.copy(color = colors.inkMuted),
                )
            }
        }
        Text(
            text = "→",
            style = typography.sansHeader.copy(color = colors.inkSubtle),
        )
    }
}

private fun ProviderKind.displayName(): String = when (this) {
    ProviderKind.Anthropic -> "Anthropic"
    ProviderKind.OpenAI -> "OpenAI"
    ProviderKind.OpenRouter -> "OpenRouter"
    ProviderKind.DeepSeek -> "DeepSeek"
}
