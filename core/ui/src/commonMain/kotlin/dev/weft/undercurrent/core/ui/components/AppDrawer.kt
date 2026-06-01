package dev.weft.undercurrent.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.domain.ConversationSummary
import dev.weft.undercurrent.core.ext.formatLastActivity
import dev.weft.undercurrent.core.ext.groupByRecency
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.common_cancel
import dev.weft.undercurrent.core.resources.common_delete
import dev.weft.undercurrent.core.resources.drawer_all_conversations
import dev.weft.undercurrent.core.resources.drawer_app_name
import dev.weft.undercurrent.core.resources.drawer_delete_dialog_message
import dev.weft.undercurrent.core.resources.drawer_delete_dialog_title
import dev.weft.undercurrent.core.resources.drawer_empty_conversations
import dev.weft.undercurrent.core.resources.drawer_memories
import dev.weft.undercurrent.core.resources.drawer_mini_apps
import dev.weft.undercurrent.core.resources.drawer_new_chat
import dev.weft.undercurrent.core.resources.drawer_personas
import dev.weft.undercurrent.core.resources.drawer_settings
import dev.weft.undercurrent.core.resources.drawer_traces
import dev.weft.undercurrent.core.resources.drawer_untitled_conversation
import dev.weft.undercurrent.core.ui.TokenDivider
import org.jetbrains.compose.resources.stringResource

/**
 * Side drawer for the chat surface. Holds three regions:
 *
 *  1. **Branding + New chat** at the top.
 *  2. **Recent conversations** (up to [RECENT_LIMIT]) grouped by date.
 *     Tap to switch; long-press opens a delete confirmation. Full
 *     management lives in the Conversations screen, reachable via
 *     "All conversations" at the bottom of the list when there are
 *     more than [RECENT_LIMIT] threads.
 *  3. **Section links** (Personas / Memories / Traces / Settings) pinned
 *     at the bottom.
 *
 * Tapping any item closes the drawer; the caller wires that via the
 * callbacks (e.g. `onSelect = { drawerState.close(); store.dispatch(...) }`).
 *
 * Delete confirmation lives inside the drawer (not at the call site) so
 * that opening the dialog doesn't require coordinating drawer close +
 * dialog open across composables. The dialog dismiss returns focus to
 * the open drawer naturally.
 */
@Composable
fun AppDrawer(
    conversations: List<ConversationSummary>,
    activeConversationId: String?,
    onSelect: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onShowAllConversations: () -> Unit,
    onShowPersonas: () -> Unit,
    onShowMiniApps: () -> Unit,
    onShowMemories: () -> Unit,
    onShowTraces: () -> Unit,
    onShowSettings: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    // Long-press target — null when no dialog is showing.
    var pendingDelete by remember { mutableStateOf<ConversationSummary?>(null) }

    ModalDrawerSheet(
        drawerContainerColor = colors.surface,
        drawerContentColor = colors.ink,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text(
                    text = stringResource(Res.string.drawer_app_name),
                    style = typography.sansHeader.copy(color = colors.ink),
                )
            }

            DrawerActionRow(symbol = "+", label = stringResource(Res.string.drawer_new_chat), onClick = onNewChat)
            TokenDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Recent conversations.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (conversations.isEmpty()) {
                    item("empty") {
                        Text(
                            text = stringResource(Res.string.drawer_empty_conversations),
                            style = typography.sansSmall.copy(color = colors.inkSubtle),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                } else {
                    val recent = conversations.take(RECENT_LIMIT)
                    val grouped = groupByRecency(recent) { it.lastMessageAtMs }
                    grouped.forEach { (bucket, items) ->
                        item(bucket) {
                            Text(
                                text = bucket.uppercase(),
                                style = typography.sansLabel.copy(color = colors.inkSubtle),
                                modifier = Modifier.padding(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = 14.dp,
                                    bottom = 6.dp,
                                ),
                            )
                        }
                        items(items, key = { it.id }) { conv ->
                            DrawerConversationRow(
                                summary = conv,
                                isActive = conv.id == activeConversationId,
                                onClick = { onSelect(conv.id) },
                                onLongPress = { pendingDelete = conv },
                            )
                        }
                    }
                    if (conversations.size > RECENT_LIMIT) {
                        item("all-conversations") {
                            DrawerActionRow(
                                symbol = "›",
                                label = stringResource(Res.string.drawer_all_conversations),
                                onClick = onShowAllConversations,
                                muted = true,
                            )
                        }
                    }
                }
            }

            // Pinned bottom section.
            TokenDivider()
            DrawerActionRow(symbol = "◐", label = stringResource(Res.string.drawer_personas), onClick = onShowPersonas)
            DrawerActionRow(symbol = "✦", label = stringResource(Res.string.drawer_mini_apps), onClick = onShowMiniApps)
            DrawerActionRow(symbol = "★", label = stringResource(Res.string.drawer_memories), onClick = onShowMemories)
            DrawerActionRow(symbol = "▶", label = stringResource(Res.string.drawer_traces), onClick = onShowTraces)
            DrawerActionRow(symbol = "⚙", label = stringResource(Res.string.drawer_settings), onClick = onShowSettings)
            Spacer(Modifier.height(12.dp))
        }
    }

    pendingDelete?.let { target ->
        val untitled = stringResource(Res.string.drawer_untitled_conversation)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(Res.string.drawer_delete_dialog_title)) },
            text = {
                val label = target.title.ifBlank { untitled }
                Text(stringResource(Res.string.drawer_delete_dialog_message, label))
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = target.id
                    pendingDelete = null
                    onDeleteConversation(id)
                }) { Text(stringResource(Res.string.common_delete), color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(Res.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun DrawerActionRow(
    symbol: String,
    label: String,
    onClick: () -> Unit,
    muted: Boolean = false,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = symbol,
            style = typography.sansHeader.copy(
                color = if (muted) colors.inkMuted else colors.inkMuted,
            ),
            modifier = Modifier.size(width = 24.dp, height = 24.dp).padding(end = 4.dp),
        )
        Text(
            text = label,
            style = typography.serifBody.copy(
                color = if (muted) colors.inkMuted else colors.ink,
            ),
        )
    }
}

/**
 * One row in the drawer's conversation list.
 *
 * Tap → switch to that thread (caller closes the drawer).
 * Long-press → open delete confirmation (caller surfaces dialog state).
 *
 * Uses `pointerInput` + `detectTapGestures` instead of `Modifier.clickable`
 * because long-press detection needs to coexist with tap routing —
 * the standard clickable doesn't expose a long-press hook.
 */
@Composable
private fun DrawerConversationRow(
    summary: ConversationSummary,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) colors.surfaceMuted else colors.surface)
            .pointerInput(summary.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() },
                )
            }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(6.dp)
                    .clip(shapes.xsmall)
                    .background(colors.accent),
            )
        } else {
            Spacer(modifier = Modifier.size(width = 16.dp, height = 6.dp))
        }
        val untitled = stringResource(Res.string.drawer_untitled_conversation)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = summary.title.ifBlank { untitled },
                style = typography.serifBody.copy(color = colors.ink),
                maxLines = 1,
            )
            Text(
                text = formatLastActivity(summary.lastMessageAtMs),
                style = typography.sansSmall.copy(color = colors.inkSubtle),
            )
        }
    }
}

private const val RECENT_LIMIT: Int = 20
