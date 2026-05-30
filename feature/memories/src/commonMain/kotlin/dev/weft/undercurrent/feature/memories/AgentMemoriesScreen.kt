package dev.weft.undercurrent.feature.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.ui.ScaffoldTextAction
import dev.weft.undercurrent.core.ui.ScreenScaffold
import dev.weft.undercurrent.core.ui.TokenDivider
import dev.weft.undercurrent.core.domain.MemoryEntry
import dev.weft.undercurrent.core.domain.MemoryScope
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

/**
 * Agent memories surface (ADR-002). Lists every fact the agent has
 * stored about the user; delete individually or wipe everything. This
 * is the user's accountability gate on persistent state — anything not
 * visible here, the substrate isn't remembering.
 *
 * Stateful entry point — hoists state from [MemoriesViewModel] and
 * forwards to the stateless overload.
 */
@Composable
fun AgentMemoriesScreen(
    onBack: () -> Unit,
    viewModel: MemoriesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    AgentMemoriesScreen(
        state = state,
        onBack = onBack,
        onDelete = { viewModel.dispatch(MemoriesIntent.Delete(it)) },
        onClearAll = { viewModel.dispatch(MemoriesIntent.ClearAll) },
    )
}

/**
 * Stateless variant — takes [state] and per-action callbacks. Used by
 * the stateful overload above plus `@Preview` / snapshot harnesses.
 */
@Composable
fun AgentMemoriesScreen(
    state: MemoriesState,
    onBack: () -> Unit,
    onDelete: (id: String) -> Unit = {},
    onClearAll: () -> Unit = {},
) {
    val memories = state.memories
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
                        onDelete = { onDelete(entry.id) },
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
                    onClearAll()
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
            val chipBg = if (entry.scope == MemoryScope.PERMANENT) colors.accent else colors.surfaceMuted
            val chipInk = if (entry.scope == MemoryScope.PERMANENT) colors.onAccent else colors.inkMuted
            Box(
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
                text = formatMemoryTime(entry.storedAtEpochMs),
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

private val MemoryScope.label: String
    get() = when (this) {
        MemoryScope.SESSION -> "SESSION"
        MemoryScope.PERMANENT -> "PERMANENT"
        MemoryScope.ANY -> "ANY"
    }

@OptIn(ExperimentalTime::class)
private fun formatMemoryTime(epochMs: Long): String {
    val ldt = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val month = ldt.month.shortName()
    val hour = ldt.hour.toString().padStart(2, '0')
    val minute = ldt.minute.toString().padStart(2, '0')
    return "$month ${ldt.day} · $hour:$minute"
}

private fun Month.shortName(): String = when (this) {
    Month.JANUARY -> "Jan"
    Month.FEBRUARY -> "Feb"
    Month.MARCH -> "Mar"
    Month.APRIL -> "Apr"
    Month.MAY -> "May"
    Month.JUNE -> "Jun"
    Month.JULY -> "Jul"
    Month.AUGUST -> "Aug"
    Month.SEPTEMBER -> "Sep"
    Month.OCTOBER -> "Oct"
    Month.NOVEMBER -> "Nov"
    Month.DECEMBER -> "Dec"
}

@Preview
@Composable
private fun AgentMemoriesScreenPreview() {
    UndercurrentTheme {
        AgentMemoriesScreen(
            state = MemoriesState(
                memories = listOf(
                    MemoryEntry(
                        id = "m1",
                        content = "Prefers terse answers; expand only when asked.",
                        tags = listOf("style"),
                        scope = MemoryScope.PERMANENT,
                        storedAtEpochMs = 1_716_900_000_000L,
                    ),
                    MemoryEntry(
                        id = "m2",
                        content = "Working on the Undercurrent KMP migration this week.",
                        tags = listOf("project", "context"),
                        scope = MemoryScope.PERMANENT,
                        storedAtEpochMs = 1_717_000_000_000L,
                    ),
                ),
            ),
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun AgentMemoriesScreenEmptyPreview() {
    UndercurrentTheme {
        AgentMemoriesScreen(
            state = MemoriesState(memories = emptyList()),
            onBack = {},
        )
    }
}
