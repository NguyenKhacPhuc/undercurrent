package dev.weft.undercurrent.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
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
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.cd_checked
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

// =============================================================================
// ListRow — leading icon + title/subtitle + optional trailing
// =============================================================================

@Serializable
internal data class ListRowProps(
    val title: String,
    val subtitle: String = "",
    /** Leading icon name. Blank = no icon. */
    val icon: String = "",
    /** Trailing right-side text (e.g. "$12.50", "3 items"). */
    val trailing: String = "",
    /** Show a chevron on the right edge — implies tappable. */
    val chevron: Boolean = false,
    /** Action fired on tap. Blank = non-interactive row. */
    val onTap: String = "",
)

internal class ListRowComponent : WeftComponent<ListRowProps>(
    name = "ListRow",
    description = "A list row with leading icon, title + optional subtitle, optional trailing text, optional chevron. title: required. subtitle/icon/trailing: optional. chevron: true to show > on right. onTap: action key — set to make the row tappable. Stack inside a Sheet (variant 'bordered') for a divided list.",
    category = ComponentCategory.LAYOUT,
    propsSerializer = ListRowProps.serializer(),
) {
    @Composable
    override fun Render(props: ListRowProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val tappable = props.onTap.isNotBlank()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (tappable) it.clickable {
                        onEvent(ComponentEvent.Action(props.onTap, "ListRow", props.title))
                    } else it
                }
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            if (props.icon.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(UndercurrentTheme.shapes.small)
                        .background(cs.accent.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = undercurrentIcon(props.icon),
                        contentDescription = null,
                        tint = cs.accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (props.icon.isNotBlank()) 12.dp else 0.dp),
            ) {
                Text(
                    text = props.title,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = cs.ink,
                )
                if (props.subtitle.isNotBlank()) {
                    Text(
                        text = props.subtitle,
                        style = tp.sansSmall,
                        color = cs.inkMuted,
                    )
                }
            }
            if (props.trailing.isNotBlank()) {
                Text(
                    text = props.trailing,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.Medium),
                    color = cs.inkMuted,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (props.chevron || tappable) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = cs.inkSubtle,
                    modifier = Modifier.size(20.dp).padding(start = 4.dp),
                )
            }
        }
    }
}

// =============================================================================
// KeyValue — definition list (term + value rows)
// =============================================================================

@Serializable
internal data class KeyValuePair(val key: String, val value: String)

@Serializable
internal data class KeyValueProps(
    val items: List<KeyValuePair>,
    /** Show hairline dividers between rows. */
    val dividers: Boolean = true,
    /** Render the key in uppercase tracking. */
    val emphasizeKeys: Boolean = false,
)

internal class KeyValueComponent : WeftComponent<KeyValueProps>(
    name = "KeyValue",
    description = "Definition list — pairs of key/value rows. items: required list of {key, value}. dividers: true default (hairlines between rows). emphasizeKeys: false default — true renders keys in uppercase tracked style. Great for metadata, settings, profile fields.",
    category = ComponentCategory.LAYOUT,
    propsSerializer = KeyValueProps.serializer(),
    example = """{"type": "KeyValue", "props": {"items": [{"key": "Created", "value": "Jul 14"}, {"key": "Author", "value": "Phuc"}, {"key": "Words", "value": "1,234"}], "emphasizeKeys": true}}""",
) {
    @Composable
    override fun Render(props: KeyValueProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(modifier = Modifier.fillMaxWidth()) {
            props.items.forEachIndexed { i, pair ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                ) {
                    Text(
                        text = if (props.emphasizeKeys) pair.key.uppercase() else pair.key,
                        style = if (props.emphasizeKeys) {
                            tp.sansSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                        } else {
                            tp.sansLabel
                        },
                        color = cs.inkMuted,
                        modifier = Modifier.width(120.dp),
                    )
                    Text(
                        text = pair.value,
                        style = tp.serifBody.copy(fontSize = 14.sp),
                        color = cs.ink,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (props.dividers && i < props.items.size - 1) {
                    HorizontalDivider(color = cs.divider)
                }
            }
        }
    }
}

// =============================================================================
// Checklist — toggle-able checkbox rows
// =============================================================================

@Serializable
internal data class ChecklistItem(
    val id: String,
    val text: String,
    val checked: Boolean = false,
)

@Serializable
internal data class ChecklistProps(
    val items: List<ChecklistItem>,
)

internal class ChecklistComponent : WeftComponent<ChecklistProps>(
    name = "Checklist",
    description = "List of checkable items. items: required list of {id, text, checked}. Tapping a row toggles it and fires ToggleChanged with sourceId = item.id, value = newChecked. Use for to-do lists, simple multi-select inputs.",
    category = ComponentCategory.INPUT,
    propsSerializer = ChecklistProps.serializer(),
) {
    @Composable
    override fun Render(props: ChecklistProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        // Local copy so taps reflect instantly without round-tripping through the agent.
        val state = remember(props.items) {
            mutableStateOf(props.items.associate { it.id to it.checked })
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            props.items.forEach { item ->
                val isChecked = state.value[item.id] == true
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newVal = !isChecked
                            state.value = state.value.toMutableMap().apply { put(item.id, newVal) }
                            onEvent(ComponentEvent.ToggleChanged(sourceId = item.id, value = newVal))
                        }
                        .padding(vertical = 10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(UndercurrentTheme.shapes.small)
                            .background(if (isChecked) cs.accent else cs.background)
                            .border(
                                width = if (isChecked) 0.dp else 1.5.dp,
                                color = if (isChecked) cs.accent else cs.divider,
                                shape = UndercurrentTheme.shapes.small,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isChecked) {
                            Icon(
                                imageVector = undercurrentIcon("check"),
                                contentDescription = stringResource(Res.string.cd_checked),
                                tint = cs.onAccent,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Text(
                        text = item.text,
                        style = tp.serifBody.copy(
                            fontSize = 15.sp,
                            textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                        ),
                        color = if (isChecked) cs.inkSubtle else cs.ink,
                        modifier = Modifier.padding(start = 12.dp).weight(1f),
                    )
                }
            }
        }
    }
}

// =============================================================================
// Timeline — vertical timeline with connector line + dots
// =============================================================================

@Serializable
internal data class TimelineEntry(
    val title: String,
    val time: String = "",
    val body: String = "",
    /** Status: done | active | pending — colors the dot. */
    val status: String = "pending",
    /** Optional icon name for the dot (overrides status coloring). */
    val icon: String = "",
)

@Serializable
internal data class TimelineProps(
    val entries: List<TimelineEntry>,
)

internal class TimelineComponent : WeftComponent<TimelineProps>(
    name = "Timeline",
    description = "Vertical timeline. entries: required list of {title, time, body, status, icon}. status colors the dot: 'done' (filled accent), 'active' (ring accent), 'pending' (default, muted). icon: optional override. Use for activity feeds, plan steps, history.",
    category = ComponentCategory.LAYOUT,
    propsSerializer = TimelineProps.serializer(),
    example = """{"type": "Timeline", "props": {"entries": [{"title": "Created the doc", "time": "9:14am", "status": "done"}, {"title": "Shared with team", "time": "10:02am", "status": "done"}, {"title": "Review meeting", "time": "2pm", "status": "active", "body": "Conference room B"}]}}""",
) {
    @Composable
    override fun Render(props: TimelineProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(modifier = Modifier.fillMaxWidth()) {
            props.entries.forEachIndexed { i, entry ->
                val isLast = i == props.entries.size - 1
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Dot + line column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(28.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                entry.icon.isNotBlank() -> Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(cs.accent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = undercurrentIcon(entry.icon),
                                        contentDescription = null,
                                        tint = cs.onAccent,
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                                entry.status == "done" -> Box(
                                    modifier = Modifier.size(12.dp).clip(CircleShape).background(cs.accent),
                                )
                                entry.status == "active" -> Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(cs.background)
                                        .border(3.dp, cs.accent, CircleShape),
                                )
                                else -> Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(cs.surfaceMuted)
                                        .border(1.5.dp, cs.divider, CircleShape),
                                )
                            }
                        }
                        if (!isLast) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(if (entry.body.isBlank()) 32.dp else 44.dp)
                                    .background(cs.divider),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp, bottom = if (!isLast) 12.dp else 0.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.title,
                                style = tp.sansLabel.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                ),
                                color = cs.ink,
                                modifier = Modifier.weight(1f),
                            )
                            if (entry.time.isNotBlank()) {
                                Text(
                                    text = entry.time,
                                    style = tp.sansSmall,
                                    color = cs.inkMuted,
                                )
                            }
                        }
                        if (entry.body.isNotBlank()) {
                            Text(
                                text = entry.body,
                                style = tp.serifBody.copy(fontSize = 13.sp, lineHeight = 19.sp),
                                color = cs.inkMuted,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Steps — horizontal step progress (1 of 3)
// =============================================================================

@Serializable
internal data class StepsProps(
    val steps: List<String>,
    /** 0-based index of current step. */
    val current: Int = 0,
)

internal class StepsComponent : WeftComponent<StepsProps>(
    name = "Steps",
    description = "Horizontal step indicator (1 of 3, 2 of 5, etc.). steps: required list of step labels. current: 0-based index of the active step (default 0). Completed steps show a check, active shows a filled dot, future shows an outlined number.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = StepsProps.serializer(),
) {
    @Composable
    override fun Render(props: StepsProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        if (props.steps.isEmpty()) return
        val current = props.current.coerceIn(0, props.steps.size - 1)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                props.steps.forEachIndexed { i, _ ->
                    val done = i < current
                    val active = i == current
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    done -> cs.accent
                                    active -> cs.accent
                                    else -> cs.background
                                },
                            )
                            .border(
                                width = if (active) 2.dp else 1.dp,
                                color = if (done || active) cs.accent else cs.divider,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) {
                            Icon(
                                imageVector = undercurrentIcon("check"),
                                contentDescription = null,
                                tint = cs.onAccent,
                                modifier = Modifier.size(14.dp),
                            )
                        } else {
                            Text(
                                text = (i + 1).toString(),
                                style = tp.sansLabel.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                ),
                                color = if (active) cs.onAccent else cs.inkMuted,
                            )
                        }
                    }
                    if (i < props.steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .background(if (i < current) cs.accent else cs.divider),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                props.steps.forEachIndexed { i, label ->
                    Text(
                        text = label,
                        style = tp.sansSmall.copy(
                            fontWeight = if (i == current) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (i <= current) cs.ink else cs.inkMuted,
                    )
                }
            }
        }
    }
}

/** Every list-tier component. */
internal val undercurrentListComponents: List<WeftComponent<*>> = listOf(
    ListRowComponent(),
    KeyValueComponent(),
    ChecklistComponent(),
    TimelineComponent(),
    StepsComponent(),
)
