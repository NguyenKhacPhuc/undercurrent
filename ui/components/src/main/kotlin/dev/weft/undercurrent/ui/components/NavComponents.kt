package dev.weft.undercurrent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// SearchBar — text input with leading search icon + clear button
// =============================================================================

@Serializable
internal data class SearchBarProps(
    val id: String,
    val placeholder: String = "Search…",
    val initial: String = "",
    /** Fired when the user taps the search icon. Optional. */
    val onSubmit: String = "",
)

internal class SearchBarComponent : WeftComponent<SearchBarProps>(
    name = "SearchBar",
    description = "Pill-style search input with leading magnifier + clear (×) button. id: stable identifier (fires TextChanged on every keystroke). placeholder/initial: optional. onSubmit: optional action key fired when the icon is tapped (e.g. 'run_search'). Use as the top of any list/grid that needs filtering.",
    category = ComponentCategory.INPUT,
    propsSerializer = SearchBarProps.serializer(),
) {
    @Composable
    override fun Render(props: SearchBarProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        var value by remember(props.id) { mutableStateOf(props.initial) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(cs.surfaceMuted)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = undercurrentIcon("search"),
                contentDescription = "Search",
                tint = cs.inkMuted,
                modifier = Modifier
                    .size(18.dp)
                    .let { m ->
                        if (props.onSubmit.isNotBlank()) m.clickable {
                            onEvent(
                                ComponentEvent.Action(
                                    action = props.onSubmit,
                                    sourceType = "SearchBar",
                                    sourceLabel = value,
                                ),
                            )
                        } else m
                    },
            )
            Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                if (value.isEmpty()) {
                    Text(
                        text = props.placeholder,
                        style = tp.sansLabel.copy(fontSize = 14.sp),
                        color = cs.inkSubtle,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = it))
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = cs.ink,
                        fontSize = 14.sp,
                        fontFamily = tp.sansLabel.fontFamily,
                    ),
                    cursorBrush = SolidColor(cs.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(cs.divider.copy(alpha = 0.6f))
                        .clickable {
                            value = ""
                            onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = ""))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = undercurrentIcon("close"),
                        contentDescription = "Clear",
                        tint = cs.background,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

// =============================================================================
// FilterChips — multi-select chip row (horizontally scrollable)
// =============================================================================

@Serializable
internal data class FilterChip(
    val id: String,
    val label: String,
    val selected: Boolean = false,
    /** Optional small leading icon name. */
    val icon: String = "",
)

@Serializable
internal data class FilterChipsProps(
    val id: String,
    val chips: List<FilterChip>,
    /** When true (default), multiple chips can be active. False = behaves as a single-select chip row. */
    val multi: Boolean = true,
)

internal class FilterChipsComponent : WeftComponent<FilterChipsProps>(
    name = "FilterChips",
    description = "Horizontally scrolling chip row for filtering. chips: list of {id, label, selected, icon}. multi: true (default — multi-select) or false (single-select). id: stable identifier (fires TextChanged with comma-separated selected chip ids on every change). Place above a list to drive client-side filtering or to seed the next agent turn with the active filters.",
    category = ComponentCategory.INPUT,
    propsSerializer = FilterChipsProps.serializer(),
    example = """{"type": "FilterChips", "props": {"id": "tags", "multi": true, "chips": [{"id": "ideas", "label": "Ideas", "selected": true}, {"id": "todo", "label": "To-do"}, {"id": "ref", "label": "Reference"}, {"id": "journal", "label": "Journal"}]}}""",
) {
    @Composable
    override fun Render(props: FilterChipsProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val state = remember(props.chips) {
            mutableStateOf(props.chips.associate { it.id to it.selected })
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            props.chips.forEach { chip ->
                val active = state.value[chip.id] == true
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (active) cs.accent else cs.background)
                        .border(
                            width = if (active) 0.dp else 1.dp,
                            color = if (active) cs.accent else cs.divider,
                            shape = CircleShape,
                        )
                        .clickable {
                            val newMap = if (props.multi) {
                                state.value.toMutableMap().apply { put(chip.id, !active) }
                            } else {
                                state.value.mapValues { (k, _) -> k == chip.id && !active }
                            }
                            state.value = newMap
                            val ids = newMap.filterValues { it }.keys.sorted().joinToString(",")
                            onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = ids))
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    if (chip.icon.isNotBlank()) {
                        Icon(
                            imageVector = undercurrentIcon(chip.icon),
                            contentDescription = null,
                            tint = if (active) cs.onAccent else cs.inkMuted,
                            modifier = Modifier.size(14.dp).padding(end = 4.dp),
                        )
                    }
                    Text(
                        text = chip.label,
                        style = tp.sansSmall.copy(
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.sp,
                        ),
                        color = if (active) cs.onAccent else cs.ink,
                    )
                }
            }
        }
    }
}

// =============================================================================
// Pagination — page X of Y with prev / next + jump-to
// =============================================================================

@Serializable
internal data class PaginationProps(
    val id: String,
    /** Current 1-based page. */
    val page: Int = 1,
    /** Total page count. */
    val pages: Int = 1,
)

internal class PaginationComponent : WeftComponent<PaginationProps>(
    name = "Pagination",
    description = "Compact pagination control: 'Page X of Y' with previous + next arrows + (when total ≤ 7) numbered jump dots. page: current 1-based page. pages: total. id: stable identifier (fires TextChanged with the new page number on tap).",
    category = ComponentCategory.INPUT,
    propsSerializer = PaginationProps.serializer(),
) {
    @Composable
    override fun Render(props: PaginationProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val total = props.pages.coerceAtLeast(1)
        var current by remember(props.id, props.page) { mutableIntStateOf(props.page.coerceIn(1, total)) }
        val canPrev = current > 1
        val canNext = current < total

        fun goTo(p: Int) {
            val next = p.coerceIn(1, total)
            if (next != current) {
                current = next
                onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = next.toString()))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
            ArrowBtn(direction = "arrow_back", enabled = canPrev, cs = cs) { goTo(current - 1) }
            // If total ≤ 7 render numbered dots; otherwise just text.
            if (total <= 7) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f).wrapContentWidth(Alignment.CenterHorizontally),
                ) {
                    for (i in 1..total) {
                        val active = i == current
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (active) cs.accent else cs.background)
                                .border(
                                    width = if (active) 0.dp else 1.dp,
                                    color = if (active) cs.accent else cs.divider,
                                    shape = CircleShape,
                                )
                                .clickable { goTo(i) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = i.toString(),
                                style = tp.sansLabel.copy(
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                ),
                                color = if (active) cs.onAccent else cs.ink,
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Page $current of $total",
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = cs.ink,
                    modifier = Modifier.weight(1f).wrapContentWidth(Alignment.CenterHorizontally),
                )
            }
            ArrowBtn(direction = "arrow_forward", enabled = canNext, cs = cs) { goTo(current + 1) }
        }
    }

    @Composable
    private fun ArrowBtn(
        direction: String,
        enabled: Boolean,
        cs: dev.weft.undercurrent.core.designsystem.UndercurrentColors,
        onClick: () -> Unit,
    ) {
        val alpha = if (enabled) 1f else 0.3f
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(cs.accent.copy(alpha = 0.12f * alpha))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = undercurrentIcon(direction),
                contentDescription = direction,
                tint = cs.accent.copy(alpha = alpha),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Every navigation component. */
internal val undercurrentNavComponents: List<WeftComponent<*>> = listOf(
    SearchBarComponent(),
    FilterChipsComponent(),
    PaginationComponent(),
)
