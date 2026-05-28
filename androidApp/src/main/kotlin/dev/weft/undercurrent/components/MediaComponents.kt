package dev.weft.undercurrent.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
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
// AudioPlayer — play/pause + progress bar + duration text (no actual playback)
// =============================================================================

@Serializable
internal data class AudioPlayerProps(
    val id: String,
    val title: String,
    val artist: String = "",
    /** Total duration in seconds (used to format the right label). */
    val durationSec: Int = 0,
    /** Current playback position in seconds. */
    val positionSec: Int = 0,
    /** Whether the player is currently playing. */
    val playing: Boolean = false,
)

internal class AudioPlayerComponent : WeftComponent<AudioPlayerProps>(
    name = "AudioPlayer",
    description = "Audio player UI — play/pause button + progress bar + position/duration labels. id: stable identifier (fires Action with 'play' / 'pause' / 'seek' as the action when the button or progress bar is tapped). title: required (track name). artist: optional. durationSec / positionSec: track time in seconds. playing: visual state — does NOT actually play audio (the host's audio engine wires up via the Action events).",
    category = ComponentCategory.DISPLAY,
    propsSerializer = AudioPlayerProps.serializer(),
) {
    @Composable
    override fun Render(props: AudioPlayerProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val progress = if (props.durationSec > 0) {
            (props.positionSec.toFloat() / props.durationSec.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val isPlaying = props.playing

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.surfaceMuted)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(cs.accent)
                        .clickable {
                            onEvent(
                                ComponentEvent.Action(
                                    action = if (isPlaying) "pause" else "play",
                                    sourceType = "AudioPlayer",
                                    sourceLabel = props.id,
                                ),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = undercurrentIcon(if (isPlaying) "pause" else "play"),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = cs.onAccent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        text = props.title,
                        style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = cs.ink,
                    )
                    if (props.artist.isNotBlank()) {
                        Text(
                            text = props.artist,
                            style = tp.sansSmall,
                            color = cs.inkMuted,
                        )
                    }
                }
            }
            // Progress bar.
            LinearProgressIndicator(
                progress = { progress },
                color = cs.accent,
                trackColor = cs.divider,
                drawStopIndicator = {},
                gapSize = 0.dp,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 12.dp),
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(formatSec(props.positionSec), style = tp.sansSmall, color = cs.inkMuted)
                Box(modifier = Modifier.weight(1f))
                Text(formatSec(props.durationSec), style = tp.sansSmall, color = cs.inkMuted)
            }
        }
    }

    private fun formatSec(s: Int): String {
        if (s <= 0) return "0:00"
        val mm = s / 60
        val ss = s % 60
        return "%d:%02d".format(mm, ss)
    }
}

// =============================================================================
// VideoFrame — thumbnail with centered play overlay (no playback)
// =============================================================================

@Serializable
internal data class VideoFrameProps(
    val thumbnailUrl: String,
    val title: String = "",
    val durationLabel: String = "",
    /** Action key fired on play tap. */
    val onPlay: String = "play_video",
)

internal class VideoFrameComponent(private val imageLoader: ImageLoader) : WeftComponent<VideoFrameProps>(
    name = "VideoFrame",
    description = "Video thumbnail with centered play button overlay + optional title and duration badge. thumbnailUrl: required image URL. title: shown bottom-left. durationLabel: shown bottom-right ('3:42'). onPlay: action fired on tap (default 'play_video').",
    category = ComponentCategory.DISPLAY,
    propsSerializer = VideoFrameProps.serializer(),
) {
    @Composable
    override fun Render(props: VideoFrameProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(UndercurrentTheme.shapes.medium)
                .background(cs.surfaceMuted)
                .clickable {
                    onEvent(
                        ComponentEvent.Action(
                            action = props.onPlay,
                            sourceType = "VideoFrame",
                            sourceLabel = props.title.ifBlank { "video" },
                        ),
                    )
                },
        ) {
            SubcomposeAsyncImage(
                model = props.thumbnailUrl,
                imageLoader = imageLoader,
                contentDescription = props.title.ifBlank { "video" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                error = {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(cs.surfaceMuted))
                },
            )
            // Centered play button.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(cs.ink.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = undercurrentIcon("play"),
                    contentDescription = "Play",
                    tint = cs.background,
                    modifier = Modifier.size(28.dp),
                )
            }
            if (props.durationLabel.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(cs.ink.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = props.durationLabel,
                        style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = cs.background,
                    )
                }
            }
            if (props.title.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(cs.ink.copy(alpha = 0.55f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = props.title,
                        style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = cs.background,
                    )
                }
            }
        }
    }
}

// =============================================================================
// ImageGallery — horizontally scrollable image strip with active index
// =============================================================================

@Serializable
internal data class GalleryImage(val url: String, val caption: String = "")

@Serializable
internal data class ImageGalleryProps(
    val id: String,
    val images: List<GalleryImage>,
    /** Initial focused index. */
    val initial: Int = 0,
)

internal class ImageGalleryComponent(private val imageLoader: ImageLoader) : WeftComponent<ImageGalleryProps>(
    name = "ImageGallery",
    description = "Horizontally scrolling image strip + selected-image enlarged preview above + caption. Tapping a thumbnail focuses it. id: stable identifier (fires TextChanged with the focused image url on tap). images: list of {url, caption}. initial: starting index.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = ImageGalleryProps.serializer(),
) {
    @Composable
    override fun Render(props: ImageGalleryProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        if (props.images.isEmpty()) return
        var focusedIdx by remember(props.id) {
            mutableIntStateOf(props.initial.coerceIn(0, props.images.size - 1))
        }
        val focused = props.images[focusedIdx.coerceIn(0, props.images.size - 1)]

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Main image.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(UndercurrentTheme.shapes.medium)
                    .background(cs.surfaceMuted),
            ) {
                SubcomposeAsyncImage(
                    model = focused.url,
                    imageLoader = imageLoader,
                    contentDescription = focused.caption.ifBlank { "image" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                    error = {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).background(cs.surfaceMuted))
                    },
                )
            }
            if (focused.caption.isNotBlank()) {
                Text(
                    text = focused.caption,
                    style = tp.serifBody.copy(
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    ),
                    color = cs.inkMuted,
                )
            }
            // Thumbnail strip.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                props.images.forEachIndexed { i, img ->
                    val isFocused = i == focusedIdx
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(UndercurrentTheme.shapes.small)
                            .background(cs.surfaceMuted)
                            .border(
                                width = if (isFocused) 2.dp else 0.dp,
                                color = if (isFocused) cs.accent else cs.divider,
                                shape = UndercurrentTheme.shapes.small,
                            )
                            .clickable {
                                focusedIdx = i
                                onEvent(
                                    ComponentEvent.TextChanged(
                                        sourceId = props.id,
                                        value = img.url,
                                    ),
                                )
                            },
                    ) {
                        SubcomposeAsyncImage(
                            model = img.url,
                            imageLoader = imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp),
                            error = { Box(modifier = Modifier.size(56.dp).background(cs.surfaceMuted)) },
                        )
                    }
                }
            }
        }
    }
}

/** Every media component. ImageLoader-bound ones constructed in factory. */
internal fun mediaComponents(imageLoader: ImageLoader): List<WeftComponent<*>> = listOf(
    AudioPlayerComponent(),
    VideoFrameComponent(imageLoader),
    ImageGalleryComponent(imageLoader),
)
