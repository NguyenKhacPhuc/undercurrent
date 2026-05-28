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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// Trend — small arrow + delta + label
// =============================================================================

@Serializable
internal data class TrendProps(
    /** Delta value (e.g. "+12%", "-3", "0.4"). The leading sign drives auto-direction. */
    val value: String,
    /** Optional label after the delta (e.g. "vs last week"). */
    val label: String = "",
    /** Override the auto-detected direction: 'up' | 'down' | 'flat'. */
    val direction: String = "",
)

internal class TrendComponent : WeftComponent<TrendProps>(
    name = "Trend",
    description = "Small directional indicator: trend arrow + delta value + optional muted label. value: required text (e.g. '+12%', '-3°', '0.4'). label: optional context like 'vs last week'. direction: 'up' (green), 'down' (red), 'flat' (muted) — auto-detected from the value's sign when blank. Use inline next to a metric.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = TrendProps.serializer(),
) {
    @Composable
    override fun Render(props: TrendProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val resolved = props.direction.ifBlank { autoDirection(props.value) }
        val (icon, color) = when (resolved.lowercase()) {
            "up" -> "trend_up" to cs.accent
            "down" -> "trend_down" to cs.error
            else -> "trend_flat" to cs.inkMuted
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = undercurrentIcon(icon),
                contentDescription = resolved,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = props.value,
                style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = color,
                modifier = Modifier.padding(start = 4.dp),
            )
            if (props.label.isNotBlank()) {
                Text(
                    text = " ${props.label}",
                    style = tp.sansSmall,
                    color = cs.inkMuted,
                )
            }
        }
    }

    private fun autoDirection(v: String): String {
        val trimmed = v.trim()
        return when {
            trimmed.startsWith("+") -> "up"
            trimmed.startsWith("-") || trimmed.startsWith("−") -> "down"
            else -> "flat"
        }
    }
}

// =============================================================================
// DotPaginator — small row of dots for swipeable indicators
// =============================================================================

@Serializable
internal data class DotPaginatorProps(
    val total: Int,
    val current: Int = 0,
)

internal class DotPaginatorComponent : WeftComponent<DotPaginatorProps>(
    name = "DotPaginator",
    description = "Tiny row of dots indicating progress through a sequence. total: number of dots. current: 0-based active index. Non-interactive — pure visual indicator. Use under Carousel-style layouts or to show 'step X of Y'.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = DotPaginatorProps.serializer(),
) {
    @Composable
    override fun Render(props: DotPaginatorProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val total = props.total.coerceAtLeast(1)
        val current = props.current.coerceIn(0, total - 1)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
            repeat(total) { i ->
                val active = i == current
                Box(
                    modifier = Modifier
                        .size(if (active) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (active) cs.accent else cs.inkSubtle.copy(alpha = 0.5f)),
                )
            }
        }
    }
}

// =============================================================================
// WeatherCard — icon + temperature + condition + location
// =============================================================================

@Serializable
internal data class WeatherCardProps(
    /** Big temperature value, e.g. "72°" or "22°C". */
    val temperature: String,
    val condition: String = "",
    val location: String = "",
    /** Generic condition kind: sunny | cloudy | rainy | snowy | stormy | clear-night. */
    val kind: String = "sunny",
    /** Optional small "high / low" line under the temp. */
    val highLow: String = "",
)

internal class WeatherCardComponent : WeftComponent<WeatherCardProps>(
    name = "WeatherCard",
    description = "Weather summary card — large emoji + temperature + condition + location. temperature: e.g. '72°' or '22°C'. condition: 'Sunny', 'Cloudy', etc. location: e.g. 'Brooklyn, NY'. kind: visual hint — 'sunny' (default) / 'cloudy' / 'rainy' / 'snowy' / 'stormy' / 'clear-night' — picks the emoji + accent tint. highLow: optional 'H 78° / L 64°' string.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = WeatherCardProps.serializer(),
    example = """{"type": "WeatherCard", "props": {"temperature": "72°", "condition": "Partly cloudy", "location": "Brooklyn, NY", "kind": "cloudy", "highLow": "H 78° / L 64°"}}""",
) {
    @Composable
    override fun Render(props: WeatherCardProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val (emoji, tintFraction) = weatherVisuals(props.kind)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.accent.copy(alpha = tintFraction))
                .padding(16.dp),
        ) {
            Text(
                text = emoji,
                style = tp.serifBodyLarge.copy(fontSize = 56.sp),
            )
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    text = props.temperature,
                    style = tp.serifBodyLarge.copy(fontSize = 36.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.ink,
                )
                if (props.condition.isNotBlank()) {
                    Text(
                        text = props.condition,
                        style = tp.sansLabel.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
                        color = cs.inkMuted,
                    )
                }
                if (props.highLow.isNotBlank()) {
                    Text(
                        text = props.highLow,
                        style = tp.sansSmall,
                        color = cs.inkMuted,
                    )
                }
            }
            if (props.location.isNotBlank()) {
                Text(
                    text = props.location,
                    style = tp.sansSmall.copy(letterSpacing = 0.5.sp),
                    color = cs.inkMuted,
                )
            }
        }
    }

    private fun weatherVisuals(kind: String): Pair<String, Float> = when (kind.lowercase()) {
        "cloudy" -> "⛅" to 0.08f
        "rainy" -> "🌧" to 0.10f
        "snowy" -> "❄️" to 0.12f
        "stormy" -> "⛈" to 0.12f
        "clear-night", "night" -> "🌙" to 0.14f
        else -> "☀️" to 0.10f
    }
}

// =============================================================================
// TipCard — lightbulb hint with optional dismiss action
// =============================================================================

@Serializable
internal data class TipCardProps(
    val text: String,
    val title: String = "",
    /** Action fired when the close (×) button is tapped. Empty = no dismiss. */
    val onDismiss: String = "",
    /** Override the leading icon (default 'lightbulb'). */
    val icon: String = "lightbulb",
)

internal class TipCardComponent : WeftComponent<TipCardProps>(
    name = "TipCard",
    description = "Soft hint / educational card with leading lightbulb (or custom) icon, title, body, and optional close button. text: required body. title: optional bold lead. icon: defaults to 'lightbulb'. onDismiss: action key for the close button (empty = no dismiss button). Quieter than Banner — use for tips, onboarding hints, 'did you know'.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = TipCardProps.serializer(),
) {
    @Composable
    override fun Render(props: TipCardProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.accent.copy(alpha = 0.08f))
                .border(1.dp, cs.accent.copy(alpha = 0.3f), UndercurrentTheme.shapes.medium)
                .padding(14.dp),
        ) {
            Icon(
                imageVector = undercurrentIcon(props.icon),
                contentDescription = "tip",
                tint = cs.accent,
                modifier = Modifier.size(22.dp).padding(top = 2.dp),
            )
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                if (props.title.isNotBlank()) {
                    Text(
                        text = props.title,
                        style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = cs.ink,
                    )
                }
                Text(
                    text = props.text,
                    style = tp.serifBody.copy(fontSize = 14.sp, lineHeight = 21.sp),
                    color = cs.ink,
                )
            }
            if (props.onDismiss.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable {
                            onEvent(
                                ComponentEvent.Action(
                                    action = props.onDismiss,
                                    sourceType = "TipCard",
                                    sourceLabel = "dismiss",
                                ),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = undercurrentIcon("close"),
                        contentDescription = "Dismiss",
                        tint = cs.inkMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/** Every signals component. */
internal val undercurrentSignalsComponents: List<WeftComponent<*>> = listOf(
    TrendComponent(),
    DotPaginatorComponent(),
    WeatherCardComponent(),
    TipCardComponent(),
)
