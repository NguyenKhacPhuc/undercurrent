package dev.weft.undercurrent.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// Heatmap — GitHub-style activity grid
// =============================================================================

@Serializable
internal data class HeatmapProps(
    /** Cell values in column-major order (each column = a week). */
    val values: List<Float>,
    /** Number of rows (typically 7 for days of week). */
    val rows: Int = 7,
    /** Auto-computed when 0; pass an explicit max to pin the scale. */
    val maxValue: Float = 0f,
    /** Optional label shown above the grid. */
    val label: String = "",
)

internal class HeatmapComponent : WeftComponent<HeatmapProps>(
    name = "Heatmap",
    description = "GitHub-style activity heatmap. values: cell values in COLUMN-MAJOR order (each row = a day, each column = a week — 7 entries per column). rows: usually 7. maxValue: 0 = auto-scale. Cell color interpolates from background to accent based on value/max. Use for habit streaks, contribution graphs.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = HeatmapProps.serializer(),
    example = """{"type": "Heatmap", "props": {"label": "Last 8 weeks", "rows": 7, "values": [0,0,1,2,0,1,3, 2,3,4,2,1,2,3, 1,2,3,4,5,3,2, 3,4,2,1,0,1,2, 2,3,4,5,4,3,2, 3,4,5,4,3,2,1, 4,3,2,3,4,5,4, 2,3,4,3,2,1,2]}}""",
) {
    @Composable
    override fun Render(props: HeatmapProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val rows = props.rows.coerceAtLeast(1)
        val cols = (props.values.size + rows - 1) / rows
        if (cols == 0) {
            EmptyChartBox(message = "Heatmap needs at least one value.")
            return
        }
        val maxV = if (props.maxValue > 0f) props.maxValue else props.values.maxOrNull() ?: 1f
        val safeMax = maxV.coerceAtLeast(0.0001f)
        val gap = 3.dp
        val targetHeight = 132.dp

        Column(modifier = Modifier.fillMaxWidth()) {
            if (props.label.isNotBlank()) {
                Text(
                    text = props.label,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = cs.ink,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Canvas(modifier = Modifier.fillMaxWidth().height(targetHeight)) {
                val gapPx = gap.toPx()
                val w = size.width
                val h = size.height
                val cellH = ((h - gapPx * (rows - 1)) / rows).coerceAtLeast(2f)
                val cellW = ((w - gapPx * (cols - 1)) / cols).coerceAtLeast(2f)
                for (col in 0 until cols) {
                    for (row in 0 until rows) {
                        val idx = col * rows + row
                        if (idx >= props.values.size) continue
                        val intensity = (props.values[idx] / safeMax).coerceIn(0f, 1f)
                        // Mix surfaceMuted → accent based on intensity.
                        val color = mix(cs.surfaceMuted, cs.accent, intensity)
                        val x = col * (cellW + gapPx)
                        val y = row * (cellH + gapPx)
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(cellW, cellH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                        )
                    }
                }
            }
            // Legend.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Less", style = tp.sansSmall, color = cs.inkMuted)
                listOf(0f, 0.3f, 0.6f, 1f).forEach { intensity ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(width = 12.dp, height = 8.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(mix(cs.surfaceMuted, cs.accent, intensity)),
                    )
                }
                Text("  More", style = tp.sansSmall, color = cs.inkMuted)
            }
        }
    }

    private fun mix(
        a: androidx.compose.ui.graphics.Color,
        b: androidx.compose.ui.graphics.Color,
        t: Float,
    ): androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = a.alpha + (b.alpha - a.alpha) * t,
    )
}

// =============================================================================
// Gauge — semi-circle gauge with center value
// =============================================================================

@Serializable
internal data class GaugeProps(
    /** 0..1 — gauge fill fraction. */
    val value: Float,
    /** Label shown in the center. Default = '${value*100}%'. */
    val label: String = "",
    /** Small caption under the label. */
    val caption: String = "",
    /** sm (120dp wide) | md (180dp, default) | lg (240dp). */
    val size: String = "md",
    /** good (accent), warn (yellow-ish), bad (error) — colors the fill. Default 'good'. */
    val tone: String = "good",
)

internal class GaugeComponent : WeftComponent<GaugeProps>(
    name = "Gauge",
    description = "Semi-circle gauge — half-arc with fill proportional to value. value: 0..1 required. label: center label (defaults to percentage). caption: small caption under it. size: sm/md (default)/lg. tone: 'good' (accent, default), 'warn' (warning), 'bad' (error). Use for KPIs that have a 'full = good' meaning (speedometer, capacity).",
    category = ComponentCategory.DISPLAY,
    propsSerializer = GaugeProps.serializer(),
) {
    @Composable
    override fun Render(props: GaugeProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val pct = props.value.coerceIn(0f, 1f)
        val fillColor = when (props.tone.lowercase()) {
            "bad" -> cs.error
            "warn" -> cs.error.copy(alpha = 0.7f)
            else -> cs.accent
        }
        val width = when (props.size.lowercase()) {
            "sm" -> 120.dp
            "lg" -> 240.dp
            else -> 180.dp
        }

        Box(
            modifier = Modifier
                .width(width)
                .aspectRatio(2f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(2f)) {
                val stroke = size.width * 0.085f
                // Arc rect: square anchored at top, spanning the full width.
                val arcRect = Rect(
                    offset = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, (size.width - stroke)),
                )
                drawArc(
                    color = cs.surfaceMuted,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = fillColor,
                    startAngle = 180f,
                    sweepAngle = 180f * pct,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                Text(
                    text = props.label.ifBlank { "${(pct * 100).toInt()}%" },
                    style = tp.serifBodyLarge.copy(
                        fontSize = (width.value * 0.18f).sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = cs.ink,
                )
                if (props.caption.isNotBlank()) {
                    Text(text = props.caption, style = tp.sansSmall, color = cs.inkMuted)
                }
            }
        }
    }
}

// =============================================================================
// Comparison — two-column "this vs that" table
// =============================================================================

@Serializable
internal data class ComparisonRow(
    val attribute: String,
    val left: String,
    val right: String,
    /** Optional winner: 'left' | 'right' | 'tie' | '' (none). */
    val winner: String = "",
)

@Serializable
internal data class ComparisonProps(
    val leftLabel: String,
    val rightLabel: String,
    val rows: List<ComparisonRow>,
)

internal class ComparisonComponent : WeftComponent<ComparisonProps>(
    name = "Comparison",
    description = "Two-column 'this vs that' comparison table. leftLabel / rightLabel: column headers. rows: list of {attribute, left, right, winner: 'left'|'right'|'tie'|''}. Winning side gets a subtle accent highlight. Use for product comparisons, options weighing.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = ComparisonProps.serializer(),
    example = """{"type": "Comparison", "props": {"leftLabel": "Option A", "rightLabel": "Option B", "rows": [{"attribute": "Price", "left": "$12", "right": "$8", "winner": "right"}, {"attribute": "Rating", "left": "4.5", "right": "4.2", "winner": "left"}, {"attribute": "Shipping", "left": "2 days", "right": "2 days", "winner": "tie"}]}}""",
) {
    @Composable
    override fun Render(props: ComparisonProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(modifier = Modifier.fillMaxWidth().clip(UndercurrentTheme.shapes.medium).background(cs.surfaceMuted)) {
            // Header row.
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                Box(modifier = Modifier.weight(1f))
                Text(
                    text = props.leftLabel,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = cs.ink,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = props.rightLabel,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = cs.ink,
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider(color = cs.divider)
            props.rows.forEachIndexed { i, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row.attribute,
                        style = tp.sansSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = cs.inkMuted,
                        modifier = Modifier.weight(1f),
                    )
                    ComparisonCell(
                        text = row.left,
                        emphasized = row.winner.lowercase() == "left" || row.winner.lowercase() == "tie",
                        cs = cs,
                        tp = tp,
                        modifier = Modifier.weight(1f),
                    )
                    ComparisonCell(
                        text = row.right,
                        emphasized = row.winner.lowercase() == "right" || row.winner.lowercase() == "tie",
                        cs = cs,
                        tp = tp,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (i < props.rows.size - 1) HorizontalDivider(color = cs.divider)
            }
        }
    }

    @Composable
    private fun ComparisonCell(
        text: String,
        emphasized: Boolean,
        cs: dev.weft.undercurrent.core.designsystem.UndercurrentColors,
        tp: dev.weft.undercurrent.core.designsystem.UndercurrentTypography,
        modifier: Modifier,
    ) {
        Text(
            text = text,
            style = tp.serifBody.copy(
                fontSize = 14.sp,
                fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (emphasized) cs.accent else cs.ink,
            modifier = modifier,
        )
    }
}

@Composable
private fun EmptyChartBox(message: String) {
    val cs = UndercurrentTheme.colors
    val tp = UndercurrentTheme.typography
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(UndercurrentTheme.shapes.medium)
            .background(cs.surfaceMuted),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, style = tp.sansSmall, color = cs.inkMuted)
    }
}

/** Every DataVizPlus component. */
internal val undercurrentDataVizPlusComponents: List<WeftComponent<*>> = listOf(
    HeatmapComponent(),
    GaugeComponent(),
    ComparisonComponent(),
)
