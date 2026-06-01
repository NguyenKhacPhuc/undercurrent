package dev.weft.undercurrent.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.cd_confirmed
import dev.weft.undercurrent.core.resources.component_boarding_pass_date
import dev.weft.undercurrent.core.resources.component_boarding_pass_flight
import dev.weft.undercurrent.core.resources.component_boarding_pass_gate
import dev.weft.undercurrent.core.resources.component_boarding_pass_passenger
import dev.weft.undercurrent.core.resources.component_boarding_pass_seat
import dev.weft.undercurrent.core.resources.component_boarding_status_boarded
import dev.weft.undercurrent.core.resources.component_boarding_status_boarding
import dev.weft.undercurrent.core.resources.component_boarding_status_delayed
import dev.weft.undercurrent.core.resources.component_boarding_status_on_time
import dev.weft.undercurrent.core.resources.component_flight_nonstop
import dev.weft.undercurrent.core.resources.component_flight_one_stop
import dev.weft.undercurrent.core.resources.component_flight_stops
import dev.weft.undercurrent.core.resources.component_reservation_confirmation
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

// =============================================================================
// BoardingPass — airline-ticket-style card
// =============================================================================

@Serializable
internal data class BoardingPassProps(
    val passenger: String,
    val origin: String,
    val destination: String,
    val departTime: String,
    val arriveTime: String,
    val date: String,
    val flightNumber: String,
    val seat: String,
    val gate: String = "",
    /** boarding | scheduled | delayed | boarded. */
    val status: String = "scheduled",
)

internal class BoardingPassComponent : WeftComponent<BoardingPassProps>(
    name = "BoardingPass",
    description = "Airline-ticket-style card with origin/destination, times, flight number, seat, gate, and status. All text fields free-form (3-letter airport codes work well). status: 'boarding' (accent), 'scheduled' (default), 'delayed' (error), 'boarded' (muted).",
    category = ComponentCategory.DISPLAY,
    propsSerializer = BoardingPassProps.serializer(),
    example = """{"type": "BoardingPass", "props": {"passenger": "Phuc Nguyen", "origin": "SGN", "destination": "JFK", "departTime": "23:45", "arriveTime": "06:20+1", "date": "Mon 26 May", "flightNumber": "VN98", "seat": "14A", "gate": "B12", "status": "boarding"}}""",
) {
    @Composable
    override fun Render(props: BoardingPassProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val (statusLabel, statusColor) = when (props.status.lowercase()) {
            "boarding" -> stringResource(Res.string.component_boarding_status_boarding) to cs.accent
            "delayed" -> stringResource(Res.string.component_boarding_status_delayed) to cs.error
            "boarded" -> stringResource(Res.string.component_boarding_status_boarded) to cs.inkSubtle
            else -> stringResource(Res.string.component_boarding_status_on_time) to cs.inkMuted
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.background)
                .border(1.dp, cs.divider, UndercurrentTheme.shapes.medium),
        ) {
            // Top — route.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = props.origin,
                        style = tp.serifBodyLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                        ),
                        color = cs.ink,
                    )
                    Text(
                        text = props.departTime,
                        style = tp.sansLabel.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        ),
                        color = cs.inkMuted,
                    )
                }
                Icon(
                    imageVector = undercurrentIcon("arrow_forward"),
                    contentDescription = null,
                    tint = cs.accent,
                    modifier = Modifier.size(20.dp),
                )
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = props.destination,
                        style = tp.serifBodyLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                        ),
                        color = cs.ink,
                    )
                    Text(
                        text = props.arriveTime,
                        style = tp.sansLabel.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        ),
                        color = cs.inkMuted,
                    )
                }
            }
            // Dotted divider.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(24) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(cs.divider),
                    )
                }
            }
            // Details grid.
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                DetailCell(stringResource(Res.string.component_boarding_pass_passenger), props.passenger, modifier = Modifier.weight(2f), cs = cs, tp = tp)
                DetailCell(stringResource(Res.string.component_boarding_pass_date), props.date, modifier = Modifier.weight(1f), cs = cs, tp = tp)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)) {
                DetailCell(stringResource(Res.string.component_boarding_pass_flight), props.flightNumber, modifier = Modifier.weight(1f), cs = cs, tp = tp)
                DetailCell(stringResource(Res.string.component_boarding_pass_seat), props.seat, modifier = Modifier.weight(1f), cs = cs, tp = tp)
                if (props.gate.isNotBlank()) {
                    DetailCell(stringResource(Res.string.component_boarding_pass_gate), props.gate, modifier = Modifier.weight(1f), cs = cs, tp = tp)
                }
            }
            // Status footer.
            HorizontalDivider(color = cs.divider)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(18.dp),
            ) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor),
                )
                Text(
                    text = statusLabel,
                    style = tp.sansSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                    ),
                    color = statusColor,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    @Composable
    private fun DetailCell(
        label: String,
        value: String,
        modifier: Modifier,
        cs: dev.weft.undercurrent.core.designsystem.UndercurrentColors,
        tp: dev.weft.undercurrent.core.designsystem.UndercurrentTypography,
    ) {
        Column(modifier = modifier) {
            Text(
                text = label,
                style = tp.sansSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                ),
                color = cs.inkSubtle,
            )
            Text(
                text = value,
                style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = cs.ink,
            )
        }
    }
}

// =============================================================================
// FlightSegment — compact single-leg flight row
// =============================================================================

@Serializable
internal data class FlightSegmentProps(
    val origin: String,
    val destination: String,
    val departTime: String,
    val arriveTime: String,
    /** Duration, e.g. "5h 20m". */
    val duration: String = "",
    /** Stops: 0 = nonstop, 1 = 1 stop, etc. */
    val stops: Int = 0,
    /** Carrier name or code. */
    val carrier: String = "",
)

internal class FlightSegmentComponent : WeftComponent<FlightSegmentProps>(
    name = "FlightSegment",
    description = "Compact single-leg flight row — origin/destination, times, duration, stops, carrier. Use multiple in a Stack for itinerary results. stops: 0 = 'Nonstop', N = 'N stop' / 'N stops'.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = FlightSegmentProps.serializer(),
) {
    @Composable
    override fun Render(props: FlightSegmentProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val stopsLabel = when (props.stops) {
            0 -> stringResource(Res.string.component_flight_nonstop)
            1 -> stringResource(Res.string.component_flight_one_stop)
            else -> stringResource(Res.string.component_flight_stops, props.stops)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.small)
                .background(cs.surfaceMuted)
                .padding(14.dp),
        ) {
            // Times column.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = props.departTime,
                    style = tp.serifBodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.ink,
                )
                Text(
                    text = props.origin,
                    style = tp.sansSmall.copy(letterSpacing = 0.8.sp),
                    color = cs.inkMuted,
                )
            }
            // Center timeline.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1.5f),
            ) {
                if (props.duration.isNotBlank()) {
                    Text(
                        text = props.duration,
                        style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = cs.accent,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(cs.accent))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.5.dp)
                            .background(cs.divider),
                    )
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(cs.accent))
                }
                Text(
                    text = stopsLabel,
                    style = tp.sansSmall,
                    color = if (props.stops == 0) cs.inkMuted else cs.error,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = props.arriveTime,
                    style = tp.serifBodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.ink,
                )
                Text(
                    text = props.destination,
                    style = tp.sansSmall.copy(letterSpacing = 0.8.sp),
                    color = cs.inkMuted,
                )
                if (props.carrier.isNotBlank()) {
                    Text(
                        text = props.carrier,
                        style = tp.sansSmall,
                        color = cs.inkSubtle,
                    )
                }
            }
        }
    }
}

// =============================================================================
// ItineraryDay — header + ordered activities for one day
// =============================================================================

@Serializable
internal data class ItineraryActivity(
    val time: String,
    val title: String,
    val location: String = "",
    /** transport | food | sight | rest | event — sets the icon. */
    val kind: String = "event",
)

@Serializable
internal data class ItineraryDayProps(
    val dayLabel: String,
    val date: String = "",
    val activities: List<ItineraryActivity>,
)

internal class ItineraryDayComponent : WeftComponent<ItineraryDayProps>(
    name = "ItineraryDay",
    description = "One-day itinerary block — bold day label + date, then a list of activities with time + icon + title + location. activities[].kind: 'transport' / 'food' / 'sight' / 'rest' / 'event'. Use a Stack of these for a multi-day trip plan.",
    category = ComponentCategory.LAYOUT,
    propsSerializer = ItineraryDayProps.serializer(),
    example = """{"type": "ItineraryDay", "props": {"dayLabel": "Day 1", "date": "Sat, May 31", "activities": [{"time": "09:00", "title": "Flight to Berlin", "location": "TXL → BER", "kind": "transport"}, {"time": "14:00", "title": "Lunch at Mustafa's", "location": "Mehringdamm", "kind": "food"}, {"time": "16:00", "title": "Brandenburg Gate walk", "location": "Mitte", "kind": "sight"}, {"time": "21:00", "title": "Check in", "kind": "rest"}]}}""",
) {
    @Composable
    override fun Render(props: ItineraryDayProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = props.dayLabel,
                    style = tp.sansHeader.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = cs.ink,
                    modifier = Modifier.weight(1f),
                )
                if (props.date.isNotBlank()) {
                    Text(
                        text = props.date,
                        style = tp.sansSmall.copy(letterSpacing = 0.5.sp),
                        color = cs.inkMuted,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                props.activities.forEachIndexed { i, activity ->
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = activity.time,
                            style = tp.sansSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = cs.inkMuted,
                            modifier = Modifier.width(52.dp).padding(top = 14.dp),
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(28.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(cs.accent.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = activityIcon(activity.kind),
                                    style = tp.serifBody.copy(fontSize = 14.sp),
                                )
                            }
                            if (i < props.activities.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(24.dp)
                                        .padding(top = 4.dp)
                                        .background(cs.divider),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp, top = 12.dp, bottom = 8.dp)) {
                            Text(
                                text = activity.title,
                                style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                color = cs.ink,
                            )
                            if (activity.location.isNotBlank()) {
                                Text(
                                    text = activity.location,
                                    style = tp.sansSmall,
                                    color = cs.inkMuted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun activityIcon(kind: String): String = when (kind.lowercase()) {
        "transport" -> "✈"
        "food" -> "🍽"
        "sight" -> "📍"
        "rest" -> "🏨"
        else -> "•"
    }
}

// =============================================================================
// HotelCard — image + name + rating + price
// =============================================================================

@Serializable
internal data class HotelCardProps(
    val name: String,
    val location: String = "",
    val imageUrl: String = "",
    /** 0-5, supports half stars. */
    val rating: Float = 0f,
    /** Number of reviews. */
    val reviews: Int = 0,
    /** Per-night price, formatted (e.g. "$120"). */
    val pricePerNight: String = "",
    val currency: String = "USD",
    /** Tag badges shown above the name (e.g. "Breakfast included"). */
    val tags: List<String> = emptyList(),
    val onTap: String = "",
)

internal class HotelCardComponent(private val imageLoader: ImageLoader) : WeftComponent<HotelCardProps>(
    name = "HotelCard",
    description = "Hotel listing card — image + tags + name + location + star rating + reviews + per-night price + currency. onTap: optional action for full details. Use in lists of search results.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = HotelCardProps.serializer(),
) {
    @Composable
    override fun Render(props: HotelCardProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.background)
                .border(1.dp, cs.divider, UndercurrentTheme.shapes.medium),
        ) {
            if (props.imageUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(cs.surfaceMuted),
                ) {
                    SubcomposeAsyncImage(
                        model = props.imageUrl,
                        imageLoader = imageLoader,
                        contentDescription = props.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        error = { Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(cs.surfaceMuted)) },
                    )
                    if (props.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            props.tags.take(3).forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(cs.ink.copy(alpha = 0.7f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = tag,
                                        style = tp.sansSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                        ),
                                        color = cs.background,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = props.name,
                            style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                            color = cs.ink,
                        )
                        if (props.location.isNotBlank()) {
                            Text(
                                text = props.location,
                                style = tp.sansSmall,
                                color = cs.inkMuted,
                            )
                        }
                    }
                    if (props.pricePerNight.isNotBlank()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = props.pricePerNight,
                                style = tp.serifBodyLarge.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = cs.accent,
                            )
                            Text(
                                text = "/night · ${props.currency.uppercase()}",
                                style = tp.sansSmall,
                                color = cs.inkMuted,
                            )
                        }
                    }
                }
                if (props.rating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = "★",
                            style = tp.serifBody.copy(fontSize = 14.sp),
                            color = cs.accent,
                        )
                        Text(
                            text = " ${props.rating.toFixed(1)}",
                            style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = cs.ink,
                        )
                        if (props.reviews > 0) {
                            Text(
                                text = "  · ${props.reviews} reviews",
                                style = tp.sansSmall,
                                color = cs.inkMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// ConfirmationCard — booking confirmed summary
// =============================================================================

@Serializable
internal data class ConfirmationCardProps(
    val title: String,
    val confirmationNumber: String,
    val subtitle: String = "",
    /** Optional list of bullets shown below ("Check-in: 3pm", "Free cancellation"). */
    val notes: List<String> = emptyList(),
)

internal class ConfirmationCardComponent : WeftComponent<ConfirmationCardProps>(
    name = "ConfirmationCard",
    description = "Booking confirmation summary — large check + title + confirmation number + optional notes list. Use after a successful purchase / booking / signup.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = ConfirmationCardProps.serializer(),
) {
    @Composable
    override fun Render(props: ConfirmationCardProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.accent.copy(alpha = 0.08f))
                .border(1.dp, cs.accent.copy(alpha = 0.3f), UndercurrentTheme.shapes.medium)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(cs.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = undercurrentIcon("check"),
                        contentDescription = stringResource(Res.string.cd_confirmed),
                        tint = cs.onAccent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = props.title,
                    style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                    color = cs.ink,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            if (props.subtitle.isNotBlank()) {
                Text(
                    text = props.subtitle,
                    style = tp.serifBody.copy(fontSize = 14.sp, lineHeight = 21.sp),
                    color = cs.inkMuted,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(UndercurrentTheme.shapes.small)
                    .background(cs.background)
                    .padding(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.component_reservation_confirmation),
                    style = tp.sansSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        fontSize = 10.sp,
                    ),
                    color = cs.inkMuted,
                )
                Text(
                    text = props.confirmationNumber,
                    style = tp.serifBodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = cs.ink,
                )
            }
            if (props.notes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    props.notes.forEach { note ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(text = "·", style = tp.serifBody.copy(fontSize = 14.sp), color = cs.accent)
                            Text(
                                text = note,
                                style = tp.serifBody.copy(fontSize = 13.sp, lineHeight = 20.sp),
                                color = cs.ink,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Every travel-tier component. */
internal fun travelComponents(imageLoader: ImageLoader): List<WeftComponent<*>> = listOf(
    BoardingPassComponent(),
    FlightSegmentComponent(),
    ItineraryDayComponent(),
    HotelCardComponent(imageLoader),
    ConfirmationCardComponent(),
)
