package dev.weft.undercurrent.feature.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.ext.formatLastActivity
import dev.weft.undercurrent.core.ext.groupByRecency
import dev.weft.undercurrent.core.ui.ScaffoldTextAction
import dev.weft.undercurrent.core.ui.ScreenScaffold
import dev.weft.undercurrent.core.ui.SectionLabel
import dev.weft.undercurrent.core.ui.TokenDivider
import dev.weft.undercurrent.core.domain.ConversationSummary
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

/**
 * Lists every persisted conversation thread, grouped by recency
 * (Today / Yesterday / Earlier this week / Older). Tap a row to resume;
 * each row has an inline Delete action that opens a confirm dialog.
 *
 * Stateful entry point — hoists state from [ConversationsViewModel]
 * and forwards to the stateless overload.
 */
@Composable
fun ConversationsListScreen(
    activeConversationId: String?,
    onSelect: (String) -> Unit,
    onNewChat: () -> Unit,
    onBack: () -> Unit,
    viewModel: ConversationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    ConversationsListScreen(
        state = state,
        activeConversationId = activeConversationId,
        onSelect = onSelect,
        onNewChat = onNewChat,
        onBack = onBack,
        onQueryChange = { viewModel.dispatch(ConversationsIntent.SetQuery(it)) },
        onDelete = { viewModel.dispatch(ConversationsIntent.Delete(it)) },
        onClearAll = { viewModel.dispatch(ConversationsIntent.ClearAll) },
    )
}

/**
 * Stateless variant — takes [state] and per-action callbacks. Used by
 * the stateful overload above plus `@Preview` / snapshot harnesses.
 * The two confirm-dialogs (per-row delete + clear-all) keep their
 * local `remember`-backed `mutableStateOf` here — they're transient
 * UI state, not persistent.
 */
@Composable
fun ConversationsListScreen(
    state: ConversationsState,
    activeConversationId: String?,
    onSelect: (String) -> Unit,
    onNewChat: () -> Unit,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit = {},
    onDelete: (id: String) -> Unit = {},
    onClearAll: () -> Unit = {},
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    var pendingDelete by remember { mutableStateOf<ConversationSummary?>(null) }
    var confirmingClearAll by remember { mutableStateOf(false) }
    val searchQuery = state.query
    val conversations = state.conversations

    ScreenScaffold(
        title = "Conversations",
        onBack = onBack,
        trailing = { ScaffoldTextAction(label = "+ New", onClick = onNewChat) },
    ) {
        // Search input — token-styled, same as chat input.
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .clip(shapes.medium)
                .border(width = 1.dp, color = colors.divider, shape = shapes.medium)
                .background(colors.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = typography.serifBody.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                singleLine = true,
            )
            if (searchQuery.isEmpty()) {
                Text(
                    text = "Search threads…",
                    style = typography.serifBody.copy(color = colors.inkSubtle),
                )
            }
        }

        if (conversations.isEmpty()) {
            EmptyState(
                title = if (searchQuery.isNotBlank()) "No matches" else "No conversations yet",
                body = if (searchQuery.isNotBlank()) {
                    "No threads have \"$searchQuery\" in the title or any message."
                } else {
                    "Start chatting and your threads will show up here."
                },
            )
        } else {
            val grouped = remember(conversations) { groupByRecency(conversations) { it.lastMessageAtMs } }
            LazyColumn(modifier = Modifier.fillMaxSize().weight(1f)) {
                grouped.forEach { (bucket, items) ->
                    item(bucket) { SectionLabel(text = bucket) }
                    items(items, key = { it.id }) { conv ->
                        ConversationRow(
                            summary = conv,
                            isActive = conv.id == activeConversationId,
                            onClick = { onSelect(conv.id) },
                            onDelete = { pendingDelete = conv },
                        )
                        TokenDivider()
                    }
                }
                item("clear-all") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        ScaffoldTextAction(
                            label = "Clear all",
                            onClick = { confirmingClearAll = true },
                            isDestructive = true,
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this thread?") },
            text = {
                val label = target.title.ifBlank { "(untitled)" }
                Text("\"$label\" and all its messages will be permanently removed.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = target.id
                    pendingDelete = null
                    onDelete(id)
                }) { Text("Delete", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (confirmingClearAll) {
        AlertDialog(
            onDismissRequest = { confirmingClearAll = false },
            title = { Text("Clear all conversations?") },
            text = { Text("Every thread and every message will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingClearAll = false
                    onClearAll()
                }) { Text("Clear all", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingClearAll = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ConversationRow(
    summary: ConversationSummary,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) colors.surfaceMuted else colors.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = summary.title.ifBlank { "(untitled)" },
                style = typography.serifBody.copy(color = colors.ink),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatLastActivity(summary.lastMessageAtMs),
                style = typography.sansSmall.copy(color = colors.inkSubtle),
            )
        }
        ScaffoldTextAction(
            label = "Delete",
            onClick = onDelete,
            isDestructive = true,
        )
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = typography.sansHeader.copy(color = colors.ink),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = typography.sansSmall.copy(color = colors.inkMuted),
        )
    }
}

@Preview
@Composable
private fun ConversationsListScreenPreview() {
    UndercurrentTheme {
        ConversationsListScreen(
            state = ConversationsState(
                query = "",
                conversations = listOf(
                    ConversationSummary(
                        id = "c1",
                        title = "Migrating to KMP",
                        createdAtMs = 1_717_000_000_000L,
                        lastMessageAtMs = 1_717_100_000_000L,
                    ),
                    ConversationSummary(
                        id = "c2",
                        title = "Design tokens question",
                        createdAtMs = 1_716_900_000_000L,
                        lastMessageAtMs = 1_716_950_000_000L,
                    ),
                    ConversationSummary(
                        id = "c3",
                        title = "",
                        createdAtMs = 1_716_000_000_000L,
                        lastMessageAtMs = 1_716_100_000_000L,
                    ),
                ),
            ),
            activeConversationId = "c1",
            onSelect = {},
            onNewChat = {},
            onBack = {},
        )
    }
}
