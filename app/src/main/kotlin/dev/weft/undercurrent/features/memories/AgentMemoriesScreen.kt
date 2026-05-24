package dev.weft.undercurrent.features.memories

import dev.weft.undercurrent.ui.TokenDivider
import dev.weft.undercurrent.ui.ScaffoldTextAction
import dev.weft.undercurrent.ui.ScreenScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import dev.weft.harness.memory.MemoryEntry
import dev.weft.harness.memory.MemoryScope
import dev.weft.undercurrent.theme.UndercurrentTheme
import dev.weft.undercurrent.features.memories.MemoriesViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Agent memories surface (ADR-002). Lists every fact the agent has stored
 * about the user; delete individually or wipe everything. This is the
 * user's accountability gate on persistent state — anything not visible
 * here, the substrate isn't remembering.
 *
 * Visual: same scaffold as the other secondary screens. Each memory is a
 * row with a small scope chip on the left, content in serif body, and a
 * trailing Delete.
 */
@Composable
internal fun AgentMemoriesScreen(
    onBack: () -> Unit,
    vm: MemoriesViewModel = koinViewModel(),
) {
    val memories by vm.memories.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }

    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    ScreenScaffold(
        title = "Memories",
        onBack = onBack,
        trailing = {
            ScaffoldTextAction(
                label = "Clear all",
                onClick = { confirmClear = true },
                enabled = memories.isNotEmpty(),
                isDestructive = true,
            )
        },
    ) {
        if (memories.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Nothing remembered yet",
                    style = typography.sansHeader.copy(color = colors.ink),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Anything the assistant wants to remember about you " +
                        "will show up here, and you can delete it any time.",
                    style = typography.sansSmall.copy(color = colors.inkMuted),
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "${memories.size} memor${if (memories.size == 1) "y" else "ies"}",
                    style = typography.sansSmall.copy(color = colors.inkMuted),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
            ) {
                items(memories, key = { it.id }) { entry ->
                    MemoryRow(
                        entry = entry,
                        onDelete = { vm.delete(entry.id) },
                    )
                    TokenDivider()
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Forget everything?") },
            text = { Text("This deletes every memory the agent has stored. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearAll()
                }) { Text("Forget all", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun MemoryRow(entry: MemoryEntry, onDelete: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Scope chip.
            val chipBg = if (entry.scope == MemoryScope.PERMANENT) colors.accent else colors.surfaceMuted
            val chipInk = if (entry.scope == MemoryScope.PERMANENT) colors.onAccent else colors.inkMuted
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .clip(shapes.xsmall)
                    .background(chipBg)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = entry.scope.label,
                    style = typography.sansLabel.copy(color = chipInk),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = timeFormat.format(Date(entry.storedAtEpochMs)),
                style = typography.sansSmall.copy(color = colors.inkSubtle),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = entry.content,
            style = typography.serifBody.copy(color = colors.ink),
        )
        if (entry.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry.tags.joinToString(" · ") { "#$it" },
                style = typography.sansSmall.copy(color = colors.inkMuted),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row {
            Spacer(modifier = Modifier.weight(1f))
            ScaffoldTextAction(
                label = "Delete",
                onClick = onDelete,
                isDestructive = true,
            )
        }
    }
}

private val timeFormat = SimpleDateFormat("MMM d · HH:mm", Locale.getDefault())

private val MemoryScope.label: String
    get() = when (this) {
        MemoryScope.SESSION -> "SESSION"
        MemoryScope.PERMANENT -> "PERMANENT"
        MemoryScope.ANY -> "ANY"
    }
