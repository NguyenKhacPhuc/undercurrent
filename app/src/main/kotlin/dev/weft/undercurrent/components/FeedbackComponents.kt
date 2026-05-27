package dev.weft.undercurrent.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.theme.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// Banner — full-width notice with optional action
// =============================================================================

@Serializable
internal data class BannerProps(
    val text: String,
    val title: String = "",
    /** info | success | warning | error. */
    val tone: String = "info",
    /** Optional CTA label. Blank = no button. */
    val actionLabel: String = "",
    /** Action key fired when the CTA is tapped. */
    val actionKey: String = "",
)

internal class BannerComponent : WeftComponent<BannerProps>(
    name = "Banner",
    description = "A full-width inline notice — tone'd background + optional CTA on the right. text: required. title: optional bold lead. tone: info (default), success, warning, error. actionLabel + actionKey: optional inline button (both must be set to render).",
    category = ComponentCategory.DISPLAY,
    propsSerializer = BannerProps.serializer(),
) {
    @Composable
    override fun Render(props: BannerProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val (bg, accent, iconName) = when (props.tone.lowercase()) {
            "success" -> Triple(cs.accent.copy(alpha = 0.10f), cs.accent, "check")
            "warning" -> Triple(cs.error.copy(alpha = 0.10f), cs.error, "warning")
            "error" -> Triple(cs.error.copy(alpha = 0.16f), cs.error, "close")
            else -> Triple(cs.accent.copy(alpha = 0.08f), cs.accent, "info")
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(bg)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = undercurrentIcon(iconName),
                contentDescription = props.tone,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                if (props.title.isNotBlank()) {
                    Text(
                        text = props.title,
                        style = tp.sansHeader.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = cs.ink,
                    )
                }
                Text(
                    text = props.text,
                    style = tp.serifBody.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = cs.ink,
                )
            }
            if (props.actionLabel.isNotBlank() && props.actionKey.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                        .clickable {
                            onEvent(
                                ComponentEvent.Action(
                                    action = props.actionKey,
                                    sourceType = "Banner",
                                    sourceLabel = props.actionLabel,
                                ),
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = props.actionLabel,
                        style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = cs.onAccent,
                    )
                }
            }
        }
    }
}

// =============================================================================
// Empty — centered placeholder (icon + title + body + optional CTA)
// =============================================================================

@Serializable
internal data class EmptyProps(
    val title: String,
    val body: String = "",
    val icon: String = "inbox",
    /** Optional CTA label. */
    val actionLabel: String = "",
    val actionKey: String = "",
)

internal class EmptyComponent : WeftComponent<EmptyProps>(
    name = "Empty",
    description = "Empty-state placeholder. Big icon + title + optional body + optional CTA, centered. title: required. icon: 'inbox' default (any Undercurrent icon name). body: small explanation. actionLabel + actionKey: optional CTA (both required to render).",
    category = ComponentCategory.DISPLAY,
    propsSerializer = EmptyProps.serializer(),
) {
    @Composable
    override fun Render(props: EmptyProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(UndercurrentTheme.shapes.medium)
                    .background(cs.surfaceMuted),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = undercurrentIcon(props.icon),
                    contentDescription = null,
                    tint = cs.inkMuted,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = props.title,
                style = tp.sansHeader.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                color = cs.ink,
                textAlign = TextAlign.Center,
            )
            if (props.body.isNotBlank()) {
                Text(
                    text = props.body,
                    style = tp.serifBody.copy(fontSize = 14.sp, lineHeight = 21.sp),
                    color = cs.inkMuted,
                    textAlign = TextAlign.Center,
                )
            }
            if (props.actionLabel.isNotBlank() && props.actionKey.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(UndercurrentTheme.shapes.medium)
                        .background(cs.accent)
                        .clickable {
                            onEvent(
                                ComponentEvent.Action(
                                    action = props.actionKey,
                                    sourceType = "Empty",
                                    sourceLabel = props.actionLabel,
                                ),
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = props.actionLabel,
                        style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = cs.onAccent,
                    )
                }
            }
        }
    }
}

// =============================================================================
// Skeleton — animated loading placeholder bars
// =============================================================================

@Serializable
internal data class SkeletonProps(
    /** Number of bars to render. */
    val lines: Int = 3,
    /** xs (8) | sm (12) | md (16) | lg (20) — bar height. */
    val barHeight: String = "md",
)

internal class SkeletonComponent : WeftComponent<SkeletonProps>(
    name = "Skeleton",
    description = "Animated loading placeholder bars. Use while waiting for async content. lines: number of bars (default 3). barHeight: xs/sm/md (default)/lg.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = SkeletonProps.serializer(),
) {
    @Composable
    override fun Render(props: SkeletonProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val height = when (props.barHeight.lowercase()) {
            "xs" -> 8.dp
            "sm" -> 12.dp
            "lg" -> 20.dp
            else -> 16.dp
        }
        val transition = rememberInfiniteTransition(label = "skeleton")
        val animatedAlpha by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "skeleton-alpha",
        )
        val count = props.lines.coerceIn(1, 12)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(count) { i ->
                // Vary the trailing bar's width so the placeholder doesn't
                // look like a perfect grid — small visual cue that this is
                // pending content, not real.
                val widthFraction = when {
                    i == count - 1 -> 0.6f
                    i == 0 -> 0.85f
                    else -> 1f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(widthFraction)
                        .height(height)
                        .clip(RoundedCornerShape(50))
                        .background(cs.surfaceMuted)
                        .alpha(animatedAlpha),
                )
            }
        }
    }
}

/** Every Feedback-tier component. */
internal val undercurrentFeedbackComponents: List<WeftComponent<*>> = listOf(
    BannerComponent(),
    EmptyComponent(),
    SkeletonComponent(),
)
