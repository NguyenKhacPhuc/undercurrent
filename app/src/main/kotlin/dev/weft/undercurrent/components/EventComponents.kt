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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.theme.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// EventCard — date block + title + venue + time
// =============================================================================

@Serializable
internal data class EventCardProps(
    val title: String,
    /** Short month abbrev — "MAY", "JUN". */
    val month: String,
    /** Day number — "26". */
    val day: String,
    val time: String = "",
    val venue: String = "",
    /** Optional attendee count. */
    val attendees: String = "",
    val onTap: String = "",
)

internal class EventCardComponent : WeftComponent<EventCardProps>(
    name = "EventCard",
    description = "Calendar-style event card — left date block (month + day) + right column (title, time, venue, attendees). title / month / day required. time/venue/attendees optional. onTap: action key fired when card is tapped (empty = non-interactive). Use for upcoming events, RSVP cards, schedule rows.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = EventCardProps.serializer(),
    example = """{"type": "EventCard", "props": {"month": "MAY", "day": "30", "title": "Design review", "time": "2:00 — 3:30pm", "venue": "Conference room B", "attendees": "6 people", "onTap": "open_event"}}""",
) {
    @Composable
    override fun Render(props: EventCardProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val tappable = props.onTap.isNotBlank()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.background)
                .border(1.dp, cs.divider, UndercurrentTheme.shapes.medium)
                .let { m ->
                    if (tappable) m.clickable {
                        onEvent(
                            ComponentEvent.Action(
                                action = props.onTap,
                                sourceType = "EventCard",
                                sourceLabel = props.title,
                            ),
                        )
                    } else m
                }
                .padding(14.dp),
        ) {
            // Date block.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier
                    .width(54.dp)
                    .clip(UndercurrentTheme.shapes.small)
                    .background(cs.accent.copy(alpha = 0.10f))
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = props.month.uppercase(),
                    style = tp.sansSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                    ),
                    color = cs.accent,
                )
                Text(
                    text = props.day,
                    style = tp.serifBodyLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = cs.ink,
                )
            }
            // Body.
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    text = props.title,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = cs.ink,
                )
                if (props.time.isNotBlank()) {
                    Text(
                        text = props.time,
                        style = tp.sansSmall.copy(fontWeight = FontWeight.Medium),
                        color = cs.accent,
                    )
                }
                if (props.venue.isNotBlank()) {
                    Text(
                        text = props.venue,
                        style = tp.sansSmall,
                        color = cs.inkMuted,
                    )
                }
                if (props.attendees.isNotBlank()) {
                    Text(
                        text = props.attendees,
                        style = tp.sansSmall,
                        color = cs.inkSubtle,
                    )
                }
            }
        }
    }
}

// =============================================================================
// LocationCard — name + address + coords; no actual map
// =============================================================================

@Serializable
internal data class LocationCardProps(
    val name: String,
    val address: String = "",
    /** Optional lat/lng pair (just shown, no map). */
    val coords: String = "",
    /** Distance from user, e.g. "0.8 mi". */
    val distance: String = "",
    val onTap: String = "",
)

internal class LocationCardComponent : WeftComponent<LocationCardProps>(
    name = "LocationCard",
    description = "Location summary card — pin icon + name + address + optional coords/distance. name: required (e.g. 'Blue Bottle Coffee'). address: street + city. coords: 'lat,lng' string (just shown — no embedded map). distance: e.g. '0.8 mi'. onTap: optional action (e.g. 'open_directions'). Use when an actual map embed isn't available or warranted.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = LocationCardProps.serializer(),
) {
    @Composable
    override fun Render(props: LocationCardProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val tappable = props.onTap.isNotBlank()
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.surfaceMuted)
                .let { m ->
                    if (tappable) m.clickable {
                        onEvent(
                            ComponentEvent.Action(
                                action = props.onTap,
                                sourceType = "LocationCard",
                                sourceLabel = props.name,
                            ),
                        )
                    } else m
                }
                .padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(cs.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "📍", style = tp.serifBodyLarge.copy(fontSize = 20.sp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = props.name,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = cs.ink,
                )
                if (props.address.isNotBlank()) {
                    Text(
                        text = props.address,
                        style = tp.serifBody.copy(fontSize = 13.sp, lineHeight = 19.sp),
                        color = cs.inkMuted,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    if (props.distance.isNotBlank()) {
                        Text(
                            text = props.distance,
                            style = tp.sansSmall.copy(fontWeight = FontWeight.Medium),
                            color = cs.accent,
                        )
                    }
                    if (props.coords.isNotBlank()) {
                        if (props.distance.isNotBlank()) {
                            Text(
                                text = "  ·  ",
                                style = tp.sansSmall,
                                color = cs.inkSubtle,
                            )
                        }
                        Text(
                            text = props.coords,
                            style = tp.sansSmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            ),
                            color = cs.inkSubtle,
                        )
                    }
                }
            }
            if (tappable) {
                Icon(
                    imageVector = undercurrentIcon("arrow_forward"),
                    contentDescription = null,
                    tint = cs.inkSubtle,
                    modifier = Modifier.size(18.dp).padding(top = 4.dp),
                )
            }
        }
    }
}

// =============================================================================
// OpenStatus — open/closed indicator with hours
// =============================================================================

@Serializable
internal data class OpenStatusProps(
    /** open | closed | closing_soon. */
    val status: String = "open",
    /** e.g. "Closes 9pm", "Opens 7am tomorrow". */
    val detail: String = "",
)

internal class OpenStatusComponent : WeftComponent<OpenStatusProps>(
    name = "OpenStatus",
    description = "Open/closed indicator (for shops, services, venues). status: 'open' (green dot + 'Open'), 'closed' (gray + 'Closed'), 'closing_soon' (amber + 'Closing soon'). detail: optional caption — e.g. 'Closes 9pm', 'Opens 7am Tue'. Use under a venue title.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = OpenStatusProps.serializer(),
) {
    @Composable
    override fun Render(props: OpenStatusProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val (dotColor, label, labelColor) = when (props.status.lowercase()) {
            "closed" -> Triple(cs.inkSubtle, "Closed", cs.inkMuted)
            "closing_soon", "closing-soon", "closingsoon" -> Triple(cs.error.copy(alpha = 0.8f), "Closing soon", cs.error)
            else -> Triple(cs.accent, "Open", cs.accent)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Text(
                text = label,
                style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = labelColor,
                modifier = Modifier.padding(start = 8.dp),
            )
            if (props.detail.isNotBlank()) {
                Text(
                    text = "  ·  ${props.detail}",
                    style = tp.sansSmall,
                    color = cs.inkMuted,
                )
            }
        }
    }
}

/** Every event-tier component. */
internal val undercurrentEventComponents: List<WeftComponent<*>> = listOf(
    EventCardComponent(),
    LocationCardComponent(),
    OpenStatusComponent(),
)
