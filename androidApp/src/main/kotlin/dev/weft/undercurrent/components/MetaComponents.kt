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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
// Status — colored dot + label (online/away/offline/custom)
// =============================================================================

@Serializable
internal data class StatusProps(
    val label: String,
    /** online (green) | away (yellow-ish) | offline (gray) | busy (red) | custom. */
    val tone: String = "online",
    /** Pulsing animation on the dot — set true for 'live' states. */
    val pulse: Boolean = false,
    /** Optional secondary text shown after the label in a muted tone. */
    val detail: String = "",
)

internal class StatusComponent : WeftComponent<StatusProps>(
    name = "Status",
    description = "Colored status dot + label, optionally pulsing. label: required. tone: 'online' (default, accent-green), 'away' (warn yellow), 'offline' (muted), 'busy' (error red). pulse: true for an animated live-dot. detail: small extra text (e.g. timestamp). Use for presence indicators, live-streaming labels, build status.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = StatusProps.serializer(),
) {
    @Composable
    override fun Render(props: StatusProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val dotColor = when (props.tone.lowercase()) {
            "away" -> cs.error.copy(alpha = 0.7f)
            "offline" -> cs.inkSubtle
            "busy" -> cs.error
            "custom" -> cs.accent
            else -> cs.accent // "online"
        }

        val animatedScale = if (props.pulse) {
            val transition = rememberInfiniteTransition(label = "status-pulse")
            val s by transition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "status-pulse-scale",
            )
            s
        } else 1f

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(animatedScale)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = props.label,
                style = tp.sansLabel.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
                color = cs.ink,
                modifier = Modifier.padding(start = 8.dp),
            )
            if (props.detail.isNotBlank()) {
                Text(
                    text = "  ${props.detail}",
                    style = tp.sansSmall,
                    color = cs.inkMuted,
                )
            }
        }
    }
}

// =============================================================================
// ChatBubble — message bubble (user vs assistant variants)
// =============================================================================

@Serializable
internal data class ChatBubbleProps(
    val text: String,
    /** user (right-aligned accent) | assistant (left-aligned surface) | system (centered muted). */
    val from: String = "assistant",
    /** Optional small label above the bubble — name / role / timestamp. */
    val label: String = "",
    /** Author avatar — initials extracted from this name; ignored for system bubbles. */
    val authorName: String = "",
)

internal class ChatBubbleComponent : WeftComponent<ChatBubbleProps>(
    name = "ChatBubble",
    description = "Chat-style message bubble. text: required body. from: 'user' (right-aligned accent fill), 'assistant' (default, left-aligned surface), 'system' (centered, muted). label: optional name/timestamp above. authorName: drives initials for the leading avatar bubble (assistant only). Use for transcript-style displays, message previews.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = ChatBubbleProps.serializer(),
) {
    @Composable
    override fun Render(props: ChatBubbleProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val isUser = props.from.lowercase() == "user"
        val isSystem = props.from.lowercase() == "system"

        if (isSystem) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = props.text,
                    style = tp.sansSmall.copy(letterSpacing = 0.3.sp),
                    color = cs.inkMuted,
                )
            }
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            if (!isUser && props.authorName.isNotBlank()) {
                AuthorBubble(name = props.authorName, cs = cs, tp = tp)
            }
            Column(modifier = Modifier.padding(horizontal = if (isUser) 0.dp else 8.dp)) {
                if (props.label.isNotBlank()) {
                    Text(
                        text = props.label,
                        style = tp.sansSmall.copy(letterSpacing = 0.3.sp),
                        color = cs.inkMuted,
                        modifier = Modifier.padding(bottom = 2.dp, start = if (isUser) 0.dp else 4.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(UndercurrentTheme.shapes.medium)
                        .background(if (isUser) cs.accent else cs.surfaceMuted)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = props.text,
                        style = tp.serifBody.copy(fontSize = 14.sp, lineHeight = 21.sp),
                        color = if (isUser) cs.onAccent else cs.ink,
                    )
                }
            }
        }
    }

    @Composable
    private fun AuthorBubble(
        name: String,
        cs: dev.weft.undercurrent.core.designsystem.UndercurrentColors,
        tp: dev.weft.undercurrent.core.designsystem.UndercurrentTypography,
    ) {
        val initials = name
            .split(' ', '-', '_')
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(cs.accent.copy(alpha = 0.15f))
                .border(1.dp, cs.accent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                color = cs.accent,
            )
        }
    }
}

// =============================================================================
// LinkPreview — URL card with thumbnail / title / description / host
// =============================================================================

@Serializable
internal data class LinkPreviewProps(
    val url: String,
    val title: String,
    val description: String = "",
    /** Optional thumbnail image (Open Graph preview). */
    val imageUrl: String = "",
    /** Optional explicit host text (e.g. "nytimes.com"). Derived from [url] when blank. */
    val host: String = "",
    /** Optional small favicon image. */
    val faviconUrl: String = "",
    /** Action key fired on tap. Empty = non-interactive. */
    val onTap: String = "",
)

internal class LinkPreviewComponent(private val imageLoader: ImageLoader) : WeftComponent<LinkPreviewProps>(
    name = "LinkPreview",
    description = "A rich link card — favicon + host + title + optional description + thumbnail. url: required. title: required. description: optional. imageUrl: optional Open Graph image. host: shown next to favicon (derived from url when blank). faviconUrl: optional. onTap: action key fired on tap (e.g. 'open_link') — leave blank for non-interactive.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = LinkPreviewProps.serializer(),
    example = """{"type": "LinkPreview", "props": {"url": "https://nytimes.com/example", "title": "Climate, Coffee, and the Future", "description": "How brewing habits are changing in the age of climate uncertainty.", "imageUrl": "https://nytimes.com/og.jpg", "host": "nytimes.com", "onTap": "open_link"}}""",
) {
    @Composable
    override fun Render(props: LinkPreviewProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val resolvedHost = props.host.ifBlank { hostFromUrl(props.url) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.background)
                .border(1.dp, cs.divider, UndercurrentTheme.shapes.medium)
                .let { m ->
                    if (props.onTap.isNotBlank()) m.clickable {
                        onEvent(
                            ComponentEvent.Action(
                                action = props.onTap,
                                sourceType = "LinkPreview",
                                sourceLabel = props.url,
                            ),
                        )
                    } else m
                },
        ) {
            if (props.imageUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = props.imageUrl,
                    imageLoader = imageLoader,
                    contentDescription = props.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    loading = {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(cs.surfaceMuted))
                    },
                    error = {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(cs.surfaceMuted))
                    },
                )
            }
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (props.faviconUrl.isNotBlank()) {
                        SubcomposeAsyncImage(
                            model = props.faviconUrl,
                            imageLoader = imageLoader,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).clip(CircleShape).background(cs.surfaceMuted),
                            error = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(cs.surfaceMuted),
                                )
                            },
                        )
                        Box(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = resolvedHost,
                        style = tp.sansSmall.copy(letterSpacing = 0.3.sp, fontWeight = FontWeight.Medium),
                        color = cs.inkMuted,
                    )
                }
                Text(
                    text = props.title,
                    style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = cs.ink,
                )
                if (props.description.isNotBlank()) {
                    Text(
                        text = props.description,
                        style = tp.serifBody.copy(fontSize = 13.sp, lineHeight = 19.sp),
                        color = cs.inkMuted,
                    )
                }
            }
        }
    }

    private fun hostFromUrl(url: String): String = runCatching {
        val noScheme = url.substringAfter("://", url)
        noScheme.substringBefore('/').substringBefore('?').lowercase()
    }.getOrDefault("")
}

/** Every meta-tier component. ImageLoader-bound ones constructed in factory. */
internal fun metaComponents(imageLoader: ImageLoader): List<WeftComponent<*>> = listOf(
    StatusComponent(),
    ChatBubbleComponent(),
    LinkPreviewComponent(imageLoader),
)
