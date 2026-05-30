package dev.weft.undercurrent.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ChatHeader(
    threadTitle: String,
    threadSubtitle: String,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onDeleteThread: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    var overflowOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "☰",
                style = typography.sansHeader.copy(
                    color = colors.ink,
                    fontSize = 22.sp,
                ),
                modifier = Modifier
                    .clickable(onClick = onOpenDrawer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            ) {
                Text(
                    text = threadTitle,
                    style = typography.sansHeader.copy(
                        color = colors.ink,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                )
                if (threadSubtitle.isNotBlank()) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = threadSubtitle,
                        style = typography.sansSmall.copy(color = colors.inkSubtle),
                        maxLines = 1,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clip(UndercurrentTheme.shapes.xsmall)
                    .clickable(onClick = onNewChat)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "+",
                    style = typography.sansHeader.copy(
                        color = colors.ink,
                        fontSize = 18.sp,
                    ),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "New",
                    style = typography.sansHeader.copy(
                        color = colors.ink,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            Box {
                Text(
                    text = "⋮",
                    style = typography.sansHeader.copy(
                        color = colors.ink,
                        fontSize = 22.sp,
                    ),
                    modifier = Modifier
                        .clickable { overflowOpen = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
                DropdownMenu(
                    expanded = overflowOpen,
                    onDismissRequest = { overflowOpen = false },
                    containerColor = colors.surface,
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Delete thread",
                                style = typography.sansHeader.copy(
                                    color = colors.error,
                                    fontSize = 15.sp,
                                ),
                            )
                        },
                        onClick = {
                            overflowOpen = false
                            confirmDelete = true
                        },
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider),
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this thread?") },
            text = {
                val label = threadTitle.ifBlank { "(untitled)" }
                Text("\"$label\" and all its messages will be permanently removed.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteThread()
                }) { Text("Delete", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Preview
@Composable
private fun ChatHeaderPreview() {
    UndercurrentTheme {
        ChatHeader(
            threadTitle = "Migrating to KMP",
            threadSubtitle = "Claude Haiku 4.5 · Default",
            onOpenDrawer = { },
            onNewChat = { },
            onDeleteThread = { },
        )
    }
}
