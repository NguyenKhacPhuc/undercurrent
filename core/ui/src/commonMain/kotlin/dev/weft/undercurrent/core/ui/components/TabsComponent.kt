package dev.weft.undercurrent.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.components.LocalCurrentNode
import dev.weft.compose.components.LocalNodeRenderer
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// Tabs — pill-style tabs with positional children
// =============================================================================

@Serializable
internal data class TabsProps(
    /** Tab labels in order. The Nth tab maps to the Nth CHILD of this node. */
    val tabs: List<String>,
    val initial: Int = 0,
)

internal class TabsComponent : WeftComponent<TabsProps>(
    name = "Tabs",
    description = "Pill-tab strip with positional children. Required: tabs (list of labels). Children align POSITIONALLY — emit one child per tab, in the same order. Only the selected tab's child renders. Tab switching is local — no agent round-trip per tap.",
    category = ComponentCategory.LAYOUT,
    propsSerializer = TabsProps.serializer(),
    example = """{"type": "Tabs", "props": {"tabs": ["Today", "Week", "Month"], "initial": 0}, "children": [{"type": "Text", "props": {"text": "Today's stuff"}}, {"type": "Text", "props": {"text": "This week"}}, {"type": "Text", "props": {"text": "This month"}}]}""",
) {
    @Composable
    override fun Render(props: TabsProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val node = LocalCurrentNode.current
        val renderNode = LocalNodeRenderer.current
        if (props.tabs.isEmpty()) return
        var selected by remember(props.tabs) {
            mutableIntStateOf(props.initial.coerceIn(0, props.tabs.size - 1))
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(cs.surfaceMuted)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                props.tabs.forEachIndexed { i, label ->
                    val active = i == selected
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (active) cs.background else cs.surfaceMuted.copy(alpha = 0f))
                            .clickable { selected = i }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = label,
                            style = tp.sansLabel.copy(
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp,
                            ),
                            color = if (active) cs.ink else cs.inkMuted,
                        )
                    }
                }
            }
            val content = node.children.getOrNull(selected)
            if (content != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    renderNode(content)
                }
            } else {
                Text(
                    text = "(no content for tab \"${props.tabs.getOrNull(selected).orEmpty()}\")",
                    style = tp.sansSmall,
                    color = cs.inkMuted,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** Tabs component as a single-item list for the aggregator. */
internal val undercurrentTabsComponents: List<WeftComponent<*>> = listOf(TabsComponent())
