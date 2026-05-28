package dev.weft.undercurrent.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
// Receipt — itemized list + subtotal/tax/total
// =============================================================================

@Serializable
internal data class ReceiptLine(
    val label: String,
    val amount: String,
    /** Optional qty/details shown small under the label. */
    val detail: String = "",
)

@Serializable
internal data class ReceiptProps(
    val title: String = "",
    /** Subtitle line — date, merchant, etc. */
    val subtitle: String = "",
    val items: List<ReceiptLine>,
    /** Aggregated lines below the items — typically Subtotal, Tax, Tip. */
    val summary: List<ReceiptLine> = emptyList(),
    /** Final total line, rendered with emphasis. */
    val total: ReceiptLine? = null,
    val currency: String = "",
)

internal class ReceiptComponent : WeftComponent<ReceiptProps>(
    name = "Receipt",
    description = "Itemized receipt — title + subtitle + line items + summary (subtotal/tax) + bold total. items / summary: list of {label, amount, detail}. total: optional {label, amount, detail}. Use for purchase summaries, order confirmations, invoices.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = ReceiptProps.serializer(),
    example = """{"type": "Receipt", "props": {"title": "Blue Bottle Coffee", "subtitle": "Today, 8:14am", "items": [{"label": "Latte", "detail": "12oz × 1", "amount": "$5.25"}, {"label": "Croissant", "amount": "$4.50"}], "summary": [{"label": "Subtotal", "amount": "$9.75"}, {"label": "Tax", "amount": "$0.87"}], "total": {"label": "Total", "amount": "$10.62"}}}""",
) {
    @Composable
    override fun Render(props: ReceiptProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.background)
                .border(1.dp, cs.divider, UndercurrentTheme.shapes.medium)
                .padding(16.dp),
        ) {
            if (props.title.isNotBlank()) {
                Text(
                    text = props.title,
                    style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                    color = cs.ink,
                )
            }
            if (props.subtitle.isNotBlank()) {
                Text(
                    text = props.subtitle,
                    style = tp.sansSmall,
                    color = cs.inkMuted,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (props.title.isNotBlank() || props.subtitle.isNotBlank()) {
                HorizontalDivider(color = cs.divider, modifier = Modifier.padding(vertical = 4.dp))
            }
            // Items.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                props.items.forEach { line ->
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = line.label,
                                style = tp.serifBody.copy(fontSize = 14.sp),
                                color = cs.ink,
                            )
                            if (line.detail.isNotBlank()) {
                                Text(
                                    text = line.detail,
                                    style = tp.sansSmall,
                                    color = cs.inkMuted,
                                )
                            }
                        }
                        Text(
                            text = line.amount,
                            style = tp.serifBody.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            ),
                            color = cs.ink,
                        )
                    }
                }
            }
            if (props.summary.isNotEmpty()) {
                HorizontalDivider(color = cs.divider)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                    props.summary.forEach { line ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = line.label,
                                style = tp.sansLabel.copy(fontSize = 13.sp),
                                color = cs.inkMuted,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = line.amount,
                                style = tp.sansLabel.copy(
                                    fontSize = 13.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                ),
                                color = cs.inkMuted,
                            )
                        }
                    }
                }
            }
            if (props.total != null) {
                HorizontalDivider(color = cs.divider, modifier = Modifier.padding(top = 8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(
                        text = props.total.label,
                        style = tp.sansHeader.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = cs.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = props.total.amount,
                        style = tp.serifBodyLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        ),
                        color = cs.accent,
                    )
                }
            }
        }
    }
}

// =============================================================================
// ContactCard — avatar + name + role + actions
// =============================================================================

@Serializable
internal data class ContactAction(
    val icon: String,
    val label: String = "",
    val onTap: String,
)

@Serializable
internal data class ContactCardProps(
    val name: String,
    val role: String = "",
    /** Avatar image url. Empty = use initials. */
    val avatarUrl: String = "",
    /** Optional small secondary line — handle, email, etc. */
    val handle: String = "",
    /** Action buttons shown on the right (call, message, email, share…). */
    val actions: List<ContactAction> = emptyList(),
)

internal class ContactCardComponent(private val imageLoader: ImageLoader) : WeftComponent<ContactCardProps>(
    name = "ContactCard",
    description = "Person card — circular avatar + name + role + handle + row of action icon buttons. avatarUrl: optional (falls back to initials from name). actions: list of {icon, label, onTap}. Use for assignee cards, attendee lists, profile summaries.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = ContactCardProps.serializer(),
    example = """{"type": "ContactCard", "props": {"name": "Maria Chen", "role": "Design Lead", "handle": "@maria", "actions": [{"icon": "open", "onTap": "open_profile"}, {"icon": "share", "onTap": "share_contact"}]}}""",
) {
    @Composable
    override fun Render(props: ContactCardProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.surfaceMuted)
                .padding(14.dp),
        ) {
            // Avatar.
            if (props.avatarUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = props.avatarUrl,
                    imageLoader = imageLoader,
                    contentDescription = props.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(cs.surfaceMuted),
                    error = { InitialsAvatar(name = props.name, cs = cs, tp = tp) },
                )
            } else {
                InitialsAvatar(name = props.name, cs = cs, tp = tp)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = props.name,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = cs.ink,
                )
                if (props.role.isNotBlank()) {
                    Text(
                        text = props.role,
                        style = tp.sansSmall,
                        color = cs.inkMuted,
                    )
                }
                if (props.handle.isNotBlank()) {
                    Text(
                        text = props.handle,
                        style = tp.sansSmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        ),
                        color = cs.inkSubtle,
                    )
                }
            }
            // Actions.
            if (props.actions.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    props.actions.forEach { action ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(cs.accent.copy(alpha = 0.12f))
                                .clickable {
                                    onEvent(
                                        ComponentEvent.Action(
                                            action = action.onTap,
                                            sourceType = "ContactCard",
                                            sourceLabel = action.label.ifBlank { action.icon },
                                        ),
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = undercurrentIcon(action.icon),
                                contentDescription = action.label.ifBlank { action.icon },
                                tint = cs.accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun InitialsAvatar(
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
                .size(44.dp)
                .clip(CircleShape)
                .background(cs.accent.copy(alpha = 0.15f))
                .border(1.dp, cs.accent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                color = cs.accent,
            )
        }
    }
}

// =============================================================================
// Reaction — multi-emoji reaction bar with counts
// =============================================================================

@Serializable
internal data class ReactionItem(
    val emoji: String,
    val count: Int = 0,
    /** Whether the current user has reacted with this. */
    val reacted: Boolean = false,
)

@Serializable
internal data class ReactionProps(
    val id: String,
    val reactions: List<ReactionItem>,
    /** Show a trailing "+ add" pill at the end. */
    val allowAdd: Boolean = true,
)

internal class ReactionComponent : WeftComponent<ReactionProps>(
    name = "Reaction",
    description = "Emoji reaction bar (Slack/Discord-style). reactions: list of {emoji, count, reacted}. id: stable identifier (fires Action with the tapped emoji as sourceLabel when toggled). allowAdd: shows a '+' chip at the end when true. Use under chat bubbles, posts, or feedback prompts.",
    category = ComponentCategory.INPUT,
    propsSerializer = ReactionProps.serializer(),
    example = """{"type": "Reaction", "props": {"id": "post1", "reactions": [{"emoji": "❤️", "count": 3, "reacted": true}, {"emoji": "👀", "count": 1}, {"emoji": "💡", "count": 2}]}}""",
) {
    @Composable
    override fun Render(props: ReactionProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val state = remember(props.reactions) {
            mutableStateOf(props.reactions.associate { it.emoji to (it.count to it.reacted) })
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            props.reactions.forEach { r ->
                val pair = state.value[r.emoji] ?: (0 to false)
                val (count, reacted) = pair
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (reacted) cs.accent.copy(alpha = 0.15f) else cs.surfaceMuted)
                        .border(
                            width = 1.dp,
                            color = if (reacted) cs.accent else cs.divider,
                            shape = CircleShape,
                        )
                        .clickable {
                            val newCount = if (reacted) (count - 1).coerceAtLeast(0) else count + 1
                            state.value = state.value.toMutableMap().apply {
                                put(r.emoji, newCount to !reacted)
                            }
                            onEvent(
                                ComponentEvent.Action(
                                    action = "react",
                                    sourceType = "Reaction",
                                    sourceLabel = r.emoji,
                                ),
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = r.emoji,
                        style = tp.serifBody.copy(fontSize = 15.sp),
                    )
                    if (count > 0) {
                        Text(
                            text = " $count",
                            style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (reacted) cs.accent else cs.inkMuted,
                        )
                    }
                }
            }
            if (props.allowAdd) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(cs.surfaceMuted)
                        .border(1.dp, cs.divider, CircleShape)
                        .clickable {
                            onEvent(
                                ComponentEvent.Action(
                                    action = "react_add",
                                    sourceType = "Reaction",
                                    sourceLabel = props.id,
                                ),
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("+", style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold), color = cs.inkMuted)
                }
            }
        }
    }
}

/** Every commerce-tier component. */
internal fun commerceComponents(imageLoader: ImageLoader): List<WeftComponent<*>> = listOf(
    ReceiptComponent(),
    ContactCardComponent(imageLoader),
    ReactionComponent(),
)
