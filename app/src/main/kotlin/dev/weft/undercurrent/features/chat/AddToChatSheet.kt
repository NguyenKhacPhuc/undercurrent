package dev.weft.undercurrent.features.chat

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
import dev.weft.undercurrent.features.savedfeatures.SavedFeature
import dev.weft.undercurrent.theme.AppPalette
import dev.weft.undercurrent.theme.ThemeMode
import dev.weft.undercurrent.theme.UndercurrentTheme
import kotlinx.coroutines.launch

/**
 * "Add to Chat" bottom sheet — the `+` button on the input row opens this.
 *
 * Three rows, matching the in-chat configuration knobs the user is most
 * likely to want to tweak mid-conversation:
 *
 *  - **Choose style** → drills into the Personas screen. Trailing label
 *    shows the active voice (and role if set), so the user can read off
 *    the current state without opening anything.
 *  - **Theme** → inline picker. Tap a palette chip or a mode chip and
 *    the app recolors immediately via the same DataStore-backed flow
 *    Settings uses. No drill-down; the picker fits in the sheet.
 *  - **Connectors** → drills into the Integrations screen. Trailing
 *    label shows the connected-integration count.
 *
 * No Camera / Photos / Files cards (yet) — multimodal user input isn't
 * wired up. When it lands, prepend a `QuickActionCard` row at the top
 * matching the screenshot's layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddToChatSheet(
    activePersonaLabel: String,
    activePalette: AppPalette,
    activeMode: ThemeMode,
    connectedIntegrationsCount: Int,
    savedFeatures: List<SavedFeature>,
    onSelectPalette: (AppPalette) -> Unit,
    onSelectMode: (ThemeMode) -> Unit,
    onShowPersonas: () -> Unit,
    onShowIntegrations: () -> Unit,
    onShowSavedFeatures: () -> Unit,
    onInvokeFeature: (SavedFeature) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Helper — animates the sheet closed before running [block]. Using
    // `onDismissRequest` directly would cut the slide-down short.
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
            // Header row — close button on the left, centered title.
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

            // ─── Saved features chip row ────────────────────────────────
            // Most-used-first horizontal scroll. Only rendered when the
            // user has at least one saved feature — empty state would
            // just be visual noise above the more important rows.
            if (savedFeatures.isNotEmpty()) {
                SavedFeaturesChipRow(
                    features = savedFeatures,
                    onInvoke = { feature ->
                        // Close the sheet first; the actual dispatch
                        // happens via the host's onInvokeFeature
                        // (ChatScreen → AppStore via SendChat).
                        closeThen { onInvokeFeature(feature) }
                    },
                    onShowAll = { closeThen(onShowSavedFeatures) },
                )
                Spacer(Modifier.height(20.dp))
            }

            // ─── Row: Choose style ──────────────────────────────────────
            SheetLinkRow(
                label = "Choose style",
                trailing = activePersonaLabel,
                onClick = { closeThen(onShowPersonas) },
            )
            Spacer(Modifier.height(8.dp))

            // ─── Section: Theme (inline) ────────────────────────────────
            SectionLabel(text = "THEME")
            Spacer(Modifier.height(8.dp))
            PalettePicker(
                selected = activePalette,
                onSelect = onSelectPalette,
            )
            Spacer(Modifier.height(10.dp))
            ModeSegmented(
                selected = activeMode,
                onSelect = onSelectMode,
            )
            Spacer(Modifier.height(16.dp))

            // ─── Row: Connectors ────────────────────────────────────────
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

/**
 * Horizontal-scroll row of saved-feature chips at the top of the
 * sheet. Each chip = one saved prompt the user can fire with one tap.
 * Sorted most-used-first so muscle memory works ("the leftmost chip
 * is the one I always tap"). A trailing "Manage" pill drills into the
 * full management screen.
 */
@Composable
private fun SavedFeaturesChipRow(
    features: List<SavedFeature>,
    onInvoke: (SavedFeature) -> Unit,
    onShowAll: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    // Sort happens here (vs. in the repo) because "most used" is a
    // view-time concern — the repo persists creation order, and other
    // screens (the management list) want stable alphabetical / by-
    // recent ordering instead.
    val sorted = remember(features) {
        features.sortedWith(
            compareByDescending<SavedFeature> { it.usageCount }
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
                text = "MY FEATURES",
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
            items(sorted, key = { it.id }) { feature ->
                FeatureChip(feature = feature, onClick = { onInvoke(feature) })
            }
        }
    }
}

@Composable
private fun FeatureChip(
    feature: SavedFeature,
    onClick: () -> Unit,
) {
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
            text = feature.emoji,
            style = typography.serifBody.copy(
                color = colors.ink,
                fontSize = 18.sp,
            ),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = feature.name,
            style = typography.serifBody.copy(
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
        )
    }
}

/**
 * Drill-down row used by Choose style + Connectors. Label on the left,
 * current value chip on the right, chevron after. Tapping anywhere on
 * the row fires [onClick].
 */
@Composable
private fun SheetLinkRow(
    label: String,
    trailing: String,
    onClick: () -> Unit,
) {
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

/**
 * Two-row palette picker. Each tile shows the palette's name + tagline;
 * the active one gets a 2dp ink border. We render the swatches as
 * background-color blocks taken from each palette's dark variant — gives
 * the user a visual preview without the full theme-switch overhead.
 */
@Composable
private fun PalettePicker(
    selected: AppPalette,
    onSelect: (AppPalette) -> Unit,
) {
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
    // Pull the swatch directly from the palette's dark variant so the
    // user sees something representative even if they're currently on
    // light mode. (We use dark because the inks are richer there.)
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
        // Tiny swatch square — surface (paper-like) over accent (the
        // chromatic dot of each palette). Two colors are enough to
        // identify the palette at a glance.
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

/**
 * Segmented control: Auto / Light / Dark. Active segment fills with
 * `colors.ink`; inactive segments sit on `surfaceMuted`. Visually
 * matches the screenshot's toggle feel without being a real switch
 * — three states don't fit the toggle metaphor.
 */
@Composable
private fun ModeSegmented(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
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

/**
 * Small uppercase section label — used above the inline theme picker.
 * Matches the look of section labels elsewhere in the app
 * (PROVIDER / VOICE / etc.).
 */
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

/**
 * Circular X close affordance — matches the screenshot's top-left close.
 * We render it as a small surface chip with an X glyph so it reads as
 * tappable without pulling in a Material icon library entry.
 */
@Composable
private fun CloseChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
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
