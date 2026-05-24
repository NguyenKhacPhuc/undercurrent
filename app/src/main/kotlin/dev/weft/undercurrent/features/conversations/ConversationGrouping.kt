package dev.weft.undercurrent.features.conversations

import dev.weft.harness.conversation.ConversationSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Shared date-grouping helpers for conversation lists. Used by both
 * [ConversationsListScreen] (full-screen browse) and [AppDrawer] (compact
 * recents). Kept in one place so the bucketing logic stays consistent.
 */

/**
 * Bucket conversations by recency-of-last-activity. Empty buckets are
 * omitted from the result.
 *
 * Buckets: Today / Yesterday / Earlier this week / Earlier this month / Older.
 */
internal fun groupConversationsByRecency(
    conversations: List<ConversationSummary>,
): List<Pair<String, List<ConversationSummary>>> {
    val now = Calendar.getInstance()
    val todayStart = startOfDay(now.timeInMillis)
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
internal fun formatLastActivity(epochMs: Long): String {
    val now = System.currentTimeMillis()
    val ageMs = (now - epochMs).coerceAtLeast(0)
    return when {
        ageMs < MS_PER_MIN -> "just now"
        ageMs < MS_PER_HOUR -> "${ageMs / MS_PER_MIN}m ago"
        ageMs < MS_PER_DAY -> "${ageMs / MS_PER_HOUR}h ago"
        ageMs < 7 * MS_PER_DAY -> "${ageMs / MS_PER_DAY}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epochMs))
    }
}

private fun startOfDay(epochMs: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private const val MS_PER_MIN: Long = 60 * 1000L
private const val MS_PER_HOUR: Long = 60 * MS_PER_MIN
private const val MS_PER_DAY: Long = 24 * MS_PER_HOUR
