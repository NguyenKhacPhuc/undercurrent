package dev.weft.undercurrent.core.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// Post — social post card (author + body + actions)
// =============================================================================

@Serializable
internal data class PostProps(
    val authorName: String,
    val body: String,
    val authorHandle: String = "",
    val authorAvatar: String = "",
    val timestamp: String = "",
    /** Optional inline image attached to the post. */
    val imageUrl: String = "",
    val likes: Int = 0,
    val comments: Int = 0,
    /** Whether the current user has liked. */
    val liked: Boolean = false,
    val onLike: String = "like_post",
    val onComment: String = "comment_post",
    val onShare: String = "share_post",
)

internal class PostComponent(private val imageLoader: ImageLoader) : WeftComponent<PostProps>(
    name = "Post",
    description = "Social post card — avatar + name + handle + timestamp + body + optional image + like/comment/share actions with counts. liked: marks the heart filled. Actions fire onLike / onComment / onShare keys.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = PostProps.serializer(),
    example = """{"type": "Post", "props": {"authorName": "Maria Chen", "authorHandle": "@maria", "timestamp": "2h", "body": "Finally finished the redesign. Three months of tiny tweaks. 🎉", "likes": 24, "comments": 5, "liked": true}}""",
) {
    @Composable
    override fun Render(props: PostProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.background)
                .border(1.dp, cs.divider, UndercurrentTheme.shapes.medium)
                .padding(14.dp),
        ) {
            // Header.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (props.authorAvatar.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = props.authorAvatar,
                        imageLoader = imageLoader,
                        contentDescription = props.authorName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(cs.surfaceMuted),
                        error = { InitialsBubble(props.authorName, cs = cs, tp = tp) },
                    )
                } else {
                    InitialsBubble(props.authorName, cs = cs, tp = tp)
                }
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        text = props.authorName,
                        style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = cs.ink,
                    )
                    Row {
                        if (props.authorHandle.isNotBlank()) {
                            Text(
                                text = props.authorHandle,
                                style = tp.sansSmall,
                                color = cs.inkMuted,
                            )
                        }
                        if (props.timestamp.isNotBlank()) {
                            Text(
                                text = if (props.authorHandle.isNotBlank()) " · ${props.timestamp}" else props.timestamp,
                                style = tp.sansSmall,
                                color = cs.inkSubtle,
                            )
                        }
                    }
                }
            }
            // Body.
            Text(
                text = props.body,
                style = tp.serifBody.copy(fontSize = 15.sp, lineHeight = 22.sp),
                color = cs.ink,
                modifier = Modifier.padding(top = 10.dp),
            )
            if (props.imageUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = props.imageUrl,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .aspectRatio(16f / 10f)
                        .clip(UndercurrentTheme.shapes.small)
                        .background(cs.surfaceMuted),
                    error = {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).background(cs.surfaceMuted))
                    },
                )
            }
            // Action bar.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                PostAction(
                    icon = if (props.liked) "favorite" else "favorite_outline",
                    count = props.likes,
                    active = props.liked,
                    cs = cs,
                    tp = tp,
                    onClick = {
                        onEvent(ComponentEvent.Action(props.onLike, "Post", "like"))
                    },
                )
                PostAction(
                    icon = "share",
                    count = props.comments,
                    active = false,
                    cs = cs,
                    tp = tp,
                    onClick = {
                        onEvent(ComponentEvent.Action(props.onComment, "Post", "comment"))
                    },
                )
                PostAction(
                    icon = "open",
                    count = 0,
                    active = false,
                    cs = cs,
                    tp = tp,
                    onClick = {
                        onEvent(ComponentEvent.Action(props.onShare, "Post", "share"))
                    },
                )
            }
        }
    }

    @Composable
    private fun PostAction(
        icon: String,
        count: Int,
        active: Boolean,
        cs: dev.weft.undercurrent.core.designsystem.UndercurrentColors,
        tp: dev.weft.undercurrent.core.designsystem.UndercurrentTypography,
        onClick: () -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onClick),
        ) {
            Icon(
                imageVector = undercurrentIcon(icon),
                contentDescription = icon,
                tint = if (active) cs.error else cs.inkMuted,
                modifier = Modifier.size(18.dp),
            )
            if (count > 0) {
                Text(
                    text = " $count",
                    style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (active) cs.error else cs.inkMuted,
                )
            }
        }
    }

    @Composable
    private fun InitialsBubble(
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
                .size(36.dp)
                .clip(CircleShape)
                .background(cs.accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = cs.accent,
            )
        }
    }
}

// =============================================================================
// Comment — threaded comment row
// =============================================================================

@Serializable
internal data class CommentProps(
    val authorName: String,
    val body: String,
    val timestamp: String = "",
    /** Indent level for threading (0 = top-level). */
    val depth: Int = 0,
    val likes: Int = 0,
    val liked: Boolean = false,
    val onReply: String = "reply_comment",
    val onLike: String = "like_comment",
)

internal class CommentComponent : WeftComponent<CommentProps>(
    name = "Comment",
    description = "Threaded comment row — initials + name + timestamp + body + reply / like actions. depth: 0 = top-level; higher values indent + add a left line. Use multiple in a Stack to render a discussion thread.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = CommentProps.serializer(),
) {
    @Composable
    override fun Render(props: CommentProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val indent = (props.depth.coerceAtLeast(0) * 20).dp
        Row(modifier = Modifier.fillMaxWidth().padding(start = indent, top = 6.dp, bottom = 6.dp)) {
            if (props.depth > 0) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(50.dp)
                        .background(cs.divider),
                )
            }
            // Avatar.
            val initials = props.authorName
                .split(' ', '-', '_')
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifBlank { "?" }
            Box(
                modifier = Modifier
                    .padding(start = if (props.depth > 0) 8.dp else 0.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(cs.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                    color = cs.accent,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = props.authorName,
                        style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = cs.ink,
                    )
                    if (props.timestamp.isNotBlank()) {
                        Text(
                            text = " · ${props.timestamp}",
                            style = tp.sansSmall,
                            color = cs.inkSubtle,
                        )
                    }
                }
                Text(
                    text = props.body,
                    style = tp.serifBody.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = cs.ink,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = if (props.liked) "♥ ${props.likes}" else "♡ ${props.likes}",
                        style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (props.liked) cs.error else cs.inkMuted,
                        modifier = Modifier.clickable {
                            onEvent(ComponentEvent.Action(props.onLike, "Comment", "like"))
                        },
                    )
                    Text(
                        text = "Reply",
                        style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = cs.inkMuted,
                        modifier = Modifier.clickable {
                            onEvent(ComponentEvent.Action(props.onReply, "Comment", "reply"))
                        },
                    )
                }
            }
        }
    }
}

// =============================================================================
// NotificationItem — icon + title + body + time + unread dot
// =============================================================================

@Serializable
internal data class NotificationItemProps(
    val title: String,
    val body: String = "",
    val time: String = "",
    val icon: String = "info",
    /** info | success | warn | error | custom. */
    val tone: String = "info",
    val unread: Boolean = false,
    val onTap: String = "",
)

internal class NotificationItemComponent : WeftComponent<NotificationItemProps>(
    name = "NotificationItem",
    description = "Notification row — colored icon block + title + body + time. unread: shows accent dot. tone: 'info' (default), 'success', 'warn', 'error', 'custom' — colors the icon block. onTap: optional action.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = NotificationItemProps.serializer(),
) {
    @Composable
    override fun Render(props: NotificationItemProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val accent = when (props.tone.lowercase()) {
            "success" -> cs.accent
            "warn", "warning" -> androidx.compose.ui.graphics.Color(0xFFD97706)
            "error" -> cs.error
            else -> cs.accent
        }
        val tappable = props.onTap.isNotBlank()
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.small)
                .background(if (props.unread) accent.copy(alpha = 0.05f) else cs.background)
                .let { m ->
                    if (tappable) m.clickable {
                        onEvent(
                            ComponentEvent.Action(
                                action = props.onTap,
                                sourceType = "NotificationItem",
                                sourceLabel = props.title,
                            ),
                        )
                    } else m
                }
                .padding(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = undercurrentIcon(props.icon),
                    contentDescription = props.tone,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = props.title,
                        style = tp.sansLabel.copy(
                            fontWeight = if (props.unread) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 14.sp,
                        ),
                        color = cs.ink,
                        modifier = Modifier.weight(1f),
                    )
                    if (props.time.isNotBlank()) {
                        Text(
                            text = props.time,
                            style = tp.sansSmall,
                            color = cs.inkMuted,
                        )
                    }
                }
                if (props.body.isNotBlank()) {
                    Text(
                        text = props.body,
                        style = tp.serifBody.copy(fontSize = 13.sp, lineHeight = 19.sp),
                        color = cs.inkMuted,
                    )
                }
            }
            if (props.unread) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, start = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
        }
    }
}

// =============================================================================
// ActivityFeedItem — generic activity entry (user + verb + object + time)
// =============================================================================

@Serializable
internal data class ActivityFeedItemProps(
    /** Actor name. */
    val actor: String,
    /** Verb phrase, e.g. "commented on", "starred", "joined". */
    val verb: String,
    /** Object phrase shown after the verb (the thing acted upon). */
    val obj: String = "",
    val timestamp: String = "",
    /** Optional small icon for the actor avatar. */
    val avatar: String = "",
    /** Optional preview line under the activity. */
    val preview: String = "",
)

internal class ActivityFeedItemComponent : WeftComponent<ActivityFeedItemProps>(
    name = "ActivityFeedItem",
    description = "One row in an activity feed — actor (bold) + verb + object + timestamp. Optional preview line in italic below. Compose multiple in a Stack for a chronological feed.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = ActivityFeedItemProps.serializer(),
    example = """{"type": "ActivityFeedItem", "props": {"actor": "Maria", "verb": "commented on", "obj": "Sprint 14 retro", "timestamp": "2h", "preview": "Loved the new charts in the dashboard."}}""",
) {
    @Composable
    override fun Render(props: ActivityFeedItemProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            // Avatar.
            val initials = props.actor
                .split(' ', '-', '_')
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifBlank { "?" }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(cs.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                    color = cs.accent,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = props.actor,
                        style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = cs.ink,
                    )
                    Text(
                        text = " ${props.verb}",
                        style = tp.serifBody.copy(fontSize = 13.sp),
                        color = cs.inkMuted,
                    )
                    if (props.obj.isNotBlank()) {
                        Text(
                            text = " ${props.obj}",
                            style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = cs.accent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (props.timestamp.isNotBlank()) {
                        Text(
                            text = props.timestamp,
                            style = tp.sansSmall,
                            color = cs.inkSubtle,
                        )
                    }
                }
                if (props.preview.isNotBlank()) {
                    Text(
                        text = "“${props.preview}”",
                        style = tp.serifBody.copy(
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 18.sp,
                        ),
                        color = cs.inkMuted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

// =============================================================================
// BookmarkRow — image thumbnail + title + meta + bookmark toggle
// =============================================================================

@Serializable
internal data class BookmarkRowProps(
    val title: String,
    val source: String = "",
    val excerpt: String = "",
    val imageUrl: String = "",
    val bookmarked: Boolean = true,
    val onTap: String = "",
    val onToggle: String = "toggle_bookmark",
)

internal class BookmarkRowComponent(private val imageLoader: ImageLoader) : WeftComponent<BookmarkRowProps>(
    name = "BookmarkRow",
    description = "Reading-list-style row — small image + title + source/meta + short excerpt + bookmark toggle. Use for saved articles, reading queues, Pocket-style lists.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = BookmarkRowProps.serializer(),
) {
    @Composable
    override fun Render(props: BookmarkRowProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val tappable = props.onTap.isNotBlank()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.small)
                .let { m ->
                    if (tappable) m.clickable {
                        onEvent(
                            ComponentEvent.Action(
                                action = props.onTap,
                                sourceType = "BookmarkRow",
                                sourceLabel = props.title,
                            ),
                        )
                    } else m
                }
                .padding(vertical = 10.dp, horizontal = 8.dp),
        ) {
            if (props.imageUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = props.imageUrl,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(UndercurrentTheme.shapes.small)
                        .background(cs.surfaceMuted),
                    error = {
                        Box(modifier = Modifier.size(72.dp).background(cs.surfaceMuted))
                    },
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                if (props.source.isNotBlank()) {
                    Text(
                        text = props.source.uppercase(),
                        style = tp.sansSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                        ),
                        color = cs.accent,
                    )
                }
                Text(
                    text = props.title,
                    style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = cs.ink,
                    maxLines = 2,
                )
                if (props.excerpt.isNotBlank()) {
                    Text(
                        text = props.excerpt,
                        style = tp.serifBody.copy(fontSize = 12.sp, lineHeight = 17.sp),
                        color = cs.inkMuted,
                        maxLines = 2,
                    )
                }
            }
            Icon(
                imageVector = undercurrentIcon(if (props.bookmarked) "bookmark" else "bookmark_outline"),
                contentDescription = "Toggle bookmark",
                tint = if (props.bookmarked) cs.accent else cs.inkMuted,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        onEvent(
                            ComponentEvent.Action(
                                action = props.onToggle,
                                sourceType = "BookmarkRow",
                                sourceLabel = props.title,
                            ),
                        )
                    },
            )
        }
    }
}

/** Every social-tier component. */
internal fun socialComponents(imageLoader: ImageLoader): List<WeftComponent<*>> = listOf(
    PostComponent(imageLoader),
    CommentComponent(),
    NotificationItemComponent(),
    ActivityFeedItemComponent(),
    BookmarkRowComponent(imageLoader),
)
