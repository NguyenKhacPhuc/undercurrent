package dev.weft.undercurrent.ui.components

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.focus.onFocusChanged
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
// Field — text input with floating label + optional hint + error
// =============================================================================

@Serializable
internal data class FieldProps(
    /** Stable id — included in TextChanged events + form snapshots. */
    val id: String,
    val label: String,
    /** Placeholder text shown when empty. */
    val placeholder: String = "",
    /** Initial value. */
    val initial: String = "",
    /** Helper text below the field. */
    val hint: String = "",
    /** Error message — shown in red, overrides hint when non-blank. */
    val error: String = "",
    /** Single-line if true, multi-line otherwise. */
    val singleLine: Boolean = true,
)

internal class FieldComponent : WeftComponent<FieldProps>(
    name = "Field",
    description = "A text input with floating label + optional hint and error. id: required stable identifier (used in TextChanged events). label: floats above when focused or filled. placeholder / initial / hint / error: optional. singleLine: true default. Use multiple Fields inside a Stack for forms.",
    category = ComponentCategory.INPUT,
    propsSerializer = FieldProps.serializer(),
) {
    @Composable
    override fun Render(props: FieldProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val shape = UndercurrentTheme.shapes.small
        var value by remember(props.id) { mutableStateOf(props.initial) }
        var focused by remember { mutableStateOf(false) }

        val hasError = props.error.isNotBlank()
        val borderColor = when {
            hasError -> cs.error
            focused -> cs.accent
            else -> cs.divider
        }
        val labelFloating = focused || value.isNotBlank()

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(cs.background)
                    .border(if (focused || hasError) 1.5.dp else 1.dp, borderColor, shape)
                    .padding(horizontal = 14.dp, vertical = if (labelFloating) 18.dp else 14.dp),
            ) {
                Text(
                    text = props.label,
                    style = (if (labelFloating) tp.sansSmall else tp.sansLabel).copy(
                        fontSize = if (labelFloating) 11.sp else 14.sp,
                    ),
                    color = if (hasError) cs.error else if (focused) cs.accent else cs.inkMuted,
                    modifier = Modifier
                        .padding(start = 0.dp)
                        .let { if (labelFloating) it else it.padding(top = 0.dp) },
                )
                BasicTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = it))
                    },
                    singleLine = props.singleLine,
                    textStyle = TextStyle(
                        color = cs.ink,
                        fontSize = 15.sp,
                        fontFamily = tp.serifBody.fontFamily,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(cs.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (labelFloating) 14.dp else 0.dp)
                        .onFocusEvent(onFocus = { focused = it }),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty() && props.placeholder.isNotBlank()) {
                                Text(
                                    text = props.placeholder,
                                    style = tp.serifBody.copy(fontSize = 15.sp),
                                    color = cs.inkSubtle,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            val helper = if (hasError) props.error else props.hint
            if (helper.isNotBlank()) {
                Text(
                    text = helper,
                    style = tp.sansSmall,
                    color = if (hasError) cs.error else cs.inkMuted,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
        }
    }
}

private fun Modifier.onFocusEvent(onFocus: (Boolean) -> Unit): Modifier =
    this.onFocusChanged { onFocus(it.isFocused) }

// =============================================================================
// Toggle — switch with title + description side by side
// =============================================================================

@Serializable
internal data class ToggleProps(
    val id: String,
    val title: String,
    val description: String = "",
    val initial: Boolean = false,
)

internal class ToggleComponent : WeftComponent<ToggleProps>(
    name = "Toggle",
    description = "A switch with title + optional description. id: stable identifier for ToggleChanged events. title: required label. description: optional supporting text. initial: false default. Tap anywhere on the row to toggle.",
    category = ComponentCategory.INPUT,
    propsSerializer = ToggleProps.serializer(),
) {
    @Composable
    override fun Render(props: ToggleProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        var on by remember(props.id) { mutableStateOf(props.initial) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    on = !on
                    onEvent(ComponentEvent.ToggleChanged(sourceId = props.id, value = on))
                }
                .padding(vertical = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = props.title,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = cs.ink,
                )
                if (props.description.isNotBlank()) {
                    Text(
                        text = props.description,
                        style = tp.sansSmall,
                        color = cs.inkMuted,
                    )
                }
            }
            Switch(
                checked = on,
                onCheckedChange = {
                    on = it
                    onEvent(ComponentEvent.ToggleChanged(sourceId = props.id, value = it))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = cs.onAccent,
                    checkedTrackColor = cs.accent,
                    uncheckedThumbColor = cs.background,
                    uncheckedTrackColor = cs.surfaceMuted,
                    uncheckedBorderColor = cs.divider,
                ),
            )
        }
    }
}

// =============================================================================
// Stepper — integer +/- with current value display
// =============================================================================

@Serializable
internal data class StepperProps(
    val id: String,
    val label: String,
    val initial: Int = 0,
    val min: Int = 0,
    val max: Int = 99,
    val step: Int = 1,
)

internal class StepperComponent : WeftComponent<StepperProps>(
    name = "Stepper",
    description = "Integer stepper — round - / value / + controls. id: stable identifier (fires TextChanged with the new value as a string). label: shown to the left. initial: 0. min/max: bounds. step: 1.",
    category = ComponentCategory.INPUT,
    propsSerializer = StepperProps.serializer(),
) {
    @Composable
    override fun Render(props: StepperProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        var value by remember(props.id) { mutableIntStateOf(props.initial.coerceIn(props.min, props.max)) }

        fun emit(newValue: Int) {
            value = newValue.coerceIn(props.min, props.max)
            onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = value.toString()))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            Text(
                text = props.label,
                style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = cs.ink,
                modifier = Modifier.weight(1f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepIcon(icon = "remove", enabled = value > props.min, cs = cs) {
                    emit(value - props.step)
                }
                Box(
                    modifier = Modifier
                        .clip(UndercurrentTheme.shapes.small)
                        .background(cs.surfaceMuted)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = value.toString(),
                        style = tp.sansHeader.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        color = cs.ink,
                    )
                }
                StepIcon(icon = "add", enabled = value < props.max, cs = cs) {
                    emit(value + props.step)
                }
            }
        }
    }
}

@Composable
private fun StepIcon(
    icon: String,
    enabled: Boolean,
    cs: dev.weft.undercurrent.core.designsystem.UndercurrentColors,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.35f
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(cs.accent.copy(alpha = 0.14f * alpha))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = undercurrentIcon(icon),
            contentDescription = icon,
            tint = cs.accent.copy(alpha = alpha),
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Every Input-tier component. */
internal val undercurrentInputComponents: List<WeftComponent<*>> = listOf(
    FieldComponent(),
    ToggleComponent(),
    StepperComponent(),
)
