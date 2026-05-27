package dev.weft.undercurrent.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.theme.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// AlbumCard — square cover + title + artist + optional play overlay
// =============================================================================

@Serializable
internal data class AlbumCardProps(
    val title: String,
    val artist: String = "",
    val coverUrl: String = "",
    /** Optional meta line shown under artist (e.g. "Album · 2024"). */
    val meta: String = "",
    /** Show a centered play button overlay on hover/always. */
    val showPlayOverlay: Boolean = false,
    val onPlay: String = "play_album",
    val onTap: String = "",
)

internal class AlbumCardComponent(private val imageLoader: ImageLoader) : WeftComponent<AlbumCardProps>(
    name = "AlbumCard",
    description = "Square album-cover card — image + title + artist + optional meta line. showPlayOverlay: shows a small centered play button on the cover (fires onPlay). onTap: optional separate action on the body (e.g. open album).",
    category = ComponentCategory.DISPLAY,
    propsSerializer = AlbumCardProps.serializer(),
) {
    @Composable
    override fun Render(props: AlbumCardProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val tappable = props.onTap.isNotBlank()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.small)
                .let { m ->
                    if (tappable) m.clickable {
                        onEvent(
                            ComponentEvent.Action(
                                action = props.onTap,
                                sourceType = "AlbumCard",
                                sourceLabel = props.title,
                            ),
                        )
                    } else m
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(UndercurrentTheme.shapes.small)
                    .background(cs.surfaceMuted),
            ) {
                if (props.coverUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = props.coverUrl,
                        imageLoader = imageLoader,
                        contentDescription = props.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        error = {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(cs.surfaceMuted))
                        },
                    )
                }
                if (props.showPlayOverlay) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(cs.accent)
                            .clickable {
                                onEvent(
                                    ComponentEvent.Action(
                                        action = props.onPlay,
                                        sourceType = "AlbumCard",
                                        sourceLabel = props.title,
                                    ),
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = undercurrentIcon("play"),
                            contentDescription = "Play",
                            tint = cs.onAccent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = props.title,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = cs.ink,
                    maxLines = 1,
                )
                if (props.artist.isNotBlank()) {
                    Text(
                        text = props.artist,
                        style = tp.sansSmall,
                        color = cs.inkMuted,
                        maxLines = 1,
                    )
                }
                if (props.meta.isNotBlank()) {
                    Text(
                        text = props.meta,
                        style = tp.sansSmall.copy(letterSpacing = 0.5.sp),
                        color = cs.inkSubtle,
                    )
                }
            }
        }
    }
}

// =============================================================================
// TrackRow — number + title + artist + duration + play
// =============================================================================

@Serializable
internal data class TrackRowProps(
    val number: Int = 0,
    val title: String,
    val artist: String = "",
    val duration: String = "",
    /** Whether this is currently playing — adds an accent indicator. */
    val playing: Boolean = false,
    val onPlay: String = "play_track",
)

internal class TrackRowComponent : WeftComponent<TrackRowProps>(
    name = "TrackRow",
    description = "Track-list row — leading track number (or playing indicator) + title + artist + duration + tap-to-play. playing: replaces the number with an animated equalizer-ish glyph and tints text accent. Compose multiple in a Stack for a tracklist.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = TrackRowProps.serializer(),
) {
    @Composable
    override fun Render(props: TrackRowProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.small)
                .clickable {
                    onEvent(
                        ComponentEvent.Action(
                            action = props.onPlay,
                            sourceType = "TrackRow",
                            sourceLabel = props.title,
                        ),
                    )
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier.width(28.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (props.playing) {
                    Text(
                        text = "▶",
                        style = tp.serifBody.copy(fontSize = 14.sp),
                        color = cs.accent,
                    )
                } else if (props.number > 0) {
                    Text(
                        text = props.number.toString(),
                        style = tp.sansSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = cs.inkMuted,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = props.title,
                    style = tp.sansLabel.copy(
                        fontWeight = if (props.playing) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 14.sp,
                    ),
                    color = if (props.playing) cs.accent else cs.ink,
                    maxLines = 1,
                )
                if (props.artist.isNotBlank()) {
                    Text(
                        text = props.artist,
                        style = tp.sansSmall,
                        color = cs.inkMuted,
                    )
                }
            }
            if (props.duration.isNotBlank()) {
                Text(
                    text = props.duration,
                    style = tp.sansSmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = cs.inkMuted,
                )
            }
        }
    }
}

// =============================================================================
// Achievement — earned badge with icon + name + description
// =============================================================================

@Serializable
internal data class AchievementProps(
    val title: String,
    val description: String = "",
    /** Single-grapheme glyph or emoji for the badge. */
    val glyph: String = "🏆",
    /** Date earned, or "Not earned" caption. */
    val earnedAt: String = "",
    /** Whether the badge is earned (color) or locked (grayscale-ish). */
    val earned: Boolean = true,
    /** bronze | silver | gold | accent — colors the badge ring. */
    val tier: String = "gold",
)

internal class AchievementComponent : WeftComponent<AchievementProps>(
    name = "Achievement",
    description = "Earned badge — circular glyph in a tier-colored ring + title + description + earned date. tier: 'bronze' / 'silver' / 'gold' / 'accent'. earned: false desaturates and dims. Use for gamification, milestones, badges.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = AchievementProps.serializer(),
    example = """{"type": "Achievement", "props": {"title": "10-day streak", "description": "Logged a note every day for 10 days.", "glyph": "🔥", "earnedAt": "Earned May 24", "tier": "gold"}}""",
) {
    @Composable
    override fun Render(props: AchievementProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val ringColor = when (props.tier.lowercase()) {
            "bronze" -> androidx.compose.ui.graphics.Color(0xFFB45309)
            "silver" -> androidx.compose.ui.graphics.Color(0xFF9CA3AF)
            "gold" -> androidx.compose.ui.graphics.Color(0xFFD97706)
            else -> cs.accent
        }
        val alpha = if (props.earned) 1f else 0.4f

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.surfaceMuted)
                .padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ringColor.copy(alpha = 0.15f * alpha))
                    .border(2.dp, ringColor.copy(alpha = alpha), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = props.glyph,
                    style = tp.serifBodyLarge.copy(fontSize = 26.sp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    text = props.title,
                    style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = cs.ink.copy(alpha = alpha),
                )
                if (props.description.isNotBlank()) {
                    Text(
                        text = props.description,
                        style = tp.serifBody.copy(fontSize = 13.sp, lineHeight = 19.sp),
                        color = cs.inkMuted.copy(alpha = alpha),
                    )
                }
                if (props.earnedAt.isNotBlank()) {
                    Text(
                        text = props.earnedAt.uppercase(),
                        style = tp.sansSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            fontSize = 10.sp,
                        ),
                        color = ringColor.copy(alpha = alpha),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

// =============================================================================
// ScoreBoard — head-to-head match score
// =============================================================================

@Serializable
internal data class ScoreBoardProps(
    val homeName: String,
    val homeScore: Int,
    val awayName: String,
    val awayScore: Int,
    /** Optional status line: 'Final', 'Q3 8:42', 'Live', etc. */
    val status: String = "",
    /** Optional venue / meta. */
    val meta: String = "",
)

internal class ScoreBoardComponent : WeftComponent<ScoreBoardProps>(
    name = "ScoreBoard",
    description = "Head-to-head score — home and away team names + scores side-by-side + optional status (Final, Q3 8:42, Live…) and meta line. Use for sports games, match summaries.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = ScoreBoardProps.serializer(),
) {
    @Composable
    override fun Render(props: ScoreBoardProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val homeWinning = props.homeScore > props.awayScore
        val awayWinning = props.awayScore > props.homeScore
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.surfaceMuted)
                .padding(16.dp),
        ) {
            if (props.status.isNotBlank()) {
                Text(
                    text = props.status.uppercase(),
                    style = tp.sansSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                    ),
                    color = cs.accent,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            ) {
                ScoreSide(
                    name = props.homeName,
                    score = props.homeScore,
                    winning = homeWinning,
                    modifier = Modifier.weight(1f),
                    cs = cs,
                    tp = tp,
                )
                Text(
                    text = "·",
                    style = tp.serifBodyLarge.copy(fontSize = 22.sp),
                    color = cs.inkSubtle,
                )
                ScoreSide(
                    name = props.awayName,
                    score = props.awayScore,
                    winning = awayWinning,
                    modifier = Modifier.weight(1f),
                    cs = cs,
                    tp = tp,
                )
            }
            if (props.meta.isNotBlank()) {
                Text(
                    text = props.meta,
                    style = tp.sansSmall,
                    color = cs.inkMuted,
                )
            }
        }
    }

    @Composable
    private fun ScoreSide(
        name: String,
        score: Int,
        winning: Boolean,
        modifier: Modifier,
        cs: dev.weft.undercurrent.theme.UndercurrentColors,
        tp: dev.weft.undercurrent.theme.UndercurrentTypography,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier,
        ) {
            Text(
                text = name,
                style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = if (winning) cs.accent else cs.inkMuted,
            )
            Text(
                text = score.toString(),
                style = tp.serifBodyLarge.copy(
                    fontSize = 44.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (winning) cs.accent else cs.ink,
            )
        }
    }
}

// =============================================================================
// LevelProgress — XP bar to next level
// =============================================================================

@Serializable
internal data class LevelProgressProps(
    /** Current level number. */
    val level: Int,
    /** Current XP within this level. */
    val xpCurrent: Int,
    /** XP needed for next level. */
    val xpNeeded: Int,
    /** Optional title like "Habit Master". */
    val title: String = "",
)

internal class LevelProgressComponent : WeftComponent<LevelProgressProps>(
    name = "LevelProgress",
    description = "Game-style level progress — large level number + optional title + XP bar to next level + 'X / Y XP' label. Use for gamified habit trackers, learning apps, fitness goals.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = LevelProgressProps.serializer(),
) {
    @Composable
    override fun Render(props: LevelProgressProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val pct = if (props.xpNeeded > 0) {
            (props.xpCurrent.toFloat() / props.xpNeeded.toFloat()).coerceIn(0f, 1f)
        } else 0f
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.surfaceMuted)
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(cs.accent.copy(alpha = 0.18f))
                    .border(2.dp, cs.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = props.level.toString(),
                    style = tp.serifBodyLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.accent,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    text = "LEVEL ${props.level}",
                    style = tp.sansSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                    ),
                    color = cs.accent,
                )
                if (props.title.isNotBlank()) {
                    Text(
                        text = props.title,
                        style = tp.sansHeader.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                        color = cs.ink,
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .padding(top = 6.dp),
                ) {
                    val w = size.width
                    val h = size.height
                    drawRoundRect(
                        color = cs.divider,
                        topLeft = Offset(0f, 0f),
                        size = Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f),
                    )
                    drawRoundRect(
                        color = cs.accent,
                        topLeft = Offset(0f, 0f),
                        size = Size(w * pct, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f),
                    )
                }
                Text(
                    text = "${props.xpCurrent} / ${props.xpNeeded} XP",
                    style = tp.sansSmall.copy(fontFamily = FontFamily.Monospace),
                    color = cs.inkMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Every music + games component. ImageLoader-bound ones constructed in factory. */
internal fun musicGamesComponents(imageLoader: ImageLoader): List<WeftComponent<*>> = listOf(
    AlbumCardComponent(imageLoader),
    TrackRowComponent(),
    AchievementComponent(),
    ScoreBoardComponent(),
    LevelProgressComponent(),
)

