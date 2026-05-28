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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
// Dialog — modal-styled centered card (renders inline, not as system overlay)
// =============================================================================

@Serializable
internal data class DialogAction(
    val label: String,
    val onTap: String,
    /** primary (filled) | secondary (outlined) | ghost (text). */
    val variant: String = "primary",
    /** Mark as destructive — uses error tint. */
    val destructive: Boolean = false,
)

@Serializable
internal data class DialogProps(
    val title: String,
    val body: String = "",
    /** Optional leading icon. */
    val icon: String = "",
    val actions: List<DialogAction> = emptyList(),
)

internal class DialogComponent : WeftComponent<DialogProps>(
    name = "Dialog",
    description = "Modal-styled confirmation card — title + body + 1-3 actions on the right. Renders inline (not as a real system overlay) but is styled like a centered dialog. title: required. body: optional (or pass a SINGLE child for a rich body). icon: optional leading icon name. actions: list of {label, onTap, variant, destructive}. Use for confirmations, important asks.",
    category = ComponentCategory.LAYOUT,
    propsSerializer = DialogProps.serializer(),
    example = """{"type": "Dialog", "props": {"icon": "warning", "title": "Delete this conversation?", "body": "This can't be undone.", "actions": [{"label": "Cancel", "onTap": "cancel", "variant": "ghost"}, {"label": "Delete", "onTap": "confirm_delete", "variant": "primary", "destructive": true}]}}""",
) {
    @Composable
    override fun Render(props: DialogProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val node = LocalCurrentNode.current
        val renderNode = LocalNodeRenderer.current
        val hasChildBody = node.children.isNotEmpty()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.background)
                .border(1.dp, cs.divider, UndercurrentTheme.shapes.medium)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                if (props.icon.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(UndercurrentTheme.shapes.small)
                            .background(cs.accent.copy(alpha = 0.12f)),
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
                Column(modifier = Modifier.weight(1f).padding(start = if (props.icon.isNotBlank()) 14.dp else 0.dp)) {
                    Text(
                        text = props.title,
                        style = tp.sansHeader.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                        color = cs.ink,
                    )
                    if (props.body.isNotBlank()) {
                        Text(
                            text = props.body,
                            style = tp.serifBody.copy(fontSize = 14.sp, lineHeight = 21.sp),
                            color = cs.inkMuted,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    if (hasChildBody) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            node.children.forEach { child -> renderNode(child) }
                        }
                    }
                }
            }
            if (props.actions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    props.actions.forEach { action ->
                        DialogActionButton(action = action, cs = cs, tp = tp, onEvent = onEvent)
                    }
                }
            }
        }
    }

    @Composable
    private fun DialogActionButton(
        action: DialogAction,
        cs: dev.weft.undercurrent.core.designsystem.UndercurrentColors,
        tp: dev.weft.undercurrent.core.designsystem.UndercurrentTypography,
        onEvent: (ComponentEvent) -> Unit,
    ) {
        val baseColor = if (action.destructive) cs.error else cs.accent
        val (bg, fg, borderC) = when (action.variant.lowercase()) {
            "secondary" -> Triple(cs.background, baseColor, cs.divider)
            "ghost" -> Triple(androidx.compose.ui.graphics.Color.Transparent, baseColor, androidx.compose.ui.graphics.Color.Transparent)
            else -> Triple(baseColor, cs.onAccent, baseColor)
        }
        Box(
            modifier = Modifier
                .clip(UndercurrentTheme.shapes.small)
                .background(bg)
                .border(if (action.variant.lowercase() == "secondary") 1.dp else 0.dp, borderC, UndercurrentTheme.shapes.small)
                .clickable {
                    onEvent(
                        ComponentEvent.Action(
                            action = action.onTap,
                            sourceType = "Dialog",
                            sourceLabel = action.label,
                        ),
                    )
                }
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text(
                text = action.label,
                style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = fg,
            )
        }
    }
}

// =============================================================================
// Toast — transient inline pill with icon + message
// =============================================================================

@Serializable
internal data class ToastProps(
    val text: String,
    /** info | success | warning | error. */
    val tone: String = "info",
    /** Override the default tone icon. */
    val icon: String = "",
)

internal class ToastComponent : WeftComponent<ToastProps>(
    name = "Toast",
    description = "Transient inline pill — colored icon + message, fits at the bottom of a screen or above content. text: required. tone: 'info' (default), 'success', 'warning', 'error' — controls accent + default icon. icon: optional override. Renders inline (not as a real auto-dismissing toast) — host or follow-up turn is responsible for clearing.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = ToastProps.serializer(),
) {
    @Composable
    override fun Render(props: ToastProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val (accent, defaultIcon) = when (props.tone.lowercase()) {
            "success" -> cs.accent to "check"
            "warning" -> cs.error to "warning"
            "error" -> cs.error to "close"
            else -> cs.accent to "info"
        }
        val iconName = props.icon.ifBlank { defaultIcon }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(cs.ink.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = undercurrentIcon(iconName),
                contentDescription = props.tone,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = props.text,
                style = tp.sansLabel.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = cs.background,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Every overlay component. */
internal val undercurrentOverlayComponents: List<WeftComponent<*>> = listOf(
    DialogComponent(),
    ToastComponent(),
)
