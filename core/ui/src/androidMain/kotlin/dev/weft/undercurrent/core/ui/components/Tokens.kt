package dev.weft.undercurrent.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Undercurrent's design tokens for agent-rendered components.
 *
 * The substrate's [dev.weft.compose.components.spacingDp] uses a different
 * scale; this one is tighter at the small end (xs = 2dp instead of 4dp)
 * and wider at the top (xxxl = 48dp) to match Undercurrent's editorial
 * feel — lots of breathing room between sections, snug pairs inside.
 *
 * Unknown tokens fall back to `md` (12dp).
 */
internal fun undercurrentSpacing(token: String): Dp = when (token.lowercase()) {
    "none" -> 0.dp
    "xs" -> 2.dp
    "sm" -> 6.dp
    "md" -> 12.dp
    "lg" -> 20.dp
    "xl" -> 28.dp
    "xxl" -> 40.dp
    "xxxl" -> 56.dp
    else -> 12.dp
}

/**
 * Icon vocabulary the LLM can reference by name across every component
 * that takes an icon prop. Curated — adding here exposes the icon to
 * every component automatically.
 *
 * Unknown names fall back to `info` so an LLM typo never crashes the
 * render. Always add the verbatim icon name to the component
 * description that uses it so the model knows what's available.
 */
internal fun undercurrentIcon(name: String): ImageVector = when (name.lowercase()) {
    "add" -> Icons.Filled.Add
    "arrow_back" -> Icons.AutoMirrored.Filled.ArrowBack
    "arrow_forward" -> Icons.AutoMirrored.Filled.ArrowForward
    "bolt" -> Icons.Filled.Bolt
    "bookmark" -> Icons.Filled.Bookmark
    "bookmark_outline" -> Icons.Filled.BookmarkBorder
    "check" -> Icons.Filled.Check
    "circle" -> Icons.Filled.Circle
    "close" -> Icons.Filled.Close
    "delete" -> Icons.Filled.Delete
    "done" -> Icons.Filled.Done
    "favorite" -> Icons.Filled.Favorite
    "favorite_outline" -> Icons.Filled.FavoriteBorder
    "filter" -> Icons.Filled.FilterAlt
    "inbox" -> Icons.Filled.Inbox
    "info" -> Icons.Filled.Info
    "lightbulb" -> Icons.Filled.Lightbulb
    "notes" -> Icons.AutoMirrored.Filled.Notes
    "open" -> Icons.AutoMirrored.Filled.OpenInNew
    "pause" -> Icons.Filled.Pause
    "play" -> Icons.Filled.PlayArrow
    "quote" -> Icons.Filled.FormatQuote
    "refresh" -> Icons.Filled.Refresh
    "remove" -> Icons.Filled.Remove
    "search" -> Icons.Filled.Search
    "settings" -> Icons.Filled.Settings
    "share" -> Icons.Filled.Share
    "star" -> Icons.Filled.Star
    "star_outline" -> Icons.Filled.StarBorder
    "sync" -> Icons.Filled.Sync
    "trend_down" -> Icons.AutoMirrored.Filled.TrendingDown
    "trend_flat" -> Icons.AutoMirrored.Filled.TrendingFlat
    "trend_up" -> Icons.AutoMirrored.Filled.TrendingUp
    "warning" -> Icons.Filled.Warning
    "expand" -> Icons.Filled.KeyboardArrowDown
    "collapse" -> Icons.Filled.KeyboardArrowUp
    else -> Icons.Filled.Info
}

/**
 * Documented list of icon names for component descriptions. Keep in sync
 * with [undercurrentIcon] above — components reference this so the
 * description string passed to the LLM stays accurate.
 */
internal const val UNDERCURRENT_ICON_NAMES: String =
    "add, arrow_back, arrow_forward, bolt, bookmark, bookmark_outline, " +
        "check, circle, close, delete, done, favorite, favorite_outline, " +
        "filter, inbox, info, lightbulb, notes, open, pause, play, quote, " +
        "refresh, remove, search, settings, share, star, star_outline, sync, " +
        "trend_down, trend_flat, trend_up, warning, expand, collapse"
