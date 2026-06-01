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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.component_countdown_invalid_target
import dev.weft.undercurrent.core.resources.component_countdown_missing_target
import dev.weft.undercurrent.core.resources.component_countdown_overdue_suffix
import dev.weft.undercurrent.core.resources.component_countdown_to_go_suffix
import dev.weft.undercurrent.core.resources.component_countdown_unit_days
import dev.weft.undercurrent.core.resources.component_countdown_unit_hours
import dev.weft.undercurrent.core.resources.component_countdown_unit_minutes
import dev.weft.undercurrent.core.resources.component_countdown_unit_seconds
import dev.weft.undercurrent.core.resources.component_countdown_unit_weeks
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

// =============================================================================
// Calendar — month grid with selectable + marked days
// =============================================================================

@Serializable
internal data class CalendarProps(
    val id: String,
    /** Year + month to render, e.g. "2026-05". Defaults to the current month. */
    val month: String = "",
    /** ISO date "YYYY-MM-DD" currently selected. */
    val selected: String = "",
    /** ISO dates that should show a marker dot (e.g. days with activity). */
    val marked: List<String> = emptyList(),
    /** Show the month name in a header row. */
    val showHeader: Boolean = true,
)

@OptIn(ExperimentalTime::class)
internal class CalendarComponent : WeftComponent<CalendarProps>(
    name = "Calendar",
    description = "Full month-grid calendar. month: 'YYYY-MM' (defaults to current). selected: ISO date currently chosen. marked: list of ISO dates to show a dot under (e.g. days with notes). id: stable identifier (fires TextChanged with the ISO date when a day is tapped). Use for date picking, habit grids, schedule overviews.",
    category = ComponentCategory.INPUT,
    propsSerializer = CalendarProps.serializer(),
    example = """{"type": "Calendar", "props": {"id": "pick_date", "month": "2026-05", "selected": "2026-05-26", "marked": ["2026-05-01", "2026-05-03", "2026-05-15"]}}""",
) {
    @Composable
    override fun Render(props: CalendarProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography

        val yearMonth = parseYearMonth(props.month) ?: currentYearMonth()
        val markedSet = remember(props.marked) { props.marked.toSet() }
        var selectedDate by remember(props.id, props.selected, props.month) {
            mutableStateOf(parseDate(props.selected))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (props.showHeader) {
                Text(
                    text = "${yearMonth.month.fullName()} ${yearMonth.year}",
                    style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                    color = cs.ink,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            // Weekday header row (Sun-Sat or Mon-Sun depending on locale; ISO is Mon-first).
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { wd ->
                    Box(
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = wd,
                            style = tp.sansSmall.copy(letterSpacing = 0.5.sp),
                            color = cs.inkMuted,
                        )
                    }
                }
            }
            // Build the grid: pad leading blanks so day 1 lands on its weekday.
            val firstDay = yearMonth.atDay(1)
            val leadingBlanks = (firstDay.dayOfWeek.isoDayNumber - 1).coerceAtLeast(0)
            val totalDays = yearMonth.lengthOfMonth()
            val cells = (1..leadingBlanks).map { 0 } + (1..totalDays).toList()
            val rows = cells.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                rows.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { day ->
                            Box(modifier = Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (day == 0) {
                                    // padding cell
                                    Box(modifier = Modifier.size(36.dp))
                                } else {
                                    val date = yearMonth.atDay(day)
                                    val iso = date.toString()
                                    val isSelected = selectedDate == date
                                    val isMarked = iso in markedSet
                                    DayCell(
                                        day = day,
                                        isSelected = isSelected,
                                        isMarked = isMarked,
                                        onTap = {
                                            selectedDate = date
                                            onEvent(
                                                ComponentEvent.TextChanged(
                                                    sourceId = props.id,
                                                    value = iso,
                                                ),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        // Pad the last row when fewer than 7 days remain so the
                        // grid stays aligned.
                        repeat(7 - row.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DayCell(day: Int, isSelected: Boolean, isMarked: Boolean, onTap: () -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) cs.accent else cs.background)
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = if (isSelected) cs.accent else cs.divider,
                        shape = CircleShape,
                    )
                    .clickable(onClick = onTap),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.toString(),
                    style = tp.sansLabel.copy(
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (isSelected) cs.onAccent else cs.ink,
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMarked && !isSelected) cs.accent
                        else androidx.compose.ui.graphics.Color.Transparent,
                    ),
            )
        }
    }
}

/**
 * Local shim — kotlinx-datetime doesn't ship a `YearMonth` type. Just a
 * tiny pair carrying the bits the calendar grid needs (year, month) +
 * the two operations the call site uses (atDay, lengthOfMonth).
 */
internal data class YearMonth(val year: Int, val month: Month) {
    fun atDay(day: Int): LocalDate = LocalDate(year, month, day)

    fun lengthOfMonth(): Int = when (month) {
        Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY,
        Month.AUGUST, Month.OCTOBER, Month.DECEMBER -> 31
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        Month.FEBRUARY -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }

    private fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}

@OptIn(ExperimentalTime::class)
private fun currentYearMonth(): YearMonth {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return YearMonth(now.year, now.month)
}

/**
 * Parses "YYYY-MM" (e.g. "2026-05"). Returns null on any input that
 * isn't strictly two integers separated by a dash.
 */
private fun parseYearMonth(s: String): YearMonth? = runCatching {
    if (s.isBlank()) return@runCatching null
    val parts = s.split("-")
    if (parts.size != 2) return@runCatching null
    val year = parts[0].toInt()
    val monthNum = parts[1].toInt()
    YearMonth(year, Month(monthNum))
}.getOrNull()

private fun parseDate(s: String): LocalDate? = runCatching {
    if (s.isBlank()) null else LocalDate.parse(s)
}.getOrNull()

private fun Month.fullName(): String = when (this) {
    Month.JANUARY -> "January"
    Month.FEBRUARY -> "February"
    Month.MARCH -> "March"
    Month.APRIL -> "April"
    Month.MAY -> "May"
    Month.JUNE -> "June"
    Month.JULY -> "July"
    Month.AUGUST -> "August"
    Month.SEPTEMBER -> "September"
    Month.OCTOBER -> "October"
    Month.NOVEMBER -> "November"
    Month.DECEMBER -> "December"
    else -> name
}

// =============================================================================
// Countdown — time-until-target display, auto-formats days/hours/minutes
// =============================================================================

@Serializable
internal data class CountdownProps(
    /** ISO date or datetime target, e.g. "2026-06-15" or "2026-06-15T14:30:00Z". */
    val target: String,
    /** Optional label shown under the time. */
    val label: String = "",
    /** sm | md (default) | lg — affects font size. */
    val size: String = "md",
)

@OptIn(ExperimentalTime::class)
internal class CountdownComponent : WeftComponent<CountdownProps>(
    name = "Countdown",
    description = "Time-until-target display. target: required ISO date 'YYYY-MM-DD' or datetime 'YYYY-MM-DDTHH:MM:SSZ'. label: small caption under the number. size: sm/md (default)/lg. Auto-formats: weeks, days, hours, or minutes — whichever is largest non-zero unit. Negative (past) targets show 'overdue by …'.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = CountdownProps.serializer(),
) {
    @Composable
    override fun Render(props: CountdownProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val now = Clock.System.now()
        val target = parseInstant(props.target)
        val invalidTarget = stringResource(Res.string.component_countdown_invalid_target)
        val overdueSuffix = stringResource(Res.string.component_countdown_overdue_suffix)
        val toGoSuffix = stringResource(Res.string.component_countdown_to_go_suffix)
        val (primary, secondary, isPast) = if (target == null) {
            Triple(stringResource(Res.string.component_countdown_missing_target), invalidTarget, false)
        } else {
            val seconds = (target - now).inWholeSeconds
            val absSec = abs(seconds)
            val (n, unitKey) = bestUnit(absSec)
            val unit = when (unitKey) {
                "weeks" -> stringResource(Res.string.component_countdown_unit_weeks)
                "days" -> stringResource(Res.string.component_countdown_unit_days)
                "hours" -> stringResource(Res.string.component_countdown_unit_hours)
                "minutes" -> stringResource(Res.string.component_countdown_unit_minutes)
                else -> stringResource(Res.string.component_countdown_unit_seconds)
            }
            val past = seconds < 0L
            Triple(
                n.toString(),
                "$unit ${if (past) overdueSuffix else toGoSuffix}",
                past,
            )
        }
        val fontSize = when (props.size.lowercase()) {
            "sm" -> 28.sp
            "lg" -> 56.sp
            else -> 40.sp
        }
        val accent = if (isPast) cs.error else cs.accent
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = primary,
                style = tp.serifBodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = fontSize),
                color = accent,
            )
            Text(
                text = secondary,
                style = tp.sansSmall.copy(letterSpacing = 0.5.sp),
                color = cs.inkMuted,
            )
            if (props.label.isNotBlank()) {
                Text(
                    text = props.label,
                    style = tp.serifBody.copy(fontSize = 14.sp),
                    color = cs.ink,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun parseInstant(s: String): Instant? = runCatching {
        // Accept date-only ("2026-06-15") by treating as start-of-day UTC.
        if (s.length == 10) LocalDate.parse(s).atStartOfDayIn(TimeZone.UTC)
        else Instant.parse(s)
    }.getOrNull()

    private fun bestUnit(secAbs: Long): Pair<Long, String> = when {
        secAbs >= 7 * 24 * 3600 -> (secAbs / (7 * 24 * 3600)) to "weeks"
        secAbs >= 24 * 3600 -> (secAbs / (24 * 3600)) to "days"
        secAbs >= 3600 -> (secAbs / 3600) to "hours"
        secAbs >= 60 -> (secAbs / 60) to "minutes"
        else -> secAbs to "seconds"
    }
}

/** Every calendar/time component. */
internal val undercurrentCalendarComponents: List<WeftComponent<*>> = listOf(
    CalendarComponent(),
    CountdownComponent(),
)
