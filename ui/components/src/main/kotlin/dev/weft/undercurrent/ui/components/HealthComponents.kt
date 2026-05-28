package dev.weft.undercurrent.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// WorkoutSet — exercise name + sets/reps/weight rows
// =============================================================================

@Serializable
internal data class WorkoutSetRow(
    val reps: String,
    val weight: String = "",
    val done: Boolean = false,
)

@Serializable
internal data class WorkoutSetProps(
    val exercise: String,
    val rows: List<WorkoutSetRow>,
    /** Small caption — muscle group, equipment, etc. */
    val detail: String = "",
)

internal class WorkoutSetComponent : WeftComponent<WorkoutSetProps>(
    name = "WorkoutSet",
    description = "Exercise card — name + small detail caption + numbered set rows (reps + weight + done checkbox). rows: list of {reps, weight, done}. Use for strength logs; Stack multiple WorkoutSets for a full session.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = WorkoutSetProps.serializer(),
    example = """{"type": "WorkoutSet", "props": {"exercise": "Bench press", "detail": "Chest · barbell", "rows": [{"reps": "5", "weight": "60kg", "done": true}, {"reps": "5", "weight": "65kg", "done": true}, {"reps": "5", "weight": "70kg"}, {"reps": "5", "weight": "70kg"}, {"reps": "AMRAP", "weight": "70kg"}]}}""",
) {
    @Composable
    override fun Render(props: WorkoutSetProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.surfaceMuted)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = props.exercise,
                        style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                        color = cs.ink,
                    )
                    if (props.detail.isNotBlank()) {
                        Text(
                            text = props.detail,
                            style = tp.sansSmall,
                            color = cs.inkMuted,
                        )
                    }
                }
                Text(
                    text = "${props.rows.count { it.done }}/${props.rows.size}",
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold),
                    color = cs.accent,
                )
            }
            HorizontalDivider(color = cs.divider, modifier = Modifier.padding(vertical = 10.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text("#", style = tp.sansSmall.copy(letterSpacing = 0.5.sp), color = cs.inkSubtle, modifier = Modifier.width(28.dp))
                Text("REPS", style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp), color = cs.inkSubtle, modifier = Modifier.weight(1f))
                Text("WEIGHT", style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp), color = cs.inkSubtle, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.size(20.dp))
            }
            props.rows.forEachIndexed { i, row ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = "${i + 1}.",
                        style = tp.sansSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = cs.inkMuted,
                        modifier = Modifier.width(28.dp),
                    )
                    Text(
                        text = row.reps,
                        style = tp.serifBody.copy(fontSize = 15.sp, fontFamily = FontFamily.Monospace),
                        color = if (row.done) cs.inkMuted else cs.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = row.weight,
                        style = tp.serifBody.copy(fontSize = 15.sp, fontFamily = FontFamily.Monospace),
                        color = if (row.done) cs.inkMuted else cs.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (row.done) cs.accent else cs.background)
                            .border(
                                width = if (row.done) 0.dp else 1.5.dp,
                                color = if (row.done) cs.accent else cs.divider,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (row.done) {
                            Text("✓", style = tp.sansSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = cs.onAccent)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// NutritionFacts — US-style nutrition label
// =============================================================================

@Serializable
internal data class NutritionFact(
    val name: String,
    /** "12g", "200mg", etc. */
    val amount: String,
    /** Daily-value percentage. */
    val dv: String = "",
    /** Indent under the parent (sub-nutrient like "Saturated Fat"). */
    val indented: Boolean = false,
    /** Emphasize as a major header (Total Fat, Cholesterol, …). */
    val major: Boolean = true,
)

@Serializable
internal data class NutritionFactsProps(
    val title: String = "Nutrition Facts",
    val servingSize: String = "",
    val servingsPerContainer: String = "",
    val calories: String,
    val facts: List<NutritionFact>,
)

internal class NutritionFactsComponent : WeftComponent<NutritionFactsProps>(
    name = "NutritionFacts",
    description = "US-style FDA nutrition label. title (default 'Nutrition Facts'), serving size, servings/container, calories, then a list of facts with name + amount + optional daily-value %. indented=true for sub-nutrients (Saturated, Trans). major controls weight.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = NutritionFactsProps.serializer(),
    example = """{"type": "NutritionFacts", "props": {"servingSize": "1 cup (228g)", "servingsPerContainer": "2", "calories": "250", "facts": [{"name": "Total Fat", "amount": "12g", "dv": "15%"}, {"name": "Saturated Fat", "amount": "3g", "dv": "15%", "indented": true, "major": false}, {"name": "Cholesterol", "amount": "30mg", "dv": "10%"}, {"name": "Sodium", "amount": "470mg", "dv": "20%"}, {"name": "Protein", "amount": "5g"}]}}""",
) {
    @Composable
    override fun Render(props: NutritionFactsProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.small)
                .background(cs.background)
                .border(2.dp, cs.ink, UndercurrentTheme.shapes.small)
                .padding(14.dp),
        ) {
            Text(
                text = props.title,
                style = tp.sansHeader.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                ),
                color = cs.ink,
            )
            if (props.servingsPerContainer.isNotBlank()) {
                Text(
                    text = "${props.servingsPerContainer} servings per container",
                    style = tp.sansSmall,
                    color = cs.ink,
                )
            }
            if (props.servingSize.isNotBlank()) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                    Text(
                        text = "Serving size",
                        style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = cs.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = props.servingSize,
                        style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = cs.ink,
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 3.dp).background(cs.ink))
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "Calories",
                    style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
                    color = cs.ink,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = props.calories,
                    style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
                    color = cs.ink,
                )
            }
            HorizontalDivider(thickness = 2.dp, color = cs.ink, modifier = Modifier.padding(vertical = 6.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Box(modifier = Modifier.weight(1f))
                Text(
                    text = "% Daily Value*",
                    style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = cs.ink,
                )
            }
            props.facts.forEach { fact ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        if (fact.indented) Box(modifier = Modifier.size(width = 12.dp, height = 1.dp))
                        Text(
                            text = if (fact.major && !fact.indented) "${fact.name} " else fact.name,
                            style = tp.sansLabel.copy(
                                fontSize = 13.sp,
                                fontWeight = if (fact.major) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            color = cs.ink,
                        )
                        Text(
                            text = fact.amount,
                            style = tp.sansLabel.copy(fontSize = 13.sp),
                            color = cs.ink,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    if (fact.dv.isNotBlank()) {
                        Text(
                            text = fact.dv,
                            style = tp.sansLabel.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = cs.ink,
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = cs.divider)
            }
            Text(
                text = "* Daily values based on a 2,000 calorie diet.",
                style = tp.sansSmall.copy(fontSize = 10.sp),
                color = cs.inkMuted,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

// =============================================================================
// WaterTracker — glasses with filled/empty state
// =============================================================================

@Serializable
internal data class WaterTrackerProps(
    val id: String,
    val target: Int = 8,
    val current: Int = 0,
    val unitLabel: String = "glasses",
)

internal class WaterTrackerComponent : WeftComponent<WaterTrackerProps>(
    name = "WaterTracker",
    description = "Hydration tracker — row of N glass icons, filled to current. target: total glasses. current: filled count. id: stable identifier (fires TextChanged with the new current on tap — taps a glass to set it as the new high). unitLabel: 'glasses', 'cups', etc.",
    category = ComponentCategory.INPUT,
    propsSerializer = WaterTrackerProps.serializer(),
) {
    @Composable
    override fun Render(props: WaterTrackerProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val total = props.target.coerceIn(1, 24)
        var current by remember(props.id, props.current) {
            mutableIntStateOf(props.current.coerceIn(0, total))
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$current",
                    style = tp.serifBodyLarge.copy(fontSize = 36.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.accent,
                )
                Text(
                    text = " / $total ${props.unitLabel}",
                    style = tp.sansLabel.copy(fontSize = 14.sp),
                    color = cs.inkMuted,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                repeat(total) { i ->
                    val isFilled = i < current
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(UndercurrentTheme.shapes.small)
                            .background(
                                if (isFilled) cs.accent.copy(alpha = 0.18f)
                                else cs.surfaceMuted,
                            )
                            .border(
                                width = 1.dp,
                                color = if (isFilled) cs.accent else cs.divider,
                                shape = UndercurrentTheme.shapes.small,
                            )
                            .clickable {
                                val newCurrent = if (current == i + 1) i else i + 1
                                current = newCurrent
                                onEvent(
                                    ComponentEvent.TextChanged(
                                        sourceId = props.id,
                                        value = newCurrent.toString(),
                                    ),
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isFilled) "💧" else "·",
                            style = tp.serifBody.copy(
                                fontSize = if (isFilled) 14.sp else 16.sp,
                            ),
                            color = if (isFilled) cs.accent else cs.inkSubtle,
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// SleepRing — circular ring split into stages (deep / light / rem / awake)
// =============================================================================

@Serializable
internal data class SleepStage(
    /** Fraction of total sleep (0..1) — should sum to 1 across stages. */
    val fraction: Float,
    val label: String,
    /** deep | light | rem | awake — picks the color. */
    val kind: String = "light",
)

@Serializable
internal data class SleepRingProps(
    /** Total sleep duration, e.g. "7h 24m". */
    val total: String,
    val stages: List<SleepStage>,
    /** Optional score/quality 0-100. */
    val score: Int = -1,
)

internal class SleepRingComponent : WeftComponent<SleepRingProps>(
    name = "SleepRing",
    description = "Circular sleep summary ring — split into deep / light / rem / awake stages around the circumference, with total time + optional quality score in the center. stages: list of {fraction (0..1), label, kind}. Fractions should sum to 1.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = SleepRingProps.serializer(),
    example = """{"type": "SleepRing", "props": {"total": "7h 24m", "score": 84, "stages": [{"fraction": 0.18, "label": "Deep", "kind": "deep"}, {"fraction": 0.52, "label": "Light", "kind": "light"}, {"fraction": 0.22, "label": "REM", "kind": "rem"}, {"fraction": 0.08, "label": "Awake", "kind": "awake"}]}}""",
) {
    @Composable
    override fun Render(props: SleepRingProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val total = props.stages.sumOf { it.fraction.toDouble() }.toFloat().coerceAtLeast(0.0001f)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier.aspectRatio(1f).fillMaxWidth(0.55f),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    val stroke = size.minDimension * 0.14f
                    val topLeft = Offset(stroke / 2f, stroke / 2f)
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    var start = -90f
                    props.stages.forEach { stage ->
                        val sweep = (stage.fraction / total) * 360f
                        drawArc(
                            color = stageColor(stage.kind, cs),
                            startAngle = start,
                            sweepAngle = sweep - 2f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                        start += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = props.total,
                        style = tp.serifBodyLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = cs.ink,
                    )
                    if (props.score >= 0) {
                        Text(
                            text = "Score · ${props.score}",
                            style = tp.sansSmall.copy(
                                letterSpacing = 0.5.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = cs.inkMuted,
                        )
                    }
                }
            }
            // Legend below.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                props.stages.forEach { stage ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(width = 16.dp, height = 4.dp)
                                .clip(UndercurrentTheme.shapes.small)
                                .background(stageColor(stage.kind, cs)),
                        )
                        Text(
                            text = stage.label,
                            style = tp.sansSmall.copy(letterSpacing = 0.3.sp),
                            color = cs.inkMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            text = "${(stage.fraction * 100).toInt()}%",
                            style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = cs.ink,
                        )
                    }
                }
            }
        }
    }

    private fun stageColor(kind: String, cs: dev.weft.undercurrent.core.designsystem.UndercurrentColors): Color = when (kind.lowercase()) {
        "deep" -> cs.accent
        "rem" -> cs.accent.copy(alpha = 0.65f)
        "awake" -> cs.error.copy(alpha = 0.8f)
        else -> cs.accent.copy(alpha = 0.35f)
    }
}

// =============================================================================
// HeartRateChart — pulse line over a colored zone backdrop
// =============================================================================

@Serializable
internal data class HeartRateChartProps(
    /** BPM readings, oldest-first. */
    val readings: List<Float>,
    val currentBpm: Int = 0,
    /** Zone thresholds: resting | fatBurn | cardio | peak — drives backdrop bands. */
    val restingMax: Int = 70,
    val fatBurnMax: Int = 110,
    val cardioMax: Int = 140,
    val peakMax: Int = 180,
)

internal class HeartRateChartComponent : WeftComponent<HeartRateChartProps>(
    name = "HeartRateChart",
    description = "Heart-rate pulse line with colored zone bands behind it (resting/fatBurn/cardio/peak). readings: oldest-first BPM list. currentBpm: shown as the big number at the top. zone caps default to standard fitness zones — override as needed.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = HeartRateChartProps.serializer(),
) {
    @Composable
    override fun Render(props: HeartRateChartProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        if (props.readings.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(UndercurrentTheme.shapes.medium)
                    .background(cs.surfaceMuted),
                contentAlignment = Alignment.Center,
            ) {
                Text("Need ≥2 readings.", style = tp.sansSmall, color = cs.inkMuted)
            }
            return
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (props.currentBpm > 0) "${props.currentBpm}" else "—",
                    style = tp.serifBodyLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.accent,
                )
                Text(
                    text = " BPM",
                    style = tp.sansLabel.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                    ),
                    color = cs.inkMuted,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                )
            }
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                val w = size.width
                val h = size.height
                val maxV = props.peakMax.toFloat()
                val minV = 50f
                val span = (maxV - minV).coerceAtLeast(1f)
                // Zone bands.
                val zones = listOf(
                    minV to props.restingMax.toFloat() to cs.inkSubtle.copy(alpha = 0.10f),
                    props.restingMax.toFloat() to props.fatBurnMax.toFloat() to cs.accent.copy(alpha = 0.10f),
                    props.fatBurnMax.toFloat() to props.cardioMax.toFloat() to cs.accent.copy(alpha = 0.18f),
                    props.cardioMax.toFloat() to props.peakMax.toFloat() to cs.error.copy(alpha = 0.18f),
                )
                zones.forEach { (range, color) ->
                    val (low, high) = range
                    val yHigh = h - ((high - minV) / span) * h
                    val yLow = h - ((low - minV) / span) * h
                    drawRect(
                        color = color,
                        topLeft = Offset(0f, yHigh),
                        size = Size(w, (yLow - yHigh).coerceAtLeast(0f)),
                    )
                }
                // Line.
                val n = props.readings.size
                val gap = w / (n - 1).toFloat()
                val coords = props.readings.mapIndexed { i, v ->
                    Offset(i * gap, h - ((v - minV) / span) * h)
                }
                val path = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    coords.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, cs.accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(cs.accent, radius = 4.dp.toPx(), center = coords.last())
                drawCircle(cs.background, radius = 2.dp.toPx(), center = coords.last())
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Resting", "Fat burn", "Cardio", "Peak").forEach {
                    Text(
                        text = it,
                        style = tp.sansSmall.copy(letterSpacing = 0.4.sp),
                        color = cs.inkMuted,
                    )
                }
            }
        }
    }
}

/** Every health-tier component. */
internal val undercurrentHealthComponents: List<WeftComponent<*>> = listOf(
    WorkoutSetComponent(),
    NutritionFactsComponent(),
    WaterTrackerComponent(),
    SleepRingComponent(),
    HeartRateChartComponent(),
)
