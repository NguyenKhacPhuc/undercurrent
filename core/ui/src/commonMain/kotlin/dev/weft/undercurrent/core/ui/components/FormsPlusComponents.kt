package dev.weft.undercurrent.core.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

// =============================================================================
// Slider — single-value slider with label + current value display
// =============================================================================

@Serializable
internal data class SliderProps(
    val id: String,
    val label: String,
    val initial: Float = 0.5f,
    val min: Float = 0f,
    val max: Float = 1f,
    /** Number of discrete stops, or 0 for continuous. */
    val steps: Int = 0,
    /** How to format the current-value display. "raw" (e.g. 0.42), "percent" (42%), "int" (42), "currency" ($42). */
    val format: String = "raw",
    /** Optional unit suffix appended after the formatted value (e.g. "mins", "$"). */
    val unit: String = "",
)

internal class SliderComponent : WeftComponent<SliderProps>(
    name = "Slider",
    description = "Single-handle slider with label and live value display. id: stable identifier (fires TextChanged with the new value as a string). label: shown above. initial / min / max: defaults 0.5 / 0 / 1. steps: discrete stops (0 = continuous). format: 'raw' (default), 'percent', 'int', 'currency'. unit: optional suffix.",
    category = ComponentCategory.INPUT,
    propsSerializer = SliderProps.serializer(),
) {
    @Composable
    override fun Render(props: SliderProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        var value by remember(props.id) { mutableStateOf(props.initial.coerceIn(props.min, props.max)) }
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = props.label,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = cs.ink,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatSliderValue(value, props.format) + props.unit.let { if (it.isNotBlank()) " $it" else "" },
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = cs.accent,
                )
            }
            Slider(
                value = value,
                onValueChange = {
                    value = it
                    onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = it.toString()))
                },
                valueRange = props.min..props.max,
                steps = props.steps,
                colors = SliderDefaults.colors(
                    thumbColor = cs.accent,
                    activeTrackColor = cs.accent,
                    inactiveTrackColor = cs.surfaceMuted,
                ),
            )
        }
    }
}

// =============================================================================
// RangeSlider — two-handle range
// =============================================================================

@Serializable
internal data class RangeSliderProps(
    val id: String,
    val label: String,
    val initialStart: Float = 0.25f,
    val initialEnd: Float = 0.75f,
    val min: Float = 0f,
    val max: Float = 1f,
    val steps: Int = 0,
    val format: String = "raw",
    val unit: String = "",
)

internal class RangeSliderComponent : WeftComponent<RangeSliderProps>(
    name = "RangeSlider",
    description = "Two-handle range slider — picks a min and max within a range. id: stable identifier (fires TextChanged with 'start,end' string on each change). label/min/max/steps/format/unit: same as Slider. initialStart/initialEnd: defaults 0.25 / 0.75.",
    category = ComponentCategory.INPUT,
    propsSerializer = RangeSliderProps.serializer(),
) {
    @Composable
    override fun Render(props: RangeSliderProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        var range by remember(props.id) {
            mutableStateOf(
                props.initialStart.coerceIn(props.min, props.max)..
                    props.initialEnd.coerceIn(props.min, props.max),
            )
        }
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = props.label,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = cs.ink,
                    modifier = Modifier.weight(1f),
                )
                val unitSuffix = props.unit.let { if (it.isNotBlank()) " $it" else "" }
                Text(
                    text = "${formatSliderValue(range.start, props.format)}$unitSuffix – " +
                        "${formatSliderValue(range.endInclusive, props.format)}$unitSuffix",
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = cs.accent,
                )
            }
            RangeSlider(
                value = range,
                onValueChange = {
                    range = it
                    onEvent(
                        ComponentEvent.TextChanged(
                            sourceId = props.id,
                            value = "${it.start},${it.endInclusive}",
                        ),
                    )
                },
                valueRange = props.min..props.max,
                steps = props.steps,
                colors = SliderDefaults.colors(
                    thumbColor = cs.accent,
                    activeTrackColor = cs.accent,
                    inactiveTrackColor = cs.surfaceMuted,
                ),
            )
        }
    }
}

private fun formatSliderValue(v: Float, format: String): String = when (format.lowercase()) {
    "percent" -> "${(v * 100).roundToInt()}%"
    "int" -> v.roundToInt().toString()
    "currency" -> "$${v.roundToInt()}"
    else -> {
        if (v == v.toInt().toFloat()) v.toInt().toString()
        else v.toFixed(2)
    }
}

// =============================================================================
// MultiChoice — checkbox group for multi-select
// =============================================================================

@Serializable
internal data class MultiChoiceOption(
    val id: String,
    val label: String,
    val description: String = "",
    val selected: Boolean = false,
)

@Serializable
internal data class MultiChoiceProps(
    val id: String,
    val title: String = "",
    val options: List<MultiChoiceOption>,
)

internal class MultiChoiceComponent : WeftComponent<MultiChoiceProps>(
    name = "MultiChoice",
    description = "Checkbox group — pick zero or more. options: list of {id, label, description, selected}. id: stable identifier (fires TextChanged with the comma-separated list of selected option ids on every change). title: optional header. Use Choice for single-select; this for multi.",
    category = ComponentCategory.INPUT,
    propsSerializer = MultiChoiceProps.serializer(),
    example = """{"type": "MultiChoice", "props": {"id": "diet", "title": "Dietary preferences", "options": [{"id": "veg", "label": "Vegetarian"}, {"id": "vegan", "label": "Vegan"}, {"id": "gf", "label": "Gluten-free", "selected": true}]}}""",
) {
    @Composable
    override fun Render(props: MultiChoiceProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val state = remember(props.options) {
            mutableStateOf(props.options.associate { it.id to it.selected })
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (props.title.isNotBlank()) {
                Text(
                    text = props.title,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = cs.ink,
                )
            }
            props.options.forEach { opt ->
                val checked = state.value[opt.id] == true
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(UndercurrentTheme.shapes.small)
                        .clickable {
                            val newMap = state.value.toMutableMap().apply { put(opt.id, !checked) }
                            state.value = newMap
                            val selectedIds = newMap.filterValues { it }.keys.sorted().joinToString(",")
                            onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = selectedIds))
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(UndercurrentTheme.shapes.small)
                            .background(if (checked) cs.accent else cs.background)
                            .border(
                                width = if (checked) 0.dp else 1.5.dp,
                                color = if (checked) cs.accent else cs.divider,
                                shape = UndercurrentTheme.shapes.small,
                            ),
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
                    Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                        Text(
                            text = opt.label,
                            style = tp.sansLabel.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp),
                            color = cs.ink,
                        )
                        if (opt.description.isNotBlank()) {
                            Text(
                                text = opt.description,
                                style = tp.sansSmall,
                                color = cs.inkMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// ColorPicker — themed swatch grid
// =============================================================================

@Serializable
internal data class ColorPickerProps(
    val id: String,
    val label: String = "",
    /** Hex colors to show as swatches. Auto-generates a small accent-rooted palette if empty. */
    val swatches: List<String> = emptyList(),
    /** Hex of the initially selected color. */
    val initial: String = "",
)

internal class ColorPickerComponent : WeftComponent<ColorPickerProps>(
    name = "ColorPicker",
    description = "Swatch-based color picker — taps a color from a small grid. id: stable identifier (fires TextChanged with the picked '#RRGGBB' on tap). label: optional. swatches: hex strings like '#FF8800' (empty = sensible default palette). initial: hex of pre-selected swatch.",
    category = ComponentCategory.INPUT,
    propsSerializer = ColorPickerProps.serializer(),
) {
    @Composable
    override fun Render(props: ColorPickerProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val list = props.swatches.takeIf { it.isNotEmpty() } ?: defaultPalette()
        var picked by remember(props.id) { mutableStateOf(props.initial.lowercase()) }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (props.label.isNotBlank()) {
                Text(
                    text = props.label,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = cs.ink,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                list.take(8).forEach { hex ->
                    val isSelected = hex.lowercase() == picked
                    val color = parseHex(hex) ?: return@forEach
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) cs.accent else cs.divider,
                                shape = CircleShape,
                            )
                            .clickable {
                                picked = hex.lowercase()
                                onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = hex))
                            },
                    )
                }
            }
        }
    }

    private fun defaultPalette(): List<String> = listOf(
        "#0D9488", "#0EA5E9", "#6366F1", "#A855F7",
        "#EC4899", "#F59E0B", "#84CC16", "#475569",
    )

    private fun parseHex(hex: String): Color? = runCatching {
        val cleaned = hex.removePrefix("#").trim()
        val parsed = when (cleaned.length) {
            6 -> "FF$cleaned"
            8 -> cleaned
            else -> return null
        }
        Color(parsed.toLong(16))
    }.getOrNull()
}

/** Every FormsPlus component. */
internal val undercurrentFormsPlusComponents: List<WeftComponent<*>> = listOf(
    SliderComponent(),
    RangeSliderComponent(),
    MultiChoiceComponent(),
    ColorPickerComponent(),
)
