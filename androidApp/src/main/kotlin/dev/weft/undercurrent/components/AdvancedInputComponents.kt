package dev.weft.undercurrent.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
// SegmentedToggle — pill segmented selector (single-select)
// =============================================================================

@Serializable
internal data class SegmentedToggleProps(
    val id: String,
    val options: List<String>,
    /** 0-based index of the initially selected option. */
    val initial: Int = 0,
)

internal class SegmentedToggleComponent : WeftComponent<SegmentedToggleProps>(
    name = "SegmentedToggle",
    description = "Pill-shaped segmented control for picking one option. id: stable identifier (fires TextChanged with the picked label). options: 2-5 option labels. initial: 0-based default selection. Use for view toggles (Today/Week/Month), category filters.",
    category = ComponentCategory.INPUT,
    propsSerializer = SegmentedToggleProps.serializer(),
) {
    @Composable
    override fun Render(props: SegmentedToggleProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        if (props.options.isEmpty()) return
        var selected by remember(props.id) {
            mutableIntStateOf(props.initial.coerceIn(0, props.options.size - 1))
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(cs.surfaceMuted)
                .padding(3.dp),
        ) {
            props.options.forEachIndexed { i, label ->
                val isActive = i == selected
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isActive) cs.accent else cs.surfaceMuted.copy(alpha = 0f))
                        .clickable {
                            selected = i
                            onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = label))
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = label,
                        style = tp.sansLabel.copy(
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp,
                        ),
                        color = if (isActive) cs.onAccent else cs.inkMuted,
                    )
                }
            }
        }
    }
}

// =============================================================================
// Rating — 1-5 star rating input
// =============================================================================

@Serializable
internal data class RatingProps(
    val id: String,
    val initial: Int = 0,
    /** Number of stars to render. */
    val max: Int = 5,
    /** Read-only display mode — no taps. */
    val readOnly: Boolean = false,
)

internal class RatingComponent : WeftComponent<RatingProps>(
    name = "Rating",
    description = "Star rating input or display. id: stable identifier (fires TextChanged with the rating as a string). initial: 0-5 default. max: number of stars (default 5). readOnly: true to disable taps. Tap a star to set rating to that value; tap the active star to clear.",
    category = ComponentCategory.INPUT,
    propsSerializer = RatingProps.serializer(),
) {
    @Composable
    override fun Render(props: RatingProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        var rating by remember(props.id) { mutableIntStateOf(props.initial.coerceIn(0, props.max)) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(props.max) { i ->
                val active = i < rating
                Icon(
                    imageVector = undercurrentIcon(if (active) "star" else "star_outline"),
                    contentDescription = "${i + 1} star",
                    tint = if (active) cs.accent else cs.inkSubtle,
                    modifier = Modifier
                        .size(28.dp)
                        .let { m ->
                            if (props.readOnly) m else m.clickable {
                                rating = if (rating == i + 1) 0 else i + 1
                                onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = rating.toString()))
                            }
                        },
                )
            }
        }
    }
}

// =============================================================================
// MoodScale — emoji-style 1-5 mood picker
// =============================================================================

@Serializable
internal data class MoodScaleProps(
    val id: String,
    val initial: Int = -1,
    /** Override the default emoji set. Must have 5 entries. */
    val emojis: List<String> = emptyList(),
)

internal class MoodScaleComponent : WeftComponent<MoodScaleProps>(
    name = "MoodScale",
    description = "5-step emoji mood picker. id: stable identifier (fires TextChanged with the 1-5 value). initial: -1 default (nothing selected). emojis: optional override (must have exactly 5). Use for journaling, daily check-ins.",
    category = ComponentCategory.INPUT,
    propsSerializer = MoodScaleProps.serializer(),
) {
    @Composable
    override fun Render(props: MoodScaleProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val defaultSet = listOf("😔", "😕", "😐", "🙂", "😊")
        val set = props.emojis.takeIf { it.size == 5 } ?: defaultSet
        var picked by remember(props.id) { mutableIntStateOf(props.initial) }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            set.forEachIndexed { i, emoji ->
                val active = picked == i + 1
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (active) cs.accent.copy(alpha = 0.15f) else cs.surfaceMuted)
                        .border(
                            width = if (active) 2.dp else 0.dp,
                            color = if (active) cs.accent else cs.divider,
                            shape = CircleShape,
                        )
                        .clickable {
                            picked = i + 1
                            onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = (i + 1).toString()))
                        },
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = emoji, style = tp.serifBodyLarge.copy(fontSize = 26.sp))
                }
            }
        }
    }
}

// =============================================================================
// DateStrip — horizontal week strip showing 7 days
// =============================================================================

@Serializable
internal data class DateStripDay(
    val label: String,
    val day: String,
    /** Mark this day as currently selected. */
    val selected: Boolean = false,
    /** Show a small dot under the day number (used for "has activity"). */
    val marked: Boolean = false,
)

@Serializable
internal data class DateStripProps(
    val id: String,
    val days: List<DateStripDay>,
)

internal class DateStripComponent : WeftComponent<DateStripProps>(
    name = "DateStrip",
    description = "Horizontal week strip — typically Mon-Sun with day labels above day numbers. days: required list of {label, day, selected, marked}. id: stable identifier (fires TextChanged with the tapped day number). Tapping a day selects it and fires the event.",
    category = ComponentCategory.INPUT,
    propsSerializer = DateStripProps.serializer(),
    example = """{"type": "DateStrip", "props": {"id": "week", "days": [{"label": "M", "day": "8"}, {"label": "T", "day": "9", "marked": true}, {"label": "W", "day": "10", "selected": true}, {"label": "T", "day": "11"}, {"label": "F", "day": "12"}, {"label": "S", "day": "13", "marked": true}, {"label": "S", "day": "14"}]}}""",
) {
    @Composable
    override fun Render(props: DateStripProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        var selectedDay by remember(props.id) {
            mutableStateOf(props.days.firstOrNull { it.selected }?.day.orEmpty())
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            props.days.forEach { d ->
                val isSelected = selectedDay == d.day
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(UndercurrentTheme.shapes.small)
                        .clickable {
                            selectedDay = d.day
                            onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = d.day))
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = d.label,
                        style = tp.sansSmall.copy(letterSpacing = 0.5.sp),
                        color = cs.inkMuted,
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) cs.accent else cs.background)
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = if (isSelected) cs.accent else cs.divider,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = d.day,
                            style = tp.sansHeader.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) cs.onAccent else cs.ink,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (d.marked && !isSelected) cs.accent
                                else androidx.compose.ui.graphics.Color.Transparent,
                            ),
                    )
                }
            }
        }
    }
}

// =============================================================================
// Composer — note input with submit button (inline editor)
// =============================================================================

@Serializable
internal data class ComposerProps(
    val id: String,
    val placeholder: String = "Write something…",
    val initial: String = "",
    /** Action key fired when the submit button is tapped. */
    val onSubmit: String = "submit",
    val submitLabel: String = "Save",
    /** Min lines visible. */
    val minLines: Int = 3,
)

internal class ComposerComponent : WeftComponent<ComposerProps>(
    name = "Composer",
    description = "Inline note composer — multi-line text field plus a submit button. id: stable identifier (fires TextChanged on every keystroke AND on submit with the full text). onSubmit: action key for the submit button. submitLabel: button label (default 'Save'). placeholder/initial/minLines: optional. Use for quick note entry, comments.",
    category = ComponentCategory.INPUT,
    propsSerializer = ComposerProps.serializer(),
) {
    @Composable
    override fun Render(props: ComposerProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val shape = UndercurrentTheme.shapes.medium
        var value by remember(props.id) { mutableStateOf(props.initial) }
        var focused by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(cs.surfaceMuted)
                .border(if (focused) 1.5.dp else 1.dp, if (focused) cs.accent else cs.divider, shape)
                .padding(12.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = {
                    value = it
                    onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = it))
                },
                textStyle = TextStyle(
                    color = cs.ink,
                    fontSize = 15.sp,
                    fontFamily = tp.serifBody.fontFamily,
                ),
                cursorBrush = SolidColor(cs.accent),
                minLines = props.minLines.coerceAtLeast(1),
                modifier = Modifier
                    .fillMaxWidth()
                    .height((props.minLines * 22 + 16).dp),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (value.isBlank()) cs.divider else cs.accent)
                        .clickable(enabled = value.isNotBlank()) {
                            onEvent(
                                ComponentEvent.Action(
                                    action = props.onSubmit,
                                    sourceType = "Composer",
                                    sourceLabel = props.id,
                                ),
                            )
                            // Also fire the latest text so the agent has it
                            // in the next user message's fieldValues map.
                            onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = value))
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = cs.onAccent,
                            modifier = Modifier.size(16.dp).padding(end = 6.dp),
                        )
                        Text(
                            text = props.submitLabel,
                            style = tp.sansLabel.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            ),
                            color = cs.onAccent,
                        )
                    }
                }
            }
        }
    }
}

/** Every advanced-input component. */
internal val undercurrentAdvancedInputComponents: List<WeftComponent<*>> = listOf(
    SegmentedToggleComponent(),
    RatingComponent(),
    MoodScaleComponent(),
    DateStripComponent(),
    ComposerComponent(),
)
