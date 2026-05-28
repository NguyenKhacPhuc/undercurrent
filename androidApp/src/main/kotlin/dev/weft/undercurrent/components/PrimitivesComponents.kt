package dev.weft.undercurrent.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
// Mention — @-name pill
// =============================================================================

@Serializable
internal data class MentionProps(
    val name: String,
    /** Action fired on tap (e.g. open the user profile). Empty = inert. */
    val onTap: String = "",
)

internal class MentionComponent : WeftComponent<MentionProps>(
    name = "Mention",
    description = "An @-mention pill. name: required (without the @). onTap: optional action fired on tap. Use inline next to text or in lists of assignees.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = MentionProps.serializer(),
) {
    @Composable
    override fun Render(props: MentionProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(cs.accent.copy(alpha = 0.12f))
                .let { m ->
                    if (props.onTap.isNotBlank()) m.clickable {
                        onEvent(
                            ComponentEvent.Action(
                                action = props.onTap,
                                sourceType = "Mention",
                                sourceLabel = props.name,
                            ),
                        )
                    } else m
                }
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = "@${props.name}",
                style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                color = cs.accent,
            )
        }
    }
}

// =============================================================================
// PersonChip — small inline avatar + name
// =============================================================================

@Serializable
internal data class PersonChipProps(
    val name: String,
    /** Optional explicit initials override; auto-derived when blank. */
    val initials: String = "",
    /** Status tone for the small ring around the avatar. neutral | online | busy. */
    val ring: String = "neutral",
    val onTap: String = "",
)

internal class PersonChipComponent : WeftComponent<PersonChipProps>(
    name = "PersonChip",
    description = "Compact inline chip — small initial avatar + name. name: required. initials: optional override (auto: first letter or 2-letter initials). ring: 'neutral' (default), 'online' (accent ring), 'busy' (error ring). onTap: optional action. Use in attendee rows, assignee badges.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = PersonChipProps.serializer(),
) {
    @Composable
    override fun Render(props: PersonChipProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val initials = props.initials.ifBlank {
            props.name
                .split(' ', '-', '_')
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifBlank { "?" }
        }
        val ringColor = when (props.ring.lowercase()) {
            "online" -> cs.accent
            "busy" -> cs.error
            else -> cs.divider
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(cs.surfaceMuted)
                .let { m ->
                    if (props.onTap.isNotBlank()) m.clickable {
                        onEvent(
                            ComponentEvent.Action(
                                action = props.onTap,
                                sourceType = "PersonChip",
                                sourceLabel = props.name,
                            ),
                        )
                    } else m
                }
                .padding(start = 3.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(cs.accent.copy(alpha = 0.18f))
                    .border(1.5.dp, ringColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                    color = cs.accent,
                )
            }
            Text(
                text = props.name,
                style = tp.sansLabel.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
                color = cs.ink,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

// =============================================================================
// Money — formatted currency display (non-input)
// =============================================================================

@Serializable
internal data class MoneyProps(
    /** Amount as a string (preserves precision). */
    val amount: String,
    val currency: String = "USD",
    /** sm | md (default) | lg | xl — affects font size. */
    val size: String = "md",
    /** Optional caption above the amount, e.g. "Total", "Subtotal". */
    val label: String = "",
    /** Color tone: ink (default) | accent | error | success. */
    val tone: String = "ink",
)

internal class MoneyComponent : WeftComponent<MoneyProps>(
    name = "Money",
    description = "Display a monetary amount with currency symbol + ISO code. amount: required (string, preserves precision). currency: ISO code (USD/EUR/JPY/VND…). size: sm/md (default)/lg/xl. label: optional small caption above. tone: 'ink' (default), 'accent' (positive), 'error' (red, negative), 'success' (accent green). For input use CurrencyField.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = MoneyProps.serializer(),
) {
    @Composable
    override fun Render(props: MoneyProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val color = when (props.tone.lowercase()) {
            "accent", "success" -> cs.accent
            "error" -> cs.error
            else -> cs.ink
        }
        val (amountSize, codeSize) = when (props.size.lowercase()) {
            "sm" -> 14.sp to 10.sp
            "lg" -> 28.sp to 12.sp
            "xl" -> 40.sp to 14.sp
            else -> 20.sp to 11.sp
        }
        Column {
            if (props.label.isNotBlank()) {
                Text(
                    text = props.label.uppercase(),
                    style = tp.sansSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = cs.inkMuted,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = symbolFor(props.currency),
                    style = tp.serifBodyLarge.copy(
                        fontSize = amountSize.value.times(0.8f).sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = color.copy(alpha = 0.65f),
                )
                Text(
                    text = props.amount,
                    style = tp.serifBodyLarge.copy(
                        fontSize = amountSize,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = color,
                    modifier = Modifier.padding(start = 2.dp),
                )
                Text(
                    text = " ${props.currency.uppercase()}",
                    style = tp.sansSmall.copy(
                        fontSize = codeSize,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                    ),
                    color = cs.inkMuted,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }

    private fun symbolFor(code: String): String = when (code.uppercase()) {
        "USD", "CAD", "AUD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY", "CNY" -> "¥"
        "INR" -> "₹"
        "KRW" -> "₩"
        "VND" -> "₫"
        else -> ""
    }
}

// =============================================================================
// HoursTable — weekly open/close hours
// =============================================================================

@Serializable
internal data class HoursDay(
    val day: String,
    /** "9–17", "Closed", "10–22, 23–02" — free text. */
    val hours: String,
    /** Highlight this row as today. */
    val today: Boolean = false,
)

@Serializable
internal data class HoursTableProps(
    val rows: List<HoursDay>,
    val title: String = "Hours",
)

internal class HoursTableComponent : WeftComponent<HoursTableProps>(
    name = "HoursTable",
    description = "Weekly hours-of-operation table — day name + hours string per row. rows: list of {day, hours, today}. today: bool — highlights the current day with accent. hours: free-form (e.g. '9–17', 'Closed', '7–11, 17–22'). Use under LocationCard / venue info.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = HoursTableProps.serializer(),
    example = """{"type": "HoursTable", "props": {"title": "Hours", "rows": [{"day": "Mon", "hours": "7–18"}, {"day": "Tue", "hours": "7–18", "today": true}, {"day": "Wed", "hours": "7–18"}, {"day": "Thu", "hours": "7–18"}, {"day": "Fri", "hours": "7–20"}, {"day": "Sat", "hours": "8–20"}, {"day": "Sun", "hours": "Closed"}]}}""",
) {
    @Composable
    override fun Render(props: HoursTableProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.surfaceMuted)
                .padding(14.dp),
        ) {
            if (props.title.isNotBlank()) {
                Text(
                    text = props.title.uppercase(),
                    style = tp.sansSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.8.sp,
                    ),
                    color = cs.accent,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            props.rows.forEach { d ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = d.day,
                        style = tp.sansLabel.copy(
                            fontSize = 14.sp,
                            fontWeight = if (d.today) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (d.today) cs.accent else cs.inkMuted,
                        modifier = Modifier.width(60.dp),
                    )
                    Text(
                        text = d.hours,
                        style = tp.serifBody.copy(
                            fontSize = 14.sp,
                            fontWeight = if (d.today) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (d.today) cs.accent else cs.ink,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// =============================================================================
// Vote — thumbs up/down with counts
// =============================================================================

@Serializable
internal data class VoteProps(
    val id: String,
    /** initial up-count. */
    val ups: Int = 0,
    /** initial down-count. */
    val downs: Int = 0,
    /** Pre-cast: 'up' | 'down' | ''. */
    val cast: String = "",
)

internal class VoteComponent : WeftComponent<VoteProps>(
    name = "Vote",
    description = "Thumbs up / down with live counts. id: stable identifier (fires Action 'upvote' or 'downvote' on tap; toggling off fires the same action). ups / downs: initial counts. cast: pre-applied vote ('up' / 'down' / '' for none). Use under posts, suggestions, generated answers.",
    category = ComponentCategory.INPUT,
    propsSerializer = VoteProps.serializer(),
) {
    @Composable
    override fun Render(props: VoteProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        var cast by remember(props.id) { mutableStateOf(props.cast.lowercase()) }
        var ups by remember(props.id) { mutableIntStateOf(props.ups) }
        var downs by remember(props.id) { mutableIntStateOf(props.downs) }

        fun cycleVote(direction: String) {
            // Toggling off if same direction; switching otherwise.
            val newCast = if (cast == direction) "" else direction
            when (cast) {
                "up" -> ups = (ups - 1).coerceAtLeast(0)
                "down" -> downs = (downs - 1).coerceAtLeast(0)
            }
            when (newCast) {
                "up" -> ups += 1
                "down" -> downs += 1
            }
            cast = newCast
            onEvent(
                ComponentEvent.Action(
                    action = if (direction == "up") "upvote" else "downvote",
                    sourceType = "Vote",
                    sourceLabel = props.id,
                ),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VoteBtn(
                icon = "trend_up",
                active = cast == "up",
                count = ups,
                accent = cs.accent,
                cs = cs,
                tp = tp,
                onClick = { cycleVote("up") },
            )
            VoteBtn(
                icon = "trend_down",
                active = cast == "down",
                count = downs,
                accent = cs.error,
                cs = cs,
                tp = tp,
                onClick = { cycleVote("down") },
            )
        }
    }

    @Composable
    private fun VoteBtn(
        icon: String,
        active: Boolean,
        count: Int,
        accent: androidx.compose.ui.graphics.Color,
        cs: dev.weft.undercurrent.core.designsystem.UndercurrentColors,
        tp: dev.weft.undercurrent.core.designsystem.UndercurrentTypography,
        onClick: () -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(if (active) accent.copy(alpha = 0.15f) else cs.surfaceMuted)
                .border(1.dp, if (active) accent else cs.divider, CircleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = undercurrentIcon(icon),
                contentDescription = icon,
                tint = if (active) accent else cs.inkMuted,
                modifier = Modifier.size(15.dp),
            )
            if (count > 0) {
                Text(
                    text = " $count",
                    style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (active) accent else cs.inkMuted,
                )
            }
        }
    }
}

/** Every primitives component. */
internal val undercurrentPrimitivesComponents: List<WeftComponent<*>> = listOf(
    MentionComponent(),
    PersonChipComponent(),
    MoneyComponent(),
    HoursTableComponent(),
    VoteComponent(),
)
