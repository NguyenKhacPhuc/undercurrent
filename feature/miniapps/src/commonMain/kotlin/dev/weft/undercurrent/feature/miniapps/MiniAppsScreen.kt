package dev.weft.undercurrent.feature.miniapps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.miniapps_empty_body
import dev.weft.undercurrent.core.resources.miniapps_empty_cta
import dev.weft.undercurrent.core.resources.miniapps_empty_title
import dev.weft.undercurrent.core.resources.miniapps_footer_hint
import dev.weft.undercurrent.core.resources.miniapps_intro
import dev.weft.undercurrent.core.resources.miniapps_new_row_subtitle
import dev.weft.undercurrent.core.resources.miniapps_new_row_title
import dev.weft.undercurrent.core.resources.miniapps_no_preview
import dev.weft.undercurrent.core.resources.miniapps_title
import dev.weft.undercurrent.core.resources.miniapps_usage_count
import dev.weft.undercurrent.core.ui.ScreenScaffold
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Drawer-reached management screen for [MiniApp]s.
 *
 * Each saved mini-app renders as a full-width card showing its
 * cached UI tree — a non-interactive preview of what the user will
 * see when they tap to open. Tap routes to [onOpenMiniApp]; long-
 * press opens [SaveAsMiniAppDialog] for rename / edit / delete.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/miniapps/MiniAppsScreen.kt`. Adjustments:
 *   - Tree-preview rendering uses the substrate's `TreeRenderer` +
 *     `WeftComponentRegistry`, both of which live in
 *     `weft-android-compose` (Android-only). Rather than pull them
 *     into commonMain, the host passes a [treePreview] composable
 *     lambda — Android wires it to a `TreeRenderer` call; iOS can
 *     wire a placeholder or a future native renderer.
 *   - Imports from `:core:ui` / `:core:design-system` / `:core:model`.
 *
 * @param treePreview Composable that renders the cached `ui_render`
 *   JSON. Receives the raw JSON string + a click handler. The
 *   handler should be wired to [onOpenMiniApp] so the preview
 *   stays non-interactive — any tap inside falls through to "open
 *   the mini-app for real" instead of firing stale actions against
 *   the agent.
 *
 * Stateless — [MiniAppsRoute] hoists state + dispatches intents.
 */
@Composable
fun MiniAppsScreen(
    state: MiniAppsState,
    treePreview: @Composable (treeJson: String, onTap: () -> Unit) -> Unit,
    onBack: () -> Unit,
    onOpenMiniApp: (MiniApp) -> Unit,
    onStartCreator: () -> Unit = {},
    onUpdate: (id: String, name: String, emoji: String, prompt: String) -> Unit = { _, _, _, _ -> },
    onDelete: (id: String) -> Unit = {},
    onSetApprovedScopes: (id: String, scopes: Set<String>) -> Unit = { _, _ -> },
) {
    val miniApps = state.miniApps
    var editing by remember { mutableStateOf<MiniApp?>(null) }

    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    ScreenScaffold(title = stringResource(Res.string.miniapps_title), onBack = onBack) {
        if (miniApps.isEmpty()) {
            EmptyState(onStartCreator = onStartCreator)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item("intro") {
                    Text(
                        text = stringResource(Res.string.miniapps_intro),
                        style = typography.serifBody.copy(
                            color = colors.ink,
                            fontStyle = FontStyle.Italic,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        ),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                item("add-new") {
                    NewMiniAppRow(onClick = onStartCreator)
                }
                items(miniApps, key = { it.id }) { miniApp ->
                    MiniAppCard(
                        miniApp = miniApp,
                        treePreview = treePreview,
                        onOpen = { onOpenMiniApp(miniApp) },
                        onLongPress = { editing = miniApp },
                    )
                }
                item("footer") {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.miniapps_footer_hint),
                        style = typography.sansSmall.copy(color = colors.inkSubtle),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    editing?.let { miniApp ->
        SaveAsMiniAppDialog(
            initial = miniApp,
            suggestedPrompt = miniApp.triggerPrompt,
            onDismiss = { editing = null },
            onSave = { name, emoji, prompt ->
                editing = null
                onUpdate(miniApp.id, name, emoji, prompt)
            },
            onDelete = {
                editing = null
                onDelete(miniApp.id)
            },
            onSetApprovedScopes = { scopes -> onSetApprovedScopes(miniApp.id, scopes) },
        )
    }
}

@Composable
private fun EmptyState(onStartCreator: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.miniapps_empty_title),
            style = typography.serifBody.copy(
                color = colors.ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.miniapps_empty_body),
            style = typography.serifBody.copy(
                color = colors.inkMuted,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(UndercurrentTheme.shapes.medium)
                .background(colors.accent)
                .clickable(onClick = onStartCreator)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(Res.string.miniapps_empty_cta),
                style = typography.sansLabel.copy(
                    color = colors.onAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                ),
            )
        }
    }
}

@Composable
private fun NewMiniAppRow(onClick: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(UndercurrentTheme.shapes.medium)
            .background(colors.accent.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = colors.accent.copy(alpha = 0.4f),
                shape = UndercurrentTheme.shapes.medium,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                style = typography.sansHeader.copy(
                    color = colors.onAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                ),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = stringResource(Res.string.miniapps_new_row_title),
                style = typography.sansLabel.copy(
                    color = colors.ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
            )
            Text(
                text = stringResource(Res.string.miniapps_new_row_subtitle),
                style = typography.sansSmall.copy(color = colors.inkMuted),
            )
        }
    }
}

@Composable
private fun MiniAppCard(
    miniApp: MiniApp,
    treePreview: @Composable (treeJson: String, onTap: () -> Unit) -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .border(width = 1.dp, color = colors.divider, shape = shapes.medium)
            .background(colors.surface)
            .pointerInput(miniApp.id) {
                detectTapGestures(
                    onTap = { onOpen() },
                    onLongPress = { onLongPress() },
                )
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmojiTile(emoji = miniApp.emoji)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = miniApp.name,
                    style = typography.serifBody.copy(
                        color = colors.ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                if (miniApp.triggerPrompt.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = miniApp.triggerPrompt,
                        style = typography.serifBody.copy(
                            color = colors.inkMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        ),
                        maxLines = 1,
                    )
                }
            }
            if (miniApp.usageCount > 0) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(Res.string.miniapps_usage_count, miniApp.usageCount),
                    style = typography.sansSmall.copy(
                        color = colors.inkSubtle,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider),
        )

        val treeJson = miniApp.lastRenderTreeJson
        if (treeJson != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = PREVIEW_MAX_HEIGHT_DP.dp)
                    .clipToBounds(),
            ) {
                treePreview(treeJson, onOpen)
                // Click absorber overlay — captures taps before they
                // reach interactive children of the rendered tree.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onOpen),
                )
            }
        } else {
            NoPreviewPlaceholder()
        }
    }
}

@Composable
private fun NoPreviewPlaceholder() {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(colors.surfaceMuted)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.miniapps_no_preview),
            style = typography.sansSmall.copy(
                color = colors.inkSubtle,
                fontStyle = FontStyle.Italic,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmojiTile(emoji: String) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shapes.small)
            .background(colors.surfaceMuted),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            style = typography.serifBody.copy(
                color = colors.ink,
                fontSize = 22.sp,
            ),
        )
    }
}

private const val PREVIEW_MAX_HEIGHT_DP = 280

@Preview
@Composable
private fun MiniAppsScreenPreview() {
    UndercurrentTheme {
        MiniAppsScreen(
            state = MiniAppsState(
                miniApps = listOf(
                    MiniApp(
                        id = "ma-1",
                        name = "Tip calculator",
                        emoji = "💰",
                        triggerPrompt = "Help me split a bill three ways with 18% tip.",
                        createdAtEpochMs = 1_716_000_000_000L,
                        usageCount = 12,
                    ),
                    MiniApp(
                        id = "ma-2",
                        name = "Standup notes",
                        emoji = "📝",
                        triggerPrompt = "Format my standup as yesterday / today / blockers.",
                        createdAtEpochMs = 1_716_500_000_000L,
                        usageCount = 3,
                    ),
                ),
            ),
            // Preview can't render the real Compose tree from JSON
            // (TreeRenderer is androidMain-only); fall back to a
            // placeholder box so the rest of the row layout is
            // still inspectable.
            treePreview = { _, onTap ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(UndercurrentTheme.colors.surfaceMuted)
                        .clickable(onClick = onTap),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Preview unavailable in @Preview",
                        style = UndercurrentTheme.typography.sansSmall.copy(
                            color = UndercurrentTheme.colors.inkSubtle,
                        ),
                    )
                }
            },
            onBack = {},
            onOpenMiniApp = {},
        )
    }
}

@Preview
@Composable
private fun MiniAppsScreenEmptyPreview() {
    UndercurrentTheme {
        MiniAppsScreen(
            state = MiniAppsState(miniApps = emptyList()),
            treePreview = { _, _ -> },
            onBack = {},
            onOpenMiniApp = {},
        )
    }
}
