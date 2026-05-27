package dev.weft.undercurrent.features.miniapps

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import dev.weft.compose.components.TreeRenderer
import dev.weft.compose.components.WeftComponentRegistry
import dev.weft.contracts.ComponentNode
import dev.weft.undercurrent.theme.UndercurrentTheme
import dev.weft.undercurrent.ui.ScreenScaffold
import kotlinx.serialization.json.Json
import org.koin.androidx.compose.koinViewModel

/**
 * Drawer-reached management screen for [MiniApp]s.
 *
 * Each saved mini-app renders as a full-width card showing its
 * cached UI tree — a non-interactive preview of what the user will
 * see when they tap to open. This is the user's launcher: every
 * mini-app they've built is visible at a glance, with its actual
 * generated UI (not just a name + prompt snippet, which the previous
 * "Saved Features" iteration showed).
 *
 * Tap behavior is delegated to [onOpenMiniApp] — MainActivity wires
 * that to the seed-bridge-then-navigate path that gives the user an
 * instant render while the agent re-runs in the background to
 * refresh the data. Long-press / explicit edit affordances open
 * [SaveAsMiniAppDialog].
 *
 * The empty state nudges the user toward the creation flow (run a
 * prompt in chat, tap "Save as mini-app" on the reply). There's no
 * "+ New" button by design — creating a mini-app without first
 * proving the prompt works loses the "this is the prompt that
 * worked" trust property.
 */
@Composable
internal fun MiniAppsScreen(
    componentRegistry: WeftComponentRegistry,
    onBack: () -> Unit,
    onOpenMiniApp: (MiniApp) -> Unit,
    /**
     * Tapped on the "+ New mini-app" header / empty-state CTA. Routes
     * through AppStore's `StartCreator(MiniApp)` reducer which kicks
     * off a guided QnA flow with the agent on the Creator screen.
     * The existing "Save as mini-app" affordance on a chat reply
     * continues to work too — that path uses the simple dialog.
     */
    onStartCreator: () -> Unit = {},
    vm: MiniAppsViewModel = koinViewModel(),
) {
    val miniApps by vm.miniApps.collectAsState()
    var editing by remember { mutableStateOf<MiniApp?>(null) }

    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    ScreenScaffold(title = "Mini apps", onBack = onBack) {
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
                        text = "Tap a mini-app to open it. Long-press to rename or delete.",
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
                        componentRegistry = componentRegistry,
                        onOpen = { onOpenMiniApp(miniApp) },
                        onLongPress = { editing = miniApp },
                    )
                }
                item("footer") {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Build a new one from scratch above, or run a prompt in chat " +
                            "and tap “Save as mini-app” on the reply.",
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
                vm.update(miniApp.id, name, emoji, prompt)
            },
            onDelete = {
                editing = null
                vm.delete(miniApp.id)
            },
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
            text = "No mini-apps yet.",
            style = typography.serifBody.copy(
                color = colors.ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Build one with a guided wizard, or run a prompt in chat and tap " +
                "“Save as mini-app” on the reply.",
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
                text = "+ New mini-app",
                style = typography.sansLabel.copy(
                    color = colors.onAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                ),
            )
        }
    }
}

/**
 * Header-style row at the top of the list. Mirrors the visual weight
 * of a [MiniAppCard] but with a clear "+" affordance and accent
 * styling so it reads as the primary CTA. Tap routes through
 * `onStartCreator` → [AppIntent.StartCreator] → guided wizard.
 */
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
                .clip(androidx.compose.foundation.shape.CircleShape)
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
                text = "New mini-app",
                style = typography.sansLabel.copy(
                    color = colors.ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
            )
            Text(
                text = "Walk through a guided wizard to set one up.",
                style = typography.sansSmall.copy(color = colors.inkMuted),
            )
        }
    }
}

/**
 * Card for one mini-app. Header row carries emoji + name + usage
 * count; body renders the cached `ui_render` tree (or a placeholder
 * if the mini-app has never been invoked).
 *
 * Tap anywhere → [onOpen]. Long-press → [onLongPress] (edit dialog).
 * The rendered tree itself is non-interactive — a pointerInput
 * overlay swallows touches before they reach buttons / text fields
 * inside the tree, so taps on the card body always open the
 * mini-app rather than firing a stale cached action against the
 * agent.
 *
 * Body height is capped via [PREVIEW_MAX_HEIGHT_DP] so a tall
 * mini-app doesn't dominate the list. Excess content clips at the
 * bottom — the user opens to see the full UI.
 */
@Composable
private fun MiniAppCard(
    miniApp: MiniApp,
    componentRegistry: WeftComponentRegistry,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    // Decode the cached tree once per (mini-app, treeJson) tuple.
    // Failures (corrupt JSON, schema drift) fall through to the
    // no-preview placeholder so the card stays usable.
    val tree: ComponentNode? = remember(miniApp.id, miniApp.lastRenderTreeJson) {
        miniApp.lastRenderTreeJson?.let { json ->
            runCatching {
                Json.decodeFromString(ComponentNode.serializer(), json)
            }.getOrNull()
        }
    }

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
        // Header — emoji + name + (optional) usage count.
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
                    text = "${miniApp.usageCount}×",
                    style = typography.sansSmall.copy(
                        color = colors.inkSubtle,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }

        // Hairline divider between header and body.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider),
        )

        // Body — rendered tree preview OR placeholder.
        if (tree != null) {
            MiniAppPreview(
                tree = tree,
                componentRegistry = componentRegistry,
                onOpen = onOpen,
            )
        } else {
            NoPreviewPlaceholder()
        }
    }
}

/**
 * Renders the cached tree non-interactively. The pointerInput
 * overlay sits on top of the tree and swallows every tap, routing
 * it to [onOpen] instead — so a button or text field inside the
 * preview can never accidentally fire its action against the agent.
 */
@Composable
private fun MiniAppPreview(
    tree: ComponentNode,
    componentRegistry: WeftComponentRegistry,
    onOpen: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = PREVIEW_MAX_HEIGHT_DP.dp)
            .clipToBounds(),
    ) {
        // Tree rendered with a no-op event sink. Events that DO fire
        // (despite the overlay) are dropped on the floor — they'd
        // be stale against current data anyway.
        TreeRenderer(
            tree = tree,
            registry = componentRegistry,
            onEvent = { /* preview-mode: ignore */ },
        )
        // Click absorber — matchParentSize covers the full body.
        // Z-order: drawn after the tree, so it's on top for hit
        // testing. Any tap on the preview body lands here and routes
        // to onOpen rather than reaching the tree's interactive
        // children.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onOpen),
        )
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
            text = "Tap to open — preview will appear after first run.",
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
