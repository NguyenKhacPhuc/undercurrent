package dev.weft.undercurrent.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
// TaskItem — rich todo row (checkbox + title + meta + optional tap)
// =============================================================================

@Serializable
internal data class TaskItemProps(
    val id: String,
    val title: String,
    /** Initial checked state. Tapping the checkbox toggles + fires ToggleChanged. */
    val done: Boolean = false,
    /** Small caption under the title — typically a due date or context label. */
    val due: String = "",
    /** Priority: 'low' | 'med' | 'high' — colors the leading bar (high = error, med = accent, low = muted). */
    val priority: String = "",
    /** Optional tag chip shown on the right (e.g. project label). */
    val tag: String = "",
    /** Optional action fired when the title area is tapped (separate from the checkbox). */
    val onTap: String = "",
)

internal class TaskItemComponent : WeftComponent<TaskItemProps>(
    name = "TaskItem",
    description = "Rich todo row — checkbox + title + optional due caption + optional priority bar + optional tag. id: stable identifier (fires ToggleChanged on checkbox tap). title: required. due: small text below. priority: 'low' / 'med' / 'high' colors the left bar. tag: chip on the right. onTap: action fired when the row body (not the checkbox) is tapped. Stack multiple in a Sheet for a task list.",
    category = ComponentCategory.LAYOUT,
    propsSerializer = TaskItemProps.serializer(),
    example = """{"type": "TaskItem", "props": {"id": "t1", "title": "Send the design doc", "due": "Today, 5pm", "priority": "high", "tag": "Work"}}""",
) {
    @Composable
    override fun Render(props: TaskItemProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        var checked by remember(props.id, props.done) { mutableStateOf(props.done) }
        val priorityColor = when (props.priority.lowercase()) {
            "high" -> cs.error
            "med", "medium" -> cs.accent
            "low" -> cs.inkSubtle
            else -> androidx.compose.ui.graphics.Color.Transparent
        }
        val tappable = props.onTap.isNotBlank()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.small)
                .padding(vertical = 4.dp),
        ) {
            // Priority bar on the leading edge.
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .size(width = 3.dp, height = 32.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .background(priorityColor),
            )
            // Checkbox.
            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(22.dp)
                    .clip(UndercurrentTheme.shapes.small)
                    .background(if (checked) cs.accent else cs.background)
                    .border(
                        width = if (checked) 0.dp else 1.5.dp,
                        color = if (checked) cs.accent else cs.divider,
                        shape = UndercurrentTheme.shapes.small,
                    )
                    .clickable {
                        checked = !checked
                        onEvent(ComponentEvent.ToggleChanged(sourceId = props.id, value = checked))
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Icon(
                        imageVector = undercurrentIcon("check"),
                        contentDescription = null,
                        tint = cs.onAccent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            // Title + due.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .let { m ->
                        if (tappable) m.clickable {
                            onEvent(
                                ComponentEvent.Action(
                                    action = props.onTap,
                                    sourceType = "TaskItem",
                                    sourceLabel = props.title,
                                ),
                            )
                        } else m
                    },
            ) {
                Text(
                    text = props.title,
                    style = tp.sansLabel.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        textDecoration = if (checked) TextDecoration.LineThrough else null,
                    ),
                    color = if (checked) cs.inkSubtle else cs.ink,
                )
                if (props.due.isNotBlank()) {
                    Text(
                        text = props.due,
                        style = tp.sansSmall,
                        color = cs.inkMuted,
                    )
                }
            }
            // Trailing tag chip.
            if (props.tag.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(cs.surfaceMuted)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = props.tag,
                        style = tp.sansSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
                        color = cs.inkMuted,
                    )
                }
            }
        }
    }
}

// =============================================================================
// KanbanColumn — single column for a kanban board layout
// =============================================================================

@Serializable
internal data class KanbanColumnProps(
    val title: String,
    /** Count badge shown next to the title — defaults to children count. */
    val count: Int = -1,
    /** Tone of the leading accent line. neutral | accent | warning | success. */
    val tone: String = "neutral",
)

internal class KanbanColumnComponent : WeftComponent<KanbanColumnProps>(
    name = "KanbanColumn",
    description = "A single column for a kanban-style board. title: column name (e.g. 'To do', 'Doing'). tone: 'neutral' (default), 'accent', 'warning', 'success' — colors the top bar. count: number badge (default -1 = use children count). CHILDREN are stacked vertically — typically TaskItem / TapCard / Sheet rows. Compose multiple KanbanColumns inside an Inline with horizontal scroll for a full board.",
    category = ComponentCategory.LAYOUT,
    propsSerializer = KanbanColumnProps.serializer(),
    example = """{"type": "KanbanColumn", "props": {"title": "To do", "tone": "accent"}, "children": [{"type": "TaskItem", "props": {"id": "t1", "title": "Wireframes", "due": "Mon"}}, {"type": "TaskItem", "props": {"id": "t2", "title": "Pick palette"}}]}""",
) {
    @Composable
    override fun Render(props: KanbanColumnProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val node = LocalCurrentNode.current
        val renderNode = LocalNodeRenderer.current

        val accent = when (props.tone.lowercase()) {
            "accent" -> cs.accent
            "warning" -> cs.error
            "success" -> cs.accent
            else -> cs.inkMuted
        }
        val resolvedCount = if (props.count >= 0) props.count else node.children.size

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.surfaceMuted),
        ) {
            // Top bar with accent stripe + title + count.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 14.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                        .background(accent),
                )
                Text(
                    text = props.title,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = cs.ink,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                Text(
                    text = resolvedCount.toString(),
                    style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = cs.inkMuted,
                )
            }
            // Items.
            if (node.children.isEmpty()) {
                Text(
                    text = "Empty",
                    style = tp.sansSmall.copy(),
                    color = cs.inkSubtle,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    node.children.forEach { child -> renderNode(child) }
                }
            }
        }
    }
}

/** Every productivity component. */
internal val undercurrentProductivityComponents: List<WeftComponent<*>> = listOf(
    TaskItemComponent(),
    KanbanColumnComponent(),
)
