package dev.weft.undercurrent.feature.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.designsystem.colors
import dev.weft.undercurrent.core.model.AppPalette
import dev.weft.undercurrent.core.model.ThemeMode
import dev.weft.undercurrent.core.ui.ScreenScaffold
import dev.weft.undercurrent.core.ui.SectionLabel

/**
 * Appearance sub-screen — palette + light/dark mode. Drill-down from
 * Settings → Appearance (or wherever the host wires it). Both controls
 * are about the same axis (how the app looks), so they share one
 * screen instead of two trivial drill-downs.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/settings/AppearanceScreen.kt`. Adjustments:
 *   - `dev.weft.undercurrent.theme.*` → `:core:design-system` /
 *     `:core:model` mirrors.
 *   - Stateless screen — no VM, no gateway. Callers own selection
 *     state (today: the root AppStore via ThemeRepository).
 */
@Composable
public fun AppearanceScreen(
    selectedPalette: AppPalette,
    selectedMode: ThemeMode,
    onPaletteSelected: (AppPalette) -> Unit,
    onModeSelected: (ThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Appearance", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("theme-label") { SectionLabel(text = "Theme") }
            items(AppPalette.entries) { palette ->
                PaletteRow(
                    palette = palette,
                    selected = palette == selectedPalette,
                    onClick = { onPaletteSelected(palette) },
                )
            }
            item("mode-spacer") { Spacer(Modifier.height(16.dp)) }
            item("mode-label") { SectionLabel(text = "Mode") }
            item("mode-control") {
                ModeSegmented(
                    selected = selectedMode,
                    onSelected = onModeSelected,
                )
            }
        }
    }
}

@Composable
private fun PaletteRow(
    palette: AppPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Use the palette's own light colors for the swatch preview so users
    // see what they're switching TO. Light is shown regardless of system
    // dark setting because comparing palettes is easier in a single mode.
    val previewColors = palette.colors(dark = false)
    val accentColor = UndercurrentTheme.colors.accent
    val borderColor = if (selected) accentColor else UndercurrentTheme.colors.divider

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(UndercurrentTheme.shapes.medium)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = UndercurrentTheme.shapes.medium,
            )
            .background(UndercurrentTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SwatchDot(color = previewColors.background)
        Spacer(Modifier.width(6.dp))
        SwatchDot(color = previewColors.ink)
        Spacer(Modifier.width(6.dp))
        SwatchDot(color = previewColors.accent)

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = palette.displayName,
                style = UndercurrentTheme.typography.sansHeader,
                color = UndercurrentTheme.colors.ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = palette.tagline,
                style = UndercurrentTheme.typography.sansSmall,
                color = UndercurrentTheme.colors.inkMuted,
            )
        }

        if (selected) {
            Text(
                text = "✓",
                style = UndercurrentTheme.typography.sansHeader,
                color = accentColor,
            )
        }
    }
}

@Composable
private fun SwatchDot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 1.dp,
                color = UndercurrentTheme.colors.divider,
                shape = CircleShape,
            ),
    )
}

@Composable
private fun ModeSegmented(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
) {
    val modes = ThemeMode.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                label = { Text(mode.displayName) },
            )
        }
    }
}
