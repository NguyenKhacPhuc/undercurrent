package dev.weft.undercurrent.core.ext

import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Buckets [items] into ("Today" / "Yesterday" / "Earlier this week" /
 * "Earlier this month" / "Older") groups based on the per-item
 * timestamp returned by [timestampOf] (epoch ms). Generic so this lives
 * in `core/ext` and doesn't bleed `core/domain` types into `core/ui`.
 */
@OptIn(ExperimentalTime::class)
fun <T> groupByRecency(
    items: List<T>,
    timestampOf: (T) -> Long,
): List<Pair<String, List<T>>> {
    val tz = TimeZone.currentSystemDefault()
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val todayStart = startOfDay(nowMs, tz)
    val yesterdayStart = todayStart - MS_PER_DAY
    val weekStart = todayStart - 7 * MS_PER_DAY
    val monthStart = todayStart - 30 * MS_PER_DAY

    val today = mutableListOf<T>()
    val yesterday = mutableListOf<T>()
    val thisWeek = mutableListOf<T>()
    val thisMonth = mutableListOf<T>()
    val older = mutableListOf<T>()

    for (c in items) {
        val ts = timestampOf(c)
        when {
            ts >= todayStart -> today += c
            ts >= yesterdayStart -> yesterday += c
            ts >= weekStart -> thisWeek += c
            ts >= monthStart -> thisMonth += c
            else -> older += c
        }
    }

    return buildList {
        if (today.isNotEmpty()) add("Today" to today)
        if (yesterday.isNotEmpty()) add("Yesterday" to yesterday)
        if (thisWeek.isNotEmpty()) add("Earlier this week" to thisWeek)
        if (thisMonth.isNotEmpty()) add("Earlier this month" to thisMonth)
        if (older.isNotEmpty()) add("Older" to older)
    }
}

/**
 * Friendly relative time for a single timestamp. "just now" / "5m ago"
 * / "3h ago" / "2d ago" / "Mar 14, 2026".
 */
@OptIn(ExperimentalTime::class)
fun formatLastActivity(epochMs: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val ageMs = (now - epochMs).coerceAtLeast(0)
    return when {
        ageMs < MS_PER_MIN -> "just now"
        ageMs < MS_PER_HOUR -> "${ageMs / MS_PER_MIN}m ago"
        ageMs < MS_PER_DAY -> "${ageMs / MS_PER_HOUR}h ago"
        ageMs < 7 * MS_PER_DAY -> "${ageMs / MS_PER_DAY}d ago"
        else -> formatDate(epochMs)
    }
}

@OptIn(ExperimentalTime::class)
private fun startOfDay(epochMs: Long, tz: TimeZone): Long =
    Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(tz)
        .date
        .atStartOfDayIn(tz)
        .toEpochMilliseconds()

@OptIn(ExperimentalTime::class)
private fun formatDate(epochMs: Long): String {
    val tz = TimeZone.currentSystemDefault()
    val ldt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(tz)
    return "${ldt.month.shortName()} ${ldt.day}, ${ldt.year}"
}

private fun Month.shortName(): String = when (this) {
    Month.JANUARY -> "Jan"
    Month.FEBRUARY -> "Feb"
    Month.MARCH -> "Mar"
    Month.APRIL -> "Apr"
    Month.MAY -> "May"
    Month.JUNE -> "Jun"
    Month.JULY -> "Jul"
    Month.AUGUST -> "Aug"
    Month.SEPTEMBER -> "Sep"
    Month.OCTOBER -> "Oct"
    Month.NOVEMBER -> "Nov"
    Month.DECEMBER -> "Dec"
}

private const val MS_PER_MIN: Long = 60 * 1000L
private const val MS_PER_HOUR: Long = 60 * MS_PER_MIN
private const val MS_PER_DAY: Long = 24 * MS_PER_HOUR
