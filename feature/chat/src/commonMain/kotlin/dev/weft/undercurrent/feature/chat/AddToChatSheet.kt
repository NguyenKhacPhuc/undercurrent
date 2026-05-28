package dev.weft.undercurrent.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.designsystem.colors
import dev.weft.undercurrent.core.model.AppPalette
import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.core.model.ThemeMode
import kotlinx.coroutines.launch

/**
 * "Add to Chat" bottom sheet — the `+` button on the input row
 * opens this. Three rows: mini-apps chip strip, Choose style drill-
 * down, inline Theme picker, Connectors drill-down.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/chat/AddToChatSheet.kt`. Adjustments:
 *   - Theme types from `:core:model` + `:core:design-system`.
 *   - `MiniApp` from `:core:model`.
 *   - No behavioral changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToChatSheet(
    activePersonaLabel: String,
    activePalette: AppPalette,
    activeMode: ThemeMode,
    connectedIntegrationsCount: Int,
    miniApps: List<MiniApp>,
    onSelectPalette: (AppPalette) -> Unit,
    onSelectMode: (ThemeMode) -> Unit,
    onShowPersonas: () -> Unit,
    onShowIntegrations: () -> Unit,
    onShowMiniApps: () -> Unit,
    onInvokeMiniApp: (MiniApp) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun closeThen(block: () -> Unit) {
        scope.launch {
            sheetState.hide()
            onDismiss()
            block()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        dragHandle = null,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Add to Chat",
                    style = typography.serifBody.copy(
                        color = colors.ink,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.align(Alignment.Center),
                )
                CloseChip(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart))
            }
            Spacer(Modifier.height(20.dp))

            if (miniApps.isNotEmpty()) {
                MiniAppsChipRow(
                    miniApps = miniApps,
                    onInvoke = { miniApp -> closeThen { onInvokeMiniApp(miniApp) } },
                    onShowAll = { closeThen(onShowMiniApps) },
                )
                Spacer(Modifier.height(20.dp))
            }

            SheetLinkRow(
                label = "Choose style",
                trailing = activePersonaLabel,
                onClick = { closeThen(onShowPersonas) },
            )
            Spacer(Modifier.height(8.dp))

            SectionLabel(text = "THEME")
            Spacer(Modifier.height(8.dp))
            PalettePicker(selected = activePalette, onSelect = onSelectPalette)
            Spacer(Modifier.height(10.dp))
            ModeSegmented(selected = activeMode, onSelect = onSelectMode)
            Spacer(Modifier.height(16.dp))

            SheetLinkRow(
                label = "Connectors",
                trailing = when (connectedIntegrationsCount) {
                    0 -> "None"
                    1 -> "1 connected"
                    else -> "$connectedIntegrationsCount connected"
                },
                onClick = { closeThen(onShowIntegrations) },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MiniAppsChipRow(
    miniApps: List<MiniApp>,
    onInvoke: (MiniApp) -> Unit,
    onShowAll: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    val sorted = remember(miniApps) {
        miniApps.sortedWith(
            compareByDescending<MiniApp> { it.usageCount }
                .thenByDescending { it.createdAtEpochMs },
        )
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "MINI APPS",
                style = typography.sansLabel.copy(color = colors.inkSubtle),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Manage",
                style = typography.sansLabel.copy(
                    color = colors.ink,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier
                    .clip(UndercurrentTheme.shapes.xsmall)
                    .clickable(onClick = onShowAll)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sorted, key = { it.id }) { miniApp ->
                MiniAppChip(miniApp = miniApp, onClick = { onInvoke(miniApp) })
            }
        }
    }
}

@Composable
private fun MiniAppChip(miniApp: MiniApp, onClick: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Row(
        modifier = Modifier
            .clip(shapes.medium)
            .background(colors.surface)
            .border(width = 1.dp, color = colors.divider, shape = shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = miniApp.emoji,
            style = typography.serifBody.copy(color = colors.ink, fontSize = 18.sp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = miniApp.name,
            style = typography.serifBody.copy(
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun SheetLinkRow(label: String, trailing: String, onClick: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = typography.serifBody.copy(
                color = colors.ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = trailing,
            style = typography.sansSmall.copy(color = colors.inkSubtle),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "›",
            style = typography.sansHeader.copy(color = colors.inkSubtle, fontSize = 18.sp),
        )
    }
}

@Composable
private fun PalettePicker(selected: AppPalette, onSelect: (AppPalette) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppPalette.entries.chunked(2).forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowEntries.forEach { palette ->
                    PaletteCard(
                        palette = palette,
                        selected = palette == selected,
                        onClick = { onSelect(palette) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                if (rowEntries.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PaletteCard(
    palette: AppPalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    val swatchColors = palette.colors(dark = true)
    Row(
        modifier = modifier
            .clip(shapes.small)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.ink else colors.divider,
                shape = shapes.small,
            )
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(shapes.xsmall)
                .background(swatchColors.surface)
                .border(
                    width = 1.dp,
                    color = swatchColors.divider,
                    shape = shapes.xsmall,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(shapes.xsmall)
                    .background(swatchColors.accent.takeIf { it != Color.Transparent } ?: swatchColors.ink),
            )
        }
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = palette.displayName,
                style = typography.serifBody.copy(
                    color = colors.ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = palette.tagline,
                style = typography.sansSmall.copy(
                    color = colors.inkSubtle,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                ),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ModeSegmented(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.small)
            .background(colors.surfaceMuted)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            val active = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shapes.xsmall)
                    .background(if (active) colors.ink else Color.Transparent)
                    .clickable(onClick = { onSelect(mode) })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = mode.displayName,
                    style = typography.sansLabel.copy(
                        color = if (active) colors.background else colors.ink,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Text(
        text = text,
        style = typography.sansLabel.copy(color = colors.inkSubtle),
        modifier = Modifier.padding(start = 14.dp),
    )
}

@Composable
private fun CloseChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.surfaceMuted)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "✕",
            style = typography.sansHeader.copy(
                color = colors.ink,
                fontSize = 14.sp,
            ),
        )
    }
}
