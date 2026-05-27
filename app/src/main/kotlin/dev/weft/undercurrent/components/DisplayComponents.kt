package dev.weft.undercurrent.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.theme.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// Text — variant-aware text using Undercurrent typography
// =============================================================================

@Serializable
internal data class TextProps(
    val text: String,
    /** body | body-sans | serif | sans | label | title | headline | display | small | code. */
    val variant: String = "body",
    /** start | center | end. */
    val align: String = "start",
    /** primary | muted | subtle | accent | error. */
    val tone: String = "primary",
)

internal class TextComponent : WeftComponent<TextProps>(
    name = "Text",
    description = "A block of text. variant: body (serif, default), body-sans (sans), label, title, headline, display, small, code. tone: primary (default), muted, subtle, accent, error. align: start (default), center, end. For pull quotes use Quote; for headings with kicker use Heading.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = TextProps.serializer(),
) {
    @Composable
    override fun Render(props: TextProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val tp = UndercurrentTheme.typography
        val cs = UndercurrentTheme.colors
        val style = when (props.variant.lowercase()) {
            "body-sans", "sans" -> tp.sansLabel.copy(fontSize = 15.sp, lineHeight = 22.sp)
            "label" -> tp.sansLabel
            "title" -> tp.sansHeader.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            "headline" -> tp.sansHeader.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            "display" -> tp.serifBodyLarge.copy(fontSize = 32.sp, lineHeight = 38.sp)
            "small" -> tp.sansSmall
            "code" -> tp.serifBody.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            else -> tp.serifBody
        }
        val color = when (props.tone.lowercase()) {
            "muted" -> cs.inkMuted
            "subtle" -> cs.inkSubtle
            "accent" -> cs.accent
            "error" -> cs.error
            else -> cs.ink
        }
        val align = when (props.align.lowercase()) {
            "center" -> TextAlign.Center
            "end" -> TextAlign.End
            else -> TextAlign.Start
        }
        Text(text = props.text, style = style, color = color, textAlign = align, modifier = Modifier.fillMaxWidth())
    }
}

// =============================================================================
// Heading — display headline with optional uppercase kicker above it
// =============================================================================

@Serializable
internal data class HeadingProps(
    val text: String,
    /** Small uppercase lead-in above the heading. Blank = no kicker. */
    val kicker: String = "",
    /** 1 | 2 | 3 — display size. 1 is largest. */
    val level: Int = 2,
)

internal class HeadingComponent : WeftComponent<HeadingProps>(
    name = "Heading",
    description = "A heading with optional small uppercase kicker above. Use for section titles, hero blocks. text: required. kicker: optional lead-in (e.g. 'this week', 'overview'). level: 1 (largest, 36sp) / 2 (default, 28sp) / 3 (22sp).",
    category = ComponentCategory.DISPLAY,
    propsSerializer = HeadingProps.serializer(),
) {
    @Composable
    override fun Render(props: HeadingProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val tp = UndercurrentTheme.typography
        val cs = UndercurrentTheme.colors
        val size = when (props.level) {
            1 -> 36.sp
            3 -> 22.sp
            else -> 28.sp
        }
        Column(modifier = Modifier.fillMaxWidth()) {
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
                text = props.text,
                style = tp.serifBodyLarge.copy(fontSize = size, lineHeight = (size.value * 1.15f).sp),
                color = cs.ink,
            )
        }
    }
}

// =============================================================================
// Quote — pull-quote with leading mark + attribution
// =============================================================================

@Serializable
internal data class QuoteProps(
    val text: String,
    /** Author / source. Blank = no attribution. */
    val attribution: String = "",
)

internal class QuoteComponent : WeftComponent<QuoteProps>(
    name = "Quote",
    description = "A pull-quote in italic serif with a leading quotation mark and optional attribution. text: required. attribution: optional (e.g. 'Anna Karenina', '— Anonymous').",
    category = ComponentCategory.DISPLAY,
    propsSerializer = QuoteProps.serializer(),
) {
    @Composable
    override fun Render(props: QuoteProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val tp = UndercurrentTheme.typography
        val cs = UndercurrentTheme.colors
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.FormatQuote,
                contentDescription = null,
                tint = cs.accent.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = props.text,
                    style = tp.serifBody.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 17.sp,
                        lineHeight = 26.sp,
                    ),
                    color = cs.ink,
                )
                if (props.attribution.isNotBlank()) {
                    Text(
                        text = "— ${props.attribution}",
                        style = tp.sansSmall,
                        color = cs.inkMuted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

// =============================================================================
// Stat — large number + label + optional delta (trend indicator)
// =============================================================================

@Serializable
internal data class StatProps(
    val value: String,
    val label: String,
    /** Delta value shown next to the number (e.g. "+12", "-3%"). Blank = no delta. */
    val delta: String = "",
    /** up | down | flat — colors the delta. */
    val trend: String = "flat",
)

internal class StatComponent : WeftComponent<StatProps>(
    name = "Stat",
    description = "A statistic: big number + small label, with optional trend delta. value: the number/text (e.g. '42', '$1,290'). label: short caption. delta: optional change indicator ('+12', '-3%'). trend: up (green), down (red), flat (muted) — colors the delta. Use Grid to lay out multiple stats side-by-side.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = StatProps.serializer(),
) {
    @Composable
    override fun Render(props: StatProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val tp = UndercurrentTheme.typography
        val cs = UndercurrentTheme.colors
        val trendColor = when (props.trend.lowercase()) {
            "up" -> cs.accent
            "down" -> cs.error
            else -> cs.inkMuted
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = props.value,
                    style = tp.serifBodyLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.ink,
                )
                if (props.delta.isNotBlank()) {
                    Text(
                        text = props.delta,
                        style = tp.sansLabel.copy(fontWeight = FontWeight.Medium),
                        color = trendColor,
                        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                    )
                }
            }
            Text(
                text = props.label,
                style = tp.sansSmall.copy(letterSpacing = 0.5.sp),
                color = cs.inkMuted,
            )
        }
    }
}

// =============================================================================
// Tag — small chip-style label
// =============================================================================

@Serializable
internal data class TagProps(
    val text: String,
    /** neutral | accent | success | warning | error. */
    val tone: String = "neutral",
)

internal class TagComponent : WeftComponent<TagProps>(
    name = "Tag",
    description = "A small label chip for categories, statuses, or counts. text: required. tone: neutral (default), accent, success, warning, error.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = TagProps.serializer(),
) {
    @Composable
    override fun Render(props: TagProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val (bg, fg) = when (props.tone.lowercase()) {
            "accent" -> cs.accent.copy(alpha = 0.14f) to cs.accent
            "success" -> cs.accent.copy(alpha = 0.18f) to cs.accent
            "warning" -> cs.error.copy(alpha = 0.12f) to cs.error
            "error" -> cs.error.copy(alpha = 0.18f) to cs.error
            else -> cs.surfaceMuted to cs.inkMuted
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(bg)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = props.text,
                style = tp.sansSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
                color = fg,
            )
        }
    }
}

// =============================================================================
// Avatar — circular bubble with initials or remote image
// =============================================================================

@Serializable
internal data class AvatarProps(
    /** Display name — first letter (or first letter of each word, up to 2) is shown. */
    val name: String = "",
    /** Optional remote image url. Takes precedence over initials. */
    val imageUrl: String = "",
    /** xs (24) | sm (32) | md (40) | lg (56) | xl (72). */
    val size: String = "md",
)

internal class AvatarComponent(private val imageLoader: ImageLoader) : WeftComponent<AvatarProps>(
    name = "Avatar",
    description = "Circular avatar — shows initials from name, or a remote image if imageUrl is set. name: display name (first letter or 2-letter initials used). imageUrl: optional remote image. size: xs/sm/md (default)/lg/xl.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = AvatarProps.serializer(),
) {
    @Composable
    override fun Render(props: AvatarProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val px = when (props.size.lowercase()) {
            "xs" -> 24.dp
            "sm" -> 32.dp
            "lg" -> 56.dp
            "xl" -> 72.dp
            else -> 40.dp
        }
        if (props.imageUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = props.imageUrl,
                imageLoader = imageLoader,
                contentDescription = props.name.ifBlank { "avatar" },
                modifier = Modifier.size(px).clip(CircleShape).background(cs.surfaceMuted),
            )
        } else {
            val initials = props.name
                .split(' ', '-', '_')
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifBlank { "?" }
            Box(
                modifier = Modifier
                    .size(px)
                    .clip(CircleShape)
                    .background(cs.accent.copy(alpha = 0.15f))
                    .border(1.dp, cs.accent.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = tp.sansLabel.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (px.value * 0.4f).sp,
                    ),
                    color = cs.accent,
                )
            }
        }
    }
}

// =============================================================================
// Note — soft callout box (info / success / warning / error)
// =============================================================================

@Serializable
internal data class NoteProps(
    val text: String,
    val title: String = "",
    /** info | success | warning | error. */
    val tone: String = "info",
    /** Icon name from Tokens.kt. Default picks per-tone; pass "none" to omit. */
    val icon: String = "",
)

internal class NoteComponent : WeftComponent<NoteProps>(
    name = "Note",
    description = "A soft callout box for asides, tips, warnings. text: required. title: optional bold lead. tone: info (default), success, warning, error. icon: optional override; defaults to lightbulb/check/warning/close per tone. Quieter than Alert — use for inline context.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = NoteProps.serializer(),
) {
    @Composable
    override fun Render(props: NoteProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val (accent, defaultIcon) = when (props.tone.lowercase()) {
            "success" -> cs.accent to "check"
            "warning" -> cs.error to "warning"
            "error" -> cs.error to "close"
            else -> cs.accent to "lightbulb"
        }
        val iconName = props.icon.ifBlank { defaultIcon }
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.surfaceMuted)
                .border(1.dp, accent.copy(alpha = 0.18f), UndercurrentTheme.shapes.medium)
                .padding(14.dp),
        ) {
            if (iconName != "none") {
                Icon(
                    imageVector = undercurrentIcon(iconName),
                    contentDescription = props.tone,
                    tint = accent,
                    modifier = Modifier.size(20.dp).padding(top = 1.dp),
                )
            }
            Column(modifier = Modifier.padding(start = if (iconName != "none") 10.dp else 0.dp)) {
                if (props.title.isNotBlank()) {
                    Text(
                        text = props.title,
                        style = tp.sansHeader.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = cs.ink,
                    )
                }
                Text(
                    text = props.text,
                    style = tp.serifBody.copy(fontSize = 14.sp, lineHeight = 21.sp),
                    color = cs.ink,
                )
            }
        }
    }
}

// =============================================================================
// Hairline — themed divider (optional accent tint)
// =============================================================================

@Serializable
internal data class HairlineProps(
    /** none | xs | sm | md | lg | xl — vertical padding above/below. */
    val padding: String = "none",
    /** divider (default) | accent — accent shows a thin colored bar instead. */
    val tone: String = "divider",
)

internal class HairlineComponent : WeftComponent<HairlineProps>(
    name = "Hairline",
    description = "A thin horizontal divider. padding: vertical space above/below (none default, xs/sm/md/lg/xl). tone: 'divider' (default, neutral) or 'accent' (themed accent bar).",
    category = ComponentCategory.DISPLAY,
    propsSerializer = HairlineProps.serializer(),
) {
    @Composable
    override fun Render(props: HairlineProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val color = if (props.tone.lowercase() == "accent") cs.accent.copy(alpha = 0.6f) else cs.divider
        HorizontalDivider(
            color = color,
            thickness = if (props.tone.lowercase() == "accent") 2.dp else 1.dp,
            modifier = Modifier.padding(vertical = undercurrentSpacing(props.padding)),
        )
    }
}

/** Every Display-tier component, in one list for [undercurrentComponents]. */
internal fun displayComponents(imageLoader: ImageLoader): List<WeftComponent<*>> = listOf(
    TextComponent(),
    HeadingComponent(),
    QuoteComponent(),
    StatComponent(),
    TagComponent(),
    AvatarComponent(imageLoader),
    NoteComponent(),
    HairlineComponent(),
)
