package dev.weft.undercurrent.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.components.LocalCurrentNode
import dev.weft.compose.components.LocalNodeRenderer
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.cd_collapse
import dev.weft.undercurrent.core.resources.cd_expand
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

// =============================================================================
// Stack — vertical column with Undercurrent spacing
// =============================================================================

@Serializable
internal data class StackProps(
    /** none | xs | sm | md (default) | lg | xl | xxl | xxxl. */
    val spacing: String = "md",
    val padding: String = "none",
    /** start | center | end | stretch. */
    val align: String = "stretch",
)

internal class StackComponent : WeftComponent<StackProps>(
    name = "Stack",
    description = "Vertical stack of children with Undercurrent spacing. spacing: between children (md default). padding: around outside (none default). align: 'stretch' default (children fill width), 'start', 'center', 'end'. Prefer this over raw Column.",
    category = ComponentCategory.LAYOUT,
    propsSerializer = StackProps.serializer(),
) {
    @Composable
    override fun Render(props: StackProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val align = when (props.align.lowercase()) {
            "center" -> Alignment.CenterHorizontally
            "end" -> Alignment.End
            "start" -> Alignment.Start
            else -> Alignment.Start
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(undercurrentSpacing(props.spacing)),
            horizontalAlignment = align,
            modifier = Modifier
                .fillMaxWidth()
                .padding(undercurrentSpacing(props.padding)),
        ) { children() }
    }
}

// =============================================================================
// Inline — horizontal row with Undercurrent spacing
// =============================================================================

@Serializable
internal data class InlineProps(
    val spacing: String = "sm",
    val padding: String = "none",
    /** start | center | end | between | around. */
    val align: String = "start",
    /** top | center | bottom | baseline. */
    val verticalAlign: String = "center",
    /** If true, the row wraps content; if false (default), fillMaxWidth. */
    val fitContent: Boolean = false,
)

internal class InlineComponent : WeftComponent<InlineProps>(
    name = "Inline",
    description = "Horizontal row of children. spacing: between siblings (sm default). align: start (default), center, end, between (SpaceBetween), around (SpaceEvenly). verticalAlign: center (default), top, bottom. fitContent: false (default, full width) / true (hugs children).",
    category = ComponentCategory.LAYOUT,
    propsSerializer = InlineProps.serializer(),
) {
    @Composable
    override fun Render(props: InlineProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val gap = undercurrentSpacing(props.spacing)
        val arrangement = when (props.align.lowercase()) {
            "center" -> Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
            "end" -> Arrangement.spacedBy(gap, Alignment.End)
            "between" -> Arrangement.SpaceBetween
            "around" -> Arrangement.SpaceEvenly
            else -> Arrangement.spacedBy(gap)
        }
        val verticalAlign = when (props.verticalAlign.lowercase()) {
            "top" -> Alignment.Top
            "bottom" -> Alignment.Bottom
            else -> Alignment.CenterVertically
        }
        Row(
            horizontalArrangement = arrangement,
            verticalAlignment = verticalAlign,
            modifier = (if (props.fitContent) Modifier else Modifier.fillMaxWidth())
                .padding(undercurrentSpacing(props.padding)),
        ) { children() }
    }
}

// =============================================================================
// Section — titled block (title + optional subtitle + body children)
// =============================================================================

@Serializable
internal data class SectionProps(
    val title: String = "",
    /** Short caption above the title (uppercase tracking). */
    val kicker: String = "",
    val subtitle: String = "",
    /** Spacing between children inside the body. */
    val spacing: String = "md",
)

internal class SectionComponent : WeftComponent<SectionProps>(
    name = "Section",
    description = "A semantic block: optional kicker + title + subtitle, then children as the body. Use to organize a screen into named regions. kicker: small uppercase lead-in. spacing: between children inside the body (md default).",
    category = ComponentCategory.LAYOUT,
    propsSerializer = SectionProps.serializer(),
) {
    @Composable
    override fun Render(props: SectionProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val tp = UndercurrentTheme.typography
        val cs = UndercurrentTheme.colors
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (props.kicker.isNotBlank()) {
                Text(
                    text = props.kicker.uppercase(),
                    style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp),
                    color = cs.accent,
                )
            }
            if (props.title.isNotBlank()) {
                Text(
                    text = props.title,
                    style = tp.sansHeader.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.ink,
                )
            }
            if (props.subtitle.isNotBlank()) {
                Text(
                    text = props.subtitle,
                    style = tp.serifBody.copy(fontSize = 14.sp, lineHeight = 21.sp),
                    color = cs.inkMuted,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(undercurrentSpacing(props.spacing))) {
                children()
            }
        }
    }
}

// =============================================================================
// Sheet — surface card themed with UndercurrentShapes
// =============================================================================

@Serializable
internal data class SheetProps(
    /** soft (default) | bordered | elevated. */
    val variant: String = "soft",
    /** Padding inside the sheet. */
    val padding: String = "lg",
    /** Spacing between children inside. */
    val spacing: String = "md",
)

internal class SheetComponent : WeftComponent<SheetProps>(
    name = "Sheet",
    description = "A surface card. variant: 'soft' (default, surfaceMuted fill), 'bordered' (background + divider outline), 'elevated' (surface + subtle shadow). padding: inside (lg default). spacing: between children (md default). Use to visually group content.",
    category = ComponentCategory.LAYOUT,
    propsSerializer = SheetProps.serializer(),
) {
    @Composable
    override fun Render(props: SheetProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val shape = UndercurrentTheme.shapes.medium
        val base = Modifier
            .fillMaxWidth()
            .clip(shape)
        val modifier = when (props.variant.lowercase()) {
            "bordered" -> base.background(cs.background).border(1.dp, cs.divider, shape)
            "elevated" -> base.background(cs.surface)
            else -> base.background(cs.surfaceMuted)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(undercurrentSpacing(props.spacing)),
            modifier = modifier.padding(undercurrentSpacing(props.padding)),
        ) { children() }
    }
}

// =============================================================================
// Grid — 2-column responsive grid (children flow into cells)
// =============================================================================

@Serializable
internal data class GridProps(
    /** 2 | 3 — number of columns. */
    val columns: Int = 2,
    /** Spacing between cells (both directions). */
    val spacing: String = "md",
)

internal class GridComponent : WeftComponent<GridProps>(
    name = "Grid",
    description = "A 2 or 3 column grid. Children flow into cells row by row. columns: 2 (default) or 3. spacing: between cells (md default). Great for laying out Stat cards or pairs of Sheets.",
    category = ComponentCategory.LAYOUT,
    propsSerializer = GridProps.serializer(),
    example = """{"type": "Grid", "props": {"columns": 2}, "children": [{"type": "Sheet", "children": [{"type": "Stat", "props": {"value": "42", "label": "Active"}}]}, {"type": "Sheet", "children": [{"type": "Stat", "props": {"value": "7", "label": "Pending"}}]}]}""",
) {
    @Composable
    override fun Render(props: GridProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val node = LocalCurrentNode.current
        val renderNode = LocalNodeRenderer.current
        val cols = props.columns.coerceIn(2, 3)
        val gap = undercurrentSpacing(props.spacing)
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
            node.children.chunked(cols).forEach { rowChildren ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    rowChildren.forEach { child ->
                        Box(modifier = Modifier.weight(1f)) { renderNode(child) }
                    }
                    // Pad short trailing rows so the last cell doesn't stretch
                    // across the empty slots.
                    repeat(cols - rowChildren.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// =============================================================================
// Reveal — collapsible disclosure (label + chevron, single child as body)
// =============================================================================

@Serializable
internal data class RevealProps(
    val title: String,
    /** Whether the section starts expanded. */
    val initialOpen: Boolean = false,
    /** Optional small subtitle next to the title. */
    val subtitle: String = "",
)

internal class RevealComponent : WeftComponent<RevealProps>(
    name = "Reveal",
    description = "A collapsible disclosure. Tapping the title row toggles the body. title: required. subtitle: optional aside. initialOpen: false default. Renders the FIRST child as the body — emit one child (use Stack inside if you need multiple).",
    category = ComponentCategory.LAYOUT,
    propsSerializer = RevealProps.serializer(),
    example = """{"type": "Reveal", "props": {"title": "Show details", "initialOpen": false}, "children": [{"type": "Stack", "children": [{"type": "Text", "props": {"text": "Hidden content here"}}]}]}""",
) {
    @Composable
    override fun Render(props: RevealProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val node = LocalCurrentNode.current
        val renderNode = LocalNodeRenderer.current
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        var open by remember { mutableStateOf(props.initialOpen) }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { open = !open }
                    .padding(vertical = 10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = props.title,
                        style = tp.sansHeader.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
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
                Icon(
                    imageVector = if (open) undercurrentIcon("collapse") else undercurrentIcon("expand"),
                    contentDescription = if (open) stringResource(Res.string.cd_collapse) else stringResource(Res.string.cd_expand),
                    tint = cs.inkMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
            HorizontalDivider(color = cs.divider)
            if (open) {
                Box(modifier = Modifier.padding(vertical = 12.dp)) {
                    node.children.firstOrNull()?.let { renderNode(it) }
                }
            }
        }
    }
}

/** Every Layout-tier component. */
internal val undercurrentLayoutComponents: List<WeftComponent<*>> = listOf(
    StackComponent(),
    InlineComponent(),
    SectionComponent(),
    SheetComponent(),
    GridComponent(),
    RevealComponent(),
)
