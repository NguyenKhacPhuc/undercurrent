package dev.weft.undercurrent.core.ext

import dev.weft.undercurrent.shared.gateway.ConversationSummary
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Shared date-grouping helpers for conversation lists. Used by both
 * [ConversationsListScreen] (full-screen browse) and the side drawer's
 * recents list (when that one migrates). Kept in one place so the
 * bucketing logic stays consistent.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/conversations/ConversationGrouping.kt`. Adjustments:
 *   - `java.util.Calendar` → `kotlinx.datetime` (timezone-correct
 *     start-of-day via `LocalDate.atStartOfDayIn(tz)`)
 *   - `java.text.SimpleDateFormat` → manual `MMM d, yyyy` formatter
 *     using `kotlinx.datetime.Month` constants (locale-agnostic, but
 *     this format only fires for conversations older than a week so
 *     the loss of locale awareness is acceptable for now)
 *   - `System.currentTimeMillis()` → `kotlin.time.Clock.System.now()`
 */
@OptIn(ExperimentalTime::class)
fun groupConversationsByRecency(
    conversations: List<ConversationSummary>,
): List<Pair<String, List<ConversationSummary>>> {
    val tz = TimeZone.currentSystemDefault()
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val todayStart = startOfDay(nowMs, tz)
    val yesterdayStart = todayStart - MS_PER_DAY
    val weekStart = todayStart - 7 * MS_PER_DAY
    val monthStart = todayStart - 30 * MS_PER_DAY

    val today = mutableListOf<ConversationSummary>()
    val yesterday = mutableListOf<ConversationSummary>()
    val thisWeek = mutableListOf<ConversationSummary>()
    val thisMonth = mutableListOf<ConversationSummary>()
    val older = mutableListOf<ConversationSummary>()

    for (c in conversations) {
        val ts = c.lastMessageAtMs
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
 * Friendly relative time for a single conversation row. "just now" /
 * "5m ago" / "3h ago" / "2d ago" / "Mar 14, 2026".
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
