package dev.weft.undercurrent.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// Hero — full-bleed image with gradient overlay + bottom text + optional CTA
// =============================================================================

@Serializable
internal data class HeroProps(
    val title: String,
    val subtitle: String = "",
    val kicker: String = "",
    val imageUrl: String,
    /** 16/9 (default) | 4/3 | 1/1 — aspect ratio of the hero. */
    val aspect: String = "16/9",
    /** Optional CTA label. */
    val ctaLabel: String = "",
    val ctaKey: String = "",
)

internal class HeroComponent(private val imageLoader: ImageLoader) : WeftComponent<HeroProps>(
    name = "Hero",
    description = "Full-bleed hero card — large image with a dark gradient at the bottom, kicker + title + subtitle overlaid, optional CTA pill. imageUrl: required. aspect: '16/9' (default) / '4/3' / '1/1'. kicker: small uppercase lead. ctaLabel + ctaKey: optional inline button. Use for landing-page-style highlights, story openers.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = HeroProps.serializer(),
    example = """{"type": "Hero", "props": {"kicker": "this week", "title": "A walking tour of Brooklyn", "subtitle": "From DUMBO to Prospect Heights, with stops along the way.", "imageUrl": "https://images.unsplash.com/photo-1518391846015-55a9cc003b25?w=1200", "ctaLabel": "Read", "ctaKey": "read_essay"}}""",
) {
    @Composable
    override fun Render(props: HeroProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val ratio = when (props.aspect) {
            "4/3" -> 4f / 3f
            "1/1" -> 1f
            else -> 16f / 9f
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.surfaceMuted),
        ) {
            SubcomposeAsyncImage(
                model = props.imageUrl,
                imageLoader = imageLoader,
                contentDescription = props.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(ratio),
                error = {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(ratio).background(cs.surfaceMuted))
                },
            )
            // Gradient overlay at the bottom for legibility.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .background(
                        Brush.verticalGradient(
                            0f to androidx.compose.ui.graphics.Color.Transparent,
                            0.55f to androidx.compose.ui.graphics.Color.Transparent,
                            1f to cs.ink.copy(alpha = 0.78f),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (props.kicker.isNotBlank()) {
                    Text(
                        text = props.kicker.uppercase(),
                        style = tp.sansSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp,
                        ),
                        color = cs.accent,
                    )
                }
                Text(
                    text = props.title,
                    style = tp.serifBodyLarge.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 30.sp,
                    ),
                    color = cs.background,
                )
                if (props.subtitle.isNotBlank()) {
                    Text(
                        text = props.subtitle,
                        style = tp.serifBody.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = cs.background.copy(alpha = 0.85f),
                    )
                }
                if (props.ctaLabel.isNotBlank() && props.ctaKey.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(UndercurrentTheme.shapes.small)
                            .background(cs.background)
                            .clickable {
                                onEvent(
                                    ComponentEvent.Action(
                                        action = props.ctaKey,
                                        sourceType = "Hero",
                                        sourceLabel = props.ctaLabel,
                                    ),
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = props.ctaLabel,
                            style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = cs.ink,
                        )
                    }
                }
            }
        }
    }
}

/** Hero component as a single-element list for the aggregator. */
internal fun heroComponents(imageLoader: ImageLoader): List<WeftComponent<*>> = listOf(
    HeroComponent(imageLoader),
)
