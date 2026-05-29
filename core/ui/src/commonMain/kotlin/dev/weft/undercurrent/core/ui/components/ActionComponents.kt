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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kotlinx.serialization.Serializable

// =============================================================================
// Button — primary / secondary / ghost
// =============================================================================

@Serializable
internal data class ButtonProps(
    val label: String,
    /** Action key sent back to the agent when tapped. */
    val onTap: String,
    /** primary (default, filled) | secondary (outlined) | ghost (text-only). */
    val variant: String = "primary",
    /** Optional leading icon name. Blank = no icon. */
    val icon: String = "",
    /** Whether the button stretches to fill its parent's width. */
    val fullWidth: Boolean = true,
    /** Disable interaction. */
    val disabled: Boolean = false,
)

internal class ButtonComponent : WeftComponent<ButtonProps>(
    name = "Button",
    description = "A tappable button. label: required. onTap: action key (e.g. 'save_note', 'open_settings'). variant: 'primary' (default, filled accent), 'secondary' (outlined), 'ghost' (text-only). icon: optional leading icon. fullWidth: true default. disabled: false default.",
    category = ComponentCategory.ACTION,
    propsSerializer = ButtonProps.serializer(),
) {
    @Composable
    override fun Render(props: ButtonProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val shape = UndercurrentTheme.shapes.medium

        val (bg, fg, borderColor) = when (props.variant.lowercase()) {
            "secondary" -> Triple(cs.background, cs.ink, cs.divider)
            "ghost" -> Triple(cs.background.copy(alpha = 0f), cs.accent, cs.background.copy(alpha = 0f))
            else -> Triple(cs.accent, cs.onAccent, cs.accent)
        }
        val alpha = if (props.disabled) 0.5f else 1f

        Box(
            modifier = (if (props.fullWidth) Modifier.fillMaxWidth() else Modifier)
                .clip(shape)
                .background(bg.copy(alpha = bg.alpha * alpha))
                .let { if (props.variant.lowercase() == "secondary") it.border(1.dp, borderColor, shape) else it }
                .clickable(enabled = !props.disabled) {
                    onEvent(
                        ComponentEvent.Action(
                            action = props.onTap,
                            sourceType = "Button",
                            sourceLabel = props.label,
                        ),
                    )
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (props.icon.isNotBlank()) {
                    Icon(
                        imageVector = undercurrentIcon(props.icon),
                        contentDescription = null,
                        tint = fg.copy(alpha = alpha),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = props.label,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = fg.copy(alpha = alpha),
                )
            }
        }
    }
}

// =============================================================================
// Link — inline text link with accent color + underline
// =============================================================================

@Serializable
internal data class LinkProps(
    val text: String,
    val onTap: String,
)

internal class LinkComponent : WeftComponent<LinkProps>(
    name = "Link",
    description = "An inline text link. Tapping fires the action. Use within sentences ('Tap here to learn more.') or as small inline affordances; use Button for primary CTAs.",
    category = ComponentCategory.ACTION,
    propsSerializer = LinkProps.serializer(),
) {
    @Composable
    override fun Render(props: LinkProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Text(
            text = props.text,
            style = tp.serifBody.copy(textDecoration = TextDecoration.Underline),
            color = cs.accent,
            modifier = Modifier.clickable {
                onEvent(
                    ComponentEvent.Action(
                        action = props.onTap,
                        sourceType = "Link",
                        sourceLabel = props.text,
                    ),
                )
            },
        )
    }
}

// =============================================================================
// TapCard — clickable surface (whole card is the tap target)
// =============================================================================

@Serializable
internal data class TapCardProps(
    val onTap: String,
    val sourceLabel: String = "",
    /** soft (default) | bordered | elevated — matches Sheet variants. */
    val variant: String = "soft",
    val padding: String = "lg",
    val spacing: String = "sm",
)

internal class TapCardComponent : WeftComponent<TapCardProps>(
    name = "TapCard",
    description = "A clickable card surface — the whole card is one tap target. Children render inside as the card's body. onTap: required action key. sourceLabel: optional human label for tracing. variant/padding/spacing: same as Sheet. Use for list items like 'today's notes', 'open mini-app X'.",
    category = ComponentCategory.ACTION,
    propsSerializer = TapCardProps.serializer(),
    example = """{"type": "TapCard", "props": {"onTap": "open_note", "sourceLabel": "Morning notes"}, "children": [{"type": "Heading", "props": {"kicker": "Today", "text": "Morning notes", "level": 3}}, {"type": "Text", "props": {"text": "3 entries", "tone": "muted", "variant": "small"}}]}""",
) {
    @Composable
    override fun Render(props: TapCardProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val shape = UndercurrentTheme.shapes.medium
        val base = Modifier
            .fillMaxWidth()
            .clip(shape)
        val styled = when (props.variant.lowercase()) {
            "bordered" -> base.background(cs.background).border(1.dp, cs.divider, shape)
            "elevated" -> base.background(cs.surface)
            else -> base.background(cs.surfaceMuted)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(undercurrentSpacing(props.spacing)),
            modifier = styled
                .clickable {
                    onEvent(
                        ComponentEvent.Action(
                            action = props.onTap,
                            sourceType = "TapCard",
                            sourceLabel = props.sourceLabel.ifBlank { props.onTap },
                        ),
                    )
                }
                .padding(undercurrentSpacing(props.padding)),
        ) { children() }
    }
}

// =============================================================================
// IconAction — round icon-only button
// =============================================================================

@Serializable
internal data class IconActionProps(
    val icon: String,
    val onTap: String,
    /** Accessibility label. */
    val label: String = "",
    /** sm (32) | md (40) (default) | lg (48). */
    val size: String = "md",
    /** filled (accent fill) | tinted (default, soft) | ghost (transparent). */
    val variant: String = "tinted",
)

internal class IconActionComponent : WeftComponent<IconActionProps>(
    name = "IconAction",
    description = "Round icon-only tappable button. icon: required name. onTap: action key. label: accessibility label. size: sm/md (default)/lg. variant: 'filled' (accent fill), 'tinted' (soft accent, default), 'ghost' (transparent). Use for compact actions next to titles.",
    category = ComponentCategory.ACTION,
    propsSerializer = IconActionProps.serializer(),
) {
    @Composable
    override fun Render(props: IconActionProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val px = when (props.size.lowercase()) {
            "sm" -> 32.dp
            "lg" -> 48.dp
            else -> 40.dp
        }
        val iconSize = when (props.size.lowercase()) {
            "sm" -> 18.dp
            "lg" -> 26.dp
            else -> 22.dp
        }
        val (bg, fg) = when (props.variant.lowercase()) {
            "filled" -> cs.accent to cs.onAccent
            "ghost" -> cs.background.copy(alpha = 0f) to cs.ink
            else -> cs.accent.copy(alpha = 0.14f) to cs.accent
        }
        Box(
            modifier = Modifier
                .size(px)
                .clip(CircleShape)
                .background(bg)
                .clickable {
                    onEvent(
                        ComponentEvent.Action(
                            action = props.onTap,
                            sourceType = "IconAction",
                            sourceLabel = props.label.ifBlank { props.icon },
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = undercurrentIcon(props.icon),
                contentDescription = props.label.ifBlank { props.icon },
                tint = fg,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

// =============================================================================
// Choice — single-select option row (radio-style)
// =============================================================================

@Serializable
internal data class ChoiceProps(
    val title: String,
    val onTap: String,
    val description: String = "",
    /** Marks this option as currently selected. */
    val selected: Boolean = false,
    /** Optional leading icon. */
    val icon: String = "",
)

internal class ChoiceComponent : WeftComponent<ChoiceProps>(
    name = "Choice",
    description = "A single selectable option row with title + optional description. title: required. onTap: action key fired on tap. selected: whether this is the current choice (default false). description: small explanatory text. icon: optional leading icon. Stack multiple Choices in a Stack to form a single-select group.",
    category = ComponentCategory.ACTION,
    propsSerializer = ChoiceProps.serializer(),
) {
    @Composable
    override fun Render(props: ChoiceProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val shape = UndercurrentTheme.shapes.small
        val borderColor = if (props.selected) cs.accent else cs.divider
        val bg = if (props.selected) cs.accent.copy(alpha = 0.06f) else cs.background

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(bg)
                .border(if (props.selected) 1.5.dp else 1.dp, borderColor, shape)
                .clickable {
                    onEvent(
                        ComponentEvent.Action(
                            action = props.onTap,
                            sourceType = "Choice",
                            sourceLabel = props.title,
                        ),
                    )
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (props.icon.isNotBlank()) {
                Icon(
                    imageVector = undercurrentIcon(props.icon),
                    contentDescription = null,
                    tint = if (props.selected) cs.accent else cs.inkMuted,
                    modifier = Modifier.size(20.dp).padding(end = 4.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = props.title,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = if (props.selected) cs.accent else cs.ink,
                )
                if (props.description.isNotBlank()) {
                    Text(
                        text = props.description,
                        style = tp.sansSmall,
                        color = cs.inkMuted,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(if (props.selected) 5.dp else 1.5.dp, if (props.selected) cs.accent else cs.divider, CircleShape),
            )
        }
    }
}

/** Every Action-tier component. */
internal val undercurrentActionComponents: List<WeftComponent<*>> = listOf(
    ButtonComponent(),
    LinkComponent(),
    TapCardComponent(),
    IconActionComponent(),
    ChoiceComponent(),
)
