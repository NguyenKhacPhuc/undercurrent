package dev.weft.undercurrent.features.conversations

import dev.weft.undercurrent.ui.TokenDivider
import dev.weft.undercurrent.ui.SectionLabel
import dev.weft.undercurrent.ui.ScaffoldTextAction
import dev.weft.undercurrent.ui.ScreenScaffold
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
import dev.weft.harness.conversation.ConversationSummary
import dev.weft.undercurrent.theme.UndercurrentTheme
import dev.weft.undercurrent.features.conversations.ConversationsViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Lists every persisted conversation thread, grouped by recency
 * (Today / Yesterday / Earlier this week / Older). Tap a row to resume.
 * Long-press surfaces delete — TODO; for now there's a small Delete action
 * on each row that opens a confirm dialog.
 */
@Composable
internal fun ConversationsListScreen(
    activeConversationId: String?,
    onSelect: (String) -> Unit,
    onNewChat: () -> Unit,
    onBack: () -> Unit,
    vm: ConversationsViewModel = koinViewModel(),
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    var pendingDelete by remember { mutableStateOf<ConversationSummary?>(null) }
    var confirmingClearAll by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val conversations by remember(searchQuery) { vm.search(searchQuery) }
        .collectAsState(initial = emptyList())

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
                onValueChange = { searchQuery = it },
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
            val grouped = remember(conversations) { groupConversationsByRecency(conversations) }
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
                    vm.delete(id)
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
                    vm.clearAll()
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
        // Active-indicator: a small accent dot on the left.
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
                style = typography.serifBody.copy(
                    color = if (isActive) colors.ink else colors.ink,
                ),
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

