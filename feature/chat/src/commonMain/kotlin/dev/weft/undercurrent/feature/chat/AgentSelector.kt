package dev.weft.undercurrent.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Compact agent picker — AssistChip + dropdown of every user-addressable
 * agent the host registered. Multi-agent hosts wire this above the
 * chat input; single-agent hosts pass a 0- or 1-item list and the
 * composable renders nothing.
 *
 * KMP — commonMain. Re-implemented from
 * `dev.weft.compose.components.AgentSelector` (Weft's
 * `:android-compose-defaults`) because that lives in an Android-only
 * module. The two stay in sync via [AgentOption]'s wire format.
 */
@Composable
fun AgentSelector(
    options: List<AgentOption>,
    selectedName: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.size <= 1) return
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.name == selectedName } ?: options.first()
    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(selected.displayName) },
            colors = AssistChipDefaults.assistChipColors(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            for (opt in options) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = opt.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        expanded = false
                        if (opt.name != selectedName) onSelect(opt.name)
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun AgentSelectorPreview() {
    UndercurrentTheme {
        AgentSelector(
            options = listOf(
                AgentOption(name = "default", displayName = "Assistant"),
                AgentOption(name = "writer", displayName = "Writer"),
            ),
            selectedName = "default",
            onSelect = { },
        )
    }
}
