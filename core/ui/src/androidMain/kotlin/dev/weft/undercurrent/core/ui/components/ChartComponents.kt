package dev.weft.undercurrent.core.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import kotlin.math.max
import kotlin.math.min

// =============================================================================
// LineChart — a single-series line chart drawn on Canvas
// =============================================================================

@Serializable
internal data class LineChartProps(
    /** Y-axis values in left-to-right order. Min 2 points to draw anything. */
    val points: List<Float>,
    /** Optional X-axis labels (rendered at evenly spaced positions). */
    val labels: List<String> = emptyList(),
    /** Show the value of the most recent (rightmost) point as a label. */
    val showLatest: Boolean = true,
    /** Fill the area under the line with a translucent accent. */
    val area: Boolean = true,
    /** Override the auto-computed min/max — leave default for auto-scale. */
    val yMin: Float? = null,
    val yMax: Float? = null,
)

internal class LineChartComponent : WeftComponent<LineChartProps>(
    name = "LineChart",
    description = "A single-series line chart. points: required list of floats (left-to-right). labels: optional X labels (positioned at points). showLatest: true default (shows last value as a chip). area: true default (translucent fill under line). yMin/yMax: optional manual scale. Use for trends over time.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = LineChartProps.serializer(),
) {
    @Composable
    override fun Render(props: LineChartProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        if (props.points.size < 2) {
            ChartEmpty(message = "LineChart needs at least 2 points.")
            return
        }
        val minV = props.yMin ?: props.points.min()
        val maxV = props.yMax ?: props.points.max()
        val span = (maxV - minV).coerceAtLeast(0.0001f)

        Column(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(top = 4.dp),
            ) {
                val w = size.width
                val h = size.height
                val n = props.points.size
                val gap = w / (n - 1).toFloat()
                val coords = props.points.mapIndexed { i, v ->
                    val x = i * gap
                    val y = h - ((v - minV) / span) * (h - 4.dp.toPx()) - 2.dp.toPx()
                    Offset(x, y)
                }
                // Grid: 3 faint horizontal lines for visual rhythm.
                val gridStroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
                repeat(3) { row ->
                    val y = (row + 1) * h / 4f
                    drawLine(cs.divider, Offset(0f, y), Offset(w, y), strokeWidth = gridStroke.width, pathEffect = gridStroke.pathEffect)
                }
                // Area fill
                if (props.area) {
                    val fillPath = Path().apply {
                        moveTo(coords.first().x, h)
                        coords.forEach { lineTo(it.x, it.y) }
                        lineTo(coords.last().x, h)
                        close()
                    }
                    drawPath(fillPath, cs.accent.copy(alpha = 0.14f))
                }
                // Line
                val linePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    coords.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(linePath, cs.accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                // Latest point dot
                if (props.showLatest) {
                    val last = coords.last()
                    drawCircle(cs.background, radius = 5.dp.toPx(), center = last)
                    drawCircle(cs.accent, radius = 4.dp.toPx(), center = last)
                }
            }
            if (props.labels.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    props.labels.forEach { lbl ->
                        Text(text = lbl, style = tp.sansSmall, color = cs.inkMuted)
                    }
                }
            }
            if (props.showLatest) {
                Text(
                    text = formatNum(props.points.last()),
                    style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = cs.accent,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

// =============================================================================
// BarChart — vertical bars
// =============================================================================

@Serializable
internal data class BarChartProps(
    val values: List<Float>,
    val labels: List<String> = emptyList(),
    /** Tint bars with accent (default). 'rotating' uses an accent-derived palette. */
    val palette: String = "accent",
)

internal class BarChartComponent : WeftComponent<BarChartProps>(
    name = "BarChart",
    description = "Vertical bar chart. values: required list of floats (≥ 0 preferred; negatives clamped to 0). labels: optional under-bar labels (must match length). palette: 'accent' default (single color) or 'rotating' (accent + variants). Use for categorical comparisons.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = BarChartProps.serializer(),
) {
    @Composable
    override fun Render(props: BarChartProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        if (props.values.isEmpty()) {
            ChartEmpty(message = "BarChart needs at least 1 value.")
            return
        }
        val maxV = props.values.maxOf { it.coerceAtLeast(0f) }.coerceAtLeast(0.0001f)
        val palette = if (props.palette == "rotating") rotatingPalette(cs.accent, props.values.size) else List(props.values.size) { cs.accent }

        Column(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                val w = size.width
                val h = size.height
                val n = props.values.size
                val slotW = w / n
                val barW = slotW * 0.65f
                val padX = (slotW - barW) / 2f
                props.values.forEachIndexed { i, v ->
                    val safe = v.coerceAtLeast(0f)
                    val barH = (safe / maxV) * (h - 4.dp.toPx())
                    val left = i * slotW + padX
                    val top = h - barH
                    drawRoundRect(
                        color = palette[i],
                        topLeft = Offset(left, top),
                        size = Size(barW, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                }
            }
            if (props.labels.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    props.labels.take(props.values.size).forEach { lbl ->
                        Text(text = lbl, style = tp.sansSmall, color = cs.inkMuted)
                    }
                }
            }
        }
    }
}

// =============================================================================
// Sparkline — minimal inline line chart, no axes, no labels
// =============================================================================

@Serializable
internal data class SparklineProps(
    val points: List<Float>,
    /** xs (24) | sm (32) (default) | md (48) — height in dp. */
    val size: String = "sm",
    /** trend (default, picks up/down/flat from first→last) | accent | muted. */
    val tone: String = "trend",
)

internal class SparklineComponent : WeftComponent<SparklineProps>(
    name = "Sparkline",
    description = "Tiny inline line chart with no axes. Great inside Stat cards or list rows for at-a-glance trends. points: required. size: xs/sm (default)/md. tone: 'trend' (default — green up, red down, muted flat), 'accent', 'muted'.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = SparklineProps.serializer(),
) {
    @Composable
    override fun Render(props: SparklineProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        if (props.points.size < 2) {
            ChartEmpty(message = "Sparkline needs ≥ 2 points.")
            return
        }
        val h = when (props.size.lowercase()) {
            "xs" -> 24.dp
            "md" -> 48.dp
            else -> 32.dp
        }
        val color = when (props.tone.lowercase()) {
            "accent" -> cs.accent
            "muted" -> cs.inkMuted
            else -> when {
                props.points.last() > props.points.first() -> cs.accent
                props.points.last() < props.points.first() -> cs.error
                else -> cs.inkMuted
            }
        }
        val minV = props.points.min()
        val maxV = props.points.max()
        val span = (maxV - minV).coerceAtLeast(0.0001f)

        Canvas(modifier = Modifier.fillMaxWidth().height(h)) {
            val w = size.width
            val hh = this.size.height
            val n = props.points.size
            val gap = w / (n - 1).toFloat()
            val coords = props.points.mapIndexed { i, v ->
                Offset(i * gap, hh - ((v - minV) / span) * (hh - 4.dp.toPx()) - 2.dp.toPx())
            }
            val path = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                coords.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(color, radius = 3.dp.toPx(), center = coords.last())
        }
    }
}

// =============================================================================
// Donut — donut chart for categorical breakdown
// =============================================================================

@Serializable
internal data class DonutSegment(
    val value: Float,
    val label: String = "",
)

@Serializable
internal data class DonutProps(
    val segments: List<DonutSegment>,
    /** Optional center text (e.g. total). Blank = auto-show sum. */
    val centerText: String = "",
    /** Caption under the center number. */
    val centerSubtext: String = "",
    /** Show a legend below the chart. */
    val showLegend: Boolean = true,
)

internal class DonutComponent : WeftComponent<DonutProps>(
    name = "Donut",
    description = "Donut chart for category breakdowns. segments: required list of {value, label}. centerText: optional center label (blank = sum of values). centerSubtext: small caption under center. showLegend: true default. Uses accent-derived rotating palette.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = DonutProps.serializer(),
    example = """{"type": "Donut", "props": {"segments": [{"value": 6, "label": "Work"}, {"value": 3, "label": "Personal"}, {"value": 2, "label": "Health"}], "centerSubtext": "tasks"}}""",
) {
    @Composable
    override fun Render(props: DonutProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        if (props.segments.isEmpty()) {
            ChartEmpty(message = "Donut needs ≥ 1 segment.")
            return
        }
        val total = props.segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.0001f)
        val palette = rotatingPalette(cs.accent, props.segments.size)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.4f),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp)) {
                    val side = min(size.width, size.height)
                    val stroke = side * 0.18f
                    val topLeft = Offset((size.width - side) / 2f + stroke / 2f, (size.height - side) / 2f + stroke / 2f)
                    val arcSize = Size(side - stroke, side - stroke)
                    var start = -90f
                    props.segments.forEachIndexed { i, seg ->
                        val sweep = (seg.value / total) * 360f
                        drawArc(
                            color = palette[i],
                            startAngle = start,
                            sweepAngle = sweep - 1.5f, // tiny gap between segments
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Butt),
                        )
                        start += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = props.centerText.ifBlank { formatNum(total) },
                        style = tp.serifBodyLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                        color = cs.ink,
                    )
                    if (props.centerSubtext.isNotBlank()) {
                        Text(
                            text = props.centerSubtext,
                            style = tp.sansSmall,
                            color = cs.inkMuted,
                        )
                    }
                }
            }
            if (props.showLegend) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    props.segments.forEachIndexed { i, seg ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(palette[i]),
                            )
                            Text(
                                text = "  ${seg.label.ifBlank { "(unnamed)" }}",
                                style = tp.sansSmall,
                                color = cs.ink,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = formatNum(seg.value),
                                    style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = cs.inkMuted,
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// ProgressRing — circular progress with center label
// =============================================================================

@Serializable
internal data class ProgressRingProps(
    /** 0..1 — progress fraction. */
    val value: Float,
    /** Big text in the center. Default = "${value*100}%". */
    val label: String = "",
    /** Small caption under the label. */
    val caption: String = "",
    /** sm (80dp) | md (120dp, default) | lg (160dp). */
    val size: String = "md",
)

internal class ProgressRingComponent : WeftComponent<ProgressRingProps>(
    name = "ProgressRing",
    description = "Circular progress with center label + caption. value: 0..1 required. label: center text (defaults to percentage). caption: small caption under label. size: sm/md (default)/lg. Use for goal completion, focus minutes, etc.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = ProgressRingProps.serializer(),
) {
    @Composable
    override fun Render(props: ProgressRingProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val pct = props.value.coerceIn(0f, 1f)
        val px = when (props.size.lowercase()) {
            "sm" -> 80.dp
            "lg" -> 160.dp
            else -> 120.dp
        }
        val centerLabel = props.label.ifBlank { "${(pct * 100).toInt()}%" }

        Box(
            modifier = Modifier.size(px),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(px)) {
                val stroke = size.minDimension * 0.12f
                val arcRect = Rect(
                    Offset(stroke / 2f, stroke / 2f),
                    Size(size.width - stroke, size.height - stroke),
                )
                drawArc(
                    color = cs.surfaceMuted,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = cs.accent,
                    startAngle = -90f,
                    sweepAngle = 360f * pct,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerLabel,
                    style = tp.serifBodyLarge.copy(
                        fontSize = (px.value * 0.22f).sp,
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
// Helpers
// =============================================================================

@Composable
private fun ChartEmpty(message: String) {
    val cs = UndercurrentTheme.colors
    val tp = UndercurrentTheme.typography
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(UndercurrentTheme.shapes.medium)
            .background(cs.surfaceMuted),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, style = tp.sansSmall, color = cs.inkMuted)
    }
}

private fun formatNum(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString() else "%.1f".format(v)

/**
 * Generate [count] colors derived from [base] by rotating hue in HSL space.
 * Each step is ~37° (≈ golden-angle/10) so adjacent segments stay visually
 * distinct without leaving the theme.
 */
private fun rotatingPalette(base: Color, count: Int): List<Color> {
    if (count <= 0) return emptyList()
    val (h, s, l) = rgbToHsl(base.red, base.green, base.blue)
    return List(count) { i ->
        val hueShift = (h + i * 37f) % 360f
        // Lightness ripples slightly so equal-saturation hues read differently.
        val lShift = (l + (i % 3 - 1) * 0.06f).coerceIn(0.25f, 0.75f)
        val (r, g, b) = hslToRgb(hueShift, s, lShift)
        Color(r, g, b, base.alpha)
    }
}

private fun rgbToHsl(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val l = (maxC + minC) / 2f
    if (maxC == minC) return Triple(0f, 0f, l)
    val d = maxC - minC
    val s = if (l > 0.5f) d / (2f - maxC - minC) else d / (maxC + minC)
    val h = when (maxC) {
        r -> (g - b) / d + (if (g < b) 6f else 0f)
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    } * 60f
    return Triple(h, s, l)
}

private fun hslToRgb(h: Float, s: Float, l: Float): Triple<Float, Float, Float> {
    if (s == 0f) return Triple(l, l, l)
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    val hk = h / 360f
    fun hue(t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }
    return Triple(hue(hk + 1f / 3f), hue(hk), hue(hk - 1f / 3f))
}

/** Every chart component. */
internal val undercurrentChartComponents: List<WeftComponent<*>> = listOf(
    LineChartComponent(),
    BarChartComponent(),
    SparklineComponent(),
    DonutComponent(),
    ProgressRingComponent(),
)
