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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// PriceCard — tier name + price/period + feature list + CTA
// =============================================================================

@Serializable
internal data class PriceCardProps(
    val tierName: String,
    /** Big price string, e.g. "$12", "Free", "$99". */
    val price: String,
    /** Caption next to price, e.g. "/month", "/year". */
    val period: String = "",
    /** Short tagline above the features. */
    val tagline: String = "",
    /** Bullet feature list. Lines starting with '-' render as strikethrough (not included). */
    val features: List<String>,
    val ctaLabel: String = "Get started",
    val ctaKey: String = "select_tier",
    /** Highlight this as the recommended tier — adds an accent ribbon + border. */
    val featured: Boolean = false,
)

internal class PriceCardComponent : WeftComponent<PriceCardProps>(
    name = "PriceCard",
    description = "Pricing tier card — tier name + big price + period + optional tagline + feature checklist + CTA. featured: true gives accent ribbon + thicker border to mark the recommended option. features: lines starting with '-' render as struck-through (not-included). ctaLabel/ctaKey: required for the bottom button. Use in a Grid columns=2 or 3 to compare tiers.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = PriceCardProps.serializer(),
    example = """{"type": "PriceCard", "props": {"tierName": "Pro", "price": "$12", "period": "/month", "tagline": "For power users", "featured": true, "features": ["Unlimited notes", "Custom personas", "Priority support", "- White-label"], "ctaLabel": "Start Pro", "ctaKey": "subscribe_pro"}}""",
) {
    @Composable
    override fun Render(props: PriceCardProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(if (props.featured) cs.accent.copy(alpha = 0.06f) else cs.background)
                .border(
                    width = if (props.featured) 2.dp else 1.dp,
                    color = if (props.featured) cs.accent else cs.divider,
                    shape = UndercurrentTheme.shapes.medium,
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Tier name + optional ribbon.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = props.tierName,
                    style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 1.5.sp),
                    color = if (props.featured) cs.accent else cs.inkMuted,
                    modifier = Modifier.weight(1f),
                )
                if (props.featured) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(cs.accent)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "RECOMMENDED",
                            style = tp.sansSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                            ),
                            color = cs.onAccent,
                        )
                    }
                }
            }
            // Price.
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = props.price,
                    style = tp.serifBodyLarge.copy(fontSize = 40.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.ink,
                )
                if (props.period.isNotBlank()) {
                    Text(
                        text = props.period,
                        style = tp.sansLabel.copy(fontSize = 14.sp),
                        color = cs.inkMuted,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                }
            }
            if (props.tagline.isNotBlank()) {
                Text(
                    text = props.tagline,
                    style = tp.serifBody.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = cs.inkMuted,
                )
            }
            // Feature list.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                props.features.forEach { rawFeature ->
                    val excluded = rawFeature.startsWith("-")
                    val text = if (excluded) rawFeature.removePrefix("-").trim() else rawFeature
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = undercurrentIcon(if (excluded) "close" else "check"),
                            contentDescription = null,
                            tint = if (excluded) cs.inkSubtle else cs.accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = text,
                            style = tp.serifBody.copy(
                                fontSize = 14.sp,
                                textDecoration = if (excluded) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            ),
                            color = if (excluded) cs.inkSubtle else cs.ink,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
            // CTA.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(UndercurrentTheme.shapes.small)
                    .background(if (props.featured) cs.accent else cs.surfaceMuted)
                    .clickable {
                        onEvent(
                            ComponentEvent.Action(
                                action = props.ctaKey,
                                sourceType = "PriceCard",
                                sourceLabel = props.tierName,
                            ),
                        )
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = props.ctaLabel,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = if (props.featured) cs.onAccent else cs.ink,
                )
            }
        }
    }
}

/** Pricing-tier components. */
internal val undercurrentPricingComponents: List<WeftComponent<*>> = listOf(
    PriceCardComponent(),
)
